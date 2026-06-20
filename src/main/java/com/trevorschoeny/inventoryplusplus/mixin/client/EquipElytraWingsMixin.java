package com.trevorschoeny.inventoryplusplus.mixin.client;

import com.trevorschoeny.inventoryplusplus.equipment.EquipElytraHolder;

import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Renders the equipment-slot elytra's wings through vanilla's own wings layer.
 *
 * <p>{@code WingsLayer.submit} reads {@code chestEquipment} and renders the wings
 * if that item carries a wings equipment asset (an elytra does; a chestplate
 * doesn't — which is why chestplate + elytra shows neither today). We redirect
 * that single field read: when the chest item isn't itself a glider but the
 * equipment slot holds one, hand the layer our elytra instead. Vanilla's exact
 * model/texture/animation code then draws it — and because this only touches the
 * wings layer (the body armor layer reads {@code chestEquipment} elsewhere), the
 * chestplate still renders on the body. Both show.
 *
 * <p>The two-elytra rule rides along for free: if the chest item <em>is</em> a
 * glider, vanilla already renders it and we leave the read untouched.
 */
@Mixin(WingsLayer.class)
public class EquipElytraWingsMixin {

    @Redirect(
            method = "submit",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;chestEquipment:Lnet/minecraft/world/item/ItemStack;",
                    opcode = Opcodes.GETFIELD))
    private ItemStack inventoryplusplus$wingsFromEquipSlot(HumanoidRenderState state) {
        ItemStack chest = state.chestEquipment;
        ItemStack equip = ((EquipElytraHolder) state).inventoryplusplus$getEquipElytra();
        if (!chest.has(DataComponents.GLIDER) && equip.has(DataComponents.GLIDER)) {
            return equip; // chest isn't a glider but our slot is → render our wings
        }
        return chest; // vanilla behavior (incl. a worn elytra winning the two-elytra rule)
    }
}
