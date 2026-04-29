package dev.voxelgame.server.entity;

import dev.voxelgame.common.entity.EntitySnapshot;
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

    private static double distanceSquared(double x, double z, double targetX, double targetZ) {
        double dx = x - targetX;
        double dz = z - targetZ;
        return dx * dx + dz * dz;
    }
}
