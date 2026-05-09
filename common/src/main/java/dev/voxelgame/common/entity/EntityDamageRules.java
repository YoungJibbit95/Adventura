package dev.voxelgame.common.entity;

import dev.voxelgame.common.physics.PhysicsNumericGuard;
import dev.voxelgame.common.gameplay.CreatureCombatRules;

import java.util.Objects;
import java.util.Optional;

public final class EntityDamageRules {
    public static final double DEFAULT_INVULNERABILITY_SECONDS = 0.28;
    public static final double DEFAULT_VERTICAL_KNOCKBACK = 0.10;

    private EntityDamageRules() {
    }

    public static DamageResolution resolveAmbientDamage(
            long entityId,
            EntitySnapshot current,
            int damageAmount,
            DamageSource source,
            double nowSeconds,
            double nextDamageAllowedAt,
            double knockbackStrength,
            KnockbackOrigin knockbackOrigin
    ) {
        return resolveAmbientDamage(
                entityId,
                current,
                damageAmount,
                source,
                nowSeconds,
                nextDamageAllowedAt,
                DEFAULT_INVULNERABILITY_SECONDS,
                knockbackStrength,
                knockbackOrigin
        );
    }

    public static DamageResolution resolveAmbientDamage(
            long entityId,
            EntitySnapshot current,
            int damageAmount,
            DamageSource source,
            double nowSeconds,
            double nextDamageAllowedAt,
            double invulnerabilitySeconds,
            double knockbackStrength,
            KnockbackOrigin knockbackOrigin
    ) {
        Objects.requireNonNull(source, "source");
        if (damageAmount <= 0) {
            return DamageResolution.rejected(entityId, DamageResult.RejectionReason.INVALID_AMOUNT, nextDamageAllowedAt);
        }
        if (current == null) {
            return DamageResolution.rejected(entityId, DamageResult.RejectionReason.UNKNOWN_TARGET, nextDamageAllowedAt);
        }
        if (isPlayerOwnedDamage(source) && CreatureCombatRules.blocksPlayerDamage(current)) {
            return DamageResolution.rejected(entityId, DamageResult.RejectionReason.PROTECTED_CREATURE, nextDamageAllowedAt);
        }
        boolean enforceCooldown = Double.isFinite(nowSeconds);
        if (enforceCooldown && nowSeconds < nextDamageAllowedAt) {
            return DamageResolution.rejected(entityId, DamageResult.RejectionReason.INVULNERABLE, nextDamageAllowedAt);
        }

        int newHealth = Math.max(0, current.health() - damageAmount);
        int appliedDamage = current.health() - newHealth;
        Knockback knockback = knockbackFor(current, knockbackOrigin, knockbackStrength);
        EntitySnapshot updated = new EntitySnapshot(
                current.entityId(),
                current.typeKey(),
                current.ownerPlayerId(),
                current.x(),
                current.y(),
                current.z(),
                current.yaw(),
                current.pitch(),
                newHealth,
                newHealth <= 0 ? EntitySnapshot.STATE_IDLE : EntitySnapshot.STATE_FLEE,
                knockback.x(),
                knockback.y(),
                knockback.z()
        );
        double nextAllowed = enforceCooldown
                ? nowSeconds + Math.max(0.0, invulnerabilitySeconds)
                : nextDamageAllowedAt;
        return DamageResolution.accepted(
                DamageResult.accepted(entityId, appliedDamage, newHealth <= 0, updated, knockback.x(), knockback.y(), knockback.z()),
                nextAllowed
        );
    }

    public static KnockbackOrigin originFrom(EntitySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new KnockbackOrigin(snapshot.x(), snapshot.y(), snapshot.z());
    }

    private static Knockback knockbackFor(EntitySnapshot target, KnockbackOrigin origin, double strength) {
        if (origin == null || !Double.isFinite(strength) || strength <= 0.0) {
            return Knockback.NONE;
        }
        double dx = target.x() - origin.x();
        double dz = target.z() - origin.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= 0.0001) {
            return new Knockback(0.0, DEFAULT_VERTICAL_KNOCKBACK, 0.0);
        }
        return new Knockback(
                dx / distance * strength,
                DEFAULT_VERTICAL_KNOCKBACK,
                dz / distance * strength
        );
    }

    private static boolean isPlayerOwnedDamage(DamageSource source) {
        return source.type() == DamageSource.Type.PLAYER_MELEE
                || (source.type() == DamageSource.Type.PROJECTILE && source.attackerPlayerId() != null);
    }

    public record DamageResolution(DamageResult result, double nextDamageAllowedAt) {
        public DamageResolution {
            Objects.requireNonNull(result, "result");
            if (!Double.isFinite(nextDamageAllowedAt) && result.accepted()) {
                nextDamageAllowedAt = 0.0;
            }
        }

        public static DamageResolution accepted(DamageResult result, double nextDamageAllowedAt) {
            if (!result.accepted()) {
                throw new IllegalArgumentException("Accepted damage resolution requires an accepted damage result");
            }
            return new DamageResolution(result, nextDamageAllowedAt);
        }

        public static DamageResolution rejected(long entityId, DamageResult.RejectionReason reason, double nextDamageAllowedAt) {
            return new DamageResolution(DamageResult.rejected(entityId, reason), nextDamageAllowedAt);
        }

        public Optional<EntitySnapshot> snapshot() {
            return result.snapshot();
        }
    }

    public record KnockbackOrigin(double x, double y, double z) {
        public KnockbackOrigin {
            if (!PhysicsNumericGuard.allFinite(x, y, z)) {
                throw new IllegalArgumentException("Knockback origin must be finite");
            }
        }
    }

    private record Knockback(double x, double y, double z) {
        private static final Knockback NONE = new Knockback(0.0, 0.0, 0.0);
    }
}
