package com.mervyn.veneer.composite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PixelMathTest {
    @Test
    void channelsAreAbgr() {
        assertEquals(0x11, PixelMath.alpha(0x11223344));
        assertEquals(0x22, PixelMath.blue(0x11223344));
        assertEquals(0x33, PixelMath.green(0x11223344));
        assertEquals(0x44, PixelMath.red(0x11223344));
        assertEquals(0x11223344, PixelMath.pack(0x11, 0x22, 0x33, 0x44));
    }

    @Test
    void greenTintRemovesRed() {
        assertEquals(0xFF000000, PixelMath.applyTint(0xFF0000FF, 0x00FF00));
    }

    @Test
    void tintTakesRgbOrder() {
        assertEquals(0x800000FF, PixelMath.applyTint(0x80FFFFFF, 0xFF0000));
        assertEquals(0x80FF0000, PixelMath.applyTint(0x80FFFFFF, 0x0000FF));
    }

    @Test
    void maskMultipliesAlpha() {
        assertEquals(0x40123456, PixelMath.applyMask(0x80123456, 0x80FFFFFF, false));
        assertEquals(0x00123456, PixelMath.applyMask(0xFF123456, 0x00FFFFFF, false));
        assertEquals(0xFF123456, PixelMath.applyMask(0xFF123456, 0xFF000000, false));
    }

    @Test
    void invertedMaskMultipliesRemainingAlpha() {
        assertEquals(0x7F123456, PixelMath.applyMask(0xFF123456, 0x80FFFFFF, true));
        assertEquals(0xFF123456, PixelMath.applyMask(0xFF123456, 0x00FFFFFF, true));
        assertEquals(0x00123456, PixelMath.applyMask(0xFF123456, 0xFF000000, true));
    }

    @Test
    void lerpMovesEveryChannel() {
        int a = PixelMath.pack(0, 255, 100, 20);
        int b = PixelMath.pack(255, 0, 200, 20);
        assertEquals(a, PixelMath.lerpAbgr(a, b, 0.0F));
        assertEquals(PixelMath.pack(128, 128, 150, 20), PixelMath.lerpAbgr(a, b, 0.5F));
        assertEquals(b, PixelMath.lerpAbgr(a, b, 1.0F));
    }
}
