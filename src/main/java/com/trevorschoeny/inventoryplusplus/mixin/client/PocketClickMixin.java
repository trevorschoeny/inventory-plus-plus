package com.trevorschoeny.inventoryplusplus.mixin.client;

import com.trevorschoeny.inventoryplusplus.config.IPPConfig;
import com.trevorschoeny.inventoryplusplus.pocket.PocketButtons;
import com.trevorschoeny.inventoryplusplus.pocket.PocketHoverState;
import com.trevorschoeny.inventoryplusplus.pocket.Pockets;
import com.trevorschoeny.inventoryplusplus.pocket.PocketState;
import com.trevorschoeny.menukit.core.MenuKitGraftInput;
import com.trevorschoeny.menukit.mixin.AbstractContainerScreenAccessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pocket input routing on the inventory screen — two concerns, both on
 * {@link AbstractContainerScreen}:
 *
 * <ol>
 *   <li><b>+/− resize buttons</b> ({@code mouseClicked}) — a click on a revealed
 *       pocket's +/− button performs the resize and consumes the click.</li>
 *   <li><b>§0046 graft input opacity</b> — the consumer half of MenuKit's
 *       {@link MenuKitGraftInput}. Vanilla appends grafted slots <em>last</em>,
 *       so {@code getHoveredSlot} resolves to the vanilla slot <em>behind</em> a
 *       revealed pocket — the click falls through. Two hooks close that:
 *       <ul>
 *         <li>{@code getHoveredSlot} → a revealed pocket <b>wins</b> hover,
 *             tooltip and the click target over the slot it covers;</li>
 *         <li>{@code mouseClicked} → a click in the panel's <em>empty</em> space
 *             is eaten so a carried item can't drop through to the vanilla slot
 *             behind it.</li>
 *       </ul></li>
 * </ol>
 *
 * <p>The button check MUST run before the covered-click guard. Injector order
 * across separate mixins is undefined, so both live in the one
 * {@code mouseClicked} injector where order is explicit (buttons first, guard
 * last). This composes with MenuKit's own §0037 {@code getHoveredSlot} hover
 * mixin: a grafted panel isn't in MenuKit's panel registry, so the two never
 * claim the same point.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class PocketClickMixin {

    /**
     * §0046, hover/click half: grafted pockets win the slot resolution over the
     * vanilla slot they cover. {@link MenuKitGraftInput#resolveHoveredSlot}
     * skips inert (un-revealed) pockets, so this only fires for a revealed
     * column — and returns {@code null} for a point inside a revealed panel but
     * between slots (covered vanilla slot is inert, §0037).
     */
    @Inject(method = "getHoveredSlot", at = @At("HEAD"), cancellable = true)
    private void inventoryplusplus$preferGraft(double mouseX, double mouseY,
                                               CallbackInfoReturnable<Slot> cir) {
        if (!((Object) this instanceof InventoryScreen)) return;
        if (!IPPConfig.pocketCyclerEnabled()) return;
        MenuKitGraftInput.Resolution r = MenuKitGraftInput.resolveHoveredSlot(
                (AbstractContainerScreen<?>) (Object) this, mouseX, mouseY);
        if (r.handled()) cir.setReturnValue(r.slot()); // grafted slot, or null for an in-panel gap
    }

    /**
     * Click routing: +/− resize buttons first (consume), then the §0046
     * covered-click guard. Over a revealed pocket <em>slot</em> we let vanilla
     * proceed — {@code getHoveredSlot} (above) now routes the click to the
     * grafted slot. Over revealed panel <em>empty space</em> we eat the click so
     * a carried item can't fall through to the vanilla slot behind.
     */
    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
            at = @At("HEAD"), cancellable = true)
    private void inventoryplusplus$pocketClick(MouseButtonEvent event, boolean doubleClick,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof InventoryScreen)) return;
        if (!IPPConfig.pocketCyclerEnabled()) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
        int leftPos = acc.menuKit$getLeftPos();
        int topPos = acc.menuKit$getTopPos();
        double mouseX = event.x();
        double mouseY = event.y();

        // ── 1) +/− resize buttons (only meaningful when a column is revealed) ──
        // Buttons are always drawn (grayed at their limit). Consume the click on
        // a button rect regardless; act only when the limit allows.
        int rev = PocketHoverState.revealedHotbar();
        if (rev >= 0) {
            int c = PocketState.count(rev);
            if (PocketButtons.inRect(mouseX, mouseY, PocketButtons.plusRect(leftPos, topPos, rev))) {
                if (c < Pockets.MAX_PER_SLOT) PocketState.grow(rev); // grayed at max → no-op
                cir.setReturnValue(true);
                return;
            } else if (PocketButtons.inRect(mouseX, mouseY, PocketButtons.minusRect(leftPos, topPos, rev))) {
                if (c > 0) PocketState.shrink(rev); // grayed at 0 → no-op
                cir.setReturnValue(true);
                return;
            }
        }

        // ── 2) §0046 covered-click guard ──
        // resolveHoveredSlot self-gates on inert pockets: if nothing is revealed
        // it returns PASS and we do nothing. Over a revealed slot it reports the
        // slot (non-null) → we DON'T eat, letting vanilla route via the grafted
        // getHoveredSlot. Only an in-panel gap (handled + null slot) is eaten.
        MenuKitGraftInput.Resolution r = MenuKitGraftInput.resolveHoveredSlot(screen, mouseX, mouseY);
        if (r.handled() && r.slot() == null) cir.setReturnValue(true);
    }
}
