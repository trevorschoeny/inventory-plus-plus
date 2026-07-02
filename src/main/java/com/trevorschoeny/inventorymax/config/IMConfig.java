package com.trevorschoeny.inventorymax.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.trevorschoeny.inventorymax.InventoryMax;
import com.trevorschoeny.inventoryplus.columncycler.hud.HudMode;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Global IM config — feature toggles for Pocket Cycler. Mirrors IP's
 * {@code IPConfig} static-field + JSON pattern. Persisted to
 * {@code config/inventorymax/config.json}.
 *
 * <p>IM owns its own config file + ModMenu entry rather than extending IP's
 * (dependency direction is IM→IP; IM storing state in IP's file would invert
 * ownership). Reuses IP's {@link HudMode} enum for the shared cycle HUD.
 *
 * <p>Per-world pocket counts are NOT here — those live per-world in
 * {@code PocketState} (different layouts per world, same as Column Cycler's
 * slot membership).
 */
public final class IMConfig {

    private IMConfig() {}

    private static final int CURRENT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Pocket Cycler master toggle. Default ON — installing IM is itself the
    // opt-in; pockets are a manual feature (not automation), so the
    // conservative-automation-default rule doesn't apply.
    private static boolean pocketCyclerEnabled = true;
    // Shared cycle HUD mode for pockets (None / Mini-hotbar). Default
    // MINI_HOTBAR, matching Column Cycler.
    private static HudMode pocketHudMode = HudMode.MINI_HOTBAR;
    // Pockets as a search source for IP's Auto-Restock + Auto Tool Switch
    // (the HotbarCyclableRegistry seam). Default ON — matches shipped
    // behavior. OFF = pockets are storage only; nothing gets pulled out
    // automatically. Read wherever the cyclable seam runs (client + server).
    private static boolean pocketsSupplyAutomation = true;
    // Equipment Slots (elytra + totem) master toggle. Default ON. OFF hides
    // the slots (content persists, inert — same model as a 0-count pocket
    // column) and disarms the slot behaviors: glide, totem-fires-last,
    // totem auto-restock, HUD cues, shift-click routing. The behavior gates
    // are read where the code runs (client render / server mixins) — in
    // singleplayer both read the same file; on a dedicated server the
    // server's config governs the server-side behaviors (mending pattern).
    private static boolean equipmentSlotsEnabled = true;
    // The elytra/totem HUD cue icons beside the hotbar (sub of Equipment
    // Slots). Default ON — matches shipped behavior.
    private static boolean equipmentHudCue = true;
    // Container Locks master toggle. Default ON. OFF = stored locks stay
    // saved but stop being enforced/shown; flipping back ON re-arms them.
    // Read at the ContainerLocks chokepoints (client UI + server mixins).
    private static boolean containerLocksEnabled = true;
    // "Mend any item in the inventory" — ambient XP mending across the whole
    // vanilla inventory, not just equipped/registered. Default ON (the feature is
    // the point; equip + pocket mending are separate always-on slot opt-ins).
    // Gates only the vanilla-inventory mend provider, read server-side.
    private static boolean mendInventoryItems = true;

    private static boolean loaded = false;

    private static Path filePath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("inventorymax")
                .resolve("config.json");
    }

    public static void load() {
        if (loaded) return;
        loaded = true;
        Path path = filePath();
        if (!Files.exists(path)) {
            InventoryMax.LOGGER.info("[config] no config at {} — using defaults", path);
            return;
        }
        try {
            String json = Files.readString(path);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            pocketCyclerEnabled = readBool(root, "pocketCyclerEnabled", pocketCyclerEnabled);
            pocketHudMode = HudMode.fromName(readString(root, "pocketHudMode", null), pocketHudMode);
            pocketsSupplyAutomation = readBool(root, "pocketsSupplyAutomation", pocketsSupplyAutomation);
            equipmentSlotsEnabled = readBool(root, "equipmentSlotsEnabled", equipmentSlotsEnabled);
            equipmentHudCue = readBool(root, "equipmentHudCue", equipmentHudCue);
            containerLocksEnabled = readBool(root, "containerLocksEnabled", containerLocksEnabled);
            mendInventoryItems = readBool(root, "mendInventoryItems", mendInventoryItems);
            InventoryMax.LOGGER.info("[config] loaded from {}", path);
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            InventoryMax.LOGGER.error("[config] failed to read {} — using defaults", path, e);
        }
    }

    private static boolean readBool(JsonObject root, String key, boolean fallback) {
        return root.has(key) && root.get(key).isJsonPrimitive()
                ? root.get(key).getAsBoolean() : fallback;
    }

    private static String readString(JsonObject root, String key, String fallback) {
        return root.has(key) && root.get(key).isJsonPrimitive()
                ? root.get(key).getAsString() : fallback;
    }

    private static void save() {
        Path path = filePath();
        try {
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", CURRENT_VERSION);
            root.addProperty("pocketCyclerEnabled", pocketCyclerEnabled);
            root.addProperty("pocketHudMode", pocketHudMode.name());
            root.addProperty("pocketsSupplyAutomation", pocketsSupplyAutomation);
            root.addProperty("equipmentSlotsEnabled", equipmentSlotsEnabled);
            root.addProperty("equipmentHudCue", equipmentHudCue);
            root.addProperty("containerLocksEnabled", containerLocksEnabled);
            root.addProperty("mendInventoryItems", mendInventoryItems);
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException e) {
            InventoryMax.LOGGER.error("[config] failed to write {} — changes won't persist", path, e);
        }
    }

    public static boolean pocketCyclerEnabled() { return pocketCyclerEnabled; }
    public static void setPocketCyclerEnabled(boolean v) { pocketCyclerEnabled = v; save(); }

    public static HudMode pocketHudMode() { return pocketHudMode; }
    public static void setPocketHudMode(HudMode v) { pocketHudMode = v; save(); }

    public static boolean pocketsSupplyAutomation() { return pocketsSupplyAutomation; }
    public static void setPocketsSupplyAutomation(boolean v) { pocketsSupplyAutomation = v; save(); }

    public static boolean equipmentSlotsEnabled() { return equipmentSlotsEnabled; }
    public static void setEquipmentSlotsEnabled(boolean v) { equipmentSlotsEnabled = v; save(); }

    public static boolean equipmentHudCue() { return equipmentHudCue; }
    public static void setEquipmentHudCue(boolean v) { equipmentHudCue = v; save(); }

    public static boolean containerLocksEnabled() { return containerLocksEnabled; }
    public static void setContainerLocksEnabled(boolean v) { containerLocksEnabled = v; save(); }

    public static boolean mendInventoryItems() { return mendInventoryItems; }
    public static void setMendInventoryItems(boolean v) { mendInventoryItems = v; save(); }
}
