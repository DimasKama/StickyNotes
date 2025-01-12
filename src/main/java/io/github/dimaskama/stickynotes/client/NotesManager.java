package io.github.dimaskama.stickynotes.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.dimaskama.stickynotes.client.screen.NotesListScreen;
import io.github.dimaskama.stickynotes.mixin.SpriteAtlasHolderAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Optional;

public class NotesManager {
    public static final double CLAMP_DIST = 16.0;
    public static final double CLAMP_SQUARED_DIST = CLAMP_DIST * CLAMP_DIST;
    private static final float SIZE_IN_WORLD = 0.5F;
    private static final float HALF_SIZE_IN_WORLD = SIZE_IN_WORLD * 0.5F;
    @Nullable
    private Note targetedNote;
    private int noteTargetTime;

    public void tick(MinecraftClient client) {
        Entity camera = client.getCameraEntity();
        HitResult hitResult = client.crosshairTarget;
        List<Note> notes = StickyNotes.getCurrentWorldNotes();
        Note targeted = null;
        if (camera != null && notes != null && !notes.isEmpty() && hitResult != null) {
            Vec3d pos = camera.getCameraPosVec(1.0F);
            double hitLen = hitResult.getPos().subtract(pos).lengthSquared();
            boolean raycastedHitFar = hitResult.getType() != HitResult.Type.MISS;
            Vec3d raycastVec = camera.getRotationVector().multiply(50.0);
            Vec3d raycastPos = raycastVec.add(pos);
            for (Note note : notes) {
                Optional<Vec3d> optional = (note.seeThrough ? note.getClampedBox(pos) : note.getBox()).raycast(pos, raycastPos);
                if (optional.isEmpty()) continue;
                if (!note.seeThrough) {
                    double len = optional.get().subtract(pos).lengthSquared();
                    if (len > hitLen) {
                        if (raycastedHitFar) continue;
                        hitLen = Note.raycastPos(camera).subtract(pos).lengthSquared();
                        raycastedHitFar = true;
                        if (len > hitLen) continue;
                    }
                }
                targeted = note;
                break;
            }
        }
        noteTargetTime = targeted == targetedNote ? noteTargetTime + 1 : 0;
        targetedNote = targeted;

        // handle input
        if (StickyNotes.OPEN_NOTES_LIST_KEY.isPressed() && notes != null) {
            client.setScreen(new NotesListScreen(client.currentScreen, notes, true));
        }
    }

    public void renderAfterEntities(WorldRenderContext context) {
        renderNotes(context, StickyNotes.getCurrentWorldNotes(), false);
    }

    public void renderLast(WorldRenderContext context) {
        renderNotes(context, StickyNotes.getCurrentWorldNotes(), true);
    }

    private static void renderNotes(WorldRenderContext context, List<Note> notes, boolean seeThrough) {
        if (notes == null || notes.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        SpriteAtlasHolderAccessor atlas = (SpriteAtlasHolderAccessor) MinecraftClient.getInstance().getMapDecorationsAtlasManager();
        RenderSystem.setShaderTexture(0, atlas.stickynotes_getAtlas().getId());
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        Camera camera = context.camera();
        float viewDistanceSq = MathHelper.square(context.gameRenderer().getViewDistance() * 2.0F);
        BufferBuilder builder = null;

        for (Note note : notes) {
            if (note.seeThrough != seeThrough) continue;
            if (!seeThrough && viewDistanceSq < note.pos.subtract(camera.getPos()).lengthSquared()) continue;
            if (builder == null) {
                builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            }
            Vec3d relPos = seeThrough ? note.getClampedRelativePos(camera.getPos()) : note.pos.subtract(camera.getPos());
            matrices.push();
            matrices.translate(relPos.x, relPos.y, relPos.z);
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(camera.getYaw() - 180.0F));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(camera.getPitch() * 0.4F));
            matrices.translate(-relPos.x, -relPos.y, -relPos.z);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float x1 = (float) relPos.x - HALF_SIZE_IN_WORLD;
            float y1 = (float) relPos.y + SIZE_IN_WORLD;
            float x2 = x1 + SIZE_IN_WORLD;
            float y2 = (float) relPos.y;
            float z = (float) relPos.z;
            Sprite sprite = atlas.stickynotes_getSprite(note.icon);
            float u1 = sprite.getMinU();
            float v1 = sprite.getMinV();
            float u2 = sprite.getMaxU();
            float v2 = sprite.getMaxV();
            builder.vertex(matrix, x1, y1, z).texture(u1, v1);
            builder.vertex(matrix, x1, y2, z).texture(u1, v2);
            builder.vertex(matrix, x2, y2, z).texture(u2, v2);
            builder.vertex(matrix, x2, y1, z).texture(u2, v1);
            matrices.pop();
        }

        if (builder != null) {
            if (seeThrough) {
                RenderSystem.disableDepthTest();
            } else {
                RenderSystem.enableDepthTest();
            }
            BufferRenderer.drawWithGlobalProgram(builder.end());
        }
    }

    public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        Note note = targetedNote;
        if (note == null || Screen.hasShiftDown()) return;
        int time = noteTargetTime;
        float delta = tickCounter.getTickDelta(false);
        int nameAlphaMask = time < 10
                ? (int) (MathHelper.clamp(((time - 6) + delta) / 4.0F, 0.0F, 1.0F) * 255.0F)
                : 0xFF;
        if (nameAlphaMask < 4) return;
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int x = (context.getScaledWindowWidth() >>> 1) + 5;
        int y = (context.getScaledWindowHeight() >>> 1) + 5;
        context.drawTextWithShadow(
                textRenderer,
                note.name,
                x, y,
                (nameAlphaMask << 24) | 0x00FFFFFF
        );
        y += 15;
        int descAlphaMask = time < 30
                ? (int) (MathHelper.clamp(((time - 26) + delta) / 4.0F, 0.0F, 1.0F) * 255.0F)
                : 0xFF;
        if (descAlphaMask < 4) return;
        context.drawTextWrapped(
                textRenderer,
                note.description,
                x, y,
                Math.max((int) ((context.getScaledWindowWidth() >>> 1) * 0.8F), 80),
                (descAlphaMask << 24) | 0x00FFFFFF
        );
    }
}
