package dev.voxelgame.client.render;

public final class RenderResourceTracker {
    private static int liveChunkMeshes;
    private static int liveChunkVertexArrays;
    private static int liveChunkBuffers;
    private static long liveChunkMeshBytes;
    private static long peakChunkMeshBytes;
    private static long createdChunkMeshes;
    private static long disposedChunkMeshes;

    private RenderResourceTracker() {
    }

    static synchronized void registerChunkMesh(long estimatedBytes) {
        liveChunkMeshes++;
        liveChunkVertexArrays++;
        liveChunkBuffers += 2;
        liveChunkMeshBytes += estimatedBytes;
        peakChunkMeshBytes = Math.max(peakChunkMeshBytes, liveChunkMeshBytes);
        createdChunkMeshes++;
    }

    static synchronized void releaseChunkMesh(long estimatedBytes) {
        if (liveChunkMeshes <= 0 || liveChunkVertexArrays <= 0 || liveChunkBuffers < 2 || liveChunkMeshBytes < estimatedBytes) {
            throw new IllegalStateException("Chunk mesh resource tracking underflow");
        }
        liveChunkMeshes--;
        liveChunkVertexArrays--;
        liveChunkBuffers -= 2;
        liveChunkMeshBytes -= estimatedBytes;
        disposedChunkMeshes++;
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(
                liveChunkMeshes,
                liveChunkVertexArrays,
                liveChunkBuffers,
                liveChunkMeshBytes,
                peakChunkMeshBytes,
                createdChunkMeshes,
                disposedChunkMeshes
        );
    }

    static synchronized void resetForTests() {
        liveChunkMeshes = 0;
        liveChunkVertexArrays = 0;
        liveChunkBuffers = 0;
        liveChunkMeshBytes = 0L;
        peakChunkMeshBytes = 0L;
        createdChunkMeshes = 0L;
        disposedChunkMeshes = 0L;
    }

    public record Snapshot(
            int liveChunkMeshes,
            int liveChunkVertexArrays,
            int liveChunkBuffers,
            long liveChunkMeshBytes,
            long peakChunkMeshBytes,
            long createdChunkMeshes,
            long disposedChunkMeshes
    ) {
    }
}
