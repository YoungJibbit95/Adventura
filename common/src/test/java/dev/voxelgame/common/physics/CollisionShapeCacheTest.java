package dev.voxelgame.common.physics;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.DimensionSettings;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollisionShapeCacheTest {
    @Test
    @Tag("physicsRegression")
    void cachesSectionShapesAndInvalidatesChangedBlock() {
        TestSource source = new TestSource();
        source.setBlock(8, 64, 8, Blocks.STONE);
        CollisionShapeCache cache = new CollisionShapeCache(source);

        assertTrue(cache.collidesPlayer(8.5, 65.0, 8.5, PlayerBounds.DEFAULT).collides());
        assertEquals(2, cache.stats().sections());
        assertEquals(1, cache.stats().shapes());

        source.setBlock(8, 64, 8, Blocks.AIR);
        cache.invalidateBlock(8, 64, 8);

        assertFalse(cache.collidesPlayer(8.5, 65.0, 8.5, PlayerBounds.DEFAULT).collides());
        assertEquals(2, cache.stats().sections());
        assertEquals(0, cache.stats().shapes());
    }

    @Test
    void missingChunkBlocksMovementAndMarksLoadingReason() {
        TestSource source = new TestSource();
        CollisionShapeCache cache = new CollisionShapeCache(source);

        CollisionShapeCache.CollisionCheck collision = cache.collidesPlayer(32.5, 65.0, 32.5, PlayerBounds.DEFAULT);

        assertTrue(collision.collides());
        assertTrue(collision.blockedByMissingChunk());
    }

    @Test
    @Tag("physicsRegression")
    void projectileImpactUsesCachedProjectileShapeSet() {
        TestSource source = new TestSource();
        source.setBlock(8, 64, 8, Blocks.CAMPFIRE);
        CollisionShapeCache cache = new CollisionShapeCache(source);

        PartialShapeImpactResolver.ImpactResult hit = cache.projectileImpact(
                7.0,
                64.25,
                8.5,
                9.0,
                64.25,
                8.5,
                ProjectileBounds.ARROW
        ).orElseThrow();

        assertEquals(8, hit.blockX());
        assertEquals(ProjectileHit.BlockFace.WEST, hit.face());
        assertEquals(8.25, hit.impactX(), 0.0001);
    }

    @Test
    void cacheEvictsLeastRecentlyUsedSectionsWithinBudget() {
        TestSource source = new TestSource();
        source.setBlock(8, 64, 8, Blocks.STONE);
        source.setBlock(40, 64, 8, Blocks.STONE);
        source.setBlock(72, 64, 8, Blocks.STONE);
        CollisionShapeCache cache = new CollisionShapeCache(source, 2);

        assertTrue(cache.collidesProjectile(8.5, 64.5, 8.5, ProjectileBounds.ARROW).collides());
        assertTrue(cache.collidesProjectile(40.5, 64.5, 8.5, ProjectileBounds.ARROW).collides());
        assertTrue(cache.collidesProjectile(72.5, 64.5, 8.5, ProjectileBounds.ARROW).collides());

        CollisionShapeCache.CacheStats stats = cache.stats();
        assertTrue(stats.sections() <= 2);
        assertTrue(stats.shapes() <= 2);
        assertTrue(stats.evictedSections() > 0);
        assertEquals(2, stats.maxSectionsPerShapeSet());
    }

    private static final class TestSource implements CollisionShapeCache.Source {
        private final Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        private final Map<BlockPos, Short> blockIds = new HashMap<>();
        private final Set<ChunkPos> loadedChunks = new HashSet<>();

        void setBlock(int x, int y, int z, short blockId) {
            blockIds.put(new BlockPos(x, y, z), blockId);
            loadedChunks.add(ChunkPos.fromBlock(x, z));
        }

        @Override
        public DimensionSettings dimension() {
            return DimensionSettings.OVERWORLD;
        }

        @Override
        public boolean ensureChunkAvailable(ChunkPos pos) {
            return loadedChunks.contains(pos);
        }

        @Override
        public short blockIdAt(int x, int y, int z) {
            return blockIds.getOrDefault(new BlockPos(x, y, z), Blocks.AIR);
        }

        @Override
        public BlockType blockType(short blockId) {
            return blocks.requireById(blockId);
        }
    }

    private record BlockPos(int x, int y, int z) {
    }
}
