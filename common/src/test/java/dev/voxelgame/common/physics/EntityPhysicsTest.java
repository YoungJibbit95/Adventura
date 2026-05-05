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
        assertEquals(0.0, firefly.fluidBuoyancyFactor(), 0.001);
        assertEquals(0.35, sheep.fluidBuoyancyFactor(), 0.001);
        assertEquals(0.90, drop.fluidBuoyancyFactor(), 0.001);
    }

    @Test
    void fluidForcesLiftTinyEntitiesMoreThanHeavyEntities() {
        EntitySnapshot bunny = new EntitySnapshot(10L, "voxel:forest_bunny", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10);
        EntitySnapshot boar = new EntitySnapshot(11L, "voxel:little_boar", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10);
        FluidPhysics.FluidSample current = FluidPhysics.water(0.40, 0.02, -0.20);

        EntitySnapshot floatingBunny = EntityPhysics.applyFluidForces(
                bunny,
                EntityPhysicsProfile.forType(bunny.typeKey()),
                current,
                0.05
        );
        EntitySnapshot heavyBoar = EntityPhysics.applyFluidForces(
                boar,
                EntityPhysicsProfile.forType(boar.typeKey()),
                current,
                0.05
        );

        assertTrue(floatingBunny.velocityY() > heavyBoar.velocityY());
        assertTrue(Math.abs(floatingBunny.velocityX()) > Math.abs(heavyBoar.velocityX()));
    }

    @Test
    void swimmerKeepsControlledMotionInFluid() {
        EntityPhysicsProfile swimmer = new EntityPhysicsProfile(
                EntityPhysicsProfile.MovementClass.SWIMMER,
                EntityPhysicsProfile.WaterBehavior.SWIM,
                0.06,
                0.10,
                0.08,
                0.82,
                0.03,
                0.20,
                0.55
        );
        EntitySnapshot current = new EntitySnapshot(12L, "voxel:test_swimmer", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10)
                .withVelocity(0.24, 0.02, 0.06);

        EntitySnapshot moved = EntityPhysics.applyFluidForces(
                current,
                swimmer,
                FluidPhysics.water(-0.60, 0.08, -0.40),
                0.05
        );

        assertTrue(moved.velocityX() > 0.0);
        assertTrue(moved.velocityZ() > 0.0);
        assertTrue(moved.velocityY() <= 0.12);
    }

    @Test
    void flyerIgnoresFluidForces() {
        EntitySnapshot firefly = new EntitySnapshot(13L, "voxel:firefly_swarm", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10)
                .withVelocity(0.18, 0.07, -0.11);

        EntitySnapshot moved = EntityPhysics.applyFluidForces(
                firefly,
                EntityPhysicsProfile.forType(firefly.typeKey()),
                FluidPhysics.water(-1.0, 0.20, 1.0),
                0.05
        );

        assertEquals(firefly.velocityX(), moved.velocityX(), 0.001);
        assertEquals(firefly.velocityY(), moved.velocityY(), 0.001);
        assertEquals(firefly.velocityZ(), moved.velocityZ(), 0.001);
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
