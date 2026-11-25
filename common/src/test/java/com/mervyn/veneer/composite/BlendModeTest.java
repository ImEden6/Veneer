package com.mervyn.veneer.composite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlendModeTest {
    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0xFFFF0000;

    @Test
    void normalOpaqueReplaces() {
        assertEquals(BLUE, BlendMode.NORMAL.blend(RED, BLUE));
    }

    @Test
    void normalHalfAlphaMixes() {
        assertEquals(0xFF80007F, BlendMode.NORMAL.blend(RED, 0x80FF0000));
    }

    @Test
    void normalHalfOverHalfIsStraightAlpha() {
        assertEquals(0xC0AA0055, BlendMode.NORMAL.blend(0x800000FF, 0x80FF0000));
    }

    @ParameterizedTest
    @EnumSource(BlendMode.class)
    void transparentSourceKeepsDestination(BlendMode mode) {
        assertEquals(RED, mode.blend(RED, 0x00FF0000));
        assertEquals(0x00123456, mode.blend(0x00123456, 0x00FF0000));
    }

    @ParameterizedTest
    @EnumSource(value = BlendMode.class, names = "MULTIPLY", mode = EnumSource.Mode.EXCLUDE)
    void transparentDestinationTakesSource(BlendMode mode) {
        assertEquals(0x80FF0000, mode.blend(0x000000FF, 0x80FF0000));
    }

    @Test
    void multiplyOverTransparentIsEmpty() {
        assertEquals(0, BlendMode.MULTIPLY.blend(0x00FFFFFF, BLUE));
    }

    @Test
    void multiply() {
        assertEquals(0xFF204080, BlendMode.MULTIPLY.blend(0xFF4080FF, 0xFF808080));
    }

    @Test
    void addClamps() {
        assertEquals(0xFFFFC0FF, BlendMode.ADD.blend(0xFFC080C0, 0xFF804080));
    }

    @Test
    void screen() {
        assertEquals(0xFFA0C0FF, BlendMode.SCREEN.blend(0xFF4080FF, 0xFF808080));
    }

    @Test
    void codecNames() {
        for (BlendMode mode : BlendMode.values()) {
            assertEquals(mode, BlendMode.CODEC.byName(mode.getSerializedName()));
        }
        assertEquals("multiply", BlendMode.MULTIPLY.getSerializedName());
    }
}
