package dev.voxelgame.client;

import dev.voxelgame.client.ui.BitmapFont;
import dev.voxelgame.client.viewmodel.LoadingScreenViewModel;
import dev.voxelgame.common.world.ChunkStreamingRings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void loadingLayoutKeepsProgressBarAndTextInsideViewport() {
        assertLoadingLayoutFits(360, 280, 2.0f);
        assertLoadingLayoutFits(640, 480, 1.5f);
        assertLoadingLayoutFits(1280, 720, 2.0f);
    }

    @Test
    void mainMenuLayoutKeepsButtonsAndStatusInsideViewport() {
        assertMainMenuLayoutFits(360, 280, 2.0f);
        assertMainMenuLayoutFits(640, 480, 1.5f);
        assertMainMenuLayoutFits(1280, 720, 2.0f);
    }

    @Test
    void loadingBarFillUsesProgressOrIndeterminateAnimation() {
        assertTrue(GameClient.loadingBarFill(LoadingScreenViewModel.streamingSpawn(3, 4), 0.0) >= 0.75);
        assertTrue(GameClient.loadingBarFill(LoadingScreenViewModel.boot(), 0.0) >= 0.18);
        assertTrue(GameClient.loadingBarFill(LoadingScreenViewModel.boot(), 1.0) <= 0.82);
    }

    @Test
    void singleplayerInitialLoadUsesSmallSpawnRing() {
        ChunkStreamingRings defaultRings = ChunkStreamingRings.client(8, 3);
        ChunkStreamingRings tinyRings = ChunkStreamingRings.client(1, 1);

        assertTrue(GameClient.initialSingleplayerLoadRadius(defaultRings) <= 2);
        assertEquals(25, GameClient.loadingChunkTarget(GameClient.initialSingleplayerLoadRadius(defaultRings)));
        assertEquals(9, GameClient.loadingChunkTarget(GameClient.initialSingleplayerLoadRadius(tinyRings)));
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

    private static void assertLoadingLayoutFits(int width, int height, float uiScale) {
        GameClient.LoadingScreenLayout layout = GameClient.loadingScreenLayout(width, height, uiScale);

        assertTrue(layout.barX() >= 0.0f);
        assertTrue(layout.barX() + layout.barWidth() <= width + 0.01f);
        assertTrue(layout.titleY() >= 0.0f);
        assertTrue(layout.detailY() > layout.titleY());
        assertTrue(layout.barY() > layout.detailY());
        assertTrue(layout.bottom() <= height + 0.01f);
        assertTrue(layout.border() > 0.0f);
    }

    private static void assertMainMenuLayoutFits(int width, int height, float uiScale) {
        GameClient.MainMenuLayout layout = GameClient.mainMenuLayout(width, height, uiScale);

        assertTrue(layout.panelX() >= 0.0f);
        assertTrue(layout.panelY() >= 0.0f);
        assertTrue(layout.panelX() + layout.panelWidth() <= width + 0.01f);
        assertTrue(layout.bottom() <= height + 0.01f);
        assertTrue(layout.buttonX() >= layout.panelX());
        assertTrue(layout.buttonX() + layout.buttonWidth() <= layout.panelX() + layout.panelWidth() + 0.01f);
        assertTrue(BitmapFont.textWidth("ADVENTURA", layout.titleScale()) <= layout.panelWidth() - 24.0f + 0.01f);
        assertTrue(layout.subtitleY() > layout.titleY());
        assertTrue(layout.firstButtonY() > layout.subtitleY());
        assertTrue(layout.buttonY(3) + layout.buttonHeight() < layout.statusY());
        assertTrue(layout.statusY() + BitmapFont.textHeight(layout.statusScale()) <= layout.bottom() + 0.01f);
        assertTrue(layout.scale() >= 0.50f);
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
        assertTrue(layout.recipeListX() >= layout.x());
        assertTrue(layout.recipeListX() + layout.recipeListWidth() <= layout.x() + layout.contentWidth() + 0.01f);
        if (layout.recipeDetailsWidth() > 0.0f) {
            assertTrue(layout.recipeDetailsX() > layout.recipeListX());
            assertTrue(layout.recipeDetailsX() + layout.recipeDetailsWidth() <= layout.x() + layout.contentWidth() + 0.01f);
        }
        assertTrue(layout.craftingPanelHeight() >= 128.0f);
        assertTrue(layout.craftingPanelBottom() < layout.inventoryY());
        assertTrue(layout.titleY() >= 0.0f);
        assertTrue(layout.inventoryY() < height);
    }
}
