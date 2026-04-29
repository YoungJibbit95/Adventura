package dev.voxelgame.common.item;

import dev.voxelgame.common.registry.Registry;

import java.util.List;
import java.util.Objects;

public record CraftingRecipe(String key, String label, List<Ingredient> ingredients, ItemStack result) {
    public CraftingRecipe {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(label, "label");
        ingredients = List.copyOf(ingredients);
        Objects.requireNonNull(result, "result");
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("Recipe needs at least one ingredient");
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Recipe result cannot be empty");
        }
    }

    public boolean canCraft(Inventory inventory, Registry<ItemType> items) {
        for (Ingredient ingredient : ingredients) {
            if (!inventory.has(ingredient.itemId(), ingredient.count())) {
                return false;
            }
        }
        Inventory simulated = inventory.copy();
        for (Ingredient ingredient : ingredients) {
            simulated.remove(ingredient.itemId(), ingredient.count());
        }
        return simulated.canAdd(result.itemId(), result.count(), items);
    }

    public boolean craft(Inventory inventory, Registry<ItemType> items) {
        if (!canCraft(inventory, items)) {
            return false;
        }
        for (Ingredient ingredient : ingredients) {
            inventory.remove(ingredient.itemId(), ingredient.count());
        }
        return inventory.add(result.itemId(), result.count(), items) == 0;
    }

    public record Ingredient(short itemId, int count) {
        public Ingredient {
            if (itemId == 0 || count < 1) {
                throw new IllegalArgumentException("Ingredient must have an item and positive count");
            }
        }
    }
}
