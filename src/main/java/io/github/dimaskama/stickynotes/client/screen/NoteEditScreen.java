package io.github.dimaskama.stickynotes.client.screen;

import io.github.dimaskama.stickynotes.client.Note;
import io.github.dimaskama.stickynotes.client.StickyNotes;
import io.github.dimaskama.stickynotes.mixin.SpriteAtlasTextureAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleConsumer;

public class NoteEditScreen extends Screen {
    private static final int WIDTH = 270;

    private final Screen parent;
    private final Note note;
    private EditBox nameWidget;
    private EditBox descWidget;
    private boolean nameDirty;
    private boolean descDirty;
    private boolean posDirty;
    private double noteX, noteY, noteZ;
    private Identifier hoveredIcon = null;

    public NoteEditScreen(@Nullable Screen parent, Note note, boolean edit) {
        super(Component.translatable(edit ? "stickynotes.edit_note" : "stickynotes.add_note"));
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

        Component nameText = Component.translatable("stickynotes.note_name");
        int nameWidth = font.width(nameText);
        addRenderableWidget(new StringWidget(left, top + 5, nameWidth, 9, nameText, font));
        if (nameWidget == null) {
            nameWidget = new EditBox(
                    font,
                    left + nameWidth + 4,
                    top,
                    WIDTH - (nameWidth + 4),
                    20,
                    CommonComponents.EMPTY
            );
            nameWidget.setValue(note.name.getString());
            nameWidget.setResponder(s -> nameDirty = true);
        }
        addRenderableWidget(nameWidget);

        Component descText = Component.translatable("stickynotes.note_desc");
        int descWidth = font.width(descText);
        addRenderableWidget(new StringWidget(left, top + 25 + 5, descWidth, 9, descText, font));
        if (descWidget == null) {
            descWidget = new EditBox(
                    font,
                    left + descWidth + 4,
                    top + 25,
                    WIDTH - (descWidth + 4),
                    20,
                    CommonComponents.EMPTY
            );
            descWidget.setValue(note.description.getString());
            descWidget.setMaxLength(256);
            descWidget.setResponder(s -> descDirty = true);
        }
        addRenderableWidget(descWidget);

        int iconsInRow = WIDTH / 10;
        int i = 0;
        for (Identifier icon : ((SpriteAtlasTextureAccessor) (minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.MAP_DECORATIONS))).stickynotes_getSprites().keySet()) {
            if (icon.equals(MissingTextureAtlasSprite.getLocation())) continue;
            addRenderableWidget(new IconButton(left + (i % iconsInRow) * 10, top + 50 + (i / iconsInRow) * 10, 10, icon));
            ++i;
        }

        Component posText = Component.translatable("stickynotes.note_pos");
        int posWidth = font.width(posText);
        addRenderableWidget(new StringWidget(left, top + 75 + 5, posWidth, 9, posText, font));
        int ww = (WIDTH - posWidth - 4 - 50) / 3;
        addPosField(left + posWidth + 4, top + 75, ww, "X", noteX, d -> noteX = d);
        addPosField(left + posWidth + 4 + ww, top + 75, ww, "Y", noteY, d -> noteY = d);
        addPosField(left + posWidth + 4 + ww + ww, top + 75, ww, "Z", noteZ, d -> noteZ = d);

        boolean inWorld = minecraft.getCameraEntity() != null;

        SquareButton moveToPlayerButton = new SquareButton(left + WIDTH - 45, top + 75, 0, () -> {
            Entity camera = minecraft.getCameraEntity();
            if (camera == null) return;
            note.pos = camera.position();
            rebuildWidgets();
        });
        moveToPlayerButton.active = inWorld;
        if (inWorld) moveToPlayerButton.setTooltip(Tooltip.create(Component.translatable("stickynotes.move_to_player")));
        addRenderableWidget(moveToPlayerButton);

        SquareButton moveToLookPosButton = new SquareButton(left + WIDTH - 20, top + 75, 1, () -> {
            Entity camera = minecraft.getCameraEntity();
            if (camera == null) return;
            note.pos = Note.raycastPos(camera);
            rebuildWidgets();
        });
        moveToLookPosButton.active = inWorld;
        if (inWorld) moveToLookPosButton.setTooltip(Tooltip.create(Component.translatable("stickynotes.move_to_look_pos")));
        addRenderableWidget(moveToLookPosButton);

        addRenderableWidget(Button.builder(
                Component.translatable("stickynotes.note_see_through", boolText(note.seeThrough)),
                button -> {
                    note.seeThrough = !note.seeThrough;
                    button.setMessage(Component.translatable("stickynotes.note_see_through", boolText(note.seeThrough)));
                }
        ).bounds(left, top + 100, WIDTH, 20).build());

        addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                button -> onClose()
        ).bounds((width - 80) >> 1, top + 135, 80, 20).build());
    }

    private Component boolText(boolean bool) {
        return bool ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
    }

    private void addPosField(int x, int y, int width, String title, double value, DoubleConsumer consumer) {
        EditBox field = new EditBox(
                font,
                x, y,
                width, 20,
                CommonComponents.EMPTY
        );
        field.setValue(String.format("%.2f", value).replace(',', '.'));
        field.setHint(Component.literal(title));
        field.setResponder(string -> {
            posDirty = true;
            boolean changed = false;
            while (!string.isBlank()) {
                try {
                    consumer.accept(Double.parseDouble(string));
                    if (changed) field.setValue(string);
                    return;
                } catch (NumberFormatException e) {
                    string = string.substring(0, string.length() - 1);
                    changed = true;
                }
            }
            if (changed) field.setValue(string);
            consumer.accept(0.0);
        });
        addRenderableWidget(field);
    }

    @Override
    public void removed() {
        if (nameDirty) note.name = Component.literal(nameWidget.getValue());
        if (descDirty) note.description = Component.literal(descWidget.getValue());
        if (posDirty) note.pos = new Vec3(noteX, noteY, noteZ);
        note.update();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(font, title, width >>> 1, height / 20 - 5, 0xFFFFFFFF);
        Identifier icon = hoveredIcon != null ? hoveredIcon : note.icon;
        int iconSize = height / 10;
        Note.draw(context, (width - iconSize) >> 1, iconSize, iconSize, iconSize, icon);
        hoveredIcon = null;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private class IconButton extends AbstractWidget {
        public final Identifier icon;

        public IconButton(int x, int y, int sideSize, Identifier icon) {
            super(x, y, sideSize, sideSize, CommonComponents.EMPTY);
            this.icon = icon;
        }

        @Override
        protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
            if (isHovered()) {
                hoveredIcon = icon;
            }
            Note.draw(context, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, icon);
            if (isHovered()) {
                context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x40FFFFFF);
            }
        }

        @Override
        public void onClick(MouseButtonEvent click, boolean doubled) {
            note.icon = icon;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {
            defaultButtonNarrationText(builder);
        }
    }

    private static class SquareButton extends AbstractWidget {
        private static final Identifier BUTTONS_TEXTURE = Identifier.fromNamespaceAndPath(StickyNotes.MOD_ID, "textures/gui/buttons.png");
        private final Runnable clickAction;
        private final int u;

        public SquareButton(int x, int y, int texture, Runnable clickAction) {
            super(x, y, 20, 20, CommonComponents.EMPTY);
            this.clickAction = clickAction;
            u = texture * 20;
        }

        @Override
        protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
            context.blit(
                    RenderPipelines.GUI_TEXTURED,
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
        public void onClick(MouseButtonEvent click, boolean doubled) {
            clickAction.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {
            defaultButtonNarrationText(builder);
        }
    }
}
