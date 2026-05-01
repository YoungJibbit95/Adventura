package dev.voxelgame.server.net;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.server.world.ServerWorld;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void asyncRequestsRunQueuedChunksByPriority() throws Exception {
        CountDownLatch enteredFirstLoad = new CountDownLatch(1);
        CountDownLatch releaseFirstLoad = new CountDownLatch(1);
        CopyOnWriteArrayList<ChunkPos> loadedAfterFirst = new CopyOnWriteArrayList<>();
        ChunkPos firstPos = new ChunkPos(0, 0);
        ChunkPos lowPriorityPos = new ChunkPos(8, 0);
        ChunkPos highPriorityPos = new ChunkPos(1, 0);
        ServerChunkStreamer streamer = ServerChunkStreamer.async(
                "test-chunk-streamer",
                1,
                8,
                (world, pos) -> {
                    if (pos.equals(firstPos)) {
                        enteredFirstLoad.countDown();
                        assertTrue(await(releaseFirstLoad));
                    } else {
                        loadedAfterFirst.add(pos);
                    }
                    return emptyChunk(pos);
                }
        );
        try {
            ServerWorld world = new ServerWorld(123L);

            CompletableFuture<GamePacket.ChunkData> first = streamer.request(world, firstPos, 100).orElseThrow();
            assertTrue(enteredFirstLoad.await(3, TimeUnit.SECONDS));
            CompletableFuture<GamePacket.ChunkData> lowPriority = streamer.request(world, lowPriorityPos, 50).orElseThrow();
            CompletableFuture<GamePacket.ChunkData> highPriority = streamer.request(world, highPriorityPos, 1).orElseThrow();

            releaseFirstLoad.countDown();

            first.get(3, TimeUnit.SECONDS);
            highPriority.get(3, TimeUnit.SECONDS);
            lowPriority.get(3, TimeUnit.SECONDS);
            assertEquals(List.of(highPriorityPos, lowPriorityPos), loadedAfterFirst);
        } finally {
            streamer.close();
        }
    }

    @Test
    void failedChunkRequestsCleanUpInFlightStateForRetry() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        ChunkPos pos = new ChunkPos(0, 0);
        ServerChunkStreamer streamer = ServerChunkStreamer.async(
                "test-chunk-streamer",
                1,
                8,
                (world, requestedPos) -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new IllegalStateException("chunk boom");
                    }
                    return emptyChunk(requestedPos);
                }
        );
        try {
            ServerWorld world = new ServerWorld(123L);

            CompletableFuture<GamePacket.ChunkData> failed = streamer.request(world, pos, 0).orElseThrow();
            ExecutionException thrown = assertThrows(ExecutionException.class, () -> failed.get(3, TimeUnit.SECONDS));
            assertInstanceOf(IllegalStateException.class, thrown.getCause());
            assertEquals(0, streamer.snapshot().inFlightChunks());

            CompletableFuture<GamePacket.ChunkData> retried = streamer.request(world, pos, 0).orElseThrow();
            assertEquals(pos, retried.get(3, TimeUnit.SECONDS).pos());

            ServerChunkStreamer.Snapshot stats = streamer.snapshot();
            assertEquals(2, attempts.get());
            assertEquals(1, stats.failedChunks());
            assertEquals(1, stats.completedChunks());
            assertEquals(0, stats.inFlightChunks());
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
