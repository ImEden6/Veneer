package com.mervyn.veneer;

import com.mervyn.veneer.atlas.CompositeSpriteSource;
import com.mervyn.veneer.mixin.SpriteSourcesAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;

public class VeneerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CompositeSpriteSource.TYPE = new SpriteSourceType(CompositeSpriteSource.CODEC);
        SpriteSourcesAccessor.veneer$getTypes().put(VeneerConstants.id("composite"), CompositeSpriteSource.TYPE);
    }
}
