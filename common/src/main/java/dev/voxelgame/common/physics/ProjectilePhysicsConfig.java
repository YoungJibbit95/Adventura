package dev.voxelgame.common.physics;

import java.util.Objects;

public record ProjectilePhysicsConfig(
        String typeKey,
        ProjectileBounds bounds,
        double initialSpeed,
        double gravity,
        double waterDrag,
        int maxLifetimeTicks,
        double maxStep,
        int damage
) {
    public static ProjectilePhysicsConfig arrow() {
        return new ProjectilePhysicsConfig(
                "voxel:arrow_projectile",
                ProjectileBounds.ARROW,
                36.0,
                18.0,
                0.72,
                160,
                0.18,
                4
        );
    }

    public ProjectilePhysicsConfig {
        typeKey = Objects.requireNonNull(typeKey, "typeKey");
        bounds = Objects.requireNonNull(bounds, "bounds");
        if (typeKey.isBlank()) {
            throw new IllegalArgumentException("Projectile type key must not be blank");
        }
        if (!Double.isFinite(initialSpeed) || initialSpeed <= 0.0
                || !Double.isFinite(gravity) || gravity < 0.0
                || !Double.isFinite(waterDrag) || waterDrag <= 0.0 || waterDrag > 1.0
                || !Double.isFinite(maxStep) || maxStep <= 0.0
                || maxLifetimeTicks <= 0
                || damage <= 0) {
            throw new IllegalArgumentException("Projectile config values must be valid");
        }
    }
}
