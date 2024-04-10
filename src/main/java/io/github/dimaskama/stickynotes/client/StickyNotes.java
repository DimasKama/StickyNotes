package io.github.dimaskama.stickynotes.client;

import io.github.dimaskama.stickynotes.config.NotesConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

public class StickyNotes implements ClientModInitializer {
    public static final String MOD_ID = "stickynotes";
    public static final Logger LOGGER = LogManager.getLogger("StickyNotes");
    public static final NotesConfig CONFIG = new NotesConfig("config/stickynotes.json");
    public static final NotesManager NOTES_MANAGER = new NotesManager();
    public static final KeyBinding OPEN_NOTES_LIST_KEY = new KeyBinding("stickynotes.open_list", GLFW.GLFW_KEY_N, MOD_ID);

    @Override
    public void onInitializeClient() {
        CONFIG.loadOrCreate();
        ClientLifecycleEvents.CLIENT_STOPPING.register(CONFIG::onClientStopping);
        ClientTickEvents.END_CLIENT_TICK.register(NOTES_MANAGER::tick);
        WorldRenderEvents.LAST.register(NOTES_MANAGER::renderWorld);
        HudRenderCallback.EVENT.register(NOTES_MANAGER::renderHud);
        KeyBindingHelper.registerKeyBinding(OPEN_NOTES_LIST_KEY);
    }
}
