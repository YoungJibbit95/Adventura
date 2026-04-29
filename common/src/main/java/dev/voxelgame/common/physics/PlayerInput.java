package dev.voxelgame.common.physics;

public record PlayerInput(
        float moveX,
        float moveZ,
        boolean jump,
        boolean descend,
        boolean sprint
) {
    public static PlayerInput idle() {
        return new PlayerInput(0.0f, 0.0f, false, false, false);
    }

    public PlayerInput {
        if (!Float.isFinite(moveX) || !Float.isFinite(moveZ)) {
            throw new IllegalArgumentException("Player input must be finite");
        }
        float lengthSquared = moveX * moveX + moveZ * moveZ;
        if (lengthSquared > 1.0f && lengthSquared > 0.0f) {
            float length = (float) Math.sqrt(lengthSquared);
            moveX /= length;
            moveZ /= length;
        }
    }
}
