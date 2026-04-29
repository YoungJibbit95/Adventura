package dev.voxelgame.common.math;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.world.DimensionSettings;
import dev.voxelgame.common.world.InMemoryWorld;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaycastTest {
    @Test
    void returnsHitFaceAndPlacementCell() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        world.setBlockId(3, 4, 5, Blocks.STONE);

        Raycast.Hit hit = Raycast.firstSolid(
                world,
                new Vector3d(3.5, 4.5, 1.5),
                new Vector3d(0.0, 0.0, 1.0),
                8.0
        ).orElseThrow();

        assertEquals(3, hit.x());
        assertEquals(4, hit.y());
        assertEquals(5, hit.z());
        assertEquals(-1, hit.faceZ());
        assertEquals(4, hit.placeZ());
        assertTrue(hit.distance() > 3.0);
    }
}
