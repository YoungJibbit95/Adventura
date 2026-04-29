package dev.voxelgame.client.render;

public record ChunkMesh(float[] vertices, int[] indices) {
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
}
