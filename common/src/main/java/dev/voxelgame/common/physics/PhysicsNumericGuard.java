package dev.voxelgame.common.physics;

public final class PhysicsNumericGuard {
    private PhysicsNumericGuard() {
    }

    public static boolean allFinite(double... values) {
        if (values == null) {
            return false;
        }
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    public static boolean allFinite(float... values) {
        if (values == null) {
            return false;
        }
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    public static double requireFinite(String label, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
        return value;
    }

    public static float requireFinite(String label, float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
        return value;
    }

    public static double requireFiniteNonNegative(String label, double value) {
        requireFinite(label, value);
        if (value < 0.0) {
            throw new IllegalArgumentException(label + " must be non-negative");
        }
        return value;
    }

    public static float requireFiniteNonNegative(String label, float value) {
        requireFinite(label, value);
        if (value < 0.0f) {
            throw new IllegalArgumentException(label + " must be non-negative");
        }
        return value;
    }

    public static double requireFinitePositive(String label, double value) {
        requireFinite(label, value);
        if (value <= 0.0) {
            throw new IllegalArgumentException(label + " must be positive");
        }
        return value;
    }

    public static float requireFinitePositive(String label, float value) {
        requireFinite(label, value);
        if (value <= 0.0f) {
            throw new IllegalArgumentException(label + " must be positive");
        }
        return value;
    }

    public static void requireFinitePosition(double x, double y, double z) {
        if (!allFinite(x, y, z)) {
            throw new IllegalArgumentException("Physics position must be finite");
        }
    }

    public static void requireFiniteVelocity(double x, double y, double z) {
        if (!allFinite(x, y, z)) {
            throw new IllegalArgumentException("Physics velocity must be finite");
        }
    }

    public static void requireFiniteBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!allFinite(minX, minY, minZ, maxX, maxY, maxZ)
                || maxX < minX
                || maxY < minY
                || maxZ < minZ) {
            throw new IllegalArgumentException("Physics bounds must be finite and ordered");
        }
    }

    public static float floatDeltaSeconds(double deltaSeconds) {
        requireFiniteNonNegative("Delta seconds", deltaSeconds);
        if (deltaSeconds > Float.MAX_VALUE) {
            throw new IllegalArgumentException("Delta seconds is too large for float physics");
        }
        return (float) deltaSeconds;
    }
}
