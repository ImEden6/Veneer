package com.mervyn.veneer.composite;

public enum PbrType {
    NORMAL("_n", PbrMath.NEUTRAL_NORMAL_ABGR),
    SPECULAR("_s", PbrMath.NEUTRAL_SPECULAR_ABGR);

    private final String suffix;
    private final int neutralAbgr;

    PbrType(String suffix, int neutralAbgr) {
        this.suffix = suffix;
        this.neutralAbgr = neutralAbgr;
    }

    public String suffix() {
        return suffix;
    }

    public int neutralAbgr() {
        return neutralAbgr;
    }

    public int lerp(int abgrA, int abgrB, float t) {
        return switch (this) {
            case NORMAL -> PbrMath.lerpNormal(abgrA, abgrB, t);
            case SPECULAR -> PbrMath.lerpSpecular(abgrA, abgrB, t);
        };
    }

    public int blend(int dstAbgr, int srcAbgr, int effectiveAlpha) {
        return switch (this) {
            case NORMAL -> PbrMath.blendNormal(dstAbgr, srcAbgr, effectiveAlpha);
            case SPECULAR -> PbrMath.blendSpecular(dstAbgr, srcAbgr, effectiveAlpha);
        };
    }
}
