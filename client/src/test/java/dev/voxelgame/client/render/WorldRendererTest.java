package dev.voxelgame.client.render;

import dev.voxelgame.common.world.ChunkPos;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

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
        assertEquals(0, stats.drawCalls());
        assertEquals(0L, stats.meshBytes());
    }
}
