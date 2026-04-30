package dev.voxelgame.client;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.math.Raycast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void exposesEasedProgressForCrackFeedback() {
        BlockBreakAnimation animation = new BlockBreakAnimation();
        Raycast.Hit hit = new Raycast.Hit(1, 2, 3, 0, 0, 1, 2.5);

        animation.startOrContinue(hit, Blocks.createDefaultRegistry().requireById(Blocks.STONE), 1.0, 4.0);

        assertEquals(0.0f, animation.easedProgress(4.0), 0.001f);
        assertEquals(0.5f, animation.easedProgress(4.5), 0.001f);
        assertEquals(1.0f, animation.easedProgress(5.0), 0.001f);
    }
}
