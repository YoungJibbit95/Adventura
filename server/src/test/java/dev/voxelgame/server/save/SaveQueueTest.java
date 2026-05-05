package dev.voxelgame.server.save;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveQueueTest {
    @TempDir
    Path tempDir;

    @Test
    void coalescesPendingWritesPerKeyAndFlushesLatestPayload() throws Exception {
        Path savePath = tempDir.resolve("player.properties");
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        try (SaveQueue queue = new SaveQueue("test-save-queue", 8)) {
            CompletableFuture<SaveQueue.SaveResult> blocker = queue.enqueue("blocker", () -> {
                blockerStarted.countDown();
                assertTrue(releaseBlocker.await(2, TimeUnit.SECONDS));
                return 0L;
            });
            assertTrue(blockerStarted.await(1, TimeUnit.SECONDS));

            CompletableFuture<SaveQueue.SaveResult> first = queue.enqueue("player:ada", () -> {
                Files.writeString(savePath, "old");
                return Files.size(savePath);
            });
            CompletableFuture<SaveQueue.SaveResult> latest = queue.enqueue("player:ada", () -> {
                Files.writeString(savePath, "latest");
                return Files.size(savePath);
            });

            assertEquals(1L, queue.stats().coalescedWrites());
            releaseBlocker.countDown();
            queue.flush();

            assertEquals("latest", Files.readString(savePath));
            assertEquals("player:ada", first.get(1, TimeUnit.SECONDS).key());
            assertEquals("player:ada", latest.get(1, TimeUnit.SECONDS).key());
            assertTrue(blocker.isDone());
            assertEquals(2L, queue.stats().completedWrites());
        }
    }

    @Test
    void recordsFailuresAndContinuesWithLaterWrites() throws Exception {
        try (SaveQueue queue = new SaveQueue("test-save-queue", 8)) {
            CompletableFuture<SaveQueue.SaveResult> failed = queue.enqueue("bad", () -> {
                throw new IOException("boom");
            });
            CompletableFuture<SaveQueue.SaveResult> ok = queue.enqueue("ok", () -> 7L);

            queue.flush();

            assertTrue(failed.isCompletedExceptionally());
            assertEquals(7L, ok.get(1, TimeUnit.SECONDS).bytesWritten());
            assertEquals(1L, queue.stats().failedWrites());
            assertEquals(1L, queue.stats().completedWrites());
        }
    }

    @Test
    void rejectsNewKeysWhenPendingLimitIsReached() throws Exception {
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        try (SaveQueue queue = new SaveQueue("test-save-queue", 1)) {
            queue.enqueue("blocker", () -> {
                blockerStarted.countDown();
                assertTrue(releaseBlocker.await(2, TimeUnit.SECONDS));
                return 0L;
            });
            assertTrue(blockerStarted.await(1, TimeUnit.SECONDS));
            CompletableFuture<SaveQueue.SaveResult> accepted = queue.enqueue("accepted", () -> 1L);
            CompletableFuture<SaveQueue.SaveResult> rejected = queue.enqueue("rejected", () -> 1L);

            assertTrue(rejected.isCompletedExceptionally());
            releaseBlocker.countDown();
            queue.flush();

            assertEquals(1L, accepted.get(1, TimeUnit.SECONDS).bytesWritten());
            assertEquals(1L, queue.stats().rejectedWrites());
        }
    }
}
