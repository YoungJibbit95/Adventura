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
        assertTrue(plankSide.contains("assets/game/textures/block/skyroot_planks_side.png"));
        assertTrue(plankSide.contains("assets/game/textures/blocks/skyroot_planks.png"));
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

        assertTrue(grassTop.contains("#grass_top,"));
        assertTrue(grassSide.contains("#grass_side,"));
        assertNotEquals(grassTop, grassSide);
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
