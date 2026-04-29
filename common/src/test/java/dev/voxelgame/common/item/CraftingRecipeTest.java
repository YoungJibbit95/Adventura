package dev.voxelgame.common.item;

import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingRecipeTest {
    @Test
    void craftsStonePickaxeFromInventory() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Inventory inventory = new Inventory(10);
        inventory.add(items.requireByKey("voxel:stone").id(), 3, items);
        inventory.add(items.requireByKey("voxel:stick").id(), 2, items);
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
        CraftingRecipe pickaxe = recipes.stream()
                .filter(recipe -> recipe.key().equals("voxel:stone_pickaxe"))
                .findFirst()
                .orElseThrow();

        assertTrue(pickaxe.craft(inventory, items));

        assertEquals(0, inventory.count(items.requireByKey("voxel:stone").id()));
        assertEquals(0, inventory.count(items.requireByKey("voxel:stick").id()));
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
}
