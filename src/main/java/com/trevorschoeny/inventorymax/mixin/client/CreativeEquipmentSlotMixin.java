package com.trevorschoeny.inventorymax.mixin.client;

import com.trevorschoeny.inventorymax.equipment.EquipSlotIcons;
import com.trevorschoeny.inventorymax.equipment.EquipmentSlots;
import com.trevorschoeny.menukit.core.MenuKitSlot;
import com.trevorschoeny.menukit.core.SlotRendering;
import com.trevorschoeny.menukit.mixin.AbstractContainerScreenAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Positions + frames the equipment slots in the creative inventory tab. Purely
 * visual — the library owns creative slot sync (§0051): MenuKit-Containers'
 * creative-set-slot bridge routes a creative placement into a grafted slot's
 * real storage, so the elytra/totem slot behaves identically in creative and
 * survival with no consumer creative-sync. This mixin only fixes how they look.
 *
 * <p>How it works: the creative inventory tab iterates EVERY slot on the player's
 * {@code inventoryMenu} — including our two grafts — and wraps each in a
 * {@code SlotWrapper} positioned by index. Our grafts have no index-position
 * mapping, so they fall into the default branch and overlay the hotbar. The
 * wrapper delegates everything (item, mayPlace, click) to our real
 * {@link MenuKitSlot}, so they function once the library bridges the write —
 * they're just in the wrong place and frameless. This mixin fixes both:
 *
 * <ul>
 *   <li><b>Position</b> — {@code SlotWrapper.x/y} are {@code final}, so we
 *       correct them at construction via {@link ModifyArgs} on the
 *       {@code new SlotWrapper(...)} call, placing our two slots to the right of
 *       the leggings/boots armor column.</li>
 *   <li><b>Frame</b> — our slots sit outside the creative GUI texture, so vanilla
 *       draws no recessed frame for them; we draw it in {@code renderBg} (vanilla
 *       then renders the item on top).</li>
 * </ul>
 *
 * <p>Pockets (also grafted, also wrapped) stay inert in creative — no hover
 * updates there — so their wrappers report {@code isActive() == false} and
 * vanilla skips them entirely. Only the always-visible equipment slots surface.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeEquipmentSlotMixin {

    @Shadow
    private static CreativeModeTab selectedTab;

    /**
     * Re-place our two grafts when creative wraps the inventoryMenu slots for the
     * inventory tab. {@code SlotWrapper.x/y} are final, so we correct them at
     * construction by modifying the {@code SlotWrapper(target, index, x, y)}
     * constructor args (2 = x, 3 = y). {@code @ModifyArgs} on the {@code <init>}
     * invoke needs no reference to the package-private SlotWrapper type.
     */
    @ModifyArgs(
            method = "selectTab",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"))
    private void inventoryMax$placeEquipSlots(Args args) {
        Slot target = args.get(0);
        if (target instanceof MenuKitSlot mk) {
            if (EquipmentSlots.ELYTRA_GROUP.equals(mk.getGroupId())) {
                args.set(2, EquipmentSlots.CREATIVE_ELYTRA_X);
                args.set(3, EquipmentSlots.CREATIVE_Y);
            } else if (EquipmentSlots.TOTEM_GROUP.equals(mk.getGroupId())) {
                args.set(2, EquipmentSlots.CREATIVE_TOTEM_X);
                args.set(3, EquipmentSlots.CREATIVE_Y);
            }
        }
    }

    /** Draw the recessed slot frame + empty-slot placeholder for our two equipment slots (inventory tab only). */
    @Inject(method = "renderBg", at = @At("TAIL"))
    private void inventoryMax$drawEquipFrames(GuiGraphics g, float partialTick,
                                                   int mouseX, int mouseY, CallbackInfo ci) {
        if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY) return;
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) this;
        int leftPos = acc.menuKit$getLeftPos();
        int topPos = acc.menuKit$getTopPos();
        int size = SlotRendering.DEFAULT_SIZE;
        // Frame top-left = item position − 1 (vanilla's 1px slot inset).
        SlotRendering.drawSlotBackground(g, leftPos + EquipmentSlots.CREATIVE_ELYTRA_X - 1,
                topPos + EquipmentSlots.CREATIVE_Y - 1, size, false);
        SlotRendering.drawSlotBackground(g, leftPos + EquipmentSlots.CREATIVE_TOTEM_X - 1,
                topPos + EquipmentSlots.CREATIVE_Y - 1, size, false);
        // Empty-slot placeholders on top of the frames (only when the slot is empty;
        // a filled slot's item is drawn later by the creative screen, on top).
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            if (EquipmentSlots.getElytra(player).isEmpty()) {
                EquipSlotIcons.draw(g, EquipSlotIcons.ELYTRA,
                        leftPos + EquipmentSlots.CREATIVE_ELYTRA_X, topPos + EquipmentSlots.CREATIVE_Y);
            }
            if (EquipmentSlots.getTotem(player).isEmpty()) {
                EquipSlotIcons.draw(g, EquipSlotIcons.TOTEM,
                        leftPos + EquipmentSlots.CREATIVE_TOTEM_X, topPos + EquipmentSlots.CREATIVE_Y);
            }
        }
    }
}
