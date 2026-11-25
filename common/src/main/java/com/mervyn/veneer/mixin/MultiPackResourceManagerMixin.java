package com.mervyn.veneer.mixin;

import com.mervyn.veneer.pbr.VeneerPbrRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {
    @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
    private void veneer$virtualPbr(ResourceLocation location, CallbackInfoReturnable<Optional<Resource>> cir) {
        if (cir.getReturnValue().isPresent() || VeneerPbrRegistry.isResolving()) {
            return;
        }
        VeneerPbrRegistry.PbrRecipe recipe = VeneerPbrRegistry.get(location);
        if (recipe == null) {
            return;
        }
        Optional<Resource> resource = VeneerPbrRegistry.build(location, recipe, (ResourceManager) (Object) this);
        if (resource.isPresent()) {
            cir.setReturnValue(resource);
        }
    }
}
