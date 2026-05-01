package dev.voxelgame.common.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkStreamingRingsTest {
    @Test
    void clientRingsClampOutwardAndKeepRetainHysteresis() {
        ChunkStreamingRings rings = ChunkStreamingRings.client(3, 1);

        assertEquals(2, rings.simulationRadiusChunks());
        assertEquals(3, rings.renderRadiusChunks());
        assertEquals(3, rings.previewRadiusChunks());
        assertEquals(5, rings.retainRadiusChunks());
        assertTrue(rings.contains(new ChunkPos(0, 0), new ChunkPos(5, 0), ChunkStreamingRings.Ring.RETAIN));
    }

    @Test
    void subscriptionRingsUseSameSquareDistanceLogicAsClientRetention() {
        ChunkStreamingRings rings = ChunkStreamingRings.subscription(2);
        ChunkPos center = new ChunkPos(4, -3);

        List<ChunkPos> positions = rings.positionsInRing(center, ChunkStreamingRings.Ring.RENDER);

        assertEquals(25, positions.size());
        assertEquals(center, positions.getFirst());
        assertTrue(positions.contains(new ChunkPos(2, -5)));
        assertTrue(positions.contains(new ChunkPos(6, -1)));
    }
}
