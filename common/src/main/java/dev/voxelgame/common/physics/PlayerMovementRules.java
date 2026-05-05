package dev.voxelgame.common.physics;

public final class PlayerMovementRules {
    private static final double MIN_DELTA_SECONDS = 1.0 / 20.0;
    private static final double MAX_DELTA_SECONDS = 0.5;
    private static final double HORIZONTAL_GRACE_BLOCKS = 1.25;
    private static final double VERTICAL_GRACE_BLOCKS = 2.0;
    private static final double UPWARD_START_EPSILON = 0.05;
    private static final double SURVIVAL_ACCELERATION_BLOCKS_PER_SECOND_SQUARED = 65.0;
    private static final double FLYING_ACCELERATION_BLOCKS_PER_SECOND_SQUARED = 220.0;
    private static final double ACCELERATION_GRACE_BLOCKS_PER_SECOND = 5.0;

    public enum MovementMode {
        SURVIVAL,
        FLYING,
        SPECTATOR
    }

    private PlayerMovementRules() {
    }

    public static boolean isFinite(double x, double y, double z, float yaw, float pitch) {
        return PhysicsNumericGuard.allFinite(x, y, z) && PhysicsNumericGuard.allFinite(yaw, pitch);
    }

    public static boolean withinVerticalBounds(double eyeY, PlayerPhysicsConfig config, int minY, int maxYExclusive) {
        PlayerBounds bounds = config.bounds();
        return bounds.minY(eyeY) >= minY && bounds.maxY(eyeY) < maxYExclusive;
    }

    public static boolean withinVerticalBounds(double eyeY, PlayerPhysicsConfig config, PhysicsStepContext context) {
        return context != null && withinVerticalBounds(eyeY, config, context.dimension().minY(), context.dimension().maxYExclusive());
    }

    public static boolean groundedClaimPlausible(boolean claimedOnGround, boolean hasGroundSupport) {
        return !claimedOnGround || hasGroundSupport;
    }

    public static boolean waterStateClaimPlausible(PlayerWaterState claimed, PlayerWaterState actual) {
        if (claimed == null || actual == null) {
            return false;
        }
        return (!claimed.feetInWater() || actual.feetInWater())
                && (!claimed.bodyInWater() || actual.bodyInWater())
                && (!claimed.headUnderwater() || actual.headUnderwater());
    }

    public static boolean waterMovementAssist(PlayerWaterState water) {
        return water != null && water.movementAffected();
    }

    public static boolean upwardMovementPlausible(
            double fromY,
            double toY,
            boolean previousGrounded,
            double previousDeltaY,
            boolean movementAssist
    ) {
        if (!Double.isFinite(fromY) || !Double.isFinite(toY) || !Double.isFinite(previousDeltaY)) {
            return false;
        }
        double deltaY = toY - fromY;
        if (deltaY <= UPWARD_START_EPSILON) {
            return true;
        }
        return previousGrounded || previousDeltaY > UPWARD_START_EPSILON || movementAssist;
    }

    public static boolean withinInitialSyncDistance(
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            double maxDistance
    ) {
        if (maxDistance < 0.0) {
            return false;
        }
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        return dx * dx + dy * dy + dz * dz <= maxDistance * maxDistance;
    }

