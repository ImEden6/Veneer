package com.mervyn.veneer.atlas;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayerDefinitionTest {
    private static int parse(JsonPrimitive json) {
        return LayerDefinition.TINT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
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
