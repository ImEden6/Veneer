package com.mervyn.veneer;

import com.mervyn.veneer.atlas.CompositeSpriteSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterSpriteSourcesEvent;

@Mod(value = VeneerConstants.MOD_ID, dist = Dist.CLIENT)
public class VeneerNeoForgeClient {
    public VeneerNeoForgeClient(IEventBus modBus) {
        modBus.addListener(RegisterSpriteSourcesEvent.class,
                event -> event.register(VeneerConstants.id("composite"), CompositeSpriteSource.CODEC));
    }
}
