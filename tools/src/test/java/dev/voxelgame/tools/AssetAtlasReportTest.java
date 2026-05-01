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
        assertTrue(plankSide.contains("textures/block/skyroot_planks_side.png"));
        assertTrue(plankSide.contains("textures/blocks/skyroot_planks.png"));
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

        AssetAtlasReport.Report report = AssetAtlasReport.generate(tempDir);

        assertEquals(List.of("unknown_sheet.png"), report.unmappedRootPngs());
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
