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
        String stateKey
) {
    public static final String STATE_IDLE = "IDLE";
    public static final String STATE_WANDER = "WANDER";
    public static final String STATE_FLEE = "FLEE";
    public static final String STATE_FOLLOW = "FOLLOW";
    public static final String STATE_GRAZE = "GRAZE";

    public EntitySnapshot(long entityId, String typeKey, UUID ownerPlayerId, double x, double y, double z, float yaw, float pitch, int health) {
        this(entityId, typeKey, ownerPlayerId, x, y, z, yaw, pitch, health, STATE_IDLE);
    }

    public EntitySnapshot {
        Objects.requireNonNull(typeKey, "typeKey");
        Objects.requireNonNull(stateKey, "stateKey");
        if (stateKey.isBlank()) {
            throw new IllegalArgumentException("stateKey cannot be blank");
        }
    }
}
