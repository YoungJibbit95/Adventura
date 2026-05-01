package dev.voxelgame.common.physics;

public record ProjectileBounds(float radius) {
    public static final ProjectileBounds ARROW = new ProjectileBounds(0.09f);

    public ProjectileBounds {
        if (!Float.isFinite(radius) || radius <= 0.0f) {
            throw new IllegalArgumentException("Projectile radius must be finite and positive");
        }
    }

    public boolean intersectsBlock(double x, double y, double z, int blockX, int blockY, int blockZ) {
        return x + radius > blockX
                && x - radius < blockX + 1.0
                && y + radius > blockY
                && y - radius < blockY + 1.0
                && z + radius > blockZ
                && z - radius < blockZ + 1.0;
    }
}
