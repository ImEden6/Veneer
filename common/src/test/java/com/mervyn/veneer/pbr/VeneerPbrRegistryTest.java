package com.mervyn.veneer.pbr;

import com.mervyn.veneer.atlas.CompositeSpriteSource;
import com.mervyn.veneer.atlas.InterpolationMode;
import com.mervyn.veneer.composite.PbrType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VeneerPbrRegistryTest {
    private static final ResourceLocation PATH = ResourceLocation.parse("veneer:textures/block/fancy_ore_n.png");
    private static final VeneerPbrRegistry.PbrRecipe RECIPE = new VeneerPbrRegistry.PbrRecipe(
            new CompositeSpriteSource(ResourceLocation.parse("veneer:block/fancy_ore"), List.of(), 0, 64, true,
                    InterpolationMode.AUTO, 1, false),
            PbrType.NORMAL);

    @BeforeEach
    void reset() {
        VeneerPbrRegistry.clear();
    }

    @Test
    void registersAndLooksUp() {
        assertNull(VeneerPbrRegistry.get(PATH));
        VeneerPbrRegistry.register(PATH, RECIPE);
        assertSame(RECIPE, VeneerPbrRegistry.get(PATH));
    }

    @Test
    void removeAndClearForget() {
        VeneerPbrRegistry.register(PATH, RECIPE);
        VeneerPbrRegistry.remove(PATH);
        assertNull(VeneerPbrRegistry.get(PATH));
        VeneerPbrRegistry.register(PATH, RECIPE);
        VeneerPbrRegistry.clear();
        assertNull(VeneerPbrRegistry.get(PATH));
    }

    @Test
    void guardIsSetOnlyWhileResolving() {
        assertFalse(VeneerPbrRegistry.isResolving());
        boolean nested = VeneerPbrRegistry.resolve(() -> {
            VeneerPbrRegistry.resolve(() -> null);
            return VeneerPbrRegistry.isResolving();
        });
        assertTrue(nested);
        assertFalse(VeneerPbrRegistry.isResolving());
    }

    @Test
    void guardIsReleasedAfterFailure() {
        assertThrows(IllegalStateException.class, () -> VeneerPbrRegistry.resolve(() -> {
            throw new IllegalStateException();
        }));
        assertFalse(VeneerPbrRegistry.isResolving());
    }

    @Test
    void guardIsPerThread() {
        boolean other = VeneerPbrRegistry.resolve(() -> CompletableFuture
                .supplyAsync(VeneerPbrRegistry::isResolving, task -> new Thread(task).start())
                .join());
        assertFalse(other);
    }
}
