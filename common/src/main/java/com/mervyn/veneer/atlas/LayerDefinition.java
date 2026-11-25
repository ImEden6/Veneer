package com.mervyn.veneer.atlas;

import com.mervyn.veneer.composite.BlendMode;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.HexFormat;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

public record LayerDefinition(ResourceLocation texture, BlendMode blend, OptionalInt tint, Optional<ResourceLocation> mask, boolean invertMask) {
    private static final Pattern HEX_RGB = Pattern.compile("#?[0-9a-fA-F]{6}");

    public static final Codec<Integer> TINT_CODEC = Codec.either(Codec.STRING, Codec.INT).comapFlatMap(
            either -> either.map(LayerDefinition::parseHex, LayerDefinition::checkRgb),
            rgb -> Either.left(String.format("#%06X", rgb)));

    public static final Codec<LayerDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(LayerDefinition::texture),
            BlendMode.CODEC.optionalFieldOf("blend", BlendMode.NORMAL).forGetter(LayerDefinition::blend),
            TINT_CODEC.optionalFieldOf("tint")
                    .xmap(tint -> tint.map(OptionalInt::of).orElseGet(OptionalInt::empty),
                            tint -> tint.isPresent() ? Optional.of(tint.getAsInt()) : Optional.<Integer>empty())
                    .forGetter(LayerDefinition::tint),
            ResourceLocation.CODEC.optionalFieldOf("mask").forGetter(LayerDefinition::mask),
            Codec.BOOL.optionalFieldOf("invert_mask", false).forGetter(LayerDefinition::invertMask)
    ).apply(instance, LayerDefinition::new));

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
