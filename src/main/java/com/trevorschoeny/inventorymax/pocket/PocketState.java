package com.trevorschoeny.inventorymax.pocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.trevorschoeny.inventoryplus.lockedslots.WorldIdentity;
import com.trevorschoeny.inventorymax.InventoryMax;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-side, per-world pocket <b>count</b> state: how many pockets (0–3) are
 * attached to each of the 9 hotbar slots. This is the structure layer of the
 * §0045 fixed-slot composition — the 27 slots always exist server-side; this
 * decides how many of each hotbar slot's 3 are revealed + interactive.
 *
 * <p>Client-side + per-world (mirrors Column Cycler's slot membership): players
 * have different layouts per world, and the count only gates client reveal —
 * the server doesn't track it (rotation/eviction payloads carry the count).
 * Persisted to {@code config/inventorymax/pockets.json}.
 *
 * <p>Pushes counts into the server-safe {@link PocketHoverState} so the slot's
 * reveal predicate sees them.
 */
public final class PocketState {

    private PocketState() {}

    private static final int CURRENT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** worldId → int[9] counts. */
    private static final Map<String, int[]> PER_WORLD = new HashMap<>();
    private static boolean loaded = false;

    private static Path filePath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("inventorymax").resolve("pockets.json");
    }

    public static void load() {
        if (loaded) return;
        loaded = true;
        Path path = filePath();
        if (!Files.exists(path)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            JsonObject perWorld = root.has("perWorld") ? root.getAsJsonObject("perWorld") : new JsonObject();
            for (var e : perWorld.entrySet()) {
                JsonObject w = e.getValue().getAsJsonObject();
                int[] counts = new int[Pockets.HOTBAR_SLOTS];
                if (w.has("counts")) {
                    JsonArray arr = w.getAsJsonArray("counts");
                    for (int i = 0; i < Pockets.HOTBAR_SLOTS && i < arr.size(); i++) {
                        counts[i] = clamp(arr.get(i).getAsInt());
                    }
                }
                PER_WORLD.put(e.getKey(), counts);
            }
            InventoryMax.LOGGER.info("[pockets] loaded counts for {} world(s)", PER_WORLD.size());
        } catch (IOException | JsonSyntaxException | IllegalStateException ex) {
            InventoryMax.LOGGER.error("[pockets] failed to read {} — starting empty", path, ex);
        }
    }

    private static int clamp(int c) {
        return Math.max(0, Math.min(Pockets.MAX_PER_SLOT, c));
    }

    /** Counts for the current world (created on demand). Null if no world id. */
    private static int[] current() {
        String id = WorldIdentity.current(Minecraft.getInstance());
        if (id == null) return null;
        return PER_WORLD.computeIfAbsent(id, k -> new int[Pockets.HOTBAR_SLOTS]);
    }

    public static int count(int hotbar) {
        if (hotbar < 0 || hotbar >= Pockets.HOTBAR_SLOTS) return 0;
        int[] c = current();
        return c == null ? 0 : c[hotbar];
    }

    private static void setCount(int hotbar, int value) {
        int[] c = current();
        if (c == null) return;
        c[hotbar] = clamp(value);
        save();
        PocketHoverState.setCount(hotbar, c[hotbar]);
    }

    /** Grow a pocket panel (+1, capped at 3). At 0 this is the "attach". */
    public static void grow(int hotbar) {
        int c = count(hotbar);
        if (c < Pockets.MAX_PER_SLOT) setCount(hotbar, c + 1);
    }

    /**
     * Shrink a pocket panel (−1). Evicts the removed top pocket's item to the
     * inventory (or drops it) via a server payload, then lowers the count.
     * At count 1, this detaches (→ 0).
     */
    public static void shrink(int hotbar) {
        int c = count(hotbar);
        if (c <= 0) return;
        int newCount = c - 1;
        // Server evicts the now-removed pocket depths [newCount, c).
        ClientPlayNetworking.send(new PocketEvictC2S(hotbar, newCount, c));
        setCount(hotbar, newCount);
    }

    /** Push every hotbar slot's count into the server-safe hover state. */
    public static void pushAll() {
        int[] c = current();
        if (c == null) return;
        for (int n = 0; n < Pockets.HOTBAR_SLOTS; n++) {
            PocketHoverState.setCount(n, c[n]);
        }
    }

    private static void save() {
        Path path = filePath();
        try {
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", CURRENT_VERSION);
            JsonObject perWorld = new JsonObject();
            for (var e : PER_WORLD.entrySet()) {
                int[] counts = e.getValue();
                boolean anyNonZero = false;
                JsonArray arr = new JsonArray();
                for (int v : counts) {
                    arr.add(v);
                    if (v != 0) anyNonZero = true;
                }
                if (!anyNonZero) continue;
                JsonObject w = new JsonObject();
                w.add("counts", arr);
                perWorld.add(e.getKey(), w);
            }
            root.add("perWorld", perWorld);
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException ex) {
            InventoryMax.LOGGER.error("[pockets] failed to write {} — counts won't persist", path, ex);
        }
    }
}
