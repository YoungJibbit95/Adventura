package dev.voxelgame.common.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record ChunkStreamingRings(
        int simulationRadiusChunks,
        int renderRadiusChunks,
        int previewRadiusChunks,
        int retainRadiusChunks
) {
    public ChunkStreamingRings {
        simulationRadiusChunks = Math.max(0, simulationRadiusChunks);
        renderRadiusChunks = Math.max(simulationRadiusChunks, renderRadiusChunks);
        previewRadiusChunks = Math.max(renderRadiusChunks, previewRadiusChunks);
        retainRadiusChunks = Math.max(previewRadiusChunks, retainRadiusChunks);
    }

    public static ChunkStreamingRings client(int renderRadiusChunks, int previewRadiusChunks) {
        int render = Math.max(0, renderRadiusChunks);
        int preview = Math.max(render, previewRadiusChunks);
        int simulation = Math.min(render, 2);
        return new ChunkStreamingRings(simulation, render, preview, preview + 2);
    }

    public static ChunkStreamingRings subscription(int streamRadiusChunks) {
        int radius = Math.max(0, streamRadiusChunks);
        return new ChunkStreamingRings(Math.min(radius, 2), radius, radius, radius);
    }

    public boolean contains(ChunkPos center, ChunkPos pos, Ring ring) {
        return distance(center, pos) <= radius(ring);
    }

    public int radius(Ring ring) {
        return switch (ring == null ? Ring.RETAIN : ring) {
            case SIMULATION -> simulationRadiusChunks;
            case RENDER -> renderRadiusChunks;
            case PREVIEW -> previewRadiusChunks;
            case RETAIN -> retainRadiusChunks;
        };
    }

    public List<ChunkPos> positionsInRing(ChunkPos center, Ring ring) {
        int radius = radius(ring);
        List<ChunkPos> positions = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        for (int z = center.z() - radius; z <= center.z() + radius; z++) {
            for (int x = center.x() - radius; x <= center.x() + radius; x++) {
                positions.add(new ChunkPos(x, z));
            }
        }
        positions.sort(Comparator.comparingLong(pos -> distanceSquared(center, pos)));
        return positions;
    }

    public static int distance(ChunkPos center, ChunkPos pos) {
        return Math.max(Math.abs(pos.x() - center.x()), Math.abs(pos.z() - center.z()));
    }

    public static long distanceSquared(ChunkPos center, ChunkPos pos) {
        long dx = (long) pos.x() - center.x();
        long dz = (long) pos.z() - center.z();
        return dx * dx + dz * dz;
    }

    public enum Ring {
        SIMULATION,
        RENDER,
        PREVIEW,
        RETAIN
    }
}
