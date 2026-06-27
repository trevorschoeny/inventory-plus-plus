package com.trevorschoeny.inventorymax.mending;

import com.trevorschoeny.inventorymax.config.IMConfig;
import com.trevorschoeny.menukit.core.MendingCandidates;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventory Max's "mend any item in the inventory" feature — feeds the player's
 * whole vanilla inventory into MenuKit-Containers' XP-repair pool via the
 * {@link MendingCandidates} hook, so a damaged Mending item repairs while just
 * sitting in your pack, not only when equipped or in a registered slot.
 *
 * <p>Rides MKC's single {@code ExperienceOrb} intercept (no dueling mixin). MKC
 * applies the candidate predicate itself (damaged AND carries {@code REPAIR_WITH_XP}),
 * so we hand over the slots raw and let it filter.
 *
 * <h3>Scope</h3>
 * Hotbar + main inventory (container slots 0–35), <b>except the selected hotbar
 * slot</b> — that's the main hand, which vanilla already mends; skipping it keeps
 * it from appearing twice in the unified pool. Offhand and armor aren't in 0–35
 * and are likewise vanilla's already. The equip slots and pockets opt in
 * separately at the slot level ({@code mendsFromXp()}, always on); this is the
 * toggleable vanilla-inventory layer.
 *
 * <h3>Server-side</h3>
 * Gathered on the server during XP pickup. Gated by
 * {@link IMConfig#mendInventoryItems()} — safe to read here because IM requires
 * IP (which owns the config's enum), so IMConfig is always present wherever IM
 * runs. {@link MendingCandidates.Candidate#stack()} returns the live inventory
 * stack MKC repairs in place; {@link MendingCandidates.Candidate#onRepaired()}
 * broadcasts the new durability to the client (persistence is automatic — it's
 * in the saved {@link Inventory}).
 */
public final class InventoryMendingProvider implements MendingCandidates.Provider {

    public static final InventoryMendingProvider INSTANCE = new InventoryMendingProvider();

    private InventoryMendingProvider() {}

    @Override
    public List<MendingCandidates.Candidate> candidatesFor(ServerPlayer player) {
        if (!IMConfig.mendInventoryItems()) return List.of();
        Inventory inv = player.getInventory();
        int selected = inv.getSelectedSlot();
        List<MendingCandidates.Candidate> out = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            if (i == selected) continue;        // main hand — already in vanilla's pool
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            out.add(new MendingCandidates.Candidate() {
                @Override public ItemStack stack() { return stack; }
                @Override public void onRepaired() { player.inventoryMenu.broadcastChanges(); }
            });
        }
        return out;
    }
}
