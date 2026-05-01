package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.entity.ItemDropType;

import java.util.Objects;
import java.util.UUID;

public final class ProjectileDamageRules {
    private ProjectileDamageRules() {
    }

    public static HitDecision canHit(ProjectileState projectile, EntitySnapshot target) {
        Objects.requireNonNull(projectile, "projectile");
        Objects.requireNonNull(target, "target");
        if (target.entityId() == projectile.projectileId()) {
            return HitDecision.rejected(Reason.SELF);
        }
        if (isOwnerPlayer(projectile.ownerPlayerId(), target)) {
            return HitDecision.rejected(Reason.OWNER);
        }
        if (isProjectile(target)) {
            return HitDecision.rejected(Reason.PROJECTILE);
        }
        if (ItemDropType.isTypeKey(target.typeKey())) {
            return HitDecision.rejected(Reason.ITEM_DROP);
        }
        if (target.health() <= 0) {
            return HitDecision.rejected(Reason.DEAD);
        }
        return HitDecision.allow();
    }

    public static boolean friendlyFireEnabled() {
        return false;
    }

    public static ProjectileObjectRule projectileObjectRule(String objectTypeKey) {
        if (objectTypeKey == null || objectTypeKey.isBlank()) {
            return ProjectileObjectRule.IGNORE;
        }
        if (objectTypeKey.endsWith("_projectile") || objectTypeKey.contains(":arrow_projectile")) {
            return ProjectileObjectRule.IGNORE;
        }
        if (objectTypeKey.contains("door") || objectTypeKey.contains("furniture") || objectTypeKey.contains("chair") || objectTypeKey.contains("table")) {
            return ProjectileObjectRule.BLOCK;
        }
        return ProjectileObjectRule.BLOCK;
    }

    private static boolean isOwnerPlayer(UUID ownerPlayerId, EntitySnapshot target) {
        return ownerPlayerId != null && ownerPlayerId.equals(target.ownerPlayerId());
    }

    private static boolean isProjectile(EntitySnapshot target) {
        return EntitySnapshot.STATE_PROJECTILE.equals(target.stateKey()) || target.typeKey().endsWith("_projectile");
    }

    public enum Reason {
        ACCEPTED,
        SELF,
        OWNER,
        PROJECTILE,
        ITEM_DROP,
        DEAD
    }

    public enum ProjectileObjectRule {
        IGNORE,
        BLOCK,
        DAMAGEABLE
    }

    public record HitDecision(boolean accepted, Reason reason) {
        public HitDecision {
            Objects.requireNonNull(reason, "reason");
            if (accepted && reason != Reason.ACCEPTED) {
                throw new IllegalArgumentException("Accepted projectile hit decisions must use ACCEPTED");
            }
        }

        public static HitDecision allow() {
            return new HitDecision(true, Reason.ACCEPTED);
        }

        public static HitDecision rejected(Reason reason) {
            return new HitDecision(false, reason);
        }
    }
}
