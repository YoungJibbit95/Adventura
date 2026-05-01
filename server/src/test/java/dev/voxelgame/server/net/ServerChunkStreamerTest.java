package dev.voxelgame.server.net;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.server.world.ServerWorld;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerChunkStreamerTest {
    @Test
    void asyncRequestsForSameChunkShareInFlightGeneration() throws Exception {
        CountDownLatch enteredLoader = new CountDownLatch(1);
        CountDownLatch releaseLoader = new CountDownLatch(1);
        AtomicInteger loads = new AtomicInteger();
        ServerChunkStreamer streamer = ServerChunkStreamer.async(
                "test-chunk-streamer",
                1,
                8,
                (world, pos) -> {
                    loads.incrementAndGet();
                    enteredLoader.countDown();
                    assertTrue(await(releaseLoader));
                    return emptyChunk(pos);
                }
        );
        try {
            ServerWorld world = new ServerWorld(123L);
            ChunkPos pos = new ChunkPos(0, 0);

            Optional<CompletableFuture<GamePacket.ChunkData>> first = streamer.request(world, pos, 0);
            assertTrue(first.isPresent());
            assertTrue(enteredLoader.await(3, TimeUnit.SECONDS));
            Optional<CompletableFuture<GamePacket.ChunkData>> second = streamer.request(world, pos, 0);

            assertTrue(second.isPresent());
            assertSame(first.get(), second.get());

            releaseLoader.countDown();
            GamePacket.ChunkData packet = first.get().get(3, TimeUnit.SECONDS);
            assertSame(packet, second.get().get(3, TimeUnit.SECONDS));
            assertEquals(1, loads.get());

            ServerChunkStreamer.Snapshot stats = streamer.snapshot();
            assertEquals(2, stats.requestedChunks());
            assertEquals(1, stats.queuedChunks());
            assertEquals(1, stats.deduplicatedRequests());
            assertEquals(1, stats.completedChunks());
        } finally {
            streamer.close();
        }
    }

    @Test
    void maxInFlightChunksRejectsRequestsForBackpressure() throws Exception {
        CountDownLatch enteredLoader = new CountDownLatch(1);
        CountDownLatch releaseLoader = new CountDownLatch(1);
        ServerChunkStreamer streamer = ServerChunkStreamer.async(
                "test-chunk-streamer",
                1,
                1,
                (world, pos) -> {
                    enteredLoader.countDown();
                    assertTrue(await(releaseLoader));
                    return emptyChunk(pos);
                }
        );
        try {
            ServerWorld world = new ServerWorld(123L);

            Optional<CompletableFuture<GamePacket.ChunkData>> first = streamer.request(world, new ChunkPos(0, 0), 0);
            assertTrue(first.isPresent());
            assertTrue(enteredLoader.await(3, TimeUnit.SECONDS));

            Optional<CompletableFuture<GamePacket.ChunkData>> rejected = streamer.request(world, new ChunkPos(1, 0), 1);

            assertTrue(rejected.isEmpty());
            assertEquals(1, streamer.snapshot().rejectedChunks());

            releaseLoader.countDown();
            first.get().get(3, TimeUnit.SECONDS);
        } finally {
            streamer.close();
        }
    }

    private static GamePacket.ChunkData emptyChunk(ChunkPos pos) {
        return new GamePacket.ChunkData(pos, 0, new short[0], new byte[0], new byte[0]);
    }

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
