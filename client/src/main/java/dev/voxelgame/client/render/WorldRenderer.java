package dev.voxelgame.client.render;

import dev.voxelgame.client.render.assets.BlockTextureAtlas;
import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkStreamingRings;
import org.joml.Matrix4f;
import org.joml.FrustumIntersection;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.lwjgl.opengl.GL20.glUseProgram;

public final class WorldRenderer implements AutoCloseable {
    private final ShaderProgram shader;
    private final BlockTextureAtlas blockTextureAtlas;
    private final TerrainMaterialLut materialLut;
    private final ChunkMesher mesher = new ChunkMesher();
    private final TerrainRenderer terrainRenderer = new TerrainRenderer(new RenderPassExecutor());
    private final Map<ChunkPos, GpuChunkMesh> opaqueMeshes = new HashMap<>();
    private final Map<ChunkPos, GpuChunkMesh> cutoutMeshes = new HashMap<>();
    private final Map<ChunkPos, GpuChunkMesh> transparentMeshes = new HashMap<>();
    private final ArrayDeque<ClientWorld.LayeredMeshBuild> pendingGpuUploads = new ArrayDeque<>();
    private long lastGpuUploadBytes;

    public WorldRenderer() {
        this.shader = ShaderProgram.fromResources(
                "assets/voxelgame/shaders/chunk.vert",
                "assets/voxelgame/shaders/chunk.frag"
        );
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        BlockRenderProperties.validateRegisteredBlocks(blocks);
        this.blockTextureAtlas = BlockTextureAtlas.load(blocks);
        this.materialLut = TerrainMaterialLut.upload(blocks, blockTextureAtlas);
        BlockTextureAtlas.AtlasValidationReport atlasReport = blockTextureAtlas.validationReport();
        if (atlasReport.hasErrors()) {
            throw new IllegalStateException("Block atlas validation failed: " + atlasReport.summary()
                    + " missingTextures=" + atlasReport.missingTextures()
                    + " duplicateMappings=" + atlasReport.duplicateMappings());
        }
        if (atlasReport.hasWarnings()) {
            System.err.println("Block atlas validation warnings: missing material metadata entries="
                    + atlasReport.missingMaterialCount());
        }
        if (blockTextureAtlas.enabled()) {
            System.out.println("Loaded " + blockTextureAtlas.loadedTextureCount() + " block texture(s); "
                    + atlasReport.summary());
        }
    }

    public int rebuildDirty(ClientWorld world) {
        return rebuildDirty(world, true, true);
    }

    public int rebuildDirty(ClientWorld world, boolean ambientOcclusion) {
        return rebuildDirty(world, ambientOcclusion, true);
    }

    public int rebuildDirty(ClientWorld world, boolean ambientOcclusion, boolean transparentWater) {
        return rebuildDirty(world, ambientOcclusion, transparentWater, Integer.MAX_VALUE);
    }

    public int rebuildDirty(ClientWorld world, boolean ambientOcclusion, boolean transparentWater, int maxBuilds) {
        return rebuildDirty(world, ambientOcclusion, transparentWater, maxBuilds, null);
    }

    public int rebuildDirty(ClientWorld world, boolean ambientOcclusion, boolean transparentWater, int maxBuilds, Vector3f priorityPosition) {
        return rebuildDirty(
                world,
                ambientOcclusion,
                transparentWater,
                maxBuilds,
                priorityPosition,
                Double.POSITIVE_INFINITY,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        );
    }

    public int rebuildDirty(
            ClientWorld world,
            boolean ambientOcclusion,
            boolean transparentWater,
            int maxBuilds,
            Vector3f priorityPosition,
            double maxBuildMilliseconds,
            int renderDistanceChunks,
            int previewRadiusChunks
    ) {
        return rebuildDirty(
                world,
                ambientOcclusion,
                transparentWater,
                maxBuilds,
                priorityPosition,
                maxBuildMilliseconds,
                Double.POSITIVE_INFINITY,
                renderDistanceChunks,
                previewRadiusChunks,
                true
        );
    }

