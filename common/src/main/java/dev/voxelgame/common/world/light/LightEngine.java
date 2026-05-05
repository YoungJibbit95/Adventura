package dev.voxelgame.common.world.light;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkSection;
import dev.voxelgame.common.world.InMemoryWorld;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class LightEngine {
    private static final int[][] DIRECTIONS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };
    private static final int[][] HORIZONTAL_DIRECTIONS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}
    };
    private WorkStats lastWorkStats = WorkStats.empty();

    public WorkStats lastWorkStats() {
        return lastWorkStats;
    }

    public void rebuildChunkLighting(InMemoryWorld world, ChunkPos center) {
        world.getOrCreateChunk(center);
        rebuildChunkLighting(world, List.of(center));
    }

    public void rebuildChunkLighting(InMemoryWorld world, Collection<ChunkPos> centers) {
        if (centers == null || centers.isEmpty()) {
            return;
        }
        Set<ChunkPos> normalizedCenters = new HashSet<>();
        for (ChunkPos center : centers) {
            if (center == null) {
                continue;
            }
            world.getOrCreateChunk(center);
            normalizedCenters.add(center);
        }
        if (normalizedCenters.isEmpty()) {
            return;
        }
        WorkStatsBuilder stats = new WorkStatsBuilder();
        stats.requestedCenters = normalizedCenters.size();
        List<Chunk> affected = affectedChunks(world, normalizedCenters);
        stats.affectedChunks = affected.size();
        rebuildSkyLight(world, affected, stats);
        rebuildBlockLight(world, affected, stats);
        lastWorkStats = stats.build();
    }

    public void rebuildSkyLight(InMemoryWorld world, ChunkPos center) {
        WorkStatsBuilder stats = new WorkStatsBuilder();
        stats.requestedCenters = 1;
        List<Chunk> affected = affectedChunks(world, center);
        stats.affectedChunks = affected.size();
        rebuildSkyLight(world, affected, stats);
        lastWorkStats = stats.build();
    }

    public void rebuildSkyLightColumn(InMemoryWorld world, Chunk chunk) {
        List<Chunk> chunks = new ArrayList<>();
        chunks.add(chunk);
        WorkStatsBuilder stats = new WorkStatsBuilder();
        stats.requestedCenters = 1;
        stats.affectedChunks = 1;
        rebuildSkyLight(world, chunks, stats);
        lastWorkStats = stats.build();
    }

    public LightUpdateResult updateBlockLight(InMemoryWorld world, int x, int y, int z) {
        if (!world.dimension().containsY(y)) {
            return LightUpdateResult.fullRebuildFallback(Set.of());
        }
        Chunk currentChunk = world.findChunk(ChunkPos.fromBlock(x, z)).orElse(null);
        if (currentChunk == null) {
            return LightUpdateResult.fullRebuildFallback(Set.of());
        }
        LightSourceRegistry sources = LightSourceRegistry.fromBlocks(world.blocks());
        Queue<LightNode> addQueue = new ArrayDeque<>();
        Queue<LightNode> removeQueue = new ArrayDeque<>();
        Set<ChunkPos> affected = new HashSet<>();
        short blockId = currentChunk.blockId(x, y, z);
        int sourceLight = sources.lightValue(blockId);
        int previousLight = currentChunk.blockLight(x, y, z);

        if (sourceLight < previousLight) {
            setBlockLight(currentChunk, x, y, z, sourceLight, affected);
            removeQueue.add(new LightNode(x, y, z, previousLight));
        } else if (sourceLight > previousLight) {
            setBlockLight(currentChunk, x, y, z, sourceLight, affected);
            addQueue.add(new LightNode(x, y, z, sourceLight));
        } else if (sourceLight > 0) {
            addQueue.add(new LightNode(x, y, z, sourceLight));
        }

        processBlockLightRemoval(world, removeQueue, addQueue, affected);
        if (sourceLight > 0) {
            Chunk sourceChunk = world.findChunk(ChunkPos.fromBlock(x, z)).orElse(null);
            if (sourceChunk != null && sourceChunk.blockLight(x, y, z) < sourceLight) {
                setBlockLight(sourceChunk, x, y, z, sourceLight, affected);
                addQueue.add(new LightNode(x, y, z, sourceLight));
            }
        }
        WorkStatsBuilder stats = new WorkStatsBuilder();
        propagateBlockLight(world, addQueue, affected, stats);
        stats.affectedChunks = affected.size();
        lastWorkStats = stats.build();
        return LightUpdateResult.incremental(affected);
    }

    private void rebuildSkyLight(InMemoryWorld world, List<Chunk> chunks) {
        WorkStatsBuilder stats = new WorkStatsBuilder();
        stats.affectedChunks = chunks.size();
        rebuildSkyLight(world, chunks, stats);
        lastWorkStats = stats.build();
    }

    private void rebuildSkyLight(InMemoryWorld world, List<Chunk> chunks, WorkStatsBuilder stats) {
        Queue<LightNode> queue = new ArrayDeque<>();
        Set<ChunkPos> affectedPositions = new HashSet<>();
        for (Chunk chunk : chunks) {
            affectedPositions.add(chunk.pos());
            seedSkyLightColumns(world, chunk, stats);
        }
        seedSkyLightBoundaryNodes(world, chunks, affectedPositions, queue, stats);
        propagateSkyLight(world, affectedPositions, queue, stats);
    }

    private void seedSkyLightColumns(InMemoryWorld world, Chunk chunk, WorkStatsBuilder stats) {
        int baseX = chunk.pos().x() * ChunkPos.SIZE;
        int baseZ = chunk.pos().z() * ChunkPos.SIZE;
        for (int localZ = 0; localZ < ChunkPos.SIZE; localZ++) {
            for (int localX = 0; localX < ChunkPos.SIZE; localX++) {
                stats.skyColumnsSeeded++;
                int light = 15;
                int x = baseX + localX;
                int z = baseZ + localZ;
                for (int y = chunk.dimension().maxYExclusive() - 1; y >= chunk.dimension().minY(); y--) {
                    BlockType block = world.blockType(chunk.blockId(x, y, z));
                    chunk.setSkyLight(x, y, z, light);
                    stats.skyCellsSeeded++;
                    int outgoingLight = LightRules.outgoingSkyLight(block, light);
                    light = outgoingLight;
                }
            }
        }
    }

    private void seedSkyLightBoundaryNodes(
            InMemoryWorld world,
            List<Chunk> chunks,
            Set<ChunkPos> affectedPositions,
            Queue<LightNode> queue,
            WorkStatsBuilder stats
    ) {
        for (Chunk chunk : chunks) {
            int baseX = chunk.pos().x() * ChunkPos.SIZE;
            int baseZ = chunk.pos().z() * ChunkPos.SIZE;
            for (int localZ = 0; localZ < ChunkPos.SIZE; localZ++) {
                for (int localX = 0; localX < ChunkPos.SIZE; localX++) {
                    int x = baseX + localX;
                    int z = baseZ + localZ;
                    for (int y = chunk.dimension().maxYExclusive() - 1; y >= chunk.dimension().minY(); y--) {
                        int light = chunk.skyLight(x, y, z);
                        if (light <= 1) {
                            continue;
                        }
                        BlockType sourceBlock = world.blockType(chunk.blockId(x, y, z));
                        int outgoingLight = LightRules.outgoingSkyLight(sourceBlock, light);
                        if (outgoingLight <= 1) {
                            continue;
                        }
                        if (canImproveHorizontalSkyNeighbor(world, affectedPositions, x, y, z, outgoingLight - 1)) {
                            queue.add(new LightNode(x, y, z, outgoingLight));
                            stats.skyBoundarySeeds++;
                        }
                    }
                }
            }
        }
    }

    private boolean canImproveHorizontalSkyNeighbor(
            InMemoryWorld world,
            Set<ChunkPos> affectedPositions,
            int x,
            int y,
            int z,
            int nextLight
    ) {
        for (int[] direction : HORIZONTAL_DIRECTIONS) {
            int nx = x + direction[0];
            int nz = z + direction[2];
            Chunk targetChunk = world.findChunk(ChunkPos.fromBlock(nx, nz)).orElse(null);
            if (targetChunk == null || !affectedPositions.contains(targetChunk.pos())) {
                continue;
            }
            BlockType targetBlock = world.blockType(targetChunk.blockId(nx, y, nz));
            if (targetBlock.opaque()) {
                continue;
            }
            if (targetChunk.skyLight(nx, y, nz) < nextLight) {
                return true;
            }
        }
        return false;
    }

    private void propagateSkyLight(InMemoryWorld world, Set<ChunkPos> affectedPositions, Queue<LightNode> queue, WorkStatsBuilder stats) {
        while (!queue.isEmpty()) {
            LightNode node = queue.remove();
            stats.skyPropagationNodes++;
            int nextLight = node.light - 1;
            if (nextLight <= 0) {
                continue;
            }
            for (int[] direction : DIRECTIONS) {
                int nx = node.x + direction[0];
                int ny = node.y + direction[1];
                int nz = node.z + direction[2];
                if (!world.dimension().containsY(ny)) {
                    continue;
                }
                Chunk targetChunk = world.findChunk(ChunkPos.fromBlock(nx, nz)).orElse(null);
                if (targetChunk == null || !affectedPositions.contains(targetChunk.pos())) {
                    continue;
                }
                BlockType block = world.blockType(targetChunk.blockId(nx, ny, nz));
                if (block.opaque()) {
                    continue;
                }
                if (targetChunk.skyLight(nx, ny, nz) >= nextLight) {
                    continue;
                }
                targetChunk.setSkyLight(nx, ny, nz, nextLight);
                int outgoingLight = LightRules.outgoingSkyLight(block, nextLight);
                if (outgoingLight > 0) {
                    queue.add(new LightNode(nx, ny, nz, outgoingLight));
                }
            }
        }
    }

    public void rebuildBlockLight(InMemoryWorld world, ChunkPos center) {
        WorkStatsBuilder stats = new WorkStatsBuilder();
        stats.requestedCenters = 1;
        List<Chunk> affected = affectedChunks(world, center);
        stats.affectedChunks = affected.size();
        rebuildBlockLight(world, affected, stats);
        lastWorkStats = stats.build();
    }

    private void rebuildBlockLight(InMemoryWorld world, List<Chunk> affected) {
        WorkStatsBuilder stats = new WorkStatsBuilder();
        stats.affectedChunks = affected.size();
        rebuildBlockLight(world, affected, stats);
        lastWorkStats = stats.build();
    }

    private void rebuildBlockLight(InMemoryWorld world, List<Chunk> affected, WorkStatsBuilder stats) {
        Queue<LightNode> queue = new ArrayDeque<>();
        Set<ChunkPos> affectedPositions = new HashSet<>();
        LightSourceRegistry sources = LightSourceRegistry.fromBlocks(world.blocks());
        for (Chunk chunk : affected) {
            chunk.clearBlockLight();
            enqueueEmitters(sources, chunk, queue, affectedPositions, stats);
        }
        propagateBlockLight(world, queue, affectedPositions, loadedPositions(affected), stats);
    }

    private void processBlockLightRemoval(
            InMemoryWorld world,
            Queue<LightNode> removeQueue,
            Queue<LightNode> addQueue,
            Set<ChunkPos> affected
    ) {
        while (!removeQueue.isEmpty()) {
            LightNode node = removeQueue.remove();
            for (int[] direction : DIRECTIONS) {
                int nx = node.x + direction[0];
                int ny = node.y + direction[1];
                int nz = node.z + direction[2];
                if (!world.dimension().containsY(ny)) {
                    continue;
                }
                Chunk targetChunk = world.findChunk(ChunkPos.fromBlock(nx, nz)).orElse(null);
                if (targetChunk == null) {
                    continue;
                }
                BlockType block = world.blockType(targetChunk.blockId(nx, ny, nz));
                if (!LightRules.passesBlockLight(block)) {
                    continue;
                }
                int neighborLight = targetChunk.blockLight(nx, ny, nz);
                if (neighborLight == 0) {
                    continue;
                }
                if (neighborLight < node.light) {
                    setBlockLight(targetChunk, nx, ny, nz, 0, affected);
                    removeQueue.add(new LightNode(nx, ny, nz, neighborLight));
                } else {
                    addQueue.add(new LightNode(nx, ny, nz, neighborLight));
                }
            }
        }
    }

    private void propagateBlockLight(InMemoryWorld world, Queue<LightNode> queue, Set<ChunkPos> affected) {
        propagateBlockLight(world, queue, affected, (Set<ChunkPos>) null, new WorkStatsBuilder());
    }

    private void propagateBlockLight(InMemoryWorld world, Queue<LightNode> queue, Set<ChunkPos> affected, WorkStatsBuilder stats) {
        propagateBlockLight(world, queue, affected, (Set<ChunkPos>) null, stats);
    }

    private void propagateBlockLight(InMemoryWorld world, Queue<LightNode> queue, Set<ChunkPos> affected, ChunkPos centerLimit) {
        Set<ChunkPos> allowedPositions = centerLimit == null ? null : affectedPositionSet(world, centerLimit);
        propagateBlockLight(world, queue, affected, allowedPositions, new WorkStatsBuilder());
    }

    private void propagateBlockLight(InMemoryWorld world, Queue<LightNode> queue, Set<ChunkPos> affected, Set<ChunkPos> allowedPositions) {
        propagateBlockLight(world, queue, affected, allowedPositions, new WorkStatsBuilder());
    }

    private void propagateBlockLight(InMemoryWorld world, Queue<LightNode> queue, Set<ChunkPos> affected, Set<ChunkPos> allowedPositions, WorkStatsBuilder stats) {
        while (!queue.isEmpty()) {
            LightNode node = queue.remove();
            stats.blockPropagationNodes++;
            int nextLight = node.light - 1;
            if (nextLight <= 0) {
                continue;
            }
            for (int[] direction : DIRECTIONS) {
                int nx = node.x + direction[0];
                int ny = node.y + direction[1];
                int nz = node.z + direction[2];
                if (!world.dimension().containsY(ny)) {
                    continue;
                }
                Chunk targetChunk = world.findChunk(ChunkPos.fromBlock(nx, nz)).orElse(null);
                if (targetChunk == null || allowedPositions != null && !allowedPositions.contains(targetChunk.pos())) {
                    continue;
                }
                BlockType block = world.blockType(targetChunk.blockId(nx, ny, nz));
                if (!LightRules.passesBlockLight(block)) {
                    continue;
                }
                if (targetChunk.blockLight(nx, ny, nz) >= nextLight) {
                    continue;
                }
                setBlockLight(targetChunk, nx, ny, nz, nextLight, affected);
                queue.add(new LightNode(nx, ny, nz, nextLight));
            }
        }
    }

    private List<Chunk> affectedChunks(InMemoryWorld world, ChunkPos center) {
        return affectedChunks(world, Set.of(center));
    }

    private List<Chunk> affectedChunks(InMemoryWorld world, Set<ChunkPos> centers) {
        Map<ChunkPos, Chunk> chunks = new LinkedHashMap<>();
        for (ChunkPos center : centers) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    ChunkPos pos = new ChunkPos(center.x() + dx, center.z() + dz);
                    world.findChunk(pos).ifPresent(chunk -> chunks.putIfAbsent(pos, chunk));
                }
            }
        }
        return new ArrayList<>(chunks.values());
    }

    private Set<ChunkPos> affectedPositionSet(InMemoryWorld world, ChunkPos center) {
        return loadedPositions(affectedChunks(world, center));
    }

    private Set<ChunkPos> loadedPositions(List<Chunk> chunks) {
        Set<ChunkPos> positions = new HashSet<>();
        for (Chunk chunk : chunks) {
            positions.add(chunk.pos());
        }
        return positions;
    }

    private void enqueueEmitters(
            LightSourceRegistry sources,
            Chunk chunk,
            Queue<LightNode> queue,
            Set<ChunkPos> affected,
            WorkStatsBuilder stats
    ) {
        int baseX = chunk.pos().x() * ChunkPos.SIZE;
        int baseZ = chunk.pos().z() * ChunkPos.SIZE;
        for (int sectionIndex = 0; sectionIndex < chunk.sectionCount(); sectionIndex++) {
            ChunkSection section = chunk.sectionByIndex(sectionIndex);
            if (section.isEmpty()) {
                continue;
            }
            stats.blockEmitterSectionsScanned++;
            int sectionBaseY = section.sectionY() * ChunkSection.SIZE;
            for (int localY = 0; localY < ChunkSection.SIZE; localY++) {
                int y = sectionBaseY + localY;
                if (!chunk.dimension().containsY(y)) {
                    continue;
                }
                for (int localZ = 0; localZ < ChunkPos.SIZE; localZ++) {
                    int z = baseZ + localZ;
                    for (int localX = 0; localX < ChunkPos.SIZE; localX++) {
                        stats.blockEmitterBlocksVisited++;
                        short blockId = section.blockId(localX, localY, localZ);
                        int light = sources.lightValue(blockId);
                        if (light > 0) {
                            int x = baseX + localX;
                            setBlockLight(chunk, x, y, z, light, affected);
                            queue.add(new LightNode(x, y, z, light));
                            stats.blockEmittersSeeded++;
                        }
                    }
                }
            }
        }
    }

    private void setBlockLight(Chunk chunk, int x, int y, int z, int light, Set<ChunkPos> affected) {
        if (chunk.blockLight(x, y, z) == light) {
            return;
        }
        chunk.setBlockLight(x, y, z, light);
        affected.add(chunk.pos());
    }

    public record LightUpdateResult(boolean fullRebuildFallback, Set<ChunkPos> affectedChunks) {
        public LightUpdateResult {
            affectedChunks = Set.copyOf(affectedChunks);
        }

        public static LightUpdateResult incremental(Set<ChunkPos> affectedChunks) {
            return new LightUpdateResult(false, affectedChunks);
        }

        public static LightUpdateResult fullRebuildFallback(Set<ChunkPos> affectedChunks) {
            return new LightUpdateResult(true, affectedChunks);
        }
    }

    private record LightNode(int x, int y, int z, int light) {
    }

    public record WorkStats(
            int requestedCenters,
            int affectedChunks,
            int skyColumnsSeeded,
            int skyCellsSeeded,
            int skyBoundarySeeds,
            long skyPropagationNodes,
            int blockEmitterSectionsScanned,
            int blockEmitterBlocksVisited,
            int blockEmittersSeeded,
            long blockPropagationNodes
    ) {
        public WorkStats {
            requestedCenters = Math.max(0, requestedCenters);
            affectedChunks = Math.max(0, affectedChunks);
            skyColumnsSeeded = Math.max(0, skyColumnsSeeded);
            skyCellsSeeded = Math.max(0, skyCellsSeeded);
            skyBoundarySeeds = Math.max(0, skyBoundarySeeds);
            skyPropagationNodes = Math.max(0L, skyPropagationNodes);
            blockEmitterSectionsScanned = Math.max(0, blockEmitterSectionsScanned);
            blockEmitterBlocksVisited = Math.max(0, blockEmitterBlocksVisited);
            blockEmittersSeeded = Math.max(0, blockEmittersSeeded);
            blockPropagationNodes = Math.max(0L, blockPropagationNodes);
        }

        public static WorkStats empty() {
            return new WorkStats(0, 0, 0, 0, 0, 0L, 0, 0, 0, 0L);
        }
    }

    private static final class WorkStatsBuilder {
        private int requestedCenters;
        private int affectedChunks;
        private int skyColumnsSeeded;
        private int skyCellsSeeded;
        private int skyBoundarySeeds;
        private long skyPropagationNodes;
        private int blockEmitterSectionsScanned;
        private int blockEmitterBlocksVisited;
        private int blockEmittersSeeded;
        private long blockPropagationNodes;

        private WorkStats build() {
            return new WorkStats(
                    requestedCenters,
                    affectedChunks,
                    skyColumnsSeeded,
                    skyCellsSeeded,
                    skyBoundarySeeds,
                    skyPropagationNodes,
                    blockEmitterSectionsScanned,
                    blockEmitterBlocksVisited,
                    blockEmittersSeeded,
                    blockPropagationNodes
            );
        }
    }
}
