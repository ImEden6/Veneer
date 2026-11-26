package com.mervyn.veneer.pbr;

import com.mervyn.veneer.VeneerConstants;
import com.mervyn.veneer.atlas.CompositeSpriteSource;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class VirtualPbrResource {
    private VirtualPbrResource() {
    }

    @Nullable
    public static Resource create(VeneerPbrRegistry.PbrRecipe recipe, ResourceManager resourceManager) {
        CompositeSpriteSource.PbrImage composite = recipe.source().compositePbr(resourceManager, recipe.type());
        if (composite == null) {
            return null;
        }

        byte[] png;
        try {
            png = composite.image().asByteArray();
        } catch (IOException e) {
            VeneerConstants.LOGGER.error("Failed to encode {} companion of {}", recipe.type(), recipe.source().sprite(), e);
            return null;
        } finally {
            composite.image().close();
        }

        ResourceMetadata metadata = composite.animation() == null ? ResourceMetadata.EMPTY
                : ResourceMetadata.of(AnimationMetadataSection.TYPE, composite.animation());
        return new Resource(VirtualPackResources.INSTANCE, () -> new ByteArrayInputStream(png), () -> metadata);
    }
}
