package dev.voxelgame.common.item;

import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemAliasTest {
    @Test
    void oldItemKeysResolveToCanonicalItems() {
        var items = Items.createDefaultRegistry();

        assertAlias(items, "voxel:wild_berries", "voxel:berries");
        assertAlias(items, "voxel:planks", "voxel:skyroot_planks");
        assertAlias(items, "voxel:wooden_plank", "voxel:skyroot_planks");
        assertAlias(items, "voxel:iron_ore", "voxel:raw_iron");
        assertAlias(items, "voxel:copper_ore", "voxel:raw_copper");
    }

    private static void assertAlias(Registry<ItemType> items, String aliasKey, String canonicalKey) {
        ItemType alias = items.requireByKey(aliasKey);
        ItemType canonical = items.requireByKey(canonicalKey);

        assertEquals(canonical, alias);
        assertEquals(canonicalKey, items.canonicalKey(aliasKey).orElseThrow());
    }
}
