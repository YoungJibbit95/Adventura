package dev.voxelgame.common.physics;

public record PlayerPhysicsConfig(
        PlayerBounds bounds,
        float gravity,
        float jumpSpeed,
        float walkSpeed,
        float sprintSpeed,
        float flySpeed,
        float flySprintSpeed,
        float waterSpeedMultiplier,
        float waterHorizontalDrag,
        float waterGravityMultiplier,
        float waterVerticalDrag,
        float swimRiseSpeed,
        float maxFallSpeed,
        float maxWaterFallSpeed,
        float groundProbeDistance,
        float maxCollisionStep
) {
    public static PlayerPhysicsConfig defaults() {
        return new PlayerPhysicsConfig(
                PlayerBounds.DEFAULT,
                34.0f,
                9.2f,
                5.2f,
                8.0f,
                18.0f,
                44.0f,
                0.58f,
                0.82f,
                0.18f,
                0.88f,
                3.6f,
                42.0f,
                7.0f,
                0.08f,
                0.45f
        );
    }

    public PlayerPhysicsConfig {
        if (bounds == null) {
            throw new IllegalArgumentException("Player bounds are required");
        }
        if (gravity <= 0.0f || jumpSpeed <= 0.0f || walkSpeed <= 0.0f || sprintSpeed <= 0.0f
                || flySpeed <= 0.0f || flySprintSpeed <= 0.0f) {
            throw new IllegalArgumentException("Movement speeds must be positive");
        }
        if (waterSpeedMultiplier <= 0.0f || waterHorizontalDrag <= 0.0f
                || waterGravityMultiplier <= 0.0f || waterVerticalDrag <= 0.0f) {
            throw new IllegalArgumentException("Water physics factors must be positive");
        }
        if (swimRiseSpeed <= 0.0f || maxFallSpeed <= 0.0f || maxWaterFallSpeed <= 0.0f) {
            throw new IllegalArgumentException("Vertical speed limits must be positive");
        }
        if (groundProbeDistance < 0.0f || maxCollisionStep <= 0.0f) {
            throw new IllegalArgumentException("Collision distances must be valid");
        }
    }
}
