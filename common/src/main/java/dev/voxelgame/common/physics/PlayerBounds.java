package dev.voxelgame.common.physics;

public record PlayerBounds(float halfWidth, float eyeHeight, float headClearance) {
    public static final PlayerBounds DEFAULT = new PlayerBounds(0.30f, 1.62f, 0.18f);

    public PlayerBounds {
        halfWidth = PhysicsNumericGuard.requireFinitePositive("Player half width", halfWidth);
        eyeHeight = PhysicsNumericGuard.requireFinitePositive("Player eye height", eyeHeight);
        headClearance = PhysicsNumericGuard.requireFiniteNonNegative("Player head clearance", headClearance);
    }

    public double minX(double eyeX) {
        return eyeX - halfWidth;
    }

    public double maxX(double eyeX) {
        return eyeX + halfWidth;
    }

    public double minY(double eyeY) {
        return eyeY - eyeHeight;
    }

    public double maxY(double eyeY) {
        return eyeY + headClearance;
    }

    public double minZ(double eyeZ) {
        return eyeZ - halfWidth;
    }

    public double maxZ(double eyeZ) {
        return eyeZ + halfWidth;
    }

    public boolean intersectsBlock(double eyeX, double eyeY, double eyeZ, int blockX, int blockY, int blockZ) {
        return minX(eyeX) < blockX + 1.0
                && maxX(eyeX) > blockX
                && minY(eyeY) < blockY + 1.0
                && maxY(eyeY) > blockY
                && minZ(eyeZ) < blockZ + 1.0
                && maxZ(eyeZ) > blockZ;
    }
}
