package dev.voxelgame.common.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.common.world.structure.Structures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverworldGeneratorTest {
    @Test
    void terrainHeightIsDeterministicForSeed() {
        OverworldGenerator a = new OverworldGenerator(42L);
        OverworldGenerator b = new OverworldGenerator(42L);
        BiomeType biome = a.biomeAt(100, -25);
        assertEquals(a.terrainHeight(100, -25, biome), b.terrainHeight(100, -25, biome));
    }

    @Test
    void spawnChunkAlwaysContainsStarterCampsite() {
        OverworldGenerator generator = new OverworldGenerator(42L);
        OverworldGenerator.GeneratedStructure structure = generator.structureAtChunk(new ChunkPos(0, 0)).orElseThrow();
        Chunk chunk = new Chunk(new ChunkPos(0, 0), DimensionSettings.OVERWORLD);

        generator.generate(chunk);

        assertEquals(Structures.campsite().key(), structure.template().key());
        assertEquals(12, structure.originX());
        assertEquals(8, structure.originZ());
        assertEquals(Blocks.CAMPFIRE, chunk.blockId(structure.originX(), structure.originY() + 1, structure.originZ()));
        assertEquals(Blocks.STORAGE_CRATE, chunk.blockId(structure.originX(), structure.originY() + 1, structure.originZ() + 2));
    }

    @Test
    void spawnChunkContainsReadableStarterResources() {
        OverworldGenerator generator = new OverworldGenerator(42L);
        Chunk chunk = new Chunk(new ChunkPos(0, 0), DimensionSettings.OVERWORLD);

        generator.generate(chunk);

        assertStarterResource(generator, chunk, 4, 6, Blocks.TWIG_PILE);
        assertStarterResource(generator, chunk, 6, 3, Blocks.TWIG_PILE);
        assertStarterResource(generator, chunk, 8, 13, Blocks.TWIG_PILE);
        assertStarterResource(generator, chunk, 3, 8, Blocks.SMALL_STONE);
        assertStarterResource(generator, chunk, 5, 12, Blocks.SMALL_STONE);
        assertStarterResource(generator, chunk, 15, 13, Blocks.SMALL_STONE);
        assertStarterResource(generator, chunk, 5, 5, Blocks.WILD_GRASS);
        assertStarterResource(generator, chunk, 7, 12, Blocks.WILD_GRASS);
        assertStarterResource(generator, chunk, 2, 10, Blocks.WILD_GRASS);
        assertStarterResource(generator, chunk, 4, 13, Blocks.BERRY_BUSH);
        assertStarterResource(generator, chunk, 7, 4, Blocks.BERRY_BUSH);
        assertStarterResource(generator, chunk, 2, 6, Blocks.HERB_PLANTER);
        assertStarterResource(generator, chunk, 6, 14, Blocks.SUN_BLOOM);
        assertStarterResource(generator, chunk, 10, 13, Blocks.RED_MUSHROOM);
        assertStarterResource(generator, chunk, 14, 14, Blocks.MUSHROOM_CLUSTER);
    }

    private static void assertStarterResource(OverworldGenerator generator, Chunk chunk, int x, int z, short blockId) {
        int y = generator.terrainHeight(x, z, generator.biomeAt(x, z)) + 1;
        assertEquals(blockId, chunk.blockId(x, y, z));
    }
}
