package dev.voxelgame.client.net;

import dev.voxelgame.common.net.GamePacket;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

public final class ClientNetworkStats {
    private final LongSupplier nanoTimeSource;
    private final long startNanos;
    private final AtomicLong sentPackets = new AtomicLong();
    private final AtomicLong receivedPackets = new AtomicLong();
    private final AtomicLong chunkPackets = new AtomicLong();
    private final AtomicLong blockUpdatePackets = new AtomicLong();
    private final AtomicLong entitySnapshotPackets = new AtomicLong();
    private final AtomicLong inventoryPackets = new AtomicLong();
    private final AtomicLong storageOpenPackets = new AtomicLong();
    private final AtomicLong chatPackets = new AtomicLong();

    public ClientNetworkStats() {
        this(System::nanoTime);
    }

    ClientNetworkStats(LongSupplier nanoTimeSource) {
        this.nanoTimeSource = nanoTimeSource;
        this.startNanos = nanoTimeSource.getAsLong();
    }

    public void recordSent(GamePacket packet) {
        if (packet != null) {
            sentPackets.incrementAndGet();
        }
    }

    public void recordReceived(GamePacket packet) {
        if (packet == null) {
            return;
        }
        receivedPackets.incrementAndGet();
        switch (packet) {
            case GamePacket.ChunkData ignored -> chunkPackets.incrementAndGet();
            case GamePacket.BlockUpdate ignored -> blockUpdatePackets.incrementAndGet();
            case GamePacket.EntitySnapshots ignored -> entitySnapshotPackets.incrementAndGet();
            case GamePacket.InventorySnapshot ignored -> inventoryPackets.incrementAndGet();
            case GamePacket.StorageOpen ignored -> storageOpenPackets.incrementAndGet();
            case GamePacket.Chat ignored -> chatPackets.incrementAndGet();
            default -> {
            }
        }
    }

    public Snapshot snapshot() {
        double elapsedSeconds = Math.max(1e-9, (nanoTimeSource.getAsLong() - startNanos) / 1_000_000_000.0);
        long sent = sentPackets.get();
        long received = receivedPackets.get();
        return new Snapshot(
                sent,
                received,
                sent / elapsedSeconds,
                received / elapsedSeconds,
                chunkPackets.get(),
                blockUpdatePackets.get(),
                entitySnapshotPackets.get(),
                inventoryPackets.get(),
                storageOpenPackets.get(),
                chatPackets.get()
        );
    }

    public record Snapshot(
            long sentPackets,
            long receivedPackets,
            double sentPacketsPerSecond,
            double receivedPacketsPerSecond,
            long chunkPackets,
            long blockUpdatePackets,
            long entitySnapshotPackets,
            long inventoryPackets,
            long storageOpenPackets,
            long chatPackets
    ) {
        public static Snapshot offline() {
            return new Snapshot(0, 0, 0.0, 0.0, 0, 0, 0, 0, 0, 0);
        }
    }
}
