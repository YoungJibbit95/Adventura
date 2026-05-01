package dev.voxelgame.server.net;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.server.world.ServerWorld;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

final class ServerChunkStreamer implements AutoCloseable {
    private static final int DEFAULT_MAX_IN_FLIGHT_CHUNKS = 256;

    private final ThreadPoolExecutor executor;
    private final int maxInFlightChunks;
    private final boolean direct;
    private final ChunkLoader chunkLoader;
    private final ConcurrentMap<RequestKey, CompletableFuture<GamePacket.ChunkData>> inFlight = new ConcurrentHashMap<>();
    private final AtomicLong requestSequence = new AtomicLong();
    private final AtomicLong requestedChunks = new AtomicLong();
    private final AtomicLong queuedChunks = new AtomicLong();
    private final AtomicLong deduplicatedRequests = new AtomicLong();
    private final AtomicLong completedChunks = new AtomicLong();
    private final AtomicLong failedChunks = new AtomicLong();
    private final AtomicLong rejectedChunks = new AtomicLong();

    private ServerChunkStreamer(ThreadPoolExecutor executor, int maxInFlightChunks, boolean direct, ChunkLoader chunkLoader) {
        this.executor = executor;
        this.maxInFlightChunks = maxInFlightChunks;
        this.direct = direct;
        this.chunkLoader = chunkLoader;
    }

    static ServerChunkStreamer direct() {
        return new ServerChunkStreamer(null, Integer.MAX_VALUE, true, ServerWorld::packetFor);
    }

    static ServerChunkStreamer createDefault() {
        int workers = Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 2));
        return async("adventura-chunk-streamer", workers, DEFAULT_MAX_IN_FLIGHT_CHUNKS);
    }

    static ServerChunkStreamer async(String threadNamePrefix, int workerCount, int maxInFlightChunks) {
        return async(threadNamePrefix, workerCount, maxInFlightChunks, ServerWorld::packetFor);
    }

    static ServerChunkStreamer async(String threadNamePrefix, int workerCount, int maxInFlightChunks, ChunkLoader chunkLoader) {
        if (workerCount < 1) {
            throw new IllegalArgumentException("workerCount must be >= 1");
        }
        if (maxInFlightChunks < 1) {
            throw new IllegalArgumentException("maxInFlightChunks must be >= 1");
        }
        Objects.requireNonNull(chunkLoader, "chunkLoader");
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                workerCount,
                workerCount,
                0L,
                TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<>(),
                daemonFactory(threadNamePrefix)
        );
        executor.prestartAllCoreThreads();
        return new ServerChunkStreamer(executor, maxInFlightChunks, false, chunkLoader);
    }

    Optional<CompletableFuture<GamePacket.ChunkData>> request(ServerWorld world, ChunkPos pos, int priority) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(pos, "pos");
        requestedChunks.incrementAndGet();
        if (direct) {
            try {
                GamePacket.ChunkData packet = chunkLoader.load(world, pos);
                completedChunks.incrementAndGet();
                return Optional.of(CompletableFuture.completedFuture(packet));
            } catch (RuntimeException error) {
                failedChunks.incrementAndGet();
                return Optional.of(CompletableFuture.failedFuture(error));
            }
        }

        RequestKey key = new RequestKey(world, pos);
        CompletableFuture<GamePacket.ChunkData> existing = inFlight.get(key);
        if (existing != null) {
            deduplicatedRequests.incrementAndGet();
            return Optional.of(existing);
        }
        if (inFlight.size() >= maxInFlightChunks) {
            rejectedChunks.incrementAndGet();
            return Optional.empty();
        }

        CompletableFuture<GamePacket.ChunkData> future = new CompletableFuture<>();
        CompletableFuture<GamePacket.ChunkData> previous = inFlight.putIfAbsent(key, future);
        if (previous != null) {
            deduplicatedRequests.incrementAndGet();
            return Optional.of(previous);
        }

        queuedChunks.incrementAndGet();
        try {
            executor.execute(new PrioritizedChunkTask(
                    priority,
                    requestSequence.getAndIncrement(),
                    () -> chunkLoader.load(world, pos),
                    future,
                    () -> inFlight.remove(key, future),
                    completedChunks,
                    failedChunks
            ));
        } catch (RejectedExecutionException error) {
            inFlight.remove(key, future);
            rejectedChunks.incrementAndGet();
            future.completeExceptionally(error);
        }
        return Optional.of(future);
    }

    Snapshot snapshot() {
        return new Snapshot(
                requestedChunks.get(),
                queuedChunks.get(),
                deduplicatedRequests.get(),
                completedChunks.get(),
                failedChunks.get(),
                rejectedChunks.get(),
                inFlight.size(),
                maxInFlightChunks
        );
    }

    @Override
    public void close() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private static ThreadFactory daemonFactory(String threadNamePrefix) {
        AtomicInteger threadId = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, threadNamePrefix + "-" + threadId.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private record RequestKey(ServerWorld world, ChunkPos pos) {
    }

    record Snapshot(
            long requestedChunks,
            long queuedChunks,
            long deduplicatedRequests,
            long completedChunks,
            long failedChunks,
            long rejectedChunks,
            int inFlightChunks,
            int maxInFlightChunks
    ) {
    }

    private static final class PrioritizedChunkTask implements Runnable, Comparable<PrioritizedChunkTask> {
        private final int priority;
        private final long sequence;
        private final ChunkSupplier supplier;
        private final CompletableFuture<GamePacket.ChunkData> future;
        private final Runnable cleanup;
        private final AtomicLong completedChunks;
        private final AtomicLong failedChunks;

        private PrioritizedChunkTask(
                int priority,
                long sequence,
                ChunkSupplier supplier,
                CompletableFuture<GamePacket.ChunkData> future,
                Runnable cleanup,
                AtomicLong completedChunks,
                AtomicLong failedChunks
        ) {
            this.priority = priority;
            this.sequence = sequence;
            this.supplier = supplier;
            this.future = future;
            this.cleanup = cleanup;
            this.completedChunks = completedChunks;
            this.failedChunks = failedChunks;
        }

        @Override
        public void run() {
            try {
                GamePacket.ChunkData packet = supplier.get();
                cleanup.run();
                completedChunks.incrementAndGet();
                future.complete(packet);
            } catch (Throwable error) {
                cleanup.run();
                failedChunks.incrementAndGet();
                future.completeExceptionally(error);
            }
        }

        @Override
        public int compareTo(PrioritizedChunkTask other) {
            int priorityComparison = Integer.compare(priority, other.priority);
            if (priorityComparison != 0) {
                return priorityComparison;
            }
            return Long.compare(sequence, other.sequence);
        }
    }

    @FunctionalInterface
    interface ChunkLoader {
        GamePacket.ChunkData load(ServerWorld world, ChunkPos pos);
    }

    @FunctionalInterface
    private interface ChunkSupplier {
        GamePacket.ChunkData get();
    }
}