    public int rebuildDirty(
            ClientWorld world,
            boolean ambientOcclusion,
            boolean transparentWater,
            int maxBuilds,
            Vector3f priorityPosition,
            double maxBuildMilliseconds,
            double maxUploadMilliseconds,
            int renderDistanceChunks,
            int previewRadiusChunks,
            boolean greedyMeshing
    ) {
        mesher.setGreedyMeshingEnabled(greedyMeshing);
        if (pendingGpuUploads.isEmpty()) {
            pendingGpuUploads.addAll(world.buildDirtyLayeredMeshes(
                    mesher,
                    ambientOcclusion,
                    transparentWater,
                    maxBuilds,
                    priorityPosition,
                    renderDistanceChunks,
                    previewRadiusChunks,
                    maxBuildMilliseconds
            ));
            sortPendingGpuUploadsByPriority(priorityPosition);
        }
        int updated = 0;
        long uploadedBytes = 0L;
        long uploadStartNanos = System.nanoTime();
        long budgetNanos = Double.isFinite(maxUploadMilliseconds) && maxUploadMilliseconds > 0.0
                ? (long) (maxUploadMilliseconds * 1_000_000.0)
                : Long.MAX_VALUE;
        while (!pendingGpuUploads.isEmpty()) {
            if (updated > 0 && System.nanoTime() - uploadStartNanos >= budgetNanos) {
                break;
            }
            ClientWorld.LayeredMeshBuild build = pendingGpuUploads.removeFirst();
            replaceMesh(opaqueMeshes, build.pos(), build.opaqueMesh());
            replaceMesh(cutoutMeshes, build.pos(), build.cutoutMesh());
            replaceMesh(transparentMeshes, build.pos(), build.transparentMesh());
            uploadedBytes += uploadBytes(build);
            updated++;
        }
        lastGpuUploadBytes = uploadedBytes;
        world.recordChunkGpuUpload(updated == 0 ? 0.0 : (System.nanoTime() - uploadStartNanos) / 1_000_000.0);
        return updated;
    }

    public MeshReleaseStats releaseChunks(Collection<ChunkPos> positions) {
        if (positions.isEmpty()) {
            return MeshReleaseStats.empty();
        }
        Set<ChunkPos> uniquePositions = new HashSet<>(positions);
        int releasedLayers = 0;
        int releasedChunkPositions = 0;
        long releasedBytes = 0L;
        for (ChunkPos pos : uniquePositions) {
            Release opaque = removeMesh(opaqueMeshes, pos);
            Release cutout = removeMesh(cutoutMeshes, pos);
            Release transparent = removeMesh(transparentMeshes, pos);
            int layers = opaque.layers() + cutout.layers() + transparent.layers();
            if (layers > 0) {
                releasedChunkPositions++;
            }
            releasedLayers += layers;
            releasedBytes += opaque.bytes() + cutout.bytes() + transparent.bytes();
        }
        return new MeshReleaseStats(releasedLayers, releasedChunkPositions, releasedBytes);
    }

    public RenderStats render(Matrix4f projection, Matrix4f view, ClientWorld world, Vector3f cameraPosition, int renderDistanceChunks) {
        return render(projection, view, world, cameraPosition, RenderSettings.defaults(renderDistanceChunks));
    }

    public RenderStats render(Matrix4f projection, Matrix4f view, ClientWorld world, Vector3f cameraPosition, RenderSettings settings) {
        return render(projection, view, world, cameraPosition, settings, 0.0);
    }

