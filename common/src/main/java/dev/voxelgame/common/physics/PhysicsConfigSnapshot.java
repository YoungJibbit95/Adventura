package dev.voxelgame.common.physics;

import java.util.Locale;
import java.util.Objects;

public record PhysicsConfigSnapshot(
        int schemaVersion,
        String playerFingerprint,
        String projectileFingerprint
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static PhysicsConfigSnapshot current() {
        return of(PlayerPhysicsConfig.defaults(), ProjectilePhysicsConfig.arrow());
    }

    public static PhysicsConfigSnapshot of(PlayerPhysicsConfig playerConfig, ProjectilePhysicsConfig projectileConfig) {
        return new PhysicsConfigSnapshot(
                CURRENT_SCHEMA_VERSION,
                playerFingerprint(playerConfig),
                projectileFingerprint(projectileConfig)
        );
    }

    public PhysicsConfigSnapshot {
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("Physics config schema version must be positive");
        }
        playerFingerprint = requireFingerprint("playerFingerprint", playerFingerprint);
        projectileFingerprint = requireFingerprint("projectileFingerprint", projectileFingerprint);
    }

    public String wireId() {
        return "physics-v" + schemaVersion + ":" + playerFingerprint + ":" + projectileFingerprint;
    }

    public boolean compatibleWith(PhysicsConfigSnapshot other) {
        return equals(other);
    }

    static String playerFingerprint(PlayerPhysicsConfig config) {
        Objects.requireNonNull(config, "config");
        PlayerBounds bounds = config.bounds();
        return hex(Objects.hash(
                CURRENT_SCHEMA_VERSION,
                bounds.halfWidth(),
                bounds.eyeHeight(),
                bounds.headClearance(),
                config.gravity(),
                config.jumpSpeed(),
                config.walkSpeed(),
                config.sprintSpeed(),
                config.flySpeed(),
                config.flySprintSpeed(),
                config.waterSpeedMultiplier(),
                config.waterHorizontalDrag(),
                config.waterGravityMultiplier(),
                config.waterVerticalDrag(),
                config.swimRiseSpeed(),
                config.maxFallSpeed(),
                config.maxWaterFallSpeed(),
                config.groundProbeDistance(),
                config.maxCollisionStep()
        ));
    }

    static String projectileFingerprint(ProjectilePhysicsConfig config) {
        Objects.requireNonNull(config, "config");
        return hex(Objects.hash(
                CURRENT_SCHEMA_VERSION,
                config.typeKey(),
                config.bounds().radius(),
                config.initialSpeed(),
                config.gravity(),
                config.waterDrag(),
                config.maxLifetimeTicks(),
                config.maxStep(),
                config.damage()
        ));
    }

    private static String requireFingerprint(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static String hex(int value) {
        return String.format(Locale.ROOT, "%08x", value);
    }
}
