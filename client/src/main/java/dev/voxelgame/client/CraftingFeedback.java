package dev.voxelgame.client;

import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.item.CraftingStationType;

import java.util.Locale;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

final class CraftingFeedback {
    private CraftingFeedback() {
    }

    static String stationUnlockMessage(CraftingStationType stationType) {
        return switch (stationType) {
            case CAMPFIRE -> "Campfire recipes available. Add fuel to cook.";
            case COOKING_POT -> "Cooking pot recipes available. Bring water and a bowl.";
            case CRAFTING_TABLE -> "Crafting table recipes available.";
            case WORKBENCH -> "Workbench recipes available.";
            case FORGE -> "Forge recipes available. Smelt iron and bind ruin seals.";
            case INVENTORY -> "Inventory recipes available.";
        };
    }

    static String missingStationMessage(CraftingStationType stationType) {
        return switch (stationType) {
            case CAMPFIRE -> "Light a campfire to cook this";
            case COOKING_POT -> "Stand near a cooking pot";
            case CRAFTING_TABLE -> "Use a crafting table";
            case WORKBENCH -> "Use a workbench";
            case FORGE -> "Use a forge";
            case INVENTORY -> "Open your inventory";
        };
    }

    static String missingIngredientMessage(
            CraftingRecipe recipe,
            IntUnaryOperator itemCount,
            IntFunction<String> itemLabel
    ) {
        for (CraftingRecipe.Ingredient ingredient : recipe.ingredients()) {
            int owned = itemCount.applyAsInt(ingredient.itemId());
            if (owned < ingredient.count()) {
                int missing = ingredient.count() - owned;
                return "Need " + itemLabel.apply(ingredient.itemId()) + " x" + missing;
            }
        }
        return "Inventory full";
    }

    static String cookingPotNeedLine(CraftingRecipe recipe, IntFunction<String> itemKey) {
        if (recipe == null) {
            return "No matching recipe";
        }
        boolean needsWater = false;
        boolean needsBowl = false;
        for (CraftingRecipe.Ingredient ingredient : recipe.ingredients()) {
            String key = itemKey.apply(ingredient.itemId()).toLowerCase(Locale.ROOT);
            needsWater |= "voxel:water_container".equals(key);
            needsBowl |= "voxel:clay_bowl".equals(key);
        }
        if (needsWater && needsBowl) {
            return "Needs water + bowl";
        }
        if (needsWater) {
            return "Needs water container";
        }
        if (needsBowl) {
            return "Needs clay bowl";
        }
        return "Needs ingredients";
    }
}
