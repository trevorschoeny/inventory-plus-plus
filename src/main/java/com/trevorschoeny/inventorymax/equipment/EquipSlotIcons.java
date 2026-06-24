package com.trevorschoeny.inventorymax.equipment;

import com.trevorschoeny.menukit.core.MenuKitSlot;
import com.trevorschoeny.menukit.core.SlotRendering;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Empty-slot placeholder icons for the equipment slots — the faint elytra/totem
 * silhouettes shown when a slot is empty, mirroring vanilla's armor-slot ghosts.
 *
 * <p><b>Why we draw these ourselves.</b> MenuKit owns grafted-slot rendering, but
 * which placeholder belongs on which slot is consumer knowledge, so we draw over
 * its exposed slot primitives (§0045: library exposes the capability, consumer
 * applies it). MenuKit's {@link SlotRendering#drawGhostIcon} dims to 40% (for
 * <em>disabled/filtered</em> slots); vanilla draws its {@code #555555} empty-slot
 * icons at <b>full opacity</b>, and Trev's art matches that color, so we blit at
 * full opacity to land exactly on the vanilla look.
 *
 * <p>Sprites live at {@code assets/inventorymax/textures/gui/sprites/container/slot/}
 * and are auto-collected into the GUI atlas (no atlas json needed).
 *
 * <p>Client-only: imports client render classes, referenced solely from client
 * mixins, so it never loads server-side.
 */
public final class EquipSlotIcons {

    private EquipSlotIcons() {}

    /** Faint elytra silhouette for the empty elytra slot. */
    public static final Identifier ELYTRA =
            Identifier.fromNamespaceAndPath(EquipmentSlots.MOD_ID, "container/slot/elytra");
    /** Faint totem silhouette for the empty totem slot. */
    public static final Identifier TOTEM =
            Identifier.fromNamespaceAndPath(EquipmentSlots.MOD_ID, "container/slot/totem");

    /** The placeholder for a graft group, or {@code null} if it isn't an equip slot. */
    public static Identifier spriteFor(String groupId) {
        if (EquipmentSlots.ELYTRA_GROUP.equals(groupId)) return ELYTRA;
        if (EquipmentSlots.TOTEM_GROUP.equals(groupId)) return TOTEM;
        return null;
    }

    /** Blit a placeholder at the slot's item top-left, full opacity (vanilla match). */
    public static void draw(GuiGraphics g, Identifier sprite, int itemX, int itemY) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, itemX, itemY, 16, 16, 1.0F);
    }

    /**
     * Survival inventory screen: MenuKit's graft render draws the slots; here we
     * overlay a placeholder on each empty equip slot. Item top-left =
     * {@code leftPos/topPos + the slot's frame graftX/Y + 1px inset}.
     */
    public static void renderEmpty(AbstractContainerMenu menu, GuiGraphics g, int leftPos, int topPos) {
        for (Slot slot : menu.slots) {
            if (!(slot instanceof MenuKitSlot mk)) continue;
            if (!slot.getItem().isEmpty()) continue;
            Identifier sprite = spriteFor(mk.getGroupId());
            if (sprite == null) continue;
            draw(g, sprite, leftPos + mk.graftX() + SlotRendering.ITEM_INSET,
                    topPos + mk.graftY() + SlotRendering.ITEM_INSET);
        }
    }
}
