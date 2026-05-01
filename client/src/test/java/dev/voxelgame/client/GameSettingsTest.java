package dev.voxelgame.client;

import dev.voxelgame.client.render.RenderDebugView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSettingsTest {
    @Test
    void clampsRuntimeSettings() {
        GameSettings settings = GameSettings.fromOptions(new ConnectionOptions(false, null, 25565, "Player", 1L, 99, 99, false, false));

        assertEquals(18, settings.renderDistanceChunks());
        assertEquals(8, settings.previewRadiusChunks());

        settings.adjustFieldOfView(1000);
        settings.adjustMouseSensitivity(-1000);
        settings.adjustUiScale(1000);
        settings.setMeshBuildBudgetChunks(99);
        settings.setMeshBuildBudgetMilliseconds(99.0);
        settings.setGpuUploadBudgetMilliseconds(99.0);
        settings.setParticleQuality(99.0);

        assertEquals(100, settings.fieldOfViewDegrees());
        assertEquals(40, settings.mouseSensitivityPercent());
        assertEquals(150, settings.uiScalePercent());
        assertEquals(12, settings.meshBuildBudgetChunks());
        assertEquals(24, settings.chunkUnloadBudgetChunks());
        assertEquals(24, settings.gpuReleaseBudgetChunks());
        assertEquals(16.0, settings.meshBuildBudgetMilliseconds(), 0.001);
        assertEquals(16.0, settings.gpuUploadBudgetMilliseconds(), 0.001);
        assertEquals(1.0, settings.particleQuality(), 0.001);

        settings.adjustUiScale(-1000);
        settings.adjustMeshBuildBudgetMilliseconds(-1000.0);
        settings.adjustGpuUploadBudgetMilliseconds(-1000.0);
        settings.adjustParticleQuality(-1000.0);

        assertEquals(80, settings.uiScalePercent());
        assertEquals(0.5, settings.meshBuildBudgetMilliseconds(), 0.001);
        assertEquals(0.5, settings.gpuUploadBudgetMilliseconds(), 0.001);
        assertEquals(0.25, settings.particleQuality(), 0.001);
    }

    @Test
    void togglesRenderingFlags() {
        GameSettings settings = GameSettings.fromOptions(new ConnectionOptions(false, null, 25565, "Player", 1L, 3, 8, false, false));

        settings.toggleFog();
        settings.toggleAmbientOcclusion();
        settings.toggleSoftShadows();
        settings.toggleBloom();
        settings.toggleHud();
        settings.toggleDebugOverlay();
        settings.toggleDebugChunkBorders();
        settings.toggleDebugMeshBounds();
        settings.toggleDebugSectionBounds();
        settings.toggleDebugParticleBounds();
        settings.toggleChat();
        settings.toggleTransparentWater();
        settings.toggleSimpleWater();
        settings.toggleGreedyMeshing();
        settings.setRenderDebugView(RenderDebugView.MATERIAL_INDEX);

        assertFalse(settings.fogEnabled());
        assertFalse(settings.ambientOcclusionEnabled());
        assertFalse(settings.softShadowsEnabled());
        assertFalse(settings.bloomEnabled());
        assertFalse(settings.hudEnabled());
        assertEquals(GameSettings.HudMode.HIDDEN, settings.hudMode());
        assertFalse(settings.chatEnabled());
        assertFalse(settings.transparentWaterEnabled());
        assertFalse(settings.greedyMeshingEnabled());
        assertTrue(settings.simpleWaterEnabled());
        assertTrue(settings.debugOverlayEnabled());
        assertTrue(settings.debugChunkBordersEnabled());
        assertTrue(settings.debugMeshBoundsEnabled());
        assertTrue(settings.debugSectionBoundsEnabled());
        assertTrue(settings.debugParticleBoundsEnabled());
        assertEquals(RenderDebugView.MATERIAL_INDEX, settings.renderDebugView());
    }

    @Test
    void cyclesAndParsesHudModes() {
        GameSettings settings = GameSettings.fromOptions(new ConnectionOptions(false, null, 25565, "Player", 1L, 3, 8, false, false));

        settings.cycleHudMode();
        assertEquals(GameSettings.HudMode.MINIMAL, settings.hudMode());
        assertTrue(settings.hudEnabled());

        settings.cycleHudMode();
        assertEquals(GameSettings.HudMode.HIDDEN, settings.hudMode());
        assertFalse(settings.hudEnabled());

        settings.setHudMode(GameSettings.HudMode.parse("normal"));
        assertEquals(GameSettings.HudMode.NORMAL, settings.hudMode());

        assertEquals(GameSettings.HudMode.MINIMAL, GameSettings.HudMode.parse("min"));
        assertEquals(GameSettings.HudMode.HIDDEN, GameSettings.HudMode.parse("off"));
    }

    @Test
    void appliesRenderPresets() {
        GameSettings settings = GameSettings.fromOptions(new ConnectionOptions(false, null, 25565, "Player", 1L, 3, 8, false, false));

        settings.applyPreset(RenderPreset.LOW);

        assertEquals("Low", settings.activePresetLabel());
        assertEquals(4, settings.renderDistanceChunks());
        assertEquals(3, settings.previewRadiusChunks());
        assertEquals(1, settings.meshBuildBudgetChunks());
        assertEquals(1.5, settings.meshBuildBudgetMilliseconds(), 0.001);
        assertEquals(1.0, settings.gpuUploadBudgetMilliseconds(), 0.001);
        assertTrue(settings.fogEnabled());
        assertFalse(settings.ambientOcclusionEnabled());
        assertFalse(settings.softShadowsEnabled());
        assertFalse(settings.bloomEnabled());
        assertTrue(settings.transparentWaterEnabled());
        assertTrue(settings.simpleWaterEnabled());
        assertEquals(0.55, settings.particleQuality(), 0.001);

        settings.applyPreset(RenderPreset.HIGH);

        assertEquals("High", settings.activePresetLabel());
        assertEquals(12, settings.renderDistanceChunks());
        assertEquals(6, settings.previewRadiusChunks());
        assertEquals(4, settings.meshBuildBudgetChunks());
        assertEquals(5.0, settings.meshBuildBudgetMilliseconds(), 0.001);
        assertEquals(4.0, settings.gpuUploadBudgetMilliseconds(), 0.001);
        assertTrue(settings.ambientOcclusionEnabled());
        assertTrue(settings.softShadowsEnabled());
        assertTrue(settings.bloomEnabled());
        assertFalse(settings.simpleWaterEnabled());
        assertEquals(1.0, settings.particleQuality(), 0.001);
    }

    @Test
    void renderPresetsScaleRenderingCostFromLowToHigh() {
        assertTrue(RenderPreset.LOW.renderDistanceChunks() < RenderPreset.MEDIUM.renderDistanceChunks());
        assertTrue(RenderPreset.MEDIUM.renderDistanceChunks() < RenderPreset.HIGH.renderDistanceChunks());
        assertTrue(RenderPreset.LOW.meshBuildBudgetMilliseconds() < RenderPreset.MEDIUM.meshBuildBudgetMilliseconds());
        assertTrue(RenderPreset.MEDIUM.meshBuildBudgetMilliseconds() < RenderPreset.HIGH.meshBuildBudgetMilliseconds());
        assertTrue(RenderPreset.LOW.gpuUploadBudgetMilliseconds() < RenderPreset.MEDIUM.gpuUploadBudgetMilliseconds());
        assertTrue(RenderPreset.MEDIUM.gpuUploadBudgetMilliseconds() < RenderPreset.HIGH.gpuUploadBudgetMilliseconds());
        assertTrue(RenderPreset.LOW.particleQuality() < RenderPreset.MEDIUM.particleQuality());
        assertTrue(RenderPreset.MEDIUM.particleQuality() < RenderPreset.HIGH.particleQuality());

        assertFalse(RenderPreset.LOW.ambientOcclusionEnabled());
        assertFalse(RenderPreset.LOW.softShadowsEnabled());
        assertFalse(RenderPreset.LOW.bloomEnabled());
        assertTrue(RenderPreset.LOW.simpleWaterEnabled());
        assertTrue(RenderPreset.MEDIUM.ambientOcclusionEnabled());
        assertTrue(RenderPreset.MEDIUM.bloomEnabled());
        assertTrue(RenderPreset.HIGH.softShadowsEnabled());
        assertFalse(RenderPreset.HIGH.simpleWaterEnabled());
    }

    @Test
    void manualPresetOptionsBecomeCustomOverrides() {
        GameSettings settings = GameSettings.fromOptions(new ConnectionOptions(false, null, 25565, "Player", 1L, 3, 8, false, false));
        settings.applyPreset(RenderPreset.LOW);

        settings.adjustRenderDistance(1);

        assertEquals("Custom", settings.activePresetLabel());

        settings.applyPreset(RenderPreset.MEDIUM);
        settings.toggleBloom();

        assertEquals("Custom", settings.activePresetLabel());
    }

    @Test
    void adaptiveBudgetsShrinkWhenPreviousFrameWasExpensive() {
        GameSettings settings = GameSettings.fromOptions(new ConnectionOptions(false, null, 25565, "Player", 1L, 3, 8, false, false));
        settings.setMeshBuildBudgetMilliseconds(4.0);
        settings.setGpuUploadBudgetMilliseconds(2.0);

        assertEquals(4.0, settings.effectiveMeshBuildBudgetMilliseconds(16.0), 0.001);
        assertEquals(2.0, settings.effectiveGpuUploadBudgetMilliseconds(16.0), 0.001);
        assertEquals(2.6, settings.effectiveMeshBuildBudgetMilliseconds(26.0), 0.001);
        assertEquals(0.9, settings.effectiveGpuUploadBudgetMilliseconds(32.0), 0.001);
    }

    @Test
    void parsesRenderPresetAliases() {
        assertEquals(RenderPreset.LOW, RenderPreset.parse("l"));
        assertEquals(RenderPreset.MEDIUM, RenderPreset.parse("med"));
        assertEquals(RenderPreset.HIGH, RenderPreset.parse("HIGH"));
    }

    @Test
    void parsesRenderDebugViewAliases() {
        assertEquals(RenderDebugView.NONE, RenderDebugView.parse("off"));
        assertEquals(RenderDebugView.LIGHT, RenderDebugView.parse("lighting"));
        assertEquals(RenderDebugView.SKY_LIGHT, RenderDebugView.parse("sky_light"));
        assertEquals(RenderDebugView.BLOCK_LIGHT, RenderDebugView.parse("blocklight"));
        assertEquals(RenderDebugView.EMISSIVE, RenderDebugView.parse("glow"));
        assertEquals(RenderDebugView.RENDER_LAYER, RenderDebugView.parse("layers"));
        assertEquals(RenderDebugView.UV_ATLAS, RenderDebugView.parse("uv_atlas"));
        assertEquals(RenderDebugView.TRANSPARENT, RenderDebugView.parse("overdraw"));
    }

    @Test
    void cyclesRenderDebugViewsInShaderOrder() {
        GameSettings settings = GameSettings.fromOptions(new ConnectionOptions(false, null, 25565, "Player", 1L, 3, 8, false, false));

        settings.cycleRenderDebugView();
        assertEquals(RenderDebugView.MATERIAL_INDEX, settings.renderDebugView());
    }
}
