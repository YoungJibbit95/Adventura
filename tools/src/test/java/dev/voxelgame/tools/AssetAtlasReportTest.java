package dev.voxelgame.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetAtlasReportTest {
    @TempDir
    Path tempDir;

    @Test
    void exposesStableBlockTextureCandidatePaths() {
        List<String> grassTop = AssetAtlasReport.textureCandidates("voxel:grass_block", AssetAtlasReport.TextureFace.TOP);
        List<String> plankSide = AssetAtlasReport.textureCandidates("voxel:skyroot_planks", AssetAtlasReport.TextureFace.SIDE);

        assertTrue(grassTop.contains("textures/block/grass_block_top.png"));
        assertTrue(grassTop.contains("textures/block/grass_top.png"));
        assertTrue(grassTop.contains("blocks/grass_block_top.png"));
        assertTrue(grassTop.contains("grass_block_top.png"));
        assertTrue(plankSide.contains("textures/block/skyroot_planks_side.png"));
        assertTrue(plankSide.contains("textures/blocks/skyroot_planks.png"));
    }

    @Test
    void exposesCurrentFlatAssetDropAliases() {
        List<String> skyrootSide = AssetAtlasReport.textureCandidates("voxel:skyroot_log", AssetAtlasReport.TextureFace.SIDE);
        List<String> pineTop = AssetAtlasReport.textureCandidates("voxel:pine_log", AssetAtlasReport.TextureFace.TOP);
        List<String> mossyPathTop = AssetAtlasReport.textureCandidates("voxel:mossy_path", AssetAtlasReport.TextureFace.TOP);
        List<String> iceSide = AssetAtlasReport.textureCandidates("voxel:ice", AssetAtlasReport.TextureFace.SIDE);
        List<String> planksSide = AssetAtlasReport.textureCandidates("voxel:skyroot_planks", AssetAtlasReport.TextureFace.SIDE);
        List<String> pinePlanksSide = AssetAtlasReport.textureCandidates("voxel:pine_planks", AssetAtlasReport.TextureFace.SIDE);
        List<String> snowyGrassBottom = AssetAtlasReport.textureCandidates("voxel:snowy_grass_block", AssetAtlasReport.TextureFace.BOTTOM);
        List<String> stoneBricksSide = AssetAtlasReport.textureCandidates("voxel:stone_bricks", AssetAtlasReport.TextureFace.SIDE);
        List<String> glassSide = AssetAtlasReport.textureCandidates("voxel:glass", AssetAtlasReport.TextureFace.SIDE);

        assertTrue(skyrootSide.contains("oak_log_side.png"));
        assertTrue(skyrootSide.contains("blocks/oak_log_side.png"));
        assertTrue(pineTop.contains("spruce_log_top.png"));
        assertTrue(mossyPathTop.contains("mossy_grass_top.png"));
        assertTrue(iceSide.contains("ice_block.png"));
        assertTrue(planksSide.contains("oak_planks.png"));
        assertTrue(pinePlanksSide.contains("spruce_planks.png"));
        assertTrue(snowyGrassBottom.contains("dirt.png"));
        assertTrue(stoneBricksSide.contains("stone_brick_block.png"));
        assertTrue(glassSide.contains("blocks/glass_block.png"));
    }

    @Test
    void reportsMissingCoverageWhenNoIndividualTexturesOrFallbackSheetsExist() throws Exception {
        AssetAtlasReport.Report report = AssetAtlasReport.generate(tempDir);

        assertFalse(report.missingBlocks().isEmpty());
        assertTrue(report.missingFallbackSheets().contains("blocks_tiles_sheet.png"));
        assertEquals(0, report.individualTextureBlockCount());
        assertEquals(0, report.fallbackBlockCount());
    }

    @Test
    void countsFallbackCoveredBlocksWhenSheetExists() throws Exception {
        Files.write(tempDir.resolve("blocks_tiles_sheet.png"), new byte[]{1});

        AssetAtlasReport.Report report = AssetAtlasReport.generate(tempDir);

        assertTrue(report.fallbackBlockCount() > 0);
        assertFalse(report.missingFallbackSheets().contains("blocks_tiles_sheet.png"));
    }

    @Test
    void bundledFallbackSheetsCoverRegisteredBlocks() throws Exception {
        Files.write(tempDir.resolve("ui_hud_sheet.png"), new byte[]{1});
        Files.write(tempDir.resolve("blocks_tiles_sheet.png"), new byte[]{1});
        Files.write(tempDir.resolve("tools_weapons_sheet.png"), new byte[]{1});
        Files.write(tempDir.resolve("nature_food_sheet.png"), new byte[]{1});
        Files.write(tempDir.resolve("ores_materials_sheet.png"), new byte[]{1});

        AssetAtlasReport.Report report = AssetAtlasReport.generate(tempDir);

        assertEquals(0, report.missingBlocks().size());
        assertTrue(report.fallbackBlockCount() > 0);
    }

    @Test
    void reportsUnmappedRootPngFiles() throws Exception {
        Files.write(tempDir.resolve("unknown_sheet.png"), new byte[]{1});
        Files.createDirectories(tempDir.resolve("blocks"));
        Files.write(tempDir.resolve("blocks").resolve("unknown_block.png"), new byte[]{1});
        Files.write(tempDir.resolve("stone.png"), new byte[]{2});

        AssetAtlasReport.Report report = AssetAtlasReport.generate(tempDir);

        assertEquals(List.of("blocks/unknown_block.png", "unknown_sheet.png"), report.unmappedRootPngs());
    }

    @Test
    void detectsDuplicateTextureFilenamesAcrossTextureRoots() throws Exception {
        Files.createDirectories(tempDir.resolve("textures/block"));
        Files.createDirectories(tempDir.resolve("textures/blocks"));
        Files.write(tempDir.resolve("textures/block/stone.png"), new byte[]{1});
        Files.write(tempDir.resolve("textures/blocks/stone.png"), new byte[]{2});

        AssetAtlasReport.Report report = AssetAtlasReport.generate(tempDir);

        assertEquals(List.of("stone.png -> textures/block/stone.png, textures/blocks/stone.png"), report.duplicateTextureNames());
    }
}
