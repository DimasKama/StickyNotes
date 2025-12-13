package io.github.dimaskama.stickynotes.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dimaskama.stickynotes.client.screen.NotesListScreen;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Optional;

public class NotesManager {

    public static final double CLAMP_DIST = 16.0;
    public static final double CLAMP_SQUARED_DIST = CLAMP_DIST * CLAMP_DIST;
    private static final float SIZE_IN_WORLD = 0.5F;
    private static final float HALF_SIZE_IN_WORLD = SIZE_IN_WORLD * 0.5F;
    public static final RenderPipeline RENDER_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(ResourceLocation.fromNamespaceAndPath(StickyNotes.MOD_ID, "stickynotes"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .build();
    private static final RenderType RENDER_LAYER = RenderType.create(
            "stickynotes",
            1536,
            RENDER_PIPELINE,
            RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(ResourceLocation.withDefaultNamespace("textures/atlas/map_decorations.png"), false))
                    .createCompositeState(false)
    );
    public static final RenderPipeline RENDER_PIPELINE_SEE_THROUGH = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(ResourceLocation.fromNamespaceAndPath(StickyNotes.MOD_ID, "stickynotes_see_through"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build();
    private static final RenderType RENDER_LAYER_SEE_THROUGH = RenderType.create(
            "stickynotes_see_through",
            1536,
            RENDER_PIPELINE_SEE_THROUGH,
            RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(ResourceLocation.withDefaultNamespace("textures/atlas/map_decorations.png"), false))
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

    public void renderAfterEntities(CameraRenderState camera, SubmitNodeCollector queue) {
        renderNotes(camera, queue, StickyNotes.getCurrentWorldNotes(), false);
    }

    public void renderLast(CameraRenderState camera, SubmitNodeCollector queue) {
        renderNotes(camera, queue, StickyNotes.getCurrentWorldNotes(), true);
    }

    private static void renderNotes(CameraRenderState camera, SubmitNodeCollector queue, List<Note> notes, boolean seeThrough) {
        if (notes == null || notes.isEmpty()) return;

        RenderType renderLayer = seeThrough ? RENDER_LAYER_SEE_THROUGH : RENDER_LAYER;

        PoseStack matrices = new PoseStack();
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.MAP_DECORATIONS);
        float viewDistanceSq = Mth.square(Minecraft.getInstance().gameRenderer.getRenderDistance() * 2.0F);

        Camera cameraObj = Minecraft.getInstance().gameRenderer.getMainCamera();
        Quaternionf rotation = new Quaternionf().rotationYXZ(
                Mth.DEG_TO_RAD * (180.0F - cameraObj.getYRot()),
                Mth.DEG_TO_RAD * (-cameraObj.getXRot() * 0.4F),
                0.0F
        );

        for (Note note : notes) {
            if (note.seeThrough != seeThrough) continue;
            if (!seeThrough && viewDistanceSq < note.pos.subtract(camera.pos).lengthSqr()) continue;
            Vec3 relPos = seeThrough ? note.getClampedRelativePos(camera.pos) : note.pos.subtract(camera.pos);
            matrices.pushPose();
            matrices.translate(relPos.x, relPos.y, relPos.z);
            matrices.mulPose(rotation);
            TextureAtlasSprite sprite = atlas.getSprite(note.icon);
            float u1 = sprite.getU0();
            float v1 = sprite.getV0();
            float u2 = sprite.getU1();
            float v2 = sprite.getV1();
            queue.submitCustomGeometry(matrices, renderLayer, (matrix, consumer) -> {
                consumer.addVertex(matrix, -HALF_SIZE_IN_WORLD, SIZE_IN_WORLD, 0).setUv(u1, v1).setColor(-1);
                consumer.addVertex(matrix, -HALF_SIZE_IN_WORLD, 0, 0).setUv(u1, v2).setColor(-1);
                consumer.addVertex(matrix, HALF_SIZE_IN_WORLD, 0, 0).setUv(u2, v2).setColor(-1);
                consumer.addVertex(matrix, HALF_SIZE_IN_WORLD, SIZE_IN_WORLD, 0).setUv(u2, v1).setColor(-1);
            });
            matrices.popPose();
        }
    }

    public void renderHud(GuiGraphics context, DeltaTracker tickCounter) {
        Note note = targetedNote;
        if (note == null || Minecraft.getInstance().hasShiftDown()) return;
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
