package io.github.dimaskama.stickynotes.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Note {
    public static final Codec<Note> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Vec3d.CODEC.fieldOf("pos").forGetter(n -> n.pos),
                    Codecs.TEXT.fieldOf("name").forGetter(n -> n.name),
                    Codecs.TEXT.fieldOf("description").forGetter(n -> n.description),
                    Codecs.NONNEGATIVE_INT.fieldOf("icon").forGetter(n -> n.icon),
                    Codec.BOOL.fieldOf("see_through").forGetter(n -> n.seeThrough)
            ).apply(instance, Note::new)
    );
    public static final int TEXTURES_COUNT = 27;
    public static final int ICON_SIDE = 8;
    public static final int TEXTURE_SIDE = 128;
    private static final int ICONS_IN_ROW = TEXTURE_SIDE / ICON_SIDE;
    public Vec3d pos;
    public Text name;
    public Text description;
    public int icon;
    public boolean seeThrough;
    private Box box;
    private float u;
    private float v;

    public Note(Vec3d pos, Text name, Text description, int icon, boolean seeThrough) {
        this.pos = pos;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.seeThrough = seeThrough;
        update();
    }

    public void update() {
        box = createBox(pos);
        u = getIconU(icon) / TEXTURE_SIDE;
        v = getIconV(icon) / TEXTURE_SIDE;
    }

    public Box getBox() {
        return box;
    }

    public float getU() {
        return u;
    }

    public float getV() {
        return v;
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

    // In pixels
    public static float getIconU(int texture) {
        return texture % ICONS_IN_ROW * ICON_SIDE;
    }

    // In pixels
    public static float getIconV(int texture) {
        return (float) (texture / ICONS_IN_ROW) * ICON_SIDE;
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
