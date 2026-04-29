package dev.voxelgame.client.render.assets;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockTextureAtlasTest {
    @Test
    void exposesStableBlockTextureCandidatePaths() {
        List<String> grassTop = BlockTextureAtlas.textureCandidates("voxel:grass_block", BlockTextureAtlas.TextureFace.TOP);
        List<String> plankSide = BlockTextureAtlas.textureCandidates("voxel:skyroot_planks", BlockTextureAtlas.TextureFace.SIDE);

        assertTrue(grassTop.contains("assets/game/textures/block/grass_block_top.png"));
        assertTrue(grassTop.contains("assets/game/textures/block/grass_top.png"));
        assertTrue(plankSide.contains("assets/game/textures/block/skyroot_planks_side.png"));
        assertTrue(plankSide.contains("assets/game/textures/blocks/skyroot_planks.png"));
    }

    @Test
    void trimsTransparentPaddingFromSheetSlices() {
        BufferedImage image = new BufferedImage(6, 5, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(2, 1, 0xFFAA5500);
        image.setRGB(3, 2, 0xFFAA5500);
        image.setRGB(4, 3, 0xFFAA5500);

        BufferedImage trimmed = BlockTextureAtlas.trimTransparentPadding(image);

        assertEquals(3, trimmed.getWidth());
        assertEquals(3, trimmed.getHeight());
        assertEquals(0xFFAA5500, trimmed.getRGB(0, 0));
        assertEquals(0xFFAA5500, trimmed.getRGB(2, 2));
    }

    @Test
    void fillsTransparentPixelsForFullBlockFaces() {
        BufferedImage image = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(1, 1, 0xFF336699);

        BufferedImage filled = BlockTextureAtlas.fillTransparentPixels(image);

        for (int y = 0; y < filled.getHeight(); y++) {
            for (int x = 0; x < filled.getWidth(); x++) {
                assertEquals(0xFF336699, filled.getRGB(x, y));
            }
        }
    }
}
