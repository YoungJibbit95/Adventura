package dev.voxelgame.common.world.light;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.InMemoryWorld;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class LightEngine {
    private static final int[][] DIRECTIONS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    public void rebuildChunkLighting(InMemoryWorld world, ChunkPos center) {
        world.getOrCreateChunk(center);
        rebuildSkyLight(world, center);
        rebuildBlockLight(world, center);
    }

    public void rebuildSkyLight(InMemoryWorld world, ChunkPos center) {
        rebuildSkyLight(world, affectedChunks(world, center));
    }

    public void rebuildSkyLightColumn(InMemoryWorld world, Chunk chunk) {
        List<Chunk> chunks = new ArrayList<>();
        chunks.add(chunk);
        rebuildSkyLight(world, chunks);
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
        propagateBlockLight(world, addQueue, affected);
        return LightUpdateResult.incremental(affected);
    }

    private void rebuildSkyLight(InMemoryWorld world, List<Chunk> chunks) {
        Queue<LightNode> queue = new ArrayDeque<>();
        Set<ChunkPos> affectedPositions = new HashSet<>();
        for (Chunk chunk : chunks) {
            affectedPositions.add(chunk.pos());
            seedSkyLightColumns(world, chunk, queue);
        }
        propagateSkyLight(world, affectedPositions, queue);
    }

    private void seedSkyLightColumns(InMemoryWorld world, Chunk chunk, Queue<LightNode> queue) {
        int baseX = chunk.pos().x() * ChunkPos.SIZE;
        int baseZ = chunk.pos().z() * ChunkPos.SIZE;
        for (int localZ = 0; localZ < ChunkPos.SIZE; localZ++) {
            for (int localX = 0; localX < ChunkPos.SIZE; localX++) {
                int light = 15;
                int x = baseX + localX;
                int z = baseZ + localZ;
                for (int y = chunk.dimension().maxYExclusive() - 1; y >= chunk.dimension().minY(); y--) {
                    BlockType block = world.blockType(chunk.blockId(x, y, z));
                    chunk.setSkyLight(x, y, z, light);
                    int outgoingLight = LightRules.outgoingSkyLight(block, light);
                    if (outgoingLight > 0) {
                        queue.add(new LightNode(x, y, z, outgoingLight));
                    }
                    light = outgoingLight;
                }
            }
        }
    }

    private void propagateSkyLight(InMemoryWorld world, Set<ChunkPos> affectedPositions, Queue<LightNode> queue) {
        while (!queue.isEmpty()) {
            LightNode node = queue.remove();
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
        Queue<LightNode> queue = new ArrayDeque<>();
        Set<ChunkPos> affectedPositions = new HashSet<>();
        List<Chunk> affected = affectedChunks(world, center);
        for (Chunk chunk : affected) {
            chunk.clearBlockLight();
            enqueueEmitters(world, LightSourceRegistry.fromBlocks(world.blocks()), chunk, queue, affectedPositions);
        }
        propagateBlockLight(world, queue, affectedPositions, center);
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
        propagateBlockLight(world, queue, affected, null);
    }

    private void propagateBlockLight(InMemoryWorld world, Queue<LightNode> queue, Set<ChunkPos> affected, ChunkPos centerLimit) {
        while (!queue.isEmpty()) {
            LightNode node = queue.remove();
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
                if (targetChunk == null || centerLimit != null && !isAffected(targetChunk.pos(), centerLimit)) {
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
        List<Chunk> chunks = new ArrayList<>();
        for (Chunk chunk : world.loadedChunks()) {
            if (isAffected(chunk.pos(), center)) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    private boolean isAffected(ChunkPos pos, ChunkPos center) {
        return Math.abs(pos.x() - center.x()) <= 1 && Math.abs(pos.z() - center.z()) <= 1;
    }

    private void enqueueEmitters(
            InMemoryWorld world,
            LightSourceRegistry sources,
            Chunk chunk,
            Queue<LightNode> queue,
            Set<ChunkPos> affected
    ) {
        int baseX = chunk.pos().x() * ChunkPos.SIZE;
        int baseZ = chunk.pos().z() * ChunkPos.SIZE;
        for (int z = baseZ; z < baseZ + ChunkPos.SIZE; z++) {
            for (int x = baseX; x < baseX + ChunkPos.SIZE; x++) {
                for (int y = world.dimension().minY(); y < world.dimension().maxYExclusive(); y++) {
                    short blockId = chunk.blockId(x, y, z);
                    int light = sources.lightValue(blockId);
                    if (light > 0) {
                        setBlockLight(chunk, x, y, z, light, affected);
                        queue.add(new LightNode(x, y, z, light));
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
}
