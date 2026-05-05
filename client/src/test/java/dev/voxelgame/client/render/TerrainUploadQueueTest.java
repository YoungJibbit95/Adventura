package dev.voxelgame.client.render;

import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.world.ChunkPos;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainUploadQueueTest {
    @Test
    void stagesUploadsByCameraPriority() {
        TerrainUploadQueue queue = new TerrainUploadQueue();
        ClientWorld.LayeredMeshBuild far = build(new ChunkPos(5, 0), mesh(6));
        ClientWorld.LayeredMeshBuild near = build(new ChunkPos(1, 0), mesh(6));
        List<ChunkPos> uploaded = new ArrayList<>();

        queue.stage(List.of(far, near), new Vector3f(8.0f, 80.0f, 8.0f));
        TerrainUploadQueue.UploadResult result = queue.drain(
                Double.POSITIVE_INFINITY,
                build -> uploaded.add(build.pos()),
                () -> 0L
        );

        assertEquals(List.of(new ChunkPos(1, 0), new ChunkPos(5, 0)), uploaded);
        assertEquals(2, result.uploadedBuilds());
        assertEquals(0, result.pendingBuilds());
    }

    @Test
    void uploadBudgetAlwaysUploadsOneThenKeepsBacklog() {
        TerrainUploadQueue queue = new TerrainUploadQueue();
        queue.stage(List.of(
                build(new ChunkPos(0, 0), mesh(6)),
                build(new ChunkPos(1, 0), mesh(6)),
                build(new ChunkPos(2, 0), mesh(6))
        ), new Vector3f(8.0f, 80.0f, 8.0f));
        List<ChunkPos> uploaded = new ArrayList<>();
        AtomicInteger timeCalls = new AtomicInteger();
        long[] times = {0L, 2_000_000L, 2_000_000L};
        LongSupplier nanoTime = () -> times[Math.min(timeCalls.getAndIncrement(), times.length - 1)];

        TerrainUploadQueue.UploadResult result = queue.drain(1.0, build -> uploaded.add(build.pos()), nanoTime);

        assertEquals(List.of(new ChunkPos(0, 0)), uploaded);
        assertEquals(1, result.uploadedBuilds());
        assertEquals(2, result.pendingBuilds());
        assertTrue(result.pendingBytes() > 0L);
    }

    @Test
    void stagingSameChunkReplacesOlderPendingBuild() {
        TerrainUploadQueue queue = new TerrainUploadQueue();
        ClientWorld.LayeredMeshBuild first = build(new ChunkPos(0, 0), mesh(3));
        ClientWorld.LayeredMeshBuild replacement = build(new ChunkPos(0, 0), mesh(12));
        List<ClientWorld.LayeredMeshBuild> uploaded = new ArrayList<>();

        queue.stage(List.of(first), null);
        queue.stage(List.of(replacement), null);
        TerrainUploadQueue.UploadResult result = queue.drain(Double.POSITIVE_INFINITY, uploaded::add, () -> 0L);

        assertEquals(List.of(replacement), uploaded);
        assertEquals(1, result.uploadedBuilds());
        assertEquals(TerrainUploadQueue.uploadBytes(replacement), result.uploadedBytes());
    }

    @Test
    void removingPositionsCancelsPendingUploadsBeforeGpuWork() {
        TerrainUploadQueue queue = new TerrainUploadQueue();
        ClientWorld.LayeredMeshBuild keep = build(new ChunkPos(0, 0), mesh(6));
        ClientWorld.LayeredMeshBuild cancel = build(new ChunkPos(4, 0), mesh(6));
        List<ChunkPos> uploaded = new ArrayList<>();

        queue.stage(List.of(keep, cancel), null);
        int removed = queue.removePositions(List.of(cancel.pos()));
        TerrainUploadQueue.UploadResult result = queue.drain(Double.POSITIVE_INFINITY, build -> uploaded.add(build.pos()), () -> 0L);

        assertEquals(1, removed);
        assertEquals(List.of(keep.pos()), uploaded);
        assertEquals(1, result.uploadedBuilds());
        assertEquals(0, result.pendingBuilds());
    }

    private static ClientWorld.LayeredMeshBuild build(ChunkPos pos, ChunkMesh opaque) {
        return new ClientWorld.LayeredMeshBuild(pos, opaque, new ChunkMesh(null, null), new ChunkMesh(null, null));
    }

    private static ChunkMesh mesh(int indices) {
        int indexCount = Math.max(0, indices);
        int[] indexData = new int[indexCount];
        for (int i = 0; i < indexCount; i++) {
            indexData[i] = i % 4;
        }
        return new ChunkMesh(new float[ChunkMesher.FLOATS_PER_VERTEX * 4], indexData);
    }
}
