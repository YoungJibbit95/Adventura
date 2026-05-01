package dev.voxelgame.client.render;

import dev.voxelgame.client.render.assets.BlockTextureAtlas;
import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.ChunkPos;
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

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL20.glUseProgram;

public final class WorldRenderer implements AutoCloseable {
    private final ShaderProgram shader;
    private final BlockTextureAtlas blockTextureAtlas;
    private final TerrainMaterialLut materialLut;
    private final ChunkMesher mesher = new ChunkMesher();
    private final Map<ChunkPos, GpuChunkMesh> opaqueMeshes = new HashMap<>();
    private final Map<ChunkPos, GpuChunkMesh> cutoutMeshes = new HashMap<>();
    private final Map<ChunkPos, GpuChunkMesh> transparentMeshes = new HashMap<>();
    private final Map<ChunkPos, Boolean> visibilityCache = new HashMap<>();
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
        Set<ChunkPos> culledPositions = new HashSet<>();
        Map<ChunkPos, Boolean> visibilityCache = new HashMap<>();
        RenderPassStats opaquePass = renderTerrainPass(context, TerrainPass.OPAQUE, opaqueMeshes, visibilityCache, culledPositions);
        RenderPassStats cutoutPass = renderTerrainPass(context, TerrainPass.CUTOUT, cutoutMeshes, visibilityCache, culledPositions);
        RenderPassStats transparentPass = renderTerrainPass(context, TerrainPass.TRANSLUCENT, transparentMeshes, visibilityCache, culledPositions);
        glUseProgram(0);
        int culledMeshes = opaquePass.culledMeshes() + cutoutPass.culledMeshes() + transparentPass.culledMeshes();
        int culledByDistance = opaquePass.culledByDistance() + cutoutPass.culledByDistance() + transparentPass.culledByDistance();
        int culledByBounds = opaquePass.culledByBounds() + cutoutPass.culledByBounds() + transparentPass.culledByBounds();
        int drawCalls = opaquePass.drawCalls() + cutoutPass.drawCalls() + transparentPass.drawCalls();
        int triangles = opaquePass.triangles() + cutoutPass.triangles() + transparentPass.triangles();
        return new RenderStats(
                opaquePass.renderedMeshes() + cutoutPass.renderedMeshes() + transparentPass.renderedMeshes(),
                culledMeshes,
                culledPositions.size(),
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

    private static RenderPassStats renderTerrainPass(
            RenderContext context,
            TerrainPass pass,
            Map<ChunkPos, GpuChunkMesh> meshes,
            Map<ChunkPos, Boolean> visibilityCache,
            Set<ChunkPos> culledPositions
    ) {
        pass.begin(context);
        int renderedMeshes = 0;
        int culledMeshes = 0;
        int drawCalls = 0;
        int triangles = 0;
        int culledByDistance = 0;
        int culledByBounds = 0;
        Set<ChunkPos> passCulledPositions = new HashSet<>();
        Iterable<ChunkPos> positions = pass == TerrainPass.TRANSLUCENT
                ? transparentRenderOrder(meshes, context.cameraPosition())
                : meshes.keySet();
        try {
            for (ChunkPos pos : positions) {
                GpuChunkMesh mesh = meshes.get(pos);
                if (mesh == null) {
                    continue;
                }
                VisibilityCulling culling = meshCulling(pos, mesh, context);
                if (culling != VisibilityCulling.VISIBLE) {
                    culledMeshes++;
                    if (culling == VisibilityCulling.DISTANCE) {
                        culledByDistance++;
                    } else {
                        culledByBounds++;
                    }
                    culledPositions.add(pos);
                    passCulledPositions.add(pos);
                    continue;
                }
                mesh.draw();
                renderedMeshes++;
                drawCalls++;
                triangles += mesh.triangleCount();
            }
        } finally {
            pass.end(context);
        }
        return new RenderPassStats(pass.passName(), renderedMeshes, culledMeshes, passCulledPositions.size(), drawCalls, triangles, culledByDistance, culledByBounds);
    }

    private static VisibilityCulling meshCulling(ChunkPos pos, GpuChunkMesh mesh, RenderContext context) {
        if (!withinRenderDistance(pos, context.cameraPosition(), context.settings().renderDistanceChunks())) {
            return VisibilityCulling.DISTANCE;
        }
        ChunkMesh.Bounds bounds = mesh.bounds();
        if (bounds == null || bounds.isEmpty()) {
            return VisibilityCulling.BOUNDS;
        }
        return context.frustum().testAab(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())
                ? VisibilityCulling.VISIBLE
                : VisibilityCulling.BOUNDS;
    }

    private enum VisibilityCulling {
        VISIBLE,
        DISTANCE,
        BOUNDS
    }

    private enum TerrainPass implements RenderPass {
        OPAQUE(RenderPassPlan.TERRAIN_OPAQUE) {
            @Override
            public void begin(RenderContext context) {
                glDisable(GL_BLEND);
                glDepthMask(true);
            }
        },
        CUTOUT(RenderPassPlan.TERRAIN_CUTOUT) {
            @Override
            public void begin(RenderContext context) {
                glDisable(GL_BLEND);
                glDepthMask(true);
            }
        },
        TRANSLUCENT(RenderPassPlan.TERRAIN_TRANSLUCENT) {
            @Override
            public void begin(RenderContext context) {
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glDepthMask(false);
            }

            @Override
            public void end(RenderContext context) {
                glDepthMask(true);
                glDisable(GL_BLEND);
            }
        };

        private final String passName;

        TerrainPass(String passName) {
            this.passName = passName;
        }

        @Override
        public String passName() {
            return passName;
        }

        @Override
        public void begin(RenderContext context) {
        }

        @Override
        public void end(RenderContext context) {
        }
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

    private static boolean isVisibleChunk(
            ChunkPos pos,
            Vector3f cameraPosition,
            int renderDistanceChunks,
            FrustumIntersection frustum,
            ClientWorld world,
            Map<ChunkPos, Boolean> visibilityCache
    ) {
        return visibilityCache.computeIfAbsent(
                pos,
                key -> withinRenderDistance(key, cameraPosition, renderDistanceChunks) && insideFrustum(frustum, world, key)
        );
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
        float centerX = pos.x() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5f;
        float centerZ = pos.z() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5f;
        float dx = centerX - cameraPosition.x;
        float dz = centerZ - cameraPosition.z;
        float maxDistance = (renderDistanceChunks + 1.0f) * ChunkPos.SIZE;
        return dx * dx + dz * dz <= maxDistance * maxDistance;
    }

    public static List<ChunkPos> transparentRenderOrder(Collection<ChunkPos> positions, Vector3f cameraPosition) {
        List<ChunkPos> ordered = new ArrayList<>(positions);
        ordered.sort(Comparator
                .comparingDouble((ChunkPos pos) -> -distanceSquaredToChunkCenter(pos, cameraPosition))
                .thenComparingInt(ChunkPos::x)
                .thenComparingInt(ChunkPos::z));
        return ordered;
    }

    public static List<ChunkPos> transparentRenderOrderByBounds(Map<ChunkPos, ChunkMesh.Bounds> boundsByPosition, Vector3f cameraPosition) {
        List<ChunkPos> ordered = new ArrayList<>(boundsByPosition.keySet());
        ordered.sort(Comparator
                .comparingDouble((ChunkPos pos) -> -distanceSquaredToBoundsCenter(pos, boundsByPosition.get(pos), cameraPosition))
                .thenComparingInt(ChunkPos::x)
                .thenComparingInt(ChunkPos::z));
        return ordered;
    }

    private static List<ChunkPos> transparentRenderOrder(Map<ChunkPos, GpuChunkMesh> meshes, Vector3f cameraPosition) {
        List<ChunkPos> ordered = new ArrayList<>(meshes.keySet());
        ordered.sort(Comparator
                .comparingDouble((ChunkPos pos) -> -distanceSquaredToMeshBoundsCenter(pos, meshes.get(pos), cameraPosition))
                .thenComparingInt(ChunkPos::x)
                .thenComparingInt(ChunkPos::z));
        return ordered;
    }

    private static double distanceSquaredToMeshBoundsCenter(ChunkPos pos, GpuChunkMesh mesh, Vector3f cameraPosition) {
        return distanceSquaredToBoundsCenter(pos, mesh == null ? null : mesh.bounds(), cameraPosition);
    }

    private static double distanceSquaredToBoundsCenter(ChunkPos pos, ChunkMesh.Bounds bounds, Vector3f cameraPosition) {
        if (bounds == null || bounds.isEmpty()) {
            return distanceSquaredToChunkCenter(pos, cameraPosition);
        }
        double centerX = (bounds.minX() + bounds.maxX()) * 0.5;
        double centerY = (bounds.minY() + bounds.maxY()) * 0.5;
        double centerZ = (bounds.minZ() + bounds.maxZ()) * 0.5;
        double dx = centerX - cameraPosition.x;
        double dy = centerY - cameraPosition.y;
        double dz = centerZ - cameraPosition.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static double distanceSquaredToChunkCenter(ChunkPos pos, Vector3f cameraPosition) {
        double centerX = pos.x() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5;
        double centerZ = pos.z() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5;
        double dx = centerX - cameraPosition.x;
        double dz = centerZ - cameraPosition.z;
        return dx * dx + dz * dz;
    }

    private static boolean insideFrustum(FrustumIntersection frustum, ClientWorld world, ChunkPos pos) {
        ChunkAabb bounds = chunkAabb(world, pos);
        return frustum.testAab(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
    }

    static ChunkAabb chunkAabb(ClientWorld world, ChunkPos pos) {
        float minX = pos.x() * ChunkPos.SIZE;
        ClientWorld.ChunkVerticalBounds verticalBounds = world.verticalBounds(pos)
                .orElse(new ClientWorld.ChunkVerticalBounds(world.dimension().minY(), world.dimension().maxYExclusive()));
        float minY = verticalBounds.minY();
        float minZ = pos.z() * ChunkPos.SIZE;
        float maxX = minX + ChunkPos.SIZE;
        float maxY = verticalBounds.maxYExclusive();
        float maxZ = minZ + ChunkPos.SIZE;
        return new ChunkAabb(minX, minY, minZ, maxX, maxY, maxZ);
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
