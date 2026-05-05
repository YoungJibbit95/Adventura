package dev.voxelgame.client.ui;

import dev.voxelgame.client.render.ShaderProgram;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
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

public final class UiRenderer implements AutoCloseable {
    private static final int FLOATS_PER_VERTEX = 6;

    private final ShaderProgram shader;
    private final int vao;
    private final int vbo;
    private final int ebo;
    private final List<Float> vertices = new ArrayList<>();
    private final List<Integer> indices = new ArrayList<>();

    public UiRenderer() {
        this.shader = ShaderProgram.fromResources(
                "assets/voxelgame/shaders/ui.vert",
                "assets/voxelgame/shaders/ui.frag"
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
        glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    public void begin() {
        vertices.clear();
        indices.clear();
    }

    public void rect(float x, float y, float width, float height, UiColor color) {
        int base = vertices.size() / FLOATS_PER_VERTEX;
        vertex(x, y, color);
        vertex(x + width, y, color);
        vertex(x + width, y + height, color);
        vertex(x, y + height, color);
        indices.add(base);
        indices.add(base + 1);
        indices.add(base + 2);
        indices.add(base);
        indices.add(base + 2);
        indices.add(base + 3);
    }

    public void text(String text, float x, float y, float scale, UiColor color) {
        float cursor = x;
        for (int i = 0; i < text.length(); i++) {
            drawGlyph(text.charAt(i), cursor, y, scale, color);
            cursor += (BitmapFont.WIDTH + 1) * scale;
        }
    }

    public void centeredText(String text, float centerX, float y, float scale, UiColor color) {
        text(text, centerX - BitmapFont.textWidth(text, scale) * 0.5f, y, scale, color);
    }

    public void button(UiButton button, boolean hovered) {
        UiColor color = !button.enabled() ? UiColor.BUTTON_DISABLED : hovered ? UiColor.BUTTON_HOVER : UiColor.BUTTON;
        rect(button.x(), button.y(), button.width(), button.height(), color);
        rect(button.x(), button.y(), button.width(), 2.0f, UiColor.ACCENT);
        float scale = 3.0f;
        centeredText(
                button.label(),
                button.x() + button.width() * 0.5f,
                button.y() + button.height() * 0.5f - BitmapFont.textHeight(scale) * 0.5f,
                scale,
                button.enabled() ? UiColor.WHITE : UiColor.MUTED
        );
    }

    public void flush(int framebufferWidth, int framebufferHeight) {
        // For backward compatibility, use window dimensions equal to framebuffer when only framebuffer size is provided
        flush(framebufferWidth, framebufferHeight, framebufferWidth, framebufferHeight);
    }

    public void flush(int framebufferWidth, int framebufferHeight, int windowWidth, int windowHeight) {
        if (indices.isEmpty()) {
            return;
        }
        boolean depthWasEnabled = glIsEnabled(GL_DEPTH_TEST);
        boolean blendWasEnabled = glIsEnabled(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.bind();
        // Use window dimensions (logical size) for UI coordinate system, not framebuffer (physical pixels)
        // This ensures UI is the same visual size on all displays (Retina and non-Retina)
        shader.setMatrix4("uProjection", new Matrix4f().ortho2D(0.0f, windowWidth, windowHeight, 0.0f));
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, toFloatArray(vertices), GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, toIntArray(indices), GL_DYNAMIC_DRAW);
        glDrawElements(GL_TRIANGLES, indices.size(), GL_UNSIGNED_INT, 0L);
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

    private void drawGlyph(char glyph, float x, float y, float scale, UiColor color) {
        for (int py = 0; py < BitmapFont.HEIGHT; py++) {
            for (int px = 0; px < BitmapFont.WIDTH; px++) {
                if (BitmapFont.pixel(glyph, px, py)) {
                    rect(x + px * scale, y + py * scale, scale, scale, color);
                }
            }
        }
    }

    private void vertex(float x, float y, UiColor color) {
        vertices.add(x);
        vertices.add(y);
        vertices.add(color.r());
        vertices.add(color.g());
        vertices.add(color.b());
        vertices.add(color.a());
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
}
