package com.mervyn.veneer.mixin;

import com.mervyn.veneer.pbr.VeneerPbrRegistry;
import com.mervyn.veneer.pbr.VirtualPbrResource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void veneer$forgetRecipes(PackType type, List<PackResources> packs, CallbackInfo ci) {
        // the integrated server builds its own manager mid-session, which must not wipe client recipes
        if (type == PackType.CLIENT_RESOURCES) {
            VeneerPbrRegistry.clear();
        }
    }

    @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
    private void veneer$virtualPbr(ResourceLocation location, CallbackInfoReturnable<Optional<Resource>> cir) {
        if (cir.getReturnValue().isPresent() || VeneerPbrRegistry.isResolving()) {
            return;
        }
        VeneerPbrRegistry.PbrRecipe recipe = VeneerPbrRegistry.get(location);
        if (recipe == null) {
            return;
        }
        Resource resource = VeneerPbrRegistry.resolve(
                () -> VirtualPbrResource.create(recipe, (ResourceManager) (Object) this));
        if (resource != null) {
            cir.setReturnValue(Optional.of(resource));
        }
    }
}
