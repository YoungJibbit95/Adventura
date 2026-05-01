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
        gravity = PhysicsNumericGuard.requireFinitePositive("Player gravity", gravity);
        jumpSpeed = PhysicsNumericGuard.requireFinitePositive("Player jump speed", jumpSpeed);
        walkSpeed = PhysicsNumericGuard.requireFinitePositive("Player walk speed", walkSpeed);
        sprintSpeed = PhysicsNumericGuard.requireFinitePositive("Player sprint speed", sprintSpeed);
        flySpeed = PhysicsNumericGuard.requireFinitePositive("Player fly speed", flySpeed);
        flySprintSpeed = PhysicsNumericGuard.requireFinitePositive("Player fly sprint speed", flySprintSpeed);
        waterSpeedMultiplier = PhysicsNumericGuard.requireFinitePositive("Player water speed multiplier", waterSpeedMultiplier);
        waterHorizontalDrag = PhysicsNumericGuard.requireFinitePositive("Player water horizontal drag", waterHorizontalDrag);
        waterGravityMultiplier = PhysicsNumericGuard.requireFinitePositive("Player water gravity multiplier", waterGravityMultiplier);
        waterVerticalDrag = PhysicsNumericGuard.requireFinitePositive("Player water vertical drag", waterVerticalDrag);
        swimRiseSpeed = PhysicsNumericGuard.requireFinitePositive("Player swim rise speed", swimRiseSpeed);
        maxFallSpeed = PhysicsNumericGuard.requireFinitePositive("Player max fall speed", maxFallSpeed);
        maxWaterFallSpeed = PhysicsNumericGuard.requireFinitePositive("Player max water fall speed", maxWaterFallSpeed);
        groundProbeDistance = PhysicsNumericGuard.requireFiniteNonNegative("Player ground probe distance", groundProbeDistance);
        maxCollisionStep = PhysicsNumericGuard.requireFinitePositive("Player max collision step", maxCollisionStep);
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

    public String fingerprint() {
        return PhysicsConfigSnapshot.playerFingerprint(this);
    }
}
