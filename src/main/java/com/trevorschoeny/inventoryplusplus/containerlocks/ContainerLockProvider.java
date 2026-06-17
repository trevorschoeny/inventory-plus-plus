package com.trevorschoeny.inventoryplusplus.containerlocks;

import com.trevorschoeny.inventoryplus.lockedslots.SlotLockProvider;
import com.trevorschoeny.inventoryplus.sort.ContainerOpenTracker;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TrappedChestBlock;

/**
 * Bridges IP's client-side lock seam ({@link SlotLockProvider}) to the shared
 * container-lock {@link ContainerLocks#CHANNEL}.
 *
 * <p>Registered into IP's {@code LockedSlots} on the client only (IP is
 * client-only), so IP's unified lock-check, the {@code L} keybind, edit-mode
 * click/drag, the lock icon, and Sorting / Move Matching skip all recognize
 * placed-container locks with no change to IP's rules. Writes flow through the
 * channel → C2S → server persist + live broadcast to every viewer (§0049).
 *
 * <h3>Why recognition can't use the container type here</h3>
 *
 * This runs on the client, where an open chest/shulker/barrel/… menu wraps a
 * generic {@code SimpleContainer}, not the real BlockEntity — so the slot's
 * container can't tell us it's a placed chest. We identify it by the block the
 * player opened, via IP's always-on {@link ContainerOpenTracker}. (Server-side
 * recognition uses the real type via {@link ContainerLocks#handles}.)
 *
 * <p>Because this class references IP's client-only types it must never load on
 * a dedicated server — only the client entrypoint instantiates it.
 */
public final class ContainerLockProvider implements SlotLockProvider {

    @Override
    public boolean handles(Slot slot) {
        // Server / SP integrated-server (real type) — defensive; on the client
        // render thread the container is a SimpleContainer and this is false.
        if (ContainerLocks.handles(slot.container)) return true;
        // Client render thread — identify the placed container by its block.
        return isClientPlacedContainer(slot);
    }

    @Override
    public boolean isLocked(Slot slot) {
        return ContainerLocks.CHANNEL.get(slot);
    }

    @Override
    public void setLocked(Slot slot, boolean locked) {
        ContainerLocks.CHANNEL.set(slot, locked);
    }

    /**
     * Client-side: a placed simple-storage container (chest + trapped, barrel,
     * shulker, hopper, dispenser, dropper), identified by the opened block.
     * Render-thread-gated; excludes the player inventory (a real {@link Inventory}
     * even on the client) and the ender chest ({@code EnderChestBlock} is absent
     * from the list — ender is IP's client-side lock).
     */
    private static boolean isClientPlacedContainer(Slot slot) {
        if (!"Render thread".equals(Thread.currentThread().getName())) return false;
        if (slot.container instanceof Inventory) return false;
        Block b = ContainerOpenTracker.openContainerBlock();
        return b instanceof ChestBlock
                || b instanceof TrappedChestBlock
                || b instanceof BarrelBlock
                || b instanceof ShulkerBoxBlock
                || b instanceof HopperBlock
                || b instanceof DispenserBlock
                || b instanceof DropperBlock;
    }
}
