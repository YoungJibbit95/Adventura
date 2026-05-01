package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.ToolType;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.physics.ProjectilePhysicsConfig;
import dev.voxelgame.common.registry.Registry;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class ProjectileItemRules {
    private static final double DEFAULT_COOLDOWN_SECONDS = 0.55;
    private static final double BOW_COOLDOWN_SECONDS = 0.42;
    private static final double FAST_THROW_COOLDOWN_SECONDS = 0.36;

    private ProjectileItemRules() {
    }

    public static boolean canLaunch(ItemStack stack, Registry<ItemType> items) {
        return projectileTypeKey(stack, items).isPresent();
    }

    public static Optional<String> projectileTypeKey(ItemStack stack, Registry<ItemType> items) {
        Objects.requireNonNull(items, "items");
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return items.findById(stack.itemId())
                .filter(ProjectileItemRules::canLaunch)
                .map(ignored -> ProjectilePhysicsConfig.arrow().typeKey());
    }

    public static boolean canLaunch(ItemType item) {
        return item != null && canLaunchKey(item.key(), item.toolType());
    }

    public static boolean canLaunchKey(String itemKey) {
        return canLaunchKey(itemKey, ToolType.NONE);
    }

    public static int durabilityDamageOnLaunch(ItemStack stack, Registry<ItemType> items) {
        Objects.requireNonNull(items, "items");
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return items.findById(stack.itemId())
                .filter(item -> item.durability() > 0)
                .filter(ProjectileItemRules::canLaunch)
                .map(ignored -> 1)
                .orElse(0);
    }

    public static double cooldownSeconds(ItemStack stack, Registry<ItemType> items) {
        Objects.requireNonNull(items, "items");
        if (stack == null || stack.isEmpty()) {
            return DEFAULT_COOLDOWN_SECONDS;
        }
        return items.findById(stack.itemId())
                .map(ProjectileItemRules::cooldownSeconds)
                .orElse(DEFAULT_COOLDOWN_SECONDS);
    }

    public static double cooldownSeconds(ItemType item) {
        if (item == null) {
            return DEFAULT_COOLDOWN_SECONDS;
        }
        String key = item.key().toLowerCase(Locale.ROOT);
        if (key.contains("bow")) {
            return BOW_COOLDOWN_SECONDS;
        }
        if (key.contains("crystal") && item.toolType() == ToolType.KNIFE) {
            return FAST_THROW_COOLDOWN_SECONDS;
        }
        return DEFAULT_COOLDOWN_SECONDS;
    }

    private static boolean canLaunchKey(String itemKey, ToolType toolType) {
        if (itemKey == null || itemKey.isBlank()) {
            return false;
        }
        String key = itemKey.toLowerCase(Locale.ROOT);
        return key.contains("bow") || toolType == ToolType.KNIFE || key.endsWith("_knife");
    }
}
