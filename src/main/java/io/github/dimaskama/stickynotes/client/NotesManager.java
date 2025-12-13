package io.github.dimaskama.stickynotes.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dimaskama.stickynotes.client.screen.NotesListScreen;
import io.github.dimaskama.stickynotes.mixin.SpriteAtlasHolderAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Optional;

public class NotesManager {
    public static final double CLAMP_DIST = 16.0;
    public static final double CLAMP_SQUARED_DIST = CLAMP_DIST * CLAMP_DIST;
    private static final float SIZE_IN_WORLD = 0.5F;
    private static final float HALF_SIZE_IN_WORLD = SIZE_IN_WORLD * 0.5F;
    private static final RenderType RENDER_LAYER = RenderType.create(
            "stickynotes",
            1536,
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath(StickyNotes.MOD_ID, "stickynotes"))
                    .build(),
            RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(ResourceLocation.withDefaultNamespace("textures/atlas/map_decorations.png"), TriState.FALSE, false))
                    .createCompositeState(false)
    );
    private static final RenderType RENDER_LAYER_SEE_THROUGH = RenderType.create(
            "stickynotes_see_through",
            1536,
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath(StickyNotes.MOD_ID, "stickynotes_see_through"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build(),
            RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(ResourceLocation.withDefaultNamespace("textures/atlas/map_decorations.png"), TriState.FALSE, false))
                    .createCompositeState(false)
    );
    @Nullable
    private Note targetedNote;
    private int noteTargetTime;

    public void tick(Minecraft client) {
        Entity camera = client.getCameraEntity();
        HitResult hitResult = client.hitResult;
        List<Note> notes = StickyNotes.getCurrentWorldNotes();
        Note targeted = null;
        if (camera != null && notes != null && !notes.isEmpty() && hitResult != null) {
            Vec3 pos = camera.getEyePosition(1.0F);
            double hitLen = hitResult.getLocation().subtract(pos).lengthSqr();
            boolean raycastedHitFar = hitResult.getType() != HitResult.Type.MISS;
            Vec3 raycastVec = camera.getLookAngle().scale(50.0);
            Vec3 raycastPos = raycastVec.add(pos);
            for (Note note : notes) {
                Optional<Vec3> optional = (note.seeThrough ? note.getClampedBox(pos) : note.getBox()).clip(pos, raycastPos);
                if (optional.isEmpty()) continue;
                if (!note.seeThrough) {
                    double len = optional.get().subtract(pos).lengthSqr();
                    if (len > hitLen) {
                        if (raycastedHitFar) continue;
                        hitLen = Note.raycastPos(camera).subtract(pos).lengthSqr();
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
        if (StickyNotes.OPEN_NOTES_LIST_KEY.isDown() && notes != null) {
            client.setScreen(new NotesListScreen(client.screen, notes, true));
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

        PoseStack matrices = context.matrixStack();
        SpriteAtlasHolderAccessor atlas = (SpriteAtlasHolderAccessor) Minecraft.getInstance().getMapDecorationTextures();
        Camera camera = context.camera();
        float viewDistanceSq = Mth.square(context.gameRenderer().getRenderDistance() * 2.0F);
        VertexConsumer consumer = null;

        for (Note note : notes) {
            if (note.seeThrough != seeThrough) continue;
            if (!seeThrough && viewDistanceSq < note.pos.subtract(camera.getPosition()).lengthSqr()) continue;
            if (consumer == null) {
                consumer = context.consumers().getBuffer(seeThrough ? RENDER_LAYER_SEE_THROUGH : RENDER_LAYER);
            }
            Vec3 relPos = seeThrough ? note.getClampedRelativePos(camera.getPosition()) : note.pos.subtract(camera.getPosition());
            matrices.pushPose();
            matrices.translate(relPos.x, relPos.y, relPos.z);
            matrices.mulPose(Axis.YN.rotationDegrees(camera.getYRot() - 180.0F));
            matrices.mulPose(Axis.XN.rotationDegrees(camera.getXRot() * 0.4F));
            matrices.translate(-relPos.x, -relPos.y, -relPos.z);
            Matrix4f matrix = matrices.last().pose();
            float x1 = (float) relPos.x - HALF_SIZE_IN_WORLD;
            float y1 = (float) relPos.y + SIZE_IN_WORLD;
            float x2 = x1 + SIZE_IN_WORLD;
            float y2 = (float) relPos.y;
            float z = (float) relPos.z;
            TextureAtlasSprite sprite = atlas.stickynotes_getSprite(note.icon);
            float u1 = sprite.getU0();
            float v1 = sprite.getV0();
            float u2 = sprite.getU1();
            float v2 = sprite.getV1();
            consumer.addVertex(matrix, x1, y1, z).setUv(u1, v1).setColor(-1);
            consumer.addVertex(matrix, x1, y2, z).setUv(u1, v2).setColor(-1);
            consumer.addVertex(matrix, x2, y2, z).setUv(u2, v2).setColor(-1);
            consumer.addVertex(matrix, x2, y1, z).setUv(u2, v1).setColor(-1);
            matrices.popPose();
        }
    }

    public void renderHud(GuiGraphics context, DeltaTracker tickCounter) {
        Note note = targetedNote;
        if (note == null || Screen.hasShiftDown()) return;
        int time = noteTargetTime;
        float delta = tickCounter.getGameTimeDeltaPartialTick(false);
        int nameAlphaMask = time < 10
                ? (int) (Mth.clamp(((time - 6) + delta) / 4.0F, 0.0F, 1.0F) * 255.0F)
                : 0xFF;
        if (nameAlphaMask < 4) return;
        Font textRenderer = Minecraft.getInstance().font;
        int x = (context.guiWidth() >>> 1) + 5;
        int y = (context.guiHeight() >>> 1) + 5;
        context.drawString(
                textRenderer,
                note.name,
                x, y,
                (nameAlphaMask << 24) | 0x00FFFFFF
        );
        y += 15;
        int descAlphaMask = time < 30
                ? (int) (Mth.clamp(((time - 26) + delta) / 4.0F, 0.0F, 1.0F) * 255.0F)
                : 0xFF;
        if (descAlphaMask < 4) return;
        context.drawWordWrap(
                textRenderer,
                note.description,
                x, y,
                Math.max((int) ((context.guiWidth() >>> 1) * 0.8F), 80),
                (descAlphaMask << 24) | 0x00FFFFFF,
                false
        );
    }
}
