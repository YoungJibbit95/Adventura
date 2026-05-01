package dev.voxelgame.client.hud;

import dev.voxelgame.client.GameSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudLayoutTest {
    @Test
    void keepsHotbarAndStatsInsideViewport() {
        assertHudLayoutFits(480, 360, 2.0f);
        assertHudLayoutFits(640, 480, 2.0f);
        assertHudLayoutFits(1280, 720, 2.0f);
    }

    @Test
    void minimalModeHidesQuietOverlays() {
        HudLayout layout = HudLayout.forViewport(1280, 720, 2.0f, GameSettings.HudMode.MINIMAL);

        assertFalse(layout.showSelectedTooltip());
        assertFalse(layout.showTime());
        assertFalse(layout.showWorldInfo());
        assertFalse(layout.showMode());
        assertFalse(layout.showComfort());
        assertTrue(layout.hotbarSlotSize() >= 34.0f);
    }

    @Test
    void clampsInvalidScaleToStableDefault() {
        HudLayout layout = HudLayout.forViewport(640, 480, Float.NaN);

        assertTrue(layout.scale() > 0.0f);
        assertTrue(layout.hotbarX() >= 0.0f);
        assertTrue(layout.hotbarBottom() <= 480.0f + 0.01f);
    }

    @Test
    void nullModeFallsBackToNormalLayout() {
        HudLayout layout = HudLayout.forViewport(1280, 720, 1.0f, null);

        assertTrue(layout.showSelectedTooltip());
        assertTrue(layout.showTime());
        assertTrue(layout.hotbarSlotSize() >= 34.0f);
    }

    @Test
    void hiddenModeSuppressesQuietOverlayFlagsWhenQueriedDirectly() {
        HudLayout layout = HudLayout.forViewport(1280, 720, 1.0f, GameSettings.HudMode.HIDDEN);

        assertFalse(layout.showSelectedTooltip());
        assertFalse(layout.showTime());
        assertFalse(layout.showWorldInfo());
        assertFalse(layout.showMode());
        assertFalse(layout.showComfort());
    }

    private static void assertHudLayoutFits(int width, int height, float uiScale) {
        HudLayout layout = HudLayout.forViewport(width, height, uiScale);

        assertTrue(layout.hotbarX() >= 0.0f);
        assertTrue(layout.hotbarX() + layout.hotbarWidth() <= width + 0.01f);
        assertTrue(layout.hotbarBottom() <= height + 0.01f);
        assertTrue(layout.statsY() > 0.0f);
        assertTrue(layout.statWidth() * 3.0f <= layout.hotbarWidth() + 0.01f);
        assertTrue(layout.feedbackBottomY() < layout.statsY());
        assertTrue(layout.hotbarSlotSize() >= 34.0f);
    }
}
