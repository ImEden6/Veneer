package com.mervyn.veneer.composite;

import com.mojang.blaze3d.platform.NativeImage;

import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

public record LayerSpec(NativeImage image, BlendMode blendMode, OptionalInt tintRgb, @Nullable NativeImage mask, boolean invertMask) {
    public LayerSpec(NativeImage image) {
        this(image, BlendMode.NORMAL, OptionalInt.empty(), null, false);
    }
}
