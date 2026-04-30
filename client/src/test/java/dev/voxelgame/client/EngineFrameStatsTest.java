package dev.voxelgame.client;

import dev.voxelgame.client.net.ClientNetworkStats;
import dev.voxelgame.client.render.RenderResourceTracker;
import dev.voxelgame.client.render.WorldRenderer;
import dev.voxelgame.client.render.entity.EntityRenderer;
import dev.voxelgame.client.render.particle.ParticleSystem;
import dev.voxelgame.client.world.ClientWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineFrameStatsTest {
    @Test
    void emptyStatsAreSafeForDebugOverlayBeforeFirstFrame() {
        EngineFrameStats stats = EngineFrameStats.empty();

        assertEquals(0, stats.frame().fps());
        assertEquals(0, stats.chunks().loadedChunks());
        assertEquals(0, stats.rendering().drawCalls());
        assertEquals(0, stats.entities().entityCount());
        assertFalse(stats.network().online());
    }

    @Test
    void captureCentralizesWorldRenderEntityParticleNetworkAndGpuStats() {
        GameSettings settings = GameSettings.fromOptions(new ConnectionOptions(
                false,
                null,
                25565,
                "Player",
                1337L,
                3,
                8,
                false,
                false
        ));
        ClientWorld world = new ClientWorld(1337L);
        world.generatePreview(1);
        WorldRenderer.RenderStats renderStats = new WorldRenderer.RenderStats(
                6,
                3,
                2,
                4,
                1,
                1,
                6,
                240,
                8,
                5,
                8192L
        );
        EntityRenderer.RenderStats entityStats = new EntityRenderer.RenderStats(3, 2, 12, 12, 4);
        ParticleSystem.RenderStats particleStats = new ParticleSystem.RenderStats(33, 4.5, 0.25, 2L, 1, 18);
        ClientNetworkStats.Snapshot networkStats = new ClientNetworkStats.Snapshot(
                10,
                20,
                1.5,
                2.5,
                96.0,
                1L,
                7,
                4,
                5,
                6,
                7,
                8,
                9
        );
        RenderResourceTracker.Snapshot resourceStats = new RenderResourceTracker.Snapshot(
                8,
                8,
                16,
                8192L,
                12288L,
                20L,
                12L,
                3,
                4096L,
                8192L,
                4L,
                1L,
                2,
                3L,
                1L,
                1,
                1,
                4096L,
                1,
                2,
                2048L,
                1
        );

        EngineFrameStats stats = EngineFrameStats.capture(
                60,
                16.6,
                2.0,
                7.5,
                1.1,
                settings,
                10,
                world,
                renderStats,
                2,
                1,
                3,
                5,
                entityStats,
                3,
                4,
                2,
                particleStats,
                true,
                networkStats,
                resourceStats
        );

        assertEquals(60, stats.frame().fps());
        assertEquals(2.0, stats.frame().clientTickMilliseconds(), 0.001);
        assertEquals(8, stats.chunks().renderDistanceChunks());
        assertEquals(9, stats.chunks().loadedChunks());
        assertEquals(3, stats.chunks().visibleChunks());
        assertEquals(2, stats.chunks().builtChunks());
        assertEquals(6, stats.rendering().drawCalls());
        assertEquals(240, stats.rendering().triangles());
        assertEquals(4, stats.rendering().debugChunkBorders());
        assertEquals(2, stats.rendering().debugMeshBounds());
        assertEquals(5, stats.entities().entityCount());
        assertEquals(3, stats.entities().visibleEntityCount());
        assertEquals(33, stats.particles().particleCount());
        assertEquals(4.5, stats.particles().spawnRate(), 0.001);
        assertEquals(0.25, stats.particles().budgetUsage(), 0.001);
        assertEquals(2L, stats.particles().evictedParticles());
        assertTrue(stats.network().online());
        assertEquals(96.0, stats.network().averagePacketBytes(), 0.001);
        assertEquals(1L, stats.network().invalidPacketsDropped());
        assertEquals(7, stats.network().chunkStreamQueueLength());
        assertEquals(6, stats.network().entitySnapshotPackets());
        assertEquals(16, stats.gpuResources().liveChunkBuffers());
        assertEquals(3, stats.gpuResources().liveTextures());
        assertEquals(2, stats.gpuResources().liveShaderPrograms());
        assertEquals(2, stats.gpuResources().liveEntityBuffers());
        assertEquals(1, stats.gpuResources().liveFramebuffers());
    }
}
