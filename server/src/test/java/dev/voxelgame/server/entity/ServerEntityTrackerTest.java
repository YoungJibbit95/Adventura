package dev.voxelgame.server.entity;

import dev.voxelgame.common.entity.DamageResult;
import dev.voxelgame.common.entity.DamageSource;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.entity.ItemDropType;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.physics.EntityPhysics;
import dev.voxelgame.common.physics.FluidPhysics;
import dev.voxelgame.common.physics.ProjectileHit;
import dev.voxelgame.common.physics.ProjectileState;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerEntityTrackerTest {
    @Test
    void tracksPlayerSnapshotsByPlayerId() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        UUID playerId = UUID.randomUUID();

        tracker.registerPlayer(playerId);
        EntitySnapshot moved = tracker.updatePlayer(playerId, 10.0, 80.0, -4.0, 45.0f, 5.0f);

        assertEquals(1, tracker.playerCount());
        assertEquals(playerId, moved.ownerPlayerId());
        assertEquals(10.0, tracker.snapshots().getFirst().x());
        assertEquals(-4.0, tracker.snapshots().getFirst().z());

        tracker.removePlayer(playerId);
        assertEquals(0, tracker.playerCount());
    }

    @Test
    void findsSnapshotsByEntityId() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        UUID playerId = UUID.randomUUID();
        EntitySnapshot registered = tracker.registerPlayer(playerId);

        assertTrue(tracker.snapshot(registered.entityId()).isPresent());
        assertEquals(playerId, tracker.snapshot(registered.entityId()).orElseThrow().ownerPlayerId());
        assertTrue(tracker.snapshot(999_999L).isEmpty());
    }

    @Test
    void feedingAmbientEntityUpdatesSnapshotHealth() {
        ServerEntityTracker tracker = new ServerEntityTracker(123L);
        EntitySnapshot ambient = tracker.snapshots().stream()
                .filter(snapshot -> snapshot.ownerPlayerId() == null)
                .findFirst()
                .orElseThrow();

        EntitySnapshot fed = tracker.feedAmbient(ambient.entityId(), 2).orElseThrow();

        assertEquals(Math.min(20, ambient.health() + 2), fed.health());
        assertEquals(fed.health(), tracker.snapshot(ambient.entityId()).orElseThrow().health());
    }

    @Test
    void fedAmbientEntityFollowsFeedingPlayer() {
        ServerEntityTracker tracker = new ServerEntityTracker(123L);
        UUID playerId = UUID.randomUUID();
        tracker.registerPlayer(playerId);
        tracker.updatePlayer(playerId, 8.0, 120.0, 8.0, 0.0f, 0.0f);
        EntitySnapshot ambient = tracker.snapshots().stream()
                .filter(snapshot -> snapshot.ownerPlayerId() == null)
                .findFirst()
                .orElseThrow();

        EntitySnapshot fed = tracker.feedAmbient(ambient.entityId(), 2, playerId).orElseThrow();
        tracker.updatePlayer(playerId, 80.0, 120.0, 80.0, 0.0f, 0.0f);
        tracker.tickAmbient(80L);
        EntitySnapshot moved = tracker.snapshot(ambient.entityId()).orElseThrow();

        assertEquals(EntitySnapshot.STATE_FOLLOW, fed.stateKey());
        assertEquals(EntitySnapshot.STATE_FOLLOW, moved.stateKey());
        assertTrue(distanceSquared(moved.x(), moved.z(), 80.0, 80.0) < distanceSquared(ambient.x(), ambient.z(), 80.0, 80.0));
    }

    @Test
    void timidAmbientEntityFleesNearbyPlayer() {
        ServerEntityTracker tracker = new ServerEntityTracker(123L);
        UUID playerId = UUID.randomUUID();
        tracker.registerPlayer(playerId);
        EntitySnapshot bunny = tracker.snapshots().stream()
                .filter(snapshot -> "voxel:forest_bunny".equals(snapshot.typeKey()))
                .findFirst()
                .orElseThrow();
        tracker.updatePlayer(playerId, bunny.x() + 0.4, bunny.y(), bunny.z(), 0.0f, 0.0f);
        double beforeDistance = distanceSquared(bunny.x(), bunny.z(), bunny.x() + 0.4, bunny.z());

        tracker.tickAmbient(20L);
        EntitySnapshot moved = tracker.snapshot(bunny.entityId()).orElseThrow();

        assertEquals(EntitySnapshot.STATE_FLEE, moved.stateKey());
        assertTrue(distanceSquared(moved.x(), moved.z(), bunny.x() + 0.4, bunny.z()) > beforeDistance);
    }

    @Test
    void reportsDangerousAmbientEntitiesNearSleepSite() {
        ServerEntityTracker tracker = new ServerEntityTracker(123L);
        EntitySnapshot boar = tracker.snapshots().stream()
                .filter(snapshot -> "voxel:little_boar".equals(snapshot.typeKey()))
                .findFirst()
                .orElseThrow();

        assertTrue(tracker.hasDangerNear(boar.x(), boar.y(), boar.z(), ServerEntityTracker.SLEEP_DANGER_RADIUS));
        assertFalse(tracker.hasDangerNear(boar.x() + 100.0, boar.y(), boar.z() + 100.0, ServerEntityTracker.SLEEP_DANGER_RADIUS));
    }

    @Test
    void ticksAmbientEntitiesWithStatefulMovement() {
        ServerEntityTracker tracker = new ServerEntityTracker(123L);
        EntitySnapshot ambient = tracker.snapshots().stream()
                .filter(snapshot -> snapshot.ownerPlayerId() == null)
                .findFirst()
                .orElseThrow();

        List<EntitySnapshot> updates = tracker.tickAmbient(40L);
        EntitySnapshot moved = tracker.snapshot(ambient.entityId()).orElseThrow();

        assertEquals(updates.size(), tracker.snapshots().stream().filter(snapshot -> snapshot.ownerPlayerId() == null).count());
        assertNotEquals(EntitySnapshot.STATE_IDLE, moved.stateKey());
        assertTrue(moved.x() != ambient.x() || moved.z() != ambient.z());
    }

    @Test
    void spawnValidatorFiltersInvalidAmbientEntities() {
        ServerEntityTracker tracker = new ServerEntityTracker(123L, snapshot -> false);

        assertEquals(0, tracker.snapshots().stream().filter(snapshot -> snapshot.ownerPlayerId() == null).count());
    }

    @Test
    void movementValidatorKeepsBlockedAmbientEntityInPlace() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EntitySnapshot ambient = new EntitySnapshot(500L, "voxel:cozy_sheep", null, 4.5, 80.0, 4.5, 0.0f, 0.0f, 10);
        tracker.addAmbient(ambient);

        List<EntitySnapshot> updates = tracker.tickAmbient(80L, (current, candidate) -> false);
        EntitySnapshot moved = tracker.snapshot(ambient.entityId()).orElseThrow();
        ServerEntityTracker.AmbientTickStats stats = tracker.lastAmbientTickStats();

        assertEquals(1, updates.size());
        assertEquals(ambient.x(), moved.x(), 0.001);
        assertEquals(ambient.z(), moved.z(), 0.001);
        assertEquals(0.0, moved.velocityX(), 0.001);
        assertEquals(0.0, moved.velocityZ(), 0.001);
        assertNotEquals(EntitySnapshot.STATE_IDLE, moved.stateKey());
        assertEquals(1, stats.activeAmbient());
        assertEquals(1, stats.blockedAmbientMoves());
        assertEquals(1, stats.emittedSnapshots());
    }

    @Test
    @Tag("physicsRegression")
    void ambientEntitiesApplyFluidForcesBeforeSweep() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EntitySnapshot bunny = new EntitySnapshot(501L, "voxel:forest_bunny", null, 4.5, 80.0, 4.5, 0.0f, 0.0f, 10);
        tracker.addAmbient(bunny);

        tracker.tickAmbient(80L, (current, candidate) -> true, (x, y, z) -> FluidPhysics.water(0.30, 0.04, 0.0));

        EntitySnapshot moved = tracker.snapshot(bunny.entityId()).orElseThrow();
        assertTrue(moved.velocityY() > 0.0);
        assertTrue(moved.y() > bunny.y());
    }

    @Test
    void ambientEntitiesSeparateFromPlayerBoundsLocally() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        UUID playerId = UUID.randomUUID();
        tracker.registerPlayer(playerId);
        EntitySnapshot player = tracker.updatePlayer(playerId, 0.9, 81.62, 0.0, 0.0f, 0.0f);
        EntitySnapshot crawler = new EntitySnapshot(0L, "voxel:dune_crawler", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10);
        tracker.addAmbient(crawler);

        tracker.tickAmbient(0L);
        EntitySnapshot moved = tracker.snapshot(crawler.entityId()).orElseThrow();

        assertFalse(EntityPhysics.overlaps(moved, player, 0.02));
        assertTrue(moved.x() != crawler.x() || moved.z() != crawler.z());
        assertEquals(EntitySnapshot.STATE_WANDER, moved.stateKey());
    }

    @Test
    void ambientEntitiesParkOutsideActivePlayerChunkTickets() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        UUID playerId = UUID.randomUUID();
        tracker.registerPlayer(playerId);
        tracker.updatePlayer(playerId, 500.0, 80.0, 500.0, 0.0f, 0.0f);
        EntitySnapshot ambient = new EntitySnapshot(700L, "voxel:cozy_sheep", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10);
        tracker.addAmbient(ambient);

        List<EntitySnapshot> updates = tracker.tickAmbient(80L);
        EntitySnapshot parked = tracker.snapshot(ambient.entityId()).orElseThrow();
        ServerEntityTracker.AmbientTickStats stats = tracker.lastAmbientTickStats();

        assertFalse(updates.stream().anyMatch(snapshot -> snapshot.entityId() == ambient.entityId()));
        assertEquals(ambient.x(), parked.x(), 0.001);
        assertEquals(ambient.z(), parked.z(), 0.001);
        assertEquals(1, stats.parkedAmbient());
        assertEquals(0, stats.activeAmbient());
        assertEquals(0, stats.emittedSnapshots());
    }

    @Test
    void ambientEntitiesStayActiveInsidePlayerChunkTickets() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        UUID playerId = UUID.randomUUID();
        tracker.registerPlayer(playerId);
        tracker.updatePlayer(playerId, 8 * 16.0 + 0.5, 80.0, 0.5, 0.0f, 0.0f);
        EntitySnapshot ambient = new EntitySnapshot(701L, "voxel:cozy_sheep", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10);
        tracker.addAmbient(ambient);

        List<EntitySnapshot> updates = tracker.tickAmbient(80L);
        ServerEntityTracker.AmbientTickStats stats = tracker.lastAmbientTickStats();

        assertTrue(updates.stream().anyMatch(snapshot -> snapshot.entityId() == ambient.entityId()));
        assertEquals(0, stats.parkedAmbient());
        assertEquals(1, stats.activeAmbient());
    }

    @Test
    void itemDropsUseEntitySnapshotsAndPickupDelay() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        DroppedItemEntity drop = tracker.spawnItemDrop("voxel:moss_clump", new ItemStack((short) 68, 1), 4.5, 80.0, 4.5, 0L);

        EntitySnapshot spawned = tracker.snapshot(drop.entityId()).orElseThrow();
        assertTrue(ItemDropType.isTypeKey(spawned.typeKey()));
        assertTrue(tracker.itemDropsNear(4.5, 80.0, 4.5, 2.0, 7L).isEmpty());
        assertEquals(1, tracker.itemDropsNear(4.5, 80.0, 4.5, 2.0, 8L).size());

        tracker.tickAmbient(1L);
        EntitySnapshot moved = tracker.snapshot(drop.entityId()).orElseThrow();

        assertTrue(moved.y() < spawned.y());
        assertTrue(moved.velocityY() < drop.velocityY());
        assertEquals(1, tracker.lastAmbientTickStats().itemDropUpdates());
    }

    @Test
    void itemDropClaimRemovesDropOnlyOnce() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        DroppedItemEntity drop = tracker.spawnItemDrop("voxel:moss_clump", new ItemStack((short) 68, 1), 4.5, 80.0, 4.5, 0L);

        assertEquals(drop.entityId(), tracker.claimItemDrop(drop.entityId()).orElseThrow().entityId());
        assertTrue(tracker.claimItemDrop(drop.entityId()).isEmpty());
        assertEquals(0, tracker.itemDropCount());
    }

    @Test
    @Tag("physicsRegression")
    void itemDropsMergeSameStackWithoutDuplicatePickup() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        tracker.spawnItemDrop("voxel:moss_clump", new ItemStack((short) 68, 12), 4.5, 80.0, 4.5, 0L);
        tracker.spawnItemDrop("voxel:moss_clump", new ItemStack((short) 68, 10), 4.55, 80.0, 4.5, 0L);

        tracker.tickAmbient(9L);

        assertEquals(1, tracker.itemDropCount());
        DroppedItemEntity merged = tracker.itemDropsNear(4.5, 80.0, 4.5, 2.0, 9L).getFirst();
        assertEquals(22, merged.stack().count());
        assertEquals(merged.entityId(), tracker.claimItemDrop(merged.entityId()).orElseThrow().entityId());
        assertTrue(tracker.claimItemDrop(merged.entityId()).isEmpty());
    }

    @Test
    @Tag("physicsRegression")
    void itemDropMergeRespectsItemStackCaps() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        tracker.spawnItemDrop("voxel:stone_pickaxe", new ItemStack((short) 40, 1), 4.5, 80.0, 4.5, 0L);
        tracker.spawnItemDrop("voxel:stone_pickaxe", new ItemStack((short) 40, 1), 4.55, 80.0, 4.5, 0L);

        tracker.tickAmbient(9L);

        assertEquals(2, tracker.itemDropCount());
    }

    @Test
    void damageAmbientReturnsServerDamageResultAndKnockback() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        UUID playerId = UUID.randomUUID();
        tracker.registerPlayer(playerId);
        tracker.updatePlayer(playerId, 3.5, 80.0, 4.5, 0.0f, 0.0f);
        EntitySnapshot ambient = new EntitySnapshot(750L, "voxel:cozy_sheep", null, 4.5, 80.0, 4.5, 0.0f, 0.0f, 10);
        tracker.addAmbient(ambient);

        DamageResult result = tracker.damageAmbient(ambient.entityId(), 3, DamageSource.playerMelee(playerId), 1.0);

        assertTrue(result.accepted());
        assertEquals(3, result.amount());
        assertFalse(result.killed());
        assertEquals(7, result.snapshot().orElseThrow().health());
        assertTrue(result.knockbackX() > 0.0);
    }

    @Test
    void damageAmbientRejectsInvulnerabilityWindow() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        UUID playerId = UUID.randomUUID();
        EntitySnapshot ambient = new EntitySnapshot(751L, "voxel:cozy_sheep", null, 4.5, 80.0, 4.5, 0.0f, 0.0f, 10);
        tracker.addAmbient(ambient);

        DamageResult first = tracker.damageAmbient(ambient.entityId(), 3, DamageSource.playerMelee(playerId), 1.0);
        DamageResult second = tracker.damageAmbient(ambient.entityId(), 3, DamageSource.playerMelee(playerId), 1.1);
        DamageResult third = tracker.damageAmbient(ambient.entityId(), 3, DamageSource.playerMelee(playerId), 1.4);

        assertTrue(first.accepted());
        assertFalse(second.accepted());
        assertEquals(DamageResult.RejectionReason.INVULNERABLE, second.rejectionReason());
        assertTrue(third.accepted());
        assertEquals(4, tracker.snapshot(ambient.entityId()).orElseThrow().health());
    }

    @Test
    void damageAmbientKillRemovesLifecycleState() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EntitySnapshot ambient = new EntitySnapshot(752L, "voxel:moss_snail", null, 4.5, 80.0, 4.5, 0.0f, 0.0f, 3);
        tracker.addAmbient(ambient);

        DamageResult result = tracker.damageAmbient(ambient.entityId(), 5, DamageSource.environment("test"), 1.0);

        assertTrue(result.accepted());
        assertTrue(result.killed());
        assertEquals(3, result.amount());
        assertTrue(tracker.snapshot(ambient.entityId()).isEmpty());
    }

    @Test
    @Tag("physicsRegression")
    void arrowProjectilesUseSnapshotsAndDamageAmbientHits() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        UUID owner = UUID.randomUUID();
        EntitySnapshot target = new EntitySnapshot(800L, "voxel:cozy_sheep", null, 4.0, 80.0, 0.0, 0.0f, 0.0f, 10);
        tracker.addAmbient(target);
        ProjectileState arrow = tracker.spawnArrowProjectile(owner, 0.0, 80.45, 0.0, 1.0, 0.0, 0.0);

        assertTrue(tracker.snapshot(arrow.projectileId()).isPresent());

        List<ProjectileHit> hits = tracker.tickProjectiles(0.2, (x, y, z, bounds) -> false, (x, y, z) -> false);
        EntitySnapshot damaged = tracker.snapshot(target.entityId()).orElseThrow();

        assertEquals(ProjectileHit.Type.ENTITY, hits.getFirst().type());
        assertEquals(target.entityId(), hits.getFirst().entityId());
        assertTrue(tracker.snapshot(arrow.projectileId()).isEmpty());
        assertEquals(6, damaged.health());
        assertEquals(0, tracker.projectileCount());
    }

    @Test
    @Tag("physicsRegression")
    void arrowProjectilesDespawnOnBlockHit() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        ProjectileState arrow = tracker.spawnArrowProjectile(null, 0.0, 80.45, 0.0, 1.0, 0.0, 0.0);

        List<ProjectileHit> hits = tracker.tickProjectiles(0.2, (x, y, z, bounds) -> x >= 2.0, (x, y, z) -> false);

        assertFalse(hits.isEmpty());
        assertEquals(ProjectileHit.Type.BLOCK, hits.getFirst().type());
        assertTrue(tracker.snapshot(arrow.projectileId()).isEmpty());
        assertEquals(0, tracker.projectileCount());
    }

    @Test
    @Tag("physicsRegression")
    void projectileTickStatsReportHitsAndDuration() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        tracker.spawnArrowProjectile(null, 0.0, 80.45, 0.0, 1.0, 0.0, 0.0);

        tracker.tickProjectiles(0.2, (x, y, z, bounds) -> x >= 2.0, (x, y, z) -> false);

        ServerEntityTracker.ProjectileTickStats stats = tracker.lastProjectileTickStats();
        assertEquals(1, stats.projectilesBeforeTick());
        assertEquals(0, stats.projectilesAfterTick());
        assertEquals(1, stats.emittedHits());
        assertEquals(1, stats.blockHits());
        assertTrue(stats.durationNanos() >= 0L);
    }

    @Test
    @Tag("physicsRegression")
    void projectileCrossesChunkBoundaryWithoutGhostHit() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        ProjectileState arrow = tracker.spawnArrowProjectile(null, 15.75, 80.45, 0.0, 1.0, 0.0, 0.0);

        List<ProjectileHit> hits = tracker.tickProjectiles(0.05, (x, y, z, bounds) -> bounds.intersectsBlock(x, y, z, 18, 80, 0), (x, y, z) -> false);

        assertEquals(ProjectileHit.Type.MISS, hits.getFirst().type());
        ProjectileState moved = tracker.snapshot(arrow.projectileId())
                .map(snapshot -> new ProjectileState(snapshot.entityId(), snapshot.ownerPlayerId(), snapshot.typeKey(), snapshot.x(), snapshot.y(), snapshot.z(), snapshot.velocityX(), snapshot.velocityY(), snapshot.velocityZ(), 1))
                .orElseThrow();
        assertTrue(moved.x() > 16.0);
    }

    @Test
    @Tag("physicsRegression")
    void knockbackBlockedByTerrainValidatorSlidesAlongFreeAxis() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        UUID playerId = UUID.randomUUID();
        tracker.registerPlayer(playerId);
        tracker.updatePlayer(playerId, 0.0, 81.62, 0.0, 0.0f, 0.0f);
        EntitySnapshot sheep = new EntitySnapshot(900L, "voxel:cozy_sheep", null, 1.0, 80.0, 1.0, 0.0f, 0.0f, 10);
        tracker.addAmbient(sheep);

        tracker.damageAmbient(sheep.entityId(), 1, playerId, 0.5);
        tracker.tickAmbient(1L, (current, candidate) -> candidate.x() <= 1.15);

        EntitySnapshot slid = tracker.snapshot(sheep.entityId()).orElseThrow();
        assertEquals(1.0, slid.x(), 0.2);
        assertTrue(slid.z() > sheep.z());
        assertEquals(0.0, slid.velocityX(), 0.001);
        assertTrue(slid.velocityZ() > 0.0);
    }

    @Test
    @Tag("physicsRegression")
    void localEntityBlockPreventsOverlapWithoutOscillation() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EntitySnapshot mover = new EntitySnapshot(901L, "voxel:cozy_sheep", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10)
                .withVelocity(0.5, 0.0, 0.0);
        EntitySnapshot blocker = new EntitySnapshot(902L, "voxel:cozy_sheep", null, 0.65, 80.0, 0.0, 0.0f, 0.0f, 10);
        tracker.addAmbient(mover);
        tracker.addAmbient(blocker);

        tracker.tickAmbient(1L, (current, candidate) -> true);
        EntitySnapshot first = tracker.snapshot(mover.entityId()).orElseThrow();
        tracker.tickAmbient(2L, (current, candidate) -> true);
        EntitySnapshot second = tracker.snapshot(mover.entityId()).orElseThrow();
        EntitySnapshot other = tracker.snapshot(blocker.entityId()).orElseThrow();

        assertEquals(first.x(), second.x(), 0.75);
        assertFalse(EntityPhysics.overlaps(second, other, 0.02));
    }

    private static double distanceSquared(double x, double z, double targetX, double targetZ) {
        double dx = x - targetX;
        double dz = z - targetZ;
        return dx * dx + dz * dz;
    }
}
