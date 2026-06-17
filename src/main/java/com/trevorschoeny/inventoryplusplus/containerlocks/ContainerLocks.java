package com.trevorschoeny.inventoryplusplus.containerlocks;

import com.mojang.serialization.Codec;
import com.trevorschoeny.menukit.core.MKSlotState;
import com.trevorschoeny.menukit.core.SlotStateChannel;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

import org.jetbrains.annotations.Nullable;

/**
 * Container Locks — the companion (IPP) half of Inventory Plus's Locked Slots:
 * shared, server-persistent locks on placed storage containers.
 *
 * <p>This class is <b>universal and free of any IP reference</b>, so it holds
 * the channel and answers enforcement on both sides — including a dedicated
 * server where IP (client-only) is absent. The client-side UI recognition lives
 * in {@link ContainerLockProvider} (registered on the client only).
 *
 * <h3>What rides MenuKit</h3>
 *
 * A single {@code SHARED} per-slot channel (§0049): one lock value per slot,
 * synced live to every viewer, writable by any of them; on shulkers it also
 * travels the break→place cycle for free (§0048). We register the channel and
 * write nothing else (§0019).
 *
 * <h3>Scope — placed simple storage containers</h3>
 *
 * {@link #handles} matches, by real (server-side) type: single chest + trapped
 * ({@link ChestBlockEntity}), barrel, shulker, hopper, dispenser + dropper
 * ({@link DispenserBlockEntity}), and the {@link CompoundContainer} backing a
 * double chest (§0050 resolves each half to its own placed-chest key). It
 * excludes functional containers (furnace/brewing/…), the player inventory, and
 * the ender chest (IP's client-side lock).
 */
public final class ContainerLocks {

    private ContainerLocks() {}

    /** SHARED boolean lock channel — one value per placed-container slot. */
    public static SlotStateChannel<Boolean> CHANNEL;

    /** Registers the channel. Universal; call once at common init before any container menu opens. */
    public static void register() {
        CHANNEL = MKSlotState.register(
                Identifier.fromNamespaceAndPath("inventoryplus", "container_lock"),
                Codec.BOOL,
                StreamCodec.<RegistryFriendlyByteBuf, Boolean>of(
                        (buf, v) -> buf.writeBoolean(v),
                        buf -> buf.readBoolean()),
                false,
                SlotStateChannel.Visibility.SHARED);
    }

    // ── Server-side recognition (real container types) ──────────────────────

    /**
     * True if {@code container} is a placed simple-storage container this feature
     * locks, by its real server-side type. The subclass checks cover trapped
     * chest (a {@link ChestBlockEntity}) and dropper (a {@link DispenserBlockEntity});
     * the {@link CompoundContainer} case is the double chest, which §0050 resolves
     * per slot to the owning half. Used by the server enforcement + automation
     * paths; the client UI uses {@link ContainerLockProvider} instead.
     */
    public static boolean handles(Container container) {
        return container instanceof ChestBlockEntity
                || container instanceof BarrelBlockEntity
                || container instanceof ShulkerBoxBlockEntity
                || container instanceof HopperBlockEntity
                || container instanceof DispenserBlockEntity
                || container instanceof CompoundContainer;
    }

    // ── Lock reads ──────────────────────────────────────────────────────────

    /**
     * Lock check for the server-side shift-click enforcement mixin. Routes through
     * the menu-free read ({@link #isLocked(Container, int)}), <b>not</b> the
     * slot-based {@code CHANNEL.get(slot)}: the slot-based read derives the server
     * from a resolvable viewer, and {@code moveItemStackTo} provides none for a
     * placed container on the server thread, so it reads false. The menu-free read
     * (§0050) derives the server from the container itself — correct here, and the
     * same path the automation enforcement already uses successfully.
     */
    public static boolean isLocked(Slot slot) {
        return isLocked(slot.container, slot.getContainerSlot());
    }

    /**
     * Menu-free read (§0050) — for automation (hopper / dropper / dispenser),
     * which touches a container by index with no Slot and no open menu. Returns
     * the SHARED value; server-side; a double chest resolves to the owning half
     * automatically.
     */
    public static boolean isLocked(Container container, int slotIndex) {
        return handles(container) && CHANNEL.get(container, slotIndex);
    }

    // ── Non-modded bypass (§0050) — acting-player capability gate ────────────
    //
    // moveItemStackTo carries no player, so the click-capture mixin stashes the
    // acting player here for the duration of a click; the enforcement mixin reads
    // it to decide whether the lock binds this player. Automation has no player —
    // machines always respect locks — so it never consults this.
    private static final ThreadLocal<Player> ACTING_PLAYER = new ThreadLocal<>();

    public static void setActingPlayer(@Nullable Player player) {
        if (player != null) ACTING_PLAYER.set(player);
    }

    public static void clearActingPlayer() {
        ACTING_PLAYER.remove();
    }

    /**
     * True if a locked container slot should bind whoever is currently acting. A
     * server player whose client can't receive slot-state sync (no MenuKit)
     * bypasses the lock — they can't see it, so it doesn't wall them (Trev's call,
     * §0050 {@code isSlotStateCapable}). Client prediction and programmatic moves
     * (no captured {@link ServerPlayer}) enforce by default: the client is modded
     * by definition, and a null acting player is a safe block.
     */
    public static boolean enforceForActingPlayer() {
        Player p = ACTING_PLAYER.get();
        if (p instanceof ServerPlayer sp) {
            return MKSlotState.isSlotStateCapable(sp);
        }
        return true;
    }
}
