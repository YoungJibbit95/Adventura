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
