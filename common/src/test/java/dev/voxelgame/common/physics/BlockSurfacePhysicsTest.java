package dev.voxelgame.common.physics;

import dev.voxelgame.common.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockSurfacePhysicsTest {
    @Test
    void blockSurfacesExposeMovementMultipliers() {
        assertEquals(BlockSurfacePhysics.ICE, BlockSurfacePhysics.forBlock(Blocks.ICE));
        assertEquals(BlockSurfacePhysics.SNOW, BlockSurfacePhysics.forBlock(Blocks.SNOW));
        assertEquals(BlockSurfacePhysics.PATH, BlockSurfacePhysics.forBlock(Blocks.MOSSY_PATH));
        assertEquals(BlockSurfacePhysics.SAND, BlockSurfacePhysics.forBlock(Blocks.RED_SAND));
        assertEquals(BlockSurfacePhysics.DEFAULT, BlockSurfacePhysics.forBlock(Blocks.WATER));

        assertTrue(BlockSurfacePhysics.ICE.frictionMultiplier() < BlockSurfacePhysics.DEFAULT.frictionMultiplier());
        assertTrue(BlockSurfacePhysics.SNOW.speedMultiplier() < BlockSurfacePhysics.DEFAULT.speedMultiplier());
        assertTrue(BlockSurfacePhysics.PATH.speedMultiplier() > BlockSurfacePhysics.DEFAULT.speedMultiplier());
    }
}
