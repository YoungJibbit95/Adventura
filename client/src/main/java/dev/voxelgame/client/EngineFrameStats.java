package dev.voxelgame.client;

import dev.voxelgame.client.net.ClientNetworkStats;
import dev.voxelgame.client.render.RenderResourceTracker;
import dev.voxelgame.client.render.WorldRenderer;
import dev.voxelgame.client.render.entity.EntityRenderer;
import dev.voxelgame.client.render.particle.ParticleSystem;
import dev.voxelgame.client.world.ChunkBuildQueue;
import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.engine.EngineJobType;
import dev.voxelgame.common.net.GamePacket;

public record EngineFrameStats(
        Frame frame,
        FramePhases phases,
        Jobs jobs,
        Chunks chunks,
        Rendering rendering,
        Entities entities,
        Particles particles,
        Network network,
        GpuResources gpuResources,
        Budgets budgets
) {
    public EngineFrameStats {
        frame = frame == null ? Frame.empty() : frame;
        phases = phases == null ? FramePhases.empty() : phases;
        jobs = jobs == null ? Jobs.empty() : jobs;
        chunks = chunks == null ? Chunks.empty() : chunks;
        rendering = rendering == null ? Rendering.empty() : rendering;
        entities = entities == null ? Entities.empty() : entities;
        particles = particles == null ? Particles.empty() : particles;
        network = network == null ? Network.offline() : network;
        gpuResources = gpuResources == null ? GpuResources.empty() : gpuResources;
        budgets = budgets == null ? Budgets.empty() : budgets;
    }

    public static EngineFrameStats empty() {
        return new EngineFrameStats(
                Frame.empty(),
                FramePhases.empty(),
                Jobs.empty(),
                Chunks.empty(),
                Rendering.empty(),
                Entities.empty(),
                Particles.empty(),
                Network.offline(),
                GpuResources.empty(),
                Budgets.empty()
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
        return capture(
                fps,
                frameMilliseconds,
                updateMilliseconds,
                renderMilliseconds,
                uiMilliseconds,
                FramePhases.fromLegacy(updateMilliseconds, renderMilliseconds, uiMilliseconds),
                settings,
                retentionRadiusChunks,
                world,
                renderStats,
                builtChunks,
                unloadedChunks,
                releasedGpuMeshLayers,
                visibleEntitySnapshots,
                entityStats,
                renderedEntityHitboxes,
                renderedChunkBorderDebugChunks,
                renderedMeshBoundsDebugBoxes,
                renderedSectionBoundsDebugBoxes,
                renderedParticleDebugBoxes,
                particleStats,
                online,
                networkStats,
                resourceStats
        );
    }

    public static EngineFrameStats capture(
            int fps,
            double frameMilliseconds,
            double updateMilliseconds,
            double renderMilliseconds,
            double uiMilliseconds,
            FramePhases phases,
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
        int terrainCacheChunks = world == null ? 0 : world.terrainCacheChunkCount();
        long terrainCacheBytes = world == null ? 0L : world.terrainCacheBytes();
        ChunkBuildQueue.Snapshot buildQueueStats = world == null ? ChunkBuildQueue.Snapshot.empty() : world.buildQueueStats();
        ClientWorld.SectionStats sectionStats = world == null
                ? new ClientWorld.SectionStats(0, 0, 0, 0, 0, 0, 0, 0)
                : world.sectionStats();
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
                phases == null ? FramePhases.fromLegacy(updateMilliseconds, renderMilliseconds, uiMilliseconds) : phases,
                Jobs.from(buildQueueStats, safeNetworkStats),
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
                        sectionStats.chunksWithSectionBounds(),
                        sectionStats.dirtyGeometrySections(),
                        sectionStats.dirtyLightSections(),
                        sectionStats.dirtyFluidSections(),
                        sectionStats.dirtyBlockEntitySections(),
                        terrainCacheChunks,
                        terrainCacheBytes
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
                        Math.max(0, renderedSectionBoundsDebugBoxes),
                        safeRenderStats.renderStateChanges(),
                        safeRenderStats.loadedSectionParts(),
                        safeRenderStats.renderedSectionParts(),
                        safeRenderStats.culledSectionParts()
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
                ),
                Budgets.capture(
                        settings,
                        frameMilliseconds,
                        buildQueueStats.lastGenerationMilliseconds(),
                        buildQueueStats.lastLightingMilliseconds(),
                        buildQueueStats.lastMeshingMilliseconds(),
                        buildQueueStats.lastGpuUploadMilliseconds(),
                        safeRenderStats.gpuUploadBytes(),
                        safeRenderStats.drawCalls(),
                        safeRenderStats.triangles(),
                        safeParticleStats.budgetUsage(),
                        safeEntityStats.renderedEntities(),
                        safeNetworkStats.serverStats().saveWriteMillisecondsPerSecond(),
                        (safeNetworkStats.sentPacketsPerSecond() + safeNetworkStats.receivedPacketsPerSecond())
                                * safeNetworkStats.averagePacketBytes()
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

    public record FramePhases(
            double inputMilliseconds,
            double networkMilliseconds,
            double playerMilliseconds,
            double worldMilliseconds,
            double chunkJobsMilliseconds,
            double gpuUploadMilliseconds,
            double renderPassMilliseconds,
            double uiMilliseconds
    ) {
        public FramePhases {
            inputMilliseconds = nonNegative(inputMilliseconds);
            networkMilliseconds = nonNegative(networkMilliseconds);
            playerMilliseconds = nonNegative(playerMilliseconds);
            worldMilliseconds = nonNegative(worldMilliseconds);
            chunkJobsMilliseconds = nonNegative(chunkJobsMilliseconds);
            gpuUploadMilliseconds = nonNegative(gpuUploadMilliseconds);
            renderPassMilliseconds = nonNegative(renderPassMilliseconds);
            uiMilliseconds = nonNegative(uiMilliseconds);
        }

        static FramePhases empty() {
            return new FramePhases(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        static FramePhases fromLegacy(double updateMilliseconds, double renderMilliseconds, double uiMilliseconds) {
            return new FramePhases(0.0, 0.0, updateMilliseconds, 0.0, 0.0, 0.0, renderMilliseconds, uiMilliseconds);
        }

        public double updateTotalMilliseconds() {
            return inputMilliseconds + networkMilliseconds + playerMilliseconds + worldMilliseconds;
        }

        public double renderTotalMilliseconds() {
            return chunkJobsMilliseconds + renderPassMilliseconds;
        }
    }

    public record Jobs(
            JobCounter chunkGenerate,
            JobCounter chunkLight,
            JobCounter chunkMesh,
            JobCounter saveWrite,
            JobCounter netEncode
    ) {
        public Jobs {
            chunkGenerate = chunkGenerate == null ? JobCounter.empty(EngineJobType.CHUNK_GENERATE) : chunkGenerate;
            chunkLight = chunkLight == null ? JobCounter.empty(EngineJobType.CHUNK_LIGHT) : chunkLight;
            chunkMesh = chunkMesh == null ? JobCounter.empty(EngineJobType.CHUNK_MESH) : chunkMesh;
            saveWrite = saveWrite == null ? JobCounter.empty(EngineJobType.SAVE_WRITE) : saveWrite;
            netEncode = netEncode == null ? JobCounter.empty(EngineJobType.NET_ENCODE) : netEncode;
        }

        static Jobs empty() {
            return new Jobs(
                    JobCounter.empty(EngineJobType.CHUNK_GENERATE),
                    JobCounter.empty(EngineJobType.CHUNK_LIGHT),
                    JobCounter.empty(EngineJobType.CHUNK_MESH),
                    JobCounter.empty(EngineJobType.SAVE_WRITE),
                    JobCounter.empty(EngineJobType.NET_ENCODE)
            );
        }

        static Jobs from(ChunkBuildQueue.Snapshot buildQueue, ClientNetworkStats.Snapshot network) {
            ChunkBuildQueue.Snapshot safeBuildQueue = buildQueue == null ? ChunkBuildQueue.Snapshot.empty() : buildQueue;
            ClientNetworkStats.Snapshot safeNetwork = network == null ? ClientNetworkStats.Snapshot.offline() : network;
            GamePacket.ServerStatsSnapshot serverStats = safeNetwork.serverStats();
            return new Jobs(
                    new JobCounter(EngineJobType.CHUNK_GENERATE, 0, 0, safeBuildQueue.completedGenerationJobs(), 0L),
                    new JobCounter(EngineJobType.CHUNK_LIGHT, 0, 0, safeBuildQueue.completedLightingJobs(), 0L),
                    new JobCounter(EngineJobType.CHUNK_MESH, safeBuildQueue.queuedBuilds(), 0, safeBuildQueue.completedBuilds(), safeBuildQueue.canceledBuilds()),
                    new JobCounter(
                            EngineJobType.SAVE_WRITE,
                            serverStats.savePendingWrites(),
                            serverStats.saveRunningWrites(),
                            serverStats.saveCompletedWrites(),
                            serverStats.saveFailedWrites() + serverStats.saveRejectedWrites()
                    ),
                    new JobCounter(
                            EngineJobType.NET_ENCODE,
                            safeNetwork.chunkStreamQueueLength(),
                            0,
                            safeNetwork.sentPackets() + serverStats.sentPackets(),
                            safeNetwork.invalidPacketsDropped() + serverStats.rejectedChunkRequests() + serverStats.failedChunkRequests()
                    )
            );
        }

        public JobCounter forType(EngineJobType type) {
            return switch (type == null ? EngineJobType.CHUNK_MESH : type) {
                case CHUNK_GENERATE -> chunkGenerate;
                case CHUNK_LIGHT -> chunkLight;
                case CHUNK_MESH -> chunkMesh;
                case SAVE_WRITE -> saveWrite;
                case NET_ENCODE -> netEncode;
            };
        }
    }

    public record JobCounter(
            EngineJobType type,
            int pendingJobs,
            int runningJobs,
            long completedJobs,
            long canceledJobs
    ) {
        public JobCounter {
            type = type == null ? EngineJobType.CHUNK_MESH : type;
            pendingJobs = Math.max(0, pendingJobs);
            runningJobs = Math.max(0, runningJobs);
            completedJobs = Math.max(0L, completedJobs);
            canceledJobs = Math.max(0L, canceledJobs);
        }

        static JobCounter empty(EngineJobType type) {
            return new JobCounter(type, 0, 0, 0L, 0L);
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
            int chunksWithSectionBounds,
            int dirtyGeometrySections,
            int dirtyLightSections,
            int dirtyFluidSections,
            int dirtyBlockEntitySections,
            int terrainCacheChunks,
            long terrainCacheBytes
    ) {
        public Chunks {
            terrainCacheChunks = Math.max(0, terrainCacheChunks);
            terrainCacheBytes = Math.max(0L, terrainCacheBytes);
        }

        static Chunks empty() {
            return new Chunks(0, 0, 0, 0, 0.0, 0, 0, 0, 0, 0, 0, 0L, 0, 0L, 0L, 0L, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0, 0L, 0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0L);
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
            int debugSectionBounds,
            int renderStateChanges,
            int loadedSectionParts,
            int renderedSectionParts,
            int culledSectionParts
    ) {
        public Rendering {
            loadedSectionParts = Math.max(0, loadedSectionParts);
            renderedSectionParts = Math.max(0, renderedSectionParts);
            culledSectionParts = Math.max(0, culledSectionParts);
        }

        static Rendering empty() {
            return new Rendering(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0L, 0L, 0, 0L, 0, 0, 0, 0, 0L, 0, 0, 0, 0, 0, 0, 0, 0);
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
                    ? GamePacket.ServerStatsSnapshot.empty()
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
                    GamePacket.ServerStatsSnapshot.empty()
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

    public record Budgets(
            String profile,
            double frameTargetMilliseconds,
            double frameUsage,
            double chunkGenerationBudgetMilliseconds,
            double chunkGenerationUsage,
            double lightingBudgetMilliseconds,
            double lightingUsage,
            double meshingBudgetMilliseconds,
            double meshingUsage,
            double gpuUploadBudgetMilliseconds,
            double gpuUploadMillisecondsUsage,
            long gpuUploadBudgetBytes,
            double gpuUploadBytesUsage,
            int drawCallBudget,
            double drawCallUsage,
            int triangleBudget,
            double triangleUsage,
            double particleUsage,
            int entityBudget,
            double entityUsage,
            double saveWriteBudgetMillisecondsPerSecond,
            double saveWriteUsage,
            double networkBytesPerSecondBudget,
            double networkBytesPerSecondUsage
    ) {
        public Budgets {
            profile = profile == null || profile.isBlank() ? "Medium" : profile;
            frameTargetMilliseconds = nonNegative(frameTargetMilliseconds);
            frameUsage = nonNegative(frameUsage);
            chunkGenerationBudgetMilliseconds = nonNegative(chunkGenerationBudgetMilliseconds);
            chunkGenerationUsage = nonNegative(chunkGenerationUsage);
            lightingBudgetMilliseconds = nonNegative(lightingBudgetMilliseconds);
            lightingUsage = nonNegative(lightingUsage);
            meshingBudgetMilliseconds = nonNegative(meshingBudgetMilliseconds);
            meshingUsage = nonNegative(meshingUsage);
            gpuUploadBudgetMilliseconds = nonNegative(gpuUploadBudgetMilliseconds);
            gpuUploadMillisecondsUsage = nonNegative(gpuUploadMillisecondsUsage);
            gpuUploadBudgetBytes = Math.max(0L, gpuUploadBudgetBytes);
            gpuUploadBytesUsage = nonNegative(gpuUploadBytesUsage);
            drawCallBudget = Math.max(0, drawCallBudget);
            drawCallUsage = nonNegative(drawCallUsage);
            triangleBudget = Math.max(0, triangleBudget);
            triangleUsage = nonNegative(triangleUsage);
            particleUsage = nonNegative(particleUsage);
            entityBudget = Math.max(0, entityBudget);
            entityUsage = nonNegative(entityUsage);
            saveWriteBudgetMillisecondsPerSecond = nonNegative(saveWriteBudgetMillisecondsPerSecond);
            saveWriteUsage = nonNegative(saveWriteUsage);
            networkBytesPerSecondBudget = nonNegative(networkBytesPerSecondBudget);
            networkBytesPerSecondUsage = nonNegative(networkBytesPerSecondUsage);
        }

        static Budgets empty() {
            return new Budgets("Medium", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0, 0, 0.0, 0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        static Budgets capture(
                GameSettings settings,
                double frameMilliseconds,
                double chunkGenerationMilliseconds,
                double lightingMilliseconds,
                double meshingMilliseconds,
                double gpuUploadMilliseconds,
                long gpuUploadBytes,
                int drawCalls,
                int triangles,
                double particleUsage,
                int renderedEntities,
                double saveWriteMillisecondsPerSecond,
                double networkBytesPerSecond
        ) {
            AlphaBudgetProfile profile = AlphaBudgetProfile.from(settings);
            double chunkGenerationBudgetMilliseconds = settings == null
                    ? profile.chunkGenerationBudgetMilliseconds()
                    : settings.chunkGenerationBudgetMilliseconds();
            double meshBudgetMilliseconds = settings == null ? profile.meshingBudgetMilliseconds() : settings.meshBuildBudgetMilliseconds();
            double uploadBudgetMilliseconds = settings == null ? profile.gpuUploadBudgetMilliseconds() : settings.gpuUploadBudgetMilliseconds();
            return new Budgets(
                    profile.label(),
                    profile.frameTargetMilliseconds(),
                    ratio(frameMilliseconds, profile.frameTargetMilliseconds()),
                    chunkGenerationBudgetMilliseconds,
                    ratio(chunkGenerationMilliseconds, chunkGenerationBudgetMilliseconds),
                    profile.lightingBudgetMilliseconds(),
                    ratio(lightingMilliseconds, profile.lightingBudgetMilliseconds()),
                    meshBudgetMilliseconds,
                    ratio(meshingMilliseconds, meshBudgetMilliseconds),
                    uploadBudgetMilliseconds,
                    ratio(gpuUploadMilliseconds, uploadBudgetMilliseconds),
                    profile.gpuUploadBudgetBytes(),
                    ratio(gpuUploadBytes, profile.gpuUploadBudgetBytes()),
                    profile.drawCallBudget(),
                    ratio(drawCalls, profile.drawCallBudget()),
                    profile.triangleBudget(),
                    ratio(triangles, profile.triangleBudget()),
                    particleUsage,
                    profile.entityBudget(),
                    ratio(renderedEntities, profile.entityBudget()),
                    profile.saveWriteBudgetMillisecondsPerSecond(),
                    ratio(saveWriteMillisecondsPerSecond, profile.saveWriteBudgetMillisecondsPerSecond()),
                    profile.networkBytesPerSecondBudget(),
                    ratio(networkBytesPerSecond, profile.networkBytesPerSecondBudget())
            );
        }

        private static double ratio(double value, double budget) {
            if (!Double.isFinite(value) || !Double.isFinite(budget) || budget <= 0.0) {
                return 0.0;
            }
            return Math.max(0.0, value / budget);
        }
    }

    private record AlphaBudgetProfile(
            String label,
            double frameTargetMilliseconds,
            double chunkGenerationBudgetMilliseconds,
            double lightingBudgetMilliseconds,
            double meshingBudgetMilliseconds,
            double gpuUploadBudgetMilliseconds,
            long gpuUploadBudgetBytes,
            int drawCallBudget,
            int triangleBudget,
            int entityBudget,
            double saveWriteBudgetMillisecondsPerSecond,
            double networkBytesPerSecondBudget
    ) {
        static AlphaBudgetProfile from(GameSettings settings) {
            int renderDistance = settings == null ? RenderPreset.MEDIUM.renderDistanceChunks() : settings.renderDistanceChunks();
            String label = settings == null ? RenderPreset.MEDIUM.label() : settings.activePresetLabel();
            if (renderDistance <= RenderPreset.LOW.renderDistanceChunks()) {
                return new AlphaBudgetProfile(label, 1000.0 / 30.0, 1.0, 1.0, 1.5, 1.0, 1_000_000L, 650, 300_000, 64, 8.0, 64_000.0);
            }
            if (renderDistance >= RenderPreset.HIGH.renderDistanceChunks()) {
                return new AlphaBudgetProfile(label, 1000.0 / 60.0, 2.0, 2.0, 5.0, 4.0, 4_000_000L, 1_800, 1_500_000, 256, 32.0, 256_000.0);
            }
            return new AlphaBudgetProfile(label, 1000.0 / 60.0, 1.5, 1.5, 3.0, 2.0, 2_000_000L, 1_100, 800_000, 128, 16.0, 128_000.0);
        }
    }
}
