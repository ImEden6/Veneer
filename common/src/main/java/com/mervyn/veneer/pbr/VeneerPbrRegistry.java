package com.mervyn.veneer.pbr;

import com.mervyn.veneer.atlas.CompositeSpriteSource;
import com.mervyn.veneer.composite.PbrType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class VeneerPbrRegistry {
    private static final Map<ResourceLocation, PbrRecipe> RECIPES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> RESOLVING = ThreadLocal.withInitial(() -> false);

    private VeneerPbrRegistry() {
    }

    public static void register(ResourceLocation texturePath, PbrRecipe recipe) {
        RECIPES.put(texturePath, recipe);
    }

    @Nullable
    public static PbrRecipe get(ResourceLocation texturePath) {
        return RECIPES.get(texturePath);
    }

    public static void remove(ResourceLocation texturePath) {
        RECIPES.remove(texturePath);
    }

    public static void clear() {
        RECIPES.clear();
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
