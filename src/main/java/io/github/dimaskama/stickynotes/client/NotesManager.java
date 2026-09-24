package io.github.dimaskama.stickynotes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import io.github.dimaskama.stickynotes.client.screen.NotesListScreen;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
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
    // Rendered in the solid phase (no blending), because position_tex_color shader doesn't support
    // Improved Transparency (OIT). Map decoration sprites have binary alpha, and fully transparent pixels are discarded.
    public static final RenderPipeline RENDER_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(StickyNotes.MOD_ID, "stickynotes"))
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .build();
    private static final RenderType RENDER_LAYER = RenderType.create(
            "stickynotes",
            RenderSetup.builder(RENDER_PIPELINE)
                    .withTexture("Sampler0", Sheets.MAP_DECORATIONS_SHEET)
                    .createRenderSetup()
    );
    // Rendered in the see-through phase (after all terrain and translucent geometry, without depth attachment)
    public static final RenderPipeline RENDER_PIPELINE_SEE_THROUGH = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(StickyNotes.MOD_ID, "stickynotes_see_through"))
            .withDepthStencilState(Optional.empty())
            .build();
    private static final RenderType RENDER_LAYER_SEE_THROUGH = RenderType.create(
            "stickynotes_see_through",
            RenderSetup.builder(RENDER_PIPELINE_SEE_THROUGH)
                    .withTexture("Sampler0", Sheets.MAP_DECORATIONS_SHEET)
                    .createRenderSetup()
    );
    public static final FeatureRendererType<SeeThroughSubmit> SEE_THROUGH_FEATURE_TYPE = FeatureRendererType.create("stickynotes:see_through");
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
            client.gui.setScreen(new NotesListScreen(client.gui.screen(), notes, true));
        }
    }

    public void collectSubmits(LevelRenderContext context) {
        List<Note> notes = StickyNotes.getCurrentWorldNotes();
        if (notes == null || notes.isEmpty()) return;

        CameraRenderState camera = context.levelState().cameraRenderState;
        SubmitNodeCollector collector = context.submitNodeCollector();
        submitNotes(camera, collector, notes, false);
        submitNotes(camera, collector, notes, true);
    }

    private static void submitNotes(CameraRenderState camera, SubmitNodeCollector collector, List<Note> notes, boolean seeThrough) {
        RenderType renderLayer = seeThrough ? RENDER_LAYER_SEE_THROUGH : RENDER_LAYER;

        PoseStack matrices = new PoseStack();
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.MAP_DECORATIONS);
        float viewDistanceSq = Mth.square(Minecraft.getInstance().options.getEffectiveRenderDistance() * 32.0F);

        Quaternionf rotation = new Quaternionf().rotationYXZ(
                Mth.DEG_TO_RAD * (180.0F - camera.yRot),
                Mth.DEG_TO_RAD * (-camera.xRot * 0.4F),
                0.0F
        );

        for (Note note : notes) {
            if (note.seeThrough != seeThrough) continue;
            if (!seeThrough && viewDistanceSq < note.pos.subtract(camera.pos).lengthSqr()) continue;
            Vec3 relPos = seeThrough ? note.getClampedRelativePos(camera.pos) : note.pos.subtract(camera.pos);
            matrices.pushPose();
            matrices.translate(relPos.x, relPos.y, relPos.z);
            matrices.rotate(rotation);
            TextureAtlasSprite sprite = atlas.getSprite(note.icon);
            float u1 = sprite.getU0();
            float v1 = sprite.getV0();
            float u2 = sprite.getU1();
            float v2 = sprite.getV1();
            SubmitNodeCollector.CustomGeometryRenderer geometry = (pose, consumer) -> {
                consumer.addVertex(pose, -HALF_SIZE_IN_WORLD, SIZE_IN_WORLD, 0).setUv(u1, v1).setColor(-1);
                consumer.addVertex(pose, -HALF_SIZE_IN_WORLD, 0, 0).setUv(u1, v2).setColor(-1);
                consumer.addVertex(pose, HALF_SIZE_IN_WORLD, 0, 0).setUv(u2, v2).setColor(-1);
                consumer.addVertex(pose, HALF_SIZE_IN_WORLD, SIZE_IN_WORLD, 0).setUv(u2, v1).setColor(-1);
            };
            if (seeThrough) {
                // Render after translucent terrain, so see-through notes are not covered by water, glass, etc.
                collector.submitCustom(SubmitRenderPhases.SEE_THROUGH_NAME_TAGS, new SeeThroughSubmit(matrices.last().copy(), renderLayer, geometry));
            } else {
                collector.submitCustomGeometry(matrices, renderLayer, geometry);
            }
            matrices.popPose();
        }
    }

    public void renderHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
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
        context.text(
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
        context.textWithWordWrap(
                textRenderer,
                note.description,
                x, y,
                Math.max((int) ((context.guiWidth() >>> 1) * 0.8F), 80),
                (descAlphaMask << 24) | 0x00FFFFFF,
                false
        );
    }

    public record SeeThroughSubmit(PoseStack.Pose pose, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) implements TranslucentSubmit {
        @Override
        public float distanceToCameraSq() {
            return TranslucentSubmit.computeDistanceToCameraSq(pose.pose());
        }

        @Override
        public FeatureRendererType<SeeThroughSubmit> featureType() {
            return SEE_THROUGH_FEATURE_TYPE;
        }
    }

    public static class SeeThroughFeatureRenderer extends RenderTypeFeatureRenderer<SeeThroughSubmit> {
        @Override
        protected void buildGroup(FeatureFrameContext context, List<SeeThroughSubmit> submits) {
            for (SeeThroughSubmit submit : submits) {
                submit.customGeometryRenderer().render(submit.pose(), getVertexBuilder(submit.renderType()));
            }
        }
    }

}
