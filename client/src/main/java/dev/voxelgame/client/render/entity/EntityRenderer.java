package dev.voxelgame.client.render.entity;

import dev.voxelgame.client.render.RenderResourceTracker;
import dev.voxelgame.client.render.ShaderProgram;
import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.entity.EntitySnapshot;
import org.joml.FrustumIntersection;
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
    private static final int FLOATS_PER_VERTEX = 6;
    private static final float[] BOX_VERTICES = {
            -0.5f, 0.0f, -0.5f, 0.0f, 0.0f, -1.0f,
            0.5f, 0.0f, -0.5f, 0.0f, 0.0f, -1.0f,
            0.5f, 1.0f, -0.5f, 0.0f, 0.0f, -1.0f,
            -0.5f, 1.0f, -0.5f, 0.0f, 0.0f, -1.0f,

            0.5f, 0.0f, 0.5f, 0.0f, 0.0f, 1.0f,
            -0.5f, 0.0f, 0.5f, 0.0f, 0.0f, 1.0f,
            -0.5f, 1.0f, 0.5f, 0.0f, 0.0f, 1.0f,
            0.5f, 1.0f, 0.5f, 0.0f, 0.0f, 1.0f,

            -0.5f, 0.0f, 0.5f, -1.0f, 0.0f, 0.0f,
            -0.5f, 0.0f, -0.5f, -1.0f, 0.0f, 0.0f,
            -0.5f, 1.0f, -0.5f, -1.0f, 0.0f, 0.0f,
            -0.5f, 1.0f, 0.5f, -1.0f, 0.0f, 0.0f,

            0.5f, 0.0f, -0.5f, 1.0f, 0.0f, 0.0f,
            0.5f, 0.0f, 0.5f, 1.0f, 0.0f, 0.0f,
            0.5f, 1.0f, 0.5f, 1.0f, 0.0f, 0.0f,
            0.5f, 1.0f, -0.5f, 1.0f, 0.0f, 0.0f,

            -0.5f, 1.0f, -0.5f, 0.0f, 1.0f, 0.0f,
            0.5f, 1.0f, -0.5f, 0.0f, 1.0f, 0.0f,
            0.5f, 1.0f, 0.5f, 0.0f, 1.0f, 0.0f,
            -0.5f, 1.0f, 0.5f, 0.0f, 1.0f, 0.0f,

            -0.5f, 0.0f, 0.5f, 0.0f, -1.0f, 0.0f,
            0.5f, 0.0f, 0.5f, 0.0f, -1.0f, 0.0f,
            0.5f, 0.0f, -0.5f, 0.0f, -1.0f, 0.0f,
            -0.5f, 0.0f, -0.5f, 0.0f, -1.0f, 0.0f
    };
    private static final int[] BOX_INDICES = {
            0, 1, 2, 0, 2, 3,
            4, 5, 6, 4, 6, 7,
            8, 9, 10, 8, 10, 11,
            12, 13, 14, 12, 14, 15,
            16, 17, 18, 16, 18, 19,
            20, 21, 22, 20, 22, 23
    };

    private final ShaderProgram shader;
    private final EntityModelRegistry models = new EntityModelRegistry();
    private final EntityAnimationLibrary animations = new EntityAnimationLibrary();
    private final int vao;
    private final int vbo;
    private final int ebo;
    private final long bufferBytes = (long) BOX_VERTICES.length * Float.BYTES + (long) BOX_INDICES.length * Integer.BYTES;
    private boolean closed;

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
        glBufferData(GL_ARRAY_BUFFER, BOX_VERTICES, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, BOX_INDICES, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
        RenderResourceTracker.registerEntityBuffers(2, bufferBytes);
    }

    public int render(Matrix4f projection, Matrix4f view, Collection<EntitySnapshot> snapshots) {
        return renderDetailed(projection, view, snapshots, 0.0).renderedEntities();
    }

    public int render(Matrix4f projection, Matrix4f view, Collection<EntitySnapshot> snapshots, double timeSeconds) {
        return renderDetailed(projection, view, snapshots, timeSeconds).renderedEntities();
    }

    public RenderStats renderDetailed(Matrix4f projection, Matrix4f view, Collection<EntitySnapshot> snapshots, double timeSeconds) {
        Matrix4f projectionView = new Matrix4f(projection).mul(view);
        FrustumIntersection frustum = new FrustumIntersection(projectionView);
        shader.bind();
        shader.setMatrix4("uProjection", projection);
        shader.setMatrix4("uView", view);
        shader.setVector3("uLightDirection", new Vector3f(-0.35f, 0.82f, -0.45f).normalize());
        glBindVertexArray(vao);
        int rendered = 0;
        int culled = 0;
        int drawCalls = 0;
        int modelParts = 0;
        for (EntitySnapshot snapshot : snapshots) {
            if (!insideFrustum(frustum, snapshot)) {
                culled++;
                continue;
            }
            EntityModel model = models.modelFor(snapshot.typeKey());
            EntityPose pose = animations.poseFor(snapshot, model, timeSeconds);
            Matrix4f base = new Matrix4f()
                    .translate((float) snapshot.x(), EntityBounds.baseY(snapshot) + pose.rootYOffset(), (float) snapshot.z())
                    .rotateY((float) Math.toRadians(-snapshot.yaw()));
            for (EntityModelPart part : model.parts()) {
                drawPart(base, snapshot.typeKey(), part, pose.part(part.name()));
                drawCalls++;
                modelParts++;
            }
            rendered++;
        }
        glBindVertexArray(0);
        glUseProgram(0);
        return new RenderStats(rendered, culled, drawCalls, modelParts, models.cachedModelCount());
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
        RenderResourceTracker.releaseEntityBuffers(2, bufferBytes);
        shader.close();
    }

    private void drawPart(Matrix4f base, String typeKey, EntityModelPart part, EntityPartPose pose) {
        Matrix4f model = new Matrix4f(base)
                .translate(part.x() + pose.offsetX(), part.y() + pose.offsetY(), part.z() + pose.offsetZ())
                .rotateXYZ(part.rotationX() + pose.rotationX(), part.rotationY() + pose.rotationY(), part.rotationZ() + pose.rotationZ())
                .scale(part.width() * pose.scaleX(), part.height() * pose.scaleY(), part.depth() * pose.scaleZ());
        shader.setMatrix4("uModel", model);
        shader.setVector3("uBaseColor", EntityModelRegistry.colorFor(typeKey, part.colorRole()));
        shader.setFloat("uEmissive", part.emissive() ? 1.0f : 0.0f);
        glDrawElements(GL_TRIANGLES, BOX_INDICES.length, GL_UNSIGNED_INT, 0L);
    }

    static boolean insideFrustum(FrustumIntersection frustum, EntitySnapshot snapshot) {
        EntityBounds bounds = EntityBounds.forType(snapshot.typeKey());
        float halfX = bounds.width() * 0.5f;
        float halfZ = bounds.depth() * 0.5f;
        float minX = (float) snapshot.x() - halfX;
        float maxX = (float) snapshot.x() + halfX;
        float minY = EntityBounds.baseY(snapshot);
        float maxY = minY + bounds.height();
        float minZ = (float) snapshot.z() - halfZ;
        float maxZ = (float) snapshot.z() + halfZ;
        return frustum.testAab(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public record RenderStats(
            int renderedEntities,
            int culledEntities,
            int drawCalls,
            int modelParts,
            int cachedModels
    ) {
        public static RenderStats empty() {
            return new RenderStats(0, 0, 0, 0, 0);
        }
    }
}
