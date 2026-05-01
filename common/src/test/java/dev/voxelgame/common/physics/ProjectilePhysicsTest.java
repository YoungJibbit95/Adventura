package dev.voxelgame.common.physics;

import dev.voxelgame.common.entity.EntitySnapshot;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectilePhysicsTest {
    @Test
    @Tag("physicsRegression")
    void sweptProjectileHitsBlockWithoutTunneling() {
        ProjectileState arrow = arrow(0.0, 64.5, 0.5, 42.0, 0.0, 0.0);

        ProjectileHit hit = ProjectilePhysics.step(
                arrow,
                0.1,
                ProjectilePhysicsConfig.arrow(),
                (x, y, z, bounds) -> bounds.intersectsBlock(x, y, z, 2, 64, 0),
                (x, y, z) -> false,
                List.of()
        );

        assertEquals(ProjectileHit.Type.BLOCK, hit.type());
        assertEquals(2, hit.blockX());
        assertEquals(ProjectileHit.BlockFace.WEST, hit.blockFace());
        assertEquals(-1, hit.blockFace().normalX());
        assertEquals(0, hit.blockFace().normalY());
        assertEquals(0, hit.blockFace().normalZ());
        assertTrue(hit.impactX() > 0.0);
        assertTrue(hit.terminal());
    }

    @Test
    @Tag("physicsRegression")
    void sweptProjectileHitsEntityBetweenSamples() {
        ProjectileState arrow = arrow(0.0, 80.45, 0.0, 36.0, 0.0, 0.0);
        EntitySnapshot sheep = new EntitySnapshot(42L, "voxel:cozy_sheep", null, 1.8, 80.0, 0.0, 0.0f, 0.0f, 10);

        ProjectileHit hit = ProjectilePhysics.step(
                arrow,
                0.1,
                ProjectilePhysicsConfig.arrow(),
                (x, y, z, bounds) -> false,
                (x, y, z) -> false,
                List.of(sheep)
        );

        assertEquals(ProjectileHit.Type.ENTITY, hit.type());
        assertEquals(42L, hit.entityId());
    }

    @Test
    @Tag("physicsRegression")
    void ownerPlayerIsIgnoredForProjectileEntityHits() {
        UUID owner = UUID.randomUUID();
        ProjectileState arrow = new ProjectileState(7L, owner, "voxel:arrow_projectile", 0.0, 80.45, 0.0, 36.0, 0.0, 0.0, 0);
        EntitySnapshot ownerSnapshot = new EntitySnapshot(1L, "voxel:player", owner, 1.8, 82.0, 0.0, 0.0f, 0.0f, 20);

        ProjectileHit hit = ProjectilePhysics.step(
                arrow,
                0.1,
                ProjectilePhysicsConfig.arrow(),
                (x, y, z, bounds) -> false,
                (x, y, z) -> false,
                List.of(ownerSnapshot)
        );

        assertEquals(ProjectileHit.Type.MISS, hit.type());
    }

    @Test
    @Tag("physicsRegression")
    void waterDragSlowsProjectileVelocity() {
        ProjectilePhysicsConfig config = new ProjectilePhysicsConfig(
                "voxel:arrow_projectile",
                ProjectileBounds.ARROW,
                36.0,
                0.0,
                0.5,
                20,
                0.18,
                4
        );
        ProjectileState arrow = arrow(0.0, 64.5, 0.0, 10.0, 0.0, 0.0);

        ProjectileHit hit = ProjectilePhysics.step(
                arrow,
                0.05,
                config,
                (x, y, z, bounds) -> false,
                (x, y, z) -> true,
                List.of()
        );

        assertEquals(ProjectileHit.Type.MISS, hit.type());
        assertTrue(hit.state().velocityX() < 10.0);
    }

    @Test
    @Tag("physicsRegression")
    void projectileExpiresAtLifetime() {
        ProjectilePhysicsConfig config = new ProjectilePhysicsConfig(
                "voxel:arrow_projectile",
                ProjectileBounds.ARROW,
                36.0,
                0.0,
                1.0,
                1,
                0.18,
                4
        );
        ProjectileState arrow = arrow(0.0, 64.5, 0.0, 1.0, 0.0, 0.0);

        ProjectileHit hit = ProjectilePhysics.step(
                arrow,
                0.05,
                config,
                (x, y, z, bounds) -> false,
                (x, y, z) -> false,
                List.of()
        );

        assertEquals(ProjectileHit.Type.EXPIRED, hit.type());
    }

    @Test
    @Tag("physicsRegression")
    void projectileCrossesChunkBoundaryWithoutGhostHit() {
        ProjectileState arrow = arrow(15.75, 64.5, 0.5, 12.0, 0.0, 0.0);

        ProjectileHit hit = ProjectilePhysics.step(
                arrow,
                0.05,
                ProjectilePhysicsConfig.arrow(),
                (x, y, z, bounds) -> bounds.intersectsBlock(x, y, z, 18, 64, 0),
                (x, y, z) -> false,
                List.of()
        );

        assertEquals(ProjectileHit.Type.MISS, hit.type());
        assertTrue(hit.state().x() > 16.0);
    }

    private static ProjectileState arrow(double x, double y, double z, double vx, double vy, double vz) {
        return new ProjectileState(1L, null, "voxel:arrow_projectile", x, y, z, vx, vy, vz, 0);
    }
}
