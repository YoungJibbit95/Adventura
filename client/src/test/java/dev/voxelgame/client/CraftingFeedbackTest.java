package dev.voxelgame.client;

import dev.voxelgame.common.item.CraftingCategory;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.item.CraftingStationType;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.RecipeUnlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CraftingFeedbackTest {
    private static final short HERBS = 1;
    private static final short WATER = 2;
    private static final short BOWL = 3;
    private static final short SOUP = 4;

    @Test
    void stationUnlockMessagesExplainWhatChanged() {
        assertEquals("Campfire recipes available. Add fuel to cook.", CraftingFeedback.stationUnlockMessage(CraftingStationType.CAMPFIRE));
        assertEquals("Cooking pot recipes available. Bring water and a bowl.", CraftingFeedback.stationUnlockMessage(CraftingStationType.COOKING_POT));
        assertEquals("Forge recipes available. Smelt iron and bind ruin seals.", CraftingFeedback.stationUnlockMessage(CraftingStationType.FORGE));
    }

    @Test
    void missingIngredientMessageNamesFirstMissingIngredient() {
        CraftingRecipe recipe = soupRecipe();
        Map<Integer, Integer> counts = Map.of((int) HERBS, 2, (int) WATER, 0, (int) BOWL, 1);

        assertEquals(
                "Need water container x1",
                CraftingFeedback.missingIngredientMessage(recipe, item -> counts.getOrDefault(item, 0), CraftingFeedbackTest::itemLabel)
        );
    }

    @Test
    void missingIngredientMessageFallsBackToInventoryFullWhenIngredientsExist() {
        CraftingRecipe recipe = soupRecipe();
        Map<Integer, Integer> counts = Map.of((int) HERBS, 2, (int) WATER, 1, (int) BOWL, 1);

        assertEquals(
                "Inventory full",
                CraftingFeedback.missingIngredientMessage(recipe, item -> counts.getOrDefault(item, 0), CraftingFeedbackTest::itemLabel)
        );
    }

    @Test
    void cookingPotNeedLineCallsOutWaterAndBowl() {
        assertEquals("Needs water + bowl", CraftingFeedback.cookingPotNeedLine(soupRecipe(), CraftingFeedbackTest::itemKey));
    }

    private static CraftingRecipe soupRecipe() {
        return new CraftingRecipe(
                "voxel:herb_soup",
                "Cook Herb Soup",
                List.of(
                        new CraftingRecipe.Ingredient(HERBS, 2),
                        new CraftingRecipe.Ingredient(WATER, 1),
                        new CraftingRecipe.Ingredient(BOWL, 1)
                ),
                new ItemStack(SOUP, 1),
                CraftingStationType.COOKING_POT,
                RecipeUnlock.NEAR_STATION,
                120,
                0,
                CraftingCategory.FOOD
        );
    }

    private static String itemKey(int itemId) {
        return switch ((short) itemId) {
            case WATER -> "voxel:water_container";
            case BOWL -> "voxel:clay_bowl";
            case HERBS -> "voxel:wild_herbs";
            default -> "voxel:unknown";
        };
    }

    private static String itemLabel(int itemId) {
        return switch ((short) itemId) {
            case WATER -> "water container";
            case BOWL -> "clay bowl";
            case HERBS -> "wild herbs";
            default -> "unknown";
        };
    }
}
