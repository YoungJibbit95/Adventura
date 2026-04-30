package dev.voxelgame.common.item;

import dev.voxelgame.common.registry.Registry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CraftingRecipes {
    private CraftingRecipes() {
    }

    public static Optional<CraftingRecipe> findByKey(List<CraftingRecipe> recipes, Registry<ItemType> items, String recipeKey) {
        Objects.requireNonNull(recipes, "recipes");
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(recipeKey, "recipeKey");
        String canonicalRecipeKey = items.canonicalKey(recipeKey).orElse(recipeKey);
        return recipes.stream()
                .filter(recipe -> recipe.key().equals(canonicalRecipeKey))
                .findFirst();
    }

    public static List<CraftingRecipe> createDefaultRecipes(Registry<ItemType> items) {
        short stone = items.requireByKey("voxel:stone").id();
        short log = items.requireByKey("voxel:skyroot_log").id();
        short planks = items.requireByKey("voxel:skyroot_planks").id();
        short stick = items.requireByKey("voxel:stick").id();
        short coal = items.requireByKey("voxel:coal").id();
        short torch = items.requireByKey("voxel:torch").id();
        short resinTorch = items.requireByKey("voxel:resin_torch").id();
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
        short cookedBerries = items.requireByKey("voxel:cooked_berries").id();
        short mushroom = items.requireByKey("voxel:mushroom").id();
        short roastedMushroom = items.requireByKey("voxel:roasted_mushroom").id();
        short herbs = items.requireByKey("voxel:wild_herbs").id();
        short dryGrass = items.requireByKey("voxel:dry_grass").id();
        short snack = items.requireByKey("voxel:healing_snack").id();
        short barkStrip = items.requireByKey("voxel:bark_strip").id();
        short toolHandle = items.requireByKey("voxel:tool_handle").id();
        short clayLump = items.requireByKey("voxel:clay_lump").id();
        short clayBowl = items.requireByKey("voxel:clay_bowl").id();
        short clayPot = items.requireByKey("voxel:clay_pot").id();
        short cookingPot = items.requireByKey("voxel:cooking_pot").id();
        short mushroomStew = items.requireByKey("voxel:mushroom_stew").id();
        short herbSoup = items.requireByKey("voxel:herb_soup").id();
        short waterContainer = items.requireByKey("voxel:water_container").id();
        short reedBundle = items.requireByKey("voxel:reed_bundle").id();
        short berryJam = items.requireByKey("voxel:berry_jam").id();
        short calmingTea = items.requireByKey("voxel:calming_tea").id();
        short heartyStew = items.requireByKey("voxel:hearty_stew").id();
        short charcoal = items.requireByKey("voxel:charcoal").id();
        short rawCopper = items.requireByKey("voxel:raw_copper").id();
        short rawIron = items.requireByKey("voxel:raw_iron").id();
        short copperIngot = items.requireByKey("voxel:copper_ingot").id();
        short copperAxe = items.requireByKey("voxel:copper_axe").id();
        short copperPickaxe = items.requireByKey("voxel:copper_pickaxe").id();
        short ironIngot = items.requireByKey("voxel:iron_ingot").id();
        short ironAxe = items.requireByKey("voxel:iron_axe").id();
        short ironPickaxe = items.requireByKey("voxel:iron_pickaxe").id();
        short resin = items.requireByKey("voxel:resin").id();
        short campfire = items.requireByKey("voxel:campfire").id();
        short workbench = items.requireByKey("voxel:workbench").id();
        short forge = items.requireByKey("voxel:forge").id();
        short storageCrate = items.requireByKey("voxel:storage_crate").id();
        short sleepingMat = items.requireByKey("voxel:sleeping_mat").id();
        short flowerPot = items.requireByKey("voxel:flower_pot").id();
        short lantern = items.requireByKey("voxel:lantern").id();
        short ancientLantern = items.requireByKey("voxel:ancient_lantern").id();
        short ancientFragment = items.requireByKey("voxel:ancient_fragment").id();
        short glowCrystal = items.requireByKey("voxel:glow_crystal").id();
        short cloth = items.requireByKey("voxel:cloth").id();
        short leatherStrip = items.requireByKey("voxel:leather_strip").id();
        short honey = items.requireByKey("voxel:honey").id();
        short ruinKey = items.requireByKey("voxel:ruin_key").id();
        short ruinSeal = items.requireByKey("voxel:ruin_seal").id();
        short rug = items.requireByKey("voxel:woven_rug").id();
        short table = items.requireByKey("voxel:small_table").id();
        short chair = items.requireByKey("voxel:wooden_chair").id();
        short fence = items.requireByKey("voxel:garden_fence").id();
        short herbPlanter = items.requireByKey("voxel:herb_planter").id();
        short berryBush = items.requireByKey("voxel:berry_bush").id();
        short mossyPath = items.requireByKey("voxel:mossy_path").id();
        short mossyStone = items.requireByKey("voxel:mossy_stone").id();

        return List.of(
                recipe(
                        "voxel:skyroot_planks",
                        "Craft Planks",
                        List.of(new CraftingRecipe.Ingredient(log, 1)),
                        new ItemStack(planks, 4),
                        CraftingCategory.BASIC
                ),
                recipe(
                        "voxel:stick",
                        "Craft Sticks",
                        List.of(new CraftingRecipe.Ingredient(planks, 2)),
                        new ItemStack(stick, 4),
                        CraftingCategory.BASIC
                ),
                recipe(
                        "voxel:simple_rope",
                        "Twist Simple Rope",
                        List.of(new CraftingRecipe.Ingredient(twig, 1), new CraftingRecipe.Ingredient(fiber, 1)),
                        new ItemStack(rope, 1),
                        CraftingCategory.BASIC
                ),
                recipe(
                        "voxel:stone_knife",
                        "Craft Stone Knife",
                        List.of(new CraftingRecipe.Ingredient(pebble, 1), new CraftingRecipe.Ingredient(twig, 1), new CraftingRecipe.Ingredient(fiber, 1)),
                        new ItemStack(knife, 1),
                        CraftingCategory.TOOLS
                ),
                recipe(
                        "voxel:stone_pickaxe",
                        "Craft Pickaxe",
                        List.of(new CraftingRecipe.Ingredient(pebble, 2), new CraftingRecipe.Ingredient(twig, 1), new CraftingRecipe.Ingredient(fiber, 1)),
                        new ItemStack(pickaxe, 1),
                        CraftingCategory.TOOLS
                ),
                recipe(
                        "voxel:stone_axe",
                        "Craft Axe",
                        List.of(new CraftingRecipe.Ingredient(pebble, 1), new CraftingRecipe.Ingredient(twig, 1), new CraftingRecipe.Ingredient(rope, 1)),
                        new ItemStack(axe, 1),
                        CraftingCategory.TOOLS
                ),
                recipe(
                        "voxel:stone_shovel",
                        "Craft Shovel",
                        List.of(new CraftingRecipe.Ingredient(stone, 1), new CraftingRecipe.Ingredient(stick, 2)),
                        new ItemStack(shovel, 1),
                        CraftingCategory.TOOLS
                ),
                recipe(
                        "voxel:stone_sword",
                        "Craft Sword",
                        List.of(new CraftingRecipe.Ingredient(stone, 2), new CraftingRecipe.Ingredient(stick, 1)),
                        new ItemStack(sword, 1),
                        CraftingCategory.TOOLS
                ),
                recipe(
                        "voxel:torch",
                        "Craft Torches",
                        List.of(new CraftingRecipe.Ingredient(coal, 1), new CraftingRecipe.Ingredient(stick, 1)),
                        new ItemStack(torch, 4),
                        CraftingCategory.BASIC
                ),
                recipe(
                        "voxel:resin_torch",
                        "Wrap Resin Torches",
                        List.of(new CraftingRecipe.Ingredient(stick, 1), new CraftingRecipe.Ingredient(resin, 1), new CraftingRecipe.Ingredient(barkStrip, 1)),
                        new ItemStack(resinTorch, 4),
                        CraftingCategory.BASIC
                ),
                recipe(
                        "voxel:tool_handle",
                        "Carve Tool Handle",
                        List.of(new CraftingRecipe.Ingredient(stick, 1), new CraftingRecipe.Ingredient(barkStrip, 2)),
                        new ItemStack(toolHandle, 1),
                        CraftingCategory.TOOLS
                ),
                recipe(
                        "voxel:campfire",
                        "Build Campfire",
                        List.of(new CraftingRecipe.Ingredient(stone, 2), new CraftingRecipe.Ingredient(twig, 3)),
                        new ItemStack(campfire, 1),
                        CraftingCategory.BUILDING
                ),
                recipe(
                        "voxel:workbench",
                        "Build Workbench",
                        List.of(
                                new CraftingRecipe.Ingredient(planks, 6),
                                new CraftingRecipe.Ingredient(toolHandle, 1),
                                new CraftingRecipe.Ingredient(resin, 1)
                        ),
                        new ItemStack(workbench, 1),
                        CraftingCategory.BUILDING
                ),
                recipe(
                        "voxel:healing_snack",
                        "Mix Healing Snack",
                        List.of(new CraftingRecipe.Ingredient(herbs, 2), new CraftingRecipe.Ingredient(berries, 2)),
                        new ItemStack(snack, 1),
                        CraftingCategory.FOOD
                ),
                stationRecipe(
                        "voxel:cooked_berries",
                        "Warm Berries",
                        List.of(new CraftingRecipe.Ingredient(berries, 2)),
                        new ItemStack(cookedBerries, 1),
                        CraftingStationType.CAMPFIRE,
                        60,
                        CraftingCategory.FOOD
                ),
                stationRecipe(
                        "voxel:roasted_mushroom",
                        "Roast Mushroom",
                        List.of(new CraftingRecipe.Ingredient(mushroom, 1)),
                        new ItemStack(roastedMushroom, 1),
                        CraftingStationType.CAMPFIRE,
                        60,
                        CraftingCategory.FOOD
                ),
                stationRecipe(
                        "voxel:mushroom_stew",
                        "Cook Mushroom Stew",
                        List.of(
                                new CraftingRecipe.Ingredient(mushroom, 2),
                                new CraftingRecipe.Ingredient(waterContainer, 1),
                                new CraftingRecipe.Ingredient(clayBowl, 1)
                        ),
                        new ItemStack(mushroomStew, 1),
                        CraftingStationType.COOKING_POT,
                        120,
                        CraftingCategory.FOOD
                ),
                stationRecipe(
                        "voxel:herb_soup",
                        "Cook Herb Soup",
                        List.of(
                                new CraftingRecipe.Ingredient(herbs, 2),
                                new CraftingRecipe.Ingredient(waterContainer, 1),
                                new CraftingRecipe.Ingredient(clayBowl, 1)
                        ),
                        new ItemStack(herbSoup, 1),
                        CraftingStationType.COOKING_POT,
                        120,
                        CraftingCategory.FOOD
                ),
                stationRecipe(
                        "voxel:charcoal",
                        "Char Logs",
                        List.of(new CraftingRecipe.Ingredient(log, 1)),
                        new ItemStack(charcoal, 2),
                        CraftingStationType.CAMPFIRE,
                        100,
                        CraftingCategory.BASIC
                ),
                stationRecipe(
                        "voxel:clay_bowl",
                        "Fire Clay Bowl",
                        List.of(new CraftingRecipe.Ingredient(clayLump, 2)),
                        new ItemStack(clayBowl, 1),
                        CraftingStationType.CAMPFIRE,
                        100,
                        CraftingCategory.BASIC
                ),
                stationRecipe(
                        "voxel:clay_pot",
                        "Fire Clay Pot",
                        List.of(new CraftingRecipe.Ingredient(clayLump, 4)),
                        new ItemStack(clayPot, 1),
                        CraftingStationType.CAMPFIRE,
                        140,
                        CraftingCategory.DECOR
                ),
                recipe(
                        "voxel:water_container",
                        "Seal Water Container",
                        List.of(new CraftingRecipe.Ingredient(clayPot, 1), new CraftingRecipe.Ingredient(reedBundle, 1)),
                        new ItemStack(waterContainer, 1),
                        CraftingCategory.BASIC
                ),
                stationRecipe(
                        "voxel:berry_jam",
                        "Simmer Berry Jam",
                        List.of(new CraftingRecipe.Ingredient(berries, 3), new CraftingRecipe.Ingredient(clayBowl, 1)),
                        new ItemStack(berryJam, 1),
                        CraftingStationType.COOKING_POT,
                        140,
                        CraftingCategory.FOOD
                ),
                stationRecipe(
                        "voxel:calming_tea",
                        "Brew Calming Tea",
                        List.of(new CraftingRecipe.Ingredient(herbs, 2), new CraftingRecipe.Ingredient(waterContainer, 1)),
                        new ItemStack(calmingTea, 1),
                        CraftingStationType.COOKING_POT,
                        120,
                        CraftingCategory.FOOD
                ),
                stationRecipe(
                        "voxel:honey",
                        "Simmer Flower Honey",
                        List.of(
                                new CraftingRecipe.Ingredient(items.requireByKey("voxel:sun_bloom").id(), 2),
                                new CraftingRecipe.Ingredient(berries, 1),
                                new CraftingRecipe.Ingredient(waterContainer, 1)
                        ),
                        new ItemStack(honey, 1),
                        CraftingStationType.COOKING_POT,
                        100,
                        CraftingCategory.FOOD
                ),
                stationRecipe(
                        "voxel:hearty_stew",
                        "Cook Hearty Stew",
                        List.of(
                                new CraftingRecipe.Ingredient(roastedMushroom, 1),
                                new CraftingRecipe.Ingredient(cookedBerries, 1),
                                new CraftingRecipe.Ingredient(herbs, 1),
                                new CraftingRecipe.Ingredient(waterContainer, 1),
                                new CraftingRecipe.Ingredient(clayBowl, 1)
                        ),
                        new ItemStack(heartyStew, 1),
                        CraftingStationType.COOKING_POT,
                        180,
                        CraftingCategory.FOOD
                ),
                stationRecipe(
                        "voxel:copper_ingot",
                        "Smelt Copper",
                        List.of(new CraftingRecipe.Ingredient(rawCopper, 2), new CraftingRecipe.Ingredient(charcoal, 1)),
                        new ItemStack(copperIngot, 1),
                        CraftingStationType.CAMPFIRE,
                        160,
                        CraftingCategory.TOOLS
                ),
                recipe(
                        "voxel:cooking_pot",
                        "Build Cooking Pot",
                        List.of(
                                new CraftingRecipe.Ingredient(clayPot, 1),
                                new CraftingRecipe.Ingredient(copperIngot, 1),
                                new CraftingRecipe.Ingredient(charcoal, 1)
                        ),
                        new ItemStack(cookingPot, 1),
                        CraftingCategory.BUILDING
                ),
                stationRecipe(
                        "voxel:forge",
                        "Build Forge",
                        List.of(
                                new CraftingRecipe.Ingredient(stone, 6),
                                new CraftingRecipe.Ingredient(clayPot, 1),
                                new CraftingRecipe.Ingredient(copperIngot, 1),
                                new CraftingRecipe.Ingredient(charcoal, 2)
                        ),
                        new ItemStack(forge, 1),
                        CraftingStationType.WORKBENCH,
                        0,
                        CraftingCategory.BUILDING
                ),
                stationRecipe(
                        "voxel:iron_ingot",
                        "Smelt Iron",
                        List.of(new CraftingRecipe.Ingredient(rawIron, 2), new CraftingRecipe.Ingredient(charcoal, 2)),
                        new ItemStack(ironIngot, 1),
                        CraftingStationType.FORGE,
                        200,
                        CraftingCategory.TOOLS
                ),
                recipe(
                        "voxel:copper_axe",
                        "Craft Copper Axe",
                        List.of(new CraftingRecipe.Ingredient(copperIngot, 2), new CraftingRecipe.Ingredient(toolHandle, 1), new CraftingRecipe.Ingredient(resin, 1)),
                        new ItemStack(copperAxe, 1),
                        CraftingCategory.TOOLS
                ),
                recipe(
                        "voxel:copper_pickaxe",
                        "Craft Copper Pickaxe",
                        List.of(new CraftingRecipe.Ingredient(copperIngot, 3), new CraftingRecipe.Ingredient(toolHandle, 2), new CraftingRecipe.Ingredient(resin, 1)),
                        new ItemStack(copperPickaxe, 1),
                        CraftingCategory.TOOLS
                ),
                stationRecipe(
                        "voxel:iron_axe",
                        "Assemble Iron Axe",
                        List.of(
                                new CraftingRecipe.Ingredient(ironIngot, 2),
                                new CraftingRecipe.Ingredient(toolHandle, 1),
                                new CraftingRecipe.Ingredient(leatherStrip, 1)
                        ),
                        new ItemStack(ironAxe, 1),
                        CraftingStationType.WORKBENCH,
                        0,
                        CraftingCategory.TOOLS
                ),
                stationRecipe(
                        "voxel:iron_pickaxe",
                        "Assemble Iron Pickaxe",
                        List.of(
                                new CraftingRecipe.Ingredient(ironIngot, 3),
                                new CraftingRecipe.Ingredient(toolHandle, 2),
                                new CraftingRecipe.Ingredient(leatherStrip, 1)
                        ),
                        new ItemStack(ironPickaxe, 1),
                        CraftingStationType.WORKBENCH,
                        0,
                        CraftingCategory.TOOLS
                ),
                recipe(
                        "voxel:storage_crate",
                        "Build Simple Chest",
                        List.of(new CraftingRecipe.Ingredient(planks, 6), new CraftingRecipe.Ingredient(fiber, 2)),
                        new ItemStack(storageCrate, 1),
                        CraftingCategory.BUILDING
                ),
                recipe(
                        "voxel:sleeping_mat",
                        "Weave Sleeping Mat",
                        List.of(new CraftingRecipe.Ingredient(dryGrass, 3), new CraftingRecipe.Ingredient(fiber, 2)),
                        new ItemStack(sleepingMat, 1),
                        CraftingCategory.BUILDING
                ),
                stationRecipe(
                        "voxel:cloth",
                        "Weave Cloth",
                        List.of(new CraftingRecipe.Ingredient(fiber, 4), new CraftingRecipe.Ingredient(reedBundle, 1)),
                        new ItemStack(cloth, 2),
                        CraftingStationType.WORKBENCH,
                        0,
                        CraftingCategory.BASIC
                ),
                stationRecipe(
                        "voxel:leather_strip",
                        "Cure Leather Strips",
                        List.of(new CraftingRecipe.Ingredient(barkStrip, 2), new CraftingRecipe.Ingredient(resin, 1), new CraftingRecipe.Ingredient(charcoal, 1)),
                        new ItemStack(leatherStrip, 2),
                        CraftingStationType.WORKBENCH,
                        0,
                        CraftingCategory.BASIC
                ),
                recipe(
                        "voxel:flower_pot",
                        "Fill Flower Pot",
                        List.of(new CraftingRecipe.Ingredient(clayPot, 1), new CraftingRecipe.Ingredient(herbs, 1)),
                        new ItemStack(flowerPot, 1),
                        CraftingCategory.DECOR
                ),
                recipe(
                        "voxel:lantern",
                        "Assemble Lantern",
                        List.of(new CraftingRecipe.Ingredient(torch, 1), new CraftingRecipe.Ingredient(rawCopperOrPebble(items), 2), new CraftingRecipe.Ingredient(resinOrFiber(items), 1)),
                        new ItemStack(lantern, 1),
                        CraftingCategory.DECOR
                ),
                stationRecipe(
                        "voxel:ancient_lantern",
                        "Restore Ancient Lantern",
                        List.of(
                                new CraftingRecipe.Ingredient(ancientFragment, 2),
                                new CraftingRecipe.Ingredient(copperIngot, 1),
                                new CraftingRecipe.Ingredient(glowCrystal, 1)
                        ),
                        new ItemStack(ancientLantern, 1),
                        CraftingStationType.WORKBENCH,
                        0,
                        CraftingCategory.DECOR
                ),
                stationRecipe(
                        "voxel:ruin_key",
                        "Assemble Ruin Key",
                        List.of(
                                new CraftingRecipe.Ingredient(ancientFragment, 3),
                                new CraftingRecipe.Ingredient(ironIngot, 1),
                                new CraftingRecipe.Ingredient(glowCrystal, 1),
                                new CraftingRecipe.Ingredient(leatherStrip, 1)
                        ),
                        new ItemStack(ruinKey, 1),
                        CraftingStationType.WORKBENCH,
                        0,
                        CraftingCategory.ADVENTURE
                ),
                stationRecipe(
                        "voxel:ruin_seal",
                        "Bind Ruin Seal",
                        List.of(
                                new CraftingRecipe.Ingredient(ruinKey, 1),
                                new CraftingRecipe.Ingredient(ancientFragment, 2),
                                new CraftingRecipe.Ingredient(glowCrystal, 2),
                                new CraftingRecipe.Ingredient(charcoal, 2)
                        ),
                        new ItemStack(ruinSeal, 1),
                        CraftingStationType.FORGE,
                        220,
                        CraftingCategory.ADVENTURE
                ),
                recipe(
                        "voxel:woven_rug",
                        "Weave Rug",
                        List.of(new CraftingRecipe.Ingredient(fiber, 6), new CraftingRecipe.Ingredient(berries, 1)),
                        new ItemStack(rug, 1),
                        CraftingCategory.DECOR
                ),
                recipe(
                        "voxel:small_table",
                        "Build Small Table",
                        List.of(new CraftingRecipe.Ingredient(planks, 4), new CraftingRecipe.Ingredient(resinOrFiber(items), 1)),
                        new ItemStack(table, 1),
                        CraftingCategory.DECOR
                ),
                recipe(
                        "voxel:wooden_chair",
                        "Build Wooden Chair",
                        List.of(new CraftingRecipe.Ingredient(planks, 3), new CraftingRecipe.Ingredient(stick, 2)),
                        new ItemStack(chair, 1),
                        CraftingCategory.DECOR
                ),
                recipe(
                        "voxel:garden_fence",
                        "Build Garden Fence",
                        List.of(new CraftingRecipe.Ingredient(stick, 6), new CraftingRecipe.Ingredient(rope, 1)),
                        new ItemStack(fence, 4),
                        CraftingCategory.BUILDING
                ),
                recipe(
                        "voxel:herb_planter",
                        "Plant Herb Box",
                        List.of(new CraftingRecipe.Ingredient(flowerPot, 1), new CraftingRecipe.Ingredient(herbs, 1)),
                        new ItemStack(herbPlanter, 1),
                        CraftingCategory.DECOR
                ),
                recipe(
                        "voxel:berry_bush",
                        "Plant Berry Bush",
                        List.of(new CraftingRecipe.Ingredient(berries, 2), new CraftingRecipe.Ingredient(fiber, 1)),
                        new ItemStack(berryBush, 1),
                        CraftingCategory.BUILDING
                ),
                recipe(
                        "voxel:mossy_path",
                        "Lay Mossy Path",
                        List.of(new CraftingRecipe.Ingredient(mossyStone, 1), new CraftingRecipe.Ingredient(fiber, 1)),
                        new ItemStack(mossyPath, 2),
                        CraftingCategory.BUILDING
                )
        );
    }

    private static CraftingRecipe recipe(String key, String label, List<CraftingRecipe.Ingredient> ingredients, ItemStack result, CraftingCategory category) {
        return new CraftingRecipe(key, label, ingredients, result, CraftingStationType.INVENTORY, RecipeUnlock.ALWAYS, 0, 0, category);
    }

    private static CraftingRecipe stationRecipe(
            String key,
            String label,
            List<CraftingRecipe.Ingredient> ingredients,
            ItemStack result,
            CraftingStationType station,
            int timeTicks,
            CraftingCategory category
    ) {
        return new CraftingRecipe(key, label, ingredients, result, station, RecipeUnlock.NEAR_STATION, timeTicks, 0, category);
    }

    private static short rawCopperOrPebble(Registry<ItemType> items) {
        return items.findByKey("voxel:raw_copper")
                .or(() -> items.findByKey("voxel:pebble"))
                .orElseThrow()
                .id();
    }

    private static short resinOrFiber(Registry<ItemType> items) {
        return items.findByKey("voxel:resin")
                .or(() -> items.findByKey("voxel:fiber"))
                .orElseThrow()
                .id();
    }
}
