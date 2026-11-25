package com.mervyn.veneer;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VeneerConstants {
    public static final String MOD_ID = "veneer";
    public static final String MOD_NAME = "Veneer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private VeneerConstants() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
