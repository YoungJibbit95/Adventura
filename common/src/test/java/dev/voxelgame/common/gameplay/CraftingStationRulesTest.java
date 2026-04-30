package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.item.CraftingStationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingStationRulesTest {
    @Test
    void stationTypesMapToTheirUsableBlocks() {
        assertTrue(CraftingStationRules.accepts(CraftingStationType.CAMPFIRE, Blocks.CAMPFIRE_ACTIVE));
        assertFalse(CraftingStationRules.accepts(CraftingStationType.CAMPFIRE, Blocks.CAMPFIRE));
        assertTrue(CraftingStationRules.accepts(CraftingStationType.COOKING_POT, Blocks.COOKING_POT));
        assertFalse(CraftingStationRules.accepts(CraftingStationType.COOKING_POT, Blocks.CLAY));
        assertTrue(CraftingStationRules.accepts(CraftingStationType.WORKBENCH, Blocks.WORKBENCH));
        assertFalse(CraftingStationRules.accepts(CraftingStationType.WORKBENCH, Blocks.SMALL_TABLE));
        assertTrue(CraftingStationRules.accepts(CraftingStationType.FORGE, Blocks.FORGE));
        assertFalse(CraftingStationRules.accepts(CraftingStationType.FORGE, Blocks.COOKING_POT));
    }
}
