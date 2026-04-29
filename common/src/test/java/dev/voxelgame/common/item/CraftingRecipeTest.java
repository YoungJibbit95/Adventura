package dev.voxelgame.common.item;

import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingRecipeTest {
    @Test
    void craftsStonePickaxeFromInventory() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Inventory inventory = new Inventory(10);
        inventory.add(items.requireByKey("voxel:pebble").id(), 2, items);
        inventory.add(items.requireByKey("voxel:twig").id(), 1, items);
        inventory.add(items.requireByKey("voxel:fiber").id(), 1, items);
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
        CraftingRecipe pickaxe = recipes.stream()
                .filter(recipe -> recipe.key().equals("voxel:stone_pickaxe"))
                .findFirst()
                .orElseThrow();

        assertTrue(pickaxe.craft(inventory, items));

        assertEquals(0, inventory.count(items.requireByKey("voxel:pebble").id()));
        assertEquals(0, inventory.count(items.requireByKey("voxel:twig").id()));
        assertEquals(0, inventory.count(items.requireByKey("voxel:fiber").id()));
        assertEquals(1, inventory.count(items.requireByKey("voxel:stone_pickaxe").id()));
    }

    @Test
    void craftsPlanksAndSticks() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Inventory inventory = new Inventory(10);
        inventory.add(items.requireByKey("voxel:skyroot_log").id(), 1, items);
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);

        recipes.stream()
                .filter(recipe -> recipe.key().equals("voxel:skyroot_planks"))
                .findFirst()
                .orElseThrow()
                .craft(inventory, items);
        recipes.stream()
                .filter(recipe -> recipe.key().equals("voxel:stick"))
                .findFirst()
                .orElseThrow()
                .craft(inventory, items);

        assertEquals(2, inventory.count(items.requireByKey("voxel:skyroot_planks").id()));
        assertEquals(4, inventory.count(items.requireByKey("voxel:stick").id()));
    }

    @Test
    void campfireRecipesRequireCampfireStation() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Inventory inventory = new Inventory(10);
        inventory.add(items.requireByKey("voxel:berries").id(), 2, items);
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
        CraftingRecipe cookedBerries = recipes.stream()
                .filter(recipe -> recipe.key().equals("voxel:cooked_berries"))
                .findFirst()
                .orElseThrow();

        assertEquals(CraftingStationType.CAMPFIRE, cookedBerries.stationType());
        assertFalse(cookedBerries.canCraft(inventory, items));
        assertTrue(cookedBerries.canCraft(inventory, items, CraftingStationType.CAMPFIRE));
    }

    @Test
    void craftsSleepingMatForSleepIntent() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Inventory inventory = new Inventory(10);
        short dryGrass = items.requireByKey("voxel:dry_grass").id();
        short fiber = items.requireByKey("voxel:fiber").id();
        short sleepingMat = items.requireByKey("voxel:sleeping_mat").id();
        inventory.add(dryGrass, 3, items);
        inventory.add(fiber, 2, items);
        CraftingRecipe recipe = CraftingRecipes.createDefaultRecipes(items).stream()
                .filter(candidate -> candidate.key().equals("voxel:sleeping_mat"))
                .findFirst()
                .orElseThrow();

        assertTrue(recipe.craft(inventory, items));

        assertEquals(1, inventory.count(sleepingMat));
        assertEquals(0, inventory.count(dryGrass));
        assertEquals(0, inventory.count(fiber));
    }

    @Test
    void duplicateIngredientsConsumeTheRealTotal() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short twig = items.requireByKey("voxel:twig").id();
        short rope = items.requireByKey("voxel:simple_rope").id();
        Inventory inventory = new Inventory(4);
        inventory.add(twig, 1, items);
        CraftingRecipe recipe = new CraftingRecipe(
                "voxel:test_double_twig",
                "Test Double Twig",
                List.of(new CraftingRecipe.Ingredient(twig, 1), new CraftingRecipe.Ingredient(twig, 1)),
                new ItemStack(rope, 1)
        );

        assertFalse(recipe.canCraft(inventory, items));
    }

    @Test
    void craftsRequestedCountAtomically() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short pebble = items.requireByKey("voxel:pebble").id();
        short twig = items.requireByKey("voxel:twig").id();
        short fiber = items.requireByKey("voxel:fiber").id();
        short pickaxe = items.requireByKey("voxel:stone_pickaxe").id();
        Inventory inventory = new Inventory(10);
        inventory.add(pebble, 4, items);
        inventory.add(twig, 2, items);
        inventory.add(fiber, 2, items);
        CraftingRecipe recipe = CraftingRecipes.createDefaultRecipes(items).stream()
                .filter(candidate -> candidate.key().equals("voxel:stone_pickaxe"))
                .findFirst()
                .orElseThrow();

        assertTrue(recipe.craft(inventory, items, CraftingStationType.INVENTORY, 2));
        assertEquals(0, inventory.count(pebble));
        assertEquals(0, inventory.count(twig));
        assertEquals(0, inventory.count(fiber));
        assertEquals(2, inventory.count(pickaxe));
    }

    @Test
    void rejectsRequestedCountWhenOutputWouldNotFit() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short pebble = items.requireByKey("voxel:pebble").id();
        short twig = items.requireByKey("voxel:twig").id();
        short fiber = items.requireByKey("voxel:fiber").id();
        short dirt = items.requireByKey("voxel:dirt").id();
        Inventory inventory = new Inventory(4);
        inventory.add(pebble, 8, items);
        inventory.add(twig, 4, items);
        inventory.add(fiber, 4, items);
        inventory.add(dirt, 1, items);
        CraftingRecipe recipe = CraftingRecipes.createDefaultRecipes(items).stream()
                .filter(candidate -> candidate.key().equals("voxel:stone_pickaxe"))
                .findFirst()
                .orElseThrow();

        assertFalse(recipe.craft(inventory, items, CraftingStationType.INVENTORY, 4));
        assertEquals(8, inventory.count(pebble));
        assertEquals(4, inventory.count(twig));
        assertEquals(4, inventory.count(fiber));
    }

    @Test
    void countCraftingCanUseFreedIngredientSlotsForOutput() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short pebble = items.requireByKey("voxel:pebble").id();
        short twig = items.requireByKey("voxel:twig").id();
        short fiber = items.requireByKey("voxel:fiber").id();
        short pickaxe = items.requireByKey("voxel:stone_pickaxe").id();
        Inventory inventory = new Inventory(3);
        inventory.add(pebble, 4, items);
        inventory.add(twig, 2, items);
        inventory.add(fiber, 2, items);
        CraftingRecipe recipe = CraftingRecipes.createDefaultRecipes(items).stream()
                .filter(candidate -> candidate.key().equals("voxel:stone_pickaxe"))
                .findFirst()
                .orElseThrow();

        assertTrue(recipe.craft(inventory, items, CraftingStationType.INVENTORY, 2));
        assertEquals(2, inventory.count(pickaxe));
    }
}
