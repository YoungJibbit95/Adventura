package dev.voxelgame.client.render;

public final class RenderResourceTracker {
    private static int liveChunkMeshes;
    private static int liveChunkVertexArrays;
    private static int liveChunkBuffers;
    private static long liveChunkMeshBytes;
    private static long peakChunkMeshBytes;
    private static long createdChunkMeshes;
    private static long disposedChunkMeshes;
    private static int liveTextures;
    private static long liveTextureBytes;
    private static long peakTextureBytes;
    private static long createdTextures;
    private static long disposedTextures;
    private static int liveShaderPrograms;
    private static long createdShaderPrograms;
    private static long disposedShaderPrograms;
    private static long shaderReloadCount;
    private static long failedShaderReloadCount;
    private static double lastShaderReloadMilliseconds;
    private static int liveParticleVertexArrays;
    private static int liveParticleBuffers;
    private static long liveParticleBufferBytes;
    private static int liveEntityVertexArrays;
    private static int liveEntityBuffers;
    private static long liveEntityBufferBytes;
    private static int liveFramebuffers;

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

    public static synchronized void registerTexture(long estimatedBytes) {
        long bytes = Math.max(0L, estimatedBytes);
        liveTextures++;
        liveTextureBytes += bytes;
        peakTextureBytes = Math.max(peakTextureBytes, liveTextureBytes);
        createdTextures++;
    }

    public static synchronized void releaseTexture(long estimatedBytes) {
        long bytes = Math.max(0L, estimatedBytes);
        if (liveTextures <= 0 || liveTextureBytes < bytes) {
            throw new IllegalStateException("Texture resource tracking underflow");
        }
        liveTextures--;
        liveTextureBytes -= bytes;
        disposedTextures++;
    }

    public static synchronized void registerShaderProgram() {
        liveShaderPrograms++;
        createdShaderPrograms++;
    }

    public static synchronized void releaseShaderProgram() {
        if (liveShaderPrograms <= 0) {
            throw new IllegalStateException("Shader program resource tracking underflow");
        }
        liveShaderPrograms--;
        disposedShaderPrograms++;
    }

    static synchronized void recordShaderReload(double elapsedMilliseconds, int failedPrograms) {
        shaderReloadCount++;
        failedShaderReloadCount += Math.max(0, failedPrograms);
        lastShaderReloadMilliseconds = !Double.isFinite(elapsedMilliseconds) || elapsedMilliseconds < 0.0
                ? 0.0
                : elapsedMilliseconds;
    }

    public static synchronized void registerParticleBuffers(long estimatedBytes) {
        liveParticleVertexArrays++;
        liveParticleBuffers++;
        liveParticleBufferBytes += Math.max(0L, estimatedBytes);
    }

    public static synchronized void releaseParticleBuffers(long estimatedBytes) {
        long bytes = Math.max(0L, estimatedBytes);
        if (liveParticleVertexArrays <= 0 || liveParticleBuffers <= 0 || liveParticleBufferBytes < bytes) {
            throw new IllegalStateException("Particle buffer resource tracking underflow");
        }
        liveParticleVertexArrays--;
        liveParticleBuffers--;
        liveParticleBufferBytes -= bytes;
    }

    public static synchronized void registerEntityBuffers(int bufferCount, long estimatedBytes) {
        int buffers = Math.max(0, bufferCount);
        liveEntityVertexArrays++;
        liveEntityBuffers += buffers;
        liveEntityBufferBytes += Math.max(0L, estimatedBytes);
    }

    public static synchronized void releaseEntityBuffers(int bufferCount, long estimatedBytes) {
        int buffers = Math.max(0, bufferCount);
        long bytes = Math.max(0L, estimatedBytes);
        if (liveEntityVertexArrays <= 0 || liveEntityBuffers < buffers || liveEntityBufferBytes < bytes) {
            throw new IllegalStateException("Entity buffer resource tracking underflow");
        }
        liveEntityVertexArrays--;
        liveEntityBuffers -= buffers;
        liveEntityBufferBytes -= bytes;
    }

    public static synchronized void registerFramebuffer() {
        liveFramebuffers++;
    }

    public static synchronized void releaseFramebuffer() {
        if (liveFramebuffers <= 0) {
            throw new IllegalStateException("Framebuffer resource tracking underflow");
        }
        liveFramebuffers--;
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(
                liveChunkMeshes,
                liveChunkVertexArrays,
                liveChunkBuffers,
                liveChunkMeshBytes,
                peakChunkMeshBytes,
                createdChunkMeshes,
                disposedChunkMeshes,
                liveTextures,
                liveTextureBytes,
                peakTextureBytes,
                createdTextures,
                disposedTextures,
                liveShaderPrograms,
                createdShaderPrograms,
                disposedShaderPrograms,
                shaderReloadCount,
                failedShaderReloadCount,
                lastShaderReloadMilliseconds,
                liveParticleVertexArrays,
                liveParticleBuffers,
                liveParticleBufferBytes,
                liveEntityVertexArrays,
                liveEntityBuffers,
                liveEntityBufferBytes,
                liveFramebuffers
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
        liveTextures = 0;
        liveTextureBytes = 0L;
        peakTextureBytes = 0L;
        createdTextures = 0L;
        disposedTextures = 0L;
        liveShaderPrograms = 0;
        createdShaderPrograms = 0L;
        disposedShaderPrograms = 0L;
        shaderReloadCount = 0L;
        failedShaderReloadCount = 0L;
        lastShaderReloadMilliseconds = 0.0;
        liveParticleVertexArrays = 0;
        liveParticleBuffers = 0;
        liveParticleBufferBytes = 0L;
        liveEntityVertexArrays = 0;
        liveEntityBuffers = 0;
        liveEntityBufferBytes = 0L;
        liveFramebuffers = 0;
    }

    public record Snapshot(
            int liveChunkMeshes,
            int liveChunkVertexArrays,
            int liveChunkBuffers,
            long liveChunkMeshBytes,
            long peakChunkMeshBytes,
            long createdChunkMeshes,
            long disposedChunkMeshes,
            int liveTextures,
            long liveTextureBytes,
            long peakTextureBytes,
            long createdTextures,
            long disposedTextures,
            int liveShaderPrograms,
            long createdShaderPrograms,
            long disposedShaderPrograms,
            long shaderReloadCount,
            long failedShaderReloadCount,
            double lastShaderReloadMilliseconds,
            int liveParticleVertexArrays,
            int liveParticleBuffers,
            long liveParticleBufferBytes,
            int liveEntityVertexArrays,
            int liveEntityBuffers,
            long liveEntityBufferBytes,
            int liveFramebuffers
    ) {
        public static Snapshot empty() {
            return new Snapshot(0, 0, 0, 0L, 0L, 0L, 0L, 0, 0L, 0L, 0L, 0L, 0, 0L, 0L, 0L, 0L, 0.0, 0, 0, 0L, 0, 0, 0L, 0);
        }
    }
}
