package dev.voxelgame.common.physics;

import dev.voxelgame.common.world.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhysicsTicketsTest {
    @Test
    void chunkTicketUsesChebyshevDistance() {
        ChunkPos center = new ChunkPos(2, -1);

        assertEquals(3, PhysicsTickets.chebyshevDistance(center, new ChunkPos(5, -4)));
        assertTrue(PhysicsTickets.insideTicket(new ChunkPos(10, -1), center, 8));
        assertFalse(PhysicsTickets.insideTicket(new ChunkPos(11, -1), center, 8));
    }

    @Test
    void chunkPositionsAreDerivedFromWorldCoordinates() {
        assertEquals(new ChunkPos(0, 0), PhysicsTickets.chunkFor(8.5, 8.5));
        assertEquals(new ChunkPos(-1, -1), PhysicsTickets.chunkFor(-0.1, -0.1));
    }

    @Test
    void insideAnyTicketChecksAllCenters() {
        List<ChunkPos> centers = List.of(new ChunkPos(0, 0), new ChunkPos(12, 0));

        assertTrue(PhysicsTickets.insideAnyTicket(new ChunkPos(20, 0), centers, 8));
        assertFalse(PhysicsTickets.insideAnyTicket(new ChunkPos(21, 0), centers, 8));
        assertEquals(25, PhysicsTickets.requiredChunkCount(2));
    }
}
