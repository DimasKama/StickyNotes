package io.github.dimaskama.stickynotes.mixin;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasHolder;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpriteAtlasHolder.class)
public interface SpriteAtlasHolderAccessor {
    @Accessor("atlas")
    SpriteAtlasTexture stickynotes_getAtlas();

    @Invoker("getSprite")
    Sprite stickynotes_getSprite(Identifier objectId);
}
