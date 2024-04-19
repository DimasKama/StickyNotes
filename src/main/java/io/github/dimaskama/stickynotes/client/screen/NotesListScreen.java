package io.github.dimaskama.stickynotes.client.screen;

import io.github.dimaskama.stickynotes.client.Note;
import io.github.dimaskama.stickynotes.client.StickyNotes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NotesListScreen extends Screen {
    @Nullable
    private final Screen parent;
    private NotesListWidget notesListWidget;
    private final List<Note> notes;
    private final boolean saveOnClose;

    public NotesListScreen(@Nullable Screen parent, List<Note> notes, boolean saveOnClose) {
        super(Text.translatable("stickynotes.notes_list"));
        this.parent = parent;
        this.notes = notes;
        this.saveOnClose = saveOnClose;
    }

    @Override
    protected void init() {
        notesListWidget = new NotesListWidget(client, width, height, 32, height - 64, this, notes);
        addDrawableChild(notesListWidget);
        int x = (width - 120 - 10 - 120) >> 1;
        int y = height - 54;
        addDrawableChild(ButtonWidget.builder(Text.translatable("stickynotes.add_note"), button -> {
            Entity camera = client.cameraEntity;
            Note note = new Note(
                    camera != null ? Note.raycastPos(camera) : Vec3d.ZERO,
                    Text.literal("Note " + (notes.size() + 1)),
                    Text.empty(),
                    0,
                    false
            );
            notes.add(note);
            client.setScreen(new NoteEditScreen(this, note, false));
            StickyNotes.CONFIG.markDirty();
        }).dimensions(x, y, 250, 20).build());
        y += 25;
        addDrawableChild(ButtonWidget.builder(Text.translatable("stickynotes.other_worlds_notes"), button -> {
            if (parent instanceof WorldsNotesScreen) {
                close();
            } else {
                client.setScreen(new WorldsNotesScreen(this, false));
            }
        }).dimensions(x, y, 120, 20).build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(x + 120 + 10, y, 120, 20).build());
    }

    @Override
    public void tick() {
        notesListWidget.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackgroundTexture(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width >>> 1, 12, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (saveOnClose) {
            StickyNotes.CONFIG.saveIfDirty(true);
        }
        client.setScreen(parent);
    }
}
