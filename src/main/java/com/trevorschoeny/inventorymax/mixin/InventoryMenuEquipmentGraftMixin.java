package com.trevorschoeny.inventorymax.mixin;

import com.trevorschoeny.inventorymax.equipment.EquipmentSlots;
import com.trevorschoeny.inventorymax.pocket.SliceStorage;
import com.trevorschoeny.menukit.core.InteractionPolicy;
import com.trevorschoeny.menukit.core.MenuKitGraft;
import com.trevorschoeny.menukit.core.Storage;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The consumer-owned equipment-slot graft (§0045) — sibling to
 * {@link InventoryMenuPocketGraftMixin}. Injects at the TAIL of
 * {@link InventoryMenu}'s constructor and grafts two fixed, always-visible,
 * type-filtered slots stacked above the offhand: an elytra slot and a totem
 * slot.
 *
 * <p>Two separate grafts because the slots accept different items — each needs
 * its own {@link InteractionPolicy} (and therefore its own group). Each is
 * capped to a single item via {@code withMaxStackSize(1)} (a totem otherwise
 * stacks to 64; one per slot is the intent). Content is a 1-slot window onto the
 * shared 2-slot {@link EquipmentSlots#EQUIPMENT} attachment via
 * {@link SliceStorage}, which also fires the advancement trigger on server-side
 * writes (so picking up an elytra into the slot still earns "Sky's the Limit").
 *
 * <p>Unlike pockets there is no reveal predicate and no runtime reposition — the
 * slots are always visible at a fixed spot. Running at the constructor TAIL means
 * the graft re-applies on every menu rebuild (login, respawn, dimension change)
 * on both sides — no lifecycle hook.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuEquipmentGraftMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void inventoryMax$graftEquipment(Inventory inv, boolean active,
                                                  Player player, CallbackInfo ci) {
        Storage backing = EquipmentSlots.EQUIPMENT.bind(player);
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        // Elytra slot (top) — accepts only elytra, one item.
        MenuKitGraft.onto(menu, player)
                .panel(EquipmentSlots.MOD_ID + ":" + EquipmentSlots.ELYTRA_GROUP)
                .group(EquipmentSlots.ELYTRA_GROUP)
                .storage(new SliceStorage(backing, EquipmentSlots.ELYTRA_INDEX, 1, player))
                .policy(InteractionPolicy.input(EquipmentSlots::isElytra)
                        .withMaxStackSize(stack -> 1))
                .layout(EquipmentSlots.SLOT_X, EquipmentSlots.ELYTRA_Y, 1)
                // Curse of Binding: a cursed elytra can't be removed while alive
                // (survival only; creative + death are handled by the library /
                // §0052). The §0053 MKC primitive — equipment-semantic opt-in.
                .bindsCursedItems()
                // Mending: a damaged Mending elytra here repairs from XP orbs
                // like worn armor — the MKC mending primitive's per-graft opt-in.
                .mendsFromXp()
                .graft();

        // Totem slot (bottom, just above the offhand) — accepts only totems, one item.
        MenuKitGraft.onto(menu, player)
                .panel(EquipmentSlots.MOD_ID + ":" + EquipmentSlots.TOTEM_GROUP)
                .group(EquipmentSlots.TOTEM_GROUP)
                .storage(new SliceStorage(backing, EquipmentSlots.TOTEM_INDEX, 1, player))
                .policy(InteractionPolicy.input(EquipmentSlots::isTotem)
                        .withMaxStackSize(stack -> 1))
                .layout(EquipmentSlots.SLOT_X, EquipmentSlots.TOTEM_Y, 1)
                // Curse of Binding: a cursed totem (uncommon, but possible via
                // the component) is likewise bound to its slot while alive.
                .bindsCursedItems()
                // Mending: opted in for consistency. Totems aren't damageable, so
                // none will ever qualify (mendable = damaged + Mending) — harmless,
                // and future-proofs a modded damageable totem-like item.
                .mendsFromXp()
                .graft();
    }
}
