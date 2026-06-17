package com.trevorschoeny.inventoryplusplus;

import com.trevorschoeny.inventoryplusplus.containerlocks.ContainerLocks;
import com.trevorschoeny.inventoryplusplus.pocket.PocketEvictC2S;
import com.trevorschoeny.inventoryplusplus.pocket.PocketRotateC2S;
import com.trevorschoeny.inventoryplusplus.pocket.PocketServerOps;
import com.trevorschoeny.inventoryplusplus.pocket.Pockets;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (both-sides) entrypoint for Inventory Plus Plus (§0005) — the
 * server-cooperative companion to IP. Universal: the pocket graft runs on the
 * server (to hold + persist content) and the client (to render + interact).
 *
 * <p>v1 ships the first IPP feature: <b>Pocket Cycler</b> — server-persistent
 * pocket slots grafted onto the vanilla inventory via MenuKit's §0045 graft
 * kit. Per §0005, IPP is purely additive over IP.
 */
public class InventoryPlusPlus implements ModInitializer {

    public static final String MOD_ID = "inventoryplusplus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Register the SHARED container-lock channel (§0049). Universal, and
        // must happen before any container menu opens so the slot state is
        // known wherever a lock is read or written.
        ContainerLocks.register();

        // Register the player-attached pocket content storage (27 slots).
        // Must happen on both sides before any InventoryMenu is constructed,
        // so the graft mixin can bind it.
        Pockets.register();

        // C2S payload types — registered on both sides (the codec must be
        // known wherever the payload travels).
        PayloadTypeRegistry.playC2S().register(PocketRotateC2S.TYPE, PocketRotateC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(PocketEvictC2S.TYPE, PocketEvictC2S.CODEC);

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

        LOGGER.info("[inventoryplusplus] Common init — pocket storage + networking registered.");
    }
}
