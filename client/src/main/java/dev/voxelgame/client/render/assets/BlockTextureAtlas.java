package dev.voxelgame.client.render.assets;

import dev.voxelgame.client.render.ShaderProgram;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.registry.Registry;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public final class BlockTextureAtlas implements AutoCloseable {
    public static final int MAX_BLOCK_ID = 256;
    public static final String BLOCK_TEXTURE_ROOT = "assets/game/textures/block/";
    public static final String BLOCKS_TEXTURE_ROOT = "assets/game/textures/blocks/";

    private final int textureId;
    private final boolean enabled;
    private final int loadedTextureCount;
    private final float[] sideUvs;
    private final float[] topUvs;
    private final float[] bottomUvs;

    private BlockTextureAtlas(int textureId, boolean enabled, int loadedTextureCount, float[] sideUvs, float[] topUvs, float[] bottomUvs) {
        this.textureId = textureId;
        this.enabled = enabled;
        this.loadedTextureCount = loadedTextureCount;
        this.sideUvs = sideUvs;
        this.topUvs = topUvs;
        this.bottomUvs = bottomUvs;
    }

    public static BlockTextureAtlas loadDefault() {
        return load(Blocks.createDefaultRegistry());
    }

    public static BlockTextureAtlas load(Registry<BlockType> blocks) {
        Map<String, BufferedImage> imagesByPath = new LinkedHashMap<>();
        Map<Short, String> sidePathByBlock = new LinkedHashMap<>();
        Map<Short, String> topPathByBlock = new LinkedHashMap<>();
        Map<Short, String> bottomPathByBlock = new LinkedHashMap<>();

        for (BlockType block : blocks.values()) {
            if (block.id() <= 0 || block.id() >= MAX_BLOCK_ID) {
                continue;
            }
            String side = findTexturePath(block, TextureFace.SIDE);
            String top = findTexturePath(block, TextureFace.TOP);
            String bottom = findTexturePath(block, TextureFace.BOTTOM);
            if (side != null) {
                sidePathByBlock.put(block.id(), side);
                imagesByPath.computeIfAbsent(side, BlockTextureAtlas::readImage);
            }
            if (top != null) {
                topPathByBlock.put(block.id(), top);
                imagesByPath.computeIfAbsent(top, BlockTextureAtlas::readImage);
            } else if (side != null) {
                topPathByBlock.put(block.id(), side);
            }
            if (bottom != null) {
                bottomPathByBlock.put(block.id(), bottom);
                imagesByPath.computeIfAbsent(bottom, BlockTextureAtlas::readImage);
            } else if (side != null) {
                bottomPathByBlock.put(block.id(), side);
            }
        }
        registerBundledSheetFallbacks(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock);

        if (imagesByPath.isEmpty()) {
            return new BlockTextureAtlas(0, false, 0, emptyUvTable(), emptyUvTable(), emptyUvTable());
        }

        int tileSize = imagesByPath.values().stream()
                .mapToInt(image -> Math.max(image.getWidth(), image.getHeight()))
                .max()
                .orElse(16);
        int columns = (int) Math.ceil(Math.sqrt(imagesByPath.size()));
        int rows = (int) Math.ceil(imagesByPath.size() / (double) columns);
        int atlasWidth = columns * tileSize;
        int atlasHeight = rows * tileSize;
        BufferedImage atlas = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        Map<String, float[]> uvByPath = new LinkedHashMap<>();
        int index = 0;
        for (Map.Entry<String, BufferedImage> entry : imagesByPath.entrySet()) {
            int column = index % columns;
            int row = index / columns;
            int x = column * tileSize;
            int y = row * tileSize;
            graphics.drawImage(entry.getValue(), x, y, tileSize, tileSize, null);
            uvByPath.put(entry.getKey(), new float[]{
                    x / (float) atlasWidth,
                    1.0f - (y + tileSize) / (float) atlasHeight,
                    (x + tileSize) / (float) atlasWidth,
                    1.0f - y / (float) atlasHeight
            });
            index++;
        }
        graphics.dispose();

        float[] sideUvs = emptyUvTable();
        float[] topUvs = emptyUvTable();
        float[] bottomUvs = emptyUvTable();
        copyBlockUvs(sideUvs, sidePathByBlock, uvByPath);
        copyBlockUvs(topUvs, topPathByBlock, uvByPath);
        copyBlockUvs(bottomUvs, bottomPathByBlock, uvByPath);

        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, atlasWidth, atlasHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, toRgbaBuffer(atlas));
        glBindTexture(GL_TEXTURE_2D, 0);

        return new BlockTextureAtlas(textureId, true, imagesByPath.size(), sideUvs, topUvs, bottomUvs);
    }

    public boolean enabled() {
        return enabled;
    }

    public int loadedTextureCount() {
        return loadedTextureCount;
    }

    public void bindAndApply(ShaderProgram shader, int unit) {
        shader.setInt("uAtlasEnabled", enabled ? 1 : 0);
        shader.setInt("uBlockAtlas", unit);
        shader.setVector4Array("uSideUv[0]", sideUvs);
        shader.setVector4Array("uTopUv[0]", topUvs);
        shader.setVector4Array("uBottomUv[0]", bottomUvs);
        if (enabled) {
            glActiveTexture(GL_TEXTURE0 + unit);
            glBindTexture(GL_TEXTURE_2D, textureId);
        }
    }

    @Override
    public void close() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
        }
    }

    public static List<String> textureCandidates(String blockKey, TextureFace face) {
        String name = blockKey.substring(blockKey.indexOf(':') + 1);
        List<String> baseNames = new ArrayList<>();
        baseNames.add(name);
        if ("grass_block".equals(name)) {
            baseNames.add("grass");
        }

        List<String> suffixes = switch (face) {
            case TOP -> List.of("_top", "_up", "");
            case BOTTOM -> List.of("_bottom", "_down", "");
            case SIDE -> List.of("_side", "");
        };
        List<String> roots = List.of(BLOCK_TEXTURE_ROOT, BLOCKS_TEXTURE_ROOT);
        List<String> candidates = new ArrayList<>();
        for (String baseName : baseNames) {
            for (String suffix : suffixes) {
                for (String root : roots) {
                    String path = root + baseName + suffix + ".png";
                    if (!candidates.contains(path)) {
                        candidates.add(path);
                    }
                }
            }
        }
        return candidates;
    }

    private static String findTexturePath(BlockType block, TextureFace face) {
        for (String path : textureCandidates(block.key(), face)) {
            if (resourceExists(path)) {
                return path;
            }
        }
        if (face != TextureFace.SIDE) {
            for (String path : textureCandidates(block.key(), TextureFace.SIDE)) {
                if (resourceExists(path)) {
                    return path;
                }
            }
        }
        return null;
    }

    private static boolean resourceExists(String path) {
        try (InputStream input = BlockTextureAtlas.class.getClassLoader().getResourceAsStream(path)) {
            return input != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static BufferedImage readImage(String path) {
        int sliceMarker = path.indexOf('#');
        if (path.startsWith("sheet:") && sliceMarker > 0) {
            return readSheetSlice(path);
        }
        try (InputStream input = BlockTextureAtlas.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing block texture: " + path);
            }
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IllegalArgumentException("Unsupported block texture: " + path);
            }
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read block texture: " + path, e);
        }
    }

    private static BufferedImage readSheetSlice(String key) {
        int marker = key.indexOf('#');
        String sheetPath = key.substring("sheet:".length(), marker);
        String[] parts = key.substring(marker + 1).split(",");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid sheet slice key: " + key);
        }
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int width = Integer.parseInt(parts[3]);
        int height = Integer.parseInt(parts[4]);
        try (InputStream input = BlockTextureAtlas.class.getClassLoader().getResourceAsStream(sheetPath)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing sprite sheet: " + sheetPath);
            }
            BufferedImage sheet = ImageIO.read(input);
            if (sheet == null) {
                throw new IllegalArgumentException("Unsupported sprite sheet: " + sheetPath);
            }
            return sheet.getSubimage(x, y, width, height);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read sheet slice: " + key, e);
        }
    }

    private static void registerBundledSheetFallbacks(
            Map<String, BufferedImage> imagesByPath,
            Map<Short, String> sidePathByBlock,
            Map<Short, String> topPathByBlock,
            Map<Short, String> bottomPathByBlock
    ) {
        String blocks = "assets/game/bloecke_blocks.png";
        String nature = "assets/game/natursachen_nature.png";
        String decor = "assets/game/deko_decor.png";
        String misc = "assets/game/misc_wasser_ui_paletten.png";

        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.STONE, blocks, "stone", 19, 29, 58, 56);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.DIRT, blocks, "dirt", 362, 29, 57, 55);
        putFace(imagesByPath, topPathByBlock, Blocks.GRASS, blocks, "grass_top", 430, 29, 56, 55);
        putFace(imagesByPath, sidePathByBlock, Blocks.GRASS, blocks, "grass_side", 497, 29, 56, 55);
        putFace(imagesByPath, bottomPathByBlock, Blocks.GRASS, blocks, "dirt", 362, 29, 57, 55);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.WATER, misc, "water_still", 48, 334, 50, 24);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SAND, blocks, "sand", 19, 104, 59, 53);
        putFace(imagesByPath, sidePathByBlock, Blocks.SKYROOT_LOG, blocks, "oak_log_side", 19, 174, 58, 54);
        putFace(imagesByPath, topPathByBlock, Blocks.SKYROOT_LOG, blocks, "oak_log_top", 88, 174, 59, 54);
        putFace(imagesByPath, bottomPathByBlock, Blocks.SKYROOT_LOG, blocks, "oak_log_top", 88, 174, 59, 54);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SKYROOT_LEAVES, blocks, "leaves_oak", 430, 174, 56, 54);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.COAL_ORE, blocks, "coal_ore", 20, 246, 57, 53);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.TORCH, decor, "torch_wall", 535, 11, 21, 36);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.WILD_GRASS, nature, "tall_grass", 4, 32, 39, 36);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SUN_BLOOM, nature, "sun_bloom", 291, 29, 33, 27);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.IRON_ORE, blocks, "iron_ore", 88, 246, 58, 53);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.COPPER_ORE, blocks, "copper_ore", 157, 245, 59, 54);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CLAY, blocks, "clay", 362, 104, 57, 53);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CACTUS, blocks, "cactus", 564, 174, 55, 53);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.MOSSY_STONE, blocks, "mossy_bricks", 226, 29, 57, 55);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.GRAVEL, blocks, "gravel", 294, 104, 58, 53);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SNOW, blocks, "snow", 430, 104, 57, 53);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.ICE, blocks, "ice", 630, 104, 57, 53);
        putFace(imagesByPath, sidePathByBlock, Blocks.PINE_LOG, blocks, "pine_log_side", 157, 174, 59, 53);
        putFace(imagesByPath, topPathByBlock, Blocks.PINE_LOG, blocks, "pine_log_top", 226, 174, 57, 54);
        putFace(imagesByPath, bottomPathByBlock, Blocks.PINE_LOG, blocks, "pine_log_top", 226, 174, 57, 54);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.PINE_LEAVES, blocks, "leaves_pine", 497, 174, 56, 54);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.RED_MUSHROOM, nature, "red_mushroom", 569, 36, 49, 43);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SKYROOT_PLANKS, blocks, "planks_oak", 497, 317, 57, 51);
    }

    private static void putAllFaces(
            Map<String, BufferedImage> imagesByPath,
            Map<Short, String> sidePathByBlock,
            Map<Short, String> topPathByBlock,
            Map<Short, String> bottomPathByBlock,
            short blockId,
            String sheetPath,
            String name,
            int x,
            int y,
            int width,
            int height
    ) {
        putFace(imagesByPath, sidePathByBlock, blockId, sheetPath, name, x, y, width, height);
        putFace(imagesByPath, topPathByBlock, blockId, sheetPath, name, x, y, width, height);
        putFace(imagesByPath, bottomPathByBlock, blockId, sheetPath, name, x, y, width, height);
    }

    private static void putFace(
            Map<String, BufferedImage> imagesByPath,
            Map<Short, String> faceMap,
            short blockId,
            String sheetPath,
            String name,
            int x,
            int y,
            int width,
            int height
    ) {
        if (faceMap.containsKey(blockId) || !resourceExists(sheetPath)) {
            return;
        }
        String key = "sheet:" + sheetPath + "#" + name + "," + x + "," + y + "," + width + "," + height;
        faceMap.put(blockId, key);
        imagesByPath.computeIfAbsent(key, BlockTextureAtlas::readImage);
    }

    private static void copyBlockUvs(float[] table, Map<Short, String> pathByBlock, Map<String, float[]> uvByPath) {
        for (Map.Entry<Short, String> entry : pathByBlock.entrySet()) {
            float[] uv = uvByPath.get(entry.getValue());
            if (uv == null) {
                continue;
            }
            int offset = entry.getKey() * 4;
            table[offset] = uv[0];
            table[offset + 1] = uv[1];
            table[offset + 2] = uv[2];
            table[offset + 3] = uv[3];
        }
    }

    private static float[] emptyUvTable() {
        float[] values = new float[MAX_BLOCK_ID * 4];
        for (int i = 0; i < MAX_BLOCK_ID; i++) {
            int offset = i * 4;
            values[offset] = -1.0f;
            values[offset + 1] = -1.0f;
            values[offset + 2] = -1.0f;
            values[offset + 3] = -1.0f;
        }
        return values;
    }

    private static ByteBuffer toRgbaBuffer(BufferedImage image) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                buffer.put((byte) ((argb >> 16) & 0xFF));
                buffer.put((byte) ((argb >> 8) & 0xFF));
                buffer.put((byte) (argb & 0xFF));
                buffer.put((byte) ((argb >> 24) & 0xFF));
            }
        }
        buffer.flip();
        return buffer;
    }

    public enum TextureFace {
        SIDE,
        TOP,
        BOTTOM
    }
}
