package dev.voxelgame.client.render;

import dev.voxelgame.common.block.BlockRenderLayer;

import java.util.List;
import java.util.Objects;

public record ChunkMesh(float[] vertices, int[] indices, Bounds bounds, List<SectionPart> parts) {
    public ChunkMesh {
        vertices = vertices == null ? new float[0] : vertices;
        indices = indices == null ? new int[0] : indices;
        bounds = bounds == null ? computeBounds(vertices) : bounds;
        parts = parts == null ? List.of() : List.copyOf(parts);
    }

    public ChunkMesh(float[] vertices, int[] indices) {
        this(vertices, indices, computeBounds(vertices), List.of());
    }

    public ChunkMesh(float[] vertices, int[] indices, List<SectionPart> parts) {
        this(vertices, indices, computeBounds(vertices), parts);
    }

    public int vertexCount() {
        return vertices.length / ChunkMesher.FLOATS_PER_VERTEX;
    }

    public int indexCount() {
        return indices.length;
    }

    public int triangleCount() {
        return indices.length / 3;
    }

    public long estimatedBytes() {
        return (long) vertices.length * Float.BYTES + (long) indices.length * Integer.BYTES;
    }

    public boolean isEmpty() {
        return indices.length == 0;
    }

    public int partCount() {
        return parts.size();
    }

    public static Bounds computeBounds(float[] vertices) {
        if (vertices == null || vertices.length < ChunkMesher.FLOATS_PER_VERTEX) {
            return Bounds.empty();
        }
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (int offset = 0; offset + ChunkMesher.POSITION_OFFSET + 2 < vertices.length; offset += ChunkMesher.FLOATS_PER_VERTEX) {
            float x = vertices[offset + ChunkMesher.POSITION_OFFSET];
            float y = vertices[offset + ChunkMesher.POSITION_OFFSET + 1];
            float z = vertices[offset + ChunkMesher.POSITION_OFFSET + 2];
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(minZ)
                || !Float.isFinite(maxX) || !Float.isFinite(maxY) || !Float.isFinite(maxZ)) {
            return Bounds.empty();
        }
        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        public static Bounds empty() {
            return new Bounds(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        public boolean isEmpty() {
            return minX == maxX && minY == maxY && minZ == maxZ;
        }
    }

    public record SectionPart(int sectionY, BlockRenderLayer layer, int indexOffset, int indexCount, Bounds bounds) {
        public SectionPart {
            Objects.requireNonNull(layer, "layer");
            bounds = bounds == null ? Bounds.empty() : bounds;
            indexOffset = Math.max(0, indexOffset);
            indexCount = Math.max(0, indexCount);
        }

        public int triangleCount() {
            return indexCount / 3;
        }

        public long byteOffset() {
            return (long) indexOffset * Integer.BYTES;
        }

        public boolean isEmpty() {
            return indexCount == 0 || bounds.isEmpty();
        }
    }
}
