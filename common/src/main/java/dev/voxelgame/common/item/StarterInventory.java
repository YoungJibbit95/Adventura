package dev.voxelgame.common.item;

import dev.voxelgame.common.registry.Registry;

public final class StarterInventory {
    private StarterInventory() {
    }

    public static void apply(Inventory inventory, Registry<ItemType> items) {
        inventory.clear();
        add(inventory, items, "voxel:dirt", 16);
        add(inventory, items, "voxel:torch", 8);
        add(inventory, items, "voxel:skyroot_log", 6);
        add(inventory, items, "voxel:stick", 4);
        add(inventory, items, "voxel:twig", 6);
        add(inventory, items, "voxel:fiber", 4);
        add(inventory, items, "voxel:pebble", 4);
        add(inventory, items, "voxel:apple", 3);
        add(inventory, items, "voxel:stone_pickaxe", 1);
    }

    private static void add(Inventory inventory, Registry<ItemType> items, String itemKey, int count) {
        ItemType item = items.requireByKey(itemKey);
        int remaining = inventory.add(item.id(), count, items);
        if (remaining != 0) {
            throw new IllegalStateException("Starter inventory overflow for " + itemKey);
        }
    }
}
