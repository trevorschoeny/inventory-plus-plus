package com.trevorschoeny.inventorymax.pocket;

import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Geometry for the +/− resize buttons, which live in their own small panel
 * <b>just below</b> the hotbar slot (separate from the pockets panel above).
 * Because that space has no vanilla slots, the buttons are reliably clickable
 * regardless of the registered-slot overlap issue affecting the pockets above.
 *
 * <p>Shared by the render helper (to draw) and the click mixin (to hit-test)
 * so the two never disagree. Coordinates are screen-absolute.
 */
public final class PocketButtons {

    private PocketButtons() {}

    /** Button size (px). */
    public static final int SIZE = 7;

    /** Top of the +/− panel, just below the hotbar slot (menu-derived hotbar y). */
    public static int panelTop(AbstractContainerMenu menu, int topPos) {
        return topPos + Pockets.hotbarItemY(menu) + Pockets.SLOT;
    }

    /** Height of the +/− panel. */
    public static int panelHeight() {
        return SIZE + 4;
    }

    private static int rowY(AbstractContainerMenu menu, int topPos) {
        return panelTop(menu, topPos) + 2;
    }

    /** {x, y, w, h} of the − button. */
    public static int[] minusRect(AbstractContainerMenu menu, int leftPos, int topPos, int hotbar) {
        int x = leftPos + Pockets.hotbarFrameX(menu, hotbar) + 1;
        return new int[] { x, rowY(menu, topPos), SIZE, SIZE };
    }

    /** {x, y, w, h} of the + button. */
    public static int[] plusRect(AbstractContainerMenu menu, int leftPos, int topPos, int hotbar) {
        int x = leftPos + Pockets.hotbarFrameX(menu, hotbar) + 1 + SIZE + 1;
        return new int[] { x, rowY(menu, topPos), SIZE, SIZE };
    }

    public static boolean inRect(double mx, double my, int[] r) {
        return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }
}
