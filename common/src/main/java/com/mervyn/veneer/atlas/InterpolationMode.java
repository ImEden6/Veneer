package com.mervyn.veneer.atlas;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum InterpolationMode implements StringRepresentable {
    AUTO,
    IGNORE;

    public static final StringRepresentable.EnumCodec<InterpolationMode> CODEC = StringRepresentable.fromEnum(InterpolationMode::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
