package dev.voxelgame.client.ui;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiSpriteSheetTest {
    @Test
    void edgeCheckerTrimRemovesConnectedWhiteBackgroundWithoutEatingInteriorHighlights() {
        BufferedImage image = new BufferedImage(6, 6, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, 0xFFFEFEFE);
            }
        }
        for (int y = 1; y <= 4; y++) {
            for (int x = 1; x <= 4; x++) {
                image.setRGB(x, y, 0xFF315533);
            }
        }
        image.setRGB(2, 2, 0xFFFFFFFF);

        BufferedImage prepared = UiSpriteSheet.prepareImage(image, UiSpriteSheet.BackgroundMode.EDGE_CHECKER_TRIM);

        assertEquals(4, prepared.getWidth());
        assertEquals(4, prepared.getHeight());
        assertEquals(0xFF315533, prepared.getRGB(0, 0));
        assertEquals(0xFFFFFFFF, prepared.getRGB(1, 1));
    }
}
