package dev.voxelgame.client;

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

        assertEquals(100, settings.fieldOfViewDegrees());
        assertEquals(40, settings.mouseSensitivityPercent());
        assertEquals(150, settings.uiScalePercent());
        assertEquals(12, settings.meshBuildBudgetChunks());

        settings.adjustUiScale(-1000);

        assertEquals(80, settings.uiScalePercent());
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
        settings.toggleChat();
        settings.toggleTransparentWater();

        assertFalse(settings.fogEnabled());
        assertFalse(settings.ambientOcclusionEnabled());
        assertFalse(settings.softShadowsEnabled());
        assertFalse(settings.bloomEnabled());
        assertFalse(settings.hudEnabled());
        assertFalse(settings.chatEnabled());
        assertFalse(settings.transparentWaterEnabled());
        assertTrue(settings.debugOverlayEnabled());
        assertTrue(settings.debugChunkBordersEnabled());
    }

    @Test
    void appliesRenderPresets() {
        GameSettings settings = GameSettings.fromOptions(new ConnectionOptions(false, null, 25565, "Player", 1L, 3, 8, false, false));

        settings.applyPreset(RenderPreset.LOW);

        assertEquals(4, settings.renderDistanceChunks());
        assertEquals(3, settings.previewRadiusChunks());
        assertEquals(1, settings.meshBuildBudgetChunks());
        assertTrue(settings.fogEnabled());
        assertFalse(settings.ambientOcclusionEnabled());
        assertFalse(settings.softShadowsEnabled());
        assertFalse(settings.bloomEnabled());
        assertTrue(settings.transparentWaterEnabled());

        settings.applyPreset(RenderPreset.HIGH);

        assertEquals(12, settings.renderDistanceChunks());
        assertEquals(6, settings.previewRadiusChunks());
        assertEquals(4, settings.meshBuildBudgetChunks());
        assertTrue(settings.ambientOcclusionEnabled());
        assertTrue(settings.softShadowsEnabled());
        assertTrue(settings.bloomEnabled());
    }

    @Test
    void parsesRenderPresetAliases() {
        assertEquals(RenderPreset.LOW, RenderPreset.parse("l"));
        assertEquals(RenderPreset.MEDIUM, RenderPreset.parse("med"));
        assertEquals(RenderPreset.HIGH, RenderPreset.parse("HIGH"));
    }
}
