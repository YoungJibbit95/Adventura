package dev.voxelgame.client.world;

import dev.voxelgame.common.engine.EngineJobPriority;
import dev.voxelgame.common.world.ChunkPos;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class ChunkBuildQueue {
    private final Map<ChunkPos, Entry> entries = new LinkedHashMap<>();
    private long nextSequence;
    private long enqueuedBuilds;
    private long replacedBuilds;
    private long canceledBuilds;
    private long completedBuilds;
    private long completedGenerationJobs;
    private long completedLightingJobs;
    private final long createdNanos = System.nanoTime();
    private double averageWaitMilliseconds;
    private double lastMeshingMilliseconds;
    private double averageMeshingMilliseconds;
    private long lastMeshBufferGrowthBytes;
    private double averageMeshBufferGrowthBytes;
    private long retainedMeshBufferBytes;
    private double lastGenerationMilliseconds;
    private double averageGenerationMilliseconds;
    private double lastLightingMilliseconds;
    private double averageLightingMilliseconds;
    private double lastGpuUploadMilliseconds;
    private double averageGpuUploadMilliseconds;

    void enqueue(ChunkPos pos) {
        enqueue(pos, false);
    }

    void enqueueUrgent(ChunkPos pos) {
        enqueue(pos, true);
    }

    private void enqueue(ChunkPos pos, boolean urgent) {
        if (pos == null) {
            return;
        }
        long now = System.nanoTime();
        Entry previous = entries.remove(pos);
        boolean keepUrgent = urgent || previous != null && previous.urgent();
        if (previous != null) {
            replacedBuilds++;
        } else {
            enqueuedBuilds++;
        }
        entries.put(pos, new Entry(pos, now, nextSequence++, keepUrgent));
    }

    BuildRequest poll(Vector3f priorityPosition, int renderDistanceChunks, int previewRadiusChunks) {
        if (entries.isEmpty()) {
            return null;
        }
        long now = System.nanoTime();
        Comparator<Entry> priorityComparator = Comparator
                .comparingInt((Entry entry) -> effectivePriority(entry, priorityPosition, renderDistanceChunks, previewRadiusChunks).sortOrder())
                .thenComparingDouble((Entry entry) -> priorityScore(entry.pos(), priorityPosition, renderDistanceChunks, previewRadiusChunks))
                .thenComparingLong(Entry::sequence);
        Optional<Entry> selected = entries.values()
                .stream()
                .min(priorityComparator);
        if (selected.isEmpty()) {
            return null;
        }
        Entry entry = selected.get();
        entries.remove(entry.pos());
        return new BuildRequest(entry.pos(), Math.max(0.0, (now - entry.enqueuedNanos()) / 1_000_000.0));
    }

    void cancel(BuildRequest request) {
        if (request != null) {
            canceledBuilds++;
        }
    }

    void cancelIf(Predicate<ChunkPos> predicate) {
        if (predicate == null || entries.isEmpty()) {
            return;
        }
        var iterator = entries.keySet().iterator();
        while (iterator.hasNext()) {
            ChunkPos pos = iterator.next();
            if (predicate.test(pos)) {
                iterator.remove();
                canceledBuilds++;
            }
        }
    }

    void complete(BuildRequest request, double meshingMilliseconds) {
        complete(request, meshingMilliseconds, 0L, 0L);
    }

    void complete(BuildRequest request, double meshingMilliseconds, long meshBufferGrowthBytes, long retainedMeshBufferBytes) {
        if (request == null) {
            return;
        }
        completedBuilds++;
        averageWaitMilliseconds = smooth(averageWaitMilliseconds, request.waitMilliseconds(), completedBuilds);
        lastMeshingMilliseconds = nonNegative(meshingMilliseconds);
        averageMeshingMilliseconds = smooth(averageMeshingMilliseconds, lastMeshingMilliseconds, completedBuilds);
        lastMeshBufferGrowthBytes = Math.max(0L, meshBufferGrowthBytes);
        averageMeshBufferGrowthBytes = smooth(averageMeshBufferGrowthBytes, lastMeshBufferGrowthBytes, completedBuilds);
        this.retainedMeshBufferBytes = Math.max(0L, retainedMeshBufferBytes);
    }

    void recordGeneration(double milliseconds) {
        completedGenerationJobs++;
        lastGenerationMilliseconds = nonNegative(milliseconds);
        averageGenerationMilliseconds = smooth(averageGenerationMilliseconds, lastGenerationMilliseconds, completedGenerationJobs);
    }

    void recordLighting(double milliseconds) {
        completedLightingJobs++;
        lastLightingMilliseconds = nonNegative(milliseconds);
        averageLightingMilliseconds = smooth(averageLightingMilliseconds, lastLightingMilliseconds, completedLightingJobs);
    }

    void recordGpuUpload(double milliseconds) {
        lastGpuUploadMilliseconds = nonNegative(milliseconds);
        averageGpuUploadMilliseconds = smooth(averageGpuUploadMilliseconds, lastGpuUploadMilliseconds, completedBuilds);
    }

    int size() {
        return entries.size();
    }

    Snapshot snapshot() {
        double elapsedSeconds = Math.max(1e-9, (System.nanoTime() - createdNanos) / 1_000_000_000.0);
        return new Snapshot(
                entries.size(),
                enqueuedBuilds,
                replacedBuilds,
                canceledBuilds,
                completedBuilds,
                completedGenerationJobs,
                completedLightingJobs,
                completedBuilds / elapsedSeconds,
                averageWaitMilliseconds,
                lastGenerationMilliseconds,
                averageGenerationMilliseconds,
                lastMeshingMilliseconds,
                averageMeshingMilliseconds,
                lastMeshBufferGrowthBytes,
                averageMeshBufferGrowthBytes,
                retainedMeshBufferBytes,
                lastLightingMilliseconds,
                averageLightingMilliseconds,
                lastGpuUploadMilliseconds,
                averageGpuUploadMilliseconds
        );
    }

    private static double priorityScore(ChunkPos pos, Vector3f priorityPosition, int renderDistanceChunks, int previewRadiusChunks) {
        EngineJobPriority priority = distancePriority(pos, priorityPosition, renderDistanceChunks, previewRadiusChunks);
        return priority.sortOrder() * 1_000_000_000.0 + distanceSquaredToChunkCenter(pos, priorityPosition);
    }

    private static EngineJobPriority effectivePriority(Entry entry, Vector3f priorityPosition, int renderDistanceChunks, int previewRadiusChunks) {
        if (entry.urgent()) {
            return EngineJobPriority.PLAYER_ACTION;
        }
        return distancePriority(entry.pos(), priorityPosition, renderDistanceChunks, previewRadiusChunks);
    }

    private static EngineJobPriority distancePriority(ChunkPos pos, Vector3f priorityPosition, int renderDistanceChunks, int previewRadiusChunks) {
        if (priorityPosition == null) {
            return EngineJobPriority.VISIBLE_CHUNK;
        }
        ChunkPos center = ChunkPos.fromBlock((int) Math.floor(priorityPosition.x), (int) Math.floor(priorityPosition.z));
        int chunkDistance = Math.max(Math.abs(pos.x() - center.x()), Math.abs(pos.z() - center.z()));
        if (chunkDistance <= Math.max(0, renderDistanceChunks)) {
            return EngineJobPriority.VISIBLE_CHUNK;
        }
        if (chunkDistance <= Math.max(renderDistanceChunks, previewRadiusChunks)) {
            return EngineJobPriority.PREVIEW;
        }
        return EngineJobPriority.BACKGROUND;
    }

    private static double distanceSquaredToChunkCenter(ChunkPos pos, Vector3f priorityPosition) {
        if (priorityPosition == null) {
            return 0.0;
        }
        double centerX = pos.x() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5;
        double centerZ = pos.z() * ChunkPos.SIZE + ChunkPos.SIZE * 0.5;
        double dx = centerX - priorityPosition.x;
        double dz = centerZ - priorityPosition.z;
        return dx * dx + dz * dz;
    }

    private static double smooth(double current, double sample, long count) {
        if (count <= 1L || current == 0.0) {
            return sample;
        }
        return current * 0.85 + sample * 0.15;
    }

    private static double nonNegative(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            return 0.0;
        }
        return value;
    }

    record BuildRequest(ChunkPos pos, double waitMilliseconds) {
    }

    private record Entry(ChunkPos pos, long enqueuedNanos, long sequence, boolean urgent) {
    }

    public record Snapshot(
            int queuedBuilds,
            long enqueuedBuilds,
            long replacedBuilds,
            long canceledBuilds,
            long completedBuilds,
            long completedGenerationJobs,
            long completedLightingJobs,
            double chunksBuiltPerSecond,
            double averageWaitMilliseconds,
            double lastGenerationMilliseconds,
            double averageGenerationMilliseconds,
            double lastMeshingMilliseconds,
            double averageMeshingMilliseconds,
            long lastMeshBufferGrowthBytes,
            double averageMeshBufferGrowthBytes,
            long retainedMeshBufferBytes,
            double lastLightingMilliseconds,
            double averageLightingMilliseconds,
            double lastGpuUploadMilliseconds,
            double averageGpuUploadMilliseconds
    ) {
        public static Snapshot empty() {
            return new Snapshot(0, 0L, 0L, 0L, 0L, 0L, 0L, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0, 0L, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
