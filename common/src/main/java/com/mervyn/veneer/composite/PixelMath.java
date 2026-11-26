package com.mervyn.veneer.composite;

public final class PixelMath {
    private PixelMath() {
    }

    public static int alpha(int abgr) {
        return abgr >>> 24;
    }

    public static int blue(int abgr) {
        return abgr >> 16 & 0xFF;
    }

    public static int green(int abgr) {
        return abgr >> 8 & 0xFF;
    }

    public static int red(int abgr) {
        return abgr & 0xFF;
    }

    public static int pack(int alpha, int blue, int green, int red) {
        return alpha << 24 | blue << 16 | green << 8 | red;
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

    public static int applyOpacity(int abgr, float opacity) {
        int scaled = Math.max(0, Math.min(255, Math.round(alpha(abgr) * opacity)));
        return pack(scaled, blue(abgr), green(abgr), red(abgr));
    }

    public static int lerpAbgr(int abgrA, int abgrB, float t) {
        float alphaA = alpha(abgrA) / 255.0F;
        float alphaB = alpha(abgrB) / 255.0F;
        float outAlpha = alphaA + (alphaB - alphaA) * t;
        if (outAlpha <= 0.0F) {
            return 0;
        }
        return pack(
                Math.round(outAlpha * 255.0F),
                lerpPremultiplied(blue(abgrA) * alphaA, blue(abgrB) * alphaB, t, outAlpha),
                lerpPremultiplied(green(abgrA) * alphaA, green(abgrB) * alphaB, t, outAlpha),
                lerpPremultiplied(red(abgrA) * alphaA, red(abgrB) * alphaB, t, outAlpha));
    }

    public static int lerpChannelsIndependent(int abgrA, int abgrB, float t) {
        return pack(
                lerp(alpha(abgrA), alpha(abgrB), t),
                lerp(blue(abgrA), blue(abgrB), t),
                lerp(green(abgrA), green(abgrB), t),
                lerp(red(abgrA), red(abgrB), t));
    }

    static int lerp(int from, int to, float t) {
        return Math.max(0, Math.min(255, Math.round(from + (to - from) * t)));
    }

    private static int lerpPremultiplied(float from, float to, float t, float outAlpha) {
        return Math.max(0, Math.min(255, Math.round((from + (to - from) * t) / outAlpha)));
    }

    private static int scale(int channel, int factor) {
        return (channel * factor + 127) / 255;
    }
}
