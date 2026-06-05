package com.trevorschoeny.inventoryplusplus.pocket;

import com.trevorschoeny.inventoryplus.columncycler.hud.HudMode;
import com.trevorschoeny.inventoryplus.cyclable.CycleHudRegistry;
import com.trevorschoeny.inventoryplus.cyclable.CycleHudSource;
import com.trevorschoeny.inventoryplus.cyclable.CycleView;
import com.trevorschoeny.inventoryplusplus.config.IPPConfig;
import com.trevorschoeny.menukit.core.Storage;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Pocket Cycler's contribution to the shared cycle HUD. Registered into IP's
 * {@link CycleHudRegistry} so the one Mini-hotbar HUD draws the pocket cycle
 * when the player is on a pocket-bearing hotbar slot — the HUD generalization
 * paying off (the same HUD serves Column and Pocket cyclers).
 *
 * <h3>Reading content client-side</h3>
 *
 * The pocket items are read from the player's pocket attachment on the client.
 * Even in-world (where the client's pocket <i>slots</i> are inert/hidden), the
 * server's pocket slots are always active, so vanilla slot sync keeps the
 * client-side attachment populated — so reading the attachment directly gives
 * the live content regardless of reveal state.
 */
public final class PocketCyclerHudSource implements CycleHudSource {

    public static final PocketCyclerHudSource INSTANCE = new PocketCyclerHudSource();

    private PocketCyclerHudSource() {}

    /** Register this source with the shared HUD registry. Call at client init. */
    public static void register() {
        CycleHudRegistry.register(INSTANCE);
    }

    @Override
    public CycleView cycleViewForHotbar(int hotbarSlot) {
        if (!IPPConfig.pocketCyclerEnabled()) return null;
        if (IPPConfig.pocketHudMode() != HudMode.MINI_HOTBAR) return null;

        int count = PocketState.count(hotbarSlot);
        if (count < 1) return null; // need ≥1 pocket + hotbar = 2 ring members

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;

        Storage pockets = Pockets.POCKETS.bind(mc.player);
        // Visual top→bottom: pocket(count-1) … pocket0, then the hotbar item.
        List<ItemStack> visual = new ArrayList<>(count + 1);
        for (int depth = count - 1; depth >= 0; depth--) {
            visual.add(pockets.getStack(Pockets.flatIndex(hotbarSlot, depth)));
        }
        visual.add(mc.player.getInventory().getItem(hotbarSlot));
        return CycleView.fromVisualOrder(visual);
    }

    /**
     * Pocket Cycler pins to the BOTTOM of the stacked HUD when it shares a
     * hotbar slot with another cycler (Trev 2026-06-04) — the lowest stack
     * order takes the bottom (nearest-the-hotbar) strip.
     */
    @Override
    public int hudStackOrder() {
        return 0;
    }
}
