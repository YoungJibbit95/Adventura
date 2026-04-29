package dev.voxelgame.client.render.assets;

import org.junit.jupiter.api.Test;

import java.util.List;

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
}
