package com.mervyn.veneer.composite;

import com.mojang.blaze3d.platform.NativeImage;

import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

public record LayerSpec(NativeImage image, BlendMode blendMode, OptionalInt tintRgb, @Nullable NativeImage mask, boolean invertMask,
                        float opacity, Transform transform) {
    public LayerSpec(NativeImage image, BlendMode blendMode, OptionalInt tintRgb, @Nullable NativeImage mask, boolean invertMask) {
        this(image, blendMode, tintRgb, mask, invertMask, 1.0F, Transform.NONE);
    }

    public LayerSpec(NativeImage image) {
        this(image, BlendMode.NORMAL, OptionalInt.empty(), null, false);
    }
}
