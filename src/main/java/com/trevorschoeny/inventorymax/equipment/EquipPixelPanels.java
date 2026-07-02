package com.trevorschoeny.inventorymax.equipment;

import com.trevorschoeny.inventorymax.config.IMConfig;
import com.trevorschoeny.menukit.core.MKCSlot;
import com.trevorschoeny.menukit.core.MKCSlotAccess;
import com.trevorschoeny.menukit.core.MKCSlots;
import com.trevorschoeny.menukit.core.Panel;
import com.trevorschoeny.menukit.core.PanelElement;
import com.trevorschoeny.menukit.core.PanelPosition;
import com.trevorschoeny.menukit.core.PanelStyle;
import com.trevorschoeny.menukit.core.RenderContext;
import com.trevorschoeny.menukit.core.SlotElement;
import com.trevorschoeny.menukit.core.SlotRendering;
import com.trevorschoeny.menukit.inject.ScreenOrigin;
import com.trevorschoeny.menukit.inject.ScreenPanelAdapter;
import com.trevorschoeny.menukit.inject.VanillaSlotResolver;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * The equipment slots' presentation — one pixel-positioned panel <b>per slot</b>
 * (elytra, totem), each hosting its {@link SlotElement} + empty-slot placeholder
 * icon, anchored via the §0057-Revision pixel override (Trev's call, 2026-07-01).
 *
 * <h3>Per-screen anchors (why two panels, not one)</h3>
 *
 * The two screens want different arrangements, so each slot anchors to its own
 * vanilla neighbor and the pair's shape falls out:
 * <ul>
 *   <li><b>Survival inventory:</b> a vertical stack directly above the offhand —
 *       elytra two slot-pitches up, totem one (totem on top of the offhand).</li>
 *   <li><b>Creative inventory tab:</b> vanilla shows no offhand there (its
 *       wrapper sits meaninglessly under the tab strip), so the pair sits beside
 *       the leggings/boots column instead — one column right with a 1px gap
 *       (matching the offhand's own 1px gap), the contiguous pair centered on
 *       the column's vertical midpoint (Trev, 2026-07-01). Anchoring to the
 *       real leggings/boots wrappers (not hardcoded pixels) keeps the pair
 *       tracking creative's column if its layout ever moves.</li>
 * </ul>
 *
 * A {@code null} origin (anchor slot not on this screen — chests, creative's
 * non-inventory tabs) skips the panel entirely. {@link VanillaSlotResolver}
 * returns absolute positions and unwraps creative's {@code SlotWrapper}, so
 * both branches are one expression each, no per-screen mixins.
 */
public final class EquipPixelPanels {

    private EquipPixelPanels() {}

    /** Elytra sits 2 slot-pitches above the offhand on survival; totem 1. */
    private static final int ELYTRA_ABOVE_OFFHAND = 2;
    private static final int TOTEM_ABOVE_OFFHAND = 1;

    /** Vanilla player-inventory container indices for the creative anchors. */
    private static final int BOOTS_INDEX = 36;
    private static final int LEGGINGS_INDEX = 37;

    /** Register both equip presentation panels. Call once at IM client init. */
    public static void register() {
        present(EquipmentSlots.ELYTRA_GROUP, ELYTRA_ABOVE_OFFHAND, /*pairIndex*/ 0);
        present(EquipmentSlots.TOTEM_GROUP, TOTEM_ABOVE_OFFHAND, /*pairIndex*/ 1);
    }

    private static void present(String group, int slotsAboveOffhand, int pairIndex) {
        String dataPanel = EquipmentSlots.MOD_ID + ":" + group;
        Panel panel = Panel.builder(dataPanel + ":present")
                .elements(List.of(
                        // Slot first, icon after — the icon draws over an empty
                        // slot's frame (the old foreground z-order).
                        new SlotElement(dataPanel, group, 0, 0, 0),
                        new EmptyIcon(group)))
                .visible(true)
                // The SlotElement draws its own frame; no backing of ours.
                .style(PanelStyle.NONE)
                .position(PanelPosition.pixel(
                        () -> anchor(slotsAboveOffhand, pairIndex)))
                .build()
                // The slot is a click-through hole vanilla routes into; the
                // panel must not eat input around it.
                .opaque(false);
        new ScreenPanelAdapter(panel, 0).onPlayerInventory();
    }

    /**
     * Per-frame outer origin for one equip slot. Creative inventory tab: one
     * column right of the leggings/boots pair with a 1px gap (matching the
     * offhand slot's own 1px gap), the contiguous 2-slot pair centered on the
     * column's vertical midpoint — {@code pairIndex} 0 = top (elytra), 1 =
     * bottom (totem). Everywhere else: the offhand stack position. Null (skip)
     * when no container screen is up or the anchor slots aren't on it.
     */
    private static ScreenOrigin anchor(int slotsAboveOffhand, int pairIndex) {
        if (!IMConfig.equipmentSlotsEnabled()) return null; // feature off → no presentation
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) {
            return null;
        }
        if (screen instanceof CreativeModeInventoryScreen) {
            var leggings = VanillaSlotResolver.resolve(screen, LEGGINGS_INDEX).orElse(null);
            var boots = VanillaSlotResolver.resolve(screen, BOOTS_INDEX).orElse(null);
            if (leggings == null || boots == null) return null;
            // 1px gap to the column, like the offhand's; pair top = column
            // midpoint minus one pitch, so the 36px pair straddles the middle
            // of the (possibly gapped) leggings→boots span.
            int x = leggings.frameX() + MKCSlots.SLOT_PITCH + 1;
            int columnTop = leggings.frameY();
            int columnBottom = boots.frameY() + MKCSlots.SLOT_PITCH;
            int pairTop = (columnTop + columnBottom) / 2 - MKCSlots.SLOT_PITCH;
            return new ScreenOrigin(x, pairTop + pairIndex * MKCSlots.SLOT_PITCH);
        }
        return VanillaSlotResolver.resolve(screen, Inventory.SLOT_OFFHAND)
                .map(offhand -> new ScreenOrigin(offhand.frameX(),
                        offhand.frameY() - slotsAboveOffhand * MKCSlots.SLOT_PITCH))
                .orElse(null);
    }

    /**
     * Faint empty-slot placeholder over one equip slot (the library's
     * {@link SlotElement} draws the frame + item; this draws the ghost icon
     * only while the slot is empty). Render-only — never consumes clicks.
     */
    private static final class EmptyIcon implements PanelElement {

        private final String group;

        EmptyIcon(String group) {
            this.group = group;
        }

        @Override public int getChildX() { return 0; }
        @Override public int getChildY() { return 0; }
        @Override public int getWidth()  { return SlotRendering.DEFAULT_SIZE; }
        @Override public int getHeight() { return SlotRendering.DEFAULT_SIZE; }

        @Override
        public void render(RenderContext ctx) {
            MKCSlot mk = find(group);
            if (mk == null || !mk.getItem().isEmpty()) return;
            Identifier sprite = EquipSlotIcons.spriteFor(group);
            if (sprite == null) return;
            EquipSlotIcons.draw(ctx.graphics(), sprite,
                    ctx.originX() + SlotRendering.ITEM_INSET,
                    ctx.originY() + SlotRendering.ITEM_INSET);
        }

        /**
         * The equip slot for {@code group} in this screen's <em>live</em> menu —
         * direct on the survival inventory, unwrapped from the creative
         * {@code SlotWrapper} on the creative inventory tab. Null when absent
         * (the panel is skipped there anyway; this is belt-and-braces).
         */
        private static MKCSlot find(String group) {
            if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> acs)) {
                return null;
            }
            for (Slot s : acs.getMenu().slots) {
                MKCSlot mk = MKCSlotAccess.asMKCSlot(s);
                if (mk != null && group.equals(mk.getGroupId())) return mk;
            }
            return null;
        }
    }
}
