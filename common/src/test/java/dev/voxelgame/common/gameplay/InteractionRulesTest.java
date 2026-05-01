package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntitySnapshot;
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
    void entityReachUsesBoundsClosestPoint() {
        EntitySnapshot sheep = new EntitySnapshot(42L, "voxel:cozy_sheep", null, 7.4, 64.0, 0.5, 0.0f, 0.0f, 10);

        assertTrue(InteractionRules.canReachEntity(0.5, 64.8, 0.5, sheep, 6.5));
        assertFalse(InteractionRules.canReachEntity(0.5, 64.8, 0.5, sheep, 5.5));
    }

    @Test
    void blockLineOfSightStopsAtOccludingIntermediateBlock() {
        assertFalse(InteractionRules.hasBlockLineOfSight(
                0.5,
                64.5,
                0.5,
                0,
                64,
                5,
                (x, y, z) -> x == 0 && y == 64 && z == 3
        ));
    }

    @Test
    void blockLineOfSightIgnoresTargetCellAsOccluder() {
        assertTrue(InteractionRules.hasBlockLineOfSight(
                0.5,
                64.5,
                0.5,
                0,
                64,
                5,
                (x, y, z) -> x == 0 && y == 64 && z == 5
        ));
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
        ItemType crystalPickaxe = items.requireByKey("voxel:crystal_pickaxe");

        assertEquals(1, InteractionRules.toolLevel(stonePickaxe));
        assertEquals(2, InteractionRules.toolLevel(copperPickaxe));
        assertEquals(4, InteractionRules.toolLevel(crystalPickaxe));
        assertTrue(copperPickaxe.toolSpeed() > stonePickaxe.toolSpeed());
        assertTrue(crystalPickaxe.toolSpeed() > copperPickaxe.toolSpeed());
    }

    @Test
    void oreMiningRequiresMatchingToolTier() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        ItemStack stonePickaxe = new ItemStack(items.requireByKey("voxel:stone_pickaxe").id(), 1);
        ItemStack copperPickaxe = new ItemStack(items.requireByKey("voxel:copper_pickaxe").id(), 1);
        ItemStack ironPickaxe = new ItemStack(items.requireByKey("voxel:iron_pickaxe").id(), 1);
        BlockType copperOre = blocks.requireByKey("voxel:copper_ore");
        BlockType ironOre = blocks.requireByKey("voxel:iron_ore");
        BlockType crystalNode = blocks.requireByKey("voxel:glow_crystal_node");

        assertEquals(1, InteractionRules.requiredToolLevel(copperOre));
        assertEquals(2, InteractionRules.requiredToolLevel(ironOre));
        assertTrue(InteractionRules.canHarvest(stonePickaxe, items, copperOre));
        assertFalse(InteractionRules.canHarvest(stonePickaxe, items, ironOre));
        assertTrue(InteractionRules.canHarvest(copperPickaxe, items, ironOre));
        assertFalse(InteractionRules.canHarvest(copperPickaxe, items, crystalNode));
        assertTrue(InteractionRules.canHarvest(ironPickaxe, items, crystalNode));
    }

    @Test
    void crystalToolsAddLateGameBonusHarvests() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        BlockType crystalNode = blocks.requireByKey("voxel:glow_crystal_node");
        BlockType pineLog = blocks.requireByKey("voxel:pine_log");
        BlockType wildGrass = blocks.requireByKey("voxel:wild_grass");

        float ironPickaxe = InteractionRules.breakMultiplier(new ItemStack(items.requireByKey("voxel:iron_pickaxe").id(), 1), items, crystalNode);
        float crystalPickaxe = InteractionRules.breakMultiplier(new ItemStack(items.requireByKey("voxel:crystal_pickaxe").id(), 1), items, crystalNode);
        float crystalAxe = InteractionRules.breakMultiplier(new ItemStack(items.requireByKey("voxel:crystal_axe").id(), 1), items, pineLog);
        float crystalKnife = InteractionRules.breakMultiplier(new ItemStack(items.requireByKey("voxel:crystal_knife").id(), 1), items, wildGrass);

        assertEquals(1, InteractionRules.dropCount(crystalNode, ironPickaxe));
        assertEquals(2, InteractionRules.dropCount(crystalNode, crystalPickaxe));
        assertEquals(2, InteractionRules.dropCount(pineLog, crystalAxe));
        assertEquals(3, InteractionRules.dropCount(wildGrass, crystalKnife));
    }

    @Test
    void wildGrassCanBeGatheredForDryGrassFuel() {
        BlockType wildGrass = Blocks.createDefaultRegistry().requireByKey("voxel:wild_grass");

        InteractionRules.BlockInteraction interaction = InteractionRules.blockInteraction(wildGrass).orElseThrow();

        assertEquals("voxel:dry_grass", interaction.itemKey());
        assertEquals(1, interaction.count());
    }

    @Test
    void berryBushCanBeHarvestedByInteraction() {
        BlockType berryBush = Blocks.createDefaultRegistry().requireByKey("voxel:berry_bush");

        InteractionRules.BlockInteraction interaction = InteractionRules.blockInteraction(berryBush).orElseThrow();

        assertEquals("voxel:berries", interaction.itemKey());
        assertEquals(2, interaction.count());
    }

    @Test
    void reedsCanBeCutForWaterContainerBinding() {
        BlockType reeds = Blocks.createDefaultRegistry().requireByKey("voxel:reeds");

        InteractionRules.BlockInteraction interaction = InteractionRules.blockInteraction(reeds).orElseThrow();

        assertEquals("voxel:reed_bundle", interaction.itemKey());
        assertEquals(1, interaction.count());
    }

    @Test
    void pineLogCanBeTappedForResin() {
        BlockType pineLog = Blocks.createDefaultRegistry().requireByKey("voxel:pine_log");

        InteractionRules.BlockInteraction interaction = InteractionRules.blockInteraction(pineLog).orElseThrow();

        assertEquals("voxel:resin", interaction.itemKey());
        assertEquals(1, interaction.count());
    }

    @Test
    void treeStumpCanBePeeledForBarkStrips() {
        BlockType treeStump = Blocks.createDefaultRegistry().requireByKey("voxel:tree_stump");

        InteractionRules.BlockInteraction interaction = InteractionRules.blockInteraction(treeStump).orElseThrow();

        assertEquals("voxel:bark_strip", interaction.itemKey());
        assertEquals(2, interaction.count());
    }

    @Test
    void glowMushroomCanBeGatheredForCookingCaps() {
        BlockType glowMushroom = Blocks.createDefaultRegistry().requireByKey("voxel:glow_mushroom");

        InteractionRules.BlockInteraction interaction = InteractionRules.blockInteraction(glowMushroom).orElseThrow();

        assertEquals("voxel:glow_mushroom_cap", interaction.itemKey());
        assertEquals(1, interaction.count());
    }

    @Test
    void sporeBlossomsCanBeGatheredForRareTea() {
        BlockType sporeBlossom = Blocks.createDefaultRegistry().requireByKey("voxel:spore_blossom");

        InteractionRules.BlockInteraction interaction = InteractionRules.blockInteraction(sporeBlossom).orElseThrow();

        assertEquals("voxel:spore_blossom", interaction.itemKey());
        assertEquals(1, interaction.count());
        assertEquals("Picked spore blossom", interaction.message());
    }
}