    public RenderStats render(Matrix4f projection, Matrix4f view, ClientWorld world, Vector3f cameraPosition, RenderSettings settings, double timeSeconds) {
        Matrix4f projectionView = new Matrix4f(projection).mul(view);
        FrustumIntersection frustum = new FrustumIntersection(projectionView);
        RenderContext context = new RenderContext(projection, view, world, cameraPosition, settings, timeSeconds, frustum);
        shader.bind();
        shader.setMatrix4("uProjection", projection);
        shader.setMatrix4("uView", view);
        shader.setVector3("uCameraPosition", cameraPosition);
        shader.setVector3("uSunDirection", new Vector3f(-0.35f, 0.82f, -0.45f).normalize());
        shader.setInt("uFogEnabled", settings.fogEnabled() ? 1 : 0);
        shader.setInt("uAmbientOcclusionEnabled", settings.ambientOcclusionEnabled() ? 1 : 0);
        shader.setInt("uSoftShadowsEnabled", settings.softShadowsEnabled() ? 1 : 0);
        shader.setInt("uBloomEnabled", settings.bloomEnabled() ? 1 : 0);
        shader.setInt("uUnderwater", settings.underwater() ? 1 : 0);
        shader.setInt("uSimpleWater", settings.simpleWater() ? 1 : 0);
        shader.setInt("uWindEnabled", 1);
        shader.setInt("uRenderDebugMode", settings.debugView().shaderId());
        shader.setFloat("uFogStart", settings.fogStart());
        shader.setFloat("uFogEnd", settings.fogEnd());
        shader.setFloat("uTime", (float) timeSeconds);
        shader.setFloat("uShadowStrength", settings.softShadowsEnabled() ? 0.38f : 0.20f);
        shader.setFloat("uBloomStrength", settings.bloomStrength());
        shader.setFloat("uBloomThreshold", settings.bloomThreshold());
        shader.setFloat("uNightLightBoost", settings.nightLightBoost());
        shader.setFloat("uCaveDarkness", settings.caveDarkness());
        shader.setFloat("uWeatherFlash", settings.weatherFlash());
        shader.setVector3("uFogColor", new Vector3f(settings.fogR(), settings.fogG(), settings.fogB()));
        shader.setVector3("uBiomeTintColor", new Vector3f(settings.biomeTintR(), settings.biomeTintG(), settings.biomeTintB()));
        shader.setFloat("uGlobalBrightness", settings.globalBrightness());
        blockTextureAtlas.bindAndApply(shader, 0);
        materialLut.bindAndApply(shader, 1);
        TerrainRenderer.Result terrain = terrainRenderer.render(context, opaqueMeshes, cutoutMeshes, transparentMeshes);
        RenderPassStats opaquePass = terrain.opaquePass();
        RenderPassStats cutoutPass = terrain.cutoutPass();
        RenderPassStats transparentPass = terrain.transparentPass();
        glUseProgram(0);
        int culledMeshes = opaquePass.culledMeshes() + cutoutPass.culledMeshes() + transparentPass.culledMeshes();
        int culledByDistance = opaquePass.culledByDistance() + cutoutPass.culledByDistance() + transparentPass.culledByDistance();
        int culledByBounds = opaquePass.culledByBounds() + cutoutPass.culledByBounds() + transparentPass.culledByBounds();
        int drawCalls = opaquePass.drawCalls() + cutoutPass.drawCalls() + transparentPass.drawCalls();
        int triangles = opaquePass.triangles() + cutoutPass.triangles() + transparentPass.triangles();
        return new RenderStats(
                opaquePass.renderedMeshes() + cutoutPass.renderedMeshes() + transparentPass.renderedMeshes(),
                culledMeshes,
                terrain.culledChunkPositions(),
                opaquePass.renderedMeshes(),
                cutoutPass.renderedMeshes(),
                transparentPass.renderedMeshes(),
                drawCalls,
                triangles,
                opaqueMeshes.size() + cutoutMeshes.size() + transparentMeshes.size(),
                loadedChunkPositions(),
                meshBytes(),
                opaquePass,
                cutoutPass,
                transparentPass,
                materialLut.materialCount(),
                materialLut.estimatedBytes(),
                materialLut.missingMaterialCount(),
                blockTextureAtlas.loadedTextureCount(),
                ChunkMesher.VERTEX_BYTES,
                culledByDistance,
                culledByBounds,
                transparentMeshes.size(),
                lastGpuUploadBytes,
                blockTextureAtlas.validationReport().atlasWidth(),
                blockTextureAtlas.validationReport().atlasHeight(),
                blockTextureAtlas.validationReport().estimatedBytes()
        );
    }

    public void clearMeshes() {
        closeMeshes(opaqueMeshes);
        closeMeshes(cutoutMeshes);
        closeMeshes(transparentMeshes);
        pendingGpuUploads.clear();
    }

    public List<ChunkMesh.Bounds> meshBounds() {
        List<ChunkMesh.Bounds> bounds = new ArrayList<>(opaqueMeshes.size() + cutoutMeshes.size() + transparentMeshes.size());
        addMeshBounds(bounds, opaqueMeshes.values());
        addMeshBounds(bounds, cutoutMeshes.values());
        addMeshBounds(bounds, transparentMeshes.values());
        return bounds;
    }

    private static void replaceMesh(Map<ChunkPos, GpuChunkMesh> target, ChunkPos pos, ChunkMesh mesh) {
        GpuChunkMesh old = target.remove(pos);
        if (old != null) {
            old.close();
        }
        if (!mesh.isEmpty()) {
            target.put(pos, new GpuChunkMesh(mesh));
        }
    }

