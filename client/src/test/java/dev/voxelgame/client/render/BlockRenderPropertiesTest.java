package dev.voxelgame.client.render;

import dev.voxelgame.client.render.assets.BlockTextureAtlas;
import dev.voxelgame.common.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockRenderPropertiesTest {
    @Test
    void materialTablesCoverShaderBlockIdRange() {
        assertEquals(BlockRenderProperties.MAX_BLOCK_ID * 4, BlockRenderProperties.colorAlphaTable().length);
        assertEquals(BlockRenderProperties.MAX_BLOCK_ID * 4, BlockRenderProperties.effectsTable().length);
        assertEquals(BlockRenderProperties.SHADER_BLOCK_ID_LIMIT * 4, BlockRenderProperties.shaderColorAlphaTable().length);
        assertEquals(BlockRenderProperties.SHADER_BLOCK_ID_LIMIT * 4, BlockRenderProperties.shaderEffectsTable().length);
        assertEquals(BlockRenderProperties.SHADER_BLOCK_ID_LIMIT, BlockTextureAtlas.SHADER_BLOCK_ID_LIMIT);
    }

    @Test
    void registeredBlocksFitMaterialTable() {
        assertDoesNotThrow(() -> BlockRenderProperties.validateRegisteredBlocks(Blocks.createDefaultRegistry()));
    }

    @Test
    void translucentAnimatedBlocksExposeShaderEffects() {
        BlockRenderProperties water = BlockRenderProperties.forBlock(Blocks.WATER);
        float[] effects = BlockRenderProperties.effectsTable();
        int offset = Blocks.WATER * 4;

        assertEquals(0.58f, water.alpha(), 0.0001f);
        assertTrue(water.animatedFluid());
        assertTrue((water.materialFlags() & BlockRenderProperties.FLAG_TRANSLUCENT) != 0);
        assertTrue((water.materialFlags() & BlockRenderProperties.FLAG_ANIMATED_FLUID) != 0);
        assertEquals(1.0f, effects[offset + 1], 0.0001f);
    }

    @Test
    void cubeLikeFacesFillTransparentSpriteEdges() {
        float[] effects = BlockRenderProperties.effectsTable();

        assertTrue(BlockRenderProperties.fillsTextureGaps(Blocks.STONE));
        assertTrue(BlockRenderProperties.fillsTextureGaps(Blocks.WATER));
        assertTrue(BlockRenderProperties.fillsTextureGaps(Blocks.SMALL_TABLE));
        assertEquals(1.0f, effects[Blocks.STONE * 4 + 3], 0.0001f);
        assertEquals(1.0f, effects[Blocks.WATER * 4 + 3], 0.0001f);
    }

    @Test
    void crossSpriteBlocksKeepAlphaCutouts() {
        float[] effects = BlockRenderProperties.effectsTable();

        assertFalse(BlockRenderProperties.fillsTextureGaps(Blocks.WILD_GRASS));
        assertFalse(BlockRenderProperties.fillsTextureGaps(Blocks.SUN_BLOOM));
        assertFalse(BlockRenderProperties.fillsTextureGaps(Blocks.CAMPFIRE_ACTIVE));
        assertEquals(0.0f, effects[Blocks.WILD_GRASS * 4 + 3], 0.0001f);
    }

    @Test
    void emissiveBlocksExposeGlowStrength() {
        BlockRenderProperties lantern = BlockRenderProperties.forBlock(Blocks.LANTERN);
        float[] effects = BlockRenderProperties.effectsTable();

        assertTrue((lantern.materialFlags() & BlockRenderProperties.FLAG_EMISSIVE) != 0);
        assertEquals(0.78f, effects[Blocks.LANTERN * 4], 0.0001f);
    }
}
