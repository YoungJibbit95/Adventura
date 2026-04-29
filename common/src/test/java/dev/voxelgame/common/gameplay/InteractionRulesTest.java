package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionRulesTest {
    @Test
    void reachUsesEyePositionAndBlockCenter() {
        assertTrue(InteractionRules.canReachBlock(0.5, 64.5, 0.5, 0, 64, 6));
        assertFalse(InteractionRules.canReachBlock(0.5, 64.5, 0.5, 0, 64, 8));
    }

    @Test
    void preferredToolIncreasesBreakSpeed() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        BlockType stone = Blocks.createDefaultRegistry().requireByKey("voxel:stone");
        ItemStack pickaxe = new ItemStack(items.requireByKey("voxel:stone_pickaxe").id(), 1);

        assertTrue(InteractionRules.breakMultiplier(pickaxe, items, stone) > InteractionRules.breakMultiplier(ItemStack.EMPTY, items, stone));
    }

    @Test
    void itemToolLevelAndSpeedComeFromTierProperties() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ItemType stonePickaxe = items.requireByKey("voxel:stone_pickaxe");
        ItemType copperPickaxe = items.requireByKey("voxel:copper_pickaxe");

        assertEquals(1, InteractionRules.toolLevel(stonePickaxe));
        assertEquals(2, InteractionRules.toolLevel(copperPickaxe));
        assertTrue(copperPickaxe.toolSpeed() > stonePickaxe.toolSpeed());
    }

    @Test
    void oreMiningRequiresMatchingToolTier() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        ItemStack stonePickaxe = new ItemStack(items.requireByKey("voxel:stone_pickaxe").id(), 1);
        ItemStack copperPickaxe = new ItemStack(items.requireByKey("voxel:copper_pickaxe").id(), 1);
        BlockType copperOre = blocks.requireByKey("voxel:copper_ore");
        BlockType ironOre = blocks.requireByKey("voxel:iron_ore");
        BlockType crystalNode = blocks.requireByKey("voxel:glow_crystal_node");

        assertEquals(1, InteractionRules.requiredToolLevel(copperOre));
        assertEquals(2, InteractionRules.requiredToolLevel(ironOre));
        assertTrue(InteractionRules.canHarvest(stonePickaxe, items, copperOre));
        assertFalse(InteractionRules.canHarvest(stonePickaxe, items, ironOre));
        assertTrue(InteractionRules.canHarvest(copperPickaxe, items, ironOre));
        assertFalse(InteractionRules.canHarvest(copperPickaxe, items, crystalNode));
    }

    @Test
    void berryBushCanBeHarvestedByInteraction() {
        BlockType berryBush = Blocks.createDefaultRegistry().requireByKey("voxel:berry_bush");

        InteractionRules.BlockInteraction interaction = InteractionRules.blockInteraction(berryBush).orElseThrow();

        assertEquals("voxel:berries", interaction.itemKey());
        assertEquals(2, interaction.count());
    }

    @Test
    void pineLogCanBeTappedForResin() {
        BlockType pineLog = Blocks.createDefaultRegistry().requireByKey("voxel:pine_log");

        InteractionRules.BlockInteraction interaction = InteractionRules.blockInteraction(pineLog).orElseThrow();

        assertEquals("voxel:resin", interaction.itemKey());
        assertEquals(1, interaction.count());
    }
}
