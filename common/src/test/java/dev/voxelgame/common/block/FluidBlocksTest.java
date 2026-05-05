package dev.voxelgame.common.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidBlocksTest {
    @Test
    void classifiesFluidBlocksForPhysicsAndWorldDirtying() {
        assertTrue(FluidBlocks.isWater(Blocks.WATER));
        assertTrue(FluidBlocks.isLava(Blocks.LAVA));
        assertTrue(FluidBlocks.isFluid(Blocks.WATER));
        assertTrue(FluidBlocks.isFluid(Blocks.LAVA));
        assertFalse(FluidBlocks.isFluid(Blocks.ICE));
        assertFalse(FluidBlocks.isFluid(Blocks.STONE));
    }

    @Test
    void exposesStableFluidKind() {
        assertEquals(FluidBlocks.FluidKind.WATER, FluidBlocks.kind(Blocks.WATER));
        assertEquals(FluidBlocks.FluidKind.LAVA, FluidBlocks.kind(Blocks.LAVA));
        assertEquals(FluidBlocks.FluidKind.NONE, FluidBlocks.kind(Blocks.AIR));
        assertTrue(FluidBlocks.kind(Blocks.WATER).fluid());
        assertFalse(FluidBlocks.kind(Blocks.AIR).fluid());
    }
}
