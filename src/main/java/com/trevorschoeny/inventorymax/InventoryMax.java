package com.trevorschoeny.inventorymax;

import com.trevorschoeny.inventorymax.containerlocks.ContainerLocks;
import com.trevorschoeny.inventorymax.equipment.EquipmentSlots;
import com.trevorschoeny.inventorymax.pocket.PocketEvictC2S;
import com.trevorschoeny.inventorymax.pocket.PocketQuickMoveC2S;
import com.trevorschoeny.inventorymax.pocket.PocketRotateC2S;
import com.trevorschoeny.inventorymax.pocket.PocketServerOps;
import com.trevorschoeny.inventorymax.pocket.Pockets;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (both-sides) entrypoint for Inventory Max (§0005) — the
 * server-cooperative companion to IP. Universal: the pocket graft runs on the
 * server (to hold + persist content) and the client (to render + interact).
 *
 * <p>v1 ships the first IM feature: <b>Pocket Cycler</b> — server-persistent
 * pocket slots grafted onto the vanilla inventory via MenuKit's §0045 graft
 * kit. Per §0005, IM is purely additive over IP.
 */
public class InventoryMax implements ModInitializer {

    public static final String MOD_ID = "inventorymax";
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

        // Register the player-attached equipment content storage (2 slots:
        // elytra + totem). Same constraint — must precede any InventoryMenu
        // construction so the equipment graft mixin can bind it.
        EquipmentSlots.register();

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

        LOGGER.info("[inventorymax] Common init — pocket + equipment storage + networking registered.");
    }
}
