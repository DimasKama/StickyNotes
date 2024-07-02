package io.github.dimaskama.stickynotes.client.screen;

import com.google.common.collect.ImmutableList;
import io.github.dimaskama.stickynotes.client.Note;
import io.github.dimaskama.stickynotes.client.StickyNotes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NotesListWidget extends ElementListWidget<NotesListWidget.Entry> {
    private final Screen screen;
    private final List<Note> notes;

    public NotesListWidget(MinecraftClient client, int width, int height, int y, Screen screen, List<Note> notes) {
        super(client, width, height, y, 24);
        this.screen = screen;
        this.notes = notes;
    }

    public void tick() {
        int entryCount = getEntryCount();
        int size = notes.size();
        if (entryCount < size) {
            for (int i = entryCount; i < size; i++) {
                addEntry(new Entry());
            }
        } else if (size < entryCount) {
            for (int i = size; i < entryCount; i++) {
                remove(size);
            }
        }
    }

    @Override
    public int getRowWidth() {
        return Math.min(400, width);
    }

    public class Entry extends ElementListWidget.Entry<Entry> {
        @Nullable
        private Note note;
        private final ButtonWidget editButton = ButtonWidget.builder(Text.translatable("selectServer.edit"), button -> {
            if (note != null) {
                MinecraftClient.getInstance().setScreen(new NoteEditScreen(screen, note, true));
                StickyNotes.CONFIG.markDirty();
            }
        }).size(40, 16).build();
        private final ButtonWidget deleteButton = ButtonWidget.builder(Text.literal("X"), button -> {
            if (note != null) {
                notes.remove(note);
                StickyNotes.CONFIG.markDirty();
            }
        }).size(16, 16).build();
        private final List<ButtonWidget> children = ImmutableList.of(editButton, deleteButton);

        public Entry() {
            editButton.setWidth(MinecraftClient.getInstance().textRenderer.getWidth(editButton.getMessage()) + 8);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return children;
        }

        @Override
        public List<? extends Element> children() {
            return children;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
            if (index >= notes.size()) return;
            Note note = notes.get(index);
            this.note = note;
            context.fill(x, y, x + entryWidth, y + entryHeight, hovered ? 0x50FFFFFF : 0x20FFFFFF);
            int iconSide = entryHeight - 4;
            Note.draw(context, x + 2, y + 2, iconSide, iconSide, note.icon);
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            context.drawTextWithShadow(
                    textRenderer,
                    note.name,
                    x + 6 + iconSide, y + 6,
                    0xFFFFFFFF
            );
            editButton.setX(x + entryWidth - 2 - deleteButton.getWidth() - 2 - editButton.getWidth());
            editButton.setY(y + ((entryHeight - editButton.getHeight()) >> 1));
            editButton.render(context, mouseX, mouseY, delta);
            deleteButton.setX(x + entryWidth - 2 - deleteButton.getWidth());
            deleteButton.setY(y + ((entryHeight - deleteButton.getHeight()) >> 1));
            deleteButton.render(context, mouseX, mouseY, delta);
            if (
                    hovered
                    && !editButton.isMouseOver(mouseX, mouseY)
                    && !deleteButton.isMouseOver(mouseX, mouseY)
                    && !note.description.getString().isBlank()
            ) {
                context.drawTooltip(textRenderer, note.description, mouseX, mouseY);
            }
            String posText = (int) note.pos.x + " " + (int) note.pos.y + " " + (int) note.pos.z;
            context.drawText(textRenderer, posText, editButton.getX() - 2 - textRenderer.getWidth(posText), y + ((entryHeight - 9) >> 1), 0xFFBBBBBB, false);
        }
    }
}
