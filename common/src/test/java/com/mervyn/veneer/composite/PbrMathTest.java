package com.mervyn.veneer.composite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PbrMathTest {
    @Test
    void neutralNormalIsFlat() {
        assertEquals(128, PixelMath.red(PbrMath.NEUTRAL_NORMAL_ABGR));
        assertEquals(128, PixelMath.green(PbrMath.NEUTRAL_NORMAL_ABGR));
        assertEquals(255, PixelMath.blue(PbrMath.NEUTRAL_NORMAL_ABGR));
        assertEquals(255, PixelMath.alpha(PbrMath.NEUTRAL_NORMAL_ABGR));
    }

    @Test
    void neutralSpecularIsDull() {
        assertEquals(0, PixelMath.red(PbrMath.NEUTRAL_SPECULAR_ABGR));
        assertEquals(0, PixelMath.green(PbrMath.NEUTRAL_SPECULAR_ABGR));
        assertEquals(0, PixelMath.blue(PbrMath.NEUTRAL_SPECULAR_ABGR));
        assertEquals(255, PixelMath.alpha(PbrMath.NEUTRAL_SPECULAR_ABGR));
    }

    @Test
    void normalLerpsEveryChannel() {
        int dst = PixelMath.pack(0, 255, 100, 20);
        int src = PixelMath.pack(255, 0, 200, 20);
        assertEquals(dst, PbrMath.blendNormal(dst, src, 0));
        assertEquals(PixelMath.pack(128, 127, 150, 20), PbrMath.blendNormal(dst, src, 128));
        assertEquals(src, PbrMath.blendNormal(dst, src, 255));
    }

    @Test
    void specularSnapsFlagsAtHalf() {
        int dst = PixelMath.pack(10, 20, 30, 0);
        int src = PixelMath.pack(200, 210, 230, 255);
        assertEquals(dst, PbrMath.blendSpecular(dst, src, 0));
        assertEquals(PixelMath.pack(200, 210, 230, 128), PbrMath.blendSpecular(dst, src, 128));
        assertEquals(PixelMath.pack(10, 20, 30, 127), PbrMath.blendSpecular(dst, src, 127));
        assertEquals(src, PbrMath.blendSpecular(dst, src, 255));
    }

    @Test
    void emissionIsNeverInterpolated() {
        int glowing = PixelMath.pack(0, 0, 0, 0);
        for (int alpha = 0; alpha <= 255; alpha++) {
            int blended = PbrMath.blendSpecular(glowing, PbrMath.NEUTRAL_SPECULAR_ABGR, alpha);
            assertEquals(alpha >= 128 ? 255 : 0, PixelMath.alpha(blended));
        }
    }

    @Test
    void specularLerpSnapsFlags() {
        int a = PixelMath.pack(10, 20, 30, 0);
        int b = PixelMath.pack(200, 210, 230, 200);
        assertEquals(PixelMath.pack(200, 210, 230, 100), PbrMath.lerpSpecular(a, b, 0.5F));
        assertEquals(PixelMath.pack(10, 20, 30, 98), PbrMath.lerpSpecular(a, b, 0.49F));
    }
}
