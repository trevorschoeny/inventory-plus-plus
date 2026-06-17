package com.trevorschoeny.inventoryplusplus.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import com.trevorschoeny.inventoryplusplus.containerlocks.ContainerLocks;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocked automation (§0050) — hoppers and droppers treat a locked container
 * slot as untouchable: they neither pull from it nor push into it.
 *
 * <p>Reads the SHARED lock with the menu-free §0050 accessor
 * ({@link ContainerLocks#isLocked(Container, int)}), since automation touches a
 * container by index with no {@code Slot} and no open menu. Server-only by
 * nature (vanilla hopper/dropper ticks run on the server). <b>No capability
 * gate</b> — a machine has no player; machines always respect locks (the modded
 * base-builder is the one who set them).
 *
 * <p>Two per-slot transfer points cover every automation case — and both target
 * vanilla's private static helpers, so a double chest resolves to the owning
 * half automatically (§0050):
 * <ul>
 *   <li><b>{@code tryTakeInItemFromSlot}</b> — a hopper extracting from a source
 *       slot. A locked source slot reports "nothing taken" ({@code false}), so
 *       the hopper moves to the next slot.</li>
 *   <li><b>{@code tryMoveInItem}</b> — a hopper <i>or dropper</i> inserting into
 *       a destination slot (droppers route their push through this same helper).
 *       A locked destination slot reports "nothing moved" (the input stack
 *       returned unchanged), so vanilla's loop skips it like a full slot.</li>
 * </ul>
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperLockEnforcementMixin {

    @Inject(method = "tryTakeInItemFromSlot", at = @At("HEAD"), cancellable = true)
    private static void inventoryplusplus$blockExtractFromLocked(
            Hopper hopper, Container container, int slot, Direction direction,
            CallbackInfoReturnable<Boolean> cir) {
        if (ContainerLocks.isLocked(container, slot)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tryMoveInItem", at = @At("HEAD"), cancellable = true)
    private static void inventoryplusplus$blockInsertIntoLocked(
            Container source, Container destination, ItemStack stack, int slot, Direction direction,
            CallbackInfoReturnable<ItemStack> cir) {
        if (ContainerLocks.isLocked(destination, slot)) {
            cir.setReturnValue(stack);
        }
    }

    /**
     * A hopper ejecting its <b>own</b> locked slot — the "automation out of a
     * hopper" case. Vanilla's {@code ejectItems} reads each of its own slots via
     * {@code getItem(i)} before pushing it out; we treat a locked slot as empty
     * there, so the eject loop skips it (its unlocked slots still push normally).
     * This sits alongside the destination guard above ({@code tryMoveInItem})
     * which protects the slot the item lands in.
     */
    @WrapOperation(
            method = "ejectItems",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getItem(I)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack inventoryplusplus$skipLockedHopperEjectSlot(
            HopperBlockEntity hopper, int slot, Operation<ItemStack> original) {
        if (ContainerLocks.isLocked(hopper, slot)) {
            return ItemStack.EMPTY;
        }
        return original.call(hopper, slot);
    }
}
