package dev.voxelgame.server.entity;

import dev.voxelgame.common.entity.EntitySnapshot;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
