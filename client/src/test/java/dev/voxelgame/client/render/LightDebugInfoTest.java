package dev.voxelgame.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LightDebugInfoTest {
    @Test
    void formatsSkyBlockCombinedAndEmissiveLight() {
        LightDebugInfo info = new LightDebugInfo(12, 64, -3, 9, 13, 0.78f);

        assertEquals(13, info.combinedLight());
        assertEquals("Light @ 12 64 -3: combined 13 sky 9 block 13 emissive 0.78", info.format());
    }

    @Test
    void formatsNonEmissiveBlocksCompactly() {
        LightDebugInfo info = new LightDebugInfo(0, 70, 0, 15, 0, 0.0f);

        assertEquals("Light @ 0 70 0: combined 15 sky 15 block 0 emissive 0", info.format());
    }

    @Test
    void rejectsInvalidDebugLightValues() {
        assertThrows(IllegalArgumentException.class, () -> new LightDebugInfo(0, 0, 0, -1, 0, 0.0f));
        assertThrows(IllegalArgumentException.class, () -> new LightDebugInfo(0, 0, 0, 0, 16, 0.0f));
        assertThrows(IllegalArgumentException.class, () -> new LightDebugInfo(0, 0, 0, 0, 0, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new LightDebugInfo(0, 0, 0, 0, 0, 1.1f));
    }
}
