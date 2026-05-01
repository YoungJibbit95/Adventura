package dev.voxelgame.client;

import dev.voxelgame.client.net.ClientNetworkStats;
import dev.voxelgame.client.render.RenderResourceTracker;
import dev.voxelgame.client.render.WorldRenderer;
import dev.voxelgame.client.render.entity.EntityRenderer;
import dev.voxelgame.client.render.particle.ParticleSystem;
import dev.voxelgame.client.world.ChunkBuildQueue;
import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.net.GamePacket;

public record EngineFrameStats(
        Frame frame,
        Chunks chunks,
        Rendering rendering,
        Entities entities,
        Particles particles,
        Network network,
        GpuResources gpuResources
) {
    public EngineFrameStats {
        frame = frame == null ? Frame.empty() : frame;
        chunks = chunks == null ? Chunks.empty() : chunks;
        rendering = rendering == null ? Rendering.empty() : rendering;
        entities = entities == null ? Entities.empty() : entities;
        particles = particles == null ? Particles.empty() : particles;
        network = network == null ? Network.offline() : network;
        gpuResources = gpuResources == null ? GpuResources.empty() : gpuResources;
    }

    public static EngineFrameStats empty() {
        return new EngineFrameStats(
                Frame.empty(),
                Chunks.empty(),
                Rendering.empty(),
                Entities.empty(),
                Particles.empty(),
                Network.offline(),
                GpuResources.empty()
        );
    }

    public static EngineFrameStats capture(
            int fps,
            double frameMilliseconds,
            double updateMilliseconds,
            double renderMilliseconds,
            double uiMilliseconds,
            GameSettings settings,
            int retentionRadiusChunks,
            ClientWorld world,
            WorldRenderer.RenderStats renderStats,
            int builtChunks,
            int unloadedChunks,
            int releasedGpuMeshLayers,
            int visibleEntitySnapshots,
            EntityRenderer.RenderStats entityStats,
            int renderedEntityHitboxes,
            int renderedChunkBorderDebugChunks,
            int renderedMeshBoundsDebugBoxes,
            int renderedSectionBoundsDebugBoxes,
            int renderedParticleDebugBoxes,
            ParticleSystem.RenderStats particleStats,
            boolean online,
            ClientNetworkStats.Snapshot networkStats,
            RenderResourceTracker.Snapshot resourceStats
    ) {
        WorldRenderer.RenderStats safeRenderStats = renderStats == null ? new WorldRenderer.RenderStats(0, 0) : renderStats;
        EntityRenderer.RenderStats safeEntityStats = entityStats == null ? EntityRenderer.RenderStats.empty() : entityStats;
        ParticleSystem.RenderStats safeParticleStats = particleStats == null ? ParticleSystem.RenderStats.empty() : particleStats;
        ClientNetworkStats.Snapshot safeNetworkStats = networkStats == null ? ClientNetworkStats.Snapshot.offline() : networkStats;
        RenderResourceTracker.Snapshot safeResourceStats = resourceStats == null
                ? RenderResourceTracker.Snapshot.empty()
                : resourceStats;
        int dirtyChunks = world == null ? 0 : world.dirtyChunkCount();
        int loadedChunks = world == null ? 0 : world.loadedChunkCount();
        long totalUnloadedChunks = world == null ? 0L : world.totalUnloadedChunkCount();
        ChunkBuildQueue.Snapshot buildQueueStats = world == null ? ChunkBuildQueue.Snapshot.empty() : world.buildQueueStats();
        ClientWorld.SectionStats sectionStats = world == null ? new ClientWorld.SectionStats(0, 0, 0, 0) : world.sectionStats();
        int visibleChunks = Math.max(0, safeRenderStats.loadedChunkPositions() - safeRenderStats.culledChunkPositions());
        int renderDistanceChunks = settings == null ? 0 : settings.renderDistanceChunks();
        int previewRadiusChunks = settings == null ? 0 : settings.previewRadiusChunks();
        int meshBudgetChunks = settings == null ? 0 : settings.meshBuildBudgetChunks();
        double meshBudgetMilliseconds = settings == null ? 0.0 : settings.meshBuildBudgetMilliseconds();

        return new EngineFrameStats(
                new Frame(
                        Math.max(0, fps),
                        nonNegative(frameMilliseconds),
                        nonNegative(updateMilliseconds),
                        nonNegative(renderMilliseconds),
                        nonNegative(uiMilliseconds),
                        nonNegative(updateMilliseconds),
                        0.0
                ),
                new Chunks(
                        renderDistanceChunks,
                        previewRadiusChunks,
                        Math.max(0, retentionRadiusChunks),
                        meshBudgetChunks,
                        nonNegative(meshBudgetMilliseconds),
                        loadedChunks,
                        visibleChunks,
                        dirtyChunks,
                        buildQueueStats.queuedBuilds(),
                        Math.max(0, builtChunks),
                        Math.max(0, unloadedChunks),
                        Math.max(0L, totalUnloadedChunks),
                        Math.max(0, releasedGpuMeshLayers),
                        buildQueueStats.replacedBuilds(),
                        buildQueueStats.canceledBuilds(),
                        buildQueueStats.completedBuilds(),
                        nonNegative(buildQueueStats.chunksBuiltPerSecond()),
                        nonNegative(buildQueueStats.averageWaitMilliseconds()),
                        nonNegative(buildQueueStats.lastGenerationMilliseconds()),
                        nonNegative(buildQueueStats.averageGenerationMilliseconds()),
                        nonNegative(buildQueueStats.lastMeshingMilliseconds()),
                        nonNegative(buildQueueStats.averageMeshingMilliseconds()),
                        Math.max(0L, buildQueueStats.lastMeshBufferGrowthBytes()),
                        nonNegative(buildQueueStats.averageMeshBufferGrowthBytes()),
                        Math.max(0L, buildQueueStats.retainedMeshBufferBytes()),
                        nonNegative(buildQueueStats.lastLightingMilliseconds()),
                        nonNegative(buildQueueStats.averageLightingMilliseconds()),
                        nonNegative(buildQueueStats.lastGpuUploadMilliseconds()),
                        nonNegative(buildQueueStats.averageGpuUploadMilliseconds()),
                        sectionStats.totalSections(),
                        sectionStats.emptySections(),
                        sectionStats.nonEmptySections(),
                        sectionStats.chunksWithSectionBounds()
                ),
                new Rendering(
                        safeRenderStats.drawCalls(),
                        safeRenderStats.opaqueDrawCalls(),
                        safeRenderStats.cutoutDrawCalls(),
                        safeRenderStats.transparentDrawCalls(),
                        safeRenderStats.triangles(),
                        safeRenderStats.renderedLayers(),
                        safeRenderStats.renderedOpaqueChunks(),
                        safeRenderStats.renderedCutoutChunks(),
                        safeRenderStats.renderedTransparentChunks(),
                        safeRenderStats.solidTriangles(),
                        safeRenderStats.cutoutTriangles(),
                        safeRenderStats.transparentTriangles(),
                        safeRenderStats.culledMeshes(),
                        safeRenderStats.culledChunkPositions(),
                        safeRenderStats.culledByDistance(),
                        safeRenderStats.culledByBounds(),
                        safeRenderStats.sortedTransparentMeshes(),
                        safeRenderStats.loadedGpuMeshes(),
                        safeRenderStats.loadedChunkPositions(),
                        safeRenderStats.meshBytes(),
                        safeRenderStats.gpuUploadBytes(),
                        safeRenderStats.materialCount(),
                        safeRenderStats.materialLutBytes(),
                        safeRenderStats.missingMaterialCount(),
                        safeRenderStats.atlasTextureCount(),
                        safeRenderStats.atlasWidth(),
                        safeRenderStats.atlasHeight(),
                        safeRenderStats.atlasBytes(),
                        safeRenderStats.chunkVertexBytes(),
                        Math.max(0, renderedChunkBorderDebugChunks),
                        Math.max(0, renderedMeshBoundsDebugBoxes),
                        Math.max(0, renderedSectionBoundsDebugBoxes)
                ),
                new Entities(
                        Math.max(0, visibleEntitySnapshots),
                        safeEntityStats.renderedEntities(),
                        safeEntityStats.culledEntities(),
                        safeEntityStats.drawCalls(),
                        safeEntityStats.modelParts(),
                        safeEntityStats.cachedModels(),
                        Math.max(0, renderedEntityHitboxes)
                ),
                new Particles(
                        safeParticleStats.liveParticles(),
                        safeParticleStats.spawnRate(),
                        safeParticleStats.budgetUsage(),
                        safeParticleStats.evictedParticles(),
                        safeParticleStats.drawCalls(),
                        safeParticleStats.triangles(),
                        Math.max(0, renderedParticleDebugBoxes)
                ),
                new Network(
                        online,
                        safeNetworkStats.sentPackets(),
                        safeNetworkStats.receivedPackets(),
                        safeNetworkStats.sentPacketsPerSecond(),
                        safeNetworkStats.receivedPacketsPerSecond(),
                        safeNetworkStats.averagePacketBytes(),
                        safeNetworkStats.invalidPacketsDropped(),
                        safeNetworkStats.chunkStreamQueueLength(),
                        safeNetworkStats.chunkPackets(),
                        safeNetworkStats.blockUpdatePackets(),
                        safeNetworkStats.entitySnapshotPackets(),
                        safeNetworkStats.inventoryPackets(),
                        safeNetworkStats.storageOpenPackets(),
                        safeNetworkStats.chatPackets(),
                        safeNetworkStats.serverStatsPackets(),
                        safeNetworkStats.serverStats()
                ),
                new GpuResources(
                        safeResourceStats.liveChunkMeshes(),
                        safeResourceStats.liveChunkVertexArrays(),
                        safeResourceStats.liveChunkBuffers(),
                        safeResourceStats.liveChunkMeshBytes(),
                        safeResourceStats.peakChunkMeshBytes(),
                        safeResourceStats.createdChunkMeshes(),
                        safeResourceStats.disposedChunkMeshes(),
                        safeResourceStats.liveTextures(),
                        safeResourceStats.liveTextureBytes(),
                        safeResourceStats.peakTextureBytes(),
                        safeResourceStats.createdTextures(),
                        safeResourceStats.disposedTextures(),
                        safeResourceStats.liveShaderPrograms(),
                        safeResourceStats.createdShaderPrograms(),
                        safeResourceStats.disposedShaderPrograms(),
                        safeResourceStats.shaderReloadCount(),
                        safeResourceStats.failedShaderReloadCount(),
                        safeResourceStats.lastShaderReloadMilliseconds(),
                        safeResourceStats.liveParticleVertexArrays(),
                        safeResourceStats.liveParticleBuffers(),
                        safeResourceStats.liveParticleBufferBytes(),
                        safeResourceStats.liveEntityVertexArrays(),
                        safeResourceStats.liveEntityBuffers(),
                        safeResourceStats.liveEntityBufferBytes(),
                        safeResourceStats.liveFramebuffers()
                )
        );
    }

    private static double nonNegative(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            return 0.0;
        }
        return value;
    }

    public record Frame(
            int fps,
            double frameMilliseconds,
            double updateMilliseconds,
            double renderMilliseconds,
            double uiMilliseconds,
            double clientTickMilliseconds,
            double serverTickMilliseconds
    ) {
        static Frame empty() {
            return new Frame(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public record Chunks(
            int renderDistanceChunks,
            int previewRadiusChunks,
            int retentionRadiusChunks,
            int meshBuildBudgetChunks,
            double meshBuildBudgetMilliseconds,
            int loadedChunks,
            int visibleChunks,
            int dirtyChunks,
            int queuedChunks,
            int builtChunks,
            int unloadedChunks,
            long totalUnloadedChunks,
            int releasedGpuMeshLayers,
            long replacedChunkBuilds,
            long canceledChunkBuilds,
            long completedChunkBuilds,
            double chunksBuiltPerSecond,
            double averageChunkBuildWaitMilliseconds,
            double chunkGenerationMilliseconds,
            double averageChunkGenerationMilliseconds,
            double meshingMilliseconds,
            double averageMeshingMilliseconds,
            long meshBufferGrowthBytes,
            double averageMeshBufferGrowthBytes,
            long retainedMeshBufferBytes,
            double lightingMilliseconds,
            double averageLightingMilliseconds,
            double gpuUploadMilliseconds,
            double averageGpuUploadMilliseconds,
            int totalSections,
            int emptySections,
            int nonEmptySections,
            int chunksWithSectionBounds
    ) {
        static Chunks empty() {
            return new Chunks(0, 0, 0, 0, 0.0, 0, 0, 0, 0, 0, 0, 0L, 0, 0L, 0L, 0L, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0, 0L, 0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0);
        }
    }

    public record Rendering(
            int drawCalls,
            int solidDrawCalls,
            int cutoutDrawCalls,
            int transparentDrawCalls,
            int triangles,
            int renderedMeshLayers,
            int solidMeshCount,
            int cutoutMeshCount,
            int transparentMeshCount,
            int solidTriangles,
            int cutoutTriangles,
            int transparentTriangles,
            int culledMeshes,
            int culledChunks,
            int culledByDistance,
            int culledByBounds,
            int sortedTransparentMeshes,
            int loadedGpuMeshes,
            int loadedGpuChunkPositions,
            long estimatedVramBytes,
            long gpuUploadBytes,
            int materialCount,
            long materialLutBytes,
            int missingMaterialCount,
            int atlasTextureCount,
            int atlasWidth,
            int atlasHeight,
            long atlasBytes,
            int chunkVertexBytes,
            int debugChunkBorders,
            int debugMeshBounds,
            int debugSectionBounds
    ) {
        static Rendering empty() {
            return new Rendering(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0L, 0L, 0, 0L, 0, 0, 0, 0, 0L, 0, 0, 0, 0);
        }
    }

    public record Entities(
            int entityCount,
            int visibleEntityCount,
            int culledEntityCount,
            int drawCalls,
            int modelParts,
            int cachedModels,
            int debugHitboxes
    ) {
        static Entities empty() {
            return new Entities(0, 0, 0, 0, 0, 0, 0);
        }
    }

    public record Particles(
            int particleCount,
            double spawnRate,
            double budgetUsage,
            long evictedParticles,
            int drawCalls,
            int triangles,
            int debugBounds
    ) {
        static Particles empty() {
            return new Particles(0, 0.0, 0.0, 0L, 0, 0, 0);
        }
    }

    public record Network(
            boolean online,
            long sentPackets,
            long receivedPackets,
            double sentPacketsPerSecond,
            double receivedPacketsPerSecond,
            double averagePacketBytes,
            long invalidPacketsDropped,
            int chunkStreamQueueLength,
            long chunkPackets,
            long blockUpdatePackets,
            long entitySnapshotPackets,
            long inventoryPackets,
            long storageOpenPackets,
            long chatPackets,
            long serverStatsPackets,
            GamePacket.ServerStatsSnapshot serverStats
    ) {
        public Network {
            serverStats = serverStats == null
                    ? new GamePacket.ServerStatsSnapshot(0, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0.0)
                    : serverStats;
        }

        static Network offline() {
            return new Network(
                    false,
                    0L,
                    0L,
                    0.0,
                    0.0,
                    0.0,
                    0L,
                    0,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    new GamePacket.ServerStatsSnapshot(0, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0.0)
            );
        }
    }

    public record GpuResources(
            int liveChunkMeshes,
            int liveChunkVertexArrays,
            int liveChunkBuffers,
            long liveChunkMeshBytes,
            long peakChunkMeshBytes,
            long createdChunkMeshes,
            long disposedChunkMeshes,
            int liveTextures,
            long liveTextureBytes,
            long peakTextureBytes,
            long createdTextures,
            long disposedTextures,
            int liveShaderPrograms,
            long createdShaderPrograms,
            long disposedShaderPrograms,
            long shaderReloadCount,
            long failedShaderReloadCount,
            double lastShaderReloadMilliseconds,
            int liveParticleVertexArrays,
            int liveParticleBuffers,
            long liveParticleBufferBytes,
            int liveEntityVertexArrays,
            int liveEntityBuffers,
            long liveEntityBufferBytes,
            int liveFramebuffers
    ) {
        static GpuResources empty() {
            return new GpuResources(0, 0, 0, 0L, 0L, 0L, 0L, 0, 0L, 0L, 0L, 0L, 0, 0L, 0L, 0L, 0L, 0.0, 0, 0, 0L, 0, 0, 0L, 0);
        }
    }
}
