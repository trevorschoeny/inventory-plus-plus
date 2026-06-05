package com.trevorschoeny.inventoryplusplus.config;

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
 * IPP's YACL config screen — one <b>IPP</b> tab (currently just Pocket
 * Cycler). Mirrors IP's {@code IPConfigScreen} patterns. IPP owns its own
 * screen + ModMenu entry (the one-way IPP→IP dependency); Keybindery
 * auto-appends a Keybinds tab. Unifying IP + IPP into one screen is deferred
 * (Trev 2026-06-04).
 */
public final class IPPConfigScreen {

    private IPPConfigScreen() {}

    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Inventory Plus Plus"))
                .category(pocketCyclerCategory())
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory pocketCyclerCategory() {
        Option<Boolean> enabled = booleanOption(
                "Enable Pocket Cycler",
                "Adds server-persistent pocket slots above your hotbar slots. "
                        + "Hover a hotbar slot to reveal its pockets; use +/− to add or remove (up to 3).",
                true,
                IPPConfig::pocketCyclerEnabled,
                IPPConfig::setPocketCyclerEnabled);

        Option<Boolean> showHud = booleanOption(
                "    Show HUD",
                "Show the mini-hotbar overlay for the pocket cycle when on a pocket slot. "
                        + "Shared with Column Cycler's HUD.",
                true,
                () -> IPPConfig.pocketHudMode() != HudMode.NONE,
                v -> IPPConfig.setPocketHudMode(v ? HudMode.MINI_HOTBAR : HudMode.NONE));

        showHud.setAvailable(IPPConfig.pocketCyclerEnabled());
        enabled.addListener((opt, val) -> showHud.setAvailable(val));

        OptionGroup.Builder group = OptionGroup.createBuilder()
                .name(Component.literal("Pocket Cycler"))
                .description(OptionDescription.of(Component.literal(
                        "Server-persistent extra pocket slots, cycled like Column Cycler.")))
                .option(enabled)
                .option(showHud);

        // Cohabitation flag (per cycle-modes shared rules): inform the player
        // when Column Cycler is also on — running multiple cyclers is allowed
        // but the UX-confusion risk is worth surfacing.
        if (IPConfig.columnCyclerEnabled()) {
            group.option(LabelOption.create(Component.literal(
                    "§7Note: Column Cycler is also enabled. Running multiple cyclers "
                            + "is fine — just be mindful of their separate keybinds.")));
        }

        return ConfigCategory.createBuilder()
                .name(Component.literal("IPP"))
                .group(group.build())
                .build();
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
