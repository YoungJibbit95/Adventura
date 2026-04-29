package dev.voxelgame.common.physics;

public record PlayerWaterState(boolean feetInWater, boolean bodyInWater, boolean headUnderwater) {
    public boolean movementAffected() {
        return feetInWater || bodyInWater || headUnderwater;
    }
}
