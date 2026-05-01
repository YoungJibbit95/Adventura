package dev.voxelgame.common.physics;

import dev.voxelgame.common.world.ChunkPos;

import java.util.Collection;
import java.util.Objects;

public final class PhysicsTickets {
    public static final int PLAYER_SIMULATION_RADIUS_CHUNKS = 8;
    public static final int CLIENT_LOADING_BARRIER_RADIUS_CHUNKS = 0;

    private PhysicsTickets() {
    }

    public static ChunkPos chunkFor(double x, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Physics ticket position must be finite");
        }
        return ChunkPos.fromBlock((int) Math.floor(x), (int) Math.floor(z));
    }

    public static int chebyshevDistance(ChunkPos first, ChunkPos second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return Math.max(Math.abs(first.x() - second.x()), Math.abs(first.z() - second.z()));
    }

    public static boolean insideTicket(ChunkPos chunk, ChunkPos center, int radiusChunks) {
        if (radiusChunks < 0) {
            return false;
        }
        return chebyshevDistance(chunk, center) <= radiusChunks;
    }

    public static boolean insideAnyTicket(ChunkPos chunk, Collection<ChunkPos> centers, int radiusChunks) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(centers, "centers");
        for (ChunkPos center : centers) {
            if (center != null && insideTicket(chunk, center, radiusChunks)) {
                return true;
            }
        }
        return false;
    }

    public static int requiredChunkCount(int radiusChunks) {
        if (radiusChunks < 0) {
            return 0;
        }
        int diameter = radiusChunks * 2 + 1;
        return diameter * diameter;
    }
}
