package io.github.dimaskama.stickynotes.client.screen;

import io.github.dimaskama.stickynotes.client.Note;
import io.github.dimaskama.stickynotes.client.NotesManager;
import io.github.dimaskama.stickynotes.client.StickyNotes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.entity.Entity;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleConsumer;

public class NoteEditScreen extends Screen {
    private static final int WIDTH = 270;

    private final Screen parent;
    private final Note note;
    private TextFieldWidget nameWidget;
    private TextFieldWidget descWidget;
    private boolean nameDirty;
    private boolean descDirty;
    private boolean posDirty;
    private double noteX, noteY, noteZ;
    private int hoveredIcon = -1;

    public NoteEditScreen(@Nullable Screen parent, Note note, boolean edit) {
        super(Text.translatable(edit ? "stickynotes.edit_note" : "stickynotes.add_note"));
        this.parent = parent;
        this.note = note;
    }

    @Override
    protected void init() {
        noteX = note.pos.x;
        noteY = note.pos.y;
        noteZ = note.pos.z;

        int left = (width - WIDTH) >> 1;
        int top = height >>> 2;

        Text nameText = Text.translatable("stickynotes.note_name");
        int nameWidth = textRenderer.getWidth(nameText);
        addDrawableChild(new TextWidget(left, top + 5, nameWidth, 9, nameText, textRenderer));
        nameWidget = new TextFieldWidget(
                textRenderer,
                left + nameWidth + 4,
                top,
                WIDTH - (nameWidth + 4),
                20,
                ScreenTexts.EMPTY
        );
        nameWidget.setText(note.name.getString());
        nameWidget.setChangedListener(s -> nameDirty = true);
        addDrawableChild(nameWidget);

        Text descText = Text.translatable("stickynotes.note_desc");
        int descWidth = textRenderer.getWidth(descText);
        addDrawableChild(new TextWidget(left, top + 25 + 5, descWidth, 9, descText, textRenderer));
        descWidget = new TextFieldWidget(
                textRenderer,
                left + descWidth + 4,
                top + 25,
                WIDTH - (descWidth + 4),
                20,
                ScreenTexts.EMPTY
        );
        descWidget.setText(note.description.getString());
        descWidget.setMaxLength(256);
        descWidget.setChangedListener(s -> descDirty = true);
        addDrawableChild(descWidget);

        for (int i = 0; i < Note.TEXTURES_COUNT; i++) {
            addDrawableChild(new IconButton(left + 10 * i, top + 50, 10, i));
        }

        Text posText = Text.translatable("stickynotes.note_pos");
        int posWidth = textRenderer.getWidth(posText);
        addDrawableChild(new TextWidget(left, top + 65 + 5, posWidth, 9, posText, textRenderer));
        int ww = (WIDTH - posWidth - 4 - 50) / 3;
        addPosField(left + posWidth + 4, top + 65, ww, "X", noteX, d -> noteX = d);
        addPosField(left + posWidth + 4 + ww, top + 65, ww, "Y", noteY, d -> noteY = d);
        addPosField(left + posWidth + 4 + ww + ww, top + 65, ww, "Z", noteZ, d -> noteZ = d);

        boolean inWorld = client.cameraEntity != null;

        SquareButton moveToPlayerButton = new SquareButton(left + WIDTH - 45, top + 65, 0, () -> {
            Entity camera = client.cameraEntity;
            if (camera == null) return;
            note.pos = camera.getPos();
            clearAndInit();
        });
        moveToPlayerButton.active = inWorld;
        if (inWorld) moveToPlayerButton.setTooltip(Tooltip.of(Text.translatable("stickynotes.move_to_player")));
        addDrawableChild(moveToPlayerButton);

        SquareButton moveToLookPosButton = new SquareButton(left + WIDTH - 20, top + 65, 1, () -> {
            Entity camera = client.cameraEntity;
            if (camera == null) return;
            note.pos = Note.raycastPos(camera);
            clearAndInit();
        });
        moveToLookPosButton.active = inWorld;
        if (inWorld) moveToLookPosButton.setTooltip(Tooltip.of(Text.translatable("stickynotes.move_to_look_pos")));
        addDrawableChild(moveToLookPosButton);

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("stickynotes.note_see_through", boolText(note.seeThrough)),
                button -> {
                    note.seeThrough = !note.seeThrough;
                    button.setMessage(Text.translatable("stickynotes.note_see_through", boolText(note.seeThrough)));
                }
        ).dimensions(left, top + 90, WIDTH, 20).build());

        addDrawableChild(ButtonWidget.builder(
                ScreenTexts.DONE,
                button -> close()
        ).dimensions((width - 80) >> 1, top + 125, 80, 20).build());
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
        field.setText(String.format("%.2f", value).replace(',', '.'));
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
        renderBackgroundTexture(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width >>> 1, height / 20 - 5, 0xFFFFFFFF);
        int icon = hoveredIcon != -1 ? hoveredIcon : note.icon;
        int iconSize = height / 10;
        context.drawTexture(
                NotesManager.ICONS_TEXTURE,
                (width - iconSize) >> 1,
                iconSize,
                iconSize, iconSize,
                Note.getIconU(icon), Note.getIconV(icon),
                Note.ICON_SIDE, Note.ICON_SIDE,
                Note.TEXTURE_SIDE, Note.TEXTURE_SIDE
        );
        hoveredIcon = -1;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private class IconButton extends ClickableWidget {
        public final int icon;

        public IconButton(int x, int y, int sideSize, int icon) {
            super(x, y, sideSize, sideSize, ScreenTexts.EMPTY);
            this.icon = icon;
        }

        @Override
        protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            if (isHovered()) {
                hoveredIcon = icon;
            }
            context.drawTexture(
                    NotesManager.ICONS_TEXTURE,
                    getX() + 1,
                    getY() + 1,
                    getWidth() - 2, getHeight() - 2,
                    Note.getIconU(icon), Note.getIconV(icon),
                    Note.ICON_SIDE, Note.ICON_SIDE,
                    Note.TEXTURE_SIDE, Note.TEXTURE_SIDE
            );
            if (isHovered()) {
                context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x40FFFFFF);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            note.icon = icon;
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }

    private static class SquareButton extends ClickableWidget {
        private static final Identifier BUTTONS_TEXTURE = new Identifier(StickyNotes.MOD_ID, "textures/gui/buttons.png");
        private final Runnable clickAction;
        private final int u;

        public SquareButton(int x, int y, int texture, Runnable clickAction) {
            super(x, y, 20, 20, ScreenTexts.EMPTY);
            this.clickAction = clickAction;
            u = texture * 20;
        }

        @Override
        protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            context.drawTexture(
                    BUTTONS_TEXTURE,
                    getX(), getY(),
                    u,
                    active
                            ? isHovered()
                                    ? 20
                                    : 0
                            : 40,
                    20, 20,
                    64, 64
            );
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            clickAction.run();
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }
}
