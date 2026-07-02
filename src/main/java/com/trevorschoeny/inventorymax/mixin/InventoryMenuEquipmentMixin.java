package com.trevorschoeny.inventorymax.mixin;

import com.trevorschoeny.inventorymax.config.IMConfig;
import com.trevorschoeny.inventorymax.equipment.EquipmentSlots;
import com.trevorschoeny.inventorymax.pocket.SliceStorage;
import com.trevorschoeny.menukit.core.MKCSlots;
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
 * The consumer-owned equipment-slot slot (§0045) — sibling to
 * {@link InventoryMenuPocketMixin}. Injects at the TAIL of
 * {@link InventoryMenu}'s constructor and slots two fixed, always-visible,
 * type-filtered slots stacked above the offhand: an elytra slot and a totem
 * slot.
 *
 * <p>Two separate slots because the slots accept different items — each needs
 * its own {@link InteractionPolicy} (and therefore its own group). Each is
 * capped to a single item via {@code withMaxStackSize(1)} (a totem otherwise
 * stacks to 64; one per slot is the intent). Content is a 1-slot window onto the
 * shared 2-slot {@link EquipmentSlots#EQUIPMENT} attachment via
 * {@link SliceStorage}, which also fires the advancement trigger on server-side
 * writes (so picking up an elytra into the slot still earns "Sky's the Limit").
 *
 * <p>Unlike pockets there is no hover reveal and no runtime reposition — the
 * slots are always visible at a fixed spot <em>while the feature is enabled</em>:
 * the reveal predicate is the Equipment Slots config toggle (side-aware, same
 * model as pockets — the server keeps the content syncing; the client hides and
 * inerts the slots when toggled off, content persists). Running at the
 * constructor TAIL means the slot re-applies on every menu rebuild (login,
 * respawn, dimension change) on both sides — no lifecycle hook.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuEquipmentMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void inventoryMax$addEquipmentSlots(Inventory inv, boolean active,
                                                  Player player, CallbackInfo ci) {
        Storage backing = EquipmentSlots.EQUIPMENT.bind(player);
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        // Elytra slot (top) — accepts only elytra, one item.
        MKCSlots.onto(menu, player)
                .panel(EquipmentSlots.MOD_ID + ":" + EquipmentSlots.ELYTRA_GROUP)
                .group(EquipmentSlots.ELYTRA_GROUP)
                .storage(new SliceStorage(backing, EquipmentSlots.ELYTRA_INDEX, 1, player))
                .layout(EquipmentSlots.SLOT_X, EquipmentSlots.ELYTRA_Y, 1)
                .revealWhen(IMConfig::equipmentSlotsEnabled)
                // Behavior-FREE creation: the accept-filter + single-item cap (GATING),
                // Curse-of-Binding (BINDING), and XP mending (MENDING) are declared by
                // the slot's address in EquipmentSlots.declareSlotBehavior().
                .register();

        // Totem slot (bottom, just above the offhand) — accepts only totems, one item.
        MKCSlots.onto(menu, player)
                .panel(EquipmentSlots.MOD_ID + ":" + EquipmentSlots.TOTEM_GROUP)
                .group(EquipmentSlots.TOTEM_GROUP)
                .storage(new SliceStorage(backing, EquipmentSlots.TOTEM_INDEX, 1, player))
                .layout(EquipmentSlots.SLOT_X, EquipmentSlots.TOTEM_Y, 1)
                .revealWhen(IMConfig::equipmentSlotsEnabled)
                // Behavior-FREE creation: GATING (totem-only + single item), BINDING,
                // and MENDING are declared by address in EquipmentSlots.declareSlotBehavior().
                .register();
    }
}
