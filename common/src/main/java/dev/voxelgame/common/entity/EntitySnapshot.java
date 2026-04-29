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
        int health
) {
    public EntitySnapshot {
        Objects.requireNonNull(typeKey, "typeKey");
    }
}
