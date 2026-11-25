package com.mervyn.veneer;

import com.mervyn.veneer.atlas.CompositeSpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;

@Mod(value = VeneerConstants.MOD_ID, dist = Dist.CLIENT)
public class VeneerNeoForgeClient {
    public VeneerNeoForgeClient(IEventBus modBus) {
        modBus.addListener(RegisterSpriteSourceTypesEvent.class, event -> {
            CompositeSpriteSource.TYPE = new SpriteSourceType(CompositeSpriteSource.CODEC);
            event.register(VeneerConstants.id("composite"), CompositeSpriteSource.TYPE);
        });
    }
}
