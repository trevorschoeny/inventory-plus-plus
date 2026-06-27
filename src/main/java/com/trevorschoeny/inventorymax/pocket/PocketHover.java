package com.trevorschoeny.inventorymax.pocket;

import com.trevorschoeny.inventorymax.config.IMConfig;

import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Per-frame hover computation for pockets (client). Decides which hotbar
 * slot's panels are revealed.
 *
 * <p><b>Reveal on hover, no keybind</b> (Trev 2026-06-02): hovering any hotbar
 * slot reveals its two panels — the pockets above (however many are attached)
 * and the +/− panel below (always, even at 0 pockets, so + can attach the
 * first). A single sustain zone spans the +/− panel (below), the hotbar slot,
 * and the pockets (above), so the cursor can travel the whole stack without it
 * collapsing.
 */
public final class PocketHover {

    private PocketHover() {}

    public static void updateHover(AbstractContainerMenu menu, int leftPos, int topPos,
                                   double mouseX, double mouseY) {
        // Keep the server-safe counts current for the slot reveal predicate.
        PocketState.pushAll();

        if (!IMConfig.pocketCyclerEnabled()) {
            PocketHoverState.setRevealedHotbar(-1);
            PocketHoverState.setHoveredHotbar(-1);
            PocketHoverState.setHoveredDepth(-1);
            return;
        }

        int hovered = columnUnderCursor(menu, leftPos, topPos, mouseX, mouseY);
        PocketHoverState.setHoveredHotbar(hovered);

        // Reveal on ANY hotbar hover (even 0 pockets — the +/− panel still
        // shows so + can attach). Sustain across the whole zone otherwise.
        int currently = PocketHoverState.revealedHotbar();
        int newReveal = -1;
        if (hovered >= 0) {
            newReveal = hovered;
        } else if (currently >= 0 && inSustainZone(menu, leftPos, topPos, mouseX, mouseY, currently)) {
            newReveal = currently;
        }
        PocketHoverState.setRevealedHotbar(newReveal);

        // Hovered pocket depth within the revealed row (for future use).
        int hoveredDepth = -1;
        if (newReveal >= 0) {
            int c = PocketState.count(newReveal);
            for (int d = 0; d < c; d++) {
                if (inBox(mouseX, mouseY,
                        leftPos + Pockets.pocketRowX(menu, newReveal, c, d),
                        topPos + Pockets.pocketRowY(menu), 16, 16)) {
                    hoveredDepth = d;
                    break;
                }
            }
        }
        PocketHoverState.setHoveredDepth(hoveredDepth);
    }

    /**
     * The hotbar column the cursor is over — counting both the hotbar slot
     * itself AND the +/− button area below it, so hovering the button area
     * reveals the column too (Trev 2026-06-02). Geometry is menu-derived so it
     * works on whatever screen the dispatch fired on.
     */
    private static int columnUnderCursor(AbstractContainerMenu menu, int leftPos, int topPos,
                                         double mx, double my) {
        for (int n = 0; n < Pockets.HOTBAR_SLOTS; n++) {
            int x = leftPos + Pockets.hotbarFrameX(menu, n);
            // The hotbar slot itself.
            if (inBox(mx, my, x, topPos + Pockets.hotbarItemY(menu), 16, 16)) return n;
            // The +/− button area just below it.
            if (inBox(mx, my, x - 1, PocketButtons.panelTop(menu, topPos), 18, PocketButtons.panelHeight())) {
                return n;
            }
        }
        return -1;
    }

    /**
     * The zone that keeps a revealed column open: spans the floating horizontal
     * pocket row (above), the hotbar slot, and the +/− panel (below), so the
     * cursor can travel the whole stack without it collapsing. Horizontal extent
     * follows the row's width (it widens with the pocket count); at 0 pockets it
     * falls back to the hotbar slot column so the +/− reveal still sustains.
     */
    private static boolean inSustainZone(AbstractContainerMenu menu, int leftPos, int topPos,
                                         double mx, double my, int hotbar) {
        int c = PocketState.count(hotbar);
        int pad = 2;
        int zleft, zright, ztop;
        if (c > 0) {
            zleft = leftPos + Pockets.pocketRowX(menu, hotbar, c, 0) - pad;
            zright = leftPos + Pockets.pocketRowX(menu, hotbar, c, c - 1) + Pockets.SLOT + pad;
            ztop = topPos + Pockets.pocketRowY(menu) - pad;
        } else {
            zleft = leftPos + Pockets.hotbarFrameX(menu, hotbar) - pad;
            zright = leftPos + Pockets.hotbarFrameX(menu, hotbar) + Pockets.SLOT + pad;
            ztop = topPos + Pockets.hotbarItemY(menu) - pad;
        }
        int zbot = PocketButtons.panelTop(menu, topPos) + PocketButtons.panelHeight() + pad;
        return inBox(mx, my, zleft, ztop, zright - zleft, zbot - ztop);
    }

    private static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
