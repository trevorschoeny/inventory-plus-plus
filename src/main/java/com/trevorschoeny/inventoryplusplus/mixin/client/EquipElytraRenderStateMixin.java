package com.trevorschoeny.inventoryplusplus.mixin.client;

import com.trevorschoeny.inventoryplusplus.equipment.EquipElytraHolder;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds the per-frame equipment-elytra slot to {@code HumanoidRenderState} (and
 * thus {@code AvatarRenderState}, which extends it) so it travels from extraction
 * to {@link com.trevorschoeny.inventoryplusplus.mixin.client.EquipElytraWingsMixin
 * the wings layer}. See {@link EquipElytraHolder}.
 */
@Mixin(HumanoidRenderState.class)
public class EquipElytraRenderStateMixin implements EquipElytraHolder {

    @Unique
    private ItemStack inventoryplusplus$equipElytra = ItemStack.EMPTY;

    @Override
    public ItemStack inventoryplusplus$getEquipElytra() {
        return this.inventoryplusplus$equipElytra;
    }

    @Override
    public void inventoryplusplus$setEquipElytra(ItemStack stack) {
        this.inventoryplusplus$equipElytra = stack;
    }
}
