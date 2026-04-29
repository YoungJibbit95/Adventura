package dev.voxelgame.client;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.math.Raycast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockBreakAnimationTest {
    @Test
    void tracksProgressAndCompletion() {
        BlockBreakAnimation animation = new BlockBreakAnimation();
        Raycast.Hit hit = new Raycast.Hit(1, 2, 3, 0, 0, 1, 2.5);

        animation.startOrContinue(hit, Blocks.createDefaultRegistry().requireById(Blocks.STONE), 0.5, 10.0);

        assertTrue(animation.active());
        assertFalse(animation.complete(10.2));
        assertTrue(animation.complete(10.6));
    }
}
