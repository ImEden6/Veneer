package com.mervyn.veneer.composite;

import com.mojang.blaze3d.platform.NativeImage;

import org.jetbrains.annotations.Nullable;

public record PbrFrameLayerSpec(NativeImage albedo, int albedoYOffset, int albedoFrameW, int albedoFrameH,
                                @Nullable NativeImage mask, boolean invertMask,
                                @Nullable NativeImage pbrImage, int pbrYOffset, int pbrFrameW, int pbrFrameH,
                                int nextAlbedoYOffset, int nextPbrYOffset, int interpWeight255,
                                float opacity, Transform transform) {
    public PbrFrameLayerSpec(NativeImage albedo, int albedoYOffset, int albedoFrameW, int albedoFrameH,
                             @Nullable NativeImage mask, boolean invertMask,
                             @Nullable NativeImage pbrImage, int pbrYOffset, int pbrFrameW, int pbrFrameH) {
        this(albedo, albedoYOffset, albedoFrameW, albedoFrameH, mask, invertMask,
                pbrImage, pbrYOffset, pbrFrameW, pbrFrameH, albedoYOffset, pbrYOffset, 0, 1.0F, Transform.NONE);
    }
}
