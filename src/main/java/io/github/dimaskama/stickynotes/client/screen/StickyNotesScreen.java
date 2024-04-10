package io.github.dimaskama.stickynotes.client.screen;

import io.github.dimaskama.stickynotes.client.Note;
import io.github.dimaskama.stickynotes.client.StickyNotes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StickyNotesScreen extends Screen {
    private final Screen parent;
    private NotesListWidget notesListWidget;

    public StickyNotesScreen(@Nullable Screen parent) {
        super(Text.translatable("stickynotes.notes_list"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        notesListWidget = new NotesListWidget(client, width, height, 32, height - 64, this);
        notesListWidget.init();
        addDrawableChild(notesListWidget);
        int x = (width - 150 - 10 - 150) >> 1;
        int y = height - 40;
        addDrawableChild(ButtonWidget.builder(Text.translatable("stickynotes.add_note"), button -> {
            List<Note> notes = StickyNotes.CONFIG.getData().notes;
            Note note = new Note(
                    Note.raycastPos(client.cameraEntity),
                    Text.literal("Note " + (notes.size() + 1)),
                    Text.empty(),
                    0,
                    false
            );
            notes.add(note);
            client.setScreen(new NoteEditScreen(this, note, false));
            StickyNotes.CONFIG.markDirty();
        }).dimensions(x, y, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(x + 150 + 10, y, 150, 20).build());
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
        StickyNotes.CONFIG.saveIfDirty(true);
        client.setScreen(parent);
    }
}
