package dev.voxelgame.common.world.light;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.DimensionSettings;
import dev.voxelgame.common.world.InMemoryWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightEngineTest {
    @Test
    void torchLightPropagatesOutward() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        Chunk chunk = world.getOrCreateChunk(new ChunkPos(0, 0));
        chunk.setBlockId(8, 8, 8, Blocks.TORCH);

        new LightEngine().rebuildChunkLighting(world, new ChunkPos(0, 0));

        assertEquals(14, world.blockLight(8, 8, 8));
        assertEquals(13, world.blockLight(9, 8, 8));
    }

    @Test
    void registeredLightSourcesKeepExpectedValues() {
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();

        assertEquals(14, blocks.requireById(Blocks.TORCH).lightEmission());
        assertEquals(13, blocks.requireById(Blocks.LANTERN).lightEmission());
        assertEquals(15, blocks.requireById(Blocks.ANCIENT_LANTERN).lightEmission());
        assertEquals(14, blocks.requireById(Blocks.CAMPFIRE_ACTIVE).lightEmission());
        assertEquals(10, blocks.requireById(Blocks.GLOW_CRYSTAL_NODE).lightEmission());
        assertEquals(1, blocks.requireById(Blocks.MUSHROOM_CLUSTER).lightEmission());
        assertEquals(0, blocks.requireById(Blocks.CAMPFIRE).lightEmission());
        assertEquals(0, blocks.requireById(Blocks.CAMPFIRE_BURNED_OUT).lightEmission());
    }

    @Test
    void configuredLightSourcesSeedBlockLightAtEmissionStrength() {
        assertSourceSeedsBlockLight(Blocks.TORCH, 14);
        assertSourceSeedsBlockLight(Blocks.LANTERN, 13);
        assertSourceSeedsBlockLight(Blocks.ANCIENT_LANTERN, 15);
        assertSourceSeedsBlockLight(Blocks.CAMPFIRE_ACTIVE, 14);
        assertSourceSeedsBlockLight(Blocks.GLOW_CRYSTAL_NODE, 10);
        assertSourceSeedsBlockLight(Blocks.MUSHROOM_CLUSTER, 1);
    }

    @Test
    void blockLightPropagatesAcrossLoadedChunkBoundary() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        world.getOrCreateChunk(new ChunkPos(0, 0)).setBlockId(15, 8, 8, Blocks.TORCH);
        world.getOrCreateChunk(new ChunkPos(1, 0));

        new LightEngine().rebuildChunkLighting(world, new ChunkPos(0, 0));

        assertEquals(14, world.blockLight(15, 8, 8));
        assertEquals(13, world.blockLight(16, 8, 8));
    }

    @Test
    void removingBoundaryEmitterClearsNeighborBlockLight() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        Chunk source = world.getOrCreateChunk(new ChunkPos(0, 0));
        world.getOrCreateChunk(new ChunkPos(1, 0));
        LightEngine lightEngine = new LightEngine();
        source.setBlockId(15, 8, 8, Blocks.TORCH);
        lightEngine.rebuildChunkLighting(world, new ChunkPos(0, 0));

        source.setBlockId(15, 8, 8, Blocks.AIR);
        lightEngine.rebuildChunkLighting(world, new ChunkPos(0, 0));

        assertEquals(0, world.blockLight(15, 8, 8));
        assertEquals(0, world.blockLight(16, 8, 8));
    }

    @Test
    void skyLightFallsOffBelowOpaqueRoofButPassesThroughWater() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        Chunk chunk = world.getOrCreateChunk(new ChunkPos(0, 0));
        buildRoof(chunk, 2, 6, 2, 6, 10);
        chunk.setBlockId(8, 10, 8, Blocks.WATER);

        new LightEngine().rebuildSkyLightColumn(world, chunk);

        assertEquals(15, world.skyLight(4, 10, 4));
        assertEquals(12, world.skyLight(4, 9, 4));
        assertEquals(15, world.skyLight(8, 10, 8));
        assertEquals(15, world.skyLight(8, 9, 8));
    }

    @Test
    void skyLightSpillsUnderOverhangWithDistanceFalloff() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        Chunk chunk = world.getOrCreateChunk(new ChunkPos(0, 0));
        buildRoof(chunk, 4, 8, 4, 8, 10);

        new LightEngine().rebuildSkyLightColumn(world, chunk);

        assertEquals(14, world.skyLight(4, 9, 6));
        assertEquals(12, world.skyLight(6, 9, 6));
    }

    @Test
    void deepCaveInteriorIsDarkerThanMouth() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        Chunk chunk = world.getOrCreateChunk(new ChunkPos(0, 0));
        buildRoof(chunk, 2, 13, 2, 13, 10);

        new LightEngine().rebuildSkyLightColumn(world, chunk);

        int mouthLight = world.skyLight(2, 9, 8);
        int caveLight = world.skyLight(8, 9, 8);

        assertEquals(14, mouthLight);
        assertTrue(caveLight < mouthLight);
        assertEquals(9, caveLight);
    }

    @Test
    void skyLightPropagatesAcrossLoadedChunkBoundary() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        world.getOrCreateChunk(new ChunkPos(0, 0));
        world.getOrCreateChunk(new ChunkPos(1, 0));
        buildWorldRoof(world, 15, 21, 6, 10, 10);

        new LightEngine().rebuildChunkLighting(world, new ChunkPos(0, 0));

        assertEquals(14, world.skyLight(15, 9, 8));
        assertEquals(13, world.skyLight(16, 9, 8));
        assertEquals(12, world.skyLight(17, 9, 8));
    }

    private static void buildRoof(Chunk chunk, int minX, int maxX, int minZ, int maxZ, int y) {
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                chunk.setBlockId(x, y, z, Blocks.STONE);
            }
        }
    }

    private static void buildWorldRoof(InMemoryWorld world, int minX, int maxX, int minZ, int maxZ, int y) {
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                world.setBlockId(x, y, z, Blocks.STONE);
            }
        }
    }

    private static void assertSourceSeedsBlockLight(short blockId, int expectedLight) {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        Chunk chunk = world.getOrCreateChunk(new ChunkPos(0, 0));
        chunk.setBlockId(8, 8, 8, blockId);

        new LightEngine().rebuildChunkLighting(world, new ChunkPos(0, 0));

        assertEquals(expectedLight, world.blockLight(8, 8, 8));
    }
}
