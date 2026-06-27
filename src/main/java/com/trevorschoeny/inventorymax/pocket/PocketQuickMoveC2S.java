package com.trevorschoeny.inventorymax.pocket;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client→server request to quick-move a single pocket's content out toward its
 * natural destination — the non-held restock path (a totem refilling the
 * equipment totem slot, a fresh armor piece equipping after a break).
 *
 * <h3>Why server-authoritative</h3>
 *
 * Like rotation ({@link PocketRotateC2S}), this can't be a client-side
 * shift-click. Auto-Restock fires in-world (no screen open), where the client's
 * pocket slots are inert — they reject clicks and report empty. The server's
 * pocket slots are always active, so the server can run the real
 * {@code quickMoveStack} from the pocket slot and let the existing routing
 * (vanilla for armor, {@code InventoryMenuQuickMoveMixin} for the totem/elytra
 * equipment slots) carry the item to the right destination, then
 * {@code broadcastChanges} syncs it back.
 *
 * <p>Held-tool restock does <b>not</b> use this — that's the dynamic-switch
 * (rotate-into-hand) path via {@link PocketCyclable#bringToHotbar}. This is only
 * for slots the player isn't holding (totem, armor, offhand).
 */
public record PocketQuickMoveC2S(int hotbar, int depth) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PocketQuickMoveC2S> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(Pockets.MOD_ID, "pocket_quick_move"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PocketQuickMoveC2S> CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeVarInt(p.hotbar);
                        buf.writeVarInt(p.depth);
                    },
                    buf -> new PocketQuickMoveC2S(buf.readVarInt(), buf.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
