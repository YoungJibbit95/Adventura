package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.item.ItemType;

import java.util.Objects;
import java.util.Optional;

public final class WeaponItemRules {
    private WeaponItemRules() {
    }

    public static boolean isWeapon(ItemType item) {
        return statsFor(item).isPresent();
    }

    public static Optional<WeaponStats> statsFor(ItemType item) {
        Objects.requireNonNull(item, "item");
        String key = item.key();
        if (key.endsWith("_knife")) {
            return Optional.of(new WeaponStats(3, 0.42f, 0.18f, true, false));
        }
        if (!key.contains("sword")) {
            return Optional.empty();
        }
        if (key.contains("titan")) {
            return Optional.of(new WeaponStats(10, 0.72f, 0.54f, false, false));
        }
        if (key.contains("platin") || key.contains("platinum")) {
            return Optional.of(new WeaponStats(8, 0.58f, 0.38f, false, false));
        }
        if (key.contains("sapphire")) {
            return Optional.of(new WeaponStats(7, 0.52f, 0.28f, false, true));
        }
        if (key.contains("iron")) {
            return Optional.of(new WeaponStats(7, 0.62f, 0.34f, false, false));
        }
        return Optional.of(new WeaponStats(5, 0.66f, 0.26f, false, false));
    }

    public record WeaponStats(
            int damage,
            float cooldownSeconds,
            float knockback,
            boolean fast,
            boolean crystalline
    ) {
        public WeaponStats {
            if (damage <= 0) {
                throw new IllegalArgumentException("Weapon damage must be > 0");
            }
            if (!Float.isFinite(cooldownSeconds) || cooldownSeconds <= 0.0f) {
                throw new IllegalArgumentException("Weapon cooldown must be finite and > 0");
            }
            if (!Float.isFinite(knockback) || knockback < 0.0f) {
                throw new IllegalArgumentException("Weapon knockback must be finite and >= 0");
            }
        }
    }
}
