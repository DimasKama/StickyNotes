package io.github.dimaskama.stickynotes.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Note {
    public static final Codec<Note> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Vec3.CODEC.fieldOf("pos").forGetter(n -> n.pos),
                    ComponentSerialization.CODEC.fieldOf("name").forGetter(n -> n.name),
                    ComponentSerialization.CODEC.fieldOf("description").forGetter(n -> n.description),
                    Identifier.CODEC.fieldOf("icon").forGetter(n -> n.icon),
                    Codec.BOOL.fieldOf("see_through").forGetter(n -> n.seeThrough)
            ).apply(instance, Note::new)
    );
    public Vec3 pos;
    public Component name;
    public Component description;
    public Identifier icon;
    public boolean seeThrough;
    private AABB box;

    public Note(Vec3 pos, Component name, Component description, Identifier icon, boolean seeThrough) {
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

    public AABB getBox() {
        return box;
    }

    public AABB getClampedBox(Vec3 cameraPos) {
        Vec3 relPos = pos.subtract(cameraPos);
        if (relPos.lengthSqr() > NotesManager.CLAMP_SQUARED_DIST) {
            return createBox(cameraPos.add(clampRelativePos(relPos)));
        }
        return box;
    }

    public Vec3 getClampedRelativePos(Vec3 cameraPos) {
        Vec3 relPos = pos.subtract(cameraPos);
        if (relPos.lengthSqr() > NotesManager.CLAMP_SQUARED_DIST) {
            return clampRelativePos(relPos);
        }
        return relPos;
    }

    private Vec3 clampRelativePos(Vec3 relPos) {
        return relPos.normalize().scale(NotesManager.CLAMP_DIST);
    }

    public static void draw(GuiGraphics context, int x, int y, int width, int height, Identifier icon) {
        context.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.MAP_DECORATIONS).getSprite(icon),
                x, y,
                width, height
        );
    }

    public static AABB createBox(Vec3 pos) {
        return new AABB(
                pos.x - 0.25, pos.y, pos.z - 0.25,
                pos.x + 0.25, pos.y + 0.5, pos.z + 0.25
        );
    }

    public static Vec3 raycastPos(Entity entity) {
        HitResult result = entity.pick(50.0, 1.0F, false);
        Vec3 pos = result.getLocation();
        if (!(result instanceof BlockHitResult blockHit)) return pos;
        Direction side = blockHit.getDirection();
        return switch (side) {
            case UP: yield pos;
            case DOWN: yield pos.add(0.0, -0.5, 0.0);
            default: yield pos.relative(side, 0.25);
        };
    }
}
