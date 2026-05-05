package dev.voxelgame.client;

import dev.voxelgame.client.net.ClientNetworkStats;
import dev.voxelgame.client.render.RenderResourceTracker;
import dev.voxelgame.client.render.RenderPassStats;
import dev.voxelgame.client.render.WorldRenderer;
import dev.voxelgame.client.render.entity.EntityRenderer;
import dev.voxelgame.client.render.particle.ParticleSystem;
import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.engine.EngineJobType;
import dev.voxelgame.common.net.GamePacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineFrameStatsTest {
    @Test
    void emptyStatsAreSafeForDebugOverlayBeforeFirstFrame() {
        EngineFrameStats stats = EngineFrameStats.empty();

        assertEquals(0, stats.frame().fps());
        assertEquals(0.0, stats.phases().updateTotalMilliseconds(), 0.001);
        assertEquals(0.0, stats.phases().renderTotalMilliseconds(), 0.001);
        assertEquals(0, stats.jobs().chunkMesh().pendingJobs());
        assertEquals(0L, stats.jobs().forType(EngineJobType.NET_ENCODE).completedJobs());
        assertEquals(0, stats.chunks().loadedChunks());
        assertEquals(0, stats.chunks().terrainCacheChunks());
        assertEquals(0L, stats.chunks().terrainCacheBytes());
        assertEquals(0, stats.rendering().drawCalls());
        assertEquals(0, stats.entities().entityCount());
        assertFalse(stats.network().online());
        assertEquals(0.0, stats.budgets().frameTargetMilliseconds(), 0.001);
    }

    @Test
    void framePhasesClampInvalidValuesAndExposeTotals() {
        EngineFrameStats.FramePhases phases = new EngineFrameStats.FramePhases(
                -1.0,
                Double.NaN,
                1.25,
                2.75,
                3.0,
                -4.0,
                5.5,
                0.8
        );

        assertEquals(0.0, phases.inputMilliseconds(), 0.001);
        assertEquals(0.0, phases.networkMilliseconds(), 0.001);
        assertEquals(0.0, phases.gpuUploadMilliseconds(), 0.001);
        assertEquals(4.0, phases.updateTotalMilliseconds(), 0.001);
        assertEquals(8.5, phases.renderTotalMilliseconds(), 0.001);
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
                3,
                2,
                1,
                6,
                240,
                8,
                5,
                8192L,
                new RenderPassStats("terrain.opaque", 3, 1, 1, 3, 120, 1, 0, 3),
                new RenderPassStats("terrain.cutout", 2, 1, 1, 2, 80, 0, 1, 3),
                new RenderPassStats("terrain.translucent", 1, 1, 1, 1, 40, 1, 0, 6),
                12,
                1024L,
                1,
                14,
                32,
                2,
                1,
                1,
                4096L,
                64,
                64,
                16384L
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
                9,
                10,
                new GamePacket.ServerStatsSnapshot(
                        5,
                        2L,
                        11L,
                        7L,
                        3L,
                        1L,
                        0L,
                        0L,
                        20L,
                        2400L,
                        120L,
                        8.0,
                        4,
                        1,
                        9L,
                        8L,
                        1L,
                        2L,
                        2048L,
                        6L,
                        0.75,
                        0.4,
                        1024.0,
                        1.25,
                        0.05
                )
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
                4L,
                1L,
                2.5,
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
                1,
                6,
                particleStats,
                true,
                networkStats,
                resourceStats
        );

        assertEquals(60, stats.frame().fps());
        assertEquals(2.0, stats.frame().clientTickMilliseconds(), 0.001);
        assertEquals(2.0, stats.phases().playerMilliseconds(), 0.001);
        assertEquals(7.5, stats.phases().renderPassMilliseconds(), 0.001);
        assertEquals(2.0, stats.phases().updateTotalMilliseconds(), 0.001);
        assertEquals(7.5, stats.phases().renderTotalMilliseconds(), 0.001);
        assertEquals(world.dirtyChunkCount(), stats.jobs().chunkMesh().pendingJobs());
        assertEquals(9L, stats.jobs().chunkGenerate().completedJobs());
        assertEquals(9L, stats.jobs().chunkLight().completedJobs());
        assertEquals(4, stats.jobs().saveWrite().pendingJobs());
        assertEquals(1, stats.jobs().saveWrite().runningJobs());
        assertEquals(8L, stats.jobs().saveWrite().completedJobs());
        assertEquals(3L, stats.jobs().saveWrite().canceledJobs());
        assertEquals(10L + networkStats.serverStats().sentPackets(), stats.jobs().netEncode().completedJobs());
        assertEquals(8, stats.chunks().renderDistanceChunks());
        assertEquals(9, stats.chunks().loadedChunks());
        assertEquals(world.terrainCacheChunkCount(), stats.chunks().terrainCacheChunks());
        assertEquals(world.terrainCacheBytes(), stats.chunks().terrainCacheBytes());
        assertEquals(3, stats.chunks().visibleChunks());
        assertEquals(2, stats.chunks().builtChunks());
        assertEquals(6, stats.rendering().drawCalls());
        assertEquals(3, stats.rendering().solidDrawCalls());
        assertEquals(2, stats.rendering().cutoutDrawCalls());
        assertEquals(1, stats.rendering().transparentDrawCalls());
        assertEquals(240, stats.rendering().triangles());
        assertEquals(1, stats.rendering().sortedTransparentMeshes());
        assertEquals(12, stats.rendering().renderStateChanges());
        assertEquals(4096L, stats.rendering().gpuUploadBytes());
        assertEquals(64, stats.rendering().atlasWidth());
        assertEquals(16384L, stats.rendering().atlasBytes());
        assertEquals(4, stats.rendering().debugChunkBorders());
        assertEquals(2, stats.rendering().debugMeshBounds());
        assertEquals(1, stats.rendering().debugSectionBounds());
        assertEquals(5, stats.entities().entityCount());
        assertEquals(3, stats.entities().visibleEntityCount());
        assertEquals(33, stats.particles().particleCount());
        assertEquals(4.5, stats.particles().spawnRate(), 0.001);
        assertEquals(0.25, stats.particles().budgetUsage(), 0.001);
        assertEquals(2L, stats.particles().evictedParticles());
        assertEquals(6, stats.particles().debugBounds());
        assertTrue(stats.network().online());
        assertEquals(96.0, stats.network().averagePacketBytes(), 0.001);
        assertEquals(1L, stats.network().invalidPacketsDropped());
        assertEquals(7, stats.network().chunkStreamQueueLength());
        assertEquals(6, stats.network().entitySnapshotPackets());
        assertEquals(10, stats.network().serverStatsPackets());
        assertEquals(5, stats.network().serverStats().chunkSubscriptions());
        assertEquals(11L, stats.network().serverStats().sentEntitySnapshots());
        assertEquals(16, stats.gpuResources().liveChunkBuffers());
        assertEquals(3, stats.gpuResources().liveTextures());
        assertEquals(2, stats.gpuResources().liveShaderPrograms());
        assertEquals(4L, stats.gpuResources().shaderReloadCount());
        assertEquals(1L, stats.gpuResources().failedShaderReloadCount());
        assertEquals(2.5, stats.gpuResources().lastShaderReloadMilliseconds(), 0.001);
        assertEquals(2, stats.gpuResources().liveEntityBuffers());
        assertEquals(1, stats.gpuResources().liveFramebuffers());
        assertEquals("Custom", stats.budgets().profile());
        assertEquals(1000.0 / 60.0, stats.budgets().frameTargetMilliseconds(), 0.001);
        assertEquals(16.6 / (1000.0 / 60.0), stats.budgets().frameUsage(), 0.001);
        assertEquals(10.0, stats.budgets().meshingBudgetMilliseconds(), 0.001);
        assertEquals(8.0, stats.budgets().gpuUploadBudgetMilliseconds(), 0.001);
        assertEquals(4096.0 / 2_000_000.0, stats.budgets().gpuUploadBytesUsage(), 0.001);
        assertEquals(6.0 / 1_100.0, stats.budgets().drawCallUsage(), 0.001);
        assertEquals(240.0 / 800_000.0, stats.budgets().triangleUsage(), 0.001);
        assertEquals(0.25, stats.budgets().particleUsage(), 0.001);
        assertEquals(1.25 / 16.0, stats.budgets().saveWriteUsage(), 0.001);
    }

    @Test
    void captureKeepsExplicitFramePhaseTimings() {
        EngineFrameStats.FramePhases phases = new EngineFrameStats.FramePhases(
                0.2,
                0.3,
                0.4,
                0.5,
                1.2,
                0.6,
                4.0,
                0.8
        );

        EngineFrameStats stats = EngineFrameStats.capture(
                55,
                18.0,
                1.4,
                5.2,
                0.8,
                phases,
                GameSettings.fromOptions(new ConnectionOptions(false, null, 25565, "Player", 1L, 2, 2, false, false)),
                4,
                null,
                null,
                0,
                0,
                0,
                0,
                null,
                0,
                0,
                0,
                0,
                0,
                null,
                false,
                null,
                null
        );

        assertEquals(1.4, stats.phases().updateTotalMilliseconds(), 0.001);
        assertEquals(5.2, stats.phases().renderTotalMilliseconds(), 0.001);
        assertEquals(0.6, stats.phases().gpuUploadMilliseconds(), 0.001);
        assertEquals(0.8, stats.phases().uiMilliseconds(), 0.001);
    }
}
