package io.github.dimaskama.stickynotes.client.screen;

import io.github.dimaskama.stickynotes.client.Note;
import io.github.dimaskama.stickynotes.client.NotesManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleConsumer;

public class NoteEditScreen extends Screen {
    private static final int WIDTH = 240;
    private static final int HEIGHT = 180;

    private final Screen parent;
    private final Note note;
    private int x;
    private int y;
    private TextFieldWidget nameWidget;
    private TextFieldWidget descWidget;
    private boolean nameDirty;
    private boolean descDirty;
    private boolean posDirty;
    private double noteX, noteY, noteZ;

    public NoteEditScreen(@Nullable Screen parent, Note note, boolean edit) {
        super(Text.translatable(edit ? "stickynotes.edit_note" : "stickynotes.add_note"));
        this.parent = parent;
        this.note = note;
        noteX = note.pos.x;
        noteY = note.pos.y;
        noteZ = note.pos.z;
    }

    @Override
    protected void init() {
        x = (width - WIDTH) >> 1;
        y = (height - HEIGHT) >> 1;
        Text descText = Text.translatable("stickynotes.note_desc");
        int w = textRenderer.getWidth(descText);
        int bX = x + w;
        addDrawableChild(new TextWidget(x + 6, y + 25, w, 9, Text.translatable("stickynotes.note_name"), textRenderer).alignRight());
        nameWidget = new TextFieldWidget(
                textRenderer,
                bX + 12,
                y + 20,
                100,
                20,
                ScreenTexts.EMPTY
        );
        nameWidget.setText(note.name.getString());
        nameWidget.setChangedListener(s -> nameDirty = true);
        addDrawableChild(nameWidget);
        addDrawableChild(new TextWidget(x + 6, y + 50, w, 9, Text.translatable("stickynotes.note_desc"), textRenderer).alignRight());
        descWidget = new TextFieldWidget(
                textRenderer,
                bX + 12,
                y + 45,
                100,
                20,
                ScreenTexts.EMPTY
        );
        descWidget.setText(note.description.getString());
        descWidget.setChangedListener(s -> descDirty = true);
        addDrawableChild(descWidget);
        addDrawableChild(new SliderWidget(
                x + 12, y + 70,
                216, 20,
                Text.translatable("stickynotes.note_icon", note.icon),
                (double) note.icon / (Note.TEXTURES_COUNT - 1)
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Text.translatable("stickynotes.note_icon", note.icon));
            }
            @Override
            protected void applyValue() {
                note.icon = (int) (value * (Note.TEXTURES_COUNT - 1));
            }
        });
        addDrawableChild(new TextWidget(x + 6, y + 100, w, 9, Text.translatable("stickynotes.note_pos"), textRenderer).alignRight());
        int ww = (100 + w) / 3;
        addPosField(bX + 12, y + 95, ww, "X", noteX, d -> noteX = d);
        addPosField(bX + 12 + ww, y + 95, ww, "Y", noteY, d -> noteY = d);
        addPosField(bX + 12 + ww + ww, y + 95, ww, "Z", noteZ, d -> noteZ = d);
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("stickynotes.note_see_through", boolText(note.seeThrough)),
                button -> {
                    note.seeThrough = !note.seeThrough;
                    button.setMessage(Text.translatable("stickynotes.note_see_through", boolText(note.seeThrough)));
                }
        ).dimensions(x + 12, y + 120, 216, 20).build());
        addDrawableChild(ButtonWidget.builder(
                ScreenTexts.DONE,
                button -> close()
        ).dimensions((width - 80) >>> 1, y + HEIGHT - 30, 80, 20).build());
    }

    private Text boolText(boolean bool) {
        return bool ? ScreenTexts.ON : ScreenTexts.OFF;
    }

    private void addPosField(int x, int y, int width, String title, double value, DoubleConsumer consumer) {
        TextFieldWidget field = new TextFieldWidget(
                textRenderer,
                x, y,
                width, 20,
                ScreenTexts.EMPTY
        );
        field.setText(String.format("%.2f", value));
        field.setPlaceholder(Text.literal(title));
        field.setChangedListener(string -> {
            posDirty = true;
            boolean changed = false;
            while (!string.isBlank()) {
                try {
                    consumer.accept(Double.parseDouble(string));
                    if (changed) field.setText(string);
                    return;
                } catch (NumberFormatException e) {
                    string = string.substring(0, string.length() - 1);
                    changed = true;
                }
            }
            if (changed) field.setText(string);
            consumer.accept(0.0);
        });
        addDrawableChild(field);
    }

    @Override
    public void removed() {
        if (nameDirty) note.name = Text.literal(nameWidget.getText());
        if (descDirty) note.description = Text.literal(descWidget.getText());
        if (posDirty) note.pos = new Vec3d(noteX, noteY, noteZ);
        note.update();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xFFFFFFFF);
        context.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF595959);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width >>> 1, y + 6, 0xFFFFFFFF);
        context.drawTexture(
                NotesManager.ICONS_TEXTURE,
                x + WIDTH - 40 - 12,
                y + 12,
                40, 40,
                Note.getIconU(note.icon), Note.getIconV(note.icon),
                Note.ICON_SIDE, Note.ICON_SIDE,
                Note.TEXTURE_SIDE, Note.TEXTURE_SIDE
        );
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
