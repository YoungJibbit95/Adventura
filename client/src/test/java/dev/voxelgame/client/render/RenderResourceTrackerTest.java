package dev.voxelgame.client.render;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RenderResourceTrackerTest {
    @BeforeEach
    void resetBefore() {
        RenderResourceTracker.resetForTests();
    }

    @AfterEach
    void resetAfter() {
        RenderResourceTracker.resetForTests();
    }

    @Test
    void tracksLiveChunkMeshResources() {
        RenderResourceTracker.registerChunkMesh(128L);
        RenderResourceTracker.registerChunkMesh(256L);

        RenderResourceTracker.Snapshot live = RenderResourceTracker.snapshot();
        assertEquals(2, live.liveChunkMeshes());
        assertEquals(2, live.liveChunkVertexArrays());
        assertEquals(4, live.liveChunkBuffers());
        assertEquals(384L, live.liveChunkMeshBytes());
        assertEquals(384L, live.peakChunkMeshBytes());
        assertEquals(2L, live.createdChunkMeshes());
        assertEquals(0L, live.disposedChunkMeshes());

        RenderResourceTracker.releaseChunkMesh(128L);

        RenderResourceTracker.Snapshot afterRelease = RenderResourceTracker.snapshot();
        assertEquals(1, afterRelease.liveChunkMeshes());
        assertEquals(256L, afterRelease.liveChunkMeshBytes());
        assertEquals(384L, afterRelease.peakChunkMeshBytes());
        assertEquals(1L, afterRelease.disposedChunkMeshes());
    }

    @Test
    void rejectsUnmatchedRelease() {
        assertThrows(IllegalStateException.class, () -> RenderResourceTracker.releaseChunkMesh(64L));

        RenderResourceTracker.Snapshot snapshot = RenderResourceTracker.snapshot();
        assertEquals(0, snapshot.liveChunkMeshes());
        assertEquals(0L, snapshot.liveChunkMeshBytes());
    }

    @Test
    void tracksTextureShaderAndParticleResources() {
        RenderResourceTracker.registerTexture(512L);
        RenderResourceTracker.registerShaderProgram();
        RenderResourceTracker.registerParticleBuffers(1024L);
        RenderResourceTracker.registerEntityBuffers(2, 256L);
        RenderResourceTracker.registerFramebuffer();

        RenderResourceTracker.Snapshot live = RenderResourceTracker.snapshot();
        assertEquals(1, live.liveTextures());
        assertEquals(512L, live.liveTextureBytes());
        assertEquals(1, live.liveShaderPrograms());
        assertEquals(1, live.liveParticleVertexArrays());
        assertEquals(1, live.liveParticleBuffers());
        assertEquals(1024L, live.liveParticleBufferBytes());
        assertEquals(1, live.liveEntityVertexArrays());
        assertEquals(2, live.liveEntityBuffers());
        assertEquals(1, live.liveFramebuffers());

        RenderResourceTracker.releaseFramebuffer();
        RenderResourceTracker.releaseEntityBuffers(2, 256L);
        RenderResourceTracker.releaseParticleBuffers(1024L);
        RenderResourceTracker.releaseShaderProgram();
        RenderResourceTracker.releaseTexture(512L);

        RenderResourceTracker.Snapshot released = RenderResourceTracker.snapshot();
        assertEquals(0, released.liveTextures());
        assertEquals(0, released.liveShaderPrograms());
        assertEquals(0, released.liveParticleBuffers());
        assertEquals(0, released.liveEntityBuffers());
        assertEquals(0, released.liveFramebuffers());
        assertEquals(1L, released.disposedTextures());
        assertEquals(1L, released.disposedShaderPrograms());
    }
}
