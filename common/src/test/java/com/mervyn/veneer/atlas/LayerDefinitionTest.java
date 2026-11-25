package com.mervyn.veneer.atlas;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mervyn.veneer.composite.BlendMode;
import com.mervyn.veneer.composite.Transform;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayerDefinitionTest {
    private static int parse(JsonPrimitive json) {
        return LayerDefinition.TINT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    private static boolean optional(String json) {
        return LayerDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow().optional();
    }

    @Test
    void layersAreRequiredByDefault() {
        assertFalse(optional("{\"texture\": \"minecraft:block/stone\"}"));
        assertTrue(optional("{\"texture\": \"minecraft:block/stone\", \"optional\": true}"));
    }

    private static DataResult<LayerDefinition> layer(String fields) {
        return LayerDefinition.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"texture\": \"minecraft:block/stone\"" + fields + "}"));
    }

    @Test
    void parsesOpacityAndTransforms() {
        LayerDefinition plain = layer("").getOrThrow();
        assertEquals(1.0F, plain.opacity());
        assertEquals(Transform.NONE, plain.transform());

        LayerDefinition turned = layer(", \"blend\": \"overlay\", \"opacity\": 0.75, \"rotate\": 90, \"flip_x\": true")
                .getOrThrow();
        assertEquals(BlendMode.OVERLAY, turned.blend());
        assertEquals(0.75F, turned.opacity());
        assertEquals(new Transform(90, true, false), turned.transform());
        assertTrue(layer(", \"flip_y\": true").getOrThrow().flipY());
    }

    @Test
    void rejectsBadOpacityAndRotation() {
        assertTrue(layer(", \"rotate\": 45").isError());
        assertTrue(layer(", \"rotate\": -90").isError());
        assertTrue(layer(", \"opacity\": 1.5").isError());
        assertTrue(layer(", \"opacity\": -0.1").isError());
    }

    @Test
    void parsesHexAndIntegers() {
        assertEquals(0xFF00AA, parse(new JsonPrimitive("#FF00AA")));
        assertEquals(0xFF00AA, parse(new JsonPrimitive("ff00aa")));
        assertEquals(0xFF00AA, parse(new JsonPrimitive(0xFF00AA)));
    }

    @Test
    void rejectsInvalidTints() {
        List<JsonPrimitive> invalid = List.of(new JsonPrimitive(""), new JsonPrimitive("#FFF"),
                new JsonPrimitive("#GG00AA"), new JsonPrimitive("##F00AA"), new JsonPrimitive("#FF00AA00"),
                new JsonPrimitive(-1), new JsonPrimitive(0x1000000));
        for (JsonPrimitive json : invalid) {
            assertTrue(LayerDefinition.TINT_CODEC.parse(JsonOps.INSTANCE, json).isError(), json.toString());
        }
    }
}
