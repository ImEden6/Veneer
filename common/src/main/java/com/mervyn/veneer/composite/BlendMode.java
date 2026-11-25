package com.mervyn.veneer.composite;

import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum BlendMode implements StringRepresentable {
    NORMAL,
    MULTIPLY,
    ADD,
    SCREEN,
    OVERLAY,
    DARKEN,
    LIGHTEN;

    public static final StringRepresentable.EnumCodec<BlendMode> CODEC = StringRepresentable.fromEnum(BlendMode::values);

    public int blend(int dstAbgr, int srcAbgr) {
        int srcAlpha = PixelMath.alpha(srcAbgr);
        if (srcAlpha == 0) {
            return dstAbgr;
        }
        int dstAlpha = PixelMath.alpha(dstAbgr);
        if (dstAlpha == 0) {
            return this == MULTIPLY ? 0 : srcAbgr;
        }

        float srcA = srcAlpha / 255.0F;
        float dstWeight = dstAlpha / 255.0F * (1.0F - srcA);
        float outA = srcA + dstWeight;

        return PixelMath.pack(
                toByte(outA),
                composite(PixelMath.blue(dstAbgr), PixelMath.blue(srcAbgr), srcA, dstWeight, outA),
                composite(PixelMath.green(dstAbgr), PixelMath.green(srcAbgr), srcA, dstWeight, outA),
                composite(PixelMath.red(dstAbgr), PixelMath.red(srcAbgr), srcA, dstWeight, outA));
    }

    private int composite(int dst, int src, float srcA, float dstWeight, float outA) {
        float d = dst / 255.0F;
        float s = src / 255.0F;
        float mixed = switch (this) {
            case NORMAL -> s;
            case MULTIPLY -> d * s;
            case ADD -> Math.min(d + s, 1.0F);
            case SCREEN -> d + s - d * s;
            case OVERLAY -> d < 0.5F ? 2.0F * d * s : 1.0F - 2.0F * (1.0F - d) * (1.0F - s);
            case DARKEN -> Math.min(d, s);
            case LIGHTEN -> Math.max(d, s);
        };
        return toByte((mixed * srcA + d * dstWeight) / outA);
    }

    private static int toByte(float value) {
        return Mth.clamp(Math.round(value * 255.0F), 0, 255);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
