package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntitySnapshot;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
