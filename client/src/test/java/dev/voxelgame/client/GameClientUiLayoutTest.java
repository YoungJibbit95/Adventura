package dev.voxelgame.client;

import dev.voxelgame.client.ui.BitmapFont;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameClientUiLayoutTest {
    @Test
    void fitTextToWidthKeepsRenderedTextInsidePixelWidth() {
        float scale = 1.4f;
        float maxWidth = 128.0f;

        String fitted = GameClient.fitTextToWidth("DRAG MOVE SHIFT CLICK RIGHT CLICK SPLIT", scale, maxWidth);

        assertFalse(fitted.isBlank());
        assertTrue(BitmapFont.textWidth(fitted, scale) <= maxWidth);
        assertTrue(fitted.endsWith("..."));
    }

    @Test
    void storageLayoutKeepsPanelsAndCloseButtonSeparated() {
        assertStorageLayoutFits(640, 480, 2.0f);
        assertStorageLayoutFits(800, 600, 1.5f);
        assertStorageLayoutFits(1280, 720, 2.0f);
    }

    @Test
    void settingsLayoutKeepsPresetControlsAndBackButtonSeparated() {
        assertSettingsLayoutFits(640, 480);
        assertSettingsLayoutFits(800, 600);
        assertSettingsLayoutFits(1280, 720);
    }

    @Test
    void craftingLayoutKeepsHeaderAndContentInsideViewport() {
        assertCraftingLayoutFits(640, 480, 2.0f);
        assertCraftingLayoutFits(800, 600, 1.5f);
        assertCraftingLayoutFits(1280, 720, 2.0f);
    }

    @Test
    void hudLayoutKeepsHotbarAndStatsInsideViewport() {
        assertHudLayoutFits(480, 360, 2.0f);
        assertHudLayoutFits(640, 480, 2.0f);
        assertHudLayoutFits(1280, 720, 2.0f);
    }

    @Test
    void minimalHudLayoutHidesQuietOverlays() {
        GameClient.HudLayout layout = GameClient.hudLayout(1280, 720, 2.0f, GameSettings.HudMode.MINIMAL);

        assertFalse(layout.showSelectedTooltip());
        assertFalse(layout.showTime());
        assertFalse(layout.showWorldInfo());
        assertFalse(layout.showMode());
        assertFalse(layout.showComfort());
        assertTrue(layout.hotbarSlotSize() >= 34.0f);
    }

    private static void assertStorageLayoutFits(int width, int height, float uiScale) {
        GameClient.StorageScreenLayout layout = GameClient.storageScreenLayout(width, height, uiScale);

        assertTrue(layout.panelX() >= 0.0f);
        assertTrue(layout.panelX() + layout.panelWidth() <= width + 0.01f);
        assertTrue(layout.cratePanelY() + layout.cratePanelHeight() < layout.backpackPanelY());
        assertTrue(layout.backpackPanelY() + layout.backpackPanelHeight() < layout.closeY());
        assertTrue(layout.bottom() <= height + 0.01f);
        assertTrue(layout.slot() >= 18.0f);
    }

    private static void assertSettingsLayoutFits(int width, int height) {
        GameClient.SettingsScreenLayout layout = GameClient.settingsScreenLayout(width, height);

        assertTrue(layout.x() >= 0.0f);
        assertTrue(layout.x() + layout.panelWidth() <= width + 0.01f);
        assertTrue(layout.contentY() > layout.subtitleY());
        assertTrue(layout.contentY() + layout.sectionHeight() < layout.presetY());
        if (layout.showControls()) {
            assertTrue(layout.presetY() + layout.presetHeight() < layout.controlsY());
            assertTrue(layout.controlsY() + layout.controlsHeight() < layout.backY());
        } else {
            assertTrue(layout.presetY() + layout.presetHeight() < layout.backY());
        }
        assertTrue(layout.bottom() <= height + 0.01f);
        assertTrue(layout.scale() >= 0.72f);
    }

    private static void assertCraftingLayoutFits(int width, int height, float uiScale) {
        GameClient.CraftingScreenLayout layout = GameClient.craftingScreenLayout(width, height, uiScale);

        assertTrue(layout.x() >= 0.0f);
        assertTrue(layout.x() + layout.contentWidth() <= width + 0.01f);
        assertTrue(BitmapFont.textWidth("CRAFTING", layout.titleScale()) <= width - layout.margin() * 2.0f + 0.01f);
        assertTrue(layout.hintY() > layout.titleY());
        assertTrue(layout.tabsY() < layout.contentY());
        assertTrue(layout.inventoryY() > layout.contentY());
        assertTrue(layout.titleY() >= 0.0f);
        assertTrue(layout.inventoryY() < height);
    }

    private static void assertHudLayoutFits(int width, int height, float uiScale) {
        GameClient.HudLayout layout = GameClient.hudLayout(width, height, uiScale);

        assertTrue(layout.hotbarX() >= 0.0f);
        assertTrue(layout.hotbarX() + layout.hotbarWidth() <= width + 0.01f);
        assertTrue(layout.hotbarBottom() <= height + 0.01f);
        assertTrue(layout.statsY() > 0.0f);
        assertTrue(layout.statWidth() * 3.0f <= layout.hotbarWidth() + 0.01f);
        assertTrue(layout.feedbackBottomY() < layout.statsY());
        assertTrue(layout.hotbarSlotSize() >= 34.0f);
    }
}
