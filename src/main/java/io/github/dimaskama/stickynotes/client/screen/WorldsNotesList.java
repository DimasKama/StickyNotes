package io.github.dimaskama.stickynotes.client.screen;

import com.google.common.collect.ArrayListMultimap;
import io.github.dimaskama.stickynotes.client.Note;
import io.github.dimaskama.stickynotes.client.StickyNotes;
import io.github.dimaskama.stickynotes.mixin.ClickableWidgetInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WorldsNotesList extends ElementListWidget<WorldsNotesList.Entry> {
    private final Screen screen;

    public WorldsNotesList(MinecraftClient client, int width, int height, int y, Screen screen) {
        super(client, width, height, y, 32);
        this.screen = screen;
    }

    public void init() {
        clearEntries();

        ArrayListMultimap<String, Note> map = StickyNotes.CONFIG.getData().worldToNotes;

        String currentId = StickyNotes.getCurrentWorldId();
        List<Note> currentList = StickyNotes.getCurrentWorldNotes();
        map.keySet().forEach(key -> addEntry(new Entry(key, map.get(key), currentList == null || key.equals(currentId) ? null : list -> {
            currentList.addAll(list);
            StickyNotes.CONFIG.markDirty();
        })));
    }

    @Override
    public int getRowWidth() {
        return 320;
    }

    public class Entry extends ElementListWidget.Entry<Entry> {
        private final Text keyText;
        private final List<ButtonWidget> buttons;
        private final int buttonsWidth;

        public Entry(String key, List<Note> notes, @Nullable Consumer<List<Note>> copyAction) {
            keyText = Text.literal(key);
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            ArrayList<ButtonWidget> list = new ArrayList<>();
            int widthCount = 0;
            if (copyAction != null) {
                Text copyText = Text.translatable("stickynotes.world_notes_list.copy");
                int copyWidth = textRenderer.getWidth(copyText) + 8;
                ButtonWidget copy = ButtonWidget.builder(copyText, button -> {
                    copyAction.accept(notes);
                }).size(copyWidth, 16).build();
                copy.setTooltip(Tooltip.of(Text.translatable("stickynotes.world_notes_list.copy.tooltip")));
                list.add(copy);
                widthCount += copyWidth + 2;
            }

            Text editText = Text.translatable("stickynotes.world_notes_list.edit");
            int editWidth = textRenderer.getWidth(editText) + 8;
            ButtonWidget editButton = ButtonWidget.builder(editText, button -> {
                MinecraftClient.getInstance().setScreen(new NotesListScreen(screen, notes, false));
            }).size(editWidth, 16).build();
            list.add(editButton);
            widthCount += editWidth + 2;

            Text deleteText = Text.translatable("stickynotes.world_notes_list.delete");
            int deleteWidth = textRenderer.getWidth(deleteText) + 8;
            ButtonWidget deleteButton = ButtonWidget.builder(deleteText, button -> {
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
        public List<? extends Selectable> selectableChildren() {
            return buttons;
        }

        @Override
        public List<? extends Element> children() {
            return buttons;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float delta) {
            int x = getContentX();
            int y = getContentY();
            int entryWidth = getContentWidth();
            int entryHeight = getContentHeight();
            context.fill(x, y, x + entryWidth, y + entryHeight, hovered ? 0x50FFFFFF : 0x20FFFFFF);
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            int textX = x + 8;
            int textY = y + ((entryHeight - 10) >> 1);
            int freeTextWidth = entryWidth - buttonsWidth - 8 - 8;
            if (freeTextWidth >= textRenderer.getWidth(keyText)) {
                context.drawTextWithShadow(
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
            for (ButtonWidget button : buttons) {
                button.setPosition(buttonX, buttonY);
                button.render(context, mouseX, mouseY, delta);
                buttonX += button.getWidth() + 2;
            }
        }
    }
}
