package dev.voxelgame.common.item;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.block.ToolType;
import dev.voxelgame.common.gameplay.WeaponItemRules;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRegistryDataTest {
    @Test
    void defaultItemsHaveUniqueIdsKeysAndDisplayableNames() {
        var items = Items.createDefaultRegistry();
        Set<Short> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();

        for (ItemType item : items.values()) {
            assertTrue(ids.add(item.id()), "Duplicate item id " + item.id());
            assertTrue(keys.add(item.key()), "Duplicate item key " + item.key());
            assertFalse(item.key().isBlank(), "Blank item key");
            assertFalse(displayName(item.key()).isBlank(), "Blank item display name " + item.key());
        }
    }

    @Test
    void toolsHaveDurabilityLevelAndSingleStackSize() {
        for (ItemType item : Items.createDefaultRegistry().values()) {
            if (item.toolType() == ToolType.NONE) {
                assertEquals(0, item.toolLevel(), item.key());
                continue;
            }
            assertEquals(1, item.maxStackSize(), item.key());
            assertTrue(item.durability() > 0, item.key());
            assertTrue(item.toolLevel() >= 1, item.key());
            assertTrue(item.toolSpeed() >= 1.0f, item.key());
        }
    }

    @Test
    void everyPlaceableItemTargetsCanonicalBlockKey() {
        var blocks = Blocks.createDefaultRegistry();

        for (ItemType item : Items.createDefaultRegistry().values()) {
            if (item.placesBlockKey() == null) {
                continue;
            }
            assertEquals(item.placesBlockKey(), blocks.canonicalKey(item.placesBlockKey()).orElseThrow(), item.key());
        }
    }

    @Test
    void mineralSwordItemsAreDurableWeaponsWithCanonicalAliases() {
        var items = Items.createDefaultRegistry();

        assertEquals("voxel:platin_sword", items.canonicalKey("voxel:platinum_sword").orElseThrow());
        assertEquals("voxel:titan_sword", items.canonicalKey("voxel:titanium_sword").orElseThrow());
        for (String key : Set.of("voxel:iron_sword", "voxel:platin_sword", "voxel:sapphire_sword", "voxel:titan_sword")) {
            ItemType item = items.requireByKey(key);
            assertEquals(1, item.maxStackSize(), key);
            assertTrue(item.durability() > 0, key);
            assertTrue(WeaponItemRules.isWeapon(item), key);
        }
    }

    private static String displayName(String key) {
        int colon = key.indexOf(':');
        return (colon >= 0 ? key.substring(colon + 1) : key).replace('_', ' ').trim();
    }
}
