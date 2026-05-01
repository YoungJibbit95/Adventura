package dev.voxelgame.common.world;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.AmbientEntitySpawner;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.common.world.structure.Structures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverworldGeneratorTest {
    @Test
    void terrainHeightIsDeterministicForSeed() {
        OverworldGenerator a = new OverworldGenerator(42L);
        OverworldGenerator b = new OverworldGenerator(42L);
        BiomeType biome = a.biomeAt(100, -25);
        assertEquals(a.terrainHeight(100, -25, biome), b.terrainHeight(100, -25, biome));
    }

    @Test
    void biomeTransitionSamplerMarksBoundariesForDebugging() {
        OverworldGenerator generator = new OverworldGenerator(1337L);
        BoundarySample boundary = findBoundary(generator);

        OverworldGenerator.BiomeTransition transition = generator.biomeTransitionAt(boundary.x(), boundary.z());
        OverworldGenerator.BiomeTransition neighborTransition = generator.biomeTransitionAt(boundary.neighborX(), boundary.neighborZ());
        int height = generator.terrainHeight(boundary.x(), boundary.z(), generator.biomeAt(boundary.x(), boundary.z()));
        int neighborHeight = generator.terrainHeight(boundary.neighborX(), boundary.neighborZ(), generator.biomeAt(boundary.neighborX(), boundary.neighborZ()));

        assertTrue(transition.boundary());
        assertTrue(transition.edgeFactor() > 0.0);
        assertTrue(neighborTransition.boundary());
        assertTrue(Math.abs(height - neighborHeight) < 72, "biome boundary height blend should avoid extreme one-step cliffs");
    }

    @Test
    void terrainCacheMatchesColumnSamplers() {
        OverworldGenerator generator = new OverworldGenerator(42L);
        ChunkPos pos = new ChunkPos(-3, 4);
        Chunk chunk = new Chunk(pos, DimensionSettings.OVERWORLD);

        generator.generate(chunk);

        ChunkTerrainCache cache = chunk.terrainCache().orElseThrow();
        assertEquals(pos, cache.pos());
        assertCachedColumn(generator, cache, chunk, pos.x() * ChunkPos.SIZE, pos.z() * ChunkPos.SIZE);
        assertCachedColumn(generator, cache, chunk, pos.x() * ChunkPos.SIZE + 7, pos.z() * ChunkPos.SIZE + 11);
        assertCachedColumn(generator, cache, chunk, pos.x() * ChunkPos.SIZE + 15, pos.z() * ChunkPos.SIZE + 15);
    }

    @Test
    void structuresCanUseChunkTerrainCache() {
        OverworldGenerator generator = new OverworldGenerator(42L);
        ChunkPos pos = new ChunkPos(1, 1);
        ChunkTerrainCache cache = generator.terrainCacheForChunk(pos);

        assertEquals(generator.structureAtChunk(pos), generator.structureAtChunk(cache));
    }

    @Test
    void generationPlanExposesPassProductsAndMetrics() {
        OverworldGenerator generator = new OverworldGenerator(42L);
        OverworldGenerator.GenerationPlan plan = generator.planChunk(new ChunkPos(0, 0));

        assertEquals(new ChunkPos(0, 0), plan.terrainCache().pos());
        assertTrue(plan.structure().isPresent());
        assertTrue(plan.spawnPoint().isPresent());
        assertEquals(ChunkTerrainCache.COLUMN_COUNT, plan.metrics().biomeSamples());
        assertEquals(ChunkTerrainCache.COLUMN_COUNT, plan.metrics().heightSamples());
        assertEquals(1, plan.metrics().structureAttempts());
        assertEquals(1, plan.metrics().structureSuccesses());
        assertTrue(plan.metrics().lootMarkers() > 0);
        assertTrue(plan.metrics().spawnCandidatesScanned() > 0);
        assertTrue(plan.metrics().spawnCandidatesAccepted() > 0);

        Chunk chunk = new Chunk(new ChunkPos(0, 0), DimensionSettings.OVERWORLD);
        generator.generate(chunk);

        OverworldGenerator.GenerationMetrics applied = generator.lastGenerationMetrics();
        assertEquals(ChunkTerrainCache.COLUMN_COUNT, applied.biomeSamples());
        assertEquals(ChunkTerrainCache.COLUMN_COUNT, applied.heightSamples());
        assertTrue(applied.featurePlacements() > 0);
        assertTrue(applied.rejectedPlacements() >= 0);
    }

    @Test
    void spawnSafetyPassFindsSafeSpawnForSmokeSeeds() {
        for (ReproducibleWorldSeeds.Scenario scenario : ReproducibleWorldSeeds.all()) {
            OverworldGenerator generator = new OverworldGenerator(scenario.seed());
            OverworldGenerator.SpawnPoint spawn = generator.safeSpawnPoint();
            InMemoryWorld world = generatedWorldAround(generator, ChunkPos.fromBlock(spawn.blockX(), spawn.blockZ()));

            assertFalse(spawn.fallback(), scenario.key() + " should not need spawn fallback");
            assertTrue(spawn.candidatesScanned() > 0, scenario.key());
            assertTrue(spawn.candidatesAccepted() > 0, scenario.key());
            assertTrue(distanceToStarterResource(spawn.blockX(), spawn.blockZ()) <= 14.0, scenario.key() + " spawn should stay near starter resources");
            assertTrue(world.blockType(world.blockId(spawn.blockX(), spawn.surfaceY(), spawn.blockZ())).solid(), scenario.key() + " support must be solid");
            assertFalse(world.blockId(spawn.blockX(), spawn.feetY(), spawn.blockZ()) == Blocks.WATER, scenario.key() + " feet must not be in water");
            assertPlayerDoesNotCollide(world, spawn, scenario.key());
            assertNoDangerousAmbientEntityDirectlyAtSpawn(scenario.seed(), spawn, scenario.key());
        }
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

    private static void assertCachedColumn(OverworldGenerator generator, ChunkTerrainCache cache, Chunk chunk, int x, int z) {
        BiomeType biome = generator.biomeAt(x, z);
        assertEquals(biome, cache.biomeAtWorld(x, z));
        int height = generator.terrainHeight(x, z, biome);
        assertEquals(height, cache.heightAtWorld(x, z));
        assertEquals(chunk.blockId(x, height, z), cache.surfaceBlockAtWorld(x, z));
        assertEquals(height < 63, cache.hasFluidAtWorld(x, z));
    }

    private static InMemoryWorld generatedWorldAround(OverworldGenerator generator, ChunkPos center) {
        InMemoryWorld world = new InMemoryWorld(DimensionSettings.OVERWORLD, Blocks.createDefaultRegistry());
        for (int z = center.z() - 1; z <= center.z() + 1; z++) {
            for (int x = center.x() - 1; x <= center.x() + 1; x++) {
                generator.generate(world.getOrCreateChunk(new ChunkPos(x, z)));
            }
        }
        return world;
    }

    private static void assertPlayerDoesNotCollide(InMemoryWorld world, OverworldGenerator.SpawnPoint spawn, String label) {
        PlayerBounds bounds = PlayerBounds.DEFAULT;
        int minX = (int) Math.floor(bounds.minX(spawn.eyeX()));
        int maxX = (int) Math.floor(bounds.maxX(spawn.eyeX()));
        int minY = (int) Math.floor(bounds.minY(spawn.eyeY()));
        int maxY = (int) Math.floor(bounds.maxY(spawn.eyeY()));
        int minZ = (int) Math.floor(bounds.minZ(spawn.eyeZ()));
        int maxZ = (int) Math.floor(bounds.maxZ(spawn.eyeZ()));
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    if (!bounds.intersectsBlock(spawn.eyeX(), spawn.eyeY(), spawn.eyeZ(), x, y, z)) {
                        continue;
                    }
                    short blockId = world.blockId(x, y, z);
                    assertFalse(blocks.requireById(blockId).collidable(), label + " spawn intersects block " + blockId + " at " + x + " " + y + " " + z);
                }
            }
        }
    }

    private static void assertNoDangerousAmbientEntityDirectlyAtSpawn(long seed, OverworldGenerator.SpawnPoint spawn, String label) {
        for (EntitySnapshot snapshot : AmbientEntitySpawner.spawnAroundSpawn(seed, 1)) {
            if (!"voxel:little_boar".equals(snapshot.typeKey())) {
                continue;
            }
            double dx = snapshot.x() - spawn.eyeX();
            double dz = snapshot.z() - spawn.eyeZ();
            assertTrue(dx * dx + dz * dz > 64.0, label + " dangerous ambient entity is too close to spawn");
        }
    }

    private static double distanceToStarterResource(int x, int z) {
        int[][] resources = {
                {4, 6}, {6, 3}, {8, 13}, {3, 8}, {5, 12}, {15, 13}, {5, 5}, {7, 12},
                {2, 10}, {4, 13}, {7, 4}, {2, 6}, {6, 14}, {10, 13}, {14, 14}
        };
        double nearest = Double.POSITIVE_INFINITY;
        for (int[] resource : resources) {
            int dx = x - resource[0];
            int dz = z - resource[1];
            nearest = Math.min(nearest, Math.sqrt(dx * dx + dz * dz));
        }
        return nearest;
    }

    private static BoundarySample findBoundary(OverworldGenerator generator) {
        for (int z = -512; z <= 512; z += 4) {
            for (int x = -512; x <= 512; x += 4) {
                String center = generator.biomeAt(x, z).key();
                if (!generator.biomeAt(x + 4, z).key().equals(center)) {
                    return new BoundarySample(x, z, x + 4, z);
                }
                if (!generator.biomeAt(x, z + 4).key().equals(center)) {
                    return new BoundarySample(x, z, x, z + 4);
                }
            }
        }
        throw new AssertionError("Expected at least one biome boundary in scan area");
    }

    private record BoundarySample(int x, int z, int neighborX, int neighborZ) {
    }
}
