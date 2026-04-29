package dev.voxelgame.common.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.common.world.structure.Structures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverworldContentTest {
    @Test
    void generatedChunkContainsNaturalSolidBlocks() {
        Chunk chunk = new Chunk(new ChunkPos(0, 0), DimensionSettings.OVERWORLD);
        new OverworldGenerator(1337L).generate(chunk);

        int visibleMaterials = 0;
        for (int z = 0; z < ChunkPos.SIZE; z++) {
            for (int x = 0; x < ChunkPos.SIZE; x++) {
                int worldX = x;
                int worldZ = z;
                for (int y = DimensionSettings.OVERWORLD.minY(); y < DimensionSettings.OVERWORLD.maxYExclusive(); y++) {
                    short block = chunk.blockId(worldX, y, worldZ);
                    if (block == Blocks.GRASS || block == Blocks.SAND || block == Blocks.CLAY || block == Blocks.STONE) {
                        visibleMaterials++;
                    }
                }
            }
        }

        assertTrue(visibleMaterials > 0);
    }

    @Test
    void biomeRegistryContainsExpandedBiomes() {
        assertTrue(Biomes.createDefaultRegistry().findByKey("voxel:frost_peaks").isPresent());
        assertTrue(Biomes.createDefaultRegistry().findByKey("voxel:mire").isPresent());
    }

    @Test
    void villageTemplateContainsRoadsAndBuildings() {
        assertEquals("voxel:compact_village", Structures.compactVillage().key());
        assertTrue(Structures.compactVillage().blocks().size() > 80);
    }

    @Test
    void chunkSectionsTrackWhetherTheyAreEmpty() {
        ChunkSection section = new ChunkSection(0);

        assertTrue(section.isEmpty());
        section.setBlockId(1, 2, 3, Blocks.STONE);
        assertEquals(1, section.nonAirBlockCount());
        section.setBlockId(1, 2, 3, Blocks.AIR);
        assertTrue(section.isEmpty());
    }
}
