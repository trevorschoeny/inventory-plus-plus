package com.trevorschoeny.inventoryplusplus.mixin;

import com.trevorschoeny.inventoryplusplus.equipment.EquipmentSlots;
import com.trevorschoeny.menukit.core.MenuKitSlot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shift-click (quick-move) routing for the equipment slots — the consumer work
 * the graft kit leaves to us (MenuKit's {@code quickMoveStack} knows nothing of
 * grafted slots). Universal: this runs server-side for the authoritative move
 * and client-side for prediction; it also covers <b>creative</b>, where the
 * inventory tab funnels shift-clicks through {@code inventoryMenu.clicked(...)}
 * → {@code quickMoveStack} just like survival.
 *
 * <p>Rules (Trev, 2026-06-17), all gated on the equipment slots being present:
 * <ul>
 *   <li><b>Elytra → dedicated elytra slot, never the chestplate slot.</b> A
 *       shift-clicked elytra goes to the equipment elytra slot first; if that's
 *       full it falls back to the normal main↔hotbar shuffle — it must <em>never</em>
 *       land in the vanilla chest-armor slot (which vanilla would otherwise do,
 *       since an elytra reports {@code EquipmentSlot.CHEST}).</li>
 *   <li><b>Totem → dedicated totem slot first</b> (if empty), else vanilla.</li>
 *   <li><b>Out of an equipment slot → the inventory</b> (main + hotbar),
 *       skipping armor/offhand.</li>
 * </ul>
 *
 * <p>We HEAD-inject and, for the cases we own, fully perform the move +
 * vanilla's exact tail bookkeeping ({@code setByPlayer}/{@code setChanged}/
 * count-compare/{@code onTake}) and cancel; everything else falls through to
 * vanilla untouched.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuQuickMoveMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void inventoryplusplus$equipQuickMove(Player player, int index,
                                                  CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        Slot from = menu.slots.get(index);
        if (from == null || !from.hasItem()) return; // let vanilla return EMPTY

        int elytraIdx = inventoryplusplus$findEquip(menu, EquipmentSlots.ELYTRA_GROUP);
        int totemIdx = inventoryplusplus$findEquip(menu, EquipmentSlots.TOTEM_GROUP);
        if (elytraIdx < 0 || totemIdx < 0) return; // equipment slots absent → vanilla

        ItemStack live = from.getItem(); // the live stack moveItemStackTo mutates

        // ── OUT of an equipment slot → main + hotbar (skip armor/offhand) ──
        if (index == elytraIdx || index == totemIdx) {
            cir.setReturnValue(inventoryplusplus$moveAndFinish(player, from, live, 9, 45, false));
            return;
        }

        // ── An elytra anywhere else → dedicated elytra slot; NEVER the chestplate ──
        if (EquipmentSlots.isElytra(live)) {
            if (!menu.slots.get(elytraIdx).hasItem()) {
                cir.setReturnValue(inventoryplusplus$moveAndFinish(
                        player, from, live, elytraIdx, elytraIdx + 1, false));
                return;
            }
            // Elytra slot occupied → normal inventory shuffle, which by
            // construction never touches the chest-armor slot.
            int[] dest = inventoryplusplus$inventoryShuffle(index);
            cir.setReturnValue(inventoryplusplus$moveAndFinish(
                    player, from, live, dest[0], dest[1], false));
            return;
        }

        // ── A totem from a non-equipment slot → dedicated totem slot if empty ──
        if (EquipmentSlots.isTotem(live) && !menu.slots.get(totemIdx).hasItem()) {
            cir.setReturnValue(inventoryplusplus$moveAndFinish(
                    player, from, live, totemIdx, totemIdx + 1, false));
            return;
        }

        // Anything else (chestplates, ordinary items, a 2nd totem) → vanilla.
    }

    /** Menu index of the grafted equipment slot in {@code groupId}, or -1. */
    private static int inventoryplusplus$findEquip(AbstractContainerMenu menu, String groupId) {
        for (int k = 0; k < menu.slots.size(); k++) {
            if (menu.slots.get(k) instanceof MenuKitSlot mk && groupId.equals(mk.getGroupId())) {
                return k;
            }
        }
        return -1;
    }

    /** Vanilla's non-equippable destination: main→hotbar, hotbar→main, else whole inventory. */
    private static int[] inventoryplusplus$inventoryShuffle(int index) {
        if (index >= 9 && index < 36) return new int[]{36, 45};  // main → hotbar
        if (index >= 36 && index < 45) return new int[]{9, 36};  // hotbar → main
        return new int[]{9, 45};                                 // armor/offhand → inventory
    }

    /**
     * Performs the move and replicates vanilla {@code quickMoveStack}'s tail
     * bookkeeping exactly. Returns the original stack (what was moved) or EMPTY
     * when nothing moved — matching vanilla's contract.
     */
    private ItemStack inventoryplusplus$moveAndFinish(Player player, Slot from, ItemStack live,
                                                      int start, int end, boolean reverse) {
        ItemStack original = live.copy();
        boolean moved = ((AbstractContainerMenuMoveInvoker) (Object) this)
                .inventoryplusplus$moveItemStackTo(live, start, end, reverse);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (live.isEmpty()) {
            from.setByPlayer(ItemStack.EMPTY, original);
        } else {
            from.setChanged();
        }
        if (live.getCount() == original.getCount()) {
            return ItemStack.EMPTY; // nothing actually moved
        }
        from.onTake(player, live);
        return original;
    }
}
