package io.github.dimaskama.stickynotes.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.dimaskama.stickynotes.client.StickyNotes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {

    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    @Shadow
    @Final
    private SubmitNodeStorage submitNodeStorage;

    @Inject(method = "method_62214", at = @At(value = "CONSTANT", args = "stringValue=submitBlockEntities", ordinal = 0))
    private void afterEntitiesRender(CallbackInfo ci) {
        StickyNotes.NOTES_MANAGER.renderAfterEntities(levelRenderState.cameraRenderState, submitNodeStorage);
    }

    @Inject(method = "method_62214", at = @At("TAIL"))
    private void lastRender(CallbackInfo ci, @Local(ordinal = 0) MultiBufferSource.BufferSource immediate) {
        StickyNotes.NOTES_MANAGER.renderLast(levelRenderState.cameraRenderState, submitNodeStorage);
        immediate.endBatch();
    }

}
