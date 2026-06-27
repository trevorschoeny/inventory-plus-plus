package com.trevorschoeny.inventorymax.pocket;

import com.trevorschoeny.inventorymax.config.IMConfig;
import com.trevorschoeny.menukit.core.SlotScreenContext;
import com.trevorschoeny.menukit.core.SlotScreenPresence;
import com.trevorschoeny.menukit.core.MKCSlotAccess;
import com.trevorschoeny.menukit.core.MKCSlot;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;

/**
 * Registers the pockets' screen presence with MenuKit's slot dispatch — the
 * library draws / hover-routes / clicks / reveals every pocket slot on every
 * inventory-bearing screen (creative included), so this replaces the old
 * {@code PocketRenderMixin} + {@code PocketClickMixin}.
 *
 * <h3>One presence per panel + one decoration carrier</h3>
 *
 * Each pocket is its own 1-slot slot/panel, so we register a presence per panel
 * (27) — that's what makes the library draw + route each pocket slot. The shared
 * pocket decoration (hover-reveal + the §0047 floating-row reposition, the raised
 * panel backing, and the +/- buttons) belongs to the column as a whole, not a
 * single slot, so it rides <b>one</b> of those presences. The dispatch batches
 * all backgrounds → all slots → all foregrounds across the matching presences, so
 * one decoration carrier + 27 slot ids still gives the correct z-order: the panel
 * draws behind every pocket slot, the buttons over them.
 *
 * <p>What the library now owns (gone from us): the registered-slot frame/item/hover
 * render, the hover→click routing, and the gap-click guard. What stays here: the
 * reveal tick, the per-screen row reposition, and the decoration + its +/- clicks.
 */
public final class PocketScreenPresence {

    private PocketScreenPresence() {}

    /** Register all pocket presences. Call once at IM client init (after slots exist). */
    public static void register() {
        boolean carriesDecoration = true;
        for (int n = 0; n < Pockets.HOTBAR_SLOTS; n++) {
            for (int d = 0; d < Pockets.MAX_PER_SLOT; d++) {
                // Pockets belong to the player's own inventory only — survival
                // InventoryScreen + creative inventory tab. Restrict every panel
                // (not just the decoration carrier): the dispatch matches each
                // presence independently for its slot render, so an unrestricted
                // sibling would still fire on chests/furnaces.
                SlotScreenPresence p =
                        SlotScreenPresence.forPanel("inventorymax:" + Pockets.groupId(n, d))
                                .onlyScreens(InventoryScreen.class, CreativeModeInventoryScreen.class);
                if (carriesDecoration) {
                    carriesDecoration = false;
                    p.onPrepare(PocketScreenPresence::prepare)
                            .background(PocketScreenPresence::background)
                            .foreground(PocketScreenPresence::foreground)
                            .onClick(PocketScreenPresence::onClick);
                }
                p.register();
            }
        }
    }

    /** Per-frame: update hover-reveal, then reposition the revealed row — both
     *  off the screen's own hotbar geometry, so they're correct on any screen. */
    private static void prepare(SlotScreenContext ctx) {
        // Pockets manifest only where the slot actually lives in the live menu:
        // the survival inventory and the creative *inventory* tab. On a
        // non-inventory creative tab the wrappers aren't in the menu, so clear any
        // stale reveal and draw nothing — background/foreground/click all key off
        // the reveal state. (The matcher already excludes non-inventory screens.)
        if (!pocketsPresent(ctx)) {
            PocketHoverState.setRevealedHotbar(-1);
            PocketHoverState.setHoveredHotbar(-1);
            PocketHoverState.setHoveredDepth(-1);
            return;
        }
        PocketHover.updateHover(ctx.menu(), ctx.leftPos(), ctx.topPos(), ctx.mouseX(), ctx.mouseY());
        PocketRow.reposition(ctx.menu());
    }

    /** Whether this screen's live menu carries our pocket slots — true on the
     *  survival inventory (the slot sits directly in the menu) and the creative
     *  inventory tab (wrapped in a creative {@code SlotWrapper}, unwrapped by
     *  {@link Slots#asMKCSlot}); false everywhere else (a non-inventory creative
     *  tab). The matcher handles non-inventory <em>screens</em>; this handles the
     *  creative screen's non-inventory <em>tabs</em>, which share its class. */
    private static boolean pocketsPresent(SlotScreenContext ctx) {
        for (Slot s : ctx.menu().slots) {
            MKCSlot mk = MKCSlotAccess.asMKCSlot(s);
            if (mk != null && Pockets.isPocketGroup(mk.getGroupId())) return true;
        }
        return false;
    }

    /** Raised panel behind the revealed pocket row. */
    private static void background(SlotScreenContext ctx, GuiGraphics g) {
        PocketPanelRender.drawBackground(g, ctx.menu(), ctx.leftPos(), ctx.topPos());
    }

    /** Floating +/- buttons over the row. */
    private static void foreground(SlotScreenContext ctx, GuiGraphics g) {
        PocketPanelRender.drawButtons(g, ctx.menu(), ctx.leftPos(), ctx.topPos(),
                ctx.mouseX(), ctx.mouseY());
    }

    /** +/- resize button hit-test (the only pocket decoration that's clickable).
     *  The gap-click guard + slot routing are the library's now. */
    private static boolean onClick(SlotScreenContext ctx, int button) {
        if (!IMConfig.pocketCyclerEnabled()) return false;
        int rev = PocketHoverState.revealedHotbar();
        if (rev < 0) return false;
        int c = PocketState.count(rev);
        double mx = ctx.mouseX(), my = ctx.mouseY();
        if (PocketButtons.inRect(mx, my,
                PocketButtons.plusRect(ctx.menu(), ctx.leftPos(), ctx.topPos(), rev))) {
            if (c < Pockets.MAX_PER_SLOT) PocketState.grow(rev); // grayed at max → no-op
            return true;
        }
        if (PocketButtons.inRect(mx, my,
                PocketButtons.minusRect(ctx.menu(), ctx.leftPos(), ctx.topPos(), rev))) {
            if (c > 0) PocketState.shrink(rev); // grayed at 0 → no-op
            return true;
        }
        return false;
    }
}
