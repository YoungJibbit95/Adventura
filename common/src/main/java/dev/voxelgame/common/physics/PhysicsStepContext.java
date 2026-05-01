package dev.voxelgame.common.physics;

import dev.voxelgame.common.world.DimensionSettings;

import java.util.Objects;

public record PhysicsStepContext(
        double deltaSeconds,
        long tick,
        DimensionSettings dimension,
        PlayerMovementRules.MovementMode movementMode,
        PlayerWaterState waterState,
        String debugSource,
        long debugSequence
) {
    public static PhysicsStepContext survival(double deltaSeconds, long tick, DimensionSettings dimension, PlayerWaterState waterState) {
        return new PhysicsStepContext(
                deltaSeconds,
                tick,
                dimension,
                PlayerMovementRules.MovementMode.SURVIVAL,
                waterState,
                "survival",
                0L
        );
    }

    public static PhysicsStepContext flying(double deltaSeconds, long tick, DimensionSettings dimension, PlayerWaterState waterState) {
        return new PhysicsStepContext(
                deltaSeconds,
                tick,
                dimension,
                PlayerMovementRules.MovementMode.FLYING,
                waterState,
                "flying",
                0L
        );
    }

    public static PhysicsStepContext projectile(double deltaSeconds, long tick, DimensionSettings dimension, String debugSource) {
        return new PhysicsStepContext(
                deltaSeconds,
                tick,
                dimension,
                PlayerMovementRules.MovementMode.SURVIVAL,
                new PlayerWaterState(false, false, false),
                debugSource == null || debugSource.isBlank() ? "projectile" : debugSource,
                0L
        );
    }

    public PhysicsStepContext {
        PhysicsNumericGuard.requireFiniteNonNegative("Physics delta seconds", deltaSeconds);
        if (tick < 0L) {
            throw new IllegalArgumentException("Physics tick must be non-negative");
        }
        dimension = Objects.requireNonNull(dimension, "dimension");
        movementMode = movementMode == null ? PlayerMovementRules.MovementMode.SURVIVAL : movementMode;
        waterState = waterState == null ? new PlayerWaterState(false, false, false) : waterState;
        debugSource = debugSource == null ? "" : debugSource;
        if (debugSequence < 0L) {
            throw new IllegalArgumentException("Physics debug sequence must be non-negative");
        }
    }

    public float floatDeltaSeconds() {
        return PhysicsNumericGuard.floatDeltaSeconds(deltaSeconds);
    }

    public boolean containsEyeY(PlayerPhysicsConfig config, double eyeY) {
        return PlayerMovementRules.withinVerticalBounds(eyeY, config, dimension.minY(), dimension.maxYExclusive());
    }

    public PhysicsStepContext withDebug(String source, long sequence) {
        return new PhysicsStepContext(deltaSeconds, tick, dimension, movementMode, waterState, source, sequence);
    }

    public PhysicsStepContext withWaterState(PlayerWaterState waterState) {
        return new PhysicsStepContext(deltaSeconds, tick, dimension, movementMode, waterState, debugSource, debugSequence);
    }
}
