package dev.voxelgame.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LightDebugInfoTest {
    @Test
    void formatsSkyBlockCombinedAndEmissiveLight() {
        LightDebugInfo info = new LightDebugInfo(
                12,
                64,
                -3,
                9,
                13,
                0.78f,
                "voxel:lantern",
                13,
                "cutout-open"
        );

        assertEquals(13, info.combinedLight());
        assertEquals("Light @ 12 64 -3 voxel:lantern: combined 13 sky 9 block 13 source 13 emissive 0.78 occlusion cutout-open", info.format());
    }

    @Test
    void formatsNonEmissiveBlocksCompactly() {
        LightDebugInfo info = new LightDebugInfo(0, 70, 0, 15, 0, 0.0f, "voxel:stone", 0, "opaque");

        assertEquals("Light @ 0 70 0 voxel:stone: combined 15 sky 15 block 0 source 0 emissive 0 occlusion opaque", info.format());
    }

    @Test
    void rejectsInvalidDebugLightValues() {
        assertThrows(IllegalArgumentException.class, () -> new LightDebugInfo(0, 0, 0, -1, 0, 0.0f));
        assertThrows(IllegalArgumentException.class, () -> new LightDebugInfo(0, 0, 0, 0, 16, 0.0f));
        assertThrows(IllegalArgumentException.class, () -> new LightDebugInfo(0, 0, 0, 0, 0, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new LightDebugInfo(0, 0, 0, 0, 0, 1.1f));
        assertThrows(IllegalArgumentException.class, () -> new LightDebugInfo(0, 0, 0, 0, 0, 0.0f, "voxel:air", 16, "open"));
        assertThrows(IllegalArgumentException.class, () -> new LightDebugInfo(0, 0, 0, 0, 0, 0.0f, " ", 0, "open"));
    }
}
