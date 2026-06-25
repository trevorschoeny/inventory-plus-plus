package com.trevorschoeny.inventorymax.mixin;

import com.trevorschoeny.inventorymax.pocket.PocketHoverState;
import com.trevorschoeny.inventorymax.pocket.Pockets;
import com.trevorschoeny.inventorymax.pocket.SliceStorage;
import com.trevorschoeny.menukit.core.MenuKitGraft;
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
 * The consumer-owned pocket graft (§0045). Injects at the TAIL of
 * {@link InventoryMenu}'s constructor and grafts the maximum pocket set —
 * a 1-slot graft for every (hotbar slot, depth) pair, 27 in all.
 *
 * <p>Each graft is its own panel with its own reveal predicate keyed to
 * {@link PocketHoverState#isRevealed(int, int)}, which gives per-depth reveal
 * granularity (the panel-level reveal API alone can't show 1-of-3). All 27
 * slots always exist server-side (so content syncs + persists); the client
 * reveals only the configured count per hotbar slot.
 *
 * <p>Running at the constructor TAIL means the graft re-applies on every menu
 * rebuild (login, respawn, dimension change) on both sides — no lifecycle hook.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuPocketGraftMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void inventoryMax$graftPockets(Inventory inv, boolean active,
                                                 Player player, CallbackInfo ci) {
        Storage backing = Pockets.POCKETS.bind(player);
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        for (int n = 0; n < Pockets.HOTBAR_SLOTS; n++) {
            for (int d = 0; d < Pockets.MAX_PER_SLOT; d++) {
                final int hotbar = n;
                final int depth = d;
                // 1-slot window onto the shared 27-slot attachment. The slice
                // also fires the advancement trigger on server-side writes.
                Storage slice = new SliceStorage(
                        backing, Pockets.flatIndex(n, d), 1, player);
                MenuKitGraft.onto(menu, player)
                        .panel("inventorymax:" + Pockets.groupId(n, d))
                        .group(Pockets.groupId(n, d))
                        .storage(slice)
                        .layout(Pockets.pocketX(n), Pockets.pocketY(d), 1)
                        // Server-safe predicate (no client imports); only
                        // evaluated client-side by MenuKit.
                        .revealWhen(() -> PocketHoverState.isRevealed(hotbar, depth))
                        // Ambient mending: a damaged Mending item stored in any
                        // pocket repairs from XP orbs. Server-side all pockets are
                        // active, so this works regardless of client reveal state.
                        .mendsFromXp()
                        .graft();
            }
        }
    }
}
