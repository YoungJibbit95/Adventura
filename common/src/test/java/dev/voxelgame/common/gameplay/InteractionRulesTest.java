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
    void berryBushCanBeHarvestedByInteraction() {
        BlockType berryBush = Blocks.createDefaultRegistry().requireByKey("voxel:berry_bush");

        InteractionRules.BlockInteraction interaction = InteractionRules.blockInteraction(berryBush).orElseThrow();

        assertEquals("voxel:berries", interaction.itemKey());
        assertEquals(2, interaction.count());
    }
}
