package dev.voxelgame.common.physics;

public record PlayerState(
        double x,
        double y,
        double z,
        float velocityX,
        float velocityY,
        float velocityZ,
        boolean onGround,
        boolean underwater,
        float fallImpactSpeed
) {
    public PlayerState {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(velocityX) || !Float.isFinite(velocityY) || !Float.isFinite(velocityZ)
                || !Float.isFinite(fallImpactSpeed)) {
            throw new IllegalArgumentException("Player state must be finite");
        }
    }

    public PlayerState withPosition(double x, double y, double z) {
        return new PlayerState(x, y, z, velocityX, velocityY, velocityZ, onGround, underwater, fallImpactSpeed);
    }

    public PlayerState withVelocity(float velocityX, float velocityY, float velocityZ) {
        return new PlayerState(x, y, z, velocityX, velocityY, velocityZ, onGround, underwater, fallImpactSpeed);
    }

    public PlayerState withGrounded(boolean onGround) {
        return new PlayerState(x, y, z, velocityX, velocityY, velocityZ, onGround, underwater, fallImpactSpeed);
    }
}
