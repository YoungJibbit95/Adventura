package dev.voxelgame.common.item;

import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryTest {
    @Test
    void mergesStacksAndRemovesItems() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short dirt = items.requireByKey("voxel:dirt").id();
        Inventory inventory = new Inventory(2);

        assertEquals(0, inventory.add(dirt, 70, items));
        assertEquals(64, inventory.slot(0).count());
        assertEquals(6, inventory.slot(1).count());

        assertTrue(inventory.remove(dirt, 65));
        assertEquals(5, inventory.count(dirt));
    }

    @Test
    void replacesAndClearsSlots() {
        Inventory inventory = new Inventory(2);

        inventory.replaceSlots(List.of(new ItemStack((short) 2, 4), new ItemStack((short) 7, 1)));
        assertEquals(new ItemStack((short) 2, 4), inventory.slot(0));
        assertEquals(new ItemStack((short) 7, 1), inventory.slot(1));

        inventory.clear();
        assertEquals(ItemStack.EMPTY, inventory.slot(0));
        assertEquals(ItemStack.EMPTY, inventory.slot(1));
    }
}
