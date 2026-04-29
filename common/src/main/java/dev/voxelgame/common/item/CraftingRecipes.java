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
        short twig = items.requireByKey("voxel:twig").id();
        short pebble = items.requireByKey("voxel:pebble").id();
        short fiber = items.requireByKey("voxel:fiber").id();
        short rope = items.requireByKey("voxel:simple_rope").id();
        short knife = items.requireByKey("voxel:stone_knife").id();
        short berries = items.requireByKey("voxel:berries").id();
        short herbs = items.requireByKey("voxel:wild_herbs").id();
        short snack = items.requireByKey("voxel:healing_snack").id();
        short clay = items.requireByKey("voxel:clay").id();
        short campfire = items.requireByKey("voxel:campfire").id();
        short storageCrate = items.requireByKey("voxel:storage_crate").id();
        short flowerPot = items.requireByKey("voxel:flower_pot").id();
        short lantern = items.requireByKey("voxel:lantern").id();
        short rug = items.requireByKey("voxel:woven_rug").id();
        short table = items.requireByKey("voxel:small_table").id();
        short chair = items.requireByKey("voxel:wooden_chair").id();
        short fence = items.requireByKey("voxel:garden_fence").id();
        short herbPlanter = items.requireByKey("voxel:herb_planter").id();
        short berryBush = items.requireByKey("voxel:berry_bush").id();
        short mossyPath = items.requireByKey("voxel:mossy_path").id();
        short mossyStone = items.requireByKey("voxel:mossy_stone").id();

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
                        "voxel:simple_rope",
                        "Twist Simple Rope",
                        List.of(new CraftingRecipe.Ingredient(twig, 2), new CraftingRecipe.Ingredient(fiber, 3)),
                        new ItemStack(rope, 2)
                ),
                new CraftingRecipe(
                        "voxel:stone_knife",
                        "Craft Stone Knife",
                        List.of(new CraftingRecipe.Ingredient(pebble, 2), new CraftingRecipe.Ingredient(twig, 1), new CraftingRecipe.Ingredient(fiber, 1)),
                        new ItemStack(knife, 1)
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
                        List.of(new CraftingRecipe.Ingredient(stone, 2), new CraftingRecipe.Ingredient(stick, 2)),
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
                ),
                new CraftingRecipe(
                        "voxel:campfire",
                        "Build Campfire",
                        List.of(new CraftingRecipe.Ingredient(stone, 3), new CraftingRecipe.Ingredient(twig, 4)),
                        new ItemStack(campfire, 1)
                ),
                new CraftingRecipe(
                        "voxel:healing_snack",
                        "Mix Healing Snack",
                        List.of(new CraftingRecipe.Ingredient(herbs, 2), new CraftingRecipe.Ingredient(berries, 2)),
                        new ItemStack(snack, 1)
                ),
                new CraftingRecipe(
                        "voxel:storage_crate",
                        "Build Simple Chest",
                        List.of(new CraftingRecipe.Ingredient(planks, 6), new CraftingRecipe.Ingredient(fiber, 2)),
                        new ItemStack(storageCrate, 1)
                ),
                new CraftingRecipe(
                        "voxel:flower_pot",
                        "Fire Clay Pot",
                        List.of(new CraftingRecipe.Ingredient(clay, 3), new CraftingRecipe.Ingredient(coal, 1)),
                        new ItemStack(flowerPot, 1)
                ),
                new CraftingRecipe(
                        "voxel:lantern",
                        "Assemble Lantern",
                        List.of(new CraftingRecipe.Ingredient(torch, 1), new CraftingRecipe.Ingredient(rawCopperOrPebble(items), 2)),
                        new ItemStack(lantern, 1)
                ),
                new CraftingRecipe(
                        "voxel:woven_rug",
                        "Weave Rug",
                        List.of(new CraftingRecipe.Ingredient(fiber, 6), new CraftingRecipe.Ingredient(berries, 1)),
                        new ItemStack(rug, 1)
                ),
                new CraftingRecipe(
                        "voxel:small_table",
                        "Build Small Table",
                        List.of(new CraftingRecipe.Ingredient(planks, 4), new CraftingRecipe.Ingredient(stick, 2)),
                        new ItemStack(table, 1)
                ),
                new CraftingRecipe(
                        "voxel:wooden_chair",
                        "Build Wooden Chair",
                        List.of(new CraftingRecipe.Ingredient(planks, 3), new CraftingRecipe.Ingredient(stick, 2)),
                        new ItemStack(chair, 1)
                ),
                new CraftingRecipe(
                        "voxel:garden_fence",
                        "Build Garden Fence",
                        List.of(new CraftingRecipe.Ingredient(stick, 6), new CraftingRecipe.Ingredient(rope, 1)),
                        new ItemStack(fence, 4)
                ),
                new CraftingRecipe(
                        "voxel:herb_planter",
                        "Plant Herb Box",
                        List.of(new CraftingRecipe.Ingredient(flowerPot, 1), new CraftingRecipe.Ingredient(herbs, 1)),
                        new ItemStack(herbPlanter, 1)
                ),
                new CraftingRecipe(
                        "voxel:berry_bush",
                        "Plant Berry Bush",
                        List.of(new CraftingRecipe.Ingredient(berries, 2), new CraftingRecipe.Ingredient(fiber, 1)),
                        new ItemStack(berryBush, 1)
                ),
                new CraftingRecipe(
                        "voxel:mossy_path",
                        "Lay Mossy Path",
                        List.of(new CraftingRecipe.Ingredient(mossyStone, 1), new CraftingRecipe.Ingredient(fiber, 1)),
                        new ItemStack(mossyPath, 2)
                )
        );
    }

    private static short rawCopperOrPebble(Registry<ItemType> items) {
        return items.findByKey("voxel:raw_copper")
                .or(() -> items.findByKey("voxel:pebble"))
                .orElseThrow()
                .id();
    }
}
