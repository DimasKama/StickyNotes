package io.github.dimaskama.stickynotes.mixin;

import io.github.dimaskama.stickynotes.client.StickyNotes;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.state.WorldRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
abstract class WorldRendererMixin {

    @Shadow
    @Final
    private WorldRenderState worldRenderState;

    @Shadow
    @Final
    private OrderedRenderCommandQueueImpl entityRenderCommandQueue;

    @Inject(method = "method_62214", at = @At(value = "CONSTANT", args = "stringValue=submitBlockEntities", ordinal = 0))
    private void afterEntitiesRender(CallbackInfo ci) {
        StickyNotes.NOTES_MANAGER.renderAfterEntities(worldRenderState.cameraRenderState, entityRenderCommandQueue);
    }

    @Inject(method = "method_62214", at = @At(value = "CONSTANT", args = "stringValue=debug", ordinal = 0))
    private void lastRender(CallbackInfo ci) {
        StickyNotes.NOTES_MANAGER.renderLast(worldRenderState.cameraRenderState, entityRenderCommandQueue);
    }

}
