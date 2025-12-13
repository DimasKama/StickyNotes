package io.github.dimaskama.stickynotes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

@Mixin(TextureAtlas.class)
public interface SpriteAtlasTextureAccessor {
    @Accessor("texturesByName")
    Map<ResourceLocation, TextureAtlasSprite> stickynotes_getSprites();
}
