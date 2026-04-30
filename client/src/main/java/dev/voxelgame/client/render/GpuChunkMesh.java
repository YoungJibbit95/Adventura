package dev.voxelgame.client.render;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class GpuChunkMesh implements AutoCloseable {
    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int indexCount;
    private final int vertexCount;
    private final long estimatedBytes;
    private final ChunkMesh.Bounds bounds;
    private boolean closed;

    public GpuChunkMesh(ChunkMesh mesh) {
        this.indexCount = mesh.indexCount();
        this.vertexCount = mesh.vertexCount();
        this.estimatedBytes = mesh.estimatedBytes();
        this.bounds = mesh.bounds();
        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();
        this.ebo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, mesh.vertices(), GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, mesh.indices(), GL_STATIC_DRAW);

        int stride = ChunkMesher.VERTEX_BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, ChunkMesher.POSITION_OFFSET * (long) Float.BYTES);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, ChunkMesher.NORMAL_OFFSET * (long) Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, ChunkMesher.MATERIAL_INDEX_OFFSET * (long) Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 1, GL_FLOAT, false, stride, ChunkMesher.LIGHT_OFFSET * (long) Float.BYTES);
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(4, 1, GL_FLOAT, false, stride, ChunkMesher.AO_OFFSET * (long) Float.BYTES);
        glEnableVertexAttribArray(4);
        glVertexAttribPointer(5, 2, GL_FLOAT, false, stride, ChunkMesher.FACE_UV_OFFSET * (long) Float.BYTES);
        glEnableVertexAttribArray(5);

        glBindVertexArray(0);
        RenderResourceTracker.registerChunkMesh(estimatedBytes);
    }

    public int vertexCount() {
        return vertexCount;
    }

    public int indexCount() {
        return indexCount;
    }

    public int triangleCount() {
        return indexCount / 3;
    }

    public long estimatedBytes() {
        return estimatedBytes;
    }

    public ChunkMesh.Bounds bounds() {
        return bounds;
    }

    public void draw() {
        if (closed) {
            throw new IllegalStateException("Attempted to draw a closed chunk mesh");
        }
        if (indexCount == 0) {
            return;
        }
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        glDeleteBuffers(ebo);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        RenderResourceTracker.releaseChunkMesh(estimatedBytes);
    }
}
