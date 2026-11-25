package com.mervyn.veneer.mixin;

import com.mervyn.veneer.pbr.VeneerPbrRegistry;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ReloadableResourceManager.class)
public class ReloadableResourceManagerMixin {
    @Shadow
    @Final
    private PackType type;

    @Inject(method = "createReload", at = @At("HEAD"))
    private void veneer$forgetRecipes(CallbackInfoReturnable<?> cir) {
        // the integrated server reloads its own data manager mid-session, which must not wipe client recipes
        if (type == PackType.CLIENT_RESOURCES) {
            VeneerPbrRegistry.clear();
        }
    }
}
