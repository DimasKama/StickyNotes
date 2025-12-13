package io.github.dimaskama.stickynotes.client.screen;

import com.google.common.collect.ArrayListMultimap;
import io.github.dimaskama.stickynotes.client.Note;
import io.github.dimaskama.stickynotes.client.StickyNotes;
import io.github.dimaskama.stickynotes.mixin.ClickableWidgetInvoker;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class WorldsNotesList extends ContainerObjectSelectionList<WorldsNotesList.Entry> {
    private final Screen screen;

    public WorldsNotesList(Minecraft client, int width, int height, int y, Screen screen) {
        super(client, width, height, y, 32);
        this.screen = screen;
    }

    public void init() {
        clearEntries();

        ArrayListMultimap<String, Note> map = StickyNotes.CONFIG.getData().worldToNotes;

        String currentId = StickyNotes.getCurrentWorldId();
        List<Note> currentList = StickyNotes.getCurrentWorldNotes();
        map.keySet().forEach(key -> addEntry(new io.github.dimaskama.stickynotes.client.screen.WorldsNotesList.Entry(key, map.get(key), currentList == null || key.equals(currentId) ? null : list -> {
            currentList.addAll(list);
            StickyNotes.CONFIG.markDirty();
        })));
    }

    @Override
    public int getRowWidth() {
        return 320;
    }

    public class Entry extends ContainerObjectSelectionList.Entry<io.github.dimaskama.stickynotes.client.screen.WorldsNotesList.Entry> {
        private final Component keyText;
        private final List<Button> buttons;
        private final int buttonsWidth;

        public Entry(String key, List<Note> notes, @Nullable Consumer<List<Note>> copyAction) {
            keyText = Component.literal(key);
            Font textRenderer = Minecraft.getInstance().font;
            ArrayList<Button> list = new ArrayList<>();
            int widthCount = 0;
            if (copyAction != null) {
                Component copyText = Component.translatable("stickynotes.world_notes_list.copy");
                int copyWidth = textRenderer.width(copyText) + 8;
                Button copy = Button.builder(copyText, button -> {
                    copyAction.accept(notes);
                }).size(copyWidth, 16).build();
                copy.setTooltip(Tooltip.create(Component.translatable("stickynotes.world_notes_list.copy.tooltip")));
                list.add(copy);
                widthCount += copyWidth + 2;
            }

            Component editText = Component.translatable("stickynotes.world_notes_list.edit");
            int editWidth = textRenderer.width(editText) + 8;
            Button editButton = Button.builder(editText, button -> {
                Minecraft.getInstance().setScreen(new NotesListScreen(screen, notes, false));
            }).size(editWidth, 16).build();
            list.add(editButton);
            widthCount += editWidth + 2;

            Component deleteText = Component.translatable("stickynotes.world_notes_list.delete");
            int deleteWidth = textRenderer.width(deleteText) + 8;
            Button deleteButton = Button.builder(deleteText, button -> {
                notes.clear();
                StickyNotes.CONFIG.markDirty();
                init();
            }).size(deleteWidth, 16).build();
            list.add(deleteButton);
            widthCount += deleteWidth + 2;
            buttons = List.copyOf(list);
            buttonsWidth = widthCount;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return buttons;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return buttons;
        }

        @Override
        public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
            context.fill(x, y, x + entryWidth, y + entryHeight, hovered ? 0x50FFFFFF : 0x20FFFFFF);
            Font textRenderer = Minecraft.getInstance().font;
            int textX = x + 8;
            int textY = y + ((entryHeight - 10) >> 1);
            int freeTextWidth = entryWidth - buttonsWidth - 8 - 8;
            if (freeTextWidth >= textRenderer.width(keyText)) {
                context.drawString(
                        textRenderer,
                        keyText,
                        textX, textY,
                        0xFFFFFFFF
                );
            } else {
                ClickableWidgetInvoker.stickynotes_drawScrollableText(
                        context,
                        textRenderer,
                        keyText,
                        textX, textY,
                        textX + freeTextWidth, textY + 10,
                        0xFFFFFFFF
                );
            }

            int buttonX = x + entryWidth - buttonsWidth;
            int buttonY = y + ((entryHeight - 16) >> 1);
            for (Button button : buttons) {
                button.setPosition(buttonX, buttonY);
                button.render(context, mouseX, mouseY, delta);
                buttonX += button.getWidth() + 2;
            }
        }
    }
}
