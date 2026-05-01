package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.entity.ItemDropType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("physicsRegression")
class EntityPhysicsTest {
    @Test
    void entityProfilesClassifyMovementAndWaterBehavior() {
        EntityPhysicsProfile firefly = EntityPhysicsProfile.forType("voxel:firefly_swarm");
        EntityPhysicsProfile sheep = EntityPhysicsProfile.forType("voxel:cozy_sheep");
        EntityPhysicsProfile drop = EntityPhysicsProfile.forType(ItemDropType.typeKey("voxel:moss_clump"));

        assertTrue(firefly.ignoresTerrainSupport());
        assertFalse(sheep.ignoresTerrainSupport());
        assertTrue(sheep.blocksWaterPlacement());
        assertEquals(EntityPhysicsProfile.WaterBehavior.FLOAT, drop.waterBehavior());
    }

    @Test
    void separationPushesOverlappingEntitiesApart() {
        EntitySnapshot mover = new EntitySnapshot(1L, "voxel:cozy_sheep", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10);
        EntitySnapshot blocker = new EntitySnapshot(2L, "voxel:cozy_sheep", null, 0.2, 80.0, 0.0, 0.0f, 0.0f, 10);

        EntitySnapshot separated = EntityPhysics.applySeparation(mover, mover, java.util.List.of(blocker));

        assertTrue(Math.abs(separated.x() - mover.x()) > 0.01 || Math.abs(separated.z() - mover.z()) > 0.01);
    }

    @Test
    void sweepWithSlideKeepsFreeAxisInsteadOfCancellingMove() {
        EntitySnapshot current = new EntitySnapshot(3L, "voxel:cozy_sheep", null, 1.0, 80.0, 1.0, 0.0f, 0.0f, 10);
        EntitySnapshot candidate = new EntitySnapshot(3L, "voxel:cozy_sheep", null, 1.3, 80.0, 1.4, 0.0f, 0.0f, 10)
                .withVelocity(0.3, 0.0, 0.4);

        EntityPhysics.MoveResult result = EntityPhysics.sweepWithSlide(
                current,
                candidate,
                (from, next) -> next.x() <= 1.05
        );

        assertTrue(result.slid());
        assertEquals(current.x(), result.snapshot().x(), 0.001);
        assertTrue(result.snapshot().z() > current.z());
        assertEquals(0.0, result.snapshot().velocityX(), 0.001);
        assertTrue(result.snapshot().velocityZ() > 0.0);
    }
}
