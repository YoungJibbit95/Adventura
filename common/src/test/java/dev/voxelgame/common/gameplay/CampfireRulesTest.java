package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampfireRulesTest {
    @Test
    void recognizesCampfireStates() {
        assertTrue(CampfireRules.isCampfire(Blocks.CAMPFIRE));
        assertTrue(CampfireRules.isCampfire(Blocks.CAMPFIRE_ACTIVE));
        assertTrue(CampfireRules.isCampfire(Blocks.CAMPFIRE_BURNED_OUT));
        assertTrue(CampfireRules.isActiveCampfire(Blocks.CAMPFIRE_ACTIVE));
        assertFalse(CampfireRules.isActiveCampfire(Blocks.CAMPFIRE));
    }

    @Test
    void onlyKnownFuelItemsBurn() {
        assertTrue(CampfireRules.fuelSeconds("voxel:twig").isPresent());
        assertTrue(CampfireRules.fuelSeconds("voxel:bark_strip").isPresent());
        assertTrue(CampfireRules.fuelSeconds("voxel:reed_bundle").isPresent());
        assertTrue(CampfireRules.fuelSeconds("voxel:charcoal").orElseThrow() > CampfireRules.fuelSeconds("voxel:twig").orElseThrow());
        assertTrue(CampfireRules.fuelSeconds("voxel:berries").isEmpty());
    }
}
