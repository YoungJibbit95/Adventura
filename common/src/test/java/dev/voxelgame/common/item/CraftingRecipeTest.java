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
}
