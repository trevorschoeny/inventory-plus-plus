package com.trevorschoeny.inventoryplusplus.mixin.client;

import com.trevorschoeny.inventoryplusplus.config.IPPConfig;
import com.trevorschoeny.inventoryplusplus.pocket.PocketHover;
import com.trevorschoeny.inventoryplusplus.pocket.PocketPanelRender;
import com.trevorschoeny.inventoryplusplus.pocket.PocketRow;
import com.trevorschoeny.menukit.core.MenuKitGraftRender;
import com.trevorschoeny.menukit.mixin.AbstractContainerScreenAccessor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client render mixin for the inventory screen's grafted slots.
 * {@link InventoryScreen} renders through {@link AbstractRecipeBookScreen}, so
 * this targets that class (same injection MenuKit uses for recipe-book-hosted
 * panels). Only the inventory screen carries grafts; others are skipped.
 *
 * <p><b>The grafted-slot render is feature-agnostic.</b>
 * {@link MenuKitGraftRender#renderGraftedSlots} draws <em>every</em> revealed
 * grafted {@code MenuKitSlot} on the menu — pockets, equipment slots, and any
 * future graft. So that call runs <b>unconditionally</b> (whenever the inventory
 * screen is open), while the Pocket-Cycler-specific decoration (hover/reveal,
 * row reposition, panel backing, +/− buttons) stays gated on the pocket toggle.
 * Lifting the shared render out from behind the pocket flag is what lets the
 * equipment slots present even with the Pocket Cycler turned off.
 *
 * <ul>
 *   <li><b>HEAD</b> — (pockets only) update the hover/reveal state before slots
 *       render, so the grafted pockets' inertness reflects the current hover.</li>
 *   <li><b>AFTER renderContents</b> — (pockets only) panel backing; then
 *       <em>always</em> MenuKit's grafted slot frames + items; then (pockets
 *       only) the +/− buttons on top.</li>
 * </ul>
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class PocketRenderMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void inventoryplusplus$updatePocketHover(GuiGraphics g, int mouseX, int mouseY,
                                                     float partialTick, CallbackInfo ci) {
        if (!((Object) this instanceof InventoryScreen)) return;
        // Pocket-specific: when the cycler is off we must NOT update hover —
        // otherwise a non-empty saved pocket count would reveal pockets that the
        // now-unconditional grafted-slot render would then draw.
        if (!IPPConfig.pocketCyclerEnabled()) return;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
        PocketHover.updateHover(acc.menuKit$getLeftPos(), acc.menuKit$getTopPos(), mouseX, mouseY);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderContents(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void inventoryplusplus$renderPockets(GuiGraphics g, int mouseX, int mouseY,
                                                 float partialTick, CallbackInfo ci) {
        if (!((Object) this instanceof InventoryScreen)) return;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
        int leftPos = acc.menuKit$getLeftPos();
        int topPos = acc.menuKit$getTopPos();

        // Pocket-specific decoration is gated on the cycler toggle; the grafted-
        // slot render itself is not (it draws all grafts — pockets + equipment +
        // any future graft — so it must run whether or not pockets are on).
        boolean pockets = IPPConfig.pocketCyclerEnabled();
        if (pockets) {
            // §0047: move the revealed pockets into the centered horizontal row
            // BEFORE the grafted slots draw (and before input hit-tests them, on
            // later click frames). Render + clicks both read graftX/Y.
            PocketRow.reposition(screen.getMenu());
            PocketPanelRender.drawBackground(g, leftPos, topPos);
        }
        MenuKitGraftRender.renderGraftedSlots(screen, g, mouseX, mouseY);
        if (pockets) {
            PocketPanelRender.drawButtons(g, leftPos, topPos, mouseX, mouseY);
        }
    }
}
