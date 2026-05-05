package dev.voxelgame.client.world;

import dev.voxelgame.client.render.ChunkMesher;
import dev.voxelgame.client.render.ChunkMesh;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.block.BlockRenderLayer;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkTerrainCache;
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
    void visibleBuildRequestsBeatPreviewAndBackgroundJobs() {
        ChunkBuildQueue queue = new ChunkBuildQueue();
        queue.enqueue(new ChunkPos(6, 0));
        queue.enqueue(new ChunkPos(2, 0));
        queue.enqueue(new ChunkPos(12, 0));

        ChunkBuildQueue.BuildRequest request = queue.poll(new Vector3f(8.0f, 80.0f, 8.0f), 3, 8);

        assertEquals(new ChunkPos(2, 0), request.pos());
    }

    @Test
    void previewGenerationCanBeBudgetedAcrossFrames() {
        ClientWorld world = new ClientWorld(123L);

        world.ensurePreviewAround(new Vector3f(8.0f, 80.0f, 8.0f), 2, 2);

        assertEquals(2, world.loadedChunkCount());
        assertTrue(world.dirtyChunkCount() > 0);
        assertEquals(2, world.buildQueueStats().completedGenerationJobs());
        assertEquals(1, world.buildQueueStats().completedLightingJobs());

        world.ensurePreviewAround(new Vector3f(8.0f, 80.0f, 8.0f), 2, 2);

        assertEquals(4, world.loadedChunkCount());
        assertEquals(4, world.buildQueueStats().completedGenerationJobs());
        assertEquals(2, world.buildQueueStats().completedLightingJobs());
    }

    @Test
    void previewGenerationTimeBudgetStillMakesFrameProgress() {
        ClientWorld world = new ClientWorld(123L);

        world.ensurePreviewAround(new Vector3f(8.0f, 80.0f, 8.0f), 2, 99, 0.000001);

        assertEquals(1, world.loadedChunkCount());
        assertEquals(1, world.buildQueueStats().completedGenerationJobs());
        assertEquals(1, world.buildQueueStats().completedLightingJobs());

        world.ensurePreviewAround(new Vector3f(8.0f, 80.0f, 8.0f), 2, 99, 0.000001);

        assertEquals(2, world.loadedChunkCount());
    }

    @Test
    void previewGenerationOnlyQueuesLoadedNeighborChunksForRemesh() {
        ClientWorld world = new ClientWorld(123L);

        world.ensurePreviewAround(new Vector3f(8.0f, 80.0f, 8.0f), 1, 1);

        assertEquals(1, world.loadedChunkCount());
        assertEquals(1, world.dirtyChunkCount());

        world.ensurePreviewAround(new Vector3f(8.0f, 80.0f, 8.0f), 1, 1);

        assertEquals(2, world.loadedChunkCount());
        assertEquals(2, world.dirtyChunkCount());
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
    void unloadOutsideRespectsPerFrameBudgetAndUnloadsFarthestChunksFirst() {
        ClientWorld world = new ClientWorld(123L);
        world.generatePreview(2);
        world.buildDirtyLayeredMeshes(new ChunkMesher(), true, false, Integer.MAX_VALUE);

        List<ChunkPos> firstBudget = world.unloadOutside(new Vector3f(8.0f, 80.0f, 8.0f), 1, 4);

        assertEquals(4, firstBudget.size());
        assertEquals(21, world.loadedChunkCount());
        assertEquals(4, world.lastUnloadedChunkCount());
        assertTrue(firstBudget.stream().allMatch(pos -> Math.max(Math.abs(pos.x()), Math.abs(pos.z())) == 2));

        List<ChunkPos> remaining = world.unloadOutside(new Vector3f(8.0f, 80.0f, 8.0f), 1, 99);

        assertEquals(12, remaining.size());
        assertEquals(9, world.loadedChunkCount());
        assertEquals(16L, world.totalUnloadedChunkCount());
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
    void longExploreSmokeKeepsLoadedChunksAndBuildQueueBounded() {
        ClientWorld world = new ClientWorld(424242L);
        ChunkMesher mesher = new ChunkMesher();
        int maxLoadedChunks = 0;
        int maxDirtyChunks = 0;
        long maxRetainedMeshBufferBytes = 0L;

        for (int step = 0; step < 18; step++) {
            Vector3f camera = new Vector3f(
                    8.0f + step * ChunkPos.SIZE,
                    88.0f,
                    8.0f + ((step % 5) - 2) * ChunkPos.SIZE
            );

            world.ensurePreviewAround(camera, 2, 8);
            for (int frame = 0; frame < 4; frame++) {
                world.buildDirtyLayeredMeshes(mesher, true, true, 4, camera, 2, 2, 8.0);
            }
            world.unloadOutside(camera, 2);

            ChunkBuildQueue.Snapshot queue = world.buildQueueStats();
            maxLoadedChunks = Math.max(maxLoadedChunks, world.loadedChunkCount());
            maxDirtyChunks = Math.max(maxDirtyChunks, world.dirtyChunkCount());
            maxRetainedMeshBufferBytes = Math.max(maxRetainedMeshBufferBytes, queue.retainedMeshBufferBytes());
        }

        assertTrue(world.totalUnloadedChunkCount() > 0);
        assertTrue(maxLoadedChunks <= 25, "loaded chunks should remain inside the retain square");
        assertTrue(maxDirtyChunks <= 25, "dirty builds should not grow without bound while exploring");
        assertTrue(maxRetainedMeshBufferBytes < 8L * 1024L * 1024L, "retained meshing buffers should stay below the smoke budget");
    }

    @Test
    void terrainCacheExposesSurfaceFluidCaveMemoryAndInvalidatesOnBlockUpdate() {
        ClientWorld world = new ClientWorld(123L);
        world.generatePreview(0);

        int height = world.terrainHeightAt(8, 8);
        short surfaceBlock = world.terrainSurfaceBlockAt(8, 8);
        boolean hasFluid = world.terrainHasFluidAt(8, 8);
        ChunkTerrainCache.FluidSurface fluidSurface = world.terrainFluidSurfaceAt(8, 8);
        boolean hasCave = world.terrainHasCaveAt(8, 8);

        assertEquals(1, world.terrainCacheChunkCount());
        assertTrue(world.terrainCacheBytes() > 0L);
        assertEquals(world.blockIdAt(8, height, 8), surfaceBlock);
        assertEquals(height < 63, hasFluid);
        assertEquals(hasFluid, fluidSurface.fluid());
        assertEquals(fluidSurface.depthHint(), world.terrainFluidDepthHintAt(8, 8));
        assertEquals(fluidSurface.shoreMask(), world.terrainShoreMaskAt(8, 8));
        assertEquals(fluidSurface.flags(), world.terrainFluidSurfaceFlagsAt(8, 8));

        world.applyBlock(new GamePacket.BlockUpdate(8, height, 8, Blocks.STONE));

        assertEquals(0, world.terrainCacheChunkCount());
        assertEquals(0L, world.terrainCacheBytes());
        assertEquals(height, world.terrainHeightAt(8, 8));
        assertEquals(hasCave, world.terrainHasCaveAt(8, 8));
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
        long generatedBefore = queued.completedGenerationJobs();
        long lightingBefore = queued.completedLightingJobs();

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
        assertEquals(generatedBefore, built.completedGenerationJobs());
        assertEquals(lightingBefore, built.completedLightingJobs());
        assertTrue(built.completedGenerationJobs() > 0L);
        assertTrue(built.completedLightingJobs() > 0L);
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
    void sectionBoundsAroundUsesRadiusAndOnlyNonEmptySections() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 32, 8, Blocks.STONE));
        world.applyBlock(new GamePacket.BlockUpdate(8, 250, 8, Blocks.STONE));
        world.applyBlock(new GamePacket.BlockUpdate(80, 32, 80, Blocks.STONE));

        List<ChunkMesh.Bounds> nearBounds = world.sectionBoundsAround(new Vector3f(8.0f, 80.0f, 8.0f), 0);

        assertEquals(2, nearBounds.size());
        assertTrue(nearBounds.stream().anyMatch(bounds -> bounds.minY() == 32.0f && bounds.maxY() == 48.0f));
        assertTrue(nearBounds.stream().anyMatch(bounds -> bounds.minY() == 240.0f && bounds.maxY() == 256.0f));
        assertEquals(3, world.sectionBoundsAround(new Vector3f(8.0f, 80.0f, 8.0f), 8).size());
    }

    @Test
    void sectionLayerBoundsSeparateNegativeHighAndDisconnectedSections() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(-8, -20, 8, Blocks.STONE));
        world.applyBlock(new GamePacket.BlockUpdate(-8, 250, 8, Blocks.WILD_GRASS));
        world.applyBlock(new GamePacket.BlockUpdate(-8, 260, 8, Blocks.WATER));

        List<ClientWorld.SectionLayerBounds> bounds = world.sectionLayerBoundsAround(new Vector3f(-8.0f, 80.0f, 8.0f), 0);

        assertTrue(bounds.stream().anyMatch(bound ->
                bound.sectionY() == -2 && bound.layer() == BlockRenderLayer.SOLID
                        && bound.bounds().minY() == -32.0f && bound.bounds().maxY() == -16.0f
        ));
        assertTrue(bounds.stream().anyMatch(bound ->
                bound.sectionY() == 15 && bound.layer() == BlockRenderLayer.CUTOUT
                        && bound.bounds().minY() == 240.0f && bound.bounds().maxY() == 256.0f
        ));
        assertTrue(bounds.stream().anyMatch(bound ->
                bound.sectionY() == 16 && bound.layer() == BlockRenderLayer.TRANSLUCENT
                        && bound.bounds().minY() == 256.0f && bound.bounds().maxY() == 272.0f
        ));
    }

    @Test
    void sectionDirtyStatsTrackAspectsAndClearAfterMeshBuild() {
        ClientWorld world = loadedCleanWorld();

        world.applyBlock(new GamePacket.BlockUpdate(8, 250, 8, Blocks.WATER));

        ClientWorld.SectionStats dirty = world.sectionStats();
        assertEquals(1, dirty.dirtyGeometrySections());
        assertTrue(dirty.dirtyLightSections() >= 1);
        assertEquals(1, dirty.dirtyFluidSections());

        world.buildDirtyLayeredMeshes(new ChunkMesher(), true, true, 5);

        ClientWorld.SectionStats clean = world.sectionStats();
        assertEquals(0, clean.dirtyGeometrySections());
        assertEquals(0, clean.dirtyFluidSections());
    }

    @Test
    void boundaryBlockUpdatesMarkNeighborSectionsForFuturePartialRebuilds() {
        ClientWorld world = loadedCleanWorld();

        world.applyBlock(new GamePacket.BlockUpdate(15, 31, 8, Blocks.WILD_GRASS));

        ClientWorld.SectionStats dirty = world.sectionStats();

        assertEquals(3, dirty.dirtyGeometrySections());
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
    void layeredBuildsSkipAbsentRenderLayers() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 32, 8, Blocks.STONE));

        ClientWorld.LayeredMeshBuild build = world.buildDirtyLayeredMeshes(new ChunkMesher(), true, true, 1)
                .getFirst();

        assertTrue(build.opaqueMesh().indexCount() > 0);
        assertEquals(0, build.cutoutMesh().indexCount());
        assertEquals(0, build.transparentMesh().indexCount());
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
