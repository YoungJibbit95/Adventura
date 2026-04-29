package dev.voxelgame.client;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotbarTest {
    @Test
    void starterInventoryCanPlaceAndConsumeDirt() {
        Hotbar hotbar = new Hotbar();
        hotbar.resetForNewGame();

        assertEquals(Blocks.DIRT, hotbar.selectedPlaceBlockId().orElseThrow());
        assertTrue(hotbar.consumeSelectedOne());
        assertTrue(hotbar.selectedLabel().contains("x15"));
    }

    @Test
    void serverSnapshotReplacesInventory() {
        Hotbar hotbar = new Hotbar();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(36, ItemStack.EMPTY));
        slots.set(0, new ItemStack((short) 7, 3));

        hotbar.applySnapshot(slots);

        assertEquals(Blocks.TORCH, hotbar.selectedPlaceBlockId().orElseThrow());
        assertTrue(hotbar.selectedLabel().contains("x3"));
    }

    @Test
    void exposesSlotViewForSpriteUi() {
        Hotbar hotbar = new Hotbar();
        hotbar.resetForNewGame();

        Hotbar.SlotView first = hotbar.slotView(0);

        assertEquals("voxel:dirt", first.itemKey());
        assertEquals(16, first.count());
    }

    @Test
    void mouseWheelScrollWrapsSelection() {
        Hotbar hotbar = new Hotbar();
        hotbar.resetForNewGame();

        assertTrue(hotbar.scroll(-1));
        assertEquals(8, hotbar.selectedIndex());
        assertTrue(hotbar.scroll(1));
        assertEquals(0, hotbar.selectedIndex());
    }
}
