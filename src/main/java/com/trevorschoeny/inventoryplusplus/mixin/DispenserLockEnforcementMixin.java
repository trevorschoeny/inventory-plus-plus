package com.trevorschoeny.inventoryplusplus.mixin;

import com.trevorschoeny.inventoryplusplus.containerlocks.ContainerLocks;

import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocked automation — dispense-source half (§0050). Stops a dispenser or dropper
 * from firing its <b>own</b> locked slot.
 *
 * <p>The hopper mixin guards transfers <i>between</i> containers, but a
 * dispenser/dropper ejecting its own contents goes through a different path: both
 * pick which slot to fire via {@link DispenserBlockEntity}'s shared
 * {@code getRandomSlot} (a dropper inherits it). We post-process that pick — if
 * the chosen slot is locked, return {@code -1}, which vanilla already handles as
 * "nothing to dispense" (a harmless fail-click, no eject).
 *
 * <p>Robust by construction: it only ever turns a locked pick into the existing
 * empty-dispenser no-op, so it can't misfire or break vanilla redstone. The cost
 * is throughput — a dispenser with some locked slots occasionally skips a tick
 * when it randomly selects a locked one; its locked items are never ejected and
 * its unlocked items still fire. No capability gate (a machine has no player;
 * machines always respect locks). A double chest can't be a dispenser, so the
 * {@code CompoundContainer} case never arises here.
 */
@Mixin(DispenserBlockEntity.class)
public abstract class DispenserLockEnforcementMixin {

    @Inject(method = "getRandomSlot", at = @At("RETURN"), cancellable = true)
    private void inventoryplusplus$skipLockedDispenseSlot(
            RandomSource random, CallbackInfoReturnable<Integer> cir) {
        int slot = cir.getReturnValueI();
        if (slot >= 0 && ContainerLocks.isLocked((Container) (Object) this, slot)) {
            cir.setReturnValue(-1);
        }
    }
}
