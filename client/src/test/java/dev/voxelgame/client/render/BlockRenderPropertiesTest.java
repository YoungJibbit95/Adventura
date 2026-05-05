package dev.voxelgame.client.render;

import dev.voxelgame.common.block.BlockRenderLayer;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.block.ToolType;
import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockRenderPropertiesTest {
    @Test
    void materialTablesCoverMaterialIndexRange() {
        assertEquals(BlockRenderProperties.MAX_BLOCK_ID * 4, BlockRenderProperties.colorAlphaTable().length);
        assertEquals(BlockRenderProperties.MAX_BLOCK_ID * 4, BlockRenderProperties.effectsTable().length);
        assertEquals(BlockRenderProperties.MATERIAL_INDEX_LIMIT * 4, BlockRenderProperties.shaderColorAlphaTable().length);
        assertEquals(BlockRenderProperties.MATERIAL_INDEX_LIMIT * 4, BlockRenderProperties.shaderEffectsTable().length);
        RenderMaterial[] materials = RenderMaterial.fromRegistry(Blocks.createDefaultRegistry());
        assertTrue(materials.length > Blocks.LAVA);
        assertTrue(materials.length <= BlockRenderProperties.MATERIAL_INDEX_LIMIT);
    }

    @Test
    void registeredBlocksFitMaterialTable() {
        assertDoesNotThrow(() -> BlockRenderProperties.validateRegisteredBlocks(Blocks.createDefaultRegistry()));
    }

    @Test
    void materialTableReportsRegisteredBlocksWithoutMaterialData() {
        Registry<BlockType> blocks = new Registry<>("blocks");
        BlockType unknown = new BlockType((short) 300, "voxel:unknown_test_block", 1.0f, ToolType.NONE, true, true, true, 0, BlockRenderLayer.SOLID, null);
        blocks.register(unknown.id(), unknown.key(), unknown);

        RenderMaterial.Table table = RenderMaterial.tableFor(blocks, null);

        assertTrue(table.materialCount() > 256);
        assertEquals(1, table.missingMaterialCount());
        assertTrue(table.materials()[unknown.id()].missingMaterialData());
    }

    @Test
    void translucentAnimatedBlocksExposeShaderEffects() {
        BlockRenderProperties water = BlockRenderProperties.forBlock(Blocks.WATER);
        BlockRenderProperties lava = BlockRenderProperties.forBlock(Blocks.LAVA);
        float[] effects = BlockRenderProperties.effectsTable();
        int offset = Blocks.WATER * 4;
        int lavaOffset = Blocks.LAVA * 4;

        assertEquals(0.58f, water.alpha(), 0.0001f);
        assertTrue(water.animatedFluid());
        assertEquals(BlockRenderProperties.BiomeTintMode.WATER, water.biomeTintMode());
        assertEquals(BlockRenderProperties.FogAffectMode.REDUCED, water.fogAffectMode());
        assertTrue((water.materialFlags() & BlockRenderProperties.FLAG_TRANSLUCENT) != 0);
        assertTrue((water.materialFlags() & BlockRenderProperties.FLAG_ANIMATED_FLUID) != 0);
        assertEquals(1.0f, effects[offset + 1], 0.0001f);
        assertEquals(0.72f, lava.alpha(), 0.0001f);
        assertEquals(1.0f, lava.emissive(), 0.0001f);
        assertTrue(lava.animatedFluid());
        assertTrue((lava.materialFlags() & BlockRenderProperties.FLAG_TRANSLUCENT) != 0);
        assertTrue((lava.materialFlags() & BlockRenderProperties.FLAG_EMISSIVE) != 0);
        assertEquals(1.0f, effects[lavaOffset], 0.0001f);
        assertEquals(1.0f, effects[lavaOffset + 1], 0.0001f);
    }

    @Test
    void cubeLikeFacesFillTransparentSpriteEdges() {
        float[] effects = BlockRenderProperties.effectsTable();

        assertTrue(BlockRenderProperties.fillsTextureGaps(Blocks.STONE));
        assertTrue(BlockRenderProperties.fillsTextureGaps(Blocks.WATER));
        assertTrue(BlockRenderProperties.fillsTextureGaps(Blocks.LAVA));
        assertTrue(BlockRenderProperties.fillsTextureGaps(Blocks.SMALL_TABLE));
        assertEquals(1.0f, effects[Blocks.STONE * 4 + 3], 0.0001f);
        assertEquals(1.0f, effects[Blocks.WATER * 4 + 3], 0.0001f);
    }

    @Test
    void crossSpriteBlocksKeepAlphaCutouts() {
        float[] effects = BlockRenderProperties.effectsTable();

        assertFalse(BlockRenderProperties.fillsTextureGaps(Blocks.WILD_GRASS));
        assertFalse(BlockRenderProperties.fillsTextureGaps(Blocks.SUN_BLOOM));
        assertFalse(BlockRenderProperties.fillsTextureGaps(Blocks.REEDS));
        assertFalse(BlockRenderProperties.fillsTextureGaps(Blocks.GLOW_MUSHROOM));
        assertFalse(BlockRenderProperties.fillsTextureGaps(Blocks.SPORE_BLOSSOM));
        assertFalse(BlockRenderProperties.fillsTextureGaps(Blocks.CAMPFIRE_ACTIVE));
        assertEquals(0.0f, effects[Blocks.WILD_GRASS * 4 + 3], 0.0001f);
    }

    @Test
    void emissiveBlocksExposeGlowStrength() {
        BlockRenderProperties lantern = BlockRenderProperties.forBlock(Blocks.LANTERN);
        BlockRenderProperties ancientLantern = BlockRenderProperties.forBlock(Blocks.ANCIENT_LANTERN);
        BlockRenderProperties glowMushroom = BlockRenderProperties.forBlock(Blocks.GLOW_MUSHROOM);
        BlockRenderProperties sporeBlossom = BlockRenderProperties.forBlock(Blocks.SPORE_BLOSSOM);
        float[] effects = BlockRenderProperties.effectsTable();

        assertTrue((lantern.materialFlags() & BlockRenderProperties.FLAG_EMISSIVE) != 0);
        assertTrue((ancientLantern.materialFlags() & BlockRenderProperties.FLAG_EMISSIVE) != 0);
        assertTrue((glowMushroom.materialFlags() & BlockRenderProperties.FLAG_EMISSIVE) != 0);
        assertTrue((sporeBlossom.materialFlags() & BlockRenderProperties.FLAG_EMISSIVE) != 0);
        assertEquals(0.78f, effects[Blocks.LANTERN * 4], 0.0001f);
        assertEquals(0.95f, effects[Blocks.ANCIENT_LANTERN * 4], 0.0001f);
        assertEquals(0.62f, effects[Blocks.GLOW_MUSHROOM * 4], 0.0001f);
        assertEquals(0.38f, effects[Blocks.SPORE_BLOSSOM * 4], 0.0001f);
    }

    @Test
    void emissiveVisualsStaySeparateFromWorldLightValues() {
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();

        assertEquals(0, blocks.requireById(Blocks.CAMPFIRE).lightEmission());
        assertTrue(BlockRenderProperties.forBlock(Blocks.CAMPFIRE).emissive() > 0.0f);
        assertEquals(14, blocks.requireById(Blocks.CAMPFIRE_ACTIVE).lightEmission());
        assertEquals(1.0f, BlockRenderProperties.forBlock(Blocks.CAMPFIRE_ACTIVE).emissive(), 0.0001f);
    }

    @Test
    void materialLutPacksUvAndRenderMetadataRows() {
        RenderMaterial[] materials = new RenderMaterial[Blocks.WATER + 1];
        Arrays.fill(materials, RenderMaterial.fallback());
        materials[Blocks.WATER] = new RenderMaterial(
                Blocks.WATER,
                "voxel:water",
                BlockRenderLayer.TRANSLUCENT,
                0.20f,
                0.42f,
                0.82f,
                0.58f,
                0.0f,
                true,
                BlockRenderProperties.DEFAULT_CUTOUT_THRESHOLD,
                BlockRenderProperties.BiomeTintMode.WATER,
                BlockRenderProperties.FogAffectMode.REDUCED,
                true,
                BlockRenderProperties.DEFAULT_ROUGHNESS,
                false,
                0.10f,
                0.20f,
                0.30f,
                0.40f,
                -1.0f,
                -1.0f,
                -1.0f,
                -1.0f,
                -1.0f,
                -1.0f,
                -1.0f,
                -1.0f
        );

        float[] pixels = TerrainMaterialLut.pixels(materials);
        int effects = offset(materials.length, Blocks.WATER, TerrainMaterialLut.ROW_EFFECTS);
        int side = offset(materials.length, Blocks.WATER, TerrainMaterialLut.ROW_SIDE_UV);
        int style = offset(materials.length, Blocks.WATER, TerrainMaterialLut.ROW_STYLE);

        assertEquals(0.0f, pixels[effects], 0.0001f);
        assertEquals(1.0f, pixels[effects + 1], 0.0001f);
        assertTrue(((int) pixels[effects + 2] & BlockRenderProperties.FLAG_ANIMATED_FLUID) != 0);
        assertTrue(((int) pixels[effects + 2] & BlockRenderProperties.FLAG_LAYER_TRANSLUCENT) != 0);
        assertEquals(BlockRenderProperties.DEFAULT_CUTOUT_THRESHOLD, pixels[effects + 3], 0.0001f);
        assertEquals(0.10f, pixels[side], 0.0001f);
        assertEquals(0.40f, pixels[side + 3], 0.0001f);
        assertEquals(BlockRenderProperties.BiomeTintMode.WATER.ordinal(), pixels[style], 0.0001f);
        assertEquals(BlockRenderProperties.FogAffectMode.REDUCED.ordinal(), pixels[style + 1], 0.0001f);
        assertEquals(BlockRenderLayer.TRANSLUCENT.ordinal(), pixels[style + 2], 0.0001f);
    }

    private static int offset(int materialCount, short materialIndex, int row) {
        return (row * materialCount + materialIndex) * 4;
    }
}
