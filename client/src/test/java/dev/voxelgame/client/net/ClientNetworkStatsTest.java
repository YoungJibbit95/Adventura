package dev.voxelgame.client.net;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.net.PacketCodec;
import dev.voxelgame.common.world.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientNetworkStatsTest {
    @Test
    void countsSentAndReceivedPacketCategories() {
        AtomicLong clock = new AtomicLong(1_000_000_000L);
        ClientNetworkStats stats = new ClientNetworkStats(clock::get);

        GamePacket.Chat sent = new GamePacket.Chat("hello");
        stats.recordSent(sent);
        stats.recordReceived(new GamePacket.ChunkData(new ChunkPos(1, -1), 0, new short[0], new byte[0], new byte[0]));
        stats.recordReceived(new GamePacket.BlockUpdate(1, 2, 3, (short) 4));
        stats.recordReceived(new GamePacket.Chat("server"));
        stats.recordReceivedBytes(30);
        stats.recordInvalidPacket();
        stats.setChunkStreamQueueLength(5);
        clock.set(3_000_000_000L);

        ClientNetworkStats.Snapshot snapshot = stats.snapshot();
        assertEquals(1, snapshot.sentPackets());
        assertEquals(3, snapshot.receivedPackets());
        assertEquals(0.5, snapshot.sentPacketsPerSecond(), 0.0001);
        assertEquals(1.5, snapshot.receivedPacketsPerSecond(), 0.0001);
        assertEquals(1, snapshot.chunkPackets());
        assertEquals(1, snapshot.blockUpdatePackets());
        assertEquals(1, snapshot.chatPackets());
        assertEquals((PacketCodec.encode(sent).length + 30) / 4.0, snapshot.averagePacketBytes(), 0.0001);
        assertEquals(1L, snapshot.invalidPacketsDropped());
        assertEquals(5, snapshot.chunkStreamQueueLength());
    }
}
