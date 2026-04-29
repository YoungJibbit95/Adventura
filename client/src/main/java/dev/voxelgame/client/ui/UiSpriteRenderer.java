package dev.voxelgame.client.ui;

import dev.voxelgame.client.render.ShaderProgram;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
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

public final class UiSpriteRenderer implements AutoCloseable {
    private static final int FLOATS_PER_VERTEX = 8;

    private final ShaderProgram shader;
    private final int vao;
    private final int vbo;
    private final int ebo;
    private final Map<Integer, Batch> batches = new LinkedHashMap<>();

    public UiSpriteRenderer() {
        this.shader = ShaderProgram.fromResources(
                "assets/voxelgame/shaders/ui_sprite.vert",
                "assets/voxelgame/shaders/ui_sprite.frag"
        );
        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();
        this.ebo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 4L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
    }

    public void begin() {
        batches.clear();
    }

    public void sprite(UiSprite sprite, float x, float y, float width, float height) {
        sprite(sprite, x, y, width, height, UiColor.WHITE);
    }

    public void sprite(UiSprite sprite, float x, float y, float width, float height, UiColor tint) {
        Batch batch = batches.computeIfAbsent(sprite.sheet().textureId(), ignored -> new Batch());
        int base = batch.vertices.size() / FLOATS_PER_VERTEX;
        vertex(batch, x, y, sprite.u0(), sprite.v1(), tint);
        vertex(batch, x + width, y, sprite.u1(), sprite.v1(), tint);
        vertex(batch, x + width, y + height, sprite.u1(), sprite.v0(), tint);
        vertex(batch, x, y + height, sprite.u0(), sprite.v0(), tint);
        batch.indices.add(base);
        batch.indices.add(base + 1);
        batch.indices.add(base + 2);
        batch.indices.add(base);
        batch.indices.add(base + 2);
        batch.indices.add(base + 3);
    }

    public void flush(int framebufferWidth, int framebufferHeight) {
        if (batches.isEmpty()) {
            return;
        }
        boolean depthWasEnabled = glIsEnabled(GL_DEPTH_TEST);
        boolean blendWasEnabled = glIsEnabled(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.bind();
        shader.setMatrix4("uProjection", new Matrix4f().ortho2D(0.0f, framebufferWidth, framebufferHeight, 0.0f));
        shader.setInt("uTexture", 0);
        glBindVertexArray(vao);
        glActiveTexture(GL_TEXTURE0);
        for (Map.Entry<Integer, Batch> entry : batches.entrySet()) {
            Batch batch = entry.getValue();
            glBindTexture(GL_TEXTURE_2D, entry.getKey());
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, toFloatArray(batch.vertices), GL_DYNAMIC_DRAW);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, toIntArray(batch.indices), GL_DYNAMIC_DRAW);
            glDrawElements(GL_TRIANGLES, batch.indices.size(), GL_UNSIGNED_INT, 0L);
        }
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(0);
        glUseProgram(0);

        if (depthWasEnabled) {
            glEnable(GL_DEPTH_TEST);
        }
        if (!blendWasEnabled) {
            glDisable(GL_BLEND);
        }
    }

    @Override
    public void close() {
        glDeleteBuffers(ebo);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        shader.close();
    }

    private static void vertex(Batch batch, float x, float y, float u, float v, UiColor tint) {
        batch.vertices.add(x);
        batch.vertices.add(y);
        batch.vertices.add(u);
        batch.vertices.add(v);
        batch.vertices.add(tint.r());
        batch.vertices.add(tint.g());
        batch.vertices.add(tint.b());
        batch.vertices.add(tint.a());
    }

    private static float[] toFloatArray(List<Float> values) {
        float[] array = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] array = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    private static final class Batch {
        private final List<Float> vertices = new ArrayList<>();
        private final List<Integer> indices = new ArrayList<>();
    }
}
