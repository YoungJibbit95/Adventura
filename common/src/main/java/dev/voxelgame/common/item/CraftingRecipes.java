package dev.voxelgame.common.item;

import dev.voxelgame.common.registry.Registry;

import java.util.List;

public final class CraftingRecipes {
    private CraftingRecipes() {
    }

    public static List<CraftingRecipe> createDefaultRecipes(Registry<ItemType> items) {
        short stone = items.requireByKey("voxel:stone").id();
        short log = items.requireByKey("voxel:skyroot_log").id();
        short planks = items.requireByKey("voxel:skyroot_planks").id();
        short stick = items.requireByKey("voxel:stick").id();
        short coal = items.requireByKey("voxel:coal").id();
        short torch = items.requireByKey("voxel:torch").id();
        short pickaxe = items.requireByKey("voxel:stone_pickaxe").id();
        short axe = items.requireByKey("voxel:stone_axe").id();
        short shovel = items.requireByKey("voxel:stone_shovel").id();
        short sword = items.requireByKey("voxel:stone_sword").id();

        return List.of(
                new CraftingRecipe(
                        "voxel:skyroot_planks",
                        "Craft Planks",
                        List.of(new CraftingRecipe.Ingredient(log, 1)),
                        new ItemStack(planks, 4)
                ),
                new CraftingRecipe(
                        "voxel:stick",
                        "Craft Sticks",
                        List.of(new CraftingRecipe.Ingredient(planks, 2)),
                        new ItemStack(stick, 4)
                ),
                new CraftingRecipe(
                        "voxel:stone_pickaxe",
                        "Craft Pickaxe",
                        List.of(new CraftingRecipe.Ingredient(stone, 3), new CraftingRecipe.Ingredient(stick, 2)),
                        new ItemStack(pickaxe, 1)
                ),
                new CraftingRecipe(
                        "voxel:stone_axe",
                        "Craft Axe",
                        List.of(new CraftingRecipe.Ingredient(stone, 3), new CraftingRecipe.Ingredient(stick, 2)),
                        new ItemStack(axe, 1)
                ),
                new CraftingRecipe(
                        "voxel:stone_shovel",
                        "Craft Shovel",
                        List.of(new CraftingRecipe.Ingredient(stone, 1), new CraftingRecipe.Ingredient(stick, 2)),
                        new ItemStack(shovel, 1)
                ),
                new CraftingRecipe(
                        "voxel:stone_sword",
                        "Craft Sword",
                        List.of(new CraftingRecipe.Ingredient(stone, 2), new CraftingRecipe.Ingredient(stick, 1)),
                        new ItemStack(sword, 1)
                ),
                new CraftingRecipe(
                        "voxel:torch",
                        "Craft Torches",
                        List.of(new CraftingRecipe.Ingredient(coal, 1), new CraftingRecipe.Ingredient(stick, 1)),
                        new ItemStack(torch, 4)
                )
        );
    }
}
