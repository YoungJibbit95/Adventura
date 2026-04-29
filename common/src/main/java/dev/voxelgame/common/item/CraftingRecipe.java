package dev.voxelgame.common.item;

import dev.voxelgame.common.registry.Registry;

import java.util.List;
import java.util.Objects;

public record CraftingRecipe(
        String key,
        String label,
        List<Ingredient> ingredients,
        ItemStack result,
        CraftingStationType stationType,
        RecipeUnlock unlockCondition,
        int craftingTimeTicks,
        int outputExperience,
        CraftingCategory category
) {
    public CraftingRecipe(String key, String label, List<Ingredient> ingredients, ItemStack result) {
        this(key, label, ingredients, result, CraftingStationType.INVENTORY, RecipeUnlock.ALWAYS, 0, 0, CraftingCategory.BASIC);
    }

    public CraftingRecipe {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(label, "label");
        ingredients = List.copyOf(ingredients);
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(stationType, "stationType");
        Objects.requireNonNull(unlockCondition, "unlockCondition");
        Objects.requireNonNull(category, "category");
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("Recipe needs at least one ingredient");
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Recipe result cannot be empty");
        }
        if (craftingTimeTicks < 0 || outputExperience < 0) {
            throw new IllegalArgumentException("Recipe timing and experience must be >= 0");
        }
    }

    public boolean canCraft(Inventory inventory, Registry<ItemType> items) {
        return canCraft(inventory, items, CraftingStationType.INVENTORY);
    }

    public boolean canCraft(Inventory inventory, Registry<ItemType> items, CraftingStationType availableStation) {
        return canCraft(inventory, items, availableStation, 1);
    }

    public boolean canCraft(Inventory inventory, Registry<ItemType> items, CraftingStationType availableStation, int count) {
        if (count < 1) {
            return false;
        }
        if (!isAvailableAt(availableStation)) {
            return false;
        }
        Inventory simulated = inventory.copy();
        for (int crafted = 0; crafted < count; crafted++) {
            for (Ingredient ingredient : ingredients) {
                if (!simulated.remove(ingredient.itemId(), ingredient.count())) {
                    return false;
                }
            }
        }
        for (int crafted = 0; crafted < count; crafted++) {
            if (simulated.add(result.itemId(), result.count(), items) != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean craft(Inventory inventory, Registry<ItemType> items) {
        return craft(inventory, items, CraftingStationType.INVENTORY);
    }

    public boolean craft(Inventory inventory, Registry<ItemType> items, CraftingStationType availableStation) {
        return craft(inventory, items, availableStation, 1);
    }

    public boolean craft(Inventory inventory, Registry<ItemType> items, CraftingStationType availableStation, int count) {
        if (!canCraft(inventory, items, availableStation, count)) {
            return false;
        }
        for (int crafted = 0; crafted < count; crafted++) {
            for (Ingredient ingredient : ingredients) {
                if (!inventory.remove(ingredient.itemId(), ingredient.count())) {
                    return false;
                }
            }
        }
        for (int crafted = 0; crafted < count; crafted++) {
            if (inventory.add(result.itemId(), result.count(), items) != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isAvailableAt(CraftingStationType availableStation) {
        return stationType == CraftingStationType.INVENTORY || stationType == availableStation;
    }

    public record Ingredient(short itemId, int count) {
        public Ingredient {
            if (itemId == 0 || count < 1) {
                throw new IllegalArgumentException("Ingredient must have an item and positive count");
            }
        }
    }
}
