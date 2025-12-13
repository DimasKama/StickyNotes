package io.github.dimaskama.stickynotes.client.screen;

import com.google.common.collect.ImmutableList;
import io.github.dimaskama.stickynotes.client.Note;
import io.github.dimaskama.stickynotes.client.StickyNotes;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NotesListWidget extends ContainerObjectSelectionList<NotesListWidget.Entry> {
    private final Screen screen;
    private final List<Note> notes;

    public NotesListWidget(Minecraft client, int width, int height, int y, Screen screen, List<Note> notes) {
        super(client, width, height, y, 24);
        this.screen = screen;
        this.notes = notes;
    }

    public void tick() {
        int entryCount = getItemCount();
        int size = notes.size();
        if (entryCount < size) {
            for (int i = entryCount; i < size; i++) {
                addEntry(new io.github.dimaskama.stickynotes.client.screen.NotesListWidget.Entry());
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

    public class Entry extends ContainerObjectSelectionList.Entry<io.github.dimaskama.stickynotes.client.screen.NotesListWidget.Entry> {
        @Nullable
        private Note note;
        private final Button editButton = Button.builder(Component.translatable("selectServer.edit"), button -> {
            if (note != null) {
                Minecraft.getInstance().setScreen(new NoteEditScreen(screen, note, true));
                StickyNotes.CONFIG.markDirty();
            }
        }).size(40, 16).build();
        private final Button deleteButton = Button.builder(Component.literal("X"), button -> {
            if (note != null) {
                notes.remove(note);
                StickyNotes.CONFIG.markDirty();
            }
        }).size(16, 16).build();
        private final List<Button> children = ImmutableList.of(editButton, deleteButton);

        public Entry() {
            editButton.setWidth(Minecraft.getInstance().font.width(editButton.getMessage()) + 8);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return children;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return children;
        }

        @Override
        public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
            if (index >= notes.size()) return;
            Note note = notes.get(index);
            this.note = note;
            context.fill(x, y, x + entryWidth, y + entryHeight, hovered ? 0x50FFFFFF : 0x20FFFFFF);
            int iconSide = entryHeight - 4;
            Note.draw(context, x + 2, y + 2, iconSide, iconSide, note.icon);
            Font textRenderer = Minecraft.getInstance().font;
            context.drawString(
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
                context.setTooltipForNextFrame(textRenderer, note.description, mouseX, mouseY);
            }
            String posText = (int) note.pos.x + " " + (int) note.pos.y + " " + (int) note.pos.z;
            context.drawString(textRenderer, posText, editButton.getX() - 2 - textRenderer.width(posText), y + ((entryHeight - 9) >> 1), 0xFFBBBBBB, false);
        }
    }
}
