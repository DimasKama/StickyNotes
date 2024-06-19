package io.github.dimaskama.stickynotes.client.screen;

import io.github.dimaskama.stickynotes.client.StickyNotes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class WorldsNotesScreen extends Screen {
    private final Screen parent;
    private WorldsNotesList worldsNotesList;
    private final boolean saveOnExit;

    public WorldsNotesScreen(Screen parent, boolean saveOnExit) {
        super(Text.translatable("stickynotes.world_notes_list"));
        this.parent = parent;
        this.saveOnExit = saveOnExit;
    }

    @Override
    protected void init() {
        worldsNotesList = new WorldsNotesList(client, width, height - 96, 32, this);
        worldsNotesList.init();
        addDrawableChild(worldsNotesList);
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions((width - 250) >> 1, height - 48, 250, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width >>> 1, 12, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        if (saveOnExit) StickyNotes.CONFIG.saveIfDirty(true);
        client.setScreen(parent);
    }
}
