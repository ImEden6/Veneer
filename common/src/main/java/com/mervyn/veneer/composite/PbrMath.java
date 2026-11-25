package com.mervyn.veneer.composite;

public final class PbrMath {
    public static final int NEUTRAL_NORMAL_ABGR = 0xFFFF8080;
    public static final int NEUTRAL_SPECULAR_ABGR = 0xFF000000;

    private PbrMath() {
    }

    public static int blendNormal(int dstAbgr, int srcAbgr, int effectiveAlpha) {
        if (effectiveAlpha <= 0) {
            return dstAbgr;
        }
        if (effectiveAlpha >= 255) {
            return srcAbgr;
        }
        return PixelMath.lerpChannelsIndependent(dstAbgr, srcAbgr, effectiveAlpha / 255.0F);
    }

    public static int blendSpecular(int dstAbgr, int srcAbgr, int effectiveAlpha) {
        if (effectiveAlpha <= 0) {
            return dstAbgr;
        }
        if (effectiveAlpha >= 255) {
            return srcAbgr;
        }
        int flags = effectiveAlpha >= 128 ? srcAbgr : dstAbgr;
        return PixelMath.pack(
                PixelMath.alpha(flags),
                PixelMath.blue(flags),
                PixelMath.green(flags),
                PixelMath.lerp(PixelMath.red(dstAbgr), PixelMath.red(srcAbgr), effectiveAlpha / 255.0F));
    }

    public static int lerpNormal(int abgrA, int abgrB, float t) {
        return PixelMath.lerpChannelsIndependent(abgrA, abgrB, t);
    }

    public static int lerpSpecular(int abgrA, int abgrB, float t) {
        int flags = t >= 0.5F ? abgrB : abgrA;
        return PixelMath.pack(
                PixelMath.alpha(flags),
                PixelMath.blue(flags),
                PixelMath.green(flags),
                PixelMath.lerp(PixelMath.red(abgrA), PixelMath.red(abgrB), t));
    }
}
