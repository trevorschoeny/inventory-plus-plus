package com.trevorschoeny.inventorymax.pocket;

import com.trevorschoeny.menukit.core.MKCSlot;
import com.trevorschoeny.menukit.core.MKCSlotAccess;
import com.trevorschoeny.menukit.inject.SlotScreenRect;
import com.trevorschoeny.menukit.inject.VanillaSlotResolver;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import org.jspecify.annotations.Nullable;

/**
 * Screen-space pocket geometry (client-only). Every coordinate here is an
 * <b>absolute screen pixel</b>, derived per call from the live hotbar slot
 * positions via MenuKit's {@link VanillaSlotResolver} — the same coordinate
 * space the pixel-panel origin suppliers and element input dispatch speak.
 * That one shared space is what makes the geometry correct on any screen that
 * surfaces the player inventory (survival + the creative inventory tab, the
 * creative {@code SlotWrapper} unwrapped by the resolver).
 *
 * <p>Split out of {@link Pockets} because resolution needs the <em>screen</em>
 * (a client type); {@code Pockets} stays server-safe for the slot mixin and
 * {@link PocketHoverState}.
 */
public final class PocketGeometry {

    private PocketGeometry() {}

    /** +/− button size (px). */
    public static final int BUTTON_SIZE = 7;
    /** Height of the +/− button strip (buttons + 2px breathing above/below). */
    public static final int BUTTON_PANEL_H = BUTTON_SIZE + 4;
    /** Margin between the raised pockets-panel edge and the slot row. */
    public static final int ROW_PAD = 4;

    /**
     * The hotbar slot {@code n} (0–8) as an absolute screen rect, or null when
     * this screen doesn't surface it. {@code rect.x/y} = item top-left;
     * {@code rect.frameX()/frameY()} = the 18px frame's top-left.
     */
    public static @Nullable SlotScreenRect hotbar(AbstractContainerScreen<?> screen, int n) {
        return VanillaSlotResolver.resolve(screen, n).orElse(null);
    }

    /**
     * Absolute frame-x of the pocket at {@code depth} (0 = leftmost) when the
     * revealed pockets float as one horizontal row of {@code count} slots,
     * centered over hotbar slot {@code hotbar}. {@code SLOT/2 - count*SLOT/2}
     * shifts the row left so its center sits over the hotbar slot's center;
     * {@code depth*SLOT} steps right across the row.
     */
    public static int rowX(SlotScreenRect hotbar, int count, int depth) {
        return hotbar.frameX() + Pockets.SLOT / 2 - (count * Pockets.SLOT) / 2
                + depth * Pockets.SLOT;
    }

    /**
     * Absolute y of the floating pocket row — one slot-height above the hotbar
     * item row, leaving {@link Pockets#HOTBAR_GAP} between the row's bottom
     * edge and the hotbar slot's top.
     */
    public static int rowY(SlotScreenRect hotbar) {
        return hotbar.y() - Pockets.HOTBAR_GAP - Pockets.SLOT;
    }

    /** Absolute top of the +/− strip, just below the hotbar slot. */
    public static int buttonsTop(SlotScreenRect hotbar) {
        return hotbar.y() + Pockets.SLOT;
    }

    /** Absolute origin-x of the +/− strip (the − button's left edge). */
    public static int buttonsX(SlotScreenRect hotbar) {
        return hotbar.frameX() + 1;
    }

    /**
     * Whether this screen's live menu actually carries our pocket slots — true
     * on the survival inventory (the {@code MKCSlot} sits directly in the menu)
     * and the creative <em>inventory</em> tab (wrapped, unwrapped by
     * {@link MKCSlotAccess#asMKCSlot}); false everywhere else. The hotbar alone
     * isn't enough to gate on: creative's non-inventory tabs surface a hotbar
     * but not the pockets, and container screens surface neither.
     */
    public static boolean pocketsPresent(AbstractContainerScreen<?> screen) {
        for (Slot s : screen.getMenu().slots) {
            MKCSlot mk = MKCSlotAccess.asMKCSlot(s);
            if (mk != null && Pockets.isPocketGroup(mk.getGroupId())) return true;
        }
        return false;
    }
}
