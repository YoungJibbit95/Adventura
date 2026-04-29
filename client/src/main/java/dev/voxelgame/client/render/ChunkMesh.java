package dev.voxelgame.client.render;

public record ChunkMesh(float[] vertices, int[] indices) {
    public int vertexCount() {
        return vertices.length / ChunkMesher.FLOATS_PER_VERTEX;
    }

    public boolean isEmpty() {
        return indices.length == 0;
    }
}
