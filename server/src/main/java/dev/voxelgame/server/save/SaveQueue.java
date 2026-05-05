package dev.voxelgame.server.save;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class SaveQueue implements AutoCloseable {
    public static final int DEFAULT_MAX_PENDING_WRITES = 128;

    private final Object lock = new Object();
    private final ArrayDeque<SaveSlot> ready = new ArrayDeque<>();
    private final Map<String, SaveSlot> pendingByKey = new HashMap<>();
    private final ExecutorService executor;
    private final AtomicBoolean draining = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final int maxPendingWrites;
    private final AtomicLong enqueuedWrites = new AtomicLong();
    private final AtomicLong coalescedWrites = new AtomicLong();
    private final AtomicLong completedWrites = new AtomicLong();
    private final AtomicLong failedWrites = new AtomicLong();
    private final AtomicLong rejectedWrites = new AtomicLong();
    private final AtomicLong writtenBytes = new AtomicLong();
    private final AtomicLong totalWriteNanos = new AtomicLong();
    private final AtomicLong lastCompletedAtNanos = new AtomicLong();
    private final AtomicLong maxObservedPendingWrites = new AtomicLong();
    private final long statsStartedAtNanos = System.nanoTime();
    private int runningWrites;

    public SaveQueue(String threadName, int maxPendingWrites) {
        if (maxPendingWrites <= 0) {
            throw new IllegalArgumentException("maxPendingWrites must be > 0");
        }
        this.maxPendingWrites = maxPendingWrites;
        this.executor = Executors.newSingleThreadExecutor(saveThreadFactory(threadName));
    }

    public static SaveQueue createDefault() {
        return new SaveQueue("adventura-save-queue", DEFAULT_MAX_PENDING_WRITES);
    }

    public CompletableFuture<SaveResult> enqueue(String key, SaveWriter writer) {
        Objects.requireNonNull(writer, "writer");
        String safeKey = requireKey(key);
        CompletableFuture<SaveResult> future = new CompletableFuture<>();
        synchronized (lock) {
            if (closed.get()) {
                rejectedWrites.incrementAndGet();
                future.completeExceptionally(new RejectedExecutionException("Save queue is closed"));
                return future;
            }
            SaveSlot existing = pendingByKey.get(safeKey);
            if (existing != null && !existing.running) {
                existing.writer = writer;
                existing.futures.add(future);
                coalescedWrites.incrementAndGet();
                return future;
            }
            if (pendingByKey.size() >= maxPendingWrites) {
                rejectedWrites.incrementAndGet();
                future.completeExceptionally(new RejectedExecutionException("Save queue has too many pending writes"));
                return future;
            }
            SaveSlot slot = new SaveSlot(safeKey, writer);
            slot.futures.add(future);
            pendingByKey.put(safeKey, slot);
            ready.addLast(slot);
            enqueuedWrites.incrementAndGet();
            updateMaxObservedPendingWritesLocked();
        }
        scheduleDrain();
        return future;
    }

    public void flush() throws InterruptedException {
        synchronized (lock) {
            while (!ready.isEmpty() || runningWrites > 0) {
                lock.wait();
            }
        }
    }

    public boolean flush(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout, "timeout");
        long timeoutNanos = Math.max(0L, timeout.toNanos());
        long deadline = System.nanoTime() + timeoutNanos;
        synchronized (lock) {
            while (!ready.isEmpty() || runningWrites > 0) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0L) {
                    return false;
                }
                TimeUnit.NANOSECONDS.timedWait(lock, remaining);
            }
            return true;
        }
    }

    public SaveQueueStats stats() {
        synchronized (lock) {
            long completed = completedWrites.get();
            long totalNanos = totalWriteNanos.get();
            long lastCompleted = lastCompletedAtNanos.get();
            double elapsedSeconds = Math.max(1e-9, (System.nanoTime() - statsStartedAtNanos) / 1_000_000_000.0);
            long ageMillis = lastCompleted == 0L
                    ? 0L
                    : TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - lastCompleted));
            long written = writtenBytes.get();
            long failed = failedWrites.get();
            return new SaveQueueStats(
                    ready.size() + runningWrites,
                    runningWrites,
                    maxObservedPendingWrites.get(),
                    enqueuedWrites.get(),
                    coalescedWrites.get(),
                    completed,
                    failed,
                    rejectedWrites.get(),
                    written,
                    TimeUnit.NANOSECONDS.toMillis(totalNanos),
                    completed == 0L ? 0.0 : TimeUnit.NANOSECONDS.toMicros(totalNanos) / 1000.0 / completed,
                    enqueuedWrites.get() / elapsedSeconds,
                    written / elapsedSeconds,
                    TimeUnit.NANOSECONDS.toMicros(totalNanos) / 1000.0 / elapsedSeconds,
                    failed / elapsedSeconds,
                    ageMillis
            );
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            flush();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
    }

    private void scheduleDrain() {
        if (draining.compareAndSet(false, true)) {
            executor.execute(this::drain);
        }
    }

    private void drain() {
        try {
            while (true) {
                SaveSlot slot;
                synchronized (lock) {
                    slot = ready.pollFirst();
                    if (slot == null) {
                        draining.set(false);
                        lock.notifyAll();
                        if (!ready.isEmpty() && draining.compareAndSet(false, true)) {
                            continue;
                        }
                        return;
                    }
                    slot.running = true;
                    pendingByKey.remove(slot.key, slot);
                    runningWrites++;
                }
                runSlot(slot);
                synchronized (lock) {
                    runningWrites--;
                    lock.notifyAll();
                }
            }
        } catch (RuntimeException exception) {
            draining.set(false);
            throw exception;
        }
    }

    private void runSlot(SaveSlot slot) {
        List<CompletableFuture<SaveResult>> futures = List.copyOf(slot.futures);
        long startedAt = System.nanoTime();
        try {
            long bytes = Math.max(0L, slot.writer.write());
            long duration = Math.max(0L, System.nanoTime() - startedAt);
            SaveResult result = new SaveResult(slot.key, bytes, duration);
            completedWrites.incrementAndGet();
            writtenBytes.addAndGet(bytes);
            totalWriteNanos.addAndGet(duration);
            lastCompletedAtNanos.set(System.nanoTime());
            for (CompletableFuture<SaveResult> future : futures) {
                future.complete(result);
            }
        } catch (Exception exception) {
            failedWrites.incrementAndGet();
            for (CompletableFuture<SaveResult> future : futures) {
                future.completeExceptionally(exception);
            }
        }
    }

    private void updateMaxObservedPendingWritesLocked() {
        long pending = ready.size() + runningWrites;
        maxObservedPendingWrites.accumulateAndGet(pending, Math::max);
    }

    private static String requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Save key must not be blank");
        }
        return key.strip();
    }

    private static ThreadFactory saveThreadFactory(String threadName) {
        String safeName = threadName == null || threadName.isBlank() ? "adventura-save-queue" : threadName.strip();
        AtomicLong sequence = new AtomicLong();
        return runnable -> {
            Thread thread = new Thread(runnable, safeName + "-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    @FunctionalInterface
    public interface SaveWriter {
        long write() throws Exception;
    }

    public record SaveResult(String key, long bytesWritten, long durationNanos) {
        public SaveResult {
            key = requireKey(key);
            bytesWritten = Math.max(0L, bytesWritten);
            durationNanos = Math.max(0L, durationNanos);
        }
    }

    public record SaveQueueStats(
            int pendingWrites,
            int runningWrites,
            long maxObservedPendingWrites,
            long enqueuedWrites,
            long coalescedWrites,
            long completedWrites,
            long failedWrites,
            long rejectedWrites,
            long writtenBytes,
            long totalWriteMilliseconds,
            double averageWriteMilliseconds,
            double enqueuedWritesPerSecond,
            double writtenBytesPerSecond,
            double writeMillisecondsPerSecond,
            double failedWritesPerSecond,
            long lastCompletedAgeMilliseconds
    ) {
        public SaveQueueStats {
            pendingWrites = Math.max(0, pendingWrites);
            runningWrites = Math.max(0, runningWrites);
            maxObservedPendingWrites = Math.max(0L, maxObservedPendingWrites);
            enqueuedWrites = Math.max(0L, enqueuedWrites);
            coalescedWrites = Math.max(0L, coalescedWrites);
            completedWrites = Math.max(0L, completedWrites);
            failedWrites = Math.max(0L, failedWrites);
            rejectedWrites = Math.max(0L, rejectedWrites);
            writtenBytes = Math.max(0L, writtenBytes);
            totalWriteMilliseconds = Math.max(0L, totalWriteMilliseconds);
            averageWriteMilliseconds = Double.isFinite(averageWriteMilliseconds)
                    ? Math.max(0.0, averageWriteMilliseconds)
                    : 0.0;
            enqueuedWritesPerSecond = Double.isFinite(enqueuedWritesPerSecond)
                    ? Math.max(0.0, enqueuedWritesPerSecond)
                    : 0.0;
            writtenBytesPerSecond = Double.isFinite(writtenBytesPerSecond)
                    ? Math.max(0.0, writtenBytesPerSecond)
                    : 0.0;
            writeMillisecondsPerSecond = Double.isFinite(writeMillisecondsPerSecond)
                    ? Math.max(0.0, writeMillisecondsPerSecond)
                    : 0.0;
            failedWritesPerSecond = Double.isFinite(failedWritesPerSecond)
                    ? Math.max(0.0, failedWritesPerSecond)
                    : 0.0;
            lastCompletedAgeMilliseconds = Math.max(0L, lastCompletedAgeMilliseconds);
        }

        public static SaveQueueStats empty() {
            return new SaveQueueStats(
                    0,
                    0,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0L
            );
        }
    }

    private static final class SaveSlot {
        private final String key;
        private final List<CompletableFuture<SaveResult>> futures = new ArrayList<>();
        private SaveWriter writer;
        private boolean running;

        private SaveSlot(String key, SaveWriter writer) {
            this.key = key;
            this.writer = writer;
        }
    }
}
