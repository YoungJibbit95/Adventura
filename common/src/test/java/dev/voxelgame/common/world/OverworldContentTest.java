package dev.voxelgame.common.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.common.world.structure.BlockPlacement;
import dev.voxelgame.common.world.structure.StructureMarker;
import dev.voxelgame.common.world.structure.StructureTemplate;
import dev.voxelgame.common.world.structure.Structures;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        assertTrue(Structures.compactVillage().markers("entity").size() >= 2);
        assertTrue(Structures.compactVillage().lootMarkers().size() >= 3);
    }

    @Test
    void structureTemplateKeepsTypedMarkers() {
        StructureMarker loot = StructureMarker.loot("voxel:test_crate", 1, 2, 3);
        StructureMarker entity = StructureMarker.entity("voxel:test_spawn", 4, 5, 6);
        StructureTemplate template = new StructureTemplate(
                "voxel:test_structure",
                List.of(new BlockPlacement(0, 0, 0, Blocks.STONE)),
                List.of(loot, entity)
        );

        assertEquals(List.of(loot), template.lootMarkers());
        assertEquals(List.of(entity), template.markers("entity"));
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
