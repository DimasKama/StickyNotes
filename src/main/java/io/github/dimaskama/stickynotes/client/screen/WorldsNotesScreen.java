package io.github.dimaskama.stickynotes.client.screen;

import io.github.dimaskama.stickynotes.client.StickyNotes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class WorldsNotesScreen extends Screen {
    private final Screen parent;
    private WorldsNotesList worldsNotesList;
    private final boolean saveOnExit;

    public WorldsNotesScreen(Screen parent, boolean saveOnExit) {
        super(Component.translatable("stickynotes.world_notes_list"));
        this.parent = parent;
        this.saveOnExit = saveOnExit;
    }

    @Override
    protected void init() {
        worldsNotesList = new WorldsNotesList(minecraft, width, height - 96, 32, this);
        worldsNotesList.init();
        addRenderableWidget(worldsNotesList);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((width - 250) >> 1, height - 48, 250, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(font, title, width >>> 1, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        if (saveOnExit) StickyNotes.CONFIG.saveIfDirty(true);
        minecraft.setScreen(parent);
    }
}
