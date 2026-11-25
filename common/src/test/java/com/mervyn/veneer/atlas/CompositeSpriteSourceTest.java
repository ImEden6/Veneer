package com.mervyn.veneer.atlas;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeSpriteSourceTest {
    private static CompositeSpriteSource parse(String fields) {
        return CompositeSpriteSource.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"sprite\": \"veneer:block/a\", \"layers\": [{\"texture\": \"minecraft:block/stone\"}]" + fields + "}"))
                .getOrThrow();
    }

    @Test
    void skipIfExistsIsOffByDefault() {
        assertFalse(parse("").skipIfExists());
        assertTrue(parse(", \"skip_if_exists\": true").skipIfExists());
    }
}
