package dev.voxelgame.client.world;

import dev.voxelgame.client.render.ChunkMesh;
import dev.voxelgame.common.entity.EntitySnapshot;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientWorldEntityTest {
    @Test
    void hidesOwnPlayerFromVisibleEntityList() {
        ClientWorld world = new ClientWorld(1L);
        UUID own = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        world.setOwnPlayerId(own);

        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:player", own, 0.0, 80.0, 0.0, 0.0f, 0.0f, 20),
                new EntitySnapshot(2L, "voxel:player", other, 4.0, 80.0, 4.0, 0.0f, 0.0f, 20)
        ));

        assertEquals(1, world.visibleEntities().size());
        assertEquals(other, world.visibleEntities().getFirst().ownerPlayerId());
    }

    @Test
    void interpolatesEntityPositionBetweenServerSnapshots() {
        ClientWorld world = new ClientWorld(1L);

        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 0.0, 80.0, 0.0, 350.0f, 0.0f, 10)
        ), 0.0);
        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 10.0, 82.0, -4.0, 10.0f, 0.0f, 8, EntitySnapshot.STATE_FOLLOW)
        ), 0.10);

        EntitySnapshot interpolated = world.visibleEntities(0.15).getFirst();

        assertEquals(5.0, interpolated.x(), 0.001);
        assertEquals(81.0, interpolated.y(), 0.001);
        assertEquals(-2.0, interpolated.z(), 0.001);
        assertEquals(0.0f, interpolated.yaw(), 0.001f);
        assertEquals(8, interpolated.health());
        assertEquals(EntitySnapshot.STATE_FOLLOW, interpolated.stateKey());
        assertEquals(100.0, interpolated.velocityX(), 0.001);
        assertEquals(20.0, interpolated.velocityY(), 0.001);
        assertEquals(-40.0, interpolated.velocityZ(), 0.001);
    }

    @Test
    void keepsOwnProjectileVisibleAndInterpolated() {
        ClientWorld world = new ClientWorld(1L);
        UUID own = UUID.randomUUID();
        world.setOwnPlayerId(own);

        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:player", own, 0.0, 80.0, 0.0, 0.0f, 0.0f, 20),
                new EntitySnapshot(-1L, "voxel:arrow_projectile", own, 0.0, 80.0, 0.0, 0.0f, 0.0f, 1, EntitySnapshot.STATE_PROJECTILE)
        ), 0.0);
        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:player", own, 0.0, 80.0, 0.0, 0.0f, 0.0f, 20),
                new EntitySnapshot(-1L, "voxel:arrow_projectile", own, 4.0, 80.0, 0.0, 0.0f, 0.0f, 1, EntitySnapshot.STATE_PROJECTILE)
        ), 0.10);

        List<EntitySnapshot> visible = world.visibleEntities(0.15);
        EntitySnapshot projectile = visible.getFirst();

        assertEquals(1, visible.size());
        assertEquals(-1L, projectile.entityId());
        assertEquals(2.0, projectile.x(), 0.001);
        assertEquals(EntitySnapshot.STATE_PROJECTILE, projectile.stateKey());
    }

    @Test
    void projectileSweepBoundsAreRadiusLimitedAndIgnoreNonProjectiles() {
        ClientWorld world = new ClientWorld(1L);

        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:arrow_projectile", null, 8.0, 80.0, 8.0, 0.0f, 0.0f, 1, EntitySnapshot.STATE_PROJECTILE)
                        .withVelocity(10.0, 0.0, -5.0),
                new EntitySnapshot(2L, "voxel:bunny", null, 9.0, 80.0, 8.0, 0.0f, 0.0f, 10, EntitySnapshot.STATE_WANDER),
                new EntitySnapshot(3L, "voxel:arrow_projectile", null, 80.0, 80.0, 80.0, 0.0f, 0.0f, 1, EntitySnapshot.STATE_PROJECTILE)
                        .withVelocity(10.0, 0.0, 0.0)
        ), 10.0);

        List<ChunkMesh.Bounds> bounds = world.projectileSweepBoundsAround(new org.joml.Vector3f(8.0f, 80.0f, 8.0f), 16, 10.0);

        assertEquals(1, bounds.size());
        ChunkMesh.Bounds sweep = bounds.getFirst();
        assertEquals(7.89f, sweep.minX(), 0.001f);
        assertEquals(10.11f, sweep.maxX(), 0.001f);
        assertEquals(6.89f, sweep.minZ(), 0.001f);
        assertEquals(8.11f, sweep.maxZ(), 0.001f);
    }

    @Test
    void interpolatesExplicitEntityVelocityForAnimationSignals() {
        ClientWorld world = new ClientWorld(1L);

        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10)
                        .withVelocity(0.2, 0.0, 0.0)
        ), 0.0);
        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 0.2, 80.0, 0.0, 0.0f, 0.0f, 10)
                        .withVelocity(1.0, 0.0, 0.4)
        ), 0.10);

        EntitySnapshot interpolated = world.visibleEntities(0.15).getFirst();

        assertEquals(0.6, interpolated.velocityX(), 0.001);
        assertEquals(0.2, interpolated.velocityZ(), 0.001);
    }

    @Test
    void clampsEntityInterpolationToLatestSnapshot() {
        ClientWorld world = new ClientWorld(1L);

        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10)
        ), 0.0);
        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 10.0, 80.0, 0.0, 90.0f, 0.0f, 10)
        ), 0.10);

        EntitySnapshot latest = world.visibleEntities(0.25).getFirst();

        assertEquals(10.0, latest.x(), 0.001);
        assertEquals(90.0f, latest.yaw(), 0.001f);
    }

    @Test
    void removesEntitiesMissingFromLatestServerSnapshot() {
        ClientWorld world = new ClientWorld(1L);

        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(1L, "voxel:bunny", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10),
                new EntitySnapshot(2L, "voxel:snail", null, 2.0, 80.0, 0.0, 0.0f, 0.0f, 10)
        ), 0.0);
        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(2L, "voxel:snail", null, 3.0, 80.0, 0.0, 0.0f, 0.0f, 10)
        ), 0.10);

        List<EntitySnapshot> visible = world.visibleEntities(0.25);

        assertEquals(1, visible.size());
        assertEquals(2L, visible.getFirst().entityId());
    }

    @Test
    void localEntityDamageAppliesKnockbackAndHealth() {
        ClientWorld world = new ClientWorld(1L);
        world.applyEntitySnapshots(List.of(
                new EntitySnapshot(9L, "voxel:moss_snail", null, 3.0, 80.0, 0.0, 0.0f, 0.0f, 6)
        ), 1.0);

        ClientWorld.LocalEntityDamageResult result = world.damageLocalEntity(
                9L,
                2,
                0.5,
                new Vector3f(0.0f, 80.0f, 0.0f),
                2.0
        );

        assertTrue(result.accepted());
        assertEquals(4, result.remainingHealth());
        EntitySnapshot updated = world.visibleEntities(2.3).getFirst();
        assertEquals(4, updated.health());
        assertTrue(updated.velocityX() > 0.0);
        assertEquals(EntitySnapshot.STATE_FLEE, updated.stateKey());
    }

    @Test
    void localItemDropsBecomePickupClaimsAfterDelay() {
        ClientWorld world = new ClientWorld(1L);

        world.spawnLocalItemDrop("voxel:moss_clump", 2, 4.0, 80.0, 4.0, 10.0);

        assertEquals(0, world.localItemDropsNear(new Vector3f(4.0f, 80.0f, 4.0f), 1.5, 10.1).size());
        List<ClientWorld.LocalItemPickup> ready = world.localItemDropsNear(new Vector3f(4.0f, 80.0f, 4.0f), 1.5, 10.5);
        assertEquals(1, ready.size());
        assertEquals("voxel:moss_clump", ready.getFirst().itemKey());
        assertEquals(2, ready.getFirst().count());
        assertTrue(world.claimLocalItemDrop(ready.getFirst().entityId()).isPresent());
        assertEquals(0, world.visibleEntities(10.6).size());
    }

    @Test
    void localItemDropsExpireAfterLifecycleBudget() {
        ClientWorld world = new ClientWorld(1L);

        world.spawnLocalItemDrop("voxel:moss_clump", 1, 4.0, 80.0, 4.0, 10.0);
        world.tickLocalEntities(new Vector3f(4.0f, 80.0f, 4.0f), 610.2);

        assertEquals(0, world.localItemDropsNear(new Vector3f(4.0f, 80.0f, 4.0f), 2.0, 610.2).size());
        assertEquals(0, world.visibleEntities(610.2).size());
    }
}
