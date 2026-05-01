package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileItemRulesTest {
    @Test
    void knivesCanLaunchProjectilesButBasicToolsCannot() {
        Registry<ItemType> items = Items.createDefaultRegistry();

        assertTrue(ProjectileItemRules.canLaunch(new ItemStack(items.requireByKey("voxel:stone_knife").id(), 1), items));
        assertTrue(ProjectileItemRules.canLaunch(new ItemStack(items.requireByKey("voxel:crystal_knife").id(), 1), items));
        assertFalse(ProjectileItemRules.canLaunch(new ItemStack(items.requireByKey("voxel:stone_pickaxe").id(), 1), items));
        assertFalse(ProjectileItemRules.canLaunch(ItemStack.EMPTY, items));
    }
}
