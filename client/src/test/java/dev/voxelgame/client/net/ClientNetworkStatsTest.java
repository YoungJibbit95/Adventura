package dev.voxelgame.client.net;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.world.ChunkPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientNetworkStatsTest {
    @Test
    void countsSentAndReceivedPacketCategories() {
        ClientNetworkStats stats = new ClientNetworkStats();

        stats.recordSent(new GamePacket.Chat("hello"));
        stats.recordReceived(new GamePacket.ChunkData(new ChunkPos(1, -1), 0, new short[0], new byte[0], new byte[0]));
        stats.recordReceived(new GamePacket.BlockUpdate(1, 2, 3, (short) 4));
        stats.recordReceived(new GamePacket.Chat("server"));

        ClientNetworkStats.Snapshot snapshot = stats.snapshot();
        assertEquals(1, snapshot.sentPackets());
        assertEquals(3, snapshot.receivedPackets());
        assertEquals(1, snapshot.chunkPackets());
        assertEquals(1, snapshot.blockUpdatePackets());
        assertEquals(1, snapshot.chatPackets());
    }
}
