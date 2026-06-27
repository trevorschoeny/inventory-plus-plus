package com.trevorschoeny.inventorymax.equipment;

import com.trevorschoeny.inventoryplus.autorestock.AutoRestockSearch;
import com.trevorschoeny.inventoryplus.config.IPConfig;
import com.trevorschoeny.inventoryplus.cyclable.HotbarCyclable.ExtraSlot;
import com.trevorschoeny.inventoryplus.cyclable.HotbarCyclableRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Auto-restock for the equipment totem slot — when the slot's totem is consumed
 * by a death save, slide another totem in from the inventory so one is always
 * ready. Composes Inventory Plus's {@link AutoRestockSearch} (IM→IP, allowed); IM
 * owns the move into its own registered slot via the shift-click routing we already
 * built.
 *
 * <p>Client tick, mirroring IP's own auto-restock. Fires only on the consume
 * <em>transition</em> (slot held a totem last tick, empty now) and only during
 * gameplay (no screen open) — so a death save refills, but manually pulling the
 * totem out in the inventory screen does not, and a slot that was never filled is
 * left alone. Follows IP's auto-restock toggle ({@code autoRestockItem} — a totem
 * is a non-damageable item, the same bucket IP refills the offhand under).
 */
public final class EquipAutoRestock {

    private EquipAutoRestock() {}

    /** Bare totem matched against the inventory (by item kind, not components). */
    private static final ItemStack TOTEM = new ItemStack(Items.TOTEM_OF_UNDYING);

    /** InventoryMenu maps inventory hotbar slots 0–8 to menu slots 36–44. */
    private static final int HOTBAR_MENU_OFFSET = 36;

    /** Whether the totem slot held a totem last tick — to catch the consume transition. */
    private static boolean totemSlotFilled = false;

    /** Tick handler — register against {@code ClientTickEvents.END_CLIENT_TICK}. */
    public static void tick(Minecraft mc) {
        if (mc == null) return;
        LocalPlayer player = mc.player;
        MultiPlayerGameMode gameMode = mc.gameMode;
        if (player == null || gameMode == null) return;
        if (player.containerMenu != player.inventoryMenu) return; // only the player's own menu

        boolean filledNow = !EquipmentSlots.getTotem(player).isEmpty();
        boolean justConsumed = totemSlotFilled && !filledNow;
        totemSlotFilled = filledNow;

        if (!justConsumed) return;                // only the full→empty transition
        if (mc.screen != null) return;            // gameplay only — not while editing the inventory
        if (!IPConfig.autoRestockItem()) return;  // follow the auto-restock toggle

        Inventory inv = player.getInventory();
        // A backup totem may live in a pocket too — outside the 0–35 model — so
        // include the cyclers' extra slots in the search.
        List<ExtraSlot> extras = HotbarCyclableRegistry.extraSearchSlots(player);
        int source = AutoRestockSearch.findSource(inv, TOTEM, AutoRestockSearch.NONE, extras);
        if (source == AutoRestockSearch.NONE) return;

        // A pocket source can't be shift-clicked client-side in-world (the pocket
        // slot is inert), so route it through the cycler, which performs the move
        // server-side (the totem lands in the equip slot via the same quick-move
        // routing). Returns false for an ordinary 0–35 source — fall through to
        // the normal client-side quick-move.
        if (HotbarCyclableRegistry.quickMoveOut(source)) return;

        // Inventory source: convert a hotbar slot (0–8) to its InventoryMenu slot,
        // then shift-click it — our quick-move routing drops the totem into the
        // (now-empty) equip totem slot.
        int menuSlot = source < AutoRestockSearch.MAIN_INV_START ? source + HOTBAR_MENU_OFFSET : source;
        gameMode.handleInventoryMouseClick(
                player.inventoryMenu.containerId, menuSlot, 0, ClickType.QUICK_MOVE, player);
    }
}
