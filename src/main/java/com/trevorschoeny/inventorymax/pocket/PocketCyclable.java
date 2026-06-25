package com.trevorschoeny.inventorymax.pocket;

import com.trevorschoeny.inventoryplus.autorestock.AutoRestockSuppression;
import com.trevorschoeny.inventoryplus.cyclable.CyclerOperation;
import com.trevorschoeny.inventoryplus.cyclable.HotbarCyclable;
import com.trevorschoeny.inventorymax.config.IMConfig;
import com.trevorschoeny.menukit.core.Storage;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapts Pocket Cycler's "rotate the ring" mechanic to the
 * {@link HotbarCyclable} contract — the pocket sibling of Column Cycler's
 * {@code ColumnCyclerCyclable}. Auto Tool Switch and Auto-Restock consume this
 * through {@code HotbarCyclableRegistry} without knowing pockets specifically;
 * they just see more Tier-2 candidates.
 *
 * <h3>Why pockets are "extra" slots</h3>
 *
 * Column Cycler's cycle slots live in the player's real inventory (container
 * slots 9-35), so they fit {@link #hotbarPositionOf}'s 0-35 contract directly.
 * Pockets do not — their content lives in a separate attachment
 * ({@link Pockets#POCKETS}), outside the 0-35 model entirely. So we surface
 * them through {@link #extraSearchSlots(Player)} with an <b>encoded id</b>
 * (see {@link #POCKET_ID_BASE}) the registry routes back to us, and read their
 * live content straight from the synced attachment — the same path the cycle
 * HUD uses, which works in-world where the pocket <i>slots</i> are inert.
 *
 * <h3>Bring-to-hotbar is server-authoritative</h3>
 *
 * Unlike Column Cycler (whose rotation applies locally at once), pocket
 * rotation is a {@link PocketRotateC2S} round-trip — the rotated contents
 * arrive a tick or two later. {@link #bringToHotbar(int)} therefore marks the
 * destination hotbar slot via {@link AutoRestockSuppression} so the round-trip
 * arrival isn't mistaken for depletion (pocket tools aren't in Auto-Restock's
 * 0-35 snapshot, so a freshly-arrived one would otherwise read as a brand-new
 * stack — Column Cycler doesn't need this because its slots <i>are</i>
 * snapshotted and its rotation is immediate).
 */
public final class PocketCyclable implements HotbarCyclable {

    /** Singleton — registered once via {@code HotbarCyclableRegistry#register} at IM client init. */
    public static final PocketCyclable INSTANCE = new PocketCyclable();

    private PocketCyclable() {}

    /**
     * Base of the encoded extra-slot id space: {@code POCKET_ID_BASE +
     * flatIndex(hotbar, depth)}, spanning {@code [100, 100 + TOTAL)}. 100 sits
     * well clear of the 0-35 inventory model <i>and</i> Column Cycler's 9-35
     * claims, satisfying {@link HotbarCyclable}'s disjoint-id contract so a
     * returned id routes to exactly one cycler.
     */
    private static final int POCKET_ID_BASE = 100;

    /** Encode a pocket's {@code (hotbar, depth)} into an extra-slot id. */
    public static int encodeId(int hotbar, int depth) {
        return POCKET_ID_BASE + Pockets.flatIndex(hotbar, depth);
    }

    /** True if {@code id} is one of this cycler's encoded pocket ids. */
    public static boolean isPocketId(int id) {
        return id >= POCKET_ID_BASE && id < POCKET_ID_BASE + Pockets.TOTAL;
    }

    /** Decode the hotbar column from a pocket id (undefined unless {@link #isPocketId}). */
    public static int hotbarOf(int id) {
        return (id - POCKET_ID_BASE) / Pockets.MAX_PER_SLOT;
    }

    /** Decode the depth (0 = closest to the hand) from a pocket id. */
    public static int depthOf(int id) {
        return (id - POCKET_ID_BASE) % Pockets.MAX_PER_SLOT;
    }

    @Override
    public int hotbarPositionOf(int slot) {
        if (!isPocketId(slot)) return -1;
        if (!IMConfig.pocketCyclerEnabled()) return -1;
        int hotbar = hotbarOf(slot);
        int depth = depthOf(slot);
        // Only revealed pockets are reachable — depth must be within the hotbar
        // slot's current per-world pocket count. (Hidden pockets exist
        // server-side but aren't part of any active cycle.)
        if (depth >= PocketState.count(hotbar)) return -1;
        // A pocket's column IS the hotbar position the rotation brings it to.
        return hotbar;
    }

    @Override
    public List<ExtraSlot> extraSearchSlots(Player player) {
        if (!IMConfig.pocketCyclerEnabled()) return List.of();
        // Read live content from the synced attachment — works in-world where
        // the pocket slots are inert/hidden (same path the cycle HUD uses).
        Storage pockets = Pockets.POCKETS.bind(player);
        List<ExtraSlot> out = new ArrayList<>();
        for (int hotbar = 0; hotbar < Pockets.HOTBAR_SLOTS; hotbar++) {
            int count = PocketState.count(hotbar);
            for (int depth = 0; depth < count; depth++) {
                ItemStack stack = pockets.getStack(Pockets.flatIndex(hotbar, depth));
                if (stack.isEmpty()) continue;
                out.add(new ExtraSlot(encodeId(hotbar, depth), stack));
            }
        }
        return out;
    }

    @Override
    public CyclerOperation bringToHotbar(int slot) {
        // Re-validate the claim — the caller may hold a stale id, or the pocket
        // count may have changed between query and bring.
        if (hotbarPositionOf(slot) == -1) return CyclerOperation.NO_OP;
        int hotbar = hotbarOf(slot);
        int depth = depthOf(slot);
        int count = PocketState.count(hotbar);
        // Cheapest rotation to land depth `d` in the hand: FORWARD d+1 steps
        // (each step brings pocket 0 → hand) or BACKWARD count-d steps (each
        // brings the topmost pocket → hand). Pick the cheaper direction, same
        // as ColumnCyclerRotator — each step is one rotation packet.
        int fwdSteps = depth + 1;
        int bwdSteps = count - depth;
        boolean forward = fwdSteps <= bwdSteps;
        int steps = Math.min(fwdSteps, bwdSteps);
        if (steps <= 0) return CyclerOperation.NO_OP;
        rotate(hotbar, count, forward, steps);
        // Undo: same step count, opposite direction. Capture the parameters so
        // the reversal doesn't depend on (possibly drifted) current state —
        // matches the CyclerOperation drift-tolerance contract.
        final int undoHotbar = hotbar;
        final int undoCount = count;
        final int undoSteps = steps;
        final boolean undoForward = !forward;
        return () -> rotate(undoHotbar, undoCount, undoForward, undoSteps);
    }

    @Override
    public boolean quickMoveOut(int slot) {
        // Same claim check as bringToHotbar — only our revealed pockets.
        if (hotbarPositionOf(slot) == -1) return false;
        // Server-authoritative move: the in-world pocket slot is inert
        // client-side, so the server runs the real quick-move and routes the
        // content (totem → equipment slot, armor → armor slot) the same way a
        // shift-click would, then syncs the result back. No hotbar slot
        // changes here (the content leaves the pocket for its destination), so
        // unlike bringToHotbar there's no Auto-Restock slot to re-baseline.
        ClientPlayNetworking.send(new PocketQuickMoveC2S(hotbarOf(slot), depthOf(slot)));
        return true;
    }

    /**
     * Send {@code steps} one-step rotation requests for {@code hotbar}'s ring,
     * and mark the slot as deliberately-changing so Auto-Restock re-baselines
     * it across the rotation's round-trip rather than mis-reading the
     * swapped-in tool as a damage / run-out event. (See the class javadoc for
     * why pockets need this and Column Cycler doesn't.)
     */
    private static void rotate(int hotbar, int count, boolean forward, int steps) {
        AutoRestockSuppression.markExternalChange(hotbar);
        for (int i = 0; i < steps; i++) {
            ClientPlayNetworking.send(new PocketRotateC2S(hotbar, count, forward));
        }
    }
}
