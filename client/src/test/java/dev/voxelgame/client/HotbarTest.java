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
    void storageCloseOnlyClosesMatchingOpenCrate() {
        Hotbar hotbar = new Hotbar();
        hotbar.openStorage(1, 70, -2);

        hotbar.closeStorage(2, 70, -2);
        assertTrue(hotbar.storageOpen());

        hotbar.closeStorage(1, 70, -2);
        assertFalse(hotbar.storageOpen());
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
    void splitStackMovesHalfToFirstEmptySlot() {
        Hotbar hotbar = new Hotbar();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(36, ItemStack.EMPTY));
        slots.set(0, new ItemStack((short) 23, 9));
        hotbar.applySnapshot(slots);

        assertTrue(hotbar.splitInventorySlot(0));

        assertEquals(5, hotbar.slotView(0).count());
        assertEquals("voxel:stick", hotbar.slotView(1).itemKey());
        assertEquals(4, hotbar.slotView(1).count());
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
    void toolSlotViewExposesComparisonAndRepairData() {
        Hotbar hotbar = new Hotbar();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(36, ItemStack.EMPTY));
        slots.set(0, new ItemStack(itemId("voxel:stone_pickaxe"), 1, 20));
        slots.set(1, new ItemStack(itemId("voxel:copper_pickaxe"), 1, 12));
        slots.set(2, new ItemStack(itemId("voxel:iron_pickaxe"), 1, 18));
        slots.set(3, new ItemStack(itemId("voxel:crystal_pickaxe"), 1, 24));
        hotbar.applySnapshot(slots);

        Hotbar.SlotView stone = hotbar.slotView(0);
        Hotbar.SlotView copper = hotbar.slotView(1);
        Hotbar.SlotView iron = hotbar.slotView(2);
        Hotbar.SlotView crystal = hotbar.slotView(3);

        assertEquals("Pickaxe", stone.toolTypeLabel());
        assertEquals(1, stone.toolLevel());
        assertEquals(1.0f, stone.toolSpeed(), 0.001f);
        assertEquals("stone or pebble", stone.repairMaterialLabel());
        assertEquals(112, stone.durabilityLeft());

        assertEquals("Pickaxe", copper.toolTypeLabel());
        assertEquals(2, copper.toolLevel());
        assertTrue(copper.toolSpeed() > stone.toolSpeed());
        assertEquals("copper ingot", copper.repairMaterialLabel());
        assertTrue(copper.durabilityLeft() > stone.durabilityLeft());

        assertEquals(3, iron.toolLevel());
        assertTrue(iron.toolSpeed() > copper.toolSpeed());
        assertEquals("iron ingot", iron.repairMaterialLabel());

        assertEquals(4, crystal.toolLevel());
        assertTrue(crystal.toolSpeed() > iron.toolSpeed());
        assertEquals("glow crystal", crystal.repairMaterialLabel());
    }

    @Test
    void selectedTooltipIncludesToolSpeedAndRepairMaterial() {
        Hotbar hotbar = new Hotbar();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(36, ItemStack.EMPTY));
        slots.set(0, new ItemStack(itemId("voxel:copper_pickaxe"), 1, 12));
        hotbar.applySnapshot(slots);

        String tooltip = hotbar.selectedTooltip();

        assertTrue(tooltip.contains("Pickaxe Level 2"));
        assertTrue(tooltip.contains("Speed x1.25"));
        assertTrue(tooltip.contains("Durability 228/240"));
        assertTrue(tooltip.contains("Repair: copper ingot"));
    }

    @Test
    void waterContainerRecipeNamesLakesideReeds() {
        Hotbar hotbar = new Hotbar();
        CraftingRecipe waterContainer = hotbar.recipes().stream()
                .filter(recipe -> recipe.key().equals("voxel:water_container"))
                .findFirst()
                .orElseThrow();

        String summary = hotbar.recipeSummary(waterContainer);

        assertTrue(summary.contains("Clay pot x1"));
        assertTrue(summary.contains("Reed bundle x1"));
    }

    @Test
    void copperToolRecipesNamePineForestMaterials() {
        Hotbar hotbar = new Hotbar();
        CraftingRecipe copperPickaxe = hotbar.recipes().stream()
                .filter(recipe -> recipe.key().equals("voxel:copper_pickaxe"))
                .findFirst()
                .orElseThrow();

        String summary = hotbar.recipeSummary(copperPickaxe);

        assertTrue(summary.contains("Copper ingot x3"));
        assertTrue(summary.contains("Tool handle x2"));
        assertTrue(summary.contains("Resin x1"));
    }

    @Test
    void ancientLanternRecipeConnectsRuinLootToComfortLight() {
        Hotbar hotbar = new Hotbar();
        CraftingRecipe ancientLantern = hotbar.recipes().stream()
                .filter(recipe -> recipe.key().equals("voxel:ancient_lantern"))
                .findFirst()
                .orElseThrow();

        String summary = hotbar.recipeSummary(ancientLantern);

        assertTrue(summary.contains("Ancient fragment x2"));
        assertTrue(summary.contains("Glow crystal x1"));
        assertTrue(summary.contains("Copper ingot x1"));
    }

    @Test
    void crystalToolRecipesNameLateGameIngredients() {
        Hotbar hotbar = new Hotbar();
        CraftingRecipe crystalPickaxe = hotbar.recipes().stream()
                .filter(recipe -> recipe.key().equals("voxel:crystal_pickaxe"))
                .findFirst()
                .orElseThrow();

        String summary = hotbar.recipeSummary(crystalPickaxe);

        assertTrue(summary.contains("Forge"));
        assertTrue(summary.contains("Iron pickaxe x1"));
        assertTrue(summary.contains("Glow crystal x3"));
        assertTrue(summary.contains("Ruin seal x1"));
        assertTrue(summary.contains("Leather strip x1"));
    }

    @Test
    void glowMushroomStewRecipeNamesMushroomGroveIngredients() {
        Hotbar hotbar = new Hotbar();
        CraftingRecipe glowStew = hotbar.recipes().stream()
                .filter(recipe -> recipe.key().equals("voxel:glow_mushroom_stew"))
                .findFirst()
                .orElseThrow();

        String summary = hotbar.recipeSummary(glowStew);

        assertTrue(summary.contains("Cooking Pot"));
        assertTrue(summary.contains("Glow mushroom cap x2"));
        assertTrue(summary.contains("Mushroom x1"));
        assertTrue(summary.contains("Water container x1"));
        assertTrue(summary.contains("Clay bowl x1"));
    }

    @Test
    void sporeTeaRecipeNamesMushroomCircleIngredients() {
        Hotbar hotbar = new Hotbar();
        CraftingRecipe sporeTea = hotbar.recipes().stream()
                .filter(recipe -> recipe.key().equals("voxel:spore_tea"))
                .findFirst()
                .orElseThrow();

        String summary = hotbar.recipeSummary(sporeTea);

        assertTrue(summary.contains("Cooking Pot"));
        assertTrue(summary.contains("Spore blossom x1"));
        assertTrue(summary.contains("Glow mushroom cap x1"));
        assertTrue(summary.contains("Water container x1"));
        assertTrue(summary.contains("Clay bowl x1"));
    }

    @Test
    void slotTooltipsExplainReedsAndUseActualComfortValues() {
        Hotbar hotbar = new Hotbar();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(36, ItemStack.EMPTY));
        slots.set(0, new ItemStack(itemId("voxel:reed_bundle"), 2));
        slots.set(1, new ItemStack(itemId("voxel:bark_strip"), 2));
        slots.set(2, new ItemStack(itemId("voxel:tool_handle"), 1));
        slots.set(3, new ItemStack(itemId("voxel:resin_torch"), 4));
        slots.set(4, new ItemStack(itemId("voxel:sleeping_mat"), 1));
        slots.set(5, new ItemStack(itemId("voxel:storage_crate"), 1));
        slots.set(6, new ItemStack(itemId("voxel:berry_bush"), 1));
        slots.set(7, new ItemStack(itemId("voxel:ancient_lantern"), 1));
        slots.set(8, new ItemStack(itemId("voxel:crystal_knife"), 1));
        hotbar.applySnapshot(slots);

        assertEquals("Seals water containers and burns briefly", hotbar.slotView(0).description());
        assertEquals("Fuel, torch wrap, and tool handle material", hotbar.slotView(1).description());
        assertEquals("Reinforced handle for copper tools", hotbar.slotView(2).description());
        hotbar.scroll(1);
        hotbar.scroll(1);
        hotbar.scroll(1);
        assertEquals(Blocks.TORCH, hotbar.selectedPlaceBlockId().orElseThrow());
        assertEquals(4, hotbar.slotView(4).comfortValue());
        assertEquals(1, hotbar.slotView(5).comfortValue());
        assertEquals(0, hotbar.slotView(6).comfortValue());
        assertEquals("Restored ruin light for cozy bases", hotbar.slotView(7).description());
        assertEquals(4, hotbar.slotView(7).comfortValue());
        assertEquals("Sharp glow tool for richer plant harvests", hotbar.slotView(8).description());
        assertEquals("glow crystal", hotbar.slotView(8).repairMaterialLabel());
    }

    @Test
    void glowMushroomFoodTooltipsExplainBiomeUse() {
        Hotbar hotbar = new Hotbar();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(36, ItemStack.EMPTY));
        slots.set(0, new ItemStack(itemId("voxel:glow_mushroom_cap"), 2));
        slots.set(1, new ItemStack(itemId("voxel:glow_mushroom_stew"), 1));
        slots.set(2, new ItemStack(itemId("voxel:spore_blossom"), 1));
        slots.set(3, new ItemStack(itemId("voxel:spore_tea"), 1));
        hotbar.applySnapshot(slots);

        assertEquals("Glowing mushroom grove ingredient", hotbar.slotView(0).description());
        assertEquals("Rich glow food from mushroom groves", hotbar.slotView(1).description());
        assertEquals(9, hotbar.slotView(1).foodValue());
        assertEquals(4, hotbar.slotView(1).healValue());
        assertEquals("Rare herb from mushroom circles", hotbar.slotView(2).description());
        assertEquals("Restorative tea from grove spores", hotbar.slotView(3).description());
        assertEquals(5, hotbar.slotView(3).foodValue());
        assertEquals(6, hotbar.slotView(3).healValue());
    }

    private static short itemId(String key) {
        return Items.createDefaultRegistry().requireByKey(key).id();
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
