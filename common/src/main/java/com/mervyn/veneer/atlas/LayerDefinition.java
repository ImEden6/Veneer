package com.mervyn.veneer.atlas;

import com.mervyn.veneer.composite.BlendMode;
import com.mervyn.veneer.composite.Transform;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.HexFormat;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

public record LayerDefinition(Identifier texture, BlendMode blend, OptionalInt tint, Optional<Identifier> mask, boolean invertMask,
                              boolean optional, float opacity, int rotate, boolean flipX, boolean flipY) {
    private static final Pattern HEX_RGB = Pattern.compile("#?[0-9a-fA-F]{6}");

    public static final Codec<Integer> TINT_CODEC = Codec.either(Codec.STRING, Codec.INT).comapFlatMap(
            either -> either.map(LayerDefinition::parseHex, LayerDefinition::checkRgb),
            rgb -> Either.left(String.format("#%06X", rgb)));

    private static final Codec<Integer> ROTATE_CODEC = Codec.INT.validate(degrees -> switch (degrees) {
        case 0, 90, 180, 270 -> DataResult.success(degrees);
        default -> DataResult.error(() -> "Invalid rotate " + degrees + ": expected 0, 90, 180 or 270");
    });

    public static final Codec<LayerDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("texture").forGetter(LayerDefinition::texture),
            BlendMode.CODEC.optionalFieldOf("blend", BlendMode.NORMAL).forGetter(LayerDefinition::blend),
            TINT_CODEC.optionalFieldOf("tint")
                    .xmap(tint -> tint.map(OptionalInt::of).orElseGet(OptionalInt::empty),
                            tint -> tint.isPresent() ? Optional.of(tint.getAsInt()) : Optional.<Integer>empty())
                    .forGetter(LayerDefinition::tint),
            Identifier.CODEC.optionalFieldOf("mask").forGetter(LayerDefinition::mask),
            Codec.BOOL.optionalFieldOf("invert_mask", false).forGetter(LayerDefinition::invertMask),
            Codec.BOOL.optionalFieldOf("optional", false).forGetter(LayerDefinition::optional),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("opacity", 1.0F).forGetter(LayerDefinition::opacity),
            ROTATE_CODEC.optionalFieldOf("rotate", 0).forGetter(LayerDefinition::rotate),
            Codec.BOOL.optionalFieldOf("flip_x", false).forGetter(LayerDefinition::flipX),
            Codec.BOOL.optionalFieldOf("flip_y", false).forGetter(LayerDefinition::flipY)
    ).apply(instance, LayerDefinition::new));

    public Transform transform() {
        return new Transform(rotate, flipX, flipY);
    }

    private static DataResult<Integer> parseHex(String value) {
        if (!HEX_RGB.matcher(value).matches()) {
            return DataResult.error(() -> "Invalid tint '" + value + "': expected #RRGGBB or RRGGBB");
        }
        return DataResult.success(HexFormat.fromHexDigits(value, value.length() - 6, value.length()));
    }

    private static DataResult<Integer> checkRgb(int value) {
        if (value < 0 || value > 0xFFFFFF) {
            return DataResult.error(() -> "Invalid tint " + value + ": expected an RGB value from 0 to 16777215");
        }
        return DataResult.success(value);
    }
}
