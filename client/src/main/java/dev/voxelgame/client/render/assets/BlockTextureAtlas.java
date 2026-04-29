package dev.voxelgame.client.render.assets;

import dev.voxelgame.client.render.BlockRenderProperties;
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
    public static final int SHADER_BLOCK_ID_LIMIT = BlockRenderProperties.SHADER_BLOCK_ID_LIMIT;
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
            float inset = 0.5f;
            uvByPath.put(entry.getKey(), new float[]{
                    (x + inset) / (float) atlasWidth,
                    1.0f - (y + tileSize - inset) / (float) atlasHeight,
                    (x + tileSize - inset) / (float) atlasWidth,
                    1.0f - (y + inset) / (float) atlasHeight
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
        if ((path.startsWith("sheet:") || path.startsWith("sheet-full:")) && sliceMarker > 0) {
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
        boolean fullFace = key.startsWith("sheet-full:");
        String prefix = fullFace ? "sheet-full:" : "sheet:";
        String sheetPath = key.substring(prefix.length(), marker);
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
            BufferedImage slice = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = slice.createGraphics();
            graphics.drawImage(sheet, 0, 0, width, height, x, y, x + width, y + height, null);
            graphics.dispose();
            if (sheetPath.contains("generated_")
                    || sheetPath.contains("ui_hud_sheet")
                    || sheetPath.contains("blocks_tiles_sheet")
                    || sheetPath.contains("ores_materials_sheet")
                    || sheetPath.contains("tools_weapons_sheet")) {
                removeEdgeCheckerBackground(slice);
            }
            slice = trimTransparentPadding(slice);
            if (fullFace) {
                slice = fillTransparentPixels(slice);
            }
            return slice;
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
        String blocks = "assets/game/blocks_tiles_sheet.png";
        String ores = "assets/game/ores_materials_sheet.png";
        String ui = "assets/game/ui_hud_sheet.png";
        String food = "assets/game/nature_food_sheet.png";

        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.FLOWER_POT, food, "flower_pot", 1137, 132, 126, 98);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.LANTERN, food, "lantern", 1170, 854, 82, 86);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CAMPFIRE, ui, "campfire", 1007, 35, 76, 78);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CAMPFIRE_ACTIVE, ui, "campfire_active", 1007, 35, 76, 78);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CAMPFIRE_BURNED_OUT, ui, "campfire_burned_out", 611, 36, 70, 74);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.STORAGE_CRATE, ui, "crate", 714, 142, 70, 66);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SMALL_TABLE, ui, "small_table", 714, 142, 70, 66);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.WOODEN_CHAIR, ui, "wooden_chair", 615, 141, 72, 68);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.WOVEN_RUG, ui, "woven_rug", 33, 870, 182, 80);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.GARDEN_FENCE, ui, "garden_fence", 33, 870, 182, 80);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.BERRY_BUSH, food, "berry_bush", 976, 855, 110, 68);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.HERB_PLANTER, food, "herbs", 1170, 677, 86, 84);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.TREE_STUMP, blocks, "tree_stump", 29, 259, 150, 158);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.MUSHROOM_CLUSTER, food, "mushroom_cluster", 958, 132, 110, 94);

        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.STONE, blocks, "stone", 576, 50, 150, 158);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.DIRT, blocks, "dirt", 211, 50, 150, 158);
        putFullFace(imagesByPath, topPathByBlock, Blocks.GRASS, blocks, "grass_top", 29, 49, 150, 158);
        putFullFace(imagesByPath, sidePathByBlock, Blocks.GRASS, blocks, "grass_side", 29, 49, 150, 158);
        putFullFace(imagesByPath, bottomPathByBlock, Blocks.GRASS, blocks, "dirt", 211, 50, 150, 158);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.WATER, blocks, "water", 211, 469, 150, 158);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SAND, blocks, "sand", 1123, 50, 150, 158);
        putFullFace(imagesByPath, sidePathByBlock, Blocks.SKYROOT_LOG, blocks, "skyroot_log_side", 211, 259, 150, 158);
        putFullFace(imagesByPath, topPathByBlock, Blocks.SKYROOT_LOG, blocks, "skyroot_log_top", 29, 259, 150, 158);
        putFullFace(imagesByPath, bottomPathByBlock, Blocks.SKYROOT_LOG, blocks, "skyroot_log_top", 29, 259, 150, 158);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SKYROOT_LEAVES, blocks, "skyroot_leaves", 758, 259, 150, 158);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.COAL_ORE, ores, "coal_ore", 742, 146, 130, 132);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.TORCH, food, "torch", 1170, 854, 82, 86);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.WILD_GRASS, food, "wild_grass", 779, 493, 112, 82);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SUN_BLOOM, blocks, "sun_bloom", 1122, 259, 150, 158);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.IRON_ORE, ores, "iron_ore", 377, 146, 130, 132);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.COPPER_ORE, ores, "copper_ore", 46, 146, 130, 132);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CLAY, blocks, "clay", 394, 50, 150, 158);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CLAY_DEPOSIT, ores, "clay_deposit", 1306, 147, 130, 132);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CACTUS, blocks, "cactus", 1306, 259, 150, 158);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.MOSSY_STONE, blocks, "mossy_stone", 758, 50, 150, 158);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.GRAVEL, blocks, "gravel", 1306, 50, 150, 158);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SNOW, blocks, "snow", 29, 469, 150, 158);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.ICE, blocks, "ice", 575, 681, 150, 158);
        putFullFace(imagesByPath, sidePathByBlock, Blocks.PINE_LOG, blocks, "pine_log_side", 576, 259, 150, 158);
        putFullFace(imagesByPath, topPathByBlock, Blocks.PINE_LOG, blocks, "pine_log_top", 394, 259, 150, 158);
        putFullFace(imagesByPath, bottomPathByBlock, Blocks.PINE_LOG, blocks, "pine_log_top", 394, 259, 150, 158);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.PINE_LEAVES, blocks, "pine_leaves", 941, 259, 150, 158);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.RED_MUSHROOM, food, "red_mushroom", 812, 132, 110, 94);
        putAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.GLOW_CRYSTAL_NODE, ores, "glow_crystal_node", 1028, 147, 133, 130);
        putFullFaceAllFaces(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SKYROOT_PLANKS, blocks, "skyroot_planks", 576, 469, 150, 158);
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

    private static void putFullFaceAllFaces(
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
        putFullFace(imagesByPath, sidePathByBlock, blockId, sheetPath, name, x, y, width, height);
        putFullFace(imagesByPath, topPathByBlock, blockId, sheetPath, name, x, y, width, height);
        putFullFace(imagesByPath, bottomPathByBlock, blockId, sheetPath, name, x, y, width, height);
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

    private static void putFullFace(
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
        String key = "sheet-full:" + sheetPath + "#" + name + "," + x + "," + y + "," + width + "," + height;
        faceMap.put(blockId, key);
        imagesByPath.computeIfAbsent(key, BlockTextureAtlas::readImage);
    }

    private static void copyBlockUvs(float[] table, Map<Short, String> pathByBlock, Map<String, float[]> uvByPath) {
        for (Map.Entry<Short, String> entry : pathByBlock.entrySet()) {
            float[] uv = uvByPath.get(entry.getValue());
            if (uv == null) {
                continue;
            }
            short blockId = entry.getKey();
            if (blockId < 0 || blockId >= SHADER_BLOCK_ID_LIMIT) {
                continue;
            }
            int offset = blockId * 4;
            table[offset] = uv[0];
            table[offset + 1] = uv[1];
            table[offset + 2] = uv[2];
            table[offset + 3] = uv[3];
        }
    }

    private static float[] emptyUvTable() {
        float[] values = new float[SHADER_BLOCK_ID_LIMIT * 4];
        for (int i = 0; i < SHADER_BLOCK_ID_LIMIT; i++) {
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

    static BufferedImage trimTransparentPadding(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (alpha(image.getRGB(x, y)) <= 8) {
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

    static BufferedImage fillTransparentPixels(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        int transparent = countTransparent(pixels);
        while (transparent > 0) {
            int[] next = pixels.clone();
            int filled = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    if (alpha(pixels[index]) > 8) {
                        continue;
                    }
                    int neighbor = opaqueNeighbor(pixels, width, height, x, y);
                    if (neighbor == 0) {
                        continue;
                    }
                    next[index] = 0xFF000000 | (neighbor & 0x00FFFFFF);
                    filled++;
                }
            }
            if (filled == 0) {
                break;
            }
            pixels = next;
            transparent -= filled;
        }
        BufferedImage filledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        filledImage.setRGB(0, 0, width, height, pixels, 0, width);
        return filledImage;
    }

    private static int countTransparent(int[] pixels) {
        int count = 0;
        for (int pixel : pixels) {
            if (alpha(pixel) <= 8) {
                count++;
            }
        }
        return count;
    }

    private static int opaqueNeighbor(int[] pixels, int width, int height, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            int ny = y + dy;
            if (ny < 0 || ny >= height) {
                continue;
            }
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx;
                if (nx < 0 || nx >= width) {
                    continue;
                }
                int pixel = pixels[ny * width + nx];
                if (alpha(pixel) > 8) {
                    return pixel;
                }
            }
        }
        return 0;
    }

    private static int alpha(int argb) {
        return (argb >> 24) & 0xFF;
    }

    private static void removeEdgeCheckerBackground(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[] background = new boolean[width * height];
        int[] queue = new int[width * height];
        int head = 0;
        int tail = 0;
        for (int x = 0; x < width; x++) {
            tail = enqueueNeutral(image, background, queue, tail, x, 0);
            tail = enqueueNeutral(image, background, queue, tail, x, height - 1);
        }
        for (int y = 1; y < height - 1; y++) {
            tail = enqueueNeutral(image, background, queue, tail, 0, y);
            tail = enqueueNeutral(image, background, queue, tail, width - 1, y);
        }
        while (head < tail) {
            int index = queue[head++];
            int x = index % width;
            int y = index / width;
            if (x > 0) {
                tail = enqueueNeutral(image, background, queue, tail, x - 1, y);
            }
            if (x + 1 < width) {
                tail = enqueueNeutral(image, background, queue, tail, x + 1, y);
            }
            if (y > 0) {
                tail = enqueueNeutral(image, background, queue, tail, x, y - 1);
            }
            if (y + 1 < height) {
                tail = enqueueNeutral(image, background, queue, tail, x, y + 1);
            }
        }
        for (int i = 0; i < background.length; i++) {
            if (background[i]) {
                int x = i % width;
                int y = i / width;
                image.setRGB(x, y, image.getRGB(x, y) & 0x00FFFFFF);
            }
        }
    }

    private static int enqueueNeutral(BufferedImage image, boolean[] background, int[] queue, int tail, int x, int y) {
        int index = y * image.getWidth() + x;
        if (background[index] || !isNeutralBackground(image.getRGB(x, y))) {
            return tail;
        }
        background[index] = true;
        queue[tail] = index;
        return tail + 1;
    }

    private static boolean isNeutralBackground(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return a <= 8 || min >= 190 && max - min <= 20;
    }

    public enum TextureFace {
        SIDE,
        TOP,
        BOTTOM
    }
}
