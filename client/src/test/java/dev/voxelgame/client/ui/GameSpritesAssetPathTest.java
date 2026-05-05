package dev.voxelgame.client.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSpritesAssetPathTest {
    @Test
    void itemCandidatesPreferIndividualItemFilesAndAssetDropFallbacks() {
        List<String> stone = GameSprites.itemTextureCandidates("voxel:stone");
        List<String> grass = GameSprites.itemTextureCandidates("voxel:grass_block");

        assertTrue(stone.indexOf("assets/game/textures/item/stone.png")
                < stone.indexOf("assets/game/blocks/stone.png"));
        assertTrue(stone.indexOf("assets/game/blocks/stone.png")
                < stone.indexOf("assets/game/stone.png"));
        assertTrue(grass.contains("assets/game/blocks/grass_block_top.png"));
        assertTrue(grass.contains("assets/game/grass_block_top.png"));
        assertTrue(grass.contains("assets/game/grass_block_side.png"));
    }

    @Test
    void itemCandidatesMapLocalWorldNamesToDroppedAssetNames() {
        List<String> skyrootLog = GameSprites.itemTextureCandidates("voxel:skyroot_log");
        List<String> pineLeaves = GameSprites.itemTextureCandidates("voxel:pine_leaves");
        List<String> mossyPath = GameSprites.itemTextureCandidates("voxel:mossy_path");
        List<String> ice = GameSprites.itemTextureCandidates("voxel:ice");
        List<String> planks = GameSprites.itemTextureCandidates("voxel:skyroot_planks");
        List<String> pinePlanks = GameSprites.itemTextureCandidates("voxel:pine_planks");
        List<String> stoneBricks = GameSprites.itemTextureCandidates("voxel:stone_bricks");
        List<String> snowyGrass = GameSprites.itemTextureCandidates("voxel:snowy_grass_block");
        List<String> glass = GameSprites.itemTextureCandidates("voxel:glass");
        List<String> gold = GameSprites.itemTextureCandidates("voxel:gold_ingot");
        List<String> titanSword = GameSprites.itemTextureCandidates("voxel:titan_sword");

        assertTrue(skyrootLog.contains("assets/game/oak_log_side.png"));
        assertTrue(skyrootLog.contains("assets/game/blocks/oak_log_side.png"));
        assertTrue(pineLeaves.contains("assets/game/spruce_leaves.png"));
        assertTrue(mossyPath.contains("assets/game/mossy_grass_top.png"));
        assertTrue(ice.contains("assets/game/ice_block.png"));
        assertTrue(planks.contains("assets/game/oak_planks.png"));
        assertTrue(pinePlanks.contains("assets/game/spruce_planks.png"));
        assertTrue(stoneBricks.contains("assets/game/stone_brick_block.png"));
        assertTrue(snowyGrass.contains("assets/game/snowy_grass_block_top.png"));
        assertTrue(glass.contains("assets/game/blocks/glass_block.png"));
        assertTrue(gold.contains("assets/game/minerals/gold_ingot.png"));
        assertTrue(titanSword.contains("assets/game/titan_sword.png"));
    }
}
