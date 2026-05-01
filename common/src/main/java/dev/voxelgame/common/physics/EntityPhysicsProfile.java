package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.ItemDropType;

public record EntityPhysicsProfile(
        MovementClass movementClass,
        WaterBehavior waterBehavior,
        double separationPadding,
        double separationStep,
        double recoveryStep,
        double knockbackFriction,
        double knockbackGravity,
        double maxFallSpeed
) {
    public static EntityPhysicsProfile forType(String typeKey) {
        if (ItemDropType.isTypeKey(typeKey)) {
            return new EntityPhysicsProfile(MovementClass.TINY, WaterBehavior.FLOAT, 0.05, 0.10, 0.08, 0.72, 0.05, 0.26);
        }
        return switch (typeKey) {
            case "voxel:firefly_swarm", "voxel:mire_wisp" ->
                    new EntityPhysicsProfile(MovementClass.FLYER, WaterBehavior.IGNORE, 0.06, 0.12, 0.12, 0.86, 0.0, 0.0);
            case "voxel:forest_bunny", "voxel:snow_hare" ->
                    new EntityPhysicsProfile(MovementClass.TINY, WaterBehavior.AVOID, 0.07, 0.16, 0.12, 0.82, 0.04, 0.24);
            case "voxel:moss_snail" ->
                    new EntityPhysicsProfile(MovementClass.HEAVY, WaterBehavior.AVOID, 0.06, 0.06, 0.06, 0.55, 0.02, 0.12);
            case "voxel:little_boar", "voxel:dune_crawler" ->
                    new EntityPhysicsProfile(MovementClass.HEAVY, WaterBehavior.AVOID, 0.09, 0.11, 0.10, 0.68, 0.05, 0.22);
            case "voxel:cozy_sheep", "voxel:forest_grazer", "voxel:meadow_grazer" ->
                    new EntityPhysicsProfile(MovementClass.GROUND, WaterBehavior.AVOID, 0.08, 0.13, 0.10, 0.74, 0.04, 0.20);
            default ->
                    new EntityPhysicsProfile(MovementClass.GROUND, WaterBehavior.AVOID, 0.08, 0.12, 0.10, 0.72, 0.04, 0.20);
        };
    }

    public EntityPhysicsProfile {
        movementClass = movementClass == null ? MovementClass.GROUND : movementClass;
        waterBehavior = waterBehavior == null ? WaterBehavior.AVOID : waterBehavior;
        separationPadding = PhysicsNumericGuard.requireFiniteNonNegative("Entity separation padding", separationPadding);
        separationStep = PhysicsNumericGuard.requireFiniteNonNegative("Entity separation step", separationStep);
        recoveryStep = PhysicsNumericGuard.requireFiniteNonNegative("Entity recovery step", recoveryStep);
        knockbackFriction = PhysicsNumericGuard.requireFiniteNonNegative("Entity knockback friction", knockbackFriction);
        knockbackGravity = PhysicsNumericGuard.requireFiniteNonNegative("Entity knockback gravity", knockbackGravity);
        maxFallSpeed = PhysicsNumericGuard.requireFiniteNonNegative("Entity max fall speed", maxFallSpeed);
    }

    public boolean needsGroundSupport() {
        return movementClass != MovementClass.FLYER && movementClass != MovementClass.SWIMMER;
    }

    public boolean ignoresTerrainSupport() {
        return movementClass == MovementClass.FLYER || movementClass == MovementClass.SWIMMER;
    }

    public boolean blocksWaterPlacement() {
        return waterBehavior == WaterBehavior.AVOID;
    }

    public boolean acceptsWaterPlacement() {
        return waterBehavior == WaterBehavior.SWIM || waterBehavior == WaterBehavior.FLOAT || waterBehavior == WaterBehavior.IGNORE;
    }

    public enum MovementClass {
        GROUND,
        FLYER,
        SWIMMER,
        TINY,
        HEAVY
    }

    public enum WaterBehavior {
        AVOID,
        SWIM,
        FLOAT,
        IGNORE
    }
}
