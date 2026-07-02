package com.trevorschoeny.inventorymax.pocket;

import com.trevorschoeny.menukit.core.CreatedSlotAdapter;
import com.trevorschoeny.menukit.core.MKCBehaviorKeys;
import com.trevorschoeny.menukit.core.StorageAttachment;
import com.trevorschoeny.menukit.window.TriBool;
import com.trevorschoeny.menukit.window.Window;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Pocket Cycler constants + the server-persistent content storage.
 *
 * <h3>Structure model (the §0045 fixed-slot + client-gated composition)</h3>
 *
 * The slot kit slots a <b>fixed</b> set of slots at menu construction; it
 * has no dynamic per-slot structure. So we slot the <b>maximum</b> — 3 pocket
 * slots above every one of the 9 hotbar slots (27 total) — as real,
 * server-synced, persistent slots, and let a <b>client-side per-world count</b>
 * ({@link PocketState}) decide how many of each hotbar slot's 3 pockets are
 * revealed + interactive. Hidden pockets are inert (§0021): invisible and
 * non-interactive, but their content still persists server-side. "Attaching"
 * a pocket is just raising the count from 0; resizing is changing it (1–3).
 *
 * <p>Each (hotbar, depth) pocket is its own single-slot slot with its own
 * panel + reveal predicate — that's what gives per-depth reveal granularity
 * the panel-level reveal API otherwise wouldn't (one panel reveals all-or-
 * nothing).
 */
public final class Pockets {

    private Pockets() {}

    public static final String MOD_ID = "inventorymax";

    /** Hotbar slots (0–8). */
    public static final int HOTBAR_SLOTS = 9;
    /** Max pockets stacked above one hotbar slot. */
    public static final int MAX_PER_SLOT = 3;
    /** Total registered pocket slots (9 × 3). */
    public static final int TOTAL = HOTBAR_SLOTS * MAX_PER_SLOT;

    // ─── Screen-relative layout (added to leftPos/topPos at render time) ──
    /** Vanilla hotbar slot row y. */
    public static final int HOTBAR_Y = 142;
    /**
     * X of the pocket column (and hotbar-slot hover). 7, not 8: a registered
     * slot's 18px frame must sit 1px left of the vanilla item x so the pocket
     * frame lines up with the vanilla hotbar slot frame below it (Trev
     * 2026-06-02 — "both panels a px too far right").
     */
    public static final int HOTBAR0_X = 7;
    /** Vanilla slot pitch. */
    public static final int SLOT = 18;
    /** Gap between the hotbar slot and the bottom of the pocket column. */
    public static final int HOTBAR_GAP = 8;
    /** Height of the +/− button row drawn above the topmost pocket. */
    public static final int BUTTON_ROW_H = 9;

    /**
     * Player-attached pocket content — 27 slots, survives logout/restart
     * (§0034). Declared at common init. The slot slices this per (hotbar,
     * depth).
     */
    public static StorageAttachment<Player, NonNullList<ItemStack>> POCKETS;

    /** Common-init registration. Call from the main entrypoint. */
    public static void register() {
        POCKETS = StorageAttachment.playerAttached(MOD_ID, "pockets", TOTAL);
    }

    /**
     * Declares each pocket slot's MENDING opt-in by the slot's address, once at
     * common init — behavior-by-address (the creation mixin is behavior-free). A
     * damaged Mending item in any pocket joins the XP-orb repair pool. Idempotent.
     */
    public static void declareSlotBehavior() {
        for (int n = 0; n < HOTBAR_SLOTS; n++) {
            for (int d = 0; d < MAX_PER_SLOT; d++) {
                String g = groupId(n, d);
                Window.slot(CreatedSlotAdapter.addressOf(MOD_ID + ":" + g, g, 0))
                        .set(MKCBehaviorKeys.MENDING, TriBool.TRUE);
            }
        }
    }

    /** Flat index into the 27-slot backing for (hotbar, depth). */
    public static int flatIndex(int hotbar, int depth) {
        return hotbar * MAX_PER_SLOT + depth;
    }

    /** Unique slot group/panel id for (hotbar, depth). */
    public static String groupId(int hotbar, int depth) {
        return "pocket_" + hotbar + "_" + depth;
    }

    /** True for any pocket slot's group id (see {@link #groupId}) — used to ask
     *  "does this screen's live menu actually carry our pockets?". */
    public static boolean isPocketGroup(String groupId) {
        return groupId.startsWith("pocket_");
    }

    /** Screen-relative x of the pocket column for a hotbar slot. */
    public static int pocketX(int hotbar) {
        return HOTBAR0_X + hotbar * SLOT;
    }

    /**
     * Screen-relative y of a pocket at {@code depth} (0 = closest to the
     * hotbar). Pockets stack upward from just above the hotbar.
     *
     * <p>This is the original <b>vertical</b> column layout — now used only as
     * the slot's construction seed. At runtime the revealed pockets are moved
     * into the floating horizontal row below ({@link #pocketRowX} /
     * {@link #pocketRowY}) via §0047 {@code setRenderPosition}.
     */
    public static int pocketY(int depth) {
        return HOTBAR_Y - HOTBAR_GAP - (depth + 1) * SLOT;
    }

    // The floating-row + live-hotbar geometry (absolute screen pixels, resolved
    // per frame off the actual hotbar slots — creative-aware) lives in the
    // client-only {@link PocketGeometry}; this class stays server-safe for the
    // slot mixin and PocketHoverState.
}
