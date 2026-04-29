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
}
