package com.trevorschoeny.inventorymax.pocket;

/**
 * Server-safe reveal/hover state for pockets, read by each slot's
 * {@code revealWhen} predicate.
 *
 * <p><b>No client imports — by design.</b> The slot mixin runs on both sides
 * and captures {@code () -> PocketHoverState.isRevealed(n, d)} as the reveal
 * predicate. MenuKit only evaluates it on the client (the server keeps slots
 * visible so content syncs), but the lambda is constructed on the server too —
 * so this class must not drag in client types. It's a plain bag of flags the
 * client render mixin updates each frame; the per-world counts are pushed in
 * from the client-side {@link PocketState}.
 *
 * <h3>Reveal model</h3>
 *
 * One hotbar slot's pocket column is revealed at a time (the hovered one,
 * with hysteresis). A pocket (hotbar, depth) is revealed iff that hotbar slot
 * is the revealed one AND the player's configured count for it exceeds depth.
 */
public final class PocketHoverState {

    private PocketHoverState() {}

    /** Hotbar slot whose pocket column is currently revealed, or -1. */
    private static volatile int revealedHotbar = -1;
    /** Hotbar slot directly under the cursor (for the attach keybind), or -1. */
    private static volatile int hoveredHotbar = -1;
    /** Pocket depth under the cursor within the revealed column, or -1. */
    private static volatile int hoveredDepth = -1;
    /** Client-pushed per-hotbar-slot pocket counts (0–3). */
    private static final int[] counts = new int[Pockets.HOTBAR_SLOTS];

    /** True if pocket (hotbar, depth) should be visible + interactive now. */
    public static boolean isRevealed(int hotbar, int depth) {
        if (hotbar < 0 || hotbar >= Pockets.HOTBAR_SLOTS) return false;
        return revealedHotbar == hotbar && counts[hotbar] > depth;
    }

    public static int revealedHotbar() { return revealedHotbar; }
    public static void setRevealedHotbar(int hotbar) { revealedHotbar = hotbar; }

    public static int hoveredHotbar() { return hoveredHotbar; }
    public static void setHoveredHotbar(int hotbar) { hoveredHotbar = hotbar; }

    public static int hoveredDepth() { return hoveredDepth; }
    public static void setHoveredDepth(int depth) { hoveredDepth = depth; }

    public static int count(int hotbar) {
        if (hotbar < 0 || hotbar >= Pockets.HOTBAR_SLOTS) return 0;
        return counts[hotbar];
    }

    /** Client pushes the live per-world count for a hotbar slot. */
    public static void setCount(int hotbar, int count) {
        if (hotbar < 0 || hotbar >= Pockets.HOTBAR_SLOTS) return;
        counts[hotbar] = count;
    }
}
