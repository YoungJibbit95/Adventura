package dev.voxelgame.client.render.assets;

import dev.voxelgame.common.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockTextureAtlasTest {
    @Test
    void exposesStableBlockTextureCandidatePaths() {
        List<String> grassTop = BlockTextureAtlas.textureCandidates("voxel:grass_block", BlockTextureAtlas.TextureFace.TOP);
        List<String> plankSide = BlockTextureAtlas.textureCandidates("voxel:skyroot_planks", BlockTextureAtlas.TextureFace.SIDE);

        assertTrue(grassTop.contains("assets/game/textures/block/grass_block_top.png"));
        assertTrue(grassTop.contains("assets/game/textures/block/grass_top.png"));
        assertTrue(grassTop.contains("assets/game/blocks/grass_block_top.png"));
        assertTrue(grassTop.contains("assets/game/grass_block_top.png"));
        assertTrue(plankSide.contains("assets/game/textures/block/skyroot_planks_side.png"));
        assertTrue(plankSide.contains("assets/game/textures/blocks/skyroot_planks.png"));
    }

    @Test
    void exposesAliasCandidatesForCurrentAssetDropNames() {
        List<String> skyrootSide = BlockTextureAtlas.textureCandidates("voxel:skyroot_log", BlockTextureAtlas.TextureFace.SIDE);
        List<String> pineTop = BlockTextureAtlas.textureCandidates("voxel:pine_log", BlockTextureAtlas.TextureFace.TOP);
        List<String> mossyPathTop = BlockTextureAtlas.textureCandidates("voxel:mossy_path", BlockTextureAtlas.TextureFace.TOP);
        List<String> iceSide = BlockTextureAtlas.textureCandidates("voxel:ice", BlockTextureAtlas.TextureFace.SIDE);
        List<String> planksSide = BlockTextureAtlas.textureCandidates("voxel:skyroot_planks", BlockTextureAtlas.TextureFace.SIDE);
        List<String> pinePlanksSide = BlockTextureAtlas.textureCandidates("voxel:pine_planks", BlockTextureAtlas.TextureFace.SIDE);
        List<String> snowyGrassBottom = BlockTextureAtlas.textureCandidates("voxel:snowy_grass_block", BlockTextureAtlas.TextureFace.BOTTOM);
        List<String> stoneBricksSide = BlockTextureAtlas.textureCandidates("voxel:stone_bricks", BlockTextureAtlas.TextureFace.SIDE);
        List<String> glassSide = BlockTextureAtlas.textureCandidates("voxel:glass", BlockTextureAtlas.TextureFace.SIDE);

        assertTrue(skyrootSide.contains("assets/game/oak_log_side.png"));
        assertTrue(skyrootSide.contains("assets/game/blocks/oak_log_side.png"));
        assertTrue(pineTop.contains("assets/game/spruce_log_top.png"));
        assertTrue(mossyPathTop.contains("assets/game/mossy_grass_top.png"));
        assertTrue(iceSide.contains("assets/game/ice_block.png"));
        assertTrue(planksSide.contains("assets/game/oak_planks.png"));
        assertTrue(pinePlanksSide.contains("assets/game/spruce_planks.png"));
        assertTrue(snowyGrassBottom.contains("assets/game/dirt.png"));
        assertTrue(stoneBricksSide.contains("assets/game/stone_brick_block.png"));
        assertTrue(glassSide.contains("assets/game/blocks/glass_block.png"));
    }

    @Test
    void trimsTransparentPaddingFromSheetSlices() {
        BufferedImage image = new BufferedImage(6, 5, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(2, 1, 0xFFAA5500);
        image.setRGB(3, 2, 0xFFAA5500);
        image.setRGB(4, 3, 0xFFAA5500);

        BufferedImage trimmed = BlockTextureAtlas.trimTransparentPadding(image);

        assertEquals(3, trimmed.getWidth());
        assertEquals(3, trimmed.getHeight());
        assertEquals(0xFFAA5500, trimmed.getRGB(0, 0));
        assertEquals(0xFFAA5500, trimmed.getRGB(2, 2));
    }

    @Test
    void fillsTransparentPixelsForFullBlockFaces() {
        BufferedImage image = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(1, 1, 0xFF336699);

        BufferedImage filled = BlockTextureAtlas.fillTransparentPixels(image);

        for (int y = 0; y < filled.getHeight(); y++) {
            for (int x = 0; x < filled.getWidth(); x++) {
                assertEquals(0xFF336699, filled.getRGB(x, y));
            }
        }
    }

    @Test
    void removesConnectedNeutralEdgeBackgroundFromIndividualTextures() {
        BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, 0xFFFFFFFF);
            }
        }
        for (int y = 1; y <= 3; y++) {
            for (int x = 1; x <= 3; x++) {
                image.setRGB(x, y, 0xFF884422);
            }
        }
        image.setRGB(2, 2, 0xFFFFFFFF);

        BufferedImage cleaned = BlockTextureAtlas.sanitizeIndividualTexture(image);

        assertEquals(3, cleaned.getWidth());
        assertEquals(3, cleaned.getHeight());
        assertEquals(0xFF884422, cleaned.getRGB(0, 0));
        assertEquals(0xFFFFFFFF, cleaned.getRGB(1, 1));
        assertEquals(0xFF884422, cleaned.getRGB(2, 2));
    }

    @Test
    void reportsNoMissingTextureMappingsForRegisteredBlocks() {
        List<String> missing = BlockTextureAtlas.missingTextureBlocks(Blocks.createDefaultRegistry());

        assertTrue(missing.isEmpty(), "Missing block texture mappings: " + missing);
    }

    @Test
    void validationReportExposesAtlasMetadataAndUvDebugLines() {
        BlockTextureAtlas.AtlasValidationReport report = BlockTextureAtlas.validationReport(Blocks.createDefaultRegistry());

        assertFalse(report.hasErrors(), "Atlas validation errors: " + report.summary());
        assertFalse(report.hasWarnings(), "Atlas validation warnings: " + report.missingMaterialCount());
        assertTrue(report.atlasWidth() > 0);
        assertTrue(report.atlasHeight() > 0);
        assertEquals(BlockTextureAtlas.TILE_PADDING_PIXELS, report.tilePaddingPixels());
        assertEquals(BlockTextureAtlas.UV_INSET_PIXELS, report.uvInsetPixels(), 0.0001f);
        assertEquals(BlockTextureAtlas.ATLAS_FILTER_MODE, report.filterMode());
        assertEquals(BlockTextureAtlas.INDIVIDUAL_ASSET_EDGE_CLEANUP_MODE, report.edgeCleanupMode());
        assertTrue(report.tileContentSize() <= BlockTextureAtlas.MAX_TILE_CONTENT_SIZE);
        assertTrue(report.textureCount() > 0);
        assertTrue(report.materialCount() > Blocks.LAVA);
        assertEquals(BlockTextureAtlas.MAX_BLOCK_ID, report.materialCapacity());
        assertEquals(0, report.missingMaterialCount());
        assertTrue(report.estimatedBytes() > 0L);
        assertTrue(report.duplicateMappings().isEmpty(), "Duplicate atlas mappings: " + report.duplicateMappings());
        assertFalse(report.uvRectDebugLines().isEmpty());
        assertTrue(report.uvRectDebugLines().stream().anyMatch(line -> line.startsWith("voxel:stone.side ")));
        assertTrue(report.summary().contains("atlas"));
    }

    @Test
    void usesDifferentGrassTopAndSideAtlasSlices() {
        BlockTextureAtlas.AtlasValidationReport report = BlockTextureAtlas.validationReport(Blocks.createDefaultRegistry());
        String grassTop = report.uvRectDebugLines().stream()
                .filter(line -> line.startsWith("voxel:grass_block.top "))
                .findFirst()
                .orElseThrow();
        String grassSide = report.uvRectDebugLines().stream()
                .filter(line -> line.startsWith("voxel:grass_block.side "))
                .findFirst()
                .orElseThrow();

        assertTrue(grassTop.contains("grass_top") || grassTop.contains("grass_block_top"));
        assertTrue(grassSide.contains("grass_side") || grassSide.contains("grass_block_side"));
        assertNotEquals(grassTop, grassSide);
    }

    @Test
    void usesAvailableFlatAssetDropTexturesBeforeBundledSheetFallbacks() {
        BlockTextureAtlas.AtlasValidationReport report = BlockTextureAtlas.validationReport(Blocks.createDefaultRegistry());

        assertTrue(report.uvRectDebugLines().stream().anyMatch(line ->
                line.startsWith("voxel:coal_ore.side assets/game/blocks/coal_ore.png ")));
        assertTrue(report.uvRectDebugLines().stream().anyMatch(line ->
                line.startsWith("voxel:copper_ore.side assets/game/blocks/copper_ore.png ")));
        assertTrue(report.uvRectDebugLines().stream().anyMatch(line ->
                line.startsWith("voxel:iron_ore.side assets/game/blocks/iron_ore.png ")));
        assertTrue(report.uvRectDebugLines().stream().anyMatch(line ->
                line.startsWith("voxel:gold_ore.side assets/game/blocks/gold_ore.png ")));
        assertTrue(report.uvRectDebugLines().stream().anyMatch(line ->
                line.startsWith("voxel:platin_ore.side assets/game/blocks/platin_ore.png ")));
        assertTrue(report.uvRectDebugLines().stream().anyMatch(line ->
                line.startsWith("voxel:ruby_ore.side assets/game/blocks/ruby_ore.png ")));
        assertTrue(report.uvRectDebugLines().stream().anyMatch(line ->
                line.startsWith("voxel:sapphire_ore.side assets/game/blocks/sapphire_ore.png ")));
        assertTrue(report.uvRectDebugLines().stream().anyMatch(line ->
                line.startsWith("voxel:titan_ore.side assets/game/blocks/titan_ore.png ")));
    }

    @Test
    void paddedTilesExtrudeOuterPixelsIntoPadding() {
        BufferedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xFFFF0000);
        source.setRGB(1, 0, 0xFF00FF00);
        source.setRGB(0, 1, 0xFF0000FF);
        source.setRGB(1, 1, 0xFFFFFFFF);

        BufferedImage padded = BlockTextureAtlas.paddedTile(source, 2, 1);

        assertEquals(4, padded.getWidth());
        assertEquals(4, padded.getHeight());
        assertEquals(0xFFFF0000, padded.getRGB(0, 0));
        assertEquals(0xFF00FF00, padded.getRGB(3, 0));
        assertEquals(0xFF0000FF, padded.getRGB(0, 3));
        assertEquals(0xFFFFFFFF, padded.getRGB(3, 3));
        assertEquals(0xFFFF0000, padded.getRGB(1, 1));
        assertEquals(0xFFFFFFFF, padded.getRGB(2, 2));
    }

    @Test
    void canWriteDebugAtlasPng(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("block-atlas.png");

        BlockTextureAtlas.AtlasValidationReport report = BlockTextureAtlas.writeDebugAtlas(Blocks.createDefaultRegistry(), output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0L);
        assertFalse(report.uvRectDebugLines().isEmpty());
    }
}
