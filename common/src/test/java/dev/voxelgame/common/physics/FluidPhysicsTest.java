package dev.voxelgame.common.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidPhysicsTest {
    @Test
    void floatingBodyReceivesBuoyancyAndCurrent() {
        FluidPhysics.Velocity velocity = FluidPhysics.applyFloatingBodyForces(
                new FluidPhysics.Velocity(0.0, -0.4, 0.0),
                0.05,
                FluidPhysics.water(0.6, 0.0, -0.2)
        );

        assertTrue(velocity.x() > 0.0);
        assertTrue(velocity.z() < 0.0);
        assertTrue(velocity.y() > -0.4);
    }

    @Test
    void lavaAppliesHighViscosityToProjectiles() {
        FluidPhysics.Velocity fast = new FluidPhysics.Velocity(8.0, 0.0, 0.0);

        FluidPhysics.Velocity water = FluidPhysics.applyProjectileForces(fast, 0.05, 9.8, FluidPhysics.stillWater());
        FluidPhysics.Velocity lava = FluidPhysics.applyProjectileForces(fast, 0.05, 9.8, FluidPhysics.stillLava());

        assertTrue(Math.abs(lava.x()) < Math.abs(water.x()));
        assertTrue(lava.y() < water.y());
        assertEquals(0.34, FluidPhysics.stillLava().drag(), 0.001);
    }
}
