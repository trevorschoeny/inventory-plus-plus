package com.trevorschoeny.inventorymax.pocket;

import com.trevorschoeny.menukit.core.PanelRendering;
import com.trevorschoeny.menukit.core.PanelStyle;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Draws the pocket UI (client):
 * <ul>
 *   <li><b>Pockets panel</b> above the hotbar slot — a MenuKit raised panel
 *       (Trev 2026-06-02), with a generous margin around the slot column.</li>
 *   <li><b>+/− buttons</b> below the hotbar slot — two <em>floating</em> boxed
 *       buttons (no panel behind them, Trev 2026-06-02), each a bordered box
 *       with a {@code −} bar / {@code +} cross, graying out at its limit
 *       (− at 0 pockets, + at 3).</li>
 * </ul>
 *
 * <p>The registered slot frames + items are drawn by MenuKit's
 * {@code MKCSlotRender} between the background and button passes.
 */
public final class PocketPanelRender {

    private PocketPanelRender() {}

    /** Margin between the raised pockets panel edge and the slot column. */
    private static final int POCKET_PANEL_PAD = 4;

    private static final int BTN_BORDER = 0xFF373737;
    private static final int BTN_FACE = 0xFF8B8B8B;
    private static final int BTN_FACE_HOVER = 0xFFFFFFFF;
    private static final int GLYPH = 0xFF202020;
    private static final int GLYPH_DISABLED = 0xFF5A5A5A;

    /**
     * Raised pockets-panel backing — drawn BEFORE the registered slot frames. Spans
     * the floating horizontal row (centered over the hovered hotbar slot), so it
     * widens as pockets are added and stays centered.
     */
    public static void drawBackground(GuiGraphics g, AbstractContainerMenu menu, int leftPos, int topPos) {
        int rev = PocketHoverState.revealedHotbar();
        if (rev < 0) return;
        int c = PocketState.count(rev);
        if (c <= 0) return; // nothing above when no pockets

        int left = leftPos + Pockets.pocketRowX(menu, rev, c, 0);
        int right = leftPos + Pockets.pocketRowX(menu, rev, c, c - 1) + Pockets.SLOT;
        int top = topPos + Pockets.pocketRowY(menu);
        PanelRendering.renderPanel(g,
                left - POCKET_PANEL_PAD, top - POCKET_PANEL_PAD,
                (right - left) + 2 * POCKET_PANEL_PAD, Pockets.SLOT + 2 * POCKET_PANEL_PAD,
                PanelStyle.RAISED);
    }

    /**
     * Floating +/− boxed buttons below the hotbar — drawn AFTER the slot
     * frames. No panel backing (PanelStyle.NONE in spirit): the two boxes
     * float in the empty space below the hotbar.
     */
    public static void drawButtons(GuiGraphics g, AbstractContainerMenu menu, int leftPos, int topPos,
                                   double mouseX, double mouseY) {
        int rev = PocketHoverState.revealedHotbar();
        if (rev < 0) return;
        int c = PocketState.count(rev);
        // Both buttons always drawn; grayed at their limit (Trev 2026-06-02)
        // — − grayed at 0, + grayed at max.
        drawBoxButton(g, PocketButtons.minusRect(menu, leftPos, topPos, rev), false, c > 0, mouseX, mouseY);
        drawBoxButton(g, PocketButtons.plusRect(menu, leftPos, topPos, rev), true, c < Pockets.MAX_PER_SLOT, mouseX, mouseY);
    }

    private static void drawBoxButton(GuiGraphics g, int[] r, boolean plus, boolean enabled,
                                      double mouseX, double mouseY) {
        boolean hover = enabled && PocketButtons.inRect(mouseX, mouseY, r);
        int x = r[0], y = r[1], w = r[2], h = r[3];
        g.fill(x, y, x + w, y + h, BTN_BORDER);                               // border
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? BTN_FACE_HOVER : BTN_FACE); // face
        int glyph = enabled ? GLYPH : GLYPH_DISABLED;
        int cy = y + h / 2;
        int cx = x + w / 2;
        g.fill(x + 2, cy, x + w - 2, cy + 1, glyph);          // horizontal bar
        if (plus) g.fill(cx, y + 2, cx + 1, y + h - 2, glyph); // vertical bar → cross
    }
}
