package com.trevorschoeny.inventorymax.equipment;

import com.trevorschoeny.menukit.core.Storage;
import com.trevorschoeny.menukit.core.StorageAttachment;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Equipment Slots — constants, the server-persistent content storage, and the
 * per-slot accept filters. The last v1 IM feature.
 *
 * <h3>The architecture (blessed thesis, 2026-06-17)</h3>
 *
 * Two extra equipment slots — an <b>elytra</b> slot and a <b>totem</b> slot —
 * stacked directly above the vanilla offhand slot, so the player can wear an
 * elytra <em>and</em> a chestplate, or keep a totem ready <em>and</em> hold
 * something in the offhand.
 *
 * <p>These are <b>grafted menu slots backed by a player attachment</b> (the
 * proven §0045 Pocket Cycler kit), <b>not</b> new vanilla {@code EquipmentSlot}s
 * — the equipment enum can't be extended cleanly, and doing so would be fragile
 * + against §0030. The consequence: a grafted-attachment slot is invisible to
 * vanilla's equipment iteration, so it piggybacks on nothing for free. Every
 * vanilla mechanic the slots need (glide, totem, render, mending, binding,
 * death-drop) is taught to <em>also</em> consult the extra slot via one narrow,
 * vanilla-reusing injection each. This file is just the storage + filters; the
 * per-mechanism widening lives in the {@code mixin} package.
 *
 * <h3>Persistence</h3>
 *
 * A 2-slot player attachment (index {@link #ELYTRA_INDEX} / {@link #TOTEM_INDEX})
 * survives logout/restart/death (§0034), synced for free via vanilla's slot
 * protocol. Declared at common init, bound by the graft mixin.
 */
public final class EquipmentSlots {

    private EquipmentSlots() {}

    public static final String MOD_ID = "inventorymax";

    /** Backing-storage slot indices. */
    public static final int ELYTRA_INDEX = 0;
    public static final int TOTEM_INDEX = 1;
    /** Total grafted equipment slots. */
    public static final int TOTAL = 2;

    /** Unique graft panel/group ids. */
    public static final String ELYTRA_GROUP = "equip_elytra";
    public static final String TOTEM_GROUP = "equip_totem";

    // ─── Screen-relative layout (frame top-left, added to leftPos/topPos) ──
    //
    // The slots stack directly above the vanilla offhand slot. Vanilla draws
    // the offhand at item (77, 62) → its 18px frame sits at (76, 61). A grafted
    // slot's graftX/Y is its frame top-left (the render helper draws the frame
    // there and insets the 16px item by 1px), so we use frame coords here, 1px
    // up-left of the item, exactly like the Pocket column aligns to the hotbar.
    // Pixel-tunable — Trev nudged the pockets by a px; same is expected here.

    /** Frame x — aligned above the offhand frame (offhand item 77 → frame 76). */
    public static final int SLOT_X = 76;
    /** Elytra slot frame y (top of the stack). */
    public static final int ELYTRA_Y = 25;
    /** Totem slot frame y (bottom — directly above the offhand frame at 61). */
    public static final int TOTEM_Y = 43;

    // ─── Creative inventory-tab layout (item positions, screen-relative) ──
    //
    // The creative inventory tab wraps every player-inventoryMenu slot in a
    // SlotWrapper and lays them out by index; our grafts fall into the default
    // (hotbar) branch and overlay it. We re-place them to the right of the
    // leggings/boots armor column. Vanilla armor there sits at ITEM positions
    // helmet(54,6) chestplate(54,33) leggings(108,6) boots(108,33); so "right of
    // leggings/boots" is x = 108 + 18 = 126 then 144, vertically centered against
    // the two rows (y 6..33) ≈ 20. These are item positions (frame is item − 1).
    // Pixel-tunable.

    /** Creative: elytra slot item x (left of the horizontal pair). */
    public static final int CREATIVE_ELYTRA_X = 127;
    /** Creative: totem slot item x (right of the pair). */
    public static final int CREATIVE_TOTEM_X = 145;
    /** Creative: both equip slots' item y (centered against leggings/boots). */
    public static final int CREATIVE_Y = 20;

    /**
     * Player-attached equipment content — 2 slots, survives logout/restart
     * (§0034). Declared at common init; the graft slices this per slot.
     */
    public static StorageAttachment<Player, NonNullList<ItemStack>> EQUIPMENT;

    /** Common-init registration. Call from the main entrypoint. */
    public static void register() {
        EQUIPMENT = StorageAttachment.playerAttached(MOD_ID, "equipment", TOTAL);
    }

    // ─── Behavior reads ──────────────────────────────────────────────────
    // The passive-behavior mixins (glide, totem, …) read the slot contents
    // directly off the player attachment — the same storage the menu graft
    // binds, so survival/creative/menu-closed all see one source of truth.

    /** The elytra in the equipment elytra slot, or EMPTY. */
    public static ItemStack getElytra(Player player) {
        return EQUIPMENT.bind(player).getStack(ELYTRA_INDEX);
    }

    /** The totem in the equipment totem slot, or EMPTY. */
    public static ItemStack getTotem(Player player) {
        return EQUIPMENT.bind(player).getStack(TOTEM_INDEX);
    }

    /**
     * Wears the equipment elytra down by {@code amount} and writes it back.
     * The storage reads return a detached stack (the attachment rebuilds a list
     * each read), so a mutation only sticks if we {@code setStack} it back —
     * that re-saves the attachment and syncs the new durability to the client.
     * A {@code ServerLevel}-backed {@code entity} is required for the damage to
     * apply (vanilla gates {@code hurtAndBreak} on the server side).
     */
    public static void wearElytra(Player player, LivingEntity entity, int amount) {
        Storage storage = EQUIPMENT.bind(player);
        ItemStack elytra = storage.getStack(ELYTRA_INDEX);
        if (elytra.isEmpty()) return;
        elytra.hurtAndBreak(amount, entity, EquipmentSlot.CHEST);
        storage.setStack(ELYTRA_INDEX, elytra);
    }

    /**
     * Consumes one totem from the equipment totem slot and writes it back. Same
     * detached-copy caveat as {@link #wearElytra}: the read returns a copy, so the
     * shrink only sticks (and syncs to the client) if we {@code setStack} it back.
     * Called when the slot's totem fires a death save.
     */
    public static void consumeTotem(Player player) {
        Storage storage = EQUIPMENT.bind(player);
        ItemStack totem = storage.getStack(TOTEM_INDEX);
        if (totem.isEmpty()) return;
        totem.shrink(1);
        storage.setStack(TOTEM_INDEX, totem);
    }

    // ─── Per-slot accept filters ─────────────────────────────────────────
    //
    // Spec: the elytra slot accepts only elytra; the totem slot accepts only
    // totems of undying. Literal item checks (not the broader GLIDER /
    // DEATH_PROTECTION component) to honor the spec exactly — broadening to
    // "any glider / any death-protection item" is a one-line change if desired.

    /** True for an elytra (the only item the elytra slot accepts). */
    public static boolean isElytra(ItemStack stack) {
        return stack.is(Items.ELYTRA);
    }

    /** True for a totem of undying (the only item the totem slot accepts). */
    public static boolean isTotem(ItemStack stack) {
        return stack.is(Items.TOTEM_OF_UNDYING);
    }
}
