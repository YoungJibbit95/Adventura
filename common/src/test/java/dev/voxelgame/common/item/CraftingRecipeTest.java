package dev.voxelgame.common.item;

import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingRecipeTest {
    @Test
    void p1ProgressionResourcesAreRegistered() {
        Registry<ItemType> items = Items.createDefaultRegistry();

        for (String key : List.of(
                "voxel:dry_grass",
                "voxel:charcoal",
                "voxel:clay_bowl",
                "voxel:clay_pot",
                "voxel:copper_ingot",
                "voxel:iron_ingot",
                "voxel:glow_crystal",
                "voxel:cloth",
                "voxel:leather_strip",
                "voxel:honey"
        )) {
            assertTrue(items.findByKey(key).isPresent(), key);
        }
    }

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
    void foodProgressionRecipesUseContainersAndCookingPot() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
        short berries = items.requireByKey("voxel:berries").id();
        short mushroom = items.requireByKey("voxel:mushroom").id();
        short clayBowl = items.requireByKey("voxel:clay_bowl").id();
        short clayPot = items.requireByKey("voxel:clay_pot").id();
        short reedBundle = items.requireByKey("voxel:reed_bundle").id();
        short herbs = items.requireByKey("voxel:wild_herbs").id();
        short cookedBerries = items.requireByKey("voxel:cooked_berries").id();
        short roastedMushroom = items.requireByKey("voxel:roasted_mushroom").id();
        short waterContainer = items.requireByKey("voxel:water_container").id();

        Inventory containerInventory = new Inventory(4);
        containerInventory.add(clayPot, 1, items);
        containerInventory.add(reedBundle, 1, items);
        CraftingRecipe waterContainerRecipe = recipeByKey(recipes, "voxel:water_container");
        assertEquals(CraftingCategory.BASIC, waterContainerRecipe.category());
        assertTrue(waterContainerRecipe.craft(containerInventory, items));
        assertEquals(1, containerInventory.count(waterContainer));
        assertEquals(0, containerInventory.count(clayPot));
        assertEquals(0, containerInventory.count(reedBundle));

        Inventory mushroomStewInventory = new Inventory(5);
        mushroomStewInventory.add(mushroom, 2, items);
        mushroomStewInventory.add(waterContainer, 1, items);
        mushroomStewInventory.add(clayBowl, 1, items);
        assertStationFoodRecipe(recipeByKey(recipes, "voxel:mushroom_stew"), mushroomStewInventory, items, CraftingStationType.COOKING_POT, 120);

        Inventory jamInventory = new Inventory(4);
        jamInventory.add(berries, 3, items);
        jamInventory.add(clayBowl, 1, items);
        assertStationFoodRecipe(recipeByKey(recipes, "voxel:berry_jam"), jamInventory, items, CraftingStationType.COOKING_POT, 140);

        Inventory soupInventory = new Inventory(5);
        soupInventory.add(herbs, 2, items);
        soupInventory.add(waterContainer, 1, items);
        soupInventory.add(clayBowl, 1, items);
        assertStationFoodRecipe(recipeByKey(recipes, "voxel:herb_soup"), soupInventory, items, CraftingStationType.COOKING_POT, 120);

        Inventory teaInventory = new Inventory(5);
        teaInventory.add(herbs, 2, items);
        teaInventory.add(waterContainer, 1, items);
        assertStationFoodRecipe(recipeByKey(recipes, "voxel:calming_tea"), teaInventory, items, CraftingStationType.COOKING_POT, 120);

        Inventory stewInventory = new Inventory(7);
        stewInventory.add(roastedMushroom, 1, items);
        stewInventory.add(cookedBerries, 1, items);
        stewInventory.add(herbs, 1, items);
        stewInventory.add(waterContainer, 1, items);
        stewInventory.add(clayBowl, 1, items);
        assertStationFoodRecipe(recipeByKey(recipes, "voxel:hearty_stew"), stewInventory, items, CraftingStationType.COOKING_POT, 180);
    }

    @Test
    void craftsCookingPotFromFiredClayCopperAndCharcoal() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Inventory inventory = new Inventory(6);
        short clayPot = items.requireByKey("voxel:clay_pot").id();
        short copperIngot = items.requireByKey("voxel:copper_ingot").id();
        short charcoal = items.requireByKey("voxel:charcoal").id();
        short cookingPot = items.requireByKey("voxel:cooking_pot").id();
        inventory.add(clayPot, 1, items);
        inventory.add(copperIngot, 1, items);
        inventory.add(charcoal, 1, items);

        CraftingRecipe recipe = recipeByKey(CraftingRecipes.createDefaultRecipes(items), "voxel:cooking_pot");

        assertTrue(recipe.craft(inventory, items));

        assertEquals(CraftingCategory.BUILDING, recipe.category());
        assertEquals(1, inventory.count(cookingPot));
        assertEquals(0, inventory.count(clayPot));
        assertEquals(0, inventory.count(copperIngot));
        assertEquals(0, inventory.count(charcoal));
    }

    @Test
    void craftsSleepingMatForSleepIntent() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Inventory inventory = new Inventory(10);
        short dryGrass = items.requireByKey("voxel:dry_grass").id();
        short fiber = items.requireByKey("voxel:fiber").id();
        short sleepingMat = items.requireByKey("voxel:sleeping_mat").id();
        inventory.add(dryGrass, 3, items);
        inventory.add(fiber, 2, items);
        CraftingRecipe recipe = CraftingRecipes.createDefaultRecipes(items).stream()
                .filter(candidate -> candidate.key().equals("voxel:sleeping_mat"))
                .findFirst()
                .orElseThrow();

        assertTrue(recipe.craft(inventory, items));

        assertEquals(1, inventory.count(sleepingMat));
        assertEquals(0, inventory.count(dryGrass));
        assertEquals(0, inventory.count(fiber));
    }

    @Test
    void pineMaterialsCraftResinTorchesAndToolHandles() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
        short stick = items.requireByKey("voxel:stick").id();
        short resin = items.requireByKey("voxel:resin").id();
        short bark = items.requireByKey("voxel:bark_strip").id();
        short resinTorch = items.requireByKey("voxel:resin_torch").id();
        short toolHandle = items.requireByKey("voxel:tool_handle").id();

        Inventory torchInventory = new Inventory(4);
        torchInventory.add(stick, 1, items);
        torchInventory.add(resin, 1, items);
        torchInventory.add(bark, 1, items);
        CraftingRecipe resinTorchRecipe = recipeByKey(recipes, "voxel:resin_torch");
        assertEquals(CraftingCategory.BASIC, resinTorchRecipe.category());
        assertTrue(resinTorchRecipe.craft(torchInventory, items));
        assertEquals(4, torchInventory.count(resinTorch));

        Inventory handleInventory = new Inventory(4);
        handleInventory.add(stick, 1, items);
        handleInventory.add(bark, 2, items);
        CraftingRecipe handleRecipe = recipeByKey(recipes, "voxel:tool_handle");
        assertEquals(CraftingCategory.TOOLS, handleRecipe.category());
        assertTrue(handleRecipe.craft(handleInventory, items));
        assertEquals(1, handleInventory.count(toolHandle));
    }

    @Test
    void copperToolsUsePineHandlesAndResinBindings() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
        short copperIngot = items.requireByKey("voxel:copper_ingot").id();
        short resin = items.requireByKey("voxel:resin").id();
        short toolHandle = items.requireByKey("voxel:tool_handle").id();
        short copperAxe = items.requireByKey("voxel:copper_axe").id();
        short copperPickaxe = items.requireByKey("voxel:copper_pickaxe").id();

        Inventory axeInventory = new Inventory(5);
        axeInventory.add(copperIngot, 2, items);
        axeInventory.add(toolHandle, 1, items);
        axeInventory.add(resin, 1, items);
        CraftingRecipe axeRecipe = recipeByKey(recipes, "voxel:copper_axe");
        assertTrue(axeRecipe.craft(axeInventory, items));
        assertEquals(1, axeInventory.count(copperAxe));

        Inventory pickaxeInventory = new Inventory(5);
        pickaxeInventory.add(copperIngot, 3, items);
        pickaxeInventory.add(toolHandle, 2, items);
        pickaxeInventory.add(resin, 1, items);
        CraftingRecipe pickaxeRecipe = recipeByKey(recipes, "voxel:copper_pickaxe");
        assertTrue(pickaxeRecipe.craft(pickaxeInventory, items));
        assertEquals(1, pickaxeInventory.count(copperPickaxe));
    }

    @Test
    void workbenchAndForgeGateMidGameProgression() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
        short planks = items.requireByKey("voxel:skyroot_planks").id();
        short toolHandle = items.requireByKey("voxel:tool_handle").id();
        short resin = items.requireByKey("voxel:resin").id();
        short workbench = items.requireByKey("voxel:workbench").id();
        short stone = items.requireByKey("voxel:stone").id();
        short clayPot = items.requireByKey("voxel:clay_pot").id();
        short copperIngot = items.requireByKey("voxel:copper_ingot").id();
        short charcoal = items.requireByKey("voxel:charcoal").id();
        short forge = items.requireByKey("voxel:forge").id();

        Inventory workbenchInventory = new Inventory(6);
        workbenchInventory.add(planks, 6, items);
        workbenchInventory.add(toolHandle, 1, items);
        workbenchInventory.add(resin, 1, items);
        CraftingRecipe workbenchRecipe = recipeByKey(recipes, "voxel:workbench");
        assertEquals(CraftingStationType.INVENTORY, workbenchRecipe.stationType());
        assertTrue(workbenchRecipe.craft(workbenchInventory, items));
        assertEquals(1, workbenchInventory.count(workbench));

        Inventory forgeInventory = new Inventory(8);
        forgeInventory.add(stone, 6, items);
        forgeInventory.add(clayPot, 1, items);
        forgeInventory.add(copperIngot, 1, items);
        forgeInventory.add(charcoal, 2, items);
        CraftingRecipe forgeRecipe = recipeByKey(recipes, "voxel:forge");
        assertEquals(CraftingStationType.WORKBENCH, forgeRecipe.stationType());
        assertFalse(forgeRecipe.craft(forgeInventory, items));
        assertTrue(forgeRecipe.craft(forgeInventory, items, CraftingStationType.WORKBENCH));
        assertEquals(1, forgeInventory.count(forge));
    }

    @Test
    void forgeSmeltsIronAndWorkbenchBuildsIronTools() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
        short rawIron = items.requireByKey("voxel:raw_iron").id();
        short charcoal = items.requireByKey("voxel:charcoal").id();
        short ironIngot = items.requireByKey("voxel:iron_ingot").id();
        short toolHandle = items.requireByKey("voxel:tool_handle").id();
        short leatherStrip = items.requireByKey("voxel:leather_strip").id();
        short ironPickaxe = items.requireByKey("voxel:iron_pickaxe").id();

        Inventory ingotInventory = new Inventory(4);
        ingotInventory.add(rawIron, 2, items);
        ingotInventory.add(charcoal, 2, items);
        CraftingRecipe ingotRecipe = recipeByKey(recipes, "voxel:iron_ingot");
        assertEquals(CraftingStationType.FORGE, ingotRecipe.stationType());
        assertEquals(200, ingotRecipe.craftingTimeTicks());
        assertFalse(ingotRecipe.craft(ingotInventory, items));
        assertTrue(ingotRecipe.craft(ingotInventory, items, CraftingStationType.FORGE));
        assertEquals(1, ingotInventory.count(ironIngot));

        Inventory toolInventory = new Inventory(6);
        toolInventory.add(ironIngot, 3, items);
        toolInventory.add(toolHandle, 2, items);
        toolInventory.add(leatherStrip, 1, items);
        CraftingRecipe pickaxeRecipe = recipeByKey(recipes, "voxel:iron_pickaxe");
        assertEquals(CraftingStationType.WORKBENCH, pickaxeRecipe.stationType());
        assertTrue(pickaxeRecipe.craft(toolInventory, items, CraftingStationType.WORKBENCH));
        assertEquals(1, toolInventory.count(ironPickaxe));
    }

    @Test
    void workbenchCreatesClothLeatherStripsAndHoneyStaysCookingProgression() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
        short fiber = items.requireByKey("voxel:fiber").id();
        short reedBundle = items.requireByKey("voxel:reed_bundle").id();
        short cloth = items.requireByKey("voxel:cloth").id();
        short barkStrip = items.requireByKey("voxel:bark_strip").id();
        short resin = items.requireByKey("voxel:resin").id();
        short charcoal = items.requireByKey("voxel:charcoal").id();
        short leatherStrip = items.requireByKey("voxel:leather_strip").id();

        Inventory clothInventory = new Inventory(4);
        clothInventory.add(fiber, 4, items);
        clothInventory.add(reedBundle, 1, items);
        CraftingRecipe clothRecipe = recipeByKey(recipes, "voxel:cloth");
        assertEquals(CraftingStationType.WORKBENCH, clothRecipe.stationType());
        assertTrue(clothRecipe.craft(clothInventory, items, CraftingStationType.WORKBENCH));
        assertEquals(2, clothInventory.count(cloth));

        Inventory leatherInventory = new Inventory(5);
        leatherInventory.add(barkStrip, 2, items);
        leatherInventory.add(resin, 1, items);
        leatherInventory.add(charcoal, 1, items);
        CraftingRecipe leatherRecipe = recipeByKey(recipes, "voxel:leather_strip");
        assertEquals(CraftingStationType.WORKBENCH, leatherRecipe.stationType());
        assertTrue(leatherRecipe.craft(leatherInventory, items, CraftingStationType.WORKBENCH));
        assertEquals(2, leatherInventory.count(leatherStrip));

        CraftingRecipe honeyRecipe = recipeByKey(recipes, "voxel:honey");
        assertEquals(CraftingStationType.COOKING_POT, honeyRecipe.stationType());
        assertEquals(CraftingCategory.FOOD, honeyRecipe.category());
    }

    @Test
    void ruinsRestoreAncientLanternsForBaseComfort() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        Inventory inventory = new Inventory(5);
        short ancientFragment = items.requireByKey("voxel:ancient_fragment").id();
        short copperIngot = items.requireByKey("voxel:copper_ingot").id();
        short glowCrystal = items.requireByKey("voxel:glow_crystal").id();
        short ancientLantern = items.requireByKey("voxel:ancient_lantern").id();
        inventory.add(ancientFragment, 2, items);
        inventory.add(copperIngot, 1, items);
        inventory.add(glowCrystal, 1, items);

        CraftingRecipe recipe = recipeByKey(CraftingRecipes.createDefaultRecipes(items), "voxel:ancient_lantern");

        assertEquals(CraftingCategory.DECOR, recipe.category());
        assertEquals(CraftingStationType.WORKBENCH, recipe.stationType());
        assertFalse(recipe.craft(inventory, items));
        assertTrue(recipe.craft(inventory, items, CraftingStationType.WORKBENCH));
        assertEquals(1, inventory.count(ancientLantern));
        assertEquals(0, inventory.count(ancientFragment));
        assertEquals(0, inventory.count(glowCrystal));
    }

    @Test
    void ruinKeyAndSealDefineLateGameRuinProgression() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
        CraftingRecipe key = recipeByKey(recipes, "voxel:ruin_key");
        CraftingRecipe seal = recipeByKey(recipes, "voxel:ruin_seal");

        assertEquals(CraftingCategory.ADVENTURE, key.category());
        assertEquals(CraftingStationType.WORKBENCH, key.stationType());
        assertEquals(CraftingCategory.ADVENTURE, seal.category());
        assertEquals(CraftingStationType.FORGE, seal.stationType());
        assertEquals(220, seal.craftingTimeTicks());
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

    @Test
    void craftsRequestedCountAtomically() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short pebble = items.requireByKey("voxel:pebble").id();
        short twig = items.requireByKey("voxel:twig").id();
        short fiber = items.requireByKey("voxel:fiber").id();
        short pickaxe = items.requireByKey("voxel:stone_pickaxe").id();
        Inventory inventory = new Inventory(10);
        inventory.add(pebble, 4, items);
        inventory.add(twig, 2, items);
        inventory.add(fiber, 2, items);
        CraftingRecipe recipe = CraftingRecipes.createDefaultRecipes(items).stream()
                .filter(candidate -> candidate.key().equals("voxel:stone_pickaxe"))
                .findFirst()
                .orElseThrow();

        assertTrue(recipe.craft(inventory, items, CraftingStationType.INVENTORY, 2));
        assertEquals(0, inventory.count(pebble));
        assertEquals(0, inventory.count(twig));
        assertEquals(0, inventory.count(fiber));
        assertEquals(2, inventory.count(pickaxe));
    }

    @Test
    void rejectsRequestedCountWhenOutputWouldNotFit() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short pebble = items.requireByKey("voxel:pebble").id();
        short twig = items.requireByKey("voxel:twig").id();
        short fiber = items.requireByKey("voxel:fiber").id();
        short dirt = items.requireByKey("voxel:dirt").id();
        Inventory inventory = new Inventory(4);
        inventory.add(pebble, 8, items);
        inventory.add(twig, 4, items);
        inventory.add(fiber, 4, items);
        inventory.add(dirt, 1, items);
        CraftingRecipe recipe = CraftingRecipes.createDefaultRecipes(items).stream()
                .filter(candidate -> candidate.key().equals("voxel:stone_pickaxe"))
                .findFirst()
                .orElseThrow();

        assertFalse(recipe.craft(inventory, items, CraftingStationType.INVENTORY, 4));
        assertEquals(8, inventory.count(pebble));
        assertEquals(4, inventory.count(twig));
        assertEquals(4, inventory.count(fiber));
    }

    @Test
    void countCraftingCanUseFreedIngredientSlotsForOutput() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short pebble = items.requireByKey("voxel:pebble").id();
        short twig = items.requireByKey("voxel:twig").id();
        short fiber = items.requireByKey("voxel:fiber").id();
        short pickaxe = items.requireByKey("voxel:stone_pickaxe").id();
        Inventory inventory = new Inventory(3);
        inventory.add(pebble, 4, items);
        inventory.add(twig, 2, items);
        inventory.add(fiber, 2, items);
        CraftingRecipe recipe = CraftingRecipes.createDefaultRecipes(items).stream()
                .filter(candidate -> candidate.key().equals("voxel:stone_pickaxe"))
                .findFirst()
                .orElseThrow();

        assertTrue(recipe.craft(inventory, items, CraftingStationType.INVENTORY, 2));
        assertEquals(2, inventory.count(pickaxe));
    }

    @Test
    void recipeLookupAcceptsLegacyAliasKeys() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);

        CraftingRecipe planks = CraftingRecipes.findByKey(recipes, items, "voxel:planks").orElseThrow();
        CraftingRecipe woodenPlank = CraftingRecipes.findByKey(recipes, items, "voxel:wooden_plank").orElseThrow();

        assertEquals("voxel:skyroot_planks", planks.key());
        assertEquals("voxel:skyroot_planks", woodenPlank.key());
    }

    @Test
    void defaultRecipeKeysAreCanonicalItemKeys() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);

        for (CraftingRecipe recipe : recipes) {
            assertEquals(recipe.key(), items.canonicalKey(recipe.key()).orElseThrow(), recipe.key());
        }
    }

    @Test
    void defaultRecipesReferenceRegisteredItemsAndUniqueKeys() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        List<CraftingRecipe> recipes = CraftingRecipes.createDefaultRecipes(items);
        Set<String> keys = new HashSet<>();

        for (CraftingRecipe recipe : recipes) {
            assertTrue(keys.add(recipe.key()), "Duplicate recipe key " + recipe.key());
            assertTrue(items.findById(recipe.result().itemId()).isPresent(), recipe.key() + " result is not registered");
            for (CraftingRecipe.Ingredient ingredient : recipe.ingredients()) {
                assertTrue(
                        items.findById(ingredient.itemId()).isPresent(),
                        recipe.key() + " ingredient id " + ingredient.itemId() + " is not registered"
                );
            }
        }
    }

    private static CraftingRecipe recipeByKey(List<CraftingRecipe> recipes, String key) {
        return recipes.stream()
                .filter(candidate -> candidate.key().equals(key))
                .findFirst()
                .orElseThrow();
    }

    private static void assertStationFoodRecipe(
            CraftingRecipe recipe,
            Inventory inventory,
            Registry<ItemType> items,
            CraftingStationType stationType,
            int timeTicks
    ) {
        assertEquals(stationType, recipe.stationType());
        assertEquals(CraftingCategory.FOOD, recipe.category());
        assertEquals(timeTicks, recipe.craftingTimeTicks());
        assertFalse(recipe.canCraft(inventory, items));
        assertTrue(recipe.canCraft(inventory, items, stationType));
    }
}
