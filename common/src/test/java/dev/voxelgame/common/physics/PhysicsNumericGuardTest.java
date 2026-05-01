package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.world.DimensionSettings;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("physicsRegression")
class PhysicsNumericGuardTest {
    @Test
    void rejectsNonFinitePlayerEntityProjectileAndHitState() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerState(Double.NaN, 64.0, 8.0, 0.0f, 0.0f, 0.0f, false, false, 0.0f));
        assertThrows(IllegalArgumentException.class, () -> new EntitySnapshot(1L, "voxel:cozy_sheep", null, 8.0, Double.POSITIVE_INFINITY, 8.0, 0.0f, 0.0f, 10));
        assertThrows(IllegalArgumentException.class, () -> new ProjectileState(1L, null, "voxel:arrow_projectile", 8.0, 64.0, 8.0, Double.NaN, 0.0, 0.0, 0));
        ProjectileState finite = new ProjectileState(1L, null, "voxel:arrow_projectile", 8.0, 64.0, 8.0, 1.0, 0.0, 0.0, 0);
        assertThrows(IllegalArgumentException.class, () -> ProjectileHit.block(finite, 8, 64, 8, Double.NaN, 64.0, 8.0, ProjectileHit.BlockFace.WEST));
    }

    @Test
    void centralGuardCoversStepContextBoundsAndConfigSnapshots() {
        assertThrows(IllegalArgumentException.class, () -> PhysicsNumericGuard.requireFiniteBounds(0.0, 0.0, 0.0, Double.NaN, 1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> PhysicsStepContext.survival(Double.POSITIVE_INFINITY, 0L, DimensionSettings.OVERWORLD, null));
        assertEquals(0.05f, PhysicsStepContext.survival(0.05, 7L, DimensionSettings.OVERWORLD, null).floatDeltaSeconds(), 0.001f);

        PhysicsConfigSnapshot snapshot = PhysicsConfigSnapshot.current();
        assertTrue(snapshot.compatibleWith(PhysicsConfigSnapshot.of(PlayerPhysicsConfig.defaults(), ProjectilePhysicsConfig.arrow())));
        assertTrue(snapshot.wireId().startsWith("physics-v"));
        assertEquals(PlayerPhysicsConfig.defaults().fingerprint(), snapshot.playerFingerprint());
        assertEquals(ProjectilePhysicsConfig.arrow().fingerprint(), snapshot.projectileFingerprint());
    }
}
