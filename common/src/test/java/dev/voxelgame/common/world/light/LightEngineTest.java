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
        LightSourceRegistry sources = LightSourceRegistry.fromBlocks(blocks);

        assertEquals(14, blocks.requireById(Blocks.TORCH).lightEmission());
        assertEquals(13, blocks.requireById(Blocks.LANTERN).lightEmission());
        assertEquals(15, blocks.requireById(Blocks.ANCIENT_LANTERN).lightEmission());
        assertEquals(14, blocks.requireById(Blocks.CAMPFIRE_ACTIVE).lightEmission());
        assertEquals(10, blocks.requireById(Blocks.GLOW_CRYSTAL_NODE).lightEmission());
        assertEquals(8, blocks.requireById(Blocks.GLOW_MUSHROOM).lightEmission());
        assertEquals(5, blocks.requireById(Blocks.SPORE_BLOSSOM).lightEmission());
        assertEquals(15, blocks.requireById(Blocks.LAVA).lightEmission());
        assertEquals(1, blocks.requireById(Blocks.MUSHROOM_CLUSTER).lightEmission());
        assertEquals(0, blocks.requireById(Blocks.CAMPFIRE).lightEmission());
        assertEquals(0, blocks.requireById(Blocks.CAMPFIRE_BURNED_OUT).lightEmission());
        assertEquals(9, sources.sourceCount());
        assertEquals(14, sources.lightValue(Blocks.CAMPFIRE_ACTIVE));
        assertEquals(0, sources.lightValue(Blocks.CAMPFIRE));
        assertTrue(sources.sourceFor(Blocks.GLOW_CRYSTAL_NODE).isPresent());
    }

    @Test
    void configuredLightSourcesSeedBlockLightAtEmissionStrength() {
        assertSourceSeedsBlockLight(Blocks.TORCH, 14);
        assertSourceSeedsBlockLight(Blocks.LANTERN, 13);
        assertSourceSeedsBlockLight(Blocks.ANCIENT_LANTERN, 15);
        assertSourceSeedsBlockLight(Blocks.CAMPFIRE_ACTIVE, 14);
        assertSourceSeedsBlockLight(Blocks.GLOW_CRYSTAL_NODE, 10);
        assertSourceSeedsBlockLight(Blocks.GLOW_MUSHROOM, 8);
        assertSourceSeedsBlockLight(Blocks.SPORE_BLOSSOM, 5);
        assertSourceSeedsBlockLight(Blocks.LAVA, 15);
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
    void skyLightFallsOffBelowOpaqueRoofAndWeakensThroughWater() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        Chunk chunk = world.getOrCreateChunk(new ChunkPos(0, 0));
        buildRoof(chunk, 2, 6, 2, 6, 10);
        chunk.setBlockId(8, 10, 8, Blocks.WATER);

        new LightEngine().rebuildSkyLightColumn(world, chunk);

        assertEquals(15, world.skyLight(4, 10, 4));
        assertEquals(12, world.skyLight(4, 9, 4));
        assertEquals(15, world.skyLight(8, 10, 8));
        assertEquals(14, world.skyLight(8, 9, 8));
    }

    @Test
    void openCutoutStaysBrightButLeavesDimSkyLightSlightly() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        Chunk chunk = world.getOrCreateChunk(new ChunkPos(0, 0));
        chunk.setBlockId(4, 10, 4, Blocks.WILD_GRASS);
        buildWorldColumns(world, 7, 9, 7, 9, 10, 10, Blocks.SKYROOT_LEAVES);

        new LightEngine().rebuildSkyLightColumn(world, chunk);

        assertEquals(15, world.skyLight(4, 9, 4));
        assertEquals(13, world.skyLight(8, 9, 8));
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

    @Test
    void skyLightRemainsContinuousBelowHighMountainAcrossChunkBoundary() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 64), Blocks.createDefaultRegistry());
        world.getOrCreateChunk(new ChunkPos(0, 0));
        world.getOrCreateChunk(new ChunkPos(1, 0));
        buildWorldColumns(world, 14, 17, 6, 10, 12, 30, Blocks.STONE);

        new LightEngine().rebuildChunkLighting(world, new ChunkPos(0, 0));

        int leftBoundaryCaveLight = world.skyLight(15, 11, 8);
        int rightBoundaryCaveLight = world.skyLight(16, 11, 8);
        assertEquals(leftBoundaryCaveLight, rightBoundaryCaveLight);
        assertTrue(leftBoundaryCaveLight > 0);
        assertTrue(leftBoundaryCaveLight < 15);
        assertEquals(15, world.skyLight(13, 11, 8));
        assertEquals(15, world.skyLight(18, 11, 8));
    }

    @Test
    void blockLightPassesThroughTransparentBlocksAcrossChunkBoundary() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        world.getOrCreateChunk(new ChunkPos(0, 0));
        world.getOrCreateChunk(new ChunkPos(1, 0));
        world.setBlockId(15, 8, 8, Blocks.TORCH);
        world.setBlockId(16, 8, 8, Blocks.WATER);
        world.setBlockId(17, 8, 8, Blocks.ICE);
        world.setBlockId(18, 8, 8, Blocks.SKYROOT_LEAVES);

        new LightEngine().rebuildChunkLighting(world, new ChunkPos(0, 0));

        assertEquals(14, world.blockLight(15, 8, 8));
        assertEquals(13, world.blockLight(16, 8, 8));
        assertEquals(12, world.blockLight(17, 8, 8));
        assertEquals(11, world.blockLight(18, 8, 8));
    }

    @Test
    void boundaryPropagationDoesNotCreateUnloadedNeighborChunk() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        world.getOrCreateChunk(new ChunkPos(0, 0)).setBlockId(15, 8, 8, Blocks.TORCH);
        LightEngine lightEngine = new LightEngine();

        lightEngine.rebuildChunkLighting(world, new ChunkPos(0, 0));

        assertTrue(world.findChunk(new ChunkPos(1, 0)).isEmpty());
        assertEquals(14, world.blockLight(15, 8, 8));
        assertEquals(0, world.blockLight(16, 8, 8));

        world.getOrCreateChunk(new ChunkPos(1, 0));
        lightEngine.rebuildChunkLighting(world, new ChunkPos(0, 0));

        assertEquals(13, world.blockLight(16, 8, 8));
    }

    @Test
    void lightSourceOnNeighborBoundaryLightsBothSides() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        world.getOrCreateChunk(new ChunkPos(0, 0));
        world.getOrCreateChunk(new ChunkPos(1, 0)).setBlockId(16, 8, 8, Blocks.TORCH);

        new LightEngine().rebuildChunkLighting(world, new ChunkPos(1, 0));

        assertEquals(13, world.blockLight(15, 8, 8));
        assertEquals(14, world.blockLight(16, 8, 8));
        assertEquals(13, world.blockLight(17, 8, 8));
    }

    @Test
    void fullRebuildFallbackClearsStaleBoundaryBlockLight() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        world.getOrCreateChunk(new ChunkPos(0, 0)).setBlockId(15, 8, 8, Blocks.TORCH);
        world.getOrCreateChunk(new ChunkPos(1, 0));
        LightEngine lightEngine = new LightEngine();
        lightEngine.rebuildChunkLighting(world, new ChunkPos(0, 0));

        world.setBlockLight(16, 8, 8, 0);
        world.setBlockLight(20, 8, 8, 15);
        lightEngine.rebuildChunkLighting(world, new ChunkPos(0, 0));

        assertEquals(13, world.blockLight(16, 8, 8));
        assertEquals(9, world.blockLight(20, 8, 8));
    }

    @Test
    void incrementalBlockLightAddPropagatesAcrossChunkBoundary() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        world.getOrCreateChunk(new ChunkPos(0, 0));
        world.getOrCreateChunk(new ChunkPos(1, 0));
        world.setBlockId(15, 8, 8, Blocks.LANTERN);

        LightEngine.LightUpdateResult result = new LightEngine().updateBlockLight(world, 15, 8, 8);

        assertEquals(false, result.fullRebuildFallback());
        assertTrue(result.affectedChunks().contains(new ChunkPos(0, 0)));
        assertTrue(result.affectedChunks().contains(new ChunkPos(1, 0)));
        assertEquals(13, world.blockLight(15, 8, 8));
        assertEquals(12, world.blockLight(16, 8, 8));
    }

    @Test
    void incrementalBlockLightRemoveClearsGhostLightAndRepropagatesOtherSources() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        world.getOrCreateChunk(new ChunkPos(0, 0));
        world.setBlockId(8, 8, 8, Blocks.TORCH);
        world.setBlockId(12, 8, 8, Blocks.LANTERN);
        LightEngine lightEngine = new LightEngine();
        lightEngine.rebuildChunkLighting(world, new ChunkPos(0, 0));

        world.setBlockId(8, 8, 8, Blocks.AIR);
        LightEngine.LightUpdateResult result = lightEngine.updateBlockLight(world, 8, 8, 8);

        assertEquals(false, result.fullRebuildFallback());
        assertEquals(9, world.blockLight(8, 8, 8));
        assertEquals(10, world.blockLight(9, 8, 8));
        assertEquals(13, world.blockLight(12, 8, 8));
    }

    @Test
    void invalidIncrementalBlockLightUpdateRequestsFullRebuildFallback() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());

        LightEngine.LightUpdateResult result = new LightEngine().updateBlockLight(world, 1, 8, 1);

        assertEquals(true, result.fullRebuildFallback());
        assertTrue(result.affectedChunks().isEmpty());
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

    private static void buildWorldColumns(
            InMemoryWorld world,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            int minY,
            int maxY,
            short blockId
    ) {
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    world.setBlockId(x, y, z, blockId);
                }
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
