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
        float fallImpactSpeed,
        float coyoteTimeSeconds,
        float jumpBufferSeconds
) {
    public PlayerState(
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
        this(x, y, z, velocityX, velocityY, velocityZ, onGround, underwater, fallImpactSpeed, onGround ? 0.10f : 0.0f, 0.0f);
    }

    public PlayerState {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(velocityX) || !Float.isFinite(velocityY) || !Float.isFinite(velocityZ)
                || !Float.isFinite(fallImpactSpeed)
                || !Float.isFinite(coyoteTimeSeconds)
                || !Float.isFinite(jumpBufferSeconds)) {
            throw new IllegalArgumentException("Player state must be finite");
        }
        coyoteTimeSeconds = Math.max(0.0f, coyoteTimeSeconds);
        jumpBufferSeconds = Math.max(0.0f, jumpBufferSeconds);
    }

    public PlayerState withPosition(double x, double y, double z) {
        return new PlayerState(x, y, z, velocityX, velocityY, velocityZ, onGround, underwater, fallImpactSpeed, coyoteTimeSeconds, jumpBufferSeconds);
    }

    public PlayerState withVelocity(float velocityX, float velocityY, float velocityZ) {
        return new PlayerState(x, y, z, velocityX, velocityY, velocityZ, onGround, underwater, fallImpactSpeed, coyoteTimeSeconds, jumpBufferSeconds);
    }

    public PlayerState withGrounded(boolean onGround) {
        return new PlayerState(x, y, z, velocityX, velocityY, velocityZ, onGround, underwater, fallImpactSpeed, coyoteTimeSeconds, jumpBufferSeconds);
    }
}
