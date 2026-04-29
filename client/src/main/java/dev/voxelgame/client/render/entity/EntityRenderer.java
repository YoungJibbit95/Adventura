package dev.voxelgame.client.render.entity;

import dev.voxelgame.client.render.ShaderProgram;
import dev.voxelgame.common.entity.EntitySnapshot;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collection;

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
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class EntityRenderer implements AutoCloseable {
    private static final float[] VERTICES = {
            -0.3f, 0.0f, -0.3f, 0.3f, 0.0f, -0.3f, 0.3f, 1.8f, -0.3f, -0.3f, 1.8f, -0.3f,
            -0.3f, 0.0f, 0.3f, 0.3f, 0.0f, 0.3f, 0.3f, 1.8f, 0.3f, -0.3f, 1.8f, 0.3f
    };
    private static final int[] INDICES = {
            0, 1, 2, 0, 2, 3,
            5, 4, 7, 5, 7, 6,
            4, 0, 3, 4, 3, 7,
            1, 5, 6, 1, 6, 2,
            3, 2, 6, 3, 6, 7,
            4, 5, 1, 4, 1, 0
    };

    private final ShaderProgram shader;
    private final int vao;
    private final int vbo;
    private final int ebo;

    public EntityRenderer() {
        this.shader = ShaderProgram.fromResources(
                "assets/voxelgame/shaders/entity.vert",
                "assets/voxelgame/shaders/entity.frag"
        );
        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();
        this.ebo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, VERTICES, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, INDICES, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    public int render(Matrix4f projection, Matrix4f view, Collection<EntitySnapshot> snapshots) {
        return render(projection, view, snapshots, 0.0);
    }

    public int render(Matrix4f projection, Matrix4f view, Collection<EntitySnapshot> snapshots, double timeSeconds) {
        shader.bind();
        shader.setMatrix4("uProjection", projection);
        shader.setMatrix4("uView", view);
        glBindVertexArray(vao);
        int rendered = 0;
        for (EntitySnapshot snapshot : snapshots) {
            float bob = "voxel:player".equals(snapshot.typeKey())
                    ? 0.0f
                    : (float) Math.sin(timeSeconds * 2.5 + snapshot.entityId() * 0.001) * 0.08f;
            float scale = entityScale(snapshot.typeKey());
            float baseY = "voxel:player".equals(snapshot.typeKey())
                    ? (float) snapshot.y() - 1.62f
                    : (float) snapshot.y();
            Matrix4f model = new Matrix4f()
                    .translate((float) snapshot.x(), baseY + bob, (float) snapshot.z())
                    .rotateY((float) Math.toRadians(-snapshot.yaw()))
                    .scale(scale);
            shader.setMatrix4("uModel", model);
            shader.setVector3("uBaseColor", baseColor(snapshot.typeKey()));
            shader.setVector3("uHeadColor", headColor(snapshot.typeKey()));
            glDrawElements(GL_TRIANGLES, INDICES.length, GL_UNSIGNED_INT, 0L);
            rendered++;
        }
        glBindVertexArray(0);
        glUseProgram(0);
        return rendered;
    }

    @Override
    public void close() {
        glDeleteBuffers(ebo);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        shader.close();
    }

    private static float entityScale(String typeKey) {
        return switch (typeKey) {
            case "voxel:cozy_sheep" -> 0.72f;
            case "voxel:forest_bunny" -> 0.38f;
            case "voxel:moss_snail" -> 0.34f;
            case "voxel:firefly_swarm" -> 0.18f;
            case "voxel:little_boar" -> 0.62f;
            case "voxel:snow_hare" -> 0.45f;
            case "voxel:mire_wisp" -> 0.62f;
            case "voxel:dune_crawler" -> 0.70f;
            case "voxel:forest_grazer", "voxel:meadow_grazer" -> 0.82f;
            default -> 1.0f;
        };
    }

    private static Vector3f baseColor(String typeKey) {
        return switch (typeKey) {
            case "voxel:cozy_sheep" -> new Vector3f(0.82f, 0.78f, 0.66f);
            case "voxel:forest_bunny" -> new Vector3f(0.45f, 0.34f, 0.26f);
            case "voxel:moss_snail" -> new Vector3f(0.20f, 0.38f, 0.22f);
            case "voxel:firefly_swarm" -> new Vector3f(0.80f, 0.76f, 0.18f);
            case "voxel:little_boar" -> new Vector3f(0.42f, 0.26f, 0.18f);
            case "voxel:snow_hare" -> new Vector3f(0.62f, 0.68f, 0.70f);
            case "voxel:mire_wisp" -> new Vector3f(0.10f, 0.36f, 0.32f);
            case "voxel:dune_crawler" -> new Vector3f(0.54f, 0.38f, 0.18f);
            case "voxel:forest_grazer" -> new Vector3f(0.18f, 0.28f, 0.20f);
            case "voxel:player" -> new Vector3f(0.18f, 0.24f, 0.30f);
            default -> new Vector3f(0.24f, 0.28f, 0.20f);
        };
    }

    private static Vector3f headColor(String typeKey) {
        return switch (typeKey) {
            case "voxel:cozy_sheep" -> new Vector3f(0.96f, 0.92f, 0.78f);
            case "voxel:forest_bunny" -> new Vector3f(0.72f, 0.58f, 0.42f);
            case "voxel:moss_snail" -> new Vector3f(0.46f, 0.64f, 0.34f);
            case "voxel:firefly_swarm" -> new Vector3f(1.0f, 0.94f, 0.34f);
            case "voxel:little_boar" -> new Vector3f(0.62f, 0.42f, 0.30f);
            case "voxel:snow_hare" -> new Vector3f(0.92f, 0.96f, 0.94f);
            case "voxel:mire_wisp" -> new Vector3f(0.28f, 0.92f, 0.70f);
            case "voxel:dune_crawler" -> new Vector3f(0.82f, 0.62f, 0.28f);
            case "voxel:forest_grazer" -> new Vector3f(0.46f, 0.58f, 0.34f);
            case "voxel:player" -> new Vector3f(0.74f, 0.84f, 0.72f);
            default -> new Vector3f(0.62f, 0.72f, 0.44f);
        };
    }
}
