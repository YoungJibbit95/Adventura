package dev.voxelgame.client.net;

import dev.voxelgame.common.net.GamePacket;

import java.util.concurrent.atomic.AtomicLong;

public final class ClientNetworkStats {
    private final AtomicLong sentPackets = new AtomicLong();
    private final AtomicLong receivedPackets = new AtomicLong();
    private final AtomicLong chunkPackets = new AtomicLong();
    private final AtomicLong blockUpdatePackets = new AtomicLong();
    private final AtomicLong entitySnapshotPackets = new AtomicLong();
    private final AtomicLong inventoryPackets = new AtomicLong();
    private final AtomicLong storageOpenPackets = new AtomicLong();
    private final AtomicLong chatPackets = new AtomicLong();

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
        return new Snapshot(
                sentPackets.get(),
                receivedPackets.get(),
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
            long chunkPackets,
            long blockUpdatePackets,
            long entitySnapshotPackets,
            long inventoryPackets,
            long storageOpenPackets,
            long chatPackets
    ) {
        public static Snapshot offline() {
            return new Snapshot(0, 0, 0, 0, 0, 0, 0, 0);
        }
    }
}
