package dev.voxelgame.common.entity;

import dev.voxelgame.common.physics.PhysicsNumericGuard;

import java.util.Optional;

public record DamageResult(
        boolean accepted,
        RejectionReason rejectionReason,
        int amount,
        boolean killed,
        long targetEntityId,
        EntitySnapshot updatedSnapshot,
        double knockbackX,
        double knockbackY,
        double knockbackZ
) {
    public static DamageResult accepted(long targetEntityId, int amount, boolean killed, EntitySnapshot updatedSnapshot, double knockbackX, double knockbackY, double knockbackZ) {
        return new DamageResult(true, RejectionReason.NONE, amount, killed, targetEntityId, updatedSnapshot, knockbackX, knockbackY, knockbackZ);
    }

    public static DamageResult rejected(long targetEntityId, RejectionReason rejectionReason) {
        return new DamageResult(false, rejectionReason, 0, false, targetEntityId, null, 0.0, 0.0, 0.0);
    }

    public Optional<EntitySnapshot> snapshot() {
        return Optional.ofNullable(updatedSnapshot);
    }

    public DamageResult {
        if (rejectionReason == null) {
            rejectionReason = accepted ? RejectionReason.NONE : RejectionReason.UNKNOWN_TARGET;
        }
        amount = Math.max(0, amount);
        if (!accepted) {
            amount = 0;
            killed = false;
            updatedSnapshot = null;
            knockbackX = 0.0;
            knockbackY = 0.0;
            knockbackZ = 0.0;
        }
        if (!PhysicsNumericGuard.allFinite(knockbackX, knockbackY, knockbackZ)) {
            throw new IllegalArgumentException("Damage knockback must be finite");
        }
    }

    public enum RejectionReason {
        NONE,
        INVALID_AMOUNT,
        UNKNOWN_TARGET,
        INVULNERABLE
    }
}