    private static long uploadBytes(ClientWorld.LayeredMeshBuild build) {
        long bytes = 0L;
        if (!build.opaqueMesh().isEmpty()) {
            bytes += build.opaqueMesh().estimatedBytes();
        }
        if (!build.cutoutMesh().isEmpty()) {
            bytes += build.cutoutMesh().estimatedBytes();
        }
        if (!build.transparentMesh().isEmpty()) {
            bytes += build.transparentMesh().estimatedBytes();
        }
        return bytes;
    }

    private void sortPendingGpuUploadsByPriority(Vector3f priorityPosition) {
        if (priorityPosition == null || pendingGpuUploads.size() <= 1) {
            return;
        }
        ChunkPos cameraChunk = ChunkPos.fromBlock(
                (int) Math.floor(priorityPosition.x),
                (int) Math.floor(priorityPosition.z)
        );
        List<ClientWorld.LayeredMeshBuild> sortedBuilds = new ArrayList<>(pendingGpuUploads);
        sortedBuilds.sort(Comparator.comparingLong(build -> ChunkStreamingRings.distanceSquared(cameraChunk, build.pos())));
        pendingGpuUploads.clear();
        pendingGpuUploads.addAll(sortedBuilds);
    }

    private static Release removeMesh(Map<ChunkPos, GpuChunkMesh> target, ChunkPos pos) {
        GpuChunkMesh mesh = target.remove(pos);
        if (mesh == null) {
            return Release.empty();
        }
        long bytes = mesh.estimatedBytes();
        mesh.close();
        return new Release(1, bytes);
    }

    private static void closeMeshes(Map<ChunkPos, GpuChunkMesh> target) {
        for (GpuChunkMesh mesh : target.values()) {
            mesh.close();
        }
        target.clear();
    }

    private long meshBytes() {
        return meshBytes(opaqueMeshes) + meshBytes(cutoutMeshes) + meshBytes(transparentMeshes);
    }

    private int loadedChunkPositions() {
        Set<ChunkPos> positions = new HashSet<>(opaqueMeshes.keySet());
        positions.addAll(cutoutMeshes.keySet());
        positions.addAll(transparentMeshes.keySet());
        return positions.size();
    }

    private static long meshBytes(Map<ChunkPos, GpuChunkMesh> meshes) {
        long bytes = 0L;
        for (GpuChunkMesh mesh : meshes.values()) {
            bytes += mesh.estimatedBytes();
        }
        return bytes;
    }

    private static void addMeshBounds(List<ChunkMesh.Bounds> target, Collection<GpuChunkMesh> meshes) {
        for (GpuChunkMesh mesh : meshes) {
            ChunkMesh.Bounds bounds = mesh.bounds();
            if (bounds != null && !bounds.isEmpty()) {
                target.add(bounds);
            }
        }
    }

    public static boolean withinRenderDistance(ChunkPos pos, Vector3f cameraPosition, int renderDistanceChunks) {
        return VisibilityCollector.withinRenderDistance(pos, cameraPosition, renderDistanceChunks);
    }

    public static List<ChunkPos> transparentRenderOrder(Collection<ChunkPos> positions, Vector3f cameraPosition) {
        return VisibilityCollector.transparentRenderOrder(positions, cameraPosition);
    }

    public static List<ChunkPos> transparentRenderOrderByBounds(Map<ChunkPos, ChunkMesh.Bounds> boundsByPosition, Vector3f cameraPosition) {
        return VisibilityCollector.transparentRenderOrderByBounds(boundsByPosition, cameraPosition);
    }

