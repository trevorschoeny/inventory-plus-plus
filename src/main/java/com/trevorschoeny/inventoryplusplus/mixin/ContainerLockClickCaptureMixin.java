package com.trevorschoeny.inventoryplusplus.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import com.trevorschoeny.inventoryplusplus.containerlocks.ContainerLocks;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Captures the acting player for the span of an {@code AbstractContainerMenu}
 * click, so {@link ContainerLockEnforcementMixin} — which wraps the player-less
 * {@code moveItemStackTo} (nested under {@code clicked} → {@code quickMoveStack})
 * — can read it for the §0050 non-modded capability gate
 * ({@link ContainerLocks#enforceForActingPlayer()}).
 *
 * <p>Uses {@code @WrapMethod} with a {@code try/finally} (rather than HEAD/RETURN
 * injections) so the capture is <b>always</b> cleared — including if {@code clicked}
 * throws. A RETURN injection would skip the clear on an exceptional exit, leaving
 * a stale {@link Player}; a later player-less {@code moveItemStackTo} could then
 * read that stale player and mis-apply (or mis-skip) the lock.
 *
 * <p>Runs on both sides; only the server captures a {@code ServerPlayer}, which is
 * the only case the capability gate acts on. A programmatic {@code moveItemStackTo}
 * outside any click sees no captured player and enforces by default.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerLockClickCaptureMixin {

    @WrapMethod(method = "clicked")
    private void inventoryplusplus$captureActingPlayer(
            int slotId, int button, ClickType clickType, Player player, Operation<Void> original) {
        ContainerLocks.setActingPlayer(player);
        try {
            original.call(slotId, button, clickType, player);
        } finally {
            ContainerLocks.clearActingPlayer();
        }
    }
}
