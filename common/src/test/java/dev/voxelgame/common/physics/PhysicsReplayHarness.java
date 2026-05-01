package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.world.DimensionSettings;

import java.util.List;
import java.util.Properties;

final class PhysicsReplayHarness {
    private PhysicsReplayHarness() {
    }

    static PlayerState replayPlayer(Properties replay, PhysicsTestWorld world) {
        PlayerPhysicsConfig config = PlayerPhysicsConfig.defaults();
        PlayerState state = new PlayerState(
                doubleValue(replay, "startX"),
                doubleValue(replay, "startY"),
                doubleValue(replay, "startZ"),
                floatValue(replay, "velocityX", 0.0f),
                floatValue(replay, "velocityY", 0.0f),
                floatValue(replay, "velocityZ", 0.0f),
                booleanValue(replay, "onGround", false),
                false,
                0.0f
        );
        int steps = intValue(replay, "steps");
        double delta = doubleValue(replay, "delta");
        int jumpStep = intValue(replay, "jumpStep", -1);
        float moveX = floatValue(replay, "moveX", 0.0f);
        float moveZ = floatValue(replay, "moveZ", 0.0f);
        for (int step = 0; step < steps; step++) {
            PhysicsStepContext context = PhysicsStepContext.survival(
                    delta,
                    step,
                    DimensionSettings.OVERWORLD,
                    new PlayerWaterState(false, false, false)
            ).withDebug("player-replay", step);
            PlayerInput input = new PlayerInput(moveX, moveZ, step == jumpStep, false, false);
            state = PlayerPhysics.stepSurvival(state, input, context, config, world.playerCollision(config));
        }
        return state;
    }

    static ProjectileHit replayProjectile(Properties replay, PhysicsTestWorld world) {
        ProjectileState state = new ProjectileState(
                1L,
                null,
                "voxel:arrow_projectile",
                doubleValue(replay, "x"),
                doubleValue(replay, "y"),
                doubleValue(replay, "z"),
                doubleValue(replay, "vx"),
                doubleValue(replay, "vy"),
                doubleValue(replay, "vz"),
                0
        );
        ProjectileHit hit = ProjectileHit.miss(state);
        int steps = intValue(replay, "steps", 1);
        double delta = doubleValue(replay, "delta");
        for (int step = 0; step < steps; step++) {
            PhysicsStepContext context = PhysicsStepContext.projectile(delta, step, DimensionSettings.OVERWORLD, "projectile-replay")
                    .withDebug("projectile-replay", step);
            hit = ProjectilePhysics.step(hit.state(), context, ProjectilePhysicsConfig.arrow(), world.projectileCollision(), world.waterQuery(), List.of());
            if (hit.terminal()) {
                return hit;
            }
        }
        return hit;
    }

    static EntitySnapshot replayEntity(Properties replay) {
        EntitySnapshot state = new EntitySnapshot(
                longValue(replay, "entityId", 1L),
                replay.getProperty("typeKey", "voxel:cozy_sheep"),
                null,
                doubleValue(replay, "x"),
                doubleValue(replay, "y"),
                doubleValue(replay, "z"),
                0.0f,
                0.0f,
                intValue(replay, "health", 10),
                EntitySnapshot.STATE_WANDER,
                doubleValue(replay, "vx", 0.0),
                doubleValue(replay, "vy", 0.0),
                doubleValue(replay, "vz", 0.0)
        );
        EntitySnapshot neighbor = new EntitySnapshot(
                longValue(replay, "neighborId", 2L),
                replay.getProperty("neighborTypeKey", "voxel:cozy_sheep"),
                null,
                doubleValue(replay, "neighborX"),
                doubleValue(replay, "neighborY"),
                doubleValue(replay, "neighborZ"),
                0.0f,
                0.0f,
                10
        );
        int steps = intValue(replay, "steps", 1);
        double stepX = doubleValue(replay, "stepX", 0.0);
        double stepZ = doubleValue(replay, "stepZ", 0.0);
        for (int step = 0; step < steps; step++) {
            EntitySnapshot candidate = EntityPhysics.withPosition(state, state.x() + stepX, state.y(), state.z() + stepZ);
            candidate = EntityPhysics.applyImpulseMotion(state, candidate, EntityPhysicsProfile.forType(state.typeKey()));
            candidate = EntityPhysics.applySeparation(state, candidate, List.of(neighbor));
            state = EntityPhysics.sweepWithSlide(
                    state,
                    candidate,
                    (from, next) -> !EntityPhysics.overlaps(next, neighbor, EntityPhysicsProfile.forType(next.typeKey()).separationPadding())
            ).snapshot();
        }
        return state;
    }

    static void addSolidBlocks(Properties replay, PhysicsTestWorld world) {
        String blocks = replay.getProperty("solidBlocks", "").trim();
        if (blocks.isBlank()) {
            return;
        }
        for (String block : blocks.split(";")) {
            String[] coordinates = block.trim().split(",");
            if (coordinates.length != 3) {
                throw new IllegalArgumentException("Invalid replay solid block: " + block);
            }
            world.solid(
                    Integer.parseInt(coordinates[0].trim()),
                    Integer.parseInt(coordinates[1].trim()),
                    Integer.parseInt(coordinates[2].trim())
            );
        }
    }

    private static double doubleValue(Properties properties, String key) {
        return Double.parseDouble(properties.getProperty(key));
    }

    private static double doubleValue(Properties properties, String key, double fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Double.parseDouble(value);
    }

    private static float floatValue(Properties properties, String key, float fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Float.parseFloat(value);
    }

    private static int intValue(Properties properties, String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    private static int intValue(Properties properties, String key, int fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private static long longValue(Properties properties, String key, long fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Long.parseLong(value);
    }

    private static boolean booleanValue(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }
}
