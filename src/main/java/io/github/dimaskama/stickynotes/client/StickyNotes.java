package io.github.dimaskama.stickynotes.client;

import io.github.dimaskama.stickynotes.config.NotesConfig;
import io.github.dimaskama.stickynotes.integration.IrisIntegration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.Optionull;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class StickyNotes implements ClientModInitializer {
    public static final String MOD_ID = "stickynotes";
    public static final Logger LOGGER = LogManager.getLogger("StickyNotes");
    public static final NotesConfig CONFIG = new NotesConfig("config/stickynotes.json");
    public static final NotesManager NOTES_MANAGER = new NotesManager();
    public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, MOD_ID));
    public static final KeyMapping OPEN_NOTES_LIST_KEY = new KeyMapping("stickynotes.open_list", GLFW.GLFW_KEY_N, KEY_CATEGORY);

    @Override
    public void onInitializeClient() {
        CONFIG.loadOrCreate();
        ClientLifecycleEvents.CLIENT_STOPPING.register(CONFIG::onClientStopping);
        ClientTickEvents.END_CLIENT_TICK.register(NOTES_MANAGER::tick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "notes"), NOTES_MANAGER::renderHud);
        KeyBindingHelper.registerKeyBinding(OPEN_NOTES_LIST_KEY);
        IrisIntegration.init();
    }

    @Nullable
    public static List<Note> getCurrentWorldNotes() {
        return Optionull.map(getCurrentWorldId(), CONFIG.getData().worldToNotes::get);
    }

    @Nullable
    public static String getCurrentWorldId() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null) return null;
        ServerData info = client.getCurrentServer();
        if (info != null) return info.ip + ":" + world.dimension().identifier().toString();
        IntegratedServer integratedServer = client.getSingleplayerServer();
        if (integratedServer == null) return null;
        return integratedServer.getWorldData().getLevelName() + ":" + world.dimension().identifier().toString();
    }
}
