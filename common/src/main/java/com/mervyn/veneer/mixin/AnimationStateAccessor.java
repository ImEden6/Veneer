package com.mervyn.veneer.mixin;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.AnimationState.class)
public interface AnimationStateAccessor {
    @Accessor("frame")
    int veneer$getFrame();

    @Accessor("frame")
    void veneer$setFrame(int frame);

    @Accessor("subFrame")
    int veneer$getSubFrame();

    @Accessor("subFrame")
    void veneer$setSubFrame(int subFrame);

    @Accessor("isDirty")
    void veneer$setDirty(boolean dirty);
}
