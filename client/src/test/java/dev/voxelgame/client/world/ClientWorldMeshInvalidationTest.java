package dev.voxelgame.client.world;

import dev.voxelgame.client.render.ChunkMesher;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.world.ChunkPos;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientWorldMeshInvalidationTest {
    @Test
    void blockUpdatesMarkChunkAndFourNeighborsDirty() {
        ClientWorld world = loadedCleanWorld();

        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.STONE));

        assertEquals(5, world.dirtyChunkCount());

        Set<ChunkPos> rebuilt = world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, 5)
                .stream()
                .map(ClientWorld.LayeredMeshBuild::pos)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                new ChunkPos(0, 0),
                new ChunkPos(1, 0),
                new ChunkPos(-1, 0),
                new ChunkPos(0, 1),
                new ChunkPos(0, -1)
        ), rebuilt);
        assertEquals(0, world.dirtyChunkCount());
    }

    @Test
    void dirtyMeshBuildsRespectPerFrameBudget() {
        ClientWorld world = loadedCleanWorld();

        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.STONE));

        assertEquals(2, world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, 2).size());
        assertEquals(3, world.dirtyChunkCount());
        assertEquals(3, world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, 99).size());
        assertEquals(0, world.dirtyChunkCount());
    }

    @Test
    void priorityPositionBuildsNearestDirtyChunkFirst() {
        ClientWorld world = new ClientWorld(123L);
        world.generatePreview(3);
        world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, Integer.MAX_VALUE);

        world.applyBlock(new GamePacket.BlockUpdate(40, 80, 40, Blocks.STONE));
        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.STONE));

        List<ClientWorld.LayeredMeshBuild> builds = world.buildDirtyLayeredMeshes(
                new ChunkMesher(),
                true,
                false,
                1,
                new Vector3f(8.0f, 80.0f, 8.0f)
        );

        assertEquals(List.of(new ChunkPos(0, 0)), builds.stream().map(ClientWorld.LayeredMeshBuild::pos).toList());
        assertTrue(world.dirtyChunkCount() > 0);
    }

    @Test
    void urgentBuildRequestsBeatDistancePriority() {
        ChunkBuildQueue queue = new ChunkBuildQueue();
        queue.enqueue(new ChunkPos(0, 0));
        queue.enqueueUrgent(new ChunkPos(4, 4));

        ChunkBuildQueue.BuildRequest request = queue.poll(new Vector3f(8.0f, 80.0f, 8.0f), 8, 8);

        assertEquals(new ChunkPos(4, 4), request.pos());
    }

    @Test
    void previewGenerationCanBeBudgetedAcrossFrames() {
        ClientWorld world = new ClientWorld(123L);

        world.ensurePreviewAround(new Vector3f(8.0f, 80.0f, 8.0f), 2, 2);

        assertEquals(2, world.loadedChunkCount());
        assertTrue(world.dirtyChunkCount() > 0);

        world.ensurePreviewAround(new Vector3f(8.0f, 80.0f, 8.0f), 2, 2);

        assertEquals(4, world.loadedChunkCount());
    }

    @Test
    void unloadOutsideRetainRadiusDropsDistantChunksAndRuntimeEntities() {
        ClientWorld world = new ClientWorld(123L);
        world.generatePreview(2);
        world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, Integer.MAX_VALUE);

        List<ChunkPos> unloaded = world.unloadOutside(new Vector3f(8.0f, 80.0f, 8.0f), 1);

        assertEquals(16, unloaded.size());
        assertEquals(9, world.loadedChunkCount());
        assertEquals(16, world.lastUnloadedChunkCount());
        assertEquals(16L, world.totalUnloadedChunkCount());
        assertEquals(0, world.dirtyChunkCount());
        assertTrue(world.visibleEntities(0.0).stream().allMatch(ClientWorldMeshInvalidationTest::insideSpawnRetainRadius));
    }

    @Test
    void unloadOutsideKeepsModifiedChunksPinnedUntilSaveExists() {
        ClientWorld world = new ClientWorld(123L);
        world.generatePreview(2);
        world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, Integer.MAX_VALUE);

        world.applyBlock(new GamePacket.BlockUpdate(40, 80, 8, Blocks.STONE));
        List<ChunkPos> unloaded = world.unloadOutside(new Vector3f(8.0f, 80.0f, 8.0f), 1);

        assertFalse(unloaded.contains(new ChunkPos(2, 0)));
        assertEquals(10, world.loadedChunkCount());
        assertEquals(Blocks.STONE, world.blockIdAt(40, 80, 8));
        assertTrue(world.dirtyChunkCount() > 0);
    }

    @Test
    void unloadedChunksCanReloadWithoutDuplicateDirtyBuilds() {
        ClientWorld world = new ClientWorld(123L);
        world.generatePreview(1);
        world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, Integer.MAX_VALUE);

        List<ChunkPos> unloaded = world.unloadOutside(new Vector3f(8.0f, 80.0f, 8.0f), 0);
        assertEquals(8, unloaded.size());
        assertEquals(1, world.loadedChunkCount());
        assertEquals(0, world.dirtyChunkCount());

        world.ensurePreviewAround(new Vector3f(8.0f, 80.0f, 8.0f), 1, 99);
        assertEquals(9, world.loadedChunkCount());

        List<ClientWorld.LayeredMeshBuild> builds = world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, 99);
        Set<ChunkPos> uniqueBuilds = builds.stream()
                .map(ClientWorld.LayeredMeshBuild::pos)
                .collect(Collectors.toSet());

        assertTrue(builds.size() > 0);
        assertEquals(uniqueBuilds.size(), builds.size());
        assertEquals(0, world.dirtyChunkCount());

        world.ensurePreviewAround(new Vector3f(8.0f, 80.0f, 8.0f), 1, 99);
        assertEquals(9, world.loadedChunkCount());
        assertEquals(0, world.dirtyChunkCount());
    }

    @Test
    void buildQueueTracksReplacementCancellationWaitAndTimings() {
        ClientWorld world = loadedCleanWorld();

        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.STONE));
        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.STONE));

        ChunkBuildQueue.Snapshot queued = world.buildQueueStats();
        assertEquals(5, queued.queuedBuilds());
        assertTrue(queued.replacedBuilds() >= 5);
        long completedBefore = queued.completedBuilds();

        List<ClientWorld.LayeredMeshBuild> builds = world.buildDirtyLayeredMeshes(
                new ChunkMesher(),
                true,
                true,
                5,
                new Vector3f(8.0f, 80.0f, 8.0f),
                1,
                3,
                1000.0
        );
        world.recordChunkGpuUpload(1.25);

        ChunkBuildQueue.Snapshot built = world.buildQueueStats();
        assertEquals(5, builds.size());
        assertEquals(completedBefore + 5L, built.completedBuilds());
        assertTrue(built.averageWaitMilliseconds() >= 0.0);
        assertTrue(built.lastMeshingMilliseconds() >= 0.0);
        assertEquals(1.25, built.lastGpuUploadMilliseconds(), 0.001);
    }

    @Test
    void sectionStatsAndVerticalBoundsUseNonEmptySections() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 250, 8, Blocks.STONE));

        ClientWorld.SectionStats stats = world.sectionStats();
        ClientWorld.ChunkVerticalBounds bounds = world.verticalBounds(new ChunkPos(0, 0)).orElseThrow();

        assertEquals(1, stats.nonEmptySections());
        assertTrue(stats.emptySections() > 0);
        assertEquals(1, stats.chunksWithSectionBounds());
        assertEquals(240, bounds.minY());
        assertEquals(256, bounds.maxYExclusive());
    }

    @Test
    void exposesLightLevelsForDebugOverlay() {
        ClientWorld world = loadedCleanWorld();

        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.LANTERN));

        assertEquals(13, world.blockLightAt(8, 80, 8));
        assertEquals(Math.max(world.skyLightAt(8, 80, 8), 13), world.combinedLightAt(8, 80, 8));
    }

    @Test
    void transparentWaterToggleControlsTransparentMeshBuilds() {
        ClientWorld world = loadedCleanWorld();
        world.applyBlock(new GamePacket.BlockUpdate(8, 250, 8, Blocks.WATER));

        boolean hasTransparentMeshWhenDisabled = world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, 1)
                .getFirst()
                .transparentMesh()
                .indexCount() > 0;

        world.applyBlock(new GamePacket.BlockUpdate(8, 250, 8, Blocks.WATER));
        boolean hasTransparentMesh = world.buildDirtyLayeredMeshes(new ChunkMesher(), true, true, 1)
                .getFirst()
                .transparentMesh()
                .indexCount() > 0;

        assertFalse(hasTransparentMeshWhenDisabled);
        assertTrue(hasTransparentMesh);
    }

    @Test
    void layeredBuildsKeepCutoutBlocksSeparateFromSolidTerrain() {
        ClientWorld world = loadedCleanWorld();
        world.applyBlock(new GamePacket.BlockUpdate(8, 250, 8, Blocks.WILD_GRASS));

        ClientWorld.LayeredMeshBuild build = world.buildDirtyLayeredMeshes(new ChunkMesher(), true, true, 1)
                .getFirst();

        assertTrue(build.opaqueMesh().indexCount() > 0);
        assertTrue(build.cutoutMesh().indexCount() > 0);
        assertEquals(0, build.transparentMesh().indexCount());
    }

    private static ClientWorld loadedCleanWorld() {
        ClientWorld world = new ClientWorld(123L);
        world.generatePreview(1);
        world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, Integer.MAX_VALUE);
        assertEquals(0, world.dirtyChunkCount());
        return world;
    }

    private static boolean insideSpawnRetainRadius(EntitySnapshot snapshot) {
        ChunkPos pos = ChunkPos.fromBlock((int) Math.floor(snapshot.x()), (int) Math.floor(snapshot.z()));
        return Math.abs(pos.x()) <= 1 && Math.abs(pos.z()) <= 1;
    }
}
