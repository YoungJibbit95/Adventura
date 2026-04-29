package dev.voxelgame.common.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkPosTest {
    @Test
    void mapsNegativeBlocksToFloorChunks() {
        assertEquals(new ChunkPos(-1, -1), ChunkPos.fromBlock(-1, -1));
        assertEquals(15, ChunkPos.localCoord(-1));
    }
}
