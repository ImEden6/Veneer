package com.mervyn.veneer.pbr;

import com.mervyn.veneer.VeneerConstants;
import com.mervyn.veneer.atlas.CompositeSpriteSource;
import com.mervyn.veneer.composite.PbrType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class VeneerPbrRegistry {
    private static final Map<Identifier, PbrRecipe> RECIPES = new ConcurrentHashMap<>();
    private static final Map<Identifier, Optional<Resource>> BUILT = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> RESOLVING = ThreadLocal.withInitial(() -> false);

    private VeneerPbrRegistry() {
    }

    public static void register(Identifier texturePath, PbrRecipe recipe) {
        PbrRecipe previous = RECIPES.put(texturePath, recipe);
        BUILT.remove(texturePath);
        if (previous != null && !previous.equals(recipe)) {
            VeneerConstants.LOGGER.warn("{} is defined by more than one composite sprite, using the last one stitched", texturePath);
        }
    }

    @Nullable
    public static PbrRecipe get(Identifier texturePath) {
        return RECIPES.get(texturePath);
    }

    public static void remove(Identifier texturePath) {
        RECIPES.remove(texturePath);
        BUILT.remove(texturePath);
    }

    public static void clear() {
        RECIPES.clear();
        BUILT.clear();
    }

    public static Optional<Resource> build(Identifier texturePath, PbrRecipe recipe, ResourceManager resourceManager) {
        Optional<Resource> built = BUILT.get(texturePath);
        if (built == null) {
            built = Optional.ofNullable(resolve(() -> VirtualPbrResource.create(recipe, resourceManager)));
            BUILT.put(texturePath, built);
        }
        return built;
    }

    public static boolean isResolving() {
        return RESOLVING.get();
    }

    public static <T> T resolve(Supplier<T> lookup) {
        boolean outer = RESOLVING.get();
        RESOLVING.set(true);
        try {
            return lookup.get();
        } finally {
            RESOLVING.set(outer);
        }
    }

    public record PbrRecipe(CompositeSpriteSource source, PbrType type) {
    }
}
