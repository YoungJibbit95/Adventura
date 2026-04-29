package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.DimensionSettings;
import dev.voxelgame.common.world.InMemoryWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComfortRulesTest {
    @Test
    void scansComfortValuesWithCap() {
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        InMemoryWorld world = new InMemoryWorld(DimensionSettings.OVERWORLD, blocks);
        world.setBlockId(0, 80, 0, Blocks.CAMPFIRE_ACTIVE);
        world.setBlockId(1, 80, 0, Blocks.WOVEN_RUG);
        world.setBlockId(2, 80, 0, Blocks.WOODEN_CHAIR);
        world.setBlockId(3, 80, 0, Blocks.STORAGE_CRATE);

        assertEquals(11, ComfortRules.scan(world, 0.5, 80.5, 0.5, 8, 25));
        assertEquals(6, ComfortRules.scan(world, 0.5, 80.5, 0.5, 8, 6));
    }

    @Test
    void exposesSurvivalMultipliers() {
        assertEquals(0.75f, ComfortRules.hungerDrainMultiplier(25), 0.0001f);
        assertEquals(1.4f, ComfortRules.staminaRegenMultiplier(25), 0.0001f);
    }
}
