package com.trevorschoeny.inventorymax;

import com.trevorschoeny.inventorymax.config.IMConfig;
import com.trevorschoeny.inventorymax.containerlocks.ContainerLocks;
import com.trevorschoeny.inventorymax.equipment.EquipmentSlots;
import com.trevorschoeny.inventorymax.mending.InventoryMendingProvider;
import com.trevorschoeny.inventorymax.pocket.PocketEvictC2S;
import com.trevorschoeny.inventorymax.pocket.PocketQuickMoveC2S;
import com.trevorschoeny.inventorymax.pocket.PocketRotateC2S;
import com.trevorschoeny.inventorymax.pocket.PocketServerOps;
import com.trevorschoeny.inventorymax.pocket.Pockets;
import com.trevorschoeny.menukit.core.MendingCandidates;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (both-sides) entrypoint for Inventory Max (§0005) — the
 * server-cooperative companion to IP. Universal: the pocket slot runs on the
 * server (to hold + persist content) and the client (to render + interact).
 *
 * <p>v1 ships the first IM feature: <b>Pocket Cycler</b> — server-persistent
 * pocket slots registered onto the vanilla inventory via MenuKit's §0045 slot
 * kit. Per §0005, IM is purely additive over IP.
 */
public class InventoryMax implements ModInitializer {

    public static final String MOD_ID = "inventorymax";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Load the config on BOTH sides (idempotent — the client entrypoint
        // calls it too). The server-read toggles (inventory mending, equipment
        // behaviors, container-lock enforcement) otherwise silently fall to
        // defaults on a dedicated server. Server-safe: IMConfig's only IP
        // type is HudMode, a plain enum.
        IMConfig.load();

        // Register the SHARED container-lock channel (§0049). Universal, and
        // must happen before any container menu opens so the slot state is
        // known wherever a lock is read or written.
        ContainerLocks.register();

        // Register the player-attached pocket content storage (27 slots).
        // Must happen on both sides before any InventoryMenu is constructed,
        // so the slot mixin can bind it.
        Pockets.register();

        // Register the player-attached equipment content storage (2 slots:
        // elytra + totem). Same constraint — must precede any InventoryMenu
        // construction so the equipment slot mixin can bind it.
        EquipmentSlots.register();

        // Declare the slots' server behavior by address (behavior-by-address; the
        // creation mixins build behavior-free). Pockets opt into MENDING; the elytra
        // and totem slots get accept-filter GATING + Curse-of-Binding + MENDING.
        Pockets.declareSlotBehavior();
        EquipmentSlots.declareSlotBehavior();

        // C2S payload types — registered on both sides (the codec must be
        // known wherever the payload travels).
        PayloadTypeRegistry.playC2S().register(PocketRotateC2S.TYPE, PocketRotateC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(PocketEvictC2S.TYPE, PocketEvictC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(PocketQuickMoveC2S.TYPE, PocketQuickMoveC2S.CODEC);

        // Server-side receivers — perform the authoritative pocket mutation
        // on the server thread (hop off the network thread via server.execute).
        ServerPlayNetworking.registerGlobalReceiver(PocketRotateC2S.TYPE, (payload, context) -> {
            ServerPlayer sp = context.player();
            MinecraftServer server = sp.level().getServer();
            if (server != null) {
                server.execute(() -> PocketServerOps.rotate(
                        sp, payload.hotbar(), payload.count(), payload.forward()));
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(PocketEvictC2S.TYPE, (payload, context) -> {
            ServerPlayer sp = context.player();
            MinecraftServer server = sp.level().getServer();
            if (server != null) {
                server.execute(() -> PocketServerOps.evict(
                        sp, payload.hotbar(), payload.fromDepth(), payload.toDepth()));
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(PocketQuickMoveC2S.TYPE, (payload, context) -> {
            ServerPlayer sp = context.player();
            MinecraftServer server = sp.level().getServer();
            if (server != null) {
                server.execute(() -> PocketServerOps.quickMove(
                        sp, payload.hotbar(), payload.depth()));
            }
        });

        // "Mend any item in the inventory" — feed the vanilla inventory into MKC's
        // XP-repair pool via the mending primitive's consumer hook (gated by the
        // IMConfig toggle, default on). Equip slots + pockets opt in separately at
        // the slot level.
        MendingCandidates.register(InventoryMendingProvider.INSTANCE);

        LOGGER.info("[inventorymax] Common init — pocket + equipment storage + networking + mending registered.");
    }
}