    static ChunkAabb chunkAabb(ClientWorld world, ChunkPos pos) {
        VisibilityCollector.ChunkAabb bounds = VisibilityCollector.chunkAabb(world, pos);
        return new ChunkAabb(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
    }

    @Override
    public void close() {
        clearMeshes();
        materialLut.close();
        blockTextureAtlas.close();
        shader.close();
    }

    public record RenderStats(
            int renderedLayers,
            int culledMeshes,
            int culledChunkPositions,
            int renderedOpaqueChunks,
            int renderedCutoutChunks,
            int renderedTransparentChunks,
            int drawCalls,
            int triangles,
            int loadedGpuMeshes,
            int loadedChunkPositions,
            long meshBytes,
            RenderPassStats opaquePass,
            RenderPassStats cutoutPass,
            RenderPassStats transparentPass,
            int materialCount,
            long materialLutBytes,
            int missingMaterialCount,
            int atlasTextureCount,
            int chunkVertexBytes,
            int culledByDistance,
            int culledByBounds,
            int sortedTransparentMeshes,
            long gpuUploadBytes,
            int atlasWidth,
            int atlasHeight,
            long atlasBytes
    ) {
        public RenderStats {
            opaquePass = opaquePass == null ? RenderPassStats.empty(RenderPassPlan.TERRAIN_OPAQUE) : opaquePass;
            cutoutPass = cutoutPass == null ? RenderPassStats.empty(RenderPassPlan.TERRAIN_CUTOUT) : cutoutPass;
            transparentPass = transparentPass == null ? RenderPassStats.empty(RenderPassPlan.TERRAIN_TRANSLUCENT) : transparentPass;
            materialCount = Math.max(0, materialCount);
            materialLutBytes = Math.max(0L, materialLutBytes);
            missingMaterialCount = Math.max(0, missingMaterialCount);
            atlasTextureCount = Math.max(0, atlasTextureCount);
            chunkVertexBytes = Math.max(0, chunkVertexBytes);
            culledByDistance = Math.max(0, culledByDistance);
            culledByBounds = Math.max(0, culledByBounds);
            sortedTransparentMeshes = Math.max(0, sortedTransparentMeshes);
            gpuUploadBytes = Math.max(0L, gpuUploadBytes);
            atlasWidth = Math.max(0, atlasWidth);
            atlasHeight = Math.max(0, atlasHeight);
            atlasBytes = Math.max(0L, atlasBytes);
        }

        public RenderStats(
                int renderedLayers,
                int culledMeshes,
                int culledChunkPositions,
                int renderedOpaqueChunks,
                int renderedCutoutChunks,
                int renderedTransparentChunks,
                int drawCalls,
                int triangles,
                int loadedGpuMeshes,
                int loadedChunkPositions,
                long meshBytes
        ) {
            this(
                    renderedLayers,
                    culledMeshes,
                    culledChunkPositions,
                    renderedOpaqueChunks,
                    renderedCutoutChunks,
                    renderedTransparentChunks,
                    drawCalls,
                    triangles,
                    loadedGpuMeshes,
                    loadedChunkPositions,
                    meshBytes,
                    RenderPassStats.empty(RenderPassPlan.TERRAIN_OPAQUE),
                    RenderPassStats.empty(RenderPassPlan.TERRAIN_CUTOUT),
                    RenderPassStats.empty(RenderPassPlan.TERRAIN_TRANSLUCENT),
                    0,
                    0L,
                    0,
                    0,
                    ChunkMesher.VERTEX_BYTES,
                    0,
                    culledMeshes,
                    renderedTransparentChunks,
                    0L,
                    0,
                    0,
                    0L
            );
        }

        public RenderStats(int renderedChunks, int culledChunks) {
            this(renderedChunks, culledChunks, culledChunks, renderedChunks, 0, 0, renderedChunks, 0, 0, 0, 0L);
        }

        public int renderedChunks() {
            return renderedLayers;
        }

        public int culledChunks() {
            return culledChunkPositions;
        }

        public int solidTriangles() {
            return opaquePass.triangles();
        }

        public int cutoutTriangles() {
            return cutoutPass.triangles();
        }

        public int transparentTriangles() {
            return transparentPass.triangles();
        }

        public int opaqueDrawCalls() {
            return opaquePass.drawCalls();
        }

        public int cutoutDrawCalls() {
            return cutoutPass.drawCalls();
        }

        public int transparentDrawCalls() {
            return transparentPass.drawCalls();
        }

        public int renderStateChanges() {
            return opaquePass.stateChanges() + cutoutPass.stateChanges() + transparentPass.stateChanges();
        }
    }

    public record MeshReleaseStats(int releasedLayers, int releasedChunkPositions, long releasedBytes) {
        public static MeshReleaseStats empty() {
            return new MeshReleaseStats(0, 0, 0L);
        }
    }

    private record Release(int layers, long bytes) {
        static Release empty() {
            return new Release(0, 0L);
        }
    }

    record ChunkAabb(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    }
}
