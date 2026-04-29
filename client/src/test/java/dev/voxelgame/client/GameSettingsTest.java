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
        settings.setMeshBuildBudgetChunks(99);

        assertEquals(100, settings.fieldOfViewDegrees());
        assertEquals(40, settings.mouseSensitivityPercent());
        assertEquals(12, settings.meshBuildBudgetChunks());
    }

    @Test
    void togglesRenderingFlags() {
        GameSettings settings = GameSettings.fromOptions(new ConnectionOptions(false, null, 25565, "Player", 1L, 3, 8, false, false));

        settings.toggleFog();
        settings.toggleAmbientOcclusion();
        settings.toggleSoftShadows();
        settings.toggleHud();
        settings.toggleDebugOverlay();
        settings.toggleChat();
        settings.toggleTransparentWater();

        assertFalse(settings.fogEnabled());
        assertFalse(settings.ambientOcclusionEnabled());
        assertFalse(settings.softShadowsEnabled());
        assertFalse(settings.hudEnabled());
        assertFalse(settings.chatEnabled());
        assertFalse(settings.transparentWaterEnabled());
        assertTrue(settings.debugOverlayEnabled());
    }
}
