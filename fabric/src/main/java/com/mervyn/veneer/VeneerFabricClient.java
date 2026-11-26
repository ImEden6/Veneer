package com.mervyn.veneer;

import com.mervyn.veneer.atlas.CompositeSpriteSource;
import com.mervyn.veneer.mixin.SpriteSourcesAccessor;
import net.fabricmc.api.ClientModInitializer;

public class VeneerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SpriteSourcesAccessor.veneer$getIdMapper().put(VeneerConstants.id("composite"), CompositeSpriteSource.CODEC);
    }
}
