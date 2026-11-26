package com.mervyn.veneer.mixin;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pbr.loader.AtlasPBRLoader$PBRTextureAtlasSprite")
public interface IrisPbrSpriteAccessor {
    @Accessor("baseSprite")
    TextureAtlasSprite veneer$getBaseSprite();
}
