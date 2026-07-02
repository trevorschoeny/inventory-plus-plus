package com.trevorschoeny.inventorymax.pocket;

import com.trevorschoeny.inventorymax.config.IMConfig;
import com.trevorschoeny.menukit.inject.SlotScreenRect;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Per-frame hover computation for pockets (client). Decides which hotbar
 * slot's pocket column is revealed.
 *
 * <p><b>Reveal on hover, no keybind</b> (Trev 2026-06-02): hovering any hotbar
 * slot reveals its two panels — the pockets above (however many are attached)
 * and the +/− strip below (always, even at 0 pockets, so + can attach the
 * first). A single sustain zone spans the +/− strip (below), the hotbar slot,
 * and the pockets (above), so the cursor can travel the whole stack without it
 * collapsing.
 *
 * <p><b>Driven by {@code ScreenEvents.beforeRender}</b> — the per-frame seam
 * that replaced the deleted presence layer's {@code onPrepare}: state updates
 * once per frame before the pixel panels resolve their origins (which read it).
 * All geometry is absolute screen pixels off the live hotbar slots
 * ({@link PocketGeometry}), so the same math is correct on survival and the
 * creative inventory tab. Screens whose menu doesn't carry the pockets (chests,
 * creative's non-inventory tabs — which do surface a hotbar) clear the state.
 */
public final class PocketHover {

    private PocketHover() {}

    /** Registers the per-frame hover tick. Call once at IM client init. */
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?> acs) {
                ScreenEvents.beforeRender(screen).register(
                        (s, graphics, mouseX, mouseY, tickDelta) ->
                                updateHover(acs, mouseX, mouseY));
            }
        });
    }

    private static void updateHover(AbstractContainerScreen<?> screen,
                                    double mouseX, double mouseY) {
        // Keep the server-safe counts current for the slots' reveal predicate.
        PocketState.pushAll();

        if (!IMConfig.pocketCyclerEnabled() || !PocketGeometry.pocketsPresent(screen)) {
            PocketHoverState.setRevealedHotbar(-1);
            PocketHoverState.setHoveredHotbar(-1);
            PocketHoverState.setHoveredDepth(-1);
            return;
        }

        int hovered = columnUnderCursor(screen, mouseX, mouseY);
        PocketHoverState.setHoveredHotbar(hovered);

        // Reveal on ANY hotbar hover (even 0 pockets — the +/− strip still
        // shows so + can attach). Sustain across the whole zone otherwise.
        int currently = PocketHoverState.revealedHotbar();
        int newReveal = -1;
        if (hovered >= 0) {
            newReveal = hovered;
        } else if (currently >= 0 && inSustainZone(screen, mouseX, mouseY, currently)) {
            newReveal = currently;
        }
        PocketHoverState.setRevealedHotbar(newReveal);

        // Hovered pocket depth within the revealed row (for the attach keybind).
        int hoveredDepth = -1;
        if (newReveal >= 0) {
            SlotScreenRect anchor = PocketGeometry.hotbar(screen, newReveal);
            int c = PocketState.count(newReveal);
            if (anchor != null) {
                for (int d = 0; d < c; d++) {
                    if (inBox(mouseX, mouseY, PocketGeometry.rowX(anchor, c, d),
                            PocketGeometry.rowY(anchor), 16, 16)) {
                        hoveredDepth = d;
                        break;
                    }
                }
            }
        }
        PocketHoverState.setHoveredDepth(hoveredDepth);
    }

    /**
     * The hotbar column the cursor is over — counting both the hotbar slot
     * itself AND the +/− strip below it, so hovering the strip reveals the
     * column too (Trev 2026-06-02). Geometry is resolved off the live hotbar
     * slots, so it works on whatever screen the tick fired on.
     */
    private static int columnUnderCursor(AbstractContainerScreen<?> screen,
                                         double mx, double my) {
        for (int n = 0; n < Pockets.HOTBAR_SLOTS; n++) {
            SlotScreenRect r = PocketGeometry.hotbar(screen, n);
            if (r == null) continue;
            // The hotbar slot itself.
            if (inBox(mx, my, r.frameX(), r.y(), 16, 16)) return n;
            // The +/− strip area just below it.
            if (inBox(mx, my, r.frameX() - 1, PocketGeometry.buttonsTop(r),
                    18, PocketGeometry.BUTTON_PANEL_H)) {
                return n;
            }
        }
        return -1;
    }

    /**
     * The zone that keeps a revealed column open: spans the floating horizontal
     * pocket row (above), the hotbar slot, and the +/− strip (below), so the
     * cursor can travel the whole stack without it collapsing. Horizontal extent
     * follows the row's width (it widens with the pocket count); at 0 pockets it
     * falls back to the hotbar slot column so the +/− reveal still sustains.
     */
    private static boolean inSustainZone(AbstractContainerScreen<?> screen,
                                         double mx, double my, int hotbar) {
        SlotScreenRect r = PocketGeometry.hotbar(screen, hotbar);
        if (r == null) return false;
        int c = PocketState.count(hotbar);
        int pad = 2;
        int zleft, zright, ztop;
        if (c > 0) {
            zleft = PocketGeometry.rowX(r, c, 0) - pad;
            zright = PocketGeometry.rowX(r, c, c - 1) + Pockets.SLOT + pad;
            ztop = PocketGeometry.rowY(r) - pad;
        } else {
            zleft = r.frameX() - pad;
            zright = r.frameX() + Pockets.SLOT + pad;
            ztop = r.y() - pad;
        }
        int zbot = PocketGeometry.buttonsTop(r) + PocketGeometry.BUTTON_PANEL_H + pad;
        return inBox(mx, my, zleft, ztop, zright - zleft, zbot - ztop);
    }

    private static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
