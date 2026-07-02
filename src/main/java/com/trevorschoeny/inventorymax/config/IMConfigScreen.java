package com.trevorschoeny.inventorymax.config;

import com.trevorschoeny.inventoryplus.columncycler.hud.HudMode;
import com.trevorschoeny.inventoryplus.config.IPConfig;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * IM's YACL config screen — the same two-tab story as IP's (Trev, 2026-07-01):
 * <b>Features</b> = every IM feature's master switch + headline choices;
 * <b>Advanced</b> = the cross-feature refinements. Every feature now has an
 * off-switch (Equipment Slots and Container Locks were previously ungated).
 *
 * <p>IM owns its own screen + ModMenu entry (one-way IM→IP dependency);
 * Keybindery auto-appends a Keybinds tab.
 */
public final class IMConfigScreen {

    private IMConfigScreen() {}

    public static Screen create(Screen parent) {
        // ─── Pocket Cycler (Features) ────────────────────────────────────
        Option<Boolean> pockets = booleanOption(
                "Enable Pocket Cycler",
                "Adds server-persistent pocket slots above your hotbar slots. "
                        + "Hover a hotbar slot to reveal its pockets; use +/− to add or remove (up to 3). "
                        + "Cycle with ← / → (rebind under Controls > Inventory Max).",
                true, IMConfig::pocketCyclerEnabled, IMConfig::setPocketCyclerEnabled);
        Option<Boolean> pocketHud = booleanOption(
                "    Show HUD",
                "Show the mini-hotbar overlay for the pocket cycle when on a pocket slot. "
                        + "Shared with Column Cycler's HUD.",
                true,
                () -> IMConfig.pocketHudMode() != HudMode.NONE,
                v -> IMConfig.setPocketHudMode(v ? HudMode.MINI_HOTBAR : HudMode.NONE));

        // ─── Pocket Cycler (Advanced) ────────────────────────────────────
        Option<Boolean> pocketsSupply = booleanOption(
                "Pockets Supply Automation",
                "Let Auto-Restock and Auto Tool Switch pull tools and items out of your "
                        + "pockets. Off: pockets are pure storage — nothing leaves them "
                        + "automatically.",
                true, IMConfig::pocketsSupplyAutomation, IMConfig::setPocketsSupplyAutomation);

        // ─── Equipment Slots (Features) ──────────────────────────────────
        Option<Boolean> equipment = booleanOption(
                "Enable Equipment Slots",
                "Two dedicated slots above your offhand: an elytra slot that lets you glide "
                        + "without giving up your chestplate, and a totem slot that fires if you'd "
                        + "die with no hand totem. Off: the slots hide (items stay stored) and both "
                        + "powers turn off.",
                true, IMConfig::equipmentSlotsEnabled, IMConfig::setEquipmentSlotsEnabled);
        Option<Boolean> equipCue = booleanOption(
                "    Show HUD Cues",
                "Show the small elytra/totem icons beside your hotbar while the slots are filled.",
                true, IMConfig::equipmentHudCue, IMConfig::setEquipmentHudCue);

        // ─── Container Locks (Features) ──────────────────────────────────
        Option<Boolean> locks = booleanOption(
                "Enable Container Locks",
                "Lock slots inside placed containers (chests, barrels, shulkers, hoppers, "
                        + "dispensers) — locked slots resist sorting, move-matching, hoppers, and "
                        + "other players. Off: existing locks stay saved but stop being enforced.",
                true, IMConfig::containerLocksEnabled, IMConfig::setContainerLocksEnabled);

        // ─── Mending (Features) ──────────────────────────────────────────
        Option<Boolean> mendInv = booleanOption(
                "Mend Any Item In Inventory",
                "When you collect XP, a damaged Mending item repairs even while just sitting "
                        + "in your inventory — not only when held or worn. (Equipment slots and "
                        + "pockets always mend, separately.)",
                true, IMConfig::mendInventoryItems, IMConfig::setMendInventoryItems);

        // ─── Availability wiring ─────────────────────────────────────────
        pocketHud.setAvailable(IMConfig.pocketCyclerEnabled());
        pocketsSupply.setAvailable(IMConfig.pocketCyclerEnabled());
        pockets.addListener((opt, val) -> {
            pocketHud.setAvailable(val);
            pocketsSupply.setAvailable(val);
        });
        equipCue.setAvailable(IMConfig.equipmentSlotsEnabled());
        equipment.addListener((opt, val) -> equipCue.setAvailable(val));

        // ─── Tab 1: Features ─────────────────────────────────────────────
        OptionGroup.Builder pocketGroup = OptionGroup.createBuilder()
                .name(Component.literal("Pocket Cycler"))
                .description(OptionDescription.of(Component.literal(
                        "Server-persistent extra pocket slots, cycled like Column Cycler.")))
                .option(pockets)
                .option(pocketHud);
        // Cohabitation flag (per cycle-modes shared rules): running multiple
        // cyclers is allowed — just surface it.
        if (IPConfig.columnCyclerEnabled()) {
            pocketGroup.option(LabelOption.create(Component.literal(
                    "§7Note: Column Cycler is also enabled. Running multiple cyclers "
                            + "is fine — just be mindful of their separate keybinds.")));
        }

        ConfigCategory features = ConfigCategory.createBuilder()
                .name(Component.literal("Features"))
                .group(pocketGroup.build())
                .group(OptionGroup.createBuilder()
                        .name(Component.literal("Equipment Slots"))
                        .description(OptionDescription.of(Component.literal(
                                "Dedicated elytra + totem slots — glide with a chestplate on; survive with a stored totem.")))
                        .option(equipment)
                        .option(equipCue)
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.literal("Container Locks"))
                        .description(OptionDescription.of(Component.literal(
                                "Slot locks for placed containers — the world-side sibling of IP's inventory Locked Slots.")))
                        .option(locks)
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Component.literal("Mending"))
                        .description(OptionDescription.of(Component.literal(
                                "Ambient XP mending across your whole inventory.")))
                        .option(mendInv)
                        .build())
                .build();

        // ─── Tab 2: Advanced ─────────────────────────────────────────────
        ConfigCategory advanced = ConfigCategory.createBuilder()
                .name(Component.literal("Advanced"))
                .group(OptionGroup.createBuilder()
                        .name(Component.literal("Automation Sources"))
                        .description(OptionDescription.of(Component.literal(
                                "What IP's automation (Auto-Restock, Auto Tool Switch) may draw from.")))
                        .option(pocketsSupply)
                        .build())
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Inventory Max"))
                .category(features)
                .category(advanced)
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> booleanOption(
            String name, String description, boolean defaultValue,
            java.util.function.Supplier<Boolean> getter,
            java.util.function.Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(description)))
                .binding(defaultValue, getter, setter)
                .controller(BooleanControllerBuilder::create)
                .build();
    }
}
