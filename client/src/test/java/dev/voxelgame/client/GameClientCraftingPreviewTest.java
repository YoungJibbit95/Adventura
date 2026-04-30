package dev.voxelgame.client;

import dev.voxelgame.common.item.CraftingCategory;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.item.CraftingStationType;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.RecipeUnlock;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameClientCraftingPreviewTest {
    @Test
    void previewUsesSelectedRecipeStationEvenWhenCookingPotIsPrimary() {
        CraftingRecipe campfireRecipe = recipe(CraftingStationType.CAMPFIRE);

        CraftingStationType station = GameClient.previewStationFor(
                campfireRecipe,
                EnumSet.of(CraftingStationType.INVENTORY, CraftingStationType.COOKING_POT)
        );

        assertEquals(CraftingStationType.CAMPFIRE, station);
    }

    @Test
    void previewFallsBackToNearestPrimaryStationWithoutRecipe() {
        CraftingStationType station = GameClient.previewStationFor(
                null,
                EnumSet.of(CraftingStationType.INVENTORY, CraftingStationType.CAMPFIRE, CraftingStationType.COOKING_POT)
        );

        assertEquals(CraftingStationType.COOKING_POT, station);
    }

    @Test
    void previewFallsBackToForgeWhenLateGameStationIsNearby() {
        CraftingStationType station = GameClient.previewStationFor(
                null,
                EnumSet.of(CraftingStationType.INVENTORY, CraftingStationType.CAMPFIRE, CraftingStationType.COOKING_POT, CraftingStationType.FORGE)
        );

        assertEquals(CraftingStationType.FORGE, station);
    }

    @Test
    void previewKeepsInventoryRecipeInInventoryPanel() {
        CraftingRecipe inventoryRecipe = recipe(CraftingStationType.INVENTORY);

        CraftingStationType station = GameClient.previewStationFor(
                inventoryRecipe,
                EnumSet.of(CraftingStationType.INVENTORY, CraftingStationType.COOKING_POT)
        );

        assertEquals(CraftingStationType.INVENTORY, station);
    }

    private static CraftingRecipe recipe(CraftingStationType stationType) {
        return new CraftingRecipe(
                "voxel:test_recipe",
                "Test Recipe",
                List.of(new CraftingRecipe.Ingredient((short) 1, 1)),
                new ItemStack((short) 2, 1),
                stationType,
                RecipeUnlock.ALWAYS,
                stationType == CraftingStationType.INVENTORY ? 0 : 60,
                0,
                CraftingCategory.BASIC
        );
    }
}
