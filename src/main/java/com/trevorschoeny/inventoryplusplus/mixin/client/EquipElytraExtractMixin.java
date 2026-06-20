package com.trevorschoeny.inventoryplusplus.mixin.client;

import com.trevorschoeny.inventoryplusplus.equipment.EquipElytraHolder;
import com.trevorschoeny.inventoryplusplus.equipment.EquipmentSlots;

import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the equipment-slot elytra into the render state alongside vanilla's
 * own equipment extraction. {@code extractHumanoidRenderState} is the shared
 * static both the mob and player renderers route through (AvatarRenderer calls
 * it), so stashing here covers every render context. Players only; non-humanoids
 * keep EMPTY.
 */
@Mixin(HumanoidMobRenderer.class)
public class EquipElytraExtractMixin {

    @Inject(method = "extractHumanoidRenderState", at = @At("TAIL"))
    private static void inventoryplusplus$stashEquipElytra(LivingEntity livingEntity,
                                                           HumanoidRenderState state, float partialTick,
                                                           ItemModelResolver resolver, CallbackInfo ci) {
        ItemStack elytra = livingEntity instanceof Player player
                ? EquipmentSlots.getElytra(player).copy()
                : ItemStack.EMPTY;
        ((EquipElytraHolder) state).inventoryplusplus$setEquipElytra(elytra);
    }
}
