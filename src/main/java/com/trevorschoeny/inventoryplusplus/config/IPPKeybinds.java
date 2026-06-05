package com.trevorschoeny.inventoryplusplus.config;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import org.lwjgl.glfw.GLFW;

/**
 * IPP's keybinds, registered as vanilla {@link KeyMapping}s so they appear in
 * the Controls menu and are user-rebindable. Mirrors IP's {@code IPKeybinds}.
 *
 * <p>Pocket keybinds live here (in IPP), not in IP's {@code IPKeybinds} — the
 * dependency direction is IPP→IP, so IP must not carry IPP-specific bindings.
 *
 * <p>Pockets reveal on hover (no keybind) and attach/resize via the on-screen
 * +/− buttons, so there's no attach keybind — only cycle.
 *
 * <ul>
 *   <li><b>Cycle Forward / Backward</b> — rotate the active pocket cycle.
 *       Defaults to the <b>→ / ←</b> arrow keys (Trev 2026-06-04), distinct
 *       from Column Cycler's {@code ] [}.</li>
 * </ul>
 */
public final class IPPKeybinds {

    private IPPKeybinds() {}

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("inventoryplusplus", "controls"));

    public static final KeyMapping CYCLE_FORWARD = new KeyMapping(
            "key.inventoryplusplus.pocket_cycle_forward",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, CATEGORY);

    public static final KeyMapping CYCLE_BACKWARD = new KeyMapping(
            "key.inventoryplusplus.pocket_cycle_backward",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, CATEGORY);

    public static void register() {
        KeyBindingHelper.registerKeyBinding(CYCLE_FORWARD);
        KeyBindingHelper.registerKeyBinding(CYCLE_BACKWARD);
    }
}
