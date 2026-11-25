package com.mervyn.veneer.composite;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransformTest {
    private static final List<String> SOURCE = List.of("ABCD", "EFGH", "IJKL", "MNOP");

    private static List<String> apply(int rotate, boolean flipX, boolean flipY) {
        Transform transform = new Transform(rotate, flipX, flipY);
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < 4; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < 4; x++) {
                row.append(SOURCE.get(transform.sourceY(x, y, 4, 4)).charAt(transform.sourceX(x, y, 4, 4)));
            }
            rows.add(row.toString());
        }
        return rows;
    }

    @Test
    void rotatesClockwise() {
        assertEquals(SOURCE, apply(0, false, false));
        assertEquals(List.of("MIEA", "NJFB", "OKGC", "PLHD"), apply(90, false, false));
        assertEquals(List.of("PONM", "LKJI", "HGFE", "DCBA"), apply(180, false, false));
        assertEquals(List.of("DHLP", "CGKO", "BFJN", "AEIM"), apply(270, false, false));
    }

    @Test
    void flips() {
        assertEquals(List.of("DCBA", "HGFE", "LKJI", "PONM"), apply(0, true, false));
        assertEquals(List.of("MNOP", "IJKL", "EFGH", "ABCD"), apply(0, false, true));
    }

    @Test
    void flipsBeforeRotating() {
        assertEquals(List.of("PLHD", "OKGC", "NJFB", "MIEA"), apply(90, true, false));
        assertEquals(List.of("AEIM", "BFJN", "CGKO", "DHLP"), apply(90, false, true));
    }

    @Test
    void quarterTurnsNeedSquareFrames() {
        assertThrows(IllegalArgumentException.class, () -> new Transform(90, false, false).validate(16, 32));
        assertThrows(IllegalArgumentException.class, () -> new Transform(270, false, false).validate(32, 16));
        assertDoesNotThrow(() -> new Transform(180, true, true).validate(16, 32));
    }
}
