package com.mervyn.veneer.composite;

import net.minecraft.util.FastColor;

public final class PixelMath {
    private PixelMath() {
    }

    public static int alpha(int abgr) {
        return FastColor.ABGR32.alpha(abgr);
    }

    public static int blue(int abgr) {
        return FastColor.ABGR32.blue(abgr);
    }

    public static int green(int abgr) {
        return FastColor.ABGR32.green(abgr);
    }

    public static int red(int abgr) {
        return FastColor.ABGR32.red(abgr);
    }

    public static int pack(int alpha, int blue, int green, int red) {
        return FastColor.ABGR32.color(alpha, blue, green, red);
    }

    public static int applyTint(int abgr, int rgbTint) {
        return pack(
                alpha(abgr),
                scale(blue(abgr), rgbTint & 0xFF),
                scale(green(abgr), rgbTint >> 8 & 0xFF),
                scale(red(abgr), rgbTint >> 16 & 0xFF));
    }

    public static int applyMask(int abgr, int maskAbgr, boolean invertMask) {
        int maskAlpha = invertMask ? 255 - alpha(maskAbgr) : alpha(maskAbgr);
        return pack(scale(alpha(abgr), maskAlpha), blue(abgr), green(abgr), red(abgr));
    }

    public static int lerpAbgr(int abgrA, int abgrB, float t) {
        return pack(
                lerp(alpha(abgrA), alpha(abgrB), t),
                lerp(blue(abgrA), blue(abgrB), t),
                lerp(green(abgrA), green(abgrB), t),
                lerp(red(abgrA), red(abgrB), t));
    }

    static int lerp(int from, int to, float t) {
        return Math.max(0, Math.min(255, Math.round(from + (to - from) * t)));
    }

    private static int scale(int channel, int factor) {
        return (channel * factor + 127) / 255;
    }
}
