package dev.voxelgame.common.actions;

import dev.voxelgame.common.content.ContentTag;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ActionDefinitions {
    public static final String PROJECTILE_SHOOT = "voxel:projectile_shoot";
    public static final String BLOCK_INTERACT = "voxel:block_interact";
    public static final String EAT = "voxel:eat";

    private ActionDefinitions() {
    }

    public static List<ActionDefinition> defaults() {
        return List.of(
                projectileShoot(),
                blockInteract(),
                eat()
        );
    }

    public static Map<String, ActionDefinition> defaultMap() {
        return defaults().stream().collect(Collectors.toUnmodifiableMap(ActionDefinition::key, Function.identity()));
    }

    public static ActionDefinition projectileShoot() {
        return new ActionDefinition(
                PROJECTILE_SHOOT,
                ActionTargetType.DIRECTION,
                Set.of(ContentTag.RANGED),
                new ActionCost(1, 0, 0),
                new ActionCooldown(0.55),
                true,
                "projectile.shoot"
        );
    }

    public static ActionDefinition blockInteract() {
        return new ActionDefinition(
                BLOCK_INTERACT,
                ActionTargetType.BLOCK,
                Set.of(),
                ActionCost.NONE,
                ActionCooldown.NONE,
                true,
                "block.interact"
        );
    }

    public static ActionDefinition eat() {
        return new ActionDefinition(
                EAT,
                ActionTargetType.NONE,
                Set.of(ContentTag.FOOD),
                ActionCost.NONE,
                new ActionCooldown(0.25),
                true,
                "item.eat"
        );
    }
}
