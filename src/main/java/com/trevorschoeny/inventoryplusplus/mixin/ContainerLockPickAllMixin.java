package com.trevorschoeny.inventoryplusplus.mixin;

import com.trevorschoeny.inventoryplusplus.containerlocks.ContainerLocks;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server-authoritative block of double-click "collect all of type"
 * ({@code PICKUP_ALL}) from a locked container slot — the double-click
 * counterpart to {@link ContainerLockEnforcementMixin}'s shift-click block.
 *
 * <p>PICKUP_ALL never passes through {@code moveItemStackTo}, so it needs its
 * own guard: vanilla gates each candidate slot on {@code canTakeItemForPickAll},
 * and returning false for a locked container slot makes the consolidation skip
 * it. Reads the lock via the menu-free path
 * ({@link ContainerLocks#isLocked(net.minecraft.world.inventory.Slot)}) and
 * respects the non-modded bypass ({@link ContainerLocks#enforceForActingPlayer()}),
 * exactly like shift-click. The acting player is supplied by
 * {@code ContainerLockClickCaptureMixin} (the PICKUP_ALL path runs inside
 * {@code clicked}).
 */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerLockPickAllMixin {

    @Inject(method = "canTakeItemForPickAll", at = @At("HEAD"), cancellable = true)
    private void inventoryplusplus$blockPickAllFromLockedContainer(
            ItemStack stack, Slot slot, CallbackInfoReturnable<Boolean> cir) {
        if (ContainerLocks.isLocked(slot) && ContainerLocks.enforceForActingPlayer()) {
            cir.setReturnValue(false);
        }
    }
}
