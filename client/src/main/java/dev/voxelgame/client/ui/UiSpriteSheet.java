package dev.voxelgame.client.ui;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public final class UiSpriteSheet implements AutoCloseable {
    private final int textureId;
    private final int width;
    private final int height;

    private UiSpriteSheet(int textureId, int width, int height) {
        this.textureId = textureId;
        this.width = width;
        this.height = height;
    }

    public static UiSpriteSheet load(String resourcePath, boolean keyBlackTransparent) {
        try (InputStream input = UiSpriteSheet.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing UI sprite sheet: " + resourcePath);
            }
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IllegalArgumentException("Unsupported UI sprite sheet: " + resourcePath);
            }
            int textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, toRgbaBuffer(image, keyBlackTransparent));
            glBindTexture(GL_TEXTURE_2D, 0);
            return new UiSpriteSheet(textureId, image.getWidth(), image.getHeight());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load UI sprite sheet: " + resourcePath, e);
        }
    }

    public UiSprite sprite(int x, int y, int width, int height) {
        return new UiSprite(
                this,
                x / (float) this.width,
                1.0f - (y + height) / (float) this.height,
                (x + width) / (float) this.width,
                1.0f - y / (float) this.height
        );
    }

    int textureId() {
        return textureId;
    }

    @Override
    public void close() {
        glDeleteTextures(textureId);
    }

    private static ByteBuffer toRgbaBuffer(BufferedImage image, boolean keyBlackTransparent) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int a = (argb >> 24) & 0xFF;
                if (keyBlackTransparent && a > 0 && r <= 8 && g <= 8 && b <= 8) {
                    a = 0;
                }
                buffer.put((byte) r);
                buffer.put((byte) g);
                buffer.put((byte) b);
                buffer.put((byte) a);
            }
        }
        buffer.flip();
        return buffer;
    }
}
