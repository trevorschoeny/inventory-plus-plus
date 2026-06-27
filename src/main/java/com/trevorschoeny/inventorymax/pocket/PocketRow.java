package com.trevorschoeny.inventorymax.pocket;

import com.trevorschoeny.menukit.core.MKCSlot;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Drives the §0047 runtime reposition of the revealed pocket slots into the
 * floating horizontal row centered above the hovered hotbar slot.
 *
 * <p>RegisteredSlots slots are laid out vertically at construction (the slot can't know
 * the per-world count or which slot is hovered); §0047 makes their position
 * mutable presentation. So each client render frame — <em>before</em> the
 * registered slots draw and before input hit-tests them — we move the revealed
 * hotbar's first {@code count} pockets onto {@link Pockets#pocketRowX} /
 * {@link Pockets#pocketRowY}. Render and clicks both read {@code renderX/renderY},
 * so both follow the live row.
 *
 * <p>Only the revealed pockets are moved; the rest stay inert (§0021) and
 * invisible, so their stale (vertical) positions never show. Nothing to reset
 * when the hover moves on — the previously-revealed column simply goes inert.
 */
public final class PocketRow {

    private PocketRow() {}

    /**
     * Position the revealed hotbar's pockets into the centered horizontal row.
     * {@code screenMenu} is the current screen's menu — its hotbar slots give the
     * row geometry (creative-aware). The slot {@link MKCSlot}s themselves
     * live on the player's {@code inventoryMenu}; on the creative screen they're
     * wrapped, so the screen menu doesn't surface them directly.
     */
    public static void reposition(AbstractContainerMenu screenMenu) {
        int rev = PocketHoverState.revealedHotbar();
        if (rev < 0) return;
        int count = PocketHoverState.count(rev);
        if (count <= 0) return; // 0 pockets → nothing above the slot to place

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        int rowY = Pockets.pocketRowY(screenMenu);
        for (Slot slot : player.inventoryMenu.slots) {
            if (!(slot instanceof MKCSlot mk)) continue;
            // Match this 1-slot slot to one of the revealed (rev, depth) pockets
            // by its group id, then place it at depth's spot in the centered row.
            for (int depth = 0; depth < count; depth++) {
                if (Pockets.groupId(rev, depth).equals(mk.getGroupId())) {
                    mk.setRenderPosition(Pockets.pocketRowX(screenMenu, rev, count, depth), rowY);
                    break;
                }
            }
        }
    }
}
