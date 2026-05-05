package dev.voxelgame.server.save;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionFileLayoutTest {
    @Test
    void mapsPositiveAndNegativeChunksWithFloorDivision() {
        RegionFileLayout layout = RegionFileLayout.DEFAULT;

        assertEquals(new RegionFileLayout.RegionPos(0, 0), layout.regionForChunk(0, 0));
        assertEquals(new RegionFileLayout.RegionPos(0, 0), layout.regionForChunk(31, 31));
        assertEquals(new RegionFileLayout.RegionPos(1, 1), layout.regionForChunk(32, 32));
        assertEquals(new RegionFileLayout.RegionPos(-1, -1), layout.regionForChunk(-1, -1));
        assertEquals(new RegionFileLayout.ChunkSlot(31, 31, 1023), layout.chunkSlot(-1, -1));
    }

    @Test
    void buildsStableRegionPathAndSaveKey() {
        RegionFileLayout layout = RegionFileLayout.DEFAULT;
        Path root = Path.of("world");

        assertEquals(Path.of("world", "regions", "r.-1.2.advregion"), layout.regionPath(root, -1, 64));
        assertEquals("region:" + root.toAbsolutePath().normalize().resolve("regions").resolve("r.-1.2.advregion"),
                layout.saveKey(root, -1, 64));
    }

    @Test
    void rejectsInvalidLayoutValues() {
        assertThrows(IllegalArgumentException.class, () -> new RegionFileLayout(0, 1, ".advregion"));
        assertThrows(IllegalArgumentException.class, () -> new RegionFileLayout(32, 0, ".advregion"));
        assertThrows(IllegalArgumentException.class, () -> new RegionFileLayout(32, 1, "advregion"));
    }
}
