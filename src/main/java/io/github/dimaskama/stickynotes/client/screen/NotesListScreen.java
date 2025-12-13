package io.github.dimaskama.stickynotes.client.screen;

import io.github.dimaskama.stickynotes.client.Note;
import io.github.dimaskama.stickynotes.client.StickyNotes;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class NotesListScreen extends Screen {
    @Nullable
    private final Screen parent;
    private NotesListWidget notesListWidget;
    private final List<Note> notes;
    private final boolean saveOnClose;

    public NotesListScreen(@Nullable Screen parent, List<Note> notes, boolean saveOnClose) {
        super(Component.translatable("stickynotes.notes_list"));
        this.parent = parent;
        this.notes = notes;
        this.saveOnClose = saveOnClose;
    }

    @Override
    protected void init() {
        notesListWidget = new NotesListWidget(minecraft, width, height - 96, 32, this, notes);
        addRenderableWidget(notesListWidget);
        int x = (width - 120 - 10 - 120) >> 1;
        int y = height - 54;
        addRenderableWidget(Button.builder(Component.translatable("stickynotes.add_note"), button -> {
            Entity camera = minecraft.getCameraEntity();
            Note note = new Note(
                    camera != null ? Note.raycastPos(camera) : Vec3.ZERO,
                    Component.literal("Note " + (notes.size() + 1)),
                    Component.empty(),
                    ResourceLocation.parse("player"),
                    false
            );
            notes.add(note);
            minecraft.setScreen(new NoteEditScreen(this, note, false));
            StickyNotes.CONFIG.markDirty();
        }).bounds(x, y, 250, 20).build());
        y += 25;
        addRenderableWidget(Button.builder(Component.translatable("stickynotes.other_worlds_notes"), button -> {
            if (parent instanceof WorldsNotesScreen) {
                onClose();
            } else {
                minecraft.setScreen(new WorldsNotesScreen(this, false));
            }
        }).bounds(x, y, 120, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(x + 120 + 10, y, 120, 20).build());
    }

    @Override
    public void tick() {
        notesListWidget.tick();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(font, title, width >>> 1, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        if (saveOnClose) {
            StickyNotes.CONFIG.saveIfDirty(true);
        }
        minecraft.setScreen(parent);
    }
}
