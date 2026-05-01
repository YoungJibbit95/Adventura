package dev.voxelgame.client.render.assets;

import dev.voxelgame.client.render.BlockRenderProperties;
import dev.voxelgame.client.render.RenderMaterial;
import dev.voxelgame.client.render.RenderResourceTracker;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    public static final int MAX_BLOCK_ID = BlockRenderProperties.MATERIAL_INDEX_LIMIT;
    /**
     * Compatibility alias for older callers. UVs now travel through TerrainMaterialLut, not shader uniform arrays.
     */
    @Deprecated
    public static final int SHADER_BLOCK_ID_LIMIT = BlockRenderProperties.MATERIAL_INDEX_LIMIT;
    public static final String BLOCK_TEXTURE_ROOT = "assets/game/textures/block/";
    public static final String BLOCKS_TEXTURE_ROOT = "assets/game/textures/blocks/";
    public static final int TILE_PADDING_PIXELS = 1;
    public static final float UV_INSET_PIXELS = 0.5f;
    public static final String ATLAS_FILTER_MODE = "nearest-no-mip";

    private final int textureId;
    private final boolean enabled;
    private final int loadedTextureCount;
    private final float[] sideUvs;
    private final float[] topUvs;
    private final float[] bottomUvs;
    private final long textureBytes;
    private final AtlasValidationReport validationReport;
    private boolean closed;

    private BlockTextureAtlas(
            int textureId,
            boolean enabled,
            int loadedTextureCount,
            float[] sideUvs,
            float[] topUvs,
            float[] bottomUvs,
            long textureBytes,
            AtlasValidationReport validationReport
    ) {
        this.textureId = textureId;
        this.enabled = enabled;
        this.loadedTextureCount = loadedTextureCount;
        this.sideUvs = sideUvs;
        this.topUvs = topUvs;
        this.bottomUvs = bottomUvs;
        this.textureBytes = textureBytes;
        this.validationReport = validationReport;
    }

    public static BlockTextureAtlas loadDefault() {
        return load(Blocks.createDefaultRegistry());
    }

    public static BlockTextureAtlas load(Registry<BlockType> blocks) {
        TexturePlan plan = texturePlan(blocks);
        Map<Short, String> sidePathByBlock = plan.sidePathByBlock();
        Map<Short, String> topPathByBlock = plan.topPathByBlock();
        Map<Short, String> bottomPathByBlock = plan.bottomPathByBlock();
        AtlasBuild atlasBuild = buildAtlas(plan);
        AtlasValidationReport report = validationReport(blocks, plan, atlasBuild);

        if (atlasBuild.isEmpty()) {
            return new BlockTextureAtlas(0, false, 0, emptyUvTable(), emptyUvTable(), emptyUvTable(), 0L, report);
        }

        float[] sideUvs = emptyUvTable();
        float[] topUvs = emptyUvTable();
        float[] bottomUvs = emptyUvTable();
        copyBlockUvs(sideUvs, sidePathByBlock, atlasBuild.uvByPath());
        copyBlockUvs(topUvs, topPathByBlock, atlasBuild.uvByPath());
        copyBlockUvs(bottomUvs, bottomPathByBlock, atlasBuild.uvByPath());

        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, atlasBuild.layout().atlasWidth(), atlasBuild.layout().atlasHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, toRgbaBuffer(atlasBuild.image()));
        glBindTexture(GL_TEXTURE_2D, 0);

        long textureBytes = (long) atlasBuild.layout().atlasWidth() * atlasBuild.layout().atlasHeight() * 4L;
        RenderResourceTracker.registerTexture(textureBytes);
        return new BlockTextureAtlas(textureId, true, atlasBuild.layout().textureCount(), sideUvs, topUvs, bottomUvs, textureBytes, report);
    }

    public static List<String> missingTextureBlocks(Registry<BlockType> blocks) {
        TexturePlan plan = texturePlan(blocks);
        return missingTextureBlocks(blocks, plan);
    }

    public static AtlasValidationReport validationReport(Registry<BlockType> blocks) {
        TexturePlan plan = texturePlan(blocks);
        return validationReport(blocks, plan, buildAtlas(plan));
    }

    public static AtlasValidationReport writeDebugAtlas(Registry<BlockType> blocks, Path output) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("Debug atlas output path must not be null");
        }
        TexturePlan plan = texturePlan(blocks);
        AtlasBuild atlasBuild = buildAtlas(plan);
        ImageIO.write(atlasBuild.image(), "png", output.toFile());
        return validationReport(blocks, plan, atlasBuild);
    }

    private static List<String> missingTextureBlocks(Registry<BlockType> blocks, TexturePlan plan) {
        List<String> missing = new ArrayList<>();
        for (BlockType block : blocks.values()) {
            if (block.id() <= 0 || block.id() >= MAX_BLOCK_ID) {
                continue;
            }
            if (!plan.sidePathByBlock().containsKey(block.id())
                    || !plan.topPathByBlock().containsKey(block.id())
                    || !plan.bottomPathByBlock().containsKey(block.id())) {
                missing.add(block.key());
            }
        }
        return missing;
    }

    private static AtlasValidationReport validationReport(Registry<BlockType> blocks, TexturePlan plan, AtlasBuild atlasBuild) {
        AtlasLayout layout = atlasBuild.layout();
        RenderMaterial.Table materialTable = RenderMaterial.tableFor(blocks, null);
        return new AtlasValidationReport(
                layout.atlasWidth(),
                layout.atlasHeight(),
                layout.tileContentSize(),
                layout.tileStride(),
                layout.paddingPixels(),
                UV_INSET_PIXELS,
                ATLAS_FILTER_MODE,
                layout.textureCount(),
                materialTable.materialCount(),
                MAX_BLOCK_ID,
                materialTable.missingMaterialCount(),
                (long) layout.atlasWidth() * layout.atlasHeight() * 4L,
                missingTextureBlocks(blocks, plan),
                plan.duplicateMappings(),
                uvRectDebugLines(blocks, plan, atlasBuild.uvByPath())
        );
    }

    private static List<String> uvRectDebugLines(Registry<BlockType> blocks, TexturePlan plan, Map<String, float[]> uvByPath) {
        List<String> lines = new ArrayList<>();
        for (BlockType block : blocks.values()) {
            addUvRectLine(lines, block, TextureFace.SIDE, plan.sidePathByBlock().get(block.id()), uvByPath);
            addUvRectLine(lines, block, TextureFace.TOP, plan.topPathByBlock().get(block.id()), uvByPath);
            addUvRectLine(lines, block, TextureFace.BOTTOM, plan.bottomPathByBlock().get(block.id()), uvByPath);
        }
        return lines;
    }

    private static void addUvRectLine(List<String> lines, BlockType block, TextureFace face, String path, Map<String, float[]> uvByPath) {
        if (path == null) {
            return;
        }
        float[] uv = uvByPath.get(path);
        if (uv == null) {
            return;
        }
        lines.add(block.key() + "." + face.name().toLowerCase(Locale.ROOT) + " " + path + " " + formatUv(uv));
    }

    private static String formatUv(float[] uv) {
        return "[" + decimal(uv[0]) + ", " + decimal(uv[1]) + ", " + decimal(uv[2]) + ", " + decimal(uv[3]) + "]";
    }

    private static String decimal(float value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static AtlasBuild buildAtlas(TexturePlan plan) {
        Map<String, BufferedImage> imagesByPath = plan.imagesByPath();
        if (imagesByPath.isEmpty()) {
            return AtlasBuild.empty();
        }
        int tileContentSize = imagesByPath.values().stream()
                .mapToInt(image -> Math.max(image.getWidth(), image.getHeight()))
                .max()
                .orElse(16);
        AtlasLayout layout = AtlasLayout.forTextureCount(imagesByPath.size(), tileContentSize);
        BufferedImage atlas = new BufferedImage(layout.atlasWidth(), layout.atlasHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        Map<String, float[]> uvByPath = new LinkedHashMap<>();
        int index = 0;
        for (Map.Entry<String, BufferedImage> entry : imagesByPath.entrySet()) {
            int column = index % layout.columns();
            int row = index / layout.columns();
            int x = column * layout.tileStride();
            int y = row * layout.tileStride();
            graphics.drawImage(paddedTile(entry.getValue(), layout.tileContentSize(), layout.paddingPixels()), x, y, null);
            float inset = UV_INSET_PIXELS;
            float contentX = x + layout.paddingPixels();
            float contentY = y + layout.paddingPixels();
            uvByPath.put(entry.getKey(), new float[]{
                    (contentX + inset) / (float) layout.atlasWidth(),
                    1.0f - (contentY + layout.tileContentSize() - inset) / (float) layout.atlasHeight(),
                    (contentX + layout.tileContentSize() - inset) / (float) layout.atlasWidth(),
                    1.0f - (contentY + inset) / (float) layout.atlasHeight()
            });
            index++;
        }
        graphics.dispose();
        return new AtlasBuild(atlas, uvByPath, layout);
    }

    static BufferedImage paddedTile(BufferedImage source, int contentSize, int paddingPixels) {
        int safeContentSize = Math.max(1, contentSize);
        int safePadding = Math.max(0, paddingPixels);
        BufferedImage scaled = new BufferedImage(safeContentSize, safeContentSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.drawImage(source, 0, 0, safeContentSize, safeContentSize, null);
        graphics.dispose();

        int stride = safeContentSize + safePadding * 2;
        BufferedImage padded = new BufferedImage(stride, stride, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < stride; y++) {
            int sourceY = clamp(y - safePadding, 0, safeContentSize - 1);
            for (int x = 0; x < stride; x++) {
                int sourceX = clamp(x - safePadding, 0, safeContentSize - 1);
                padded.setRGB(x, y, scaled.getRGB(sourceX, sourceY));
            }
        }
        return padded;
    }

    private static TexturePlan texturePlan(Registry<BlockType> blocks) {
        Map<String, BufferedImage> imagesByPath = new LinkedHashMap<>();
        Map<Short, String> sidePathByBlock = new LinkedHashMap<>();
        Map<Short, String> topPathByBlock = new LinkedHashMap<>();
        Map<Short, String> bottomPathByBlock = new LinkedHashMap<>();
        List<String> duplicateMappings = new ArrayList<>();

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
        registerBundledSheetFallbacks(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, duplicateMappings);
        return new TexturePlan(imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, duplicateMappings);
    }

    public boolean enabled() {
        return enabled;
    }

    public int loadedTextureCount() {
        return loadedTextureCount;
    }

    public AtlasValidationReport validationReport() {
        return validationReport;
    }

    public int materialCount() {
        return sideUvs.length / 4;
    }

    public float[] sideUvs() {
        return sideUvs.clone();
    }

    public float[] topUvs() {
        return topUvs.clone();
    }

    public float[] bottomUvs() {
        return bottomUvs.clone();
    }

    public float[] uvRect(short blockId, TextureFace face) {
        if (blockId < 0 || blockId >= materialCount()) {
            return new float[]{RenderMaterial.MISSING_UV, RenderMaterial.MISSING_UV, RenderMaterial.MISSING_UV, RenderMaterial.MISSING_UV};
        }
        float[] source = switch (face) {
            case SIDE -> sideUvs;
            case TOP -> topUvs;
            case BOTTOM -> bottomUvs;
        };
        int offset = blockId * 4;
        return new float[]{source[offset], source[offset + 1], source[offset + 2], source[offset + 3]};
    }

    public void bindAndApply(ShaderProgram shader, int unit) {
        shader.setInt("uAtlasEnabled", enabled ? 1 : 0);
        shader.setInt("uBlockAtlas", unit);
        if (enabled) {
            glActiveTexture(GL_TEXTURE0 + unit);
            glBindTexture(GL_TEXTURE_2D, textureId);
        }
    }

    @Override
    public void close() {
        if (!closed && textureId != 0) {
            closed = true;
            glDeleteTextures(textureId);
            RenderResourceTracker.releaseTexture(textureBytes);
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
            Map<Short, String> bottomPathByBlock,
            List<String> duplicateMappings
    ) {
        String blocks = "assets/game/blocks_tiles_sheet.png";
        String ores = "assets/game/ores_materials_sheet.png";
        String ui = "assets/game/ui_hud_sheet.png";
        String food = "assets/game/nature_food_sheet.png";

        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.FLOWER_POT, food, "flower_pot", 1137, 132, 126, 98);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.COOKING_POT, food, "cooking_pot", 1137, 132, 126, 98);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.LANTERN, food, "lantern", 1170, 854, 82, 86);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.ANCIENT_LANTERN, food, "ancient_lantern", 1170, 854, 82, 86);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CAMPFIRE, ui, "campfire", 1007, 35, 76, 78);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CAMPFIRE_ACTIVE, ui, "campfire_active", 1007, 35, 76, 78);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CAMPFIRE_BURNED_OUT, ui, "campfire_burned_out", 611, 36, 70, 74);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.STORAGE_CRATE, ui, "crate", 714, 142, 70, 66);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SMALL_TABLE, ui, "small_table", 714, 142, 70, 66);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.WORKBENCH, ui, "workbench", 714, 142, 70, 66);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.FORGE, ores, "forge", 1306, 147, 130, 132);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.WOODEN_CHAIR, ui, "wooden_chair", 615, 141, 72, 68);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.WOVEN_RUG, ui, "woven_rug", 33, 870, 182, 80);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SLEEPING_MAT, ui, "sleeping_mat", 33, 870, 182, 80);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.GARDEN_FENCE, ui, "garden_fence", 33, 870, 182, 80);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.BERRY_BUSH, food, "berry_bush", 976, 855, 110, 68);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.HERB_PLANTER, food, "herbs", 1170, 677, 86, 84);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.TREE_STUMP, blocks, "tree_stump", 29, 259, 150, 158);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.MUSHROOM_CLUSTER, food, "mushroom_cluster", 958, 132, 110, 94);

        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.STONE, blocks, "stone", 576, 50, 150, 158);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.DIRT, blocks, "dirt", 211, 50, 150, 158);
        putFullFace(duplicateMappings, imagesByPath, topPathByBlock, Blocks.GRASS, blocks, "grass_top", 1123, 259, 150, 158);
        putFullFace(duplicateMappings, imagesByPath, sidePathByBlock, Blocks.GRASS, blocks, "grass_side", 29, 49, 150, 158);
        putFullFace(duplicateMappings, imagesByPath, bottomPathByBlock, Blocks.GRASS, blocks, "dirt", 211, 50, 150, 158);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.WATER, blocks, "water", 211, 469, 150, 158);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SAND, blocks, "sand", 1123, 50, 150, 158);
        putFullFace(duplicateMappings, imagesByPath, sidePathByBlock, Blocks.SKYROOT_LOG, blocks, "skyroot_log_side", 211, 259, 150, 158);
        putFullFace(duplicateMappings, imagesByPath, topPathByBlock, Blocks.SKYROOT_LOG, blocks, "skyroot_log_top", 29, 259, 150, 158);
        putFullFace(duplicateMappings, imagesByPath, bottomPathByBlock, Blocks.SKYROOT_LOG, blocks, "skyroot_log_top", 29, 259, 150, 158);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SKYROOT_LEAVES, blocks, "skyroot_leaves", 758, 259, 150, 158);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.COAL_ORE, ores, "coal_ore", 742, 146, 130, 132);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.TORCH, food, "torch", 1170, 854, 82, 86);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.WILD_GRASS, food, "wild_grass", 779, 493, 112, 82);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.REEDS, food, "reeds", 643, 683, 107, 67);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.TWIG_PILE, food, "twig_pile", 218, 496, 106, 63);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SMALL_STONE, food, "small_stone", 374, 499, 99, 65);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SUN_BLOOM, blocks, "sun_bloom", 1122, 259, 150, 158);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.IRON_ORE, ores, "iron_ore", 377, 146, 130, 132);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.COPPER_ORE, ores, "copper_ore", 46, 146, 130, 132);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CLAY, blocks, "clay", 394, 50, 150, 158);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CLAY_DEPOSIT, ores, "clay_deposit", 1306, 147, 130, 132);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.CACTUS, blocks, "cactus", 1306, 259, 150, 158);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.MOSSY_STONE, blocks, "mossy_stone", 758, 50, 150, 158);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.MOSSY_PATH, blocks, "mossy_path", 941, 469, 150, 158);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.GRAVEL, blocks, "gravel", 1306, 50, 150, 158);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SNOW, blocks, "snow", 29, 469, 150, 158);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.ICE, blocks, "ice", 575, 681, 150, 158);
        putFullFace(duplicateMappings, imagesByPath, sidePathByBlock, Blocks.PINE_LOG, blocks, "pine_log_side", 576, 259, 150, 158);
        putFullFace(duplicateMappings, imagesByPath, topPathByBlock, Blocks.PINE_LOG, blocks, "pine_log_top", 394, 259, 150, 158);
        putFullFace(duplicateMappings, imagesByPath, bottomPathByBlock, Blocks.PINE_LOG, blocks, "pine_log_top", 394, 259, 150, 158);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.PINE_LEAVES, blocks, "pine_leaves", 941, 259, 150, 158);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.RED_MUSHROOM, food, "red_mushroom", 812, 132, 110, 94);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.GLOW_MUSHROOM, ores, "glow_mushroom", 961, 519, 132, 116);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SPORE_BLOSSOM, food, "spore_blossom", 982, 678, 106, 70);
        putAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.GLOW_CRYSTAL_NODE, ores, "glow_crystal_node", 1028, 147, 133, 130);
        putFullFaceAllFaces(duplicateMappings, imagesByPath, sidePathByBlock, topPathByBlock, bottomPathByBlock, Blocks.SKYROOT_PLANKS, blocks, "skyroot_planks", 576, 469, 150, 158);
    }

    private static void putAllFaces(
            List<String> duplicateMappings,
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
        putFace(duplicateMappings, imagesByPath, sidePathByBlock, blockId, sheetPath, name, x, y, width, height);
        putFace(duplicateMappings, imagesByPath, topPathByBlock, blockId, sheetPath, name, x, y, width, height);
        putFace(duplicateMappings, imagesByPath, bottomPathByBlock, blockId, sheetPath, name, x, y, width, height);
    }

    private static void putFullFaceAllFaces(
            List<String> duplicateMappings,
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
        putFullFace(duplicateMappings, imagesByPath, sidePathByBlock, blockId, sheetPath, name, x, y, width, height);
        putFullFace(duplicateMappings, imagesByPath, topPathByBlock, blockId, sheetPath, name, x, y, width, height);
        putFullFace(duplicateMappings, imagesByPath, bottomPathByBlock, blockId, sheetPath, name, x, y, width, height);
    }

    private static void putFace(
            List<String> duplicateMappings,
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
        if (!resourceExists(sheetPath)) {
            return;
        }
        String key = "sheet:" + sheetPath + "#" + name + "," + x + "," + y + "," + width + "," + height;
        if (faceMap.containsKey(blockId)) {
            recordDuplicateMapping(duplicateMappings, blockId, faceMap.get(blockId), key);
            return;
        }
        faceMap.put(blockId, key);
        imagesByPath.computeIfAbsent(key, BlockTextureAtlas::readImage);
    }

    private static void putFullFace(
            List<String> duplicateMappings,
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
        if (!resourceExists(sheetPath)) {
            return;
        }
        String key = "sheet-full:" + sheetPath + "#" + name + "," + x + "," + y + "," + width + "," + height;
        if (faceMap.containsKey(blockId)) {
            recordDuplicateMapping(duplicateMappings, blockId, faceMap.get(blockId), key);
            return;
        }
        faceMap.put(blockId, key);
        imagesByPath.computeIfAbsent(key, BlockTextureAtlas::readImage);
    }

    private static void recordDuplicateMapping(List<String> duplicateMappings, short blockId, String existing, String skipped) {
        if (duplicateMappings == null || existing == null || existing.equals(skipped)) {
            return;
        }
        duplicateMappings.add("block " + blockId + " keeps " + existing + ", skipped " + skipped);
    }

    private static void copyBlockUvs(float[] table, Map<Short, String> pathByBlock, Map<String, float[]> uvByPath) {
        for (Map.Entry<Short, String> entry : pathByBlock.entrySet()) {
            float[] uv = uvByPath.get(entry.getValue());
            if (uv == null) {
                continue;
            }
            short blockId = entry.getKey();
            if (blockId < 0 || blockId >= MAX_BLOCK_ID) {
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
        float[] values = new float[MAX_BLOCK_ID * 4];
        for (int i = 0; i < MAX_BLOCK_ID; i++) {
            int offset = i * 4;
            values[offset] = RenderMaterial.MISSING_UV;
            values[offset + 1] = RenderMaterial.MISSING_UV;
            values[offset + 2] = RenderMaterial.MISSING_UV;
            values[offset + 3] = RenderMaterial.MISSING_UV;
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

    public record AtlasValidationReport(
            int atlasWidth,
            int atlasHeight,
            int tileContentSize,
            int tileStride,
            int tilePaddingPixels,
            float uvInsetPixels,
            String filterMode,
            int textureCount,
            int materialCount,
            int materialCapacity,
            int missingMaterialCount,
            long estimatedBytes,
            List<String> missingTextures,
            List<String> duplicateMappings,
            List<String> uvRectDebugLines
    ) {
        public AtlasValidationReport {
            atlasWidth = Math.max(0, atlasWidth);
            atlasHeight = Math.max(0, atlasHeight);
            tileContentSize = Math.max(0, tileContentSize);
            tileStride = Math.max(0, tileStride);
            tilePaddingPixels = Math.max(0, tilePaddingPixels);
            uvInsetPixels = Float.isFinite(uvInsetPixels) ? Math.max(0.0f, uvInsetPixels) : 0.0f;
            filterMode = filterMode == null ? ATLAS_FILTER_MODE : filterMode;
            textureCount = Math.max(0, textureCount);
            materialCount = Math.max(0, materialCount);
            materialCapacity = Math.max(0, materialCapacity);
            missingMaterialCount = Math.max(0, missingMaterialCount);
            estimatedBytes = Math.max(0L, estimatedBytes);
            missingTextures = missingTextures == null ? List.of() : List.copyOf(missingTextures);
            duplicateMappings = duplicateMappings == null ? List.of() : List.copyOf(duplicateMappings);
            uvRectDebugLines = uvRectDebugLines == null ? List.of() : List.copyOf(uvRectDebugLines);
        }

        public boolean hasErrors() {
            return !missingTextures.isEmpty() || !duplicateMappings.isEmpty();
        }

        public boolean hasWarnings() {
            return missingMaterialCount > 0;
        }

        public String summary() {
            return textureCount + " texture(s), " + materialCount + "/" + materialCapacity
                    + " material slot(s), atlas " + atlasWidth + "x" + atlasHeight
                    + ", tile " + tileContentSize + "+" + tilePaddingPixels + "px padding"
                    + ", filter " + filterMode;
        }
    }

    private record AtlasLayout(
            int tileContentSize,
            int tileStride,
            int paddingPixels,
            int columns,
            int rows,
            int atlasWidth,
            int atlasHeight,
            int textureCount
    ) {
        static AtlasLayout empty() {
            return new AtlasLayout(0, 0, 0, 0, 0, 0, 0, 0);
        }

        static AtlasLayout forTextureCount(int textureCount, int tileContentSize) {
            int safeTextureCount = Math.max(0, textureCount);
            if (safeTextureCount == 0) {
                return empty();
            }
            int safeTileContentSize = Math.max(1, tileContentSize);
            int tileStride = safeTileContentSize + TILE_PADDING_PIXELS * 2;
            int columns = (int) Math.ceil(Math.sqrt(safeTextureCount));
            int rows = (int) Math.ceil(safeTextureCount / (double) columns);
            return new AtlasLayout(
                    safeTileContentSize,
                    tileStride,
                    TILE_PADDING_PIXELS,
                    columns,
                    rows,
                    columns * tileStride,
                    rows * tileStride,
                    safeTextureCount
            );
        }
    }

    private record AtlasBuild(BufferedImage image, Map<String, float[]> uvByPath, AtlasLayout layout) {
        static AtlasBuild empty() {
            return new AtlasBuild(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), Map.of(), AtlasLayout.empty());
        }

        boolean isEmpty() {
            return layout.textureCount() == 0;
        }
    }

    private record TexturePlan(
            Map<String, BufferedImage> imagesByPath,
            Map<Short, String> sidePathByBlock,
            Map<Short, String> topPathByBlock,
            Map<Short, String> bottomPathByBlock,
            List<String> duplicateMappings
    ) {
        private TexturePlan {
            duplicateMappings = duplicateMappings == null ? List.of() : List.copyOf(duplicateMappings);
        }
    }
}
