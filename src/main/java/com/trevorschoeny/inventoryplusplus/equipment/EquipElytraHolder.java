package com.trevorschoeny.inventoryplusplus.equipment;

import net.minecraft.world.item.ItemStack;

/**
 * Duck interface mixed onto {@code HumanoidRenderState} so the equipment-slot
 * elytra can ride the render state from extraction to the wings layer. The
 * render state is the only bridge between the entity (where the equip slot lives)
 * and {@code WingsLayer.submit} (which only receives the render state) — this lets
 * the wings layer render the equip elytra using vanilla's own code, in every
 * context that renders the player (third-person, inventory preview, …).
 */
public interface EquipElytraHolder {

    /** The equipment-slot elytra captured for this frame, or EMPTY. */
    ItemStack inventoryplusplus$getEquipElytra();

    void inventoryplusplus$setEquipElytra(ItemStack stack);
}
