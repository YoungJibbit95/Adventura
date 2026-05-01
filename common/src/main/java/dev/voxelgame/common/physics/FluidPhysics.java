package dev.voxelgame.common.physics;

import java.util.Objects;

public final class FluidPhysics {
    private static final FluidSample AIR = new FluidSample(false, 0.0, 0.0, 0.0, 1.0, 0.0);
    private static final FluidSample STILL_WATER = new FluidSample(true, 0.0, 0.0, 0.0, 0.72, 6.5);

    private FluidPhysics() {
    }

    @FunctionalInterface
    public interface FluidQuery {
        FluidSample sample(double x, double y, double z);
    }

    public static FluidSample air() {
        return AIR;
    }

    public static FluidSample stillWater() {
        return STILL_WATER;
    }

    public static FluidSample water(double velocityX, double velocityY, double velocityZ) {
        return new FluidSample(true, velocityX, velocityY, velocityZ, 0.72, 6.5);
    }

    public static FluidSample water(double velocityX, double velocityY, double velocityZ, double drag, double buoyancy) {
        return new FluidSample(true, velocityX, velocityY, velocityZ, drag, buoyancy);
    }

    public static Velocity applyProjectileForces(Velocity velocity, double deltaSeconds, double gravity, FluidSample sample) {
        Objects.requireNonNull(velocity, "velocity");
        Objects.requireNonNull(sample, "sample");
        deltaSeconds = PhysicsNumericGuard.requireFiniteNonNegative("Fluid projectile delta", deltaSeconds);
        if (!sample.inFluid() || deltaSeconds == 0.0) {
            return new Velocity(
                    velocity.x(),
                    velocity.y() - gravity * deltaSeconds,
                    velocity.z()
            );
        }
        double currentBlend = Math.min(1.0, deltaSeconds * 4.0);
        double nextX = (velocity.x() + (sample.velocityX() - velocity.x()) * currentBlend) * sample.drag();
        double nextY = (velocity.y() + (sample.velocityY() - velocity.y()) * currentBlend) * sample.drag();
        double nextZ = (velocity.z() + (sample.velocityZ() - velocity.z()) * currentBlend) * sample.drag();
        nextY += (sample.buoyancy() - gravity * 0.35) * deltaSeconds;
        return new Velocity(nextX, nextY, nextZ);
    }

    public static Velocity applyFloatingBodyForces(Velocity velocity, double deltaSeconds, FluidSample sample) {
        Objects.requireNonNull(velocity, "velocity");
        Objects.requireNonNull(sample, "sample");
        deltaSeconds = PhysicsNumericGuard.requireFiniteNonNegative("Fluid body delta", deltaSeconds);
        if (!sample.inFluid() || deltaSeconds == 0.0) {
            return velocity;
        }
        double currentBlend = Math.min(1.0, deltaSeconds * 3.0);
        double nextX = (velocity.x() + (sample.velocityX() - velocity.x()) * currentBlend) * Math.min(sample.drag(), 0.86);
        double nextY = velocity.y() + sample.buoyancy() * 0.45 * deltaSeconds;
        double nextZ = (velocity.z() + (sample.velocityZ() - velocity.z()) * currentBlend) * Math.min(sample.drag(), 0.86);
        return new Velocity(nextX, Math.min(0.24, nextY), nextZ);
    }

    public record FluidSample(
            boolean inFluid,
            double velocityX,
            double velocityY,
            double velocityZ,
            double drag,
            double buoyancy
    ) {
        public FluidSample {
            if (!PhysicsNumericGuard.allFinite(velocityX, velocityY, velocityZ, drag, buoyancy)) {
                throw new IllegalArgumentException("Fluid sample values must be finite");
            }
            if (drag < 0.0 || drag > 1.0) {
                throw new IllegalArgumentException("Fluid drag must be in 0..1");
            }
            if (buoyancy < 0.0) {
                throw new IllegalArgumentException("Fluid buoyancy must be >= 0");
            }
        }
    }

    public record Velocity(double x, double y, double z) {
        public Velocity {
            if (!PhysicsNumericGuard.allFinite(x, y, z)) {
                throw new IllegalArgumentException("Fluid velocity must be finite");
            }
        }
    }
}
