package dev.voxelgame.common.block;

import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockAliasTest {
    @Test
    void oldBlockKeysResolveToCanonicalBlocks() {
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();

        assertAlias(blocks, "voxel:grass", "voxel:grass_block");
        assertAlias(blocks, "voxel:planks", "voxel:skyroot_planks");
        assertAlias(blocks, "voxel:wooden_plank", "voxel:skyroot_planks");
        assertAlias(blocks, "voxel:active_campfire", "voxel:campfire_active");
        assertAlias(blocks, "voxel:burned_out_campfire", "voxel:campfire_burned_out");
    }

    @Test
    void everyDeclaredBlockAliasTargetsCanonicalBlock() {
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();

        for (var alias : blocks.aliases().entrySet()) {
            assertTrue(blocks.findByKey(alias.getValue()).isPresent(), alias.getKey());
            assertEquals(alias.getValue(), blocks.canonicalKey(alias.getValue()).orElseThrow(), alias.getKey());
            assertEquals(alias.getValue(), blocks.canonicalKey(alias.getKey()).orElseThrow(), alias.getKey());
        }
    }

    private static void assertAlias(Registry<BlockType> blocks, String aliasKey, String canonicalKey) {
        BlockType alias = blocks.requireByKey(aliasKey);
        BlockType canonical = blocks.requireByKey(canonicalKey);

        assertEquals(canonical, alias);
        assertEquals(canonicalKey, blocks.canonicalKey(aliasKey).orElseThrow());
    }
}
