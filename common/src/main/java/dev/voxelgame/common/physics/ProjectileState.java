package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntitySnapshot;

import java.util.Objects;
import java.util.UUID;

public record ProjectileState(
        long projectileId,
        UUID ownerPlayerId,
        String typeKey,
        double x,
        double y,
        double z,
        double velocityX,
        double velocityY,
        double velocityZ,
        int ageTicks
) {
    public ProjectileState {
        typeKey = Objects.requireNonNull(typeKey, "typeKey");
        if (typeKey.isBlank()) {
            throw new IllegalArgumentException("Projectile type key must not be blank");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Double.isFinite(velocityX) || !Double.isFinite(velocityY) || !Double.isFinite(velocityZ)
                || ageTicks < 0) {
            throw new IllegalArgumentException("Projectile state values must be finite");
        }
    }

    public EntitySnapshot snapshot() {
        double horizontal = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        float yaw = horizontal <= 0.0001 ? 0.0f : (float) Math.toDegrees(Math.atan2(velocityZ, velocityX));
        float pitch = horizontal <= 0.0001 ? 0.0f : (float) Math.toDegrees(Math.atan2(velocityY, horizontal));
        return new EntitySnapshot(
                projectileId,
                typeKey,
                ownerPlayerId,
                x,
                y,
                z,
                yaw,
                pitch,
                1,
                EntitySnapshot.STATE_PROJECTILE,
                velocityX,
                velocityY,
                velocityZ
        );
    }
}
