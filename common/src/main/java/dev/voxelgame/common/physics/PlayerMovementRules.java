package dev.voxelgame.common.physics;

public final class PlayerMovementRules {
    private static final double MIN_DELTA_SECONDS = 1.0 / 20.0;
    private static final double MAX_DELTA_SECONDS = 0.5;
    private static final double HORIZONTAL_GRACE_BLOCKS = 1.25;
    private static final double VERTICAL_GRACE_BLOCKS = 2.0;

    private PlayerMovementRules() {
    }

    public static boolean isFinite(double x, double y, double z, float yaw, float pitch) {
        return Double.isFinite(x)
                && Double.isFinite(y)
                && Double.isFinite(z)
                && Float.isFinite(yaw)
                && Float.isFinite(pitch);
    }

    public static boolean withinVerticalBounds(double eyeY, PlayerPhysicsConfig config, int minY, int maxYExclusive) {
        PlayerBounds bounds = config.bounds();
        return bounds.minY(eyeY) >= minY && bounds.maxY(eyeY) < maxYExclusive;
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

    private static double validationDeltaSeconds(double deltaSeconds) {
        return Math.max(MIN_DELTA_SECONDS, Math.min(deltaSeconds, MAX_DELTA_SECONDS));
    }
}
