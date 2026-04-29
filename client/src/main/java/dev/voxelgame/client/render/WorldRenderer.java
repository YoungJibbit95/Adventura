package dev.voxelgame.client.render;

import dev.voxelgame.client.render.assets.BlockTextureAtlas;
import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.world.ChunkPos;
import org.joml.Matrix4f;
import org.joml.FrustumIntersection;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

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
    private final Map<ChunkPos, GpuChunkMesh> transparentMeshes = new HashMap<>();

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
        shader.setFloat("uFogStart", settings.fogStart());
        shader.setFloat("uFogEnd", settings.fogEnd());
        shader.setFloat("uTime", (float) timeSeconds);
        shader.setFloat("uShadowStrength", settings.softShadowsEnabled() ? 0.38f : 0.20f);
        shader.setVector3("uFogColor", new Vector3f(settings.skyR(), settings.skyG(), settings.skyB()));
        blockTextureAtlas.bindAndApply(shader, 0);
        int rendered = 0;
        int culled = 0;
        for (Map.Entry<ChunkPos, GpuChunkMesh> entry : opaqueMeshes.entrySet()) {
            ChunkPos pos = entry.getKey();
            if (!withinRenderDistance(pos, cameraPosition, settings.renderDistanceChunks()) || !insideFrustum(frustum, world, pos)) {
                culled++;
                continue;
            }
            entry.getValue().draw();
            rendered++;
        }
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        for (Map.Entry<ChunkPos, GpuChunkMesh> entry : transparentMeshes.entrySet()) {
            ChunkPos pos = entry.getKey();
            if (!withinRenderDistance(pos, cameraPosition, settings.renderDistanceChunks()) || !insideFrustum(frustum, world, pos)) {
                continue;
            }
            entry.getValue().draw();
        }
        glDepthMask(true);
        glDisable(GL_BLEND);
        glUseProgram(0);
        return new RenderStats(rendered, culled);
    }

    public void clearMeshes() {
        closeMeshes(opaqueMeshes);
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

    public static boolean withinRenderDistance(ChunkPos pos, Vector3f cameraPosition, int renderDistanceChunks) {
        float centerX = pos.x() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5f;
        float centerZ = pos.z() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5f;
        float dx = centerX - cameraPosition.x;
        float dz = centerZ - cameraPosition.z;
        float maxDistance = (renderDistanceChunks + 1.0f) * ChunkPos.SIZE;
        return dx * dx + dz * dz <= maxDistance * maxDistance;
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

    public record RenderStats(int renderedChunks, int culledChunks) {
    }
}
