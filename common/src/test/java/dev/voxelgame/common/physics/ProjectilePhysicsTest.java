package dev.voxelgame.common.physics;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.world.DimensionSettings;
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
        PhysicsTestWorld world = new PhysicsTestWorld().solid(2, 64, 0);

        ProjectileHit hit = ProjectilePhysics.step(
                arrow,
                PhysicsStepContext.projectile(0.1, 1L, DimensionSettings.OVERWORLD, "projectile-test"),
                ProjectilePhysicsConfig.arrow(),
                world.projectileCollision(),
                world.waterQuery(),
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
    void sweptProjectileUsesPartialShapeImpactQuery() {
        ProjectileState arrow = arrow(0.0, 64.5, 0.5, 30.0, 0.0, 0.0);

        ProjectileHit hit = ProjectilePhysics.step(
                arrow,
                0.12,
                noGravityConfig(0.18),
                (fromX, fromY, fromZ, toX, toY, toZ, bounds) -> BlockCollisionShapes.collisionShape(Blocks.GARDEN_FENCE)
                        .raycastProjectile(fromX, fromY, fromZ, toX, toY, toZ, bounds, 2, 64, 0),
                (ProjectilePhysics.WaterQuery) (x, y, z) -> false,
                List.of()
        );

        assertEquals(ProjectileHit.Type.BLOCK, hit.type());
        assertEquals(2, hit.blockX());
        assertEquals(ProjectileHit.BlockFace.WEST, hit.blockFace());
        assertEquals(2.36, hit.impactX(), 0.0001);
        assertTrue(hit.state().x() < hit.impactX());
    }

    @Test
    @Tag("physicsRegression")
    void nearerBlockImpactWinsOverFartherEntityHit() {
        ProjectileState arrow = arrow(0.0, 80.45, 0.5, 30.0, 0.0, 0.0);
        EntitySnapshot sheep = new EntitySnapshot(42L, "voxel:cozy_sheep", null, 2.3, 80.0, 0.5, 0.0f, 0.0f, 10);

        ProjectileHit hit = ProjectilePhysics.step(
                arrow,
                0.1,
                noGravityConfig(10.0),
                (fromX, fromY, fromZ, toX, toY, toZ, bounds) -> BlockCollisionShape.FULL
                        .raycastProjectile(fromX, fromY, fromZ, toX, toY, toZ, bounds, 1, 80, 0),
                (ProjectilePhysics.WaterQuery) (x, y, z) -> false,
                List.of(sheep)
        );

        assertEquals(ProjectileHit.Type.BLOCK, hit.type());
        assertEquals(1, hit.blockX());
        assertEquals(ProjectileHit.BlockFace.WEST, hit.blockFace());
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
                (ProjectilePhysics.WaterQuery) (x, y, z) -> false,
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
                (ProjectilePhysics.WaterQuery) (x, y, z) -> false,
                List.of(ownerSnapshot)
        );

        assertEquals(ProjectileHit.Type.MISS, hit.type());
    }

    @Test
    @Tag("physicsRegression")
    void projectileTargetsAreIgnoredForProjectileEntityHits() {
        ProjectileState arrow = new ProjectileState(7L, null, "voxel:arrow_projectile", 0.0, 80.45, 0.0, 36.0, 0.0, 0.0, 0);
        EntitySnapshot otherProjectile = new EntitySnapshot(8L, "voxel:arrow_projectile", null, 1.8, 80.45, 0.0, 0.0f, 0.0f, 1, EntitySnapshot.STATE_PROJECTILE);

        ProjectileHit hit = ProjectilePhysics.step(
                arrow,
                0.1,
                ProjectilePhysicsConfig.arrow(),
                (x, y, z, bounds) -> false,
                (ProjectilePhysics.WaterQuery) (x, y, z) -> false,
                List.of(otherProjectile)
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
        PhysicsTestWorld world = new PhysicsTestWorld().water(0, 64, 0);

        ProjectileHit hit = ProjectilePhysics.step(
                arrow,
                0.05,
                config,
                (x, y, z, bounds) -> false,
                world.waterQuery(),
                List.of()
        );

        assertEquals(ProjectileHit.Type.MISS, hit.type());
        assertTrue(hit.state().velocityX() < 10.0);
    }

    @Test
    @Tag("physicsRegression")
    void fluidCurrentCarriesProjectile() {
        ProjectilePhysicsConfig config = new ProjectilePhysicsConfig(
                "voxel:arrow_projectile",
                ProjectileBounds.ARROW,
                36.0,
                0.0,
                0.82,
                20,
                0.18,
                4
        );
        ProjectileState arrow = arrow(0.0, 64.5, 0.0, 0.0, 0.0, 0.0);

        ProjectileHit hit = ProjectilePhysics.step(
                arrow,
                0.05,
                config,
                (x, y, z, bounds) -> false,
                (FluidPhysics.FluidQuery) (x, y, z) -> FluidPhysics.water(0.8, 0.0, 0.0, 0.82, 5.0),
                List.of()
        );

        assertEquals(ProjectileHit.Type.MISS, hit.type());
        assertTrue(hit.state().velocityX() > 0.0);
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
                (ProjectilePhysics.WaterQuery) (x, y, z) -> false,
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
                (ProjectilePhysics.WaterQuery) (x, y, z) -> false,
                List.of()
        );

        assertEquals(ProjectileHit.Type.MISS, hit.type());
        assertTrue(hit.state().x() > 16.0);
    }

    private static ProjectileState arrow(double x, double y, double z, double vx, double vy, double vz) {
        return new ProjectileState(1L, null, "voxel:arrow_projectile", x, y, z, vx, vy, vz, 0);
    }

    private static ProjectilePhysicsConfig noGravityConfig(double maxStep) {
        return new ProjectilePhysicsConfig(
                "voxel:arrow_projectile",
                ProjectileBounds.ARROW,
                36.0,
                0.0,
                0.72,
                160,
                maxStep,
                4
        );
    }
}
