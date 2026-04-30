package dev.voxelgame.client.render;

import dev.voxelgame.client.render.assets.BlockTextureAtlas;
import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.world.ChunkPos;
import org.joml.Matrix4f;
import org.joml.FrustumIntersection;
import org.joml.Vector3f;

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
    private final ChunkMesher mesher = new ChunkMesher();
    private final Map<ChunkPos, GpuChunkMesh> opaqueMeshes = new HashMap<>();
    private final Map<ChunkPos, GpuChunkMesh> cutoutMeshes = new HashMap<>();
    private final Map<ChunkPos, GpuChunkMesh> transparentMeshes = new HashMap<>();
    private final Map<ChunkPos, Boolean> visibilityCache = new HashMap<>();
    private final float[] blockColorAlphaTable = BlockRenderProperties.shaderColorAlphaTable();
    private final float[] blockEffectsTable = BlockRenderProperties.shaderEffectsTable();

    public WorldRenderer() {
        this.shader = ShaderProgram.fromResources(
                "assets/voxelgame/shaders/chunk.vert",
                "assets/voxelgame/shaders/chunk.frag"
        );
        this.blockTextureAtlas = BlockTextureAtlas.loadDefault();
        if (blockTextureAtlas.enabled()) {
            System.out.println("Loaded " + blockTextureAtlas.loadedTextureCount() + " block texture(s) from assets/game");
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
        int updated = 0;
        for (ClientWorld.LayeredMeshBuild build : world.buildDirtyLayeredMeshes(mesher, ambientOcclusion, transparentWater, maxBuilds)) {
            replaceMesh(opaqueMeshes, build.pos(), build.opaqueMesh());
            replaceMesh(cutoutMeshes, build.pos(), build.cutoutMesh());
            replaceMesh(transparentMeshes, build.pos(), build.transparentMesh());
            updated++;
        }
        return updated;
    }

    public int rebuildDirty(ClientWorld world, boolean ambientOcclusion, boolean transparentWater, int maxBuilds, Vector3f priorityPosition) {
        int updated = 0;
        for (ClientWorld.LayeredMeshBuild build : world.buildDirtyLayeredMeshes(mesher, ambientOcclusion, transparentWater, maxBuilds, priorityPosition)) {
            replaceMesh(opaqueMeshes, build.pos(), build.opaqueMesh());
            replaceMesh(cutoutMeshes, build.pos(), build.cutoutMesh());
            replaceMesh(transparentMeshes, build.pos(), build.transparentMesh());
            updated++;
        }
        return updated;
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
        shader.bind();
        shader.setMatrix4("uProjection", projection);
        shader.setMatrix4("uView", view);
        shader.setVector3("uCameraPosition", cameraPosition);
        shader.setVector3("uSunDirection", new Vector3f(-0.35f, 0.82f, -0.45f).normalize());
        shader.setInt("uFogEnabled", settings.fogEnabled() ? 1 : 0);
        shader.setInt("uAmbientOcclusionEnabled", settings.ambientOcclusionEnabled() ? 1 : 0);
        shader.setInt("uSoftShadowsEnabled", settings.softShadowsEnabled() ? 1 : 0);
        shader.setInt("uBloomEnabled", settings.bloomEnabled() ? 1 : 0);
        shader.setFloat("uFogStart", settings.fogStart());
        shader.setFloat("uFogEnd", settings.fogEnd());
        shader.setFloat("uTime", (float) timeSeconds);
        shader.setFloat("uShadowStrength", settings.softShadowsEnabled() ? 0.38f : 0.20f);
        shader.setFloat("uBloomStrength", settings.bloomStrength());
        shader.setVector3("uFogColor", new Vector3f(settings.skyR(), settings.skyG(), settings.skyB()));
        float skyLuma = settings.skyR() * 0.2126f + settings.skyG() * 0.7152f + settings.skyB() * 0.0722f;
        float globalBrightness = Math.max(0.35f, Math.min(1.0f, 0.30f + skyLuma * 0.90f));
        shader.setFloat("uGlobalBrightness", globalBrightness);
        shader.setVector4Array("uBlockColorAlpha[0]", blockColorAlphaTable);
        shader.setVector4Array("uBlockEffects[0]", blockEffectsTable);
        blockTextureAtlas.bindAndApply(shader, 0);
        int renderedOpaque = 0;
        int renderedCutout = 0;
        int renderedTransparent = 0;
        int culledMeshes = 0;
        Set<ChunkPos> culledPositions = new HashSet<>();
        Map<ChunkPos, Boolean> visibilityCache = new HashMap<>();
        int drawCalls = 0;
        int triangles = 0;
        for (Map.Entry<ChunkPos, GpuChunkMesh> entry : opaqueMeshes.entrySet()) {
            ChunkPos pos = entry.getKey();
            if (!isVisibleChunk(pos, cameraPosition, settings.renderDistanceChunks(), frustum, world, visibilityCache)) {
                culledMeshes++;
                culledPositions.add(pos);
                continue;
            }
            GpuChunkMesh mesh = entry.getValue();
            mesh.draw();
            renderedOpaque++;
            drawCalls++;
            triangles += mesh.triangleCount();
        }
        for (Map.Entry<ChunkPos, GpuChunkMesh> entry : cutoutMeshes.entrySet()) {
            ChunkPos pos = entry.getKey();
            if (!isVisibleChunk(pos, cameraPosition, settings.renderDistanceChunks(), frustum, world, visibilityCache)) {
                culledMeshes++;
                culledPositions.add(pos);
                continue;
            }
            GpuChunkMesh mesh = entry.getValue();
            mesh.draw();
            renderedCutout++;
            drawCalls++;
            triangles += mesh.triangleCount();
        }
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        for (ChunkPos pos : transparentRenderOrder(transparentMeshes.keySet(), cameraPosition)) {
            if (!isVisibleChunk(pos, cameraPosition, settings.renderDistanceChunks(), frustum, world, visibilityCache)) {
                culledMeshes++;
                culledPositions.add(pos);
                continue;
            }
            GpuChunkMesh mesh = transparentMeshes.get(pos);
            if (mesh == null) {
                continue;
            }
            mesh.draw();
            renderedTransparent++;
            drawCalls++;
            triangles += mesh.triangleCount();
        }
        glDepthMask(true);
        glDisable(GL_BLEND);
        glUseProgram(0);
        return new RenderStats(
                renderedOpaque + renderedCutout + renderedTransparent,
                culledMeshes,
                culledPositions.size(),
                renderedOpaque,
                renderedCutout,
                renderedTransparent,
                drawCalls,
                triangles,
                opaqueMeshes.size() + cutoutMeshes.size() + transparentMeshes.size(),
                loadedChunkPositions(),
                meshBytes()
        );
    }

    public void clearMeshes() {
        closeMeshes(opaqueMeshes);
        closeMeshes(cutoutMeshes);
        closeMeshes(transparentMeshes);
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

    private static double distanceSquaredToChunkCenter(ChunkPos pos, Vector3f cameraPosition) {
        double centerX = pos.x() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5;
        double centerZ = pos.z() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5;
        double dx = centerX - cameraPosition.x;
        double dz = centerZ - cameraPosition.z;
        return dx * dx + dz * dz;
    }

    private static boolean insideFrustum(FrustumIntersection frustum, ClientWorld world, ChunkPos pos) {
        float minX = pos.x() * ChunkPos.SIZE;
        float minY = world.dimension().minY();
        float minZ = pos.z() * ChunkPos.SIZE;
        float maxX = minX + ChunkPos.SIZE;
        float maxY = world.dimension().maxYExclusive();
        float maxZ = minZ + ChunkPos.SIZE;
        return frustum.testAab(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void close() {
        clearMeshes();
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
            long meshBytes
    ) {
        public RenderStats(int renderedChunks, int culledChunks) {
            this(renderedChunks, culledChunks, culledChunks, renderedChunks, 0, 0, renderedChunks, 0, 0, 0, 0L);
        }

        public int renderedChunks() {
            return renderedLayers;
        }

        public int culledChunks() {
            return culledChunkPositions;
        }
    }
}
