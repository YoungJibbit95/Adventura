package dev.voxelgame.client.render;

import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.world.ChunkPos;
import org.joml.FrustumIntersection;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class VisibilityCollector {
    private VisibilityCollector() {
    }

    static Culling meshCulling(ChunkPos pos, GpuChunkMesh mesh, RenderContext context) {
        if (!withinRenderDistance(pos, context.cameraPosition(), context.settings().renderDistanceChunks())) {
            return Culling.DISTANCE;
        }
        ChunkMesh.Bounds bounds = mesh.bounds();
        if (bounds == null || bounds.isEmpty()) {
            return Culling.BOUNDS;
        }
        return context.frustum().testAab(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())
                ? Culling.VISIBLE
                : Culling.BOUNDS;
    }

    static boolean withinRenderDistance(ChunkPos pos, Vector3f cameraPosition, int renderDistanceChunks) {
        float centerX = pos.x() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5f;
        float centerZ = pos.z() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5f;
        float dx = centerX - cameraPosition.x;
        float dz = centerZ - cameraPosition.z;
        float maxDistance = (renderDistanceChunks + 1.0f) * ChunkPos.SIZE;
        return dx * dx + dz * dz <= maxDistance * maxDistance;
    }

    static List<ChunkPos> transparentRenderOrder(Collection<ChunkPos> positions, Vector3f cameraPosition) {
        List<ChunkPos> ordered = new ArrayList<>(positions);
        ordered.sort(Comparator
                .comparingDouble((ChunkPos pos) -> -distanceSquaredToChunkCenter(pos, cameraPosition))
                .thenComparingInt(ChunkPos::x)
                .thenComparingInt(ChunkPos::z));
        return ordered;
    }

    static List<ChunkPos> transparentRenderOrderByBounds(Map<ChunkPos, ChunkMesh.Bounds> boundsByPosition, Vector3f cameraPosition) {
        List<ChunkPos> ordered = new ArrayList<>(boundsByPosition.keySet());
        ordered.sort(Comparator
                .comparingDouble((ChunkPos pos) -> -distanceSquaredToBoundsCenter(pos, boundsByPosition.get(pos), cameraPosition))
                .thenComparingInt(ChunkPos::x)
                .thenComparingInt(ChunkPos::z));
        return ordered;
    }

    static List<ChunkPos> transparentRenderOrderByMeshBounds(Map<ChunkPos, GpuChunkMesh> meshes, Vector3f cameraPosition) {
        List<ChunkPos> ordered = new ArrayList<>(meshes.keySet());
        ordered.sort(Comparator
                .comparingDouble((ChunkPos pos) -> -distanceSquaredToMeshBoundsCenter(pos, meshes.get(pos), cameraPosition))
                .thenComparingInt(ChunkPos::x)
                .thenComparingInt(ChunkPos::z));
        return ordered;
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

    static boolean insideFrustum(FrustumIntersection frustum, ClientWorld world, ChunkPos pos) {
        ChunkAabb bounds = chunkAabb(world, pos);
        return frustum.testAab(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
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

    enum Culling {
        VISIBLE,
        DISTANCE,
        BOUNDS
    }

    record ChunkAabb(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    }
}
