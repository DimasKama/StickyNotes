package io.github.dimaskama.stickynotes.mixin;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TextureAtlasHolder.class)
public interface SpriteAtlasHolderAccessor {
    @Accessor("textureAtlas")
    TextureAtlas stickynotes_getAtlas();

    @Invoker("getSprite")
    TextureAtlasSprite stickynotes_getSprite(ResourceLocation objectId);
}
