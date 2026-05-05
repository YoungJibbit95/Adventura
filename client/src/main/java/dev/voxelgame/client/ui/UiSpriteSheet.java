package dev.voxelgame.client.ui;

import dev.voxelgame.client.render.RenderResourceTracker;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
    private final long textureBytes;
    private boolean closed;

    public enum BackgroundMode {
        OPAQUE,
        KEY_BLACK,
        EDGE_CHECKER,
        EDGE_CHECKER_TRIM
    }

    private UiSpriteSheet(int textureId, int width, int height, long textureBytes) {
        this.textureId = textureId;
        this.width = width;
        this.height = height;
        this.textureBytes = textureBytes;
    }

    public static UiSpriteSheet load(String resourcePath, boolean keyBlackTransparent) {
        return load(resourcePath, keyBlackTransparent ? BackgroundMode.KEY_BLACK : BackgroundMode.OPAQUE);
    }

    public static UiSpriteSheet load(String resourcePath, BackgroundMode backgroundMode) {
        return load(resourcePath, backgroundMode, 0);
    }

    public static UiSpriteSheet load(String resourcePath, BackgroundMode backgroundMode, int maxDimension) {
        try (InputStream input = UiSpriteSheet.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing UI sprite sheet: " + resourcePath);
            }
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IllegalArgumentException("Unsupported UI sprite sheet: " + resourcePath);
            }
            image = prepareImage(image, backgroundMode);
            image = scaledImage(image, maxDimension);
            BackgroundMode uploadMode = backgroundMode == BackgroundMode.EDGE_CHECKER_TRIM ? BackgroundMode.OPAQUE : backgroundMode;
            int textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, toRgbaBuffer(image, uploadMode));
            glBindTexture(GL_TEXTURE_2D, 0);
            long textureBytes = (long) image.getWidth() * image.getHeight() * 4L;
            RenderResourceTracker.registerTexture(textureBytes);
            return new UiSpriteSheet(textureId, image.getWidth(), image.getHeight(), textureBytes);
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
                1.0f - y / (float) this.height,
                width,
                height
        );
    }

    public UiSprite fullSprite() {
        return sprite(0, 0, width, height);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    int textureId() {
        return textureId;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        glDeleteTextures(textureId);
        RenderResourceTracker.releaseTexture(textureBytes);
    }

    private static ByteBuffer toRgbaBuffer(BufferedImage image, BackgroundMode backgroundMode) {
        boolean[] edgeBackground = backgroundMode == BackgroundMode.EDGE_CHECKER ? edgeCheckerBackground(image) : null;
        ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int a = (argb >> 24) & 0xFF;
                if (backgroundMode == BackgroundMode.KEY_BLACK && a > 0 && r <= 8 && g <= 8 && b <= 8) {
                    a = 0;
                }
                if (edgeBackground != null && edgeBackground[y * image.getWidth() + x]) {
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

    static BufferedImage prepareImage(BufferedImage image, BackgroundMode backgroundMode) {
        if (backgroundMode != BackgroundMode.EDGE_CHECKER_TRIM) {
            return image;
        }
        BufferedImage cleaned = copyArgb(image);
        boolean[] background = edgeCheckerBackground(cleaned);
        for (int i = 0; i < background.length; i++) {
            if (!background[i]) {
                continue;
            }
            int x = i % cleaned.getWidth();
            int y = i / cleaned.getWidth();
            cleaned.setRGB(x, y, cleaned.getRGB(x, y) & 0x00FFFFFF);
        }
        return trimTransparentPadding(cleaned);
    }

    private static boolean[] edgeCheckerBackground(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] palette = edgeBackgroundPalette(image);
        boolean[] background = new boolean[width * height];
        int[] queue = new int[width * height];
        int head = 0;
        int tail = 0;

        for (int x = 0; x < width; x++) {
            tail = enqueueChecker(image, palette, background, queue, tail, x, 0);
            tail = enqueueChecker(image, palette, background, queue, tail, x, height - 1);
        }
        for (int y = 1; y < height - 1; y++) {
            tail = enqueueChecker(image, palette, background, queue, tail, 0, y);
            tail = enqueueChecker(image, palette, background, queue, tail, width - 1, y);
        }

        while (head < tail) {
            int index = queue[head++];
            int x = index % width;
            int y = index / width;
            if (x > 0) {
                tail = enqueueChecker(image, palette, background, queue, tail, x - 1, y);
            }
            if (x + 1 < width) {
                tail = enqueueChecker(image, palette, background, queue, tail, x + 1, y);
            }
            if (y > 0) {
                tail = enqueueChecker(image, palette, background, queue, tail, x, y - 1);
            }
            if (y + 1 < height) {
                tail = enqueueChecker(image, palette, background, queue, tail, x, y + 1);
            }
        }
        return background;
    }

    private static int enqueueChecker(BufferedImage image, int[] palette, boolean[] background, int[] queue, int tail, int x, int y) {
        int index = y * image.getWidth() + x;
        if (background[index] || !matchesCheckerPalette(image.getRGB(x, y), palette)) {
            return tail;
        }
        background[index] = true;
        queue[tail] = index;
        return tail + 1;
    }

    private static int[] edgeBackgroundPalette(BufferedImage image) {
        int[] colors = new int[16];
        int size = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        for (int x = 0; x < width; x++) {
            size = addPaletteColor(colors, size, image.getRGB(x, 0));
            size = addPaletteColor(colors, size, image.getRGB(x, height - 1));
        }
        for (int y = 1; y < height - 1; y++) {
            size = addPaletteColor(colors, size, image.getRGB(0, y));
            size = addPaletteColor(colors, size, image.getRGB(width - 1, y));
        }
        int[] palette = new int[size];
        System.arraycopy(colors, 0, palette, 0, size);
        return palette;
    }

    private static int addPaletteColor(int[] colors, int size, int argb) {
        if (size >= colors.length || !isNeutralEdgeColor(argb)) {
            return size;
        }
        int rgb = argb & 0xFFFFFF;
        for (int i = 0; i < size; i++) {
            if (colorDistance(rgb, colors[i]) <= 8) {
                return size;
            }
        }
        colors[size] = rgb;
        return size + 1;
    }

    private static boolean matchesCheckerPalette(int argb, int[] palette) {
        int a = (argb >> 24) & 0xFF;
        if (a <= 8) {
            return true;
        }
        if (palette.length == 0 || !isNeutralEdgeColor(argb)) {
            return false;
        }
        int rgb = argb & 0xFFFFFF;
        for (int color : palette) {
            if (colorDistance(rgb, color) <= 10) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNeutralEdgeColor(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return a > 8 && min >= 190 && max - min <= 18;
    }

    private static int colorDistance(int left, int right) {
        int lr = (left >> 16) & 0xFF;
        int lg = (left >> 8) & 0xFF;
        int lb = left & 0xFF;
        int rr = (right >> 16) & 0xFF;
        int rg = (right >> 8) & 0xFF;
        int rb = right & 0xFF;
        return Math.max(Math.abs(lr - rr), Math.max(Math.abs(lg - rg), Math.abs(lb - rb)));
    }

    private static BufferedImage trimTransparentPadding(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >> 24) & 0xFF) <= 8) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < minX || maxY < minY) {
            return image;
        }
        if (minX == 0 && minY == 0 && maxX == image.getWidth() - 1 && maxY == image.getHeight() - 1) {
            return image;
        }
        BufferedImage trimmed = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = trimmed.createGraphics();
        graphics.drawImage(image, 0, 0, trimmed.getWidth(), trimmed.getHeight(), minX, minY, maxX + 1, maxY + 1, null);
        graphics.dispose();
        return trimmed;
    }

    private static BufferedImage copyArgb(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private static BufferedImage scaledImage(BufferedImage image, int maxDimension) {
        if (maxDimension <= 0 || Math.max(image.getWidth(), image.getHeight()) <= maxDimension) {
            return image;
        }
        float scale = maxDimension / (float) Math.max(image.getWidth(), image.getHeight());
        int width = Math.max(1, Math.round(image.getWidth() * scale));
        int height = Math.max(1, Math.round(image.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }
}
