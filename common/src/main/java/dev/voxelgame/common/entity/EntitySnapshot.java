package dev.voxelgame.common.entity;

import java.util.Objects;
import java.util.UUID;

public record EntitySnapshot(
        long entityId,
        String typeKey,
        UUID ownerPlayerId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        int health,
        String stateKey,
        double velocityX,
        double velocityY,
        double velocityZ
) {
    public static final String STATE_IDLE = "IDLE";
    public static final String STATE_WANDER = "WANDER";
    public static final String STATE_FLEE = "FLEE";
    public static final String STATE_FOLLOW = "FOLLOW";
    public static final String STATE_GRAZE = "GRAZE";

    public EntitySnapshot(long entityId, String typeKey, UUID ownerPlayerId, double x, double y, double z, float yaw, float pitch, int health) {
        this(entityId, typeKey, ownerPlayerId, x, y, z, yaw, pitch, health, STATE_IDLE, 0.0, 0.0, 0.0);
    }

    public EntitySnapshot(long entityId, String typeKey, UUID ownerPlayerId, double x, double y, double z, float yaw, float pitch, int health, String stateKey) {
        this(entityId, typeKey, ownerPlayerId, x, y, z, yaw, pitch, health, stateKey, 0.0, 0.0, 0.0);
    }

    public EntitySnapshot withVelocity(double vx, double vy, double vz) {
        return new EntitySnapshot(entityId, typeKey, ownerPlayerId, x, y, z, yaw, pitch, health, stateKey, vx, vy, vz);
    }

    public EntitySnapshot {
        Objects.requireNonNull(typeKey, "typeKey");
        Objects.requireNonNull(stateKey, "stateKey");
        if (stateKey.isBlank()) {
            throw new IllegalArgumentException("stateKey cannot be blank");
        }
        if (health < 0) {
            throw new IllegalArgumentException("health cannot be negative");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)
                || !Double.isFinite(velocityX) || !Double.isFinite(velocityY) || !Double.isFinite(velocityZ)) {
            throw new IllegalArgumentException("Entity snapshot coordinates must be finite");
        }
    }
}