    public static boolean isPlausibleDelta(
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            double deltaSeconds,
            PlayerPhysicsConfig config
    ) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0) {
            return false;
        }
        double seconds = validationDeltaSeconds(deltaSeconds);
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double maxHorizontalDistance = config.flySprintSpeed() * seconds + HORIZONTAL_GRACE_BLOCKS;
        double maxVerticalDistance = Math.max(config.maxFallSpeed(), config.flySprintSpeed()) * seconds + VERTICAL_GRACE_BLOCKS;
        return horizontalDistance <= maxHorizontalDistance && Math.abs(dy) <= maxVerticalDistance;
    }

    public static boolean isPlausibleSurvivalDelta(
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            double deltaSeconds,
            PlayerPhysicsConfig config,
            PlayerWaterState water
    ) {
        return isPlausibleSurvivalDelta(
                fromX,
                fromY,
                fromZ,
                toX,
                toY,
                toZ,
                deltaSeconds,
                config,
                water,
                BlockSurfacePhysics.DEFAULT,
                BlockSurfacePhysics.DEFAULT
        );
    }

    public static boolean isPlausibleSurvivalDelta(
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            double deltaSeconds,
            PlayerPhysicsConfig config,
            PlayerWaterState water,
            BlockSurfacePhysics.SurfaceMaterial fromSurface,
            BlockSurfacePhysics.SurfaceMaterial toSurface
    ) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0) {
            return false;
        }
        double seconds = validationDeltaSeconds(deltaSeconds);
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double surfaceSpeedMultiplier = surfaceSpeedMultiplier(water, fromSurface, toSurface);
        double maxHorizontalDistance = config.sprintSpeed() * surfaceSpeedMultiplier * seconds + HORIZONTAL_GRACE_BLOCKS;
        double maxVerticalSpeed = waterMovementAssist(water) ? config.maxWaterFallSpeed() : config.maxFallSpeed();
        double maxVerticalDistance = maxVerticalSpeed * seconds + VERTICAL_GRACE_BLOCKS;
        return horizontalDistance <= maxHorizontalDistance && Math.abs(dy) <= maxVerticalDistance;
    }

    public static boolean isPlausibleModeDelta(
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            double deltaSeconds,
            PlayerPhysicsConfig config,
            PlayerWaterState water,
            MovementMode mode
    ) {
        if (mode == null) {
            return false;
        }
        return switch (mode) {
            case SURVIVAL -> isPlausibleSurvivalDelta(fromX, fromY, fromZ, toX, toY, toZ, deltaSeconds, config, water);
            case FLYING, SPECTATOR -> isPlausibleDelta(fromX, fromY, fromZ, toX, toY, toZ, deltaSeconds, config);
        };
    }

    public static boolean isPlausibleModeDelta(
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            PlayerPhysicsConfig config,
            PhysicsStepContext context
    ) {
        if (context == null) {
            return false;
        }
        return isPlausibleModeDelta(
                fromX,
                fromY,
                fromZ,
                toX,
                toY,
                toZ,
                context.deltaSeconds(),
                config,
                context.waterState(),
                context.movementMode()
        );
    }

    public static boolean isPlausibleModeDelta(
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            double deltaSeconds,
            PlayerPhysicsConfig config,
            PlayerWaterState water,
            MovementMode mode,
            BlockSurfacePhysics.SurfaceMaterial fromSurface,
            BlockSurfacePhysics.SurfaceMaterial toSurface
    ) {
        if (mode == null) {
            return false;
        }
        return switch (mode) {
            case SURVIVAL -> isPlausibleSurvivalDelta(
                    fromX,
                    fromY,
                    fromZ,
                    toX,
                    toY,
                    toZ,
                    deltaSeconds,
                    config,
                    water,
                    fromSurface,
                    toSurface
            );
            case FLYING, SPECTATOR -> isPlausibleDelta(fromX, fromY, fromZ, toX, toY, toZ, deltaSeconds, config);
        };
    }

    public static boolean isPlausibleHorizontalAcceleration(
            double previousDeltaX,
            double previousDeltaZ,
            double previousDeltaSeconds,
            double currentDeltaX,
            double currentDeltaZ,
            double currentDeltaSeconds,
            PlayerPhysicsConfig config,
            MovementMode mode
    ) {
        if (config == null || mode == null
                || !Double.isFinite(previousDeltaX)
                || !Double.isFinite(previousDeltaZ)
                || !Double.isFinite(previousDeltaSeconds)
                || !Double.isFinite(currentDeltaX)
                || !Double.isFinite(currentDeltaZ)
                || !Double.isFinite(currentDeltaSeconds)
                || previousDeltaSeconds < 0.0
                || currentDeltaSeconds < 0.0) {
            return false;
        }
        double previousSeconds = validationDeltaSeconds(previousDeltaSeconds);
        double currentSeconds = validationDeltaSeconds(currentDeltaSeconds);
        double previousSpeed = Math.sqrt(previousDeltaX * previousDeltaX + previousDeltaZ * previousDeltaZ) / previousSeconds;
        double currentSpeed = Math.sqrt(currentDeltaX * currentDeltaX + currentDeltaZ * currentDeltaZ) / currentSeconds;
        double acceleration = switch (mode) {
            case SURVIVAL -> SURVIVAL_ACCELERATION_BLOCKS_PER_SECOND_SQUARED;
            case FLYING, SPECTATOR -> FLYING_ACCELERATION_BLOCKS_PER_SECOND_SQUARED;
        };
        double modeSpeed = switch (mode) {
            case SURVIVAL -> config.sprintSpeed();
            case FLYING, SPECTATOR -> config.flySprintSpeed();
        };
        double maxSpeedAfterAcceleration = previousSpeed + acceleration * currentSeconds + ACCELERATION_GRACE_BLOCKS_PER_SECOND;
        return currentSpeed <= Math.max(modeSpeed + ACCELERATION_GRACE_BLOCKS_PER_SECOND, maxSpeedAfterAcceleration);
    }

    private static double validationDeltaSeconds(double deltaSeconds) {
        return Math.max(MIN_DELTA_SECONDS, Math.min(deltaSeconds, MAX_DELTA_SECONDS));
    }

    private static double surfaceSpeedMultiplier(
            PlayerWaterState water,
            BlockSurfacePhysics.SurfaceMaterial fromSurface,
            BlockSurfacePhysics.SurfaceMaterial toSurface
    ) {
        if (waterMovementAssist(water)) {
            return 1.0;
        }
        return Math.max(safeSurface(fromSurface).speedMultiplier(), safeSurface(toSurface).speedMultiplier());
    }

    private static BlockSurfacePhysics.SurfaceMaterial safeSurface(BlockSurfacePhysics.SurfaceMaterial surface) {
        return surface == null ? BlockSurfacePhysics.DEFAULT : surface;
    }
}
