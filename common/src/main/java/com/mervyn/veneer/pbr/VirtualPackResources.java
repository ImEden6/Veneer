package com.mervyn.veneer.pbr;

import com.mervyn.veneer.VeneerConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

final class VirtualPackResources implements PackResources {
    static final VirtualPackResources INSTANCE = new VirtualPackResources();

    private final PackLocationInfo location = new PackLocationInfo(VeneerConstants.MOD_ID + "_virtual",
            Component.literal(VeneerConstants.MOD_NAME), PackSource.BUILT_IN, Optional.empty());

    private VirtualPackResources() {
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return Set.of();
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return location;
    }

    @Override
    public void close() {
    }
}
