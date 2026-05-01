package dev.voxelgame.server.entity;

import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.physics.FluidPhysics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DroppedItemEntityTest {
    @Test
    void fluidCurrentAndBuoyancyMoveDroppedItems() {
        DroppedItemEntity drop = new DroppedItemEntity(
                -1L,
                "voxel:pebble",
                new ItemStack((short) 28, 1),
                0.0,
                4.0,
                0.0,
                0.0,
                0.0,
                -0.4,
                0.0,
                0L
        );

        DroppedItemEntity next = drop.tick(1L, (x, y, z) -> FluidPhysics.water(0.5, 0.0, -0.25));

        assertTrue(next.velocityX() > drop.velocityX());
        assertTrue(next.velocityZ() < drop.velocityZ());
        assertTrue(next.velocityY() > drop.velocityY());
    }
}
