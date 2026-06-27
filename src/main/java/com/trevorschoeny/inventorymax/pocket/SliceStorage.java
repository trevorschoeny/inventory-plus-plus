package com.trevorschoeny.inventorymax.pocket;

import com.trevorschoeny.menukit.core.Storage;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A fixed-length window onto a larger backing {@link Storage} — used to give
 * each (hotbar, depth) pocket slot its own 1-slot view of the shared 27-slot
 * {@link Pockets#POCKETS} attachment.
 *
 * <h3>Advancement parity (§0045 Gap 3 — consumer-owned)</h3>
 *
 * RegisteredSlots slots back a separate container, so they don't feed vanilla's
 * {@code InventoryChangeTrigger} automatically the way real inventory slots
 * do. MenuKit deliberately ships no advancement API (narrow surface). So we
 * fire the trigger ourselves: on a <b>server-side</b> write of a non-empty
 * stack into a pocket, we call {@link CriteriaTriggers#INVENTORY_CHANGED} for
 * the owning player, giving pockets the same advancement behavior as normal
 * inventory slots.
 */
public final class SliceStorage implements Storage {

    private final Storage backing;
    private final int offset;
    private final int len;
    private final Player player;

    public SliceStorage(Storage backing, int offset, int len, Player player) {
        this.backing = backing;
        this.offset = offset;
        this.len = len;
        this.player = player;
    }

    @Override
    public ItemStack getStack(int localIndex) {
        return backing.getStack(offset + localIndex);
    }

    @Override
    public void setStack(int localIndex, ItemStack stack) {
        backing.setStack(offset + localIndex, stack);
        // Advancement parity — server-side, non-empty writes trigger the
        // same criterion real inventory slots do.
        if (!stack.isEmpty()
                && !player.level().isClientSide()
                && player instanceof ServerPlayer sp) {
            CriteriaTriggers.INVENTORY_CHANGED.trigger(sp, sp.getInventory(), stack);
        }
    }

    @Override
    public int size() {
        return len;
    }

    @Override
    public void markDirty() {
        backing.markDirty();
    }
}
