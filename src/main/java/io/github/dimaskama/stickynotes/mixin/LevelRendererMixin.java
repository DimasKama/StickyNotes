package io.github.dimaskama.stickynotes.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.dimaskama.stickynotes.client.StickyNotes;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.state.level.LevelRenderState;
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

    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;submitBlockEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/SubmitNodeStorage;)V"))
    private void afterEntitiesRender(CallbackInfo ci, @Local(name = "bufferSource") MultiBufferSource.BufferSource immediate) {
        StickyNotes.NOTES_MANAGER.renderAfterEntities(levelRenderState.cameraRenderState, immediate);
    }

    @Inject(method = "lambda$addMainPass$0", at = @At("TAIL"))
    private void lastRender(CallbackInfo ci, @Local(name = "bufferSource") MultiBufferSource.BufferSource immediate) {
        StickyNotes.NOTES_MANAGER.renderLast(levelRenderState.cameraRenderState, immediate);
        immediate.endBatch();
    }

}
