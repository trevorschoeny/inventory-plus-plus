package com.trevorschoeny.inventorymax.equipment;

import com.trevorschoeny.inventorymax.config.IMConfig;

import com.trevorschoeny.menukit.core.ItemDisplay;
import com.trevorschoeny.menukit.core.PanelStyle;
import com.trevorschoeny.menukit.hud.MKHudAnchor;
import com.trevorschoeny.menukit.hud.MKHudPanel;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * HUD occupancy cue for the equipment slots — small elytra + totem item icons
 * stacked vertically (elytra above totem) to the left of the hotbar, each shown
 * only when its slot is filled (Trev, 2026-06-24). The elytra icon also shows
 * when an elytra is worn in the vanilla chest slot, so "you've got wings" reads
 * the same however you equipped them.
 *
 * <p>Built on MenuKit's HUD primitive ({@code MKHudPanel}) — HUD is its UI
 * wheelhouse. Render-only, registered once at client init. Icon-only (no
 * count/durability overlay) for a clean little indicator. An empty supplier
 * renders nothing, so "show when occupied" falls out for free. Pixel position
 * is tunable via the anchor offsets below.
 */
public final class EquipHud {

    private EquipHud() {}

    // BOTTOM_CENTER tracks the hotbar's screen-centered position across GUI
    // scales; the offsets nudge the 16px icon column just left of the hotbar.
    private static final int ICON_SIZE = 12;  // 12px icons (¾ of vanilla's native 16px)
    private static final int OFFSET_X = -99;  // snug to the hotbar (~2px gap, no awkward space)
    private static final int OFFSET_Y = -3;   // bottom icon aligned to the hotbar item row
    private static final int ICON_PITCH = 13; // elytra above totem (1px vertical gap)

    /** Register the HUD cue. Call once from client init. */
    public static void register() {
        MKHudPanel.builder(EquipmentSlots.MOD_ID + ":equip_cues")
                .anchor(MKHudAnchor.BOTTOM_CENTER, OFFSET_X, OFFSET_Y)
                .padding(0).autoSize()
                .style(PanelStyle.NONE)
                .showInScreen() // persist like a status cue — don't vanish when a screen opens
                .showWhen(() -> IMConfig.equipmentSlotsEnabled() && IMConfig.equipmentHudCue()
                        && (!elytraCue().isEmpty() || !totemCue().isEmpty()))
                .element(new ItemDisplay(0, 0, ICON_SIZE, EquipHud::elytraCue, false, false))
                .element(new ItemDisplay(0, ICON_PITCH, ICON_SIZE, EquipHud::totemCue, false, false))
                .build();
    }

    /** Elytra to show: the equip slot first, else a worn chest-slot elytra, else none. */
    private static ItemStack elytraCue() {
        Player p = Minecraft.getInstance().player;
        if (p == null) return ItemStack.EMPTY;
        ItemStack equip = EquipmentSlots.getElytra(p);
        if (!equip.isEmpty()) return equip;
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        return chest.is(Items.ELYTRA) ? chest : ItemStack.EMPTY;
    }

    /** Totem to show: the equip totem slot, else none. */
    private static ItemStack totemCue() {
        Player p = Minecraft.getInstance().player;
        return p == null ? ItemStack.EMPTY : EquipmentSlots.getTotem(p);
    }
}
