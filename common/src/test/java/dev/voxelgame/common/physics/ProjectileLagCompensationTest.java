package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntitySnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileLagCompensationTest {
    @Test
    void requestedRewindIsClampedToFairWindow() {
        ProjectileLagCompensation.RewindWindow window = ProjectileLagCompensation.evaluate(1.0);

        assertEquals(ProjectileLagCompensation.MAX_REWIND_SECONDS, window.appliedSeconds(), 0.0001);
        assertTrue(window.clipped());
    }

    @Test
    void linearRewindUsesSnapshotVelocity() {
        EntitySnapshot moving = new EntitySnapshot(42L, "voxel:cozy_sheep", null, 10.0, 80.0, 5.0, 0.0f, 0.0f, 10)
                .withVelocity(2.0, 0.0, -1.0);

        EntitySnapshot rewind = ProjectileLagCompensation.rewindLinear(moving, 0.10);

        assertEquals(9.8, rewind.x(), 0.0001);
        assertEquals(5.1, rewind.z(), 0.0001);
    }
}
