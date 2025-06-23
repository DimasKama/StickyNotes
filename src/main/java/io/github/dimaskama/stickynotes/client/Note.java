package io.github.dimaskama.stickynotes.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dimaskama.stickynotes.mixin.SpriteAtlasHolderAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Note {
    public static final Codec<Note> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Vec3d.CODEC.fieldOf("pos").forGetter(n -> n.pos),
                    TextCodecs.CODEC.fieldOf("name").forGetter(n -> n.name),
                    TextCodecs.CODEC.fieldOf("description").forGetter(n -> n.description),
                    Identifier.CODEC.fieldOf("icon").forGetter(n -> n.icon),
                    Codec.BOOL.fieldOf("see_through").forGetter(n -> n.seeThrough)
            ).apply(instance, Note::new)
    );
    public Vec3d pos;
    public Text name;
    public Text description;
    public Identifier icon;
    public boolean seeThrough;
    private Box box;

    public Note(Vec3d pos, Text name, Text description, Identifier icon, boolean seeThrough) {
        this.pos = pos;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.seeThrough = seeThrough;
        update();
    }

    public void update() {
        box = createBox(pos);
    }

    public Box getBox() {
        return box;
    }

    public Box getClampedBox(Vec3d cameraPos) {
        Vec3d relPos = pos.subtract(cameraPos);
        if (relPos.lengthSquared() > NotesManager.CLAMP_SQUARED_DIST) {
            return createBox(cameraPos.add(clampRelativePos(relPos)));
        }
        return box;
    }

    public Vec3d getClampedRelativePos(Vec3d cameraPos) {
        Vec3d relPos = pos.subtract(cameraPos);
        if (relPos.lengthSquared() > NotesManager.CLAMP_SQUARED_DIST) {
            return clampRelativePos(relPos);
        }
        return relPos;
    }

    private Vec3d clampRelativePos(Vec3d relPos) {
        return relPos.normalize().multiply(NotesManager.CLAMP_DIST);
    }

    public static void draw(DrawContext context, int x, int y, int width, int height, Identifier icon) {
        context.drawSpriteStretched(
                RenderPipelines.GUI_TEXTURED,
                ((SpriteAtlasHolderAccessor) MinecraftClient.getInstance().getMapDecorationsAtlasManager()).stickynotes_getSprite(icon),
                x, y,
                width, height
        );
    }

    public static Box createBox(Vec3d pos) {
        return new Box(
                pos.x - 0.25, pos.y, pos.z - 0.25,
                pos.x + 0.25, pos.y + 0.5, pos.z + 0.25
        );
    }

    public static Vec3d raycastPos(Entity entity) {
        HitResult result = entity.raycast(50.0, 1.0F, false);
        Vec3d pos = result.getPos();
        if (!(result instanceof BlockHitResult blockHit)) return pos;
        Direction side = blockHit.getSide();
        return switch (side) {
            case UP: yield pos;
            case DOWN: yield pos.add(0.0, -0.5, 0.0);
            default: yield pos.offset(side, 0.25);
        };
    }
}
