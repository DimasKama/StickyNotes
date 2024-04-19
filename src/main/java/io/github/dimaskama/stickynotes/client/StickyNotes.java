package io.github.dimaskama.stickynotes.client;

import io.github.dimaskama.stickynotes.config.NotesConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Nullables;
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
    public static final KeyBinding OPEN_NOTES_LIST_KEY = new KeyBinding("stickynotes.open_list", GLFW.GLFW_KEY_N, MOD_ID);

    @Override
    public void onInitializeClient() {
        CONFIG.loadOrCreate();
        ClientLifecycleEvents.CLIENT_STOPPING.register(CONFIG::onClientStopping);
        ClientTickEvents.END_CLIENT_TICK.register(NOTES_MANAGER::tick);
        WorldRenderEvents.BEFORE_ENTITIES.register(NOTES_MANAGER::renderBeforeTranslucent);
        WorldRenderEvents.LAST.register(NOTES_MANAGER::renderLast);
        HudRenderCallback.EVENT.register(NOTES_MANAGER::renderHud);
        KeyBindingHelper.registerKeyBinding(OPEN_NOTES_LIST_KEY);
    }

    @Nullable
    public static List<Note> getCurrentWorldNotes() {
        return Nullables.map(getCurrentWorldId(), CONFIG.getData().worldToNotes::get);
    }

    @Nullable
    public static String getCurrentWorldId() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) return null;
        ServerInfo info = client.getCurrentServerEntry();
        if (info != null) return info.address + ":" + world.getRegistryKey().getValue().toString();
        IntegratedServer integratedServer = client.getServer();
        if (integratedServer == null) return null;
        return integratedServer.getSaveProperties().getLevelName() + ":" + world.getRegistryKey().getValue().toString();
    }
}
