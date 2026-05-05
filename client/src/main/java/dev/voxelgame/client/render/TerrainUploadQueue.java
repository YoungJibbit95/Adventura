package dev.voxelgame.client.render;

import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkStreamingRings;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

final class TerrainUploadQueue {
    private final LinkedHashMap<ChunkPos, ClientWorld.LayeredMeshBuild> pendingByPosition = new LinkedHashMap<>();

    boolean isEmpty() {
        return pendingByPosition.isEmpty();
    }

    int pendingBuilds() {
        return pendingByPosition.size();
    }

    long pendingBytes() {
        long bytes = 0L;
        for (ClientWorld.LayeredMeshBuild build : pendingByPosition.values()) {
            bytes += uploadBytes(build);
        }
        return bytes;
    }

    void clear() {
        pendingByPosition.clear();
    }

    void stage(Collection<ClientWorld.LayeredMeshBuild> builds, Vector3f priorityPosition) {
        Objects.requireNonNull(builds, "builds");
        if (builds.isEmpty()) {
            return;
        }
        for (ClientWorld.LayeredMeshBuild build : builds) {
            Objects.requireNonNull(build, "build");
            pendingByPosition.remove(build.pos());
            pendingByPosition.put(build.pos(), build);
        }
        sortByPriority(priorityPosition);
    }

    int removePositions(Collection<ChunkPos> positions) {
        Objects.requireNonNull(positions, "positions");
        int removed = 0;
        for (ChunkPos pos : positions) {
            if (pendingByPosition.remove(pos) != null) {
                removed++;
            }
        }
        return removed;
    }

    UploadResult drain(double maxUploadMilliseconds, LayeredMeshUploader uploader, LongSupplier nanoTime) {
        Objects.requireNonNull(uploader, "uploader");
        Objects.requireNonNull(nanoTime, "nanoTime");
        long startNanos = nanoTime.getAsLong();
        long budgetNanos = uploadBudgetNanos(maxUploadMilliseconds);
        int uploaded = 0;
        long uploadedBytes = 0L;
        while (!pendingByPosition.isEmpty()) {
            if (uploaded > 0 && nanoTime.getAsLong() - startNanos >= budgetNanos) {
                break;
            }
            ClientWorld.LayeredMeshBuild build = removeFirst();
            uploader.upload(build);
            uploadedBytes += uploadBytes(build);
            uploaded++;
        }
        double elapsedMilliseconds = uploaded == 0 ? 0.0 : Math.max(0L, nanoTime.getAsLong() - startNanos) / 1_000_000.0;
        return new UploadResult(uploaded, uploadedBytes, pendingBuilds(), pendingBytes(), elapsedMilliseconds);
    }

    private ClientWorld.LayeredMeshBuild removeFirst() {
        Map.Entry<ChunkPos, ClientWorld.LayeredMeshBuild> first = pendingByPosition.entrySet().iterator().next();
        ClientWorld.LayeredMeshBuild build = first.getValue();
        pendingByPosition.remove(first.getKey());
        return build;
    }

    private void sortByPriority(Vector3f priorityPosition) {
        if (priorityPosition == null || pendingByPosition.size() <= 1) {
            return;
        }
        ChunkPos cameraChunk = ChunkPos.fromBlock(
                (int) Math.floor(priorityPosition.x),
                (int) Math.floor(priorityPosition.z)
        );
        List<ClientWorld.LayeredMeshBuild> sortedBuilds = new ArrayList<>(pendingByPosition.values());
        sortedBuilds.sort((left, right) -> {
            int byDistance = Long.compare(
                    ChunkStreamingRings.distanceSquared(cameraChunk, left.pos()),
                    ChunkStreamingRings.distanceSquared(cameraChunk, right.pos())
            );
            if (byDistance != 0) {
                return byDistance;
            }
            int byX = Integer.compare(left.pos().x(), right.pos().x());
            if (byX != 0) {
                return byX;
            }
            return Integer.compare(left.pos().z(), right.pos().z());
        });
        pendingByPosition.clear();
        for (ClientWorld.LayeredMeshBuild build : sortedBuilds) {
            pendingByPosition.put(build.pos(), build);
        }
    }

    static long uploadBytes(ClientWorld.LayeredMeshBuild build) {
        Objects.requireNonNull(build, "build");
        long bytes = 0L;
        if (!build.opaqueMesh().isEmpty()) {
            bytes += build.opaqueMesh().estimatedBytes();
        }
        if (!build.cutoutMesh().isEmpty()) {
            bytes += build.cutoutMesh().estimatedBytes();
        }
        if (!build.transparentMesh().isEmpty()) {
            bytes += build.transparentMesh().estimatedBytes();
        }
        return bytes;
    }

    private static long uploadBudgetNanos(double maxUploadMilliseconds) {
        if (!Double.isFinite(maxUploadMilliseconds) || maxUploadMilliseconds <= 0.0) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, (long) (maxUploadMilliseconds * 1_000_000.0));
    }

    @FunctionalInterface
    interface LayeredMeshUploader {
        void upload(ClientWorld.LayeredMeshBuild build);
    }

    record UploadResult(
            int uploadedBuilds,
            long uploadedBytes,
            int pendingBuilds,
            long pendingBytes,
            double elapsedMilliseconds
    ) {
        UploadResult {
            uploadedBuilds = Math.max(0, uploadedBuilds);
            uploadedBytes = Math.max(0L, uploadedBytes);
            pendingBuilds = Math.max(0, pendingBuilds);
            pendingBytes = Math.max(0L, pendingBytes);
            elapsedMilliseconds = Math.max(0.0, elapsedMilliseconds);
        }
    }
}
