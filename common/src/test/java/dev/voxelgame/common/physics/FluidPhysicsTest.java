package dev.voxelgame.common.physics;

import org.junit.jupiter.api.Test;

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
}
