package dev.voxelgame.common.item;

import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void everyDeclaredItemAliasTargetsCanonicalItem() {
        var items = Items.createDefaultRegistry();

        for (var alias : items.aliases().entrySet()) {
            assertTrue(items.findByKey(alias.getValue()).isPresent(), alias.getKey());
            assertEquals(alias.getValue(), items.canonicalKey(alias.getValue()).orElseThrow(), alias.getKey());
            assertEquals(alias.getValue(), items.canonicalKey(alias.getKey()).orElseThrow(), alias.getKey());
        }
    }

    private static void assertAlias(Registry<ItemType> items, String aliasKey, String canonicalKey) {
        ItemType alias = items.requireByKey(aliasKey);
        ItemType canonical = items.requireByKey(canonicalKey);

        assertEquals(canonical, alias);
        assertEquals(canonicalKey, items.canonicalKey(aliasKey).orElseThrow());
    }
}
