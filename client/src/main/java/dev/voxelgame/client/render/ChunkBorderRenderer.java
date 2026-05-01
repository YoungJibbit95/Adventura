package dev.voxelgame.client.render;

import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.DimensionSettings;
import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.math.Raycast;
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

    public int renderSectionBounds(Matrix4f projection, Matrix4f view, Collection<ChunkMesh.Bounds> bounds) {
        if (bounds.isEmpty()) {
            return 0;
        }
        float[] vertices = meshBoundsVertices(bounds);
        if (vertices.length == 0) {
            return 0;
        }
        renderLines(projection, view, vertices, new Vector3f(0.88f, 0.68f, 0.34f), 0.36f, true);
        return bounds.size();
    }

    public int renderParticleBounds(Matrix4f projection, Matrix4f view, Collection<ChunkMesh.Bounds> bounds) {
        if (bounds.isEmpty()) {
            return 0;
        }
        float[] vertices = meshBoundsVertices(bounds);
        if (vertices.length == 0) {
            return 0;
        }
        renderLines(projection, view, vertices, new Vector3f(0.86f, 0.52f, 0.94f), 0.32f, true);
        return bounds.size();
    }

    public int renderCollisionShapeBounds(Matrix4f projection, Matrix4f view, Collection<ChunkMesh.Bounds> bounds) {
        if (bounds.isEmpty()) {
            return 0;
        }
        float[] vertices = meshBoundsVertices(bounds);
        if (vertices.length == 0) {
            return 0;
        }
        renderLines(projection, view, vertices, new Vector3f(1.0f, 0.62f, 0.28f), 0.54f, true);
        return bounds.size();
    }

    public int renderProjectileSweepBounds(Matrix4f projection, Matrix4f view, Collection<ChunkMesh.Bounds> bounds) {
        if (bounds.isEmpty()) {
            return 0;
        }
        float[] vertices = meshBoundsVertices(bounds);
        if (vertices.length == 0) {
            return 0;
        }
        renderLines(projection, view, vertices, new Vector3f(1.0f, 0.88f, 0.32f), 0.62f, true);
        return bounds.size();
    }

    public int renderBlockOutline(Matrix4f projection, Matrix4f view, int x, int y, int z, Vector3f color, float alpha, boolean depthTest) {
        float[] vertices = blockBoxVertices(x, y, z, 0.003f);
        renderLines(projection, view, vertices, color, alpha, depthTest);
        return 1;
    }

    public int renderMiningFaceProgress(Matrix4f projection, Matrix4f view, Raycast.Hit hit, float progress, Vector3f color, float alpha) {
        float[] vertices = faceProgressVertices(hit, progress);
        if (vertices.length == 0) {
            return 0;
        }
        renderLines(projection, view, vertices, color, alpha, true);
        return 1;
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

    static float[] blockBoxVertices(int x, int y, int z, float inflate) {
        float pad = Math.max(0.0f, inflate);
        float minX = x - pad;
        float minY = y - pad;
        float minZ = z - pad;
        float maxX = x + 1.0f + pad;
        float maxY = y + 1.0f + pad;
        float maxZ = z + 1.0f + pad;
        float[] vertices = new float[24 * FLOATS_PER_VERTEX];
        box(vertices, 0, minX, minY, minZ, maxX, maxY, maxZ);
        return vertices;
    }

    static float[] faceProgressVertices(Raycast.Hit hit, float progress) {
        if (hit == null) {
            return new float[0];
        }
        float t = Math.max(0.0f, Math.min(1.0f, progress));
        float half = 0.12f + 0.14f * t;
        float cx = hit.x() + 0.5f;
        float cy = hit.y() + 0.5f;
        float cz = hit.z() + 0.5f;
        float epsilon = 0.006f;
        float[] vertices = new float[8 * FLOATS_PER_VERTEX];
        int offset = 0;
        if (hit.faceX() != 0) {
            float x = hit.x() + (hit.faceX() > 0 ? 1.0f : 0.0f) + epsilon * hit.faceX();
            offset = line(vertices, offset, x, cy - half, cz - half, x, cy + half, cz - half);
            offset = line(vertices, offset, x, cy + half, cz - half, x, cy + half, cz + half);
            offset = line(vertices, offset, x, cy + half, cz + half, x, cy - half, cz + half);
            line(vertices, offset, x, cy - half, cz + half, x, cy - half, cz - half);
            return vertices;
        }
        if (hit.faceY() != 0) {
            float y = hit.y() + (hit.faceY() > 0 ? 1.0f : 0.0f) + epsilon * hit.faceY();
            offset = line(vertices, offset, cx - half, y, cz - half, cx + half, y, cz - half);
            offset = line(vertices, offset, cx + half, y, cz - half, cx + half, y, cz + half);
            offset = line(vertices, offset, cx + half, y, cz + half, cx - half, y, cz + half);
            line(vertices, offset, cx - half, y, cz + half, cx - half, y, cz - half);
            return vertices;
        }
        float z = hit.z() + (hit.faceZ() > 0 ? 1.0f : 0.0f) + epsilon * hit.faceZ();
        offset = line(vertices, offset, cx - half, cy - half, z, cx + half, cy - half, z);
        offset = line(vertices, offset, cx + half, cy - half, z, cx + half, cy + half, z);
        offset = line(vertices, offset, cx + half, cy + half, z, cx - half, cy + half, z);
        line(vertices, offset, cx - half, cy + half, z, cx - half, cy - half, z);
        return vertices;
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
