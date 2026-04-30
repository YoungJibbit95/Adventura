package dev.voxelgame.server.entity;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.entity.ItemDropType;
import dev.voxelgame.common.item.ItemStack;
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

        assertEquals(1, updates.size());
        assertEquals(ambient.x(), moved.x(), 0.001);
        assertEquals(ambient.z(), moved.z(), 0.001);
        assertEquals(0.0, moved.velocityX(), 0.001);
        assertEquals(0.0, moved.velocityZ(), 0.001);
        assertNotEquals(EntitySnapshot.STATE_IDLE, moved.stateKey());
    }

    @Test
    void ambientEntitiesAvoidPlayerBoundsLocally() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        UUID playerId = UUID.randomUUID();
        tracker.registerPlayer(playerId);
        tracker.updatePlayer(playerId, 0.9, 81.62, 0.0, 0.0f, 0.0f);
        EntitySnapshot crawler = new EntitySnapshot(0L, "voxel:dune_crawler", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10);
        tracker.addAmbient(crawler);

        tracker.tickAmbient(0L);
        EntitySnapshot moved = tracker.snapshot(crawler.entityId()).orElseThrow();

        assertEquals(crawler.x(), moved.x(), 0.001);
        assertEquals(crawler.z(), moved.z(), 0.001);
        assertEquals(EntitySnapshot.STATE_WANDER, moved.stateKey());
    }

    @Test
    void ambientEntitiesParkOutsideActivePlayerRadius() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        UUID playerId = UUID.randomUUID();
        tracker.registerPlayer(playerId);
        tracker.updatePlayer(playerId, 500.0, 80.0, 500.0, 0.0f, 0.0f);
        EntitySnapshot ambient = new EntitySnapshot(700L, "voxel:cozy_sheep", null, 0.0, 80.0, 0.0, 0.0f, 0.0f, 10);
        tracker.addAmbient(ambient);

        List<EntitySnapshot> updates = tracker.tickAmbient(80L);
        EntitySnapshot parked = tracker.snapshot(ambient.entityId()).orElseThrow();

        assertFalse(updates.stream().anyMatch(snapshot -> snapshot.entityId() == ambient.entityId()));
        assertEquals(ambient.x(), parked.x(), 0.001);
        assertEquals(ambient.z(), parked.z(), 0.001);
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
    }

    @Test
    void itemDropClaimRemovesDropOnlyOnce() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        DroppedItemEntity drop = tracker.spawnItemDrop("voxel:moss_clump", new ItemStack((short) 68, 1), 4.5, 80.0, 4.5, 0L);

        assertEquals(drop.entityId(), tracker.claimItemDrop(drop.entityId()).orElseThrow().entityId());
        assertTrue(tracker.claimItemDrop(drop.entityId()).isEmpty());
        assertEquals(0, tracker.itemDropCount());
    }

    private static double distanceSquared(double x, double z, double targetX, double targetZ) {
        double dx = x - targetX;
        double dz = z - targetZ;
        return dx * dx + dz * dz;
    }
}
