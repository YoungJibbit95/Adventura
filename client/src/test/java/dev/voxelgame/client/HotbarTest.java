package dev.voxelgame.client;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.item.CraftingRecipes;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.Items;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void localStorageTransfersStacksBothWays() {
        Hotbar hotbar = new Hotbar();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(36, ItemStack.EMPTY));
        slots.set(0, new ItemStack((short) 2, 4));
        hotbar.applySnapshot(slots);
        hotbar.openStorage(1, 70, -2);

        assertTrue(hotbar.transferStorage(false, 0));
        assertTrue(hotbar.slotView(0).isEmpty());
        assertEquals("voxel:dirt", hotbar.storageSlotView(0).itemKey());
        assertEquals(4, hotbar.storageSlotView(0).count());

        assertTrue(hotbar.transferStorage(true, 0));
        assertEquals("voxel:dirt", hotbar.slotView(0).itemKey());
        assertEquals(4, hotbar.slotView(0).count());
        assertTrue(hotbar.storageSlotView(0).isEmpty());
    }

    @Test
    void quickMoveMovesBackpackStackIntoHotbar() {
        Hotbar hotbar = new Hotbar();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(36, ItemStack.EMPTY));
        slots.set(9, new ItemStack((short) 23, 4));
        hotbar.applySnapshot(slots);

        assertTrue(hotbar.quickMoveInventorySlot(9));

        assertEquals("voxel:stick", hotbar.slotView(0).itemKey());
        assertEquals(4, hotbar.slotView(0).count());
        assertTrue(hotbar.slotView(9).isEmpty());
    }

    @Test
    void quickMoveMergesHotbarStackIntoBackpack() {
        Hotbar hotbar = new Hotbar();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(36, ItemStack.EMPTY));
        slots.set(0, new ItemStack((short) 23, 4));
        slots.set(9, new ItemStack((short) 23, 60));
        hotbar.applySnapshot(slots);

        assertTrue(hotbar.quickMoveInventorySlot(0));

        assertTrue(hotbar.slotView(0).isEmpty());
        assertEquals("voxel:stick", hotbar.slotView(9).itemKey());
        assertEquals(64, hotbar.slotView(9).count());
    }

    @Test
    void dragMoveMergesStacksIntoTargetSlot() {
        Hotbar hotbar = new Hotbar();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(36, ItemStack.EMPTY));
        slots.set(0, new ItemStack((short) 23, 10));
        slots.set(9, new ItemStack((short) 23, 60));
        hotbar.applySnapshot(slots);

        assertTrue(hotbar.moveInventorySlot(0, 9));

        assertEquals("voxel:stick", hotbar.slotView(9).itemKey());
        assertEquals(64, hotbar.slotView(9).count());
        assertEquals("voxel:stick", hotbar.slotView(0).itemKey());
        assertEquals(6, hotbar.slotView(0).count());
    }

    @Test
    void dragMoveSwapsDifferentStacks() {
        Hotbar hotbar = new Hotbar();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(36, ItemStack.EMPTY));
        slots.set(0, new ItemStack((short) 2, 4));
        slots.set(1, new ItemStack((short) 7, 3));
        hotbar.applySnapshot(slots);

        assertTrue(hotbar.moveInventorySlot(0, 1));

        assertEquals("voxel:torch", hotbar.slotView(0).itemKey());
        assertEquals(3, hotbar.slotView(0).count());
        assertEquals("voxel:dirt", hotbar.slotView(1).itemKey());
        assertEquals(4, hotbar.slotView(1).count());
    }

    @Test
    void trashClearsInventorySlot() {
        Hotbar hotbar = new Hotbar();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(36, ItemStack.EMPTY));
        slots.set(4, new ItemStack((short) 25, 3));
        hotbar.applySnapshot(slots);

        assertTrue(hotbar.trashInventorySlot(4));

        assertTrue(hotbar.slotView(4).isEmpty());
    }

    @Test
    void newlyCollectedItemsDiscoverRecipes() {
        Hotbar hotbar = new Hotbar();
        hotbar.resetForNewGame();

        assertTrue(hotbar.discoveredRecipes().stream().noneMatch(recipe -> recipe.key().equals("voxel:cooked_berries")));

        assertTrue(hotbar.addItem("voxel:berries", 1));

        assertTrue(hotbar.discoveredRecipes().stream().anyMatch(recipe -> recipe.key().equals("voxel:cooked_berries")));
    }

    @Test
    void findsInputSlotsForCookingIntent() {
        Hotbar hotbar = new Hotbar();
        hotbar.resetForNewGame();
        CraftingRecipe charcoal = CraftingRecipes.createDefaultRecipes(Items.createDefaultRegistry()).stream()
                .filter(recipe -> recipe.key().equals("voxel:charcoal"))
                .findFirst()
                .orElseThrow();

        assertEquals(List.of(2), hotbar.inputSlotsFor(charcoal).orElseThrow());
    }

    @Test
    void selectedToolRespectsMiningTierGate() {
        Hotbar hotbar = new Hotbar();
        hotbar.resetForNewGame();
        hotbar.scroll(-1);
        BlockType copperOre = Blocks.createDefaultRegistry().requireByKey("voxel:copper_ore");
        BlockType ironOre = Blocks.createDefaultRegistry().requireByKey("voxel:iron_ore");

        assertTrue(hotbar.canHarvestSelected(copperOre));
        assertFalse(hotbar.canHarvestSelected(ironOre));
    }

    @Test
    void invalidSlotAccessorsAreUiSafe() {
        Hotbar hotbar = new Hotbar();
        hotbar.resetForNewGame();

        assertEquals("Invalid slot", hotbar.slotLabel(-1));
        assertEquals("Invalid slot", hotbar.inventorySlotLabel(36));
        assertTrue(hotbar.slotView(999).isEmpty());
    }

    @Test
    void unknownItemIdInSnapshotDoesNotBreakHudLabelsOrViews() {
        Hotbar hotbar = new Hotbar();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(36, ItemStack.EMPTY));
        slots.set(0, new ItemStack((short) 999, 2));
        hotbar.applySnapshot(slots);

        assertEquals("Unknown Item x2", hotbar.selectedLabel());
        assertTrue(hotbar.slotLabel(0).contains("Unknown Item"));
        assertEquals("unknown:999", hotbar.slotView(0).itemKey());
        assertEquals("unknown:999", hotbar.selectedItemKey().orElseThrow());
        assertTrue(hotbar.selectedTooltip().contains("Unrecognized item id 999"));
    }
}
