package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.entity.ItemDropType;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.registry.Registry;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class MeleeAttackRules {
    private static final AttackProfile EMPTY_HAND = new AttackProfile(1, 0.48, 2.35, 0.25, 0);
    private static final AttackProfile HELD_ITEM = new AttackProfile(1, 0.52, 2.25, 0.20, 0);

    private MeleeAttackRules() {
    }

    public static AttackProfile profileFor(ItemStack selected, Registry<ItemType> items) {
        Objects.requireNonNull(selected, "selected");
        Objects.requireNonNull(items, "items");
        if (selected.isEmpty()) {
            return EMPTY_HAND;
        }
        Optional<ItemType> maybeItem = items.findById(selected.itemId());
        if (maybeItem.isEmpty()) {
            return HELD_ITEM;
        }
        ItemType item = maybeItem.get();
        Optional<WeaponItemRules.WeaponStats> weapon = WeaponItemRules.statsFor(item);
        if (weapon.isPresent()) {
            WeaponItemRules.WeaponStats stats = weapon.get();
            double range = stats.fast() ? 2.65 : stats.crystalline() ? 3.35 : 3.05;
            return new AttackProfile(
                    stats.damage(),
                    stats.cooldownSeconds(),
                    range,
                    stats.knockback(),
                    1
            );
        }
        if (item.isTool()) {
            int level = Math.max(0, item.toolLevel());
            return new AttackProfile(
                    2 + level,
                    0.72 + level * 0.03,
                    2.70,
                    0.34 + level * 0.11,
                    1
            );
        }
        return HELD_ITEM;
    }

    public static TargetDecision canAttack(UUID attackerPlayerId, EntitySnapshot target) {
        Objects.requireNonNull(target, "target");
        if (target.health() <= 0) {
            return TargetDecision.rejected(RejectionReason.DEAD);
        }
        if (EntitySnapshot.STATE_PROJECTILE.equals(target.stateKey()) || target.typeKey().endsWith("_projectile")) {
            return TargetDecision.rejected(RejectionReason.PROJECTILE);
        }
        if (ItemDropType.isTypeKey(target.typeKey())) {
            return TargetDecision.rejected(RejectionReason.ITEM_DROP);
        }
        if (attackerPlayerId != null && attackerPlayerId.equals(target.ownerPlayerId())) {
            return TargetDecision.rejected(RejectionReason.OWNED_OR_SELF);
        }
        return TargetDecision.allow();
    }

    public enum RejectionReason {
        ACCEPTED,
        DEAD,
        PROJECTILE,
        ITEM_DROP,
        OWNED_OR_SELF
    }

    public record AttackProfile(
            int damage,
            double cooldownSeconds,
            double range,
            double knockbackStrength,
            int durabilityDamage
    ) {
        public AttackProfile {
            if (damage <= 0) {
                throw new IllegalArgumentException("Melee damage must be > 0");
            }
            if (!Double.isFinite(cooldownSeconds) || cooldownSeconds <= 0.0) {
                throw new IllegalArgumentException("Melee cooldown must be finite and > 0");
            }
            if (!Double.isFinite(range) || range <= 0.0) {
                throw new IllegalArgumentException("Melee range must be finite and > 0");
            }
            if (!Double.isFinite(knockbackStrength) || knockbackStrength < 0.0) {
                throw new IllegalArgumentException("Melee knockback must be finite and >= 0");
            }
            if (durabilityDamage < 0) {
                throw new IllegalArgumentException("Melee durability damage cannot be negative");
            }
        }
    }

    public record TargetDecision(boolean accepted, RejectionReason reason) {
        public TargetDecision {
            Objects.requireNonNull(reason, "reason");
            if (accepted && reason != RejectionReason.ACCEPTED) {
                throw new IllegalArgumentException("Accepted melee target decisions must use ACCEPTED");
            }
        }

        public static TargetDecision allow() {
            return new TargetDecision(true, RejectionReason.ACCEPTED);
        }

        public static TargetDecision rejected(RejectionReason reason) {
            return new TargetDecision(false, reason);
        }
    }
}
