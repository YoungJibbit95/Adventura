package dev.voxelgame.client.render;

import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.DimensionSettings;
import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.entity.EntitySnapshot;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class ChunkBorderRenderer implements AutoCloseable {
    private static final int FLOATS_PER_VERTEX = 3;

    private final ShaderProgram shader;
    private final int vao;
    private final int vbo;

    public ChunkBorderRenderer() {
        this.shader = ShaderProgram.fromResources(
                "assets/voxelgame/shaders/debug_line.vert",
                "assets/voxelgame/shaders/debug_line.frag"
        );
        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    public int render(Matrix4f projection, Matrix4f view, Vector3f cameraPosition, DimensionSettings dimension, int radiusChunks) {
        List<ChunkPos> chunks = chunkPositionsAround(cameraPosition, radiusChunks);
        float[] vertices = borderVertices(chunks, dimension);
        if (vertices.length == 0) {
            return 0;
        }
        renderLines(projection, view, vertices, new Vector3f(0.96f, 0.78f, 0.36f), 0.48f, false);
        return chunks.size();
    }

    public int renderEntityHitboxes(Matrix4f projection, Matrix4f view, Collection<EntitySnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return 0;
        }
        float[] vertices = entityBoxVertices(snapshots);
        if (vertices.length == 0) {
            return 0;
        }
        renderLines(projection, view, vertices, new Vector3f(0.46f, 0.88f, 0.82f), 0.58f, true);
        return snapshots.size();
    }

    public int renderMeshBounds(Matrix4f projection, Matrix4f view, Collection<ChunkMesh.Bounds> bounds) {
        if (bounds.isEmpty()) {
            return 0;
        }
        float[] vertices = meshBoundsVertices(bounds);
        if (vertices.length == 0) {
            return 0;
        }
        renderLines(projection, view, vertices, new Vector3f(0.62f, 0.84f, 1.0f), 0.42f, true);
        return bounds.size();
    }

    private void renderLines(Matrix4f projection, Matrix4f view, float[] vertices, Vector3f color, float alpha, boolean depthTest) {
        boolean depthWasEnabled = glIsEnabled(GL_DEPTH_TEST);
        boolean blendWasEnabled = glIsEnabled(GL_BLEND);
        if (!depthTest) {
            glDisable(GL_DEPTH_TEST);
        }
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.bind();
        shader.setMatrix4("uProjection", projection);
        shader.setMatrix4("uView", view);
        shader.setVector3("uColor", color);
        shader.setFloat("uAlpha", alpha);

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
        glDrawArrays(GL_LINES, 0, vertices.length / FLOATS_PER_VERTEX);
        glBindVertexArray(0);
        glUseProgram(0);

        if (depthWasEnabled) {
            glEnable(GL_DEPTH_TEST);
        }
        if (!blendWasEnabled) {
            glDisable(GL_BLEND);
        }
    }

    public static List<ChunkPos> chunkPositionsAround(Vector3f cameraPosition, int radiusChunks) {
        int radius = Math.max(0, radiusChunks);
        ChunkPos center = ChunkPos.fromBlock((int) Math.floor(cameraPosition.x), (int) Math.floor(cameraPosition.z));
        List<ChunkPos> chunks = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        for (int z = center.z() - radius; z <= center.z() + radius; z++) {
            for (int x = center.x() - radius; x <= center.x() + radius; x++) {
                chunks.add(new ChunkPos(x, z));
            }
        }
        return chunks;
    }

    static float[] borderVertices(List<ChunkPos> chunks, DimensionSettings dimension) {
        float[] vertices = new float[chunks.size() * 24 * FLOATS_PER_VERTEX];
        int offset = 0;
        for (ChunkPos chunk : chunks) {
            float minX = chunk.x() * ChunkPos.SIZE;
            float maxX = minX + ChunkPos.SIZE;
            float minZ = chunk.z() * ChunkPos.SIZE;
            float maxZ = minZ + ChunkPos.SIZE;
            float minY = dimension.minY();
            float maxY = dimension.maxYExclusive();

            offset = line(vertices, offset, minX, minY, minZ, maxX, minY, minZ);
            offset = line(vertices, offset, maxX, minY, minZ, maxX, minY, maxZ);
            offset = line(vertices, offset, maxX, minY, maxZ, minX, minY, maxZ);
            offset = line(vertices, offset, minX, minY, maxZ, minX, minY, minZ);

            offset = line(vertices, offset, minX, maxY, minZ, maxX, maxY, minZ);
            offset = line(vertices, offset, maxX, maxY, minZ, maxX, maxY, maxZ);
            offset = line(vertices, offset, maxX, maxY, maxZ, minX, maxY, maxZ);
            offset = line(vertices, offset, minX, maxY, maxZ, minX, maxY, minZ);

            offset = line(vertices, offset, minX, minY, minZ, minX, maxY, minZ);
            offset = line(vertices, offset, maxX, minY, minZ, maxX, maxY, minZ);
            offset = line(vertices, offset, maxX, minY, maxZ, maxX, maxY, maxZ);
            offset = line(vertices, offset, minX, minY, maxZ, minX, maxY, maxZ);
        }
        return vertices;
    }

    static float[] entityBoxVertices(Collection<EntitySnapshot> snapshots) {
        float[] vertices = new float[snapshots.size() * 24 * FLOATS_PER_VERTEX];
        int offset = 0;
        for (EntitySnapshot snapshot : snapshots) {
            EntityBounds bounds = EntityBounds.forType(snapshot.typeKey());
            float halfX = bounds.width() * 0.5f;
            float halfZ = bounds.depth() * 0.5f;
            float minX = (float) snapshot.x() - halfX;
            float maxX = (float) snapshot.x() + halfX;
            float minY = EntityBounds.baseY(snapshot);
            float maxY = minY + bounds.height();
            float minZ = (float) snapshot.z() - halfZ;
            float maxZ = (float) snapshot.z() + halfZ;
            offset = box(vertices, offset, minX, minY, minZ, maxX, maxY, maxZ);
        }
        return vertices;
    }

    static float[] meshBoundsVertices(Collection<ChunkMesh.Bounds> bounds) {
        float[] vertices = new float[bounds.size() * 24 * FLOATS_PER_VERTEX];
        int offset = 0;
        for (ChunkMesh.Bounds box : bounds) {
            if (box == null || box.isEmpty()) {
                continue;
            }
            offset = box(vertices, offset, box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
        }
        if (offset == vertices.length) {
            return vertices;
        }
        float[] trimmed = new float[offset];
        System.arraycopy(vertices, 0, trimmed, 0, offset);
        return trimmed;
    }

    private static int box(float[] vertices, int offset, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        offset = line(vertices, offset, minX, minY, minZ, maxX, minY, minZ);
        offset = line(vertices, offset, maxX, minY, minZ, maxX, minY, maxZ);
        offset = line(vertices, offset, maxX, minY, maxZ, minX, minY, maxZ);
        offset = line(vertices, offset, minX, minY, maxZ, minX, minY, minZ);
        offset = line(vertices, offset, minX, maxY, minZ, maxX, maxY, minZ);
        offset = line(vertices, offset, maxX, maxY, minZ, maxX, maxY, maxZ);
        offset = line(vertices, offset, maxX, maxY, maxZ, minX, maxY, maxZ);
        offset = line(vertices, offset, minX, maxY, maxZ, minX, maxY, minZ);
        offset = line(vertices, offset, minX, minY, minZ, minX, maxY, minZ);
        offset = line(vertices, offset, maxX, minY, minZ, maxX, maxY, minZ);
        offset = line(vertices, offset, maxX, minY, maxZ, maxX, maxY, maxZ);
        return line(vertices, offset, minX, minY, maxZ, minX, maxY, maxZ);
    }

    private static int line(float[] vertices, int offset, float ax, float ay, float az, float bx, float by, float bz) {
        offset = vertex(vertices, offset, ax, ay, az);
        return vertex(vertices, offset, bx, by, bz);
    }

    private static int vertex(float[] vertices, int offset, float x, float y, float z) {
        vertices[offset++] = x;
        vertices[offset++] = y;
        vertices[offset++] = z;
        return offset;
    }

    @Override
    public void close() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        shader.close();
    }
}
