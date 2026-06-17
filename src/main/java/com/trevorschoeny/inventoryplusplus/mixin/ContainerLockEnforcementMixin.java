package com.trevorschoeny.inventoryplusplus.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import com.trevorschoeny.inventoryplusplus.containerlocks.ContainerLocks;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The <b>sole server-side authority</b> for shift-click into locked container
 * slots (§0005, §0049, §0050).
 *
 * <p>IP's own {@code moveItemStackTo} wrap predicts the block on the client
 * render thread, but IP deliberately stops there for container slots — its
 * unified check returns "unlocked" off the render thread — so that server-side
 * enforcement runs here alone, where the non-modded bypass can be applied. This
 * universal mixin makes vanilla {@code moveItemStackTo} treat a locked container
 * slot as permanently full/unplaceable, reading the shared channel via
 * {@link ContainerLocks#isLocked(net.minecraft.world.inventory.Slot)} — touching
 * no IP class, so it loads on a dedicated server (where IP is absent).
 *
 * <h3>Two axes</h3>
 *
 * <ul>
 *   <li><b>Container scope</b> — {@link ContainerLocks#isLocked} returns false
 *       for the player-inventory portion of the menu (those wrap a real
 *       {@code Inventory}, not a placed-container type), so only the container's
 *       own slots are protected.</li>
 *   <li><b>Non-modded bypass</b> — {@link ContainerLocks#enforceForActingPlayer()}
 *       skips a player whose client can't see the lock (§0050 capability check).
 *       The acting player is supplied by {@code ContainerLockClickCaptureMixin}
 *       (moveItemStackTo carries none). Automation has no player and never
 *       reaches this mixin — machines always respect locks.</li>
 * </ul>
 *
 * <h3>Composition with IP's render-thread wrap</h3>
 *
 * In single-player / on a LAN host both this mixin and IP's wrap exist in the
 * same JVM; the two {@code @WrapOperation}s on the same {@code getItem} /
 * {@code mayPlace} call chain via MixinExtras. They never both fire for a
 * container slot on the same thread (IP's is render-thread-only for containers,
 * this one bites on the server thread), so there is no double-block.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerLockEnforcementMixin {

    @WrapOperation(
            method = "moveItemStackTo",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack inventoryplusplus$treatLockedContainerAsEmpty(Slot slot,
                                                                    Operation<ItemStack> original) {
        // Merge pass sees empty → nothing to merge into → skip. Empty pass sees
        // empty → falls through to mayPlace, which the wrap below rejects.
        if (ContainerLocks.isLocked(slot) && ContainerLocks.enforceForActingPlayer()) {
            return ItemStack.EMPTY;
        }
        return original.call(slot);
    }

    @WrapOperation(
            method = "moveItemStackTo",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;mayPlace(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean inventoryplusplus$blockLockedContainerDestination(Slot slot, ItemStack stack,
                                                                      Operation<Boolean> original) {
        if (ContainerLocks.isLocked(slot) && ContainerLocks.enforceForActingPlayer()) {
            return false;
        }
        return original.call(slot, stack);
    }
}
