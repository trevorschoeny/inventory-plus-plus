package com.trevorschoeny.inventorymax;

import com.trevorschoeny.inventoryplus.cyclable.HotbarCyclableRegistry;
import com.trevorschoeny.inventoryplus.lockedslots.LockedSlots;
import com.trevorschoeny.inventorymax.config.IMConfig;
import com.trevorschoeny.inventorymax.config.IMKeybinds;
import com.trevorschoeny.inventorymax.containerlocks.ContainerLockProvider;
import com.trevorschoeny.inventorymax.equipment.EquipAutoRestock;
import com.trevorschoeny.inventorymax.equipment.EquipHud;
import com.trevorschoeny.inventorymax.equipment.EquipPixelPanels;
import com.trevorschoeny.inventorymax.pocket.PocketCyclable;
import com.trevorschoeny.inventorymax.pocket.PocketCyclerHudSource;
import com.trevorschoeny.inventorymax.pocket.PocketHover;
import com.trevorschoeny.inventorymax.pocket.PocketInput;
import com.trevorschoeny.inventorymax.pocket.PocketPixelPanels;
import com.trevorschoeny.inventorymax.pocket.PocketState;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Client entrypoint for Inventory Max. Wires Pocket Cycler's client
 * surface: config, keybinds, per-world count state, input dispatch, and the
 * pocket source for IP's shared cycle HUD.
 *
 * <p>The slot mixins (slot construction) activate via {@code mixins.json}; the
 * registered slots' on-screen presentation — render, hover, click, reveal across
 * survival <em>and</em> creative — rides MenuKit's panel pipeline through the
 * pixel-positioned panels registered below (§0057 Revision). The shared HUD
 * panel itself is registered by IP (this just contributes a source into IP's
 * {@code CycleHudRegistry}).
 */
public class InventoryMaxClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        IMConfig.load();
        IMKeybinds.register();
        PocketState.load();
        PocketInput.register();
        // Contribute pockets to IP's shared cycle HUD (the generalization
        // paying off — one HUD, both cyclers).
        PocketCyclerHudSource.register();
        // Register pockets as a hotbar cycler so Auto Tool Switch + Auto-Restock
        // can source tools from them (Tier 2 dynamic switch) — the search/cycle
        // sibling of PocketCyclerHudSource's render contribution.
        HotbarCyclableRegistry.register(PocketCyclable.INSTANCE);
        // Equipment-slot HUD cue — elytra + totem icons to the left of the hotbar.
        EquipHud.register();
        // Presentation: pocket + equip slots ride MenuKit's panel pipeline on
        // pixel-positioned panels (row centered over the hovered hotbar column;
        // equip anchored above the offhand) — correct on survival AND creative
        // from one registration. PocketHover's per-frame tick drives the
        // hover-reveal state the pocket panels' origin suppliers read.
        PocketHover.register();
        PocketPixelPanels.register();
        EquipPixelPanels.register();
        // Auto-restock the totem slot from inventory after a death save (composes
        // IP's auto-restock search; follows the auto-restock toggle).
        ClientTickEvents.END_CLIENT_TICK.register(EquipAutoRestock::tick);

        // Plug Container Locks into IP's client-side lock seam, so IP's unified
        // lock-check / edit UI / icon / sort+move-matching skip recognize placed
        // containers. Client-only: IP's LockedSlots is a client-only class.
        LockedSlots.registerProvider(new ContainerLockProvider());

        InventoryMax.LOGGER.info("[inventorymax] Client init — Pocket Cycler + Container Locks active.");
    }
}
