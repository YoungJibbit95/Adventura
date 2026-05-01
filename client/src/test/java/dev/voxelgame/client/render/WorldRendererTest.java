package dev.voxelgame.client.render;

import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.world.ChunkPos;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldRendererTest {
    @Test
    void checksHorizontalRenderDistanceAgainstChunkCenter() {
        Vector3f camera = new Vector3f(8.0f, 80.0f, 8.0f);

        assertTrue(WorldRenderer.withinRenderDistance(new ChunkPos(0, 0), camera, 2));
        assertTrue(WorldRenderer.withinRenderDistance(new ChunkPos(2, 0), camera, 2));
        assertFalse(WorldRenderer.withinRenderDistance(new ChunkPos(5, 0), camera, 2));
    }

    @Test
    void renderSettingsDefaultFogTracksDistance() {
        RenderSettings settings = RenderSettings.defaults(8);

        assertTrue(settings.fogEnabled());
        assertTrue(settings.bloomEnabled());
        assertEquals(8, settings.renderDistanceChunks());
        assertTrue(settings.fogEnd() > settings.fogStart());
        assertEquals(RenderDebugView.NONE, settings.debugView());
    }

    @Test
    void ordersTransparentChunksBackToFront() {
        Vector3f camera = new Vector3f(8.0f, 80.0f, 8.0f);

        List<ChunkPos> ordered = WorldRenderer.transparentRenderOrder(List.of(
                new ChunkPos(0, 0),
                new ChunkPos(2, 0),
                new ChunkPos(-3, 0)
        ), camera);

        assertEquals(List.of(
                new ChunkPos(-3, 0),
                new ChunkPos(2, 0),
                new ChunkPos(0, 0)
        ), ordered);
    }

    @Test
    void ordersTransparentBoundsBackToFrontWithStableTieBreaker() {
        Vector3f camera = new Vector3f(8.0f, 8.0f, 8.0f);
        Map<ChunkPos, ChunkMesh.Bounds> bounds = Map.of(
                new ChunkPos(0, 0), new ChunkMesh.Bounds(0.0f, 4.0f, 0.0f, 16.0f, 12.0f, 16.0f),
                new ChunkPos(1, 0), new ChunkMesh.Bounds(32.0f, 4.0f, 0.0f, 48.0f, 12.0f, 16.0f),
                new ChunkPos(-1, 0), new ChunkMesh.Bounds(-32.0f, 4.0f, 0.0f, -16.0f, 12.0f, 16.0f)
        );

        List<ChunkPos> ordered = WorldRenderer.transparentRenderOrderByBounds(bounds, camera);

        assertEquals(List.of(
                new ChunkPos(-1, 0),
                new ChunkPos(1, 0),
                new ChunkPos(0, 0)
        ), ordered);
    }

    @Test
    void chunkMeshReportsCountsAndEstimatedBytes() {
        ChunkMesh mesh = new ChunkMesh(
                new float[ChunkMesher.FLOATS_PER_VERTEX * 4],
                new int[]{0, 1, 2, 0, 2, 3}
        );

        assertEquals(4, mesh.vertexCount());
        assertEquals(6, mesh.indexCount());
        assertEquals(2, mesh.triangleCount());
        assertEquals((long) mesh.vertices().length * Float.BYTES + (long) mesh.indices().length * Integer.BYTES, mesh.estimatedBytes());
    }

    @Test
    void renderStatsKeepsSmallConstructorForEmptyStats() {
        WorldRenderer.RenderStats stats = new WorldRenderer.RenderStats(0, 0);

        assertEquals(0, stats.renderedChunks());
        assertEquals(0, stats.renderedLayers());
        assertEquals(0, stats.drawCalls());
        assertEquals(0L, stats.meshBytes());
    }

    @Test
    void renderStatsSeparatesCulledMeshesFromChunkPositions() {
        WorldRenderer.RenderStats stats = new WorldRenderer.RenderStats(
                3,
                4,
                2,
                2,
                1,
                0,
                3,
                30,
                5,
                4,
                1024L
        );

        assertEquals(3, stats.renderedLayers());
        assertEquals(4, stats.culledMeshes());
        assertEquals(2, stats.culledChunkPositions());
        assertEquals(2, stats.culledChunks());
        assertEquals(1, stats.renderedCutoutChunks());
        assertEquals(0, stats.sortedTransparentMeshes());
        assertEquals(5, stats.loadedGpuMeshes());
        assertEquals(4, stats.loadedChunkPositions());
    }

    @Test
    void meshReleaseStatsHasEmptySnapshot() {
        WorldRenderer.MeshReleaseStats stats = WorldRenderer.MeshReleaseStats.empty();

        assertEquals(0, stats.releasedLayers());
        assertEquals(0, stats.releasedChunkPositions());
        assertEquals(0L, stats.releasedBytes());
    }

    @Test
    void chunkAabbUsesNonEmptySectionBoundsWhenAvailable() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 250, 8, Blocks.STONE));

        WorldRenderer.ChunkAabb bounds = WorldRenderer.chunkAabb(world, new ChunkPos(0, 0));

        assertEquals(240.0f, bounds.minY());
        assertEquals(256.0f, bounds.maxY());
    }
}
