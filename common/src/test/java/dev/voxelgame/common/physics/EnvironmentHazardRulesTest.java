package dev.voxelgame.common.physics;

import dev.voxelgame.common.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentHazardRulesTest {
    @Test
    void hotColdAndThornBlocksMapToHazards() {
        EnvironmentHazardRules.Hazard hot = EnvironmentHazardRules.forBlock(Blocks.CAMPFIRE_ACTIVE);
        EnvironmentHazardRules.Hazard thorn = EnvironmentHazardRules.forBlock(Blocks.CACTUS);
        EnvironmentHazardRules.Hazard cold = EnvironmentHazardRules.forBlock(Blocks.ICE);

        assertTrue(hot.hot());
        assertTrue(hot.damagePerPulse() > 0);
        assertTrue(thorn.damagePerPulse() > 0);
        assertTrue(cold.cold());
        assertFalse(cold.hot());
    }
}
