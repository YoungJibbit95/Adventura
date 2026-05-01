package dev.voxelgame.common.physics;

public record ProjectileBounds(float radius) {
    public static final ProjectileBounds ARROW = new ProjectileBounds(0.09f);

    public ProjectileBounds {
        radius = PhysicsNumericGuard.requireFinitePositive("Projectile radius", radius);
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
