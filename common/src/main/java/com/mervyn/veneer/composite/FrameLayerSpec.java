package com.mervyn.veneer.composite;

import com.mojang.blaze3d.platform.NativeImage;

import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

public record FrameLayerSpec(NativeImage image, int srcYOffset, int srcFrameW, int srcFrameH, BlendMode blendMode,
                             OptionalInt tintRgb, @Nullable NativeImage mask, boolean invertMask,
                             int nextSrcYOffset, int interpWeight255, float opacity, Transform transform) {
    public FrameLayerSpec(NativeImage image, int srcYOffset, int srcFrameW, int srcFrameH, BlendMode blendMode,
                          OptionalInt tintRgb, @Nullable NativeImage mask, boolean invertMask) {
        this(image, srcYOffset, srcFrameW, srcFrameH, blendMode, tintRgb, mask, invertMask,
                srcYOffset, 0, 1.0F, Transform.NONE);
    }
}
