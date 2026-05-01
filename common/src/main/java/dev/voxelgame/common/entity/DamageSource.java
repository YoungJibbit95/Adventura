package dev.voxelgame.common.entity;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record DamageSource(
        Type type,
        UUID attackerPlayerId,
        long sourceEntityId,
        String causeKey
) {
    public static DamageSource playerMelee(UUID attackerPlayerId) {
        return new DamageSource(Type.PLAYER_MELEE, attackerPlayerId, 0L, "player_melee");
    }

    public static DamageSource projectile(UUID attackerPlayerId, long projectileEntityId, String projectileTypeKey) {
        return new DamageSource(Type.PROJECTILE, attackerPlayerId, projectileEntityId, projectileTypeKey);
    }

    public static DamageSource fall() {
        return typed(Type.FALL);
    }

    public static DamageSource fire() {
        return typed(Type.FIRE);
    }

    public static DamageSource drowning() {
        return typed(Type.DROWNING);
    }

    public static DamageSource environment(String causeKey) {
        return new DamageSource(Type.ENVIRONMENT, null, 0L, causeKey);
    }

    public static DamageSource unknown() {
        return typed(Type.UNKNOWN);
    }

    private static DamageSource typed(Type type) {
        return new DamageSource(type, null, 0L, type.name().toLowerCase(Locale.ROOT));
    }

    public DamageSource {
        type = Objects.requireNonNull(type, "type");
        if (causeKey == null || causeKey.isBlank()) {
            causeKey = type.name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Type {
        PLAYER_MELEE,
        PROJECTILE,
        FALL,
        FIRE,
        DROWNING,
        ENVIRONMENT,
        UNKNOWN
    }
}
