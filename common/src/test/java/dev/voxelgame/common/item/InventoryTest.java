package dev.voxelgame.common.item;

import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void fullInventoryReturnsOverflowAndCanAddPredictsSpace() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short dirt = items.requireByKey("voxel:dirt").id();
        short torch = items.requireByKey("voxel:torch").id();
        Inventory inventory = new Inventory(1);

        assertEquals(0, inventory.add(dirt, 64, items));
        assertFalse(inventory.canAdd(torch, 1, items));
        assertEquals(1, inventory.add(torch, 1, items));
        assertEquals(0, inventory.count(torch));
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

    @Test
    void addStackKeepsToolDamage() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short axe = items.requireByKey("voxel:stone_axe").id();
        Inventory inventory = new Inventory(2);
        ItemStack damagedTool = new ItemStack(axe, 1, 9);

        assertEquals(0, inventory.addStack(damagedTool, items));

        assertEquals(damagedTool, inventory.slot(0));
    }

    @Test
    void damageSlotRemovesToolAtDurabilityLimit() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ItemType axe = items.requireByKey("voxel:stone_axe");
        Inventory inventory = new Inventory(1);
        inventory.add(axe.id(), 1, items);

        assertTrue(inventory.damageSlot(0, axe.durability(), items));

        assertEquals(ItemStack.EMPTY, inventory.slot(0));
    }

    @Test
    void removePreservesDamageForPartialDamagedStack() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short dirt = items.requireByKey("voxel:dirt").id();
        Inventory inventory = new Inventory(1);
        inventory.setSlot(0, new ItemStack(dirt, 10, 3));

        assertTrue(inventory.remove(dirt, 4));
        assertEquals(new ItemStack(dirt, 6, 3), inventory.slot(0));
    }

    @Test
    void rejectsNegativeItemIdsInItemStacks() {
        assertThrows(IllegalArgumentException.class, () -> new ItemStack((short) -1, 1));
    }

    @Test
    void rejectsZeroCountStacksWithNonEmptyIdOrDamage() {
        assertThrows(IllegalArgumentException.class, () -> new ItemStack((short) 5, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ItemStack((short) 4, 0, 2));
    }
}
