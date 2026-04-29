package dev.voxelgame.client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BitmapFontTest {
    @Test
    void measuresTextWithGlyphSpacing() {
        assertEquals(17.0f, BitmapFont.textWidth("ABC", 1.0f));
        assertEquals(34.0f, BitmapFont.textWidth("ABC", 2.0f));
    }

    @Test
    void returnsPixelsForKnownGlyphs() {
        assertTrue(BitmapFont.pixel('A', 2, 0));
        assertTrue(BitmapFont.pixel('1', 2, 0));
    }
}
