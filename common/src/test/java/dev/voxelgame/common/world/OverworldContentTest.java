package dev.voxelgame.common.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.common.world.structure.BlockPlacement;
import dev.voxelgame.common.world.structure.StructureMarker;
import dev.voxelgame.common.world.structure.StructureTemplate;
import dev.voxelgame.common.world.structure.Structures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
    void biomeSpecificResourcesAppearDeterministically() {
        long seed = 1337L;

        assertBiomeResourceAppears(seed, "voxel:cozy_meadow", Set.of(Blocks.TWIG_PILE, Blocks.SMALL_STONE, Blocks.BERRY_BUSH, Blocks.HERB_PLANTER));
        assertBiomeResourceAppears(seed, "voxel:pine_forest", Set.of(Blocks.PINE_LOG, Blocks.PINE_LEAVES));
        assertBiomeResourceAppears(seed, "voxel:lakeside", Set.of(Blocks.CLAY, Blocks.CLAY_DEPOSIT));
        assertBiomeResourceAppears(seed, "voxel:mushroom_grove", Set.of(Blocks.MUSHROOM_CLUSTER, Blocks.RED_MUSHROOM, Blocks.GLOW_MUSHROOM));
        assertBiomeResourceAppearsNear(seed, "voxel:old_ruins", Set.of(Blocks.MOSSY_STONE, Blocks.GLOW_CRYSTAL_NODE), ReproducibleWorldSeeds.OLD_RUINS.focusX(), ReproducibleWorldSeeds.OLD_RUINS.focusZ(), 4);
        assertBiomeResourceAppearsNear(seed, "voxel:highlands", Set.of(Blocks.SMALL_STONE, Blocks.GLOW_CRYSTAL_NODE, Blocks.STONE), ReproducibleWorldSeeds.HIGHLANDS_ORES.focusX(), ReproducibleWorldSeeds.HIGHLANDS_ORES.focusZ(), 4);
        assertBiomeResourceAppearsNear(seed, "voxel:frost_peaks", Set.of(Blocks.SNOW, Blocks.ICE, Blocks.GLOW_CRYSTAL_NODE), ReproducibleWorldSeeds.FROST_PEAKS.focusX(), ReproducibleWorldSeeds.FROST_PEAKS.focusZ(), 4);
    }

    @Test
    void villageTemplateContainsRoadsAndBuildings() {
        assertEquals("voxel:compact_village", Structures.compactVillage().key());
        assertTrue(Structures.compactVillage().blocks().size() > 80);
        assertTrue(Structures.compactVillage().markers("entity").size() >= 2);
        assertTrue(Structures.compactVillage().lootMarkers().size() >= 3);
    }

    @Test
    void mushroomCircleTemplateCreatesGroveAdventureHook() {
        StructureTemplate circle = Structures.mushroomCircle();

        assertEquals("voxel:mushroom_circle", circle.key());
        assertTrue(circle.blocks().stream().anyMatch(block -> block.blockId() == Blocks.SPORE_BLOSSOM));
        assertTrue(circle.blocks().stream().anyMatch(block -> block.blockId() == Blocks.GLOW_MUSHROOM));
        assertTrue(circle.blocks().stream().anyMatch(block -> block.blockId() == Blocks.GLOW_CRYSTAL_NODE));
        assertTrue(circle.markers("metadata").stream().anyMatch(marker -> marker.key().equals("voxel:mushroom_circle_center")));
    }

    @Test
    void mushroomGroveCanGenerateMushroomCircleStructures() {
        OverworldGenerator generator = new OverworldGenerator(1337L);
        OverworldGenerator.GeneratedStructure structure = generator.structureAtChunk(new ChunkPos(20, -45)).orElseThrow();

        assertEquals("voxel:mushroom_grove", generator.biomeAt(20 * ChunkPos.SIZE + 8, -45 * ChunkPos.SIZE + 8).key());
        assertEquals(Structures.mushroomCircle().key(), structure.template().key());
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

    private static void assertBiomeResourceAppears(long seed, String biomeKey, Set<Short> resourceBlocks) {
        assertBiomeResourceAppearsNear(seed, biomeKey, resourceBlocks, 0, 0, 36);
    }

    private static void assertBiomeResourceAppearsNear(long seed, String biomeKey, Set<Short> resourceBlocks, int focusX, int focusZ, int radiusChunks) {
        OverworldGenerator generator = new OverworldGenerator(seed);
        int matchingColumns = 0;
        ChunkPos focus = ChunkPos.fromBlock(focusX, focusZ);
        for (int chunkZ = focus.z() - radiusChunks; chunkZ <= focus.z() + radiusChunks; chunkZ++) {
            for (int chunkX = focus.x() - radiusChunks; chunkX <= focus.x() + radiusChunks; chunkX++) {
                ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                int centerX = chunkX * ChunkPos.SIZE + ChunkPos.SIZE / 2;
                int centerZ = chunkZ * ChunkPos.SIZE + ChunkPos.SIZE / 2;
                boolean broadScan = radiusChunks > 8;
                if (broadScan && !generator.biomeAt(centerX, centerZ).key().equals(biomeKey)) {
                    continue;
                }
                Chunk chunk = new Chunk(pos, DimensionSettings.OVERWORLD);
                generator.generate(chunk);
                for (int localZ = 0; localZ < ChunkPos.SIZE; localZ++) {
                    for (int localX = 0; localX < ChunkPos.SIZE; localX++) {
                        int worldX = chunkX * ChunkPos.SIZE + localX;
                        int worldZ = chunkZ * ChunkPos.SIZE + localZ;
                        BiomeType biome = generator.biomeAt(worldX, worldZ);
                        if (!biome.key().equals(biomeKey)) {
                            continue;
                        }
                        matchingColumns++;
                        int height = generator.terrainHeight(worldX, worldZ, biome);
                        int minY = Math.max(DimensionSettings.OVERWORLD.minY(), height - 4);
                        int maxY = Math.min(DimensionSettings.OVERWORLD.maxYExclusive() - 1, height + 8);
                        for (int y = minY; y <= maxY; y++) {
                            if (resourceBlocks.contains(chunk.blockId(worldX, y, worldZ))) {
                                return;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(matchingColumns > 0, "Expected biome columns for " + biomeKey);
        fail("Expected biome resource in " + biomeKey);
    }
}
