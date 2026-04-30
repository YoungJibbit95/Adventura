package dev.voxelgame.client;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.ItemDropType;
import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InteractionHintTest {
    private final Registry<BlockType> blocks = Blocks.createDefaultRegistry();

    @Test
    void storageCrateShowsOpenAction() {
        InteractionHint hint = InteractionHint.forBlock(blocks.requireById(Blocks.STORAGE_CRATE), context(true, false, false, false, true, 1.0f, 0.0f));

        assertEquals("Storage crate", hint.title());
        assertEquals("Right click open crate", hint.action());
        assertEquals(InteractionHint.Tone.READY, hint.tone());
    }

    @Test
    void farStorageCrateShowsRangeHint() {
        InteractionHint hint = InteractionHint.forBlock(blocks.requireById(Blocks.STORAGE_CRATE), context(false, false, false, false, true, 1.0f, 0.0f));

        assertEquals("Move closer", hint.action());
        assertEquals("Crate opens at reach range", hint.detail());
        assertEquals(InteractionHint.Tone.WARNING, hint.tone());
    }

    @Test
    void campfireWithoutFuelExplainsMissingFuel() {
        InteractionHint hint = InteractionHint.forBlock(blocks.requireById(Blocks.CAMPFIRE), context(true, false, false, false, true, 1.0f, 0.0f));

        assertEquals("Needs fuel in hand", hint.action());
        assertEquals("Sticks, logs, coal, grass, bark, reeds", hint.detail());
        assertEquals(InteractionHint.Tone.WARNING, hint.tone());
    }

    @Test
    void campfireWithFuelShowsLightAction() {
        InteractionHint hint = InteractionHint.forBlock(blocks.requireById(Blocks.CAMPFIRE_BURNED_OUT), context(true, true, false, false, true, 1.0f, 0.0f));

        assertEquals("Right click light campfire", hint.action());
        assertEquals(InteractionHint.Tone.READY, hint.tone());
    }

    @Test
    void cookingPotShowsCookingStationAction() {
        InteractionHint hint = InteractionHint.forBlock(blocks.requireById(Blocks.COOKING_POT), context(true, false, false, false, true, 1.0f, 0.0f));

        assertEquals("Cooking pot", hint.title());
        assertEquals("Press E nearby to cook", hint.action());
        assertEquals("Soups, stew, tea, and jam", hint.detail());
        assertEquals(InteractionHint.Tone.READY, hint.tone());
    }

    @Test
    void workbenchShowsProgressionStationAction() {
        InteractionHint hint = InteractionHint.forBlock(blocks.requireById(Blocks.WORKBENCH), context(true, false, false, false, true, 1.0f, 0.0f));

        assertEquals("Workbench", hint.title());
        assertEquals("Press E nearby to craft", hint.action());
        assertEquals("Workbench recipes and iron tools", hint.detail());
        assertEquals(InteractionHint.Tone.READY, hint.tone());
    }

    @Test
    void forgeShowsLateGameStationAction() {
        InteractionHint hint = InteractionHint.forBlock(blocks.requireById(Blocks.FORGE), context(true, false, false, false, true, 1.0f, 0.0f));

        assertEquals("Forge", hint.title());
        assertEquals("Press E nearby to forge", hint.action());
        assertEquals("Iron ingots and ruin seals", hint.detail());
        assertEquals(InteractionHint.Tone.READY, hint.tone());
    }

    @Test
    void wildGrassShowsDryGrassGatherHint() {
        InteractionHint hint = InteractionHint.forBlock(blocks.requireById(Blocks.WILD_GRASS), context(true, false, false, false, true, 1.0f, 0.0f));

        assertEquals("Wild grass", hint.title());
        assertEquals("Right click gather dry grass", hint.action());
        assertEquals("Left click cuts fiber", hint.detail());
        assertEquals(InteractionHint.Tone.READY, hint.tone());
    }

    @Test
    void reedsShowHarvestAndCraftingHint() {
        InteractionHint hint = InteractionHint.forBlock(blocks.requireById(Blocks.REEDS), context(true, false, false, false, true, 1.0f, 0.0f));

        assertEquals("Reeds", hint.title());
        assertEquals("Right click cut reeds", hint.action());
        assertEquals("Used for water containers and fuel", hint.detail());
        assertEquals(InteractionHint.Tone.READY, hint.tone());
    }

    @Test
    void treeStumpsShowBarkHarvestHint() {
        InteractionHint hint = InteractionHint.forBlock(blocks.requireById(Blocks.TREE_STUMP), context(true, false, false, false, true, 1.0f, 0.0f));

        assertEquals("Tree stump", hint.title());
        assertEquals("Right click peel bark", hint.action());
        assertEquals("Used for tool handles and fuel", hint.detail());
        assertEquals(InteractionHint.Tone.READY, hint.tone());
    }

    @Test
    void tierGatedMiningShowsRequiredToolLevel() {
        InteractionHint hint = InteractionHint.forBlock(blocks.requireById(Blocks.IRON_ORE), context(true, false, false, false, false, 0.55f, 0.0f));

        assertEquals("Need Level 2 pickaxe", hint.action());
        assertEquals("Swap tools, then hold left", hint.detail());
        assertEquals(InteractionHint.Tone.WARNING, hint.tone());
    }

    @Test
    void miningProgressTakesPriorityWhileHoldingLeft() {
        InteractionHint hint = InteractionHint.forBlock(blocks.requireById(Blocks.STONE), context(true, false, false, false, true, 1.0f, 0.42f));

        assertEquals("Mining 42%", hint.action());
        assertEquals("Keep holding left", hint.detail());
        assertEquals(InteractionHint.Tone.READY, hint.tone());
    }

    @Test
    void creatureWithoutFoodShowsObserveAction() {
        InteractionHint hint = InteractionHint.forEntity("voxel:forest_bunny", false);

        assertEquals("Forest bunny", hint.title());
        assertEquals("Right click observe", hint.action());
        assertEquals("Watch behavior and movement", hint.detail());
        assertEquals(InteractionHint.Tone.NEUTRAL, hint.tone());
    }

    @Test
    void creatureWithFoodShowsFeedAction() {
        InteractionHint hint = InteractionHint.forEntity("voxel:cozy_sheep", true);

        assertEquals("Cozy sheep", hint.title());
        assertEquals("Right click feed", hint.action());
        assertEquals("May follow if interested", hint.detail());
        assertEquals(InteractionHint.Tone.READY, hint.tone());
    }

    @Test
    void itemDropShowsPickupAction() {
        InteractionHint hint = InteractionHint.forEntity(ItemDropType.typeKey("voxel:berries"), false);

        assertEquals("Berries", hint.title());
        assertEquals("Walk over to pick up", hint.action());
        assertEquals("Inventory collects nearby drops", hint.detail());
        assertEquals(InteractionHint.Tone.READY, hint.tone());
    }

    private static InteractionHint.Context context(boolean inReach, boolean selectedFuel, boolean selectedFood, boolean selectedPlaceable, boolean canHarvest, float breakMultiplier, float miningProgress) {
        return new InteractionHint.Context(inReach, selectedFuel, selectedFood, selectedPlaceable, canHarvest, breakMultiplier, GameMode.SURVIVAL, miningProgress);
    }
}
