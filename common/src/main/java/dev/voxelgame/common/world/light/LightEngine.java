package dev.voxelgame.common.world.light;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.InMemoryWorld;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public final class LightEngine {
    private static final int[][] DIRECTIONS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    public void rebuildChunkLighting(InMemoryWorld world, ChunkPos center) {
        Chunk chunk = world.getOrCreateChunk(center);
        rebuildSkyLightColumn(world, chunk);
        rebuildBlockLight(world, center);
    }

    public void rebuildSkyLightColumn(InMemoryWorld world, Chunk chunk) {
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
                    if (block.opaque()) {
                        light = 0;
                    }
                }
            }
        }
    }

    public void rebuildBlockLight(InMemoryWorld world, ChunkPos center) {
        Queue<LightNode> queue = new ArrayDeque<>();
        List<Chunk> affected = affectedChunks(world, center);
        for (Chunk chunk : affected) {
            chunk.clearBlockLight();
            enqueueEmitters(world, chunk, queue);
        }

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
                if (targetChunk == null || !isAffected(targetChunk.pos(), center)) {
                    continue;
                }
                BlockType block = world.blockType(targetChunk.blockId(nx, ny, nz));
                if (block.opaque()) {
                    continue;
                }
                if (targetChunk.blockLight(nx, ny, nz) >= nextLight) {
                    continue;
                }
                targetChunk.setBlockLight(nx, ny, nz, nextLight);
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

    private void enqueueEmitters(InMemoryWorld world, Chunk chunk, Queue<LightNode> queue) {
        int baseX = chunk.pos().x() * ChunkPos.SIZE;
        int baseZ = chunk.pos().z() * ChunkPos.SIZE;
        for (int z = baseZ; z < baseZ + ChunkPos.SIZE; z++) {
            for (int x = baseX; x < baseX + ChunkPos.SIZE; x++) {
                for (int y = world.dimension().minY(); y < world.dimension().maxYExclusive(); y++) {
                    BlockType block = world.blockType(chunk.blockId(x, y, z));
                    if (block.emitsLight()) {
                        chunk.setBlockLight(x, y, z, block.lightEmission());
                        queue.add(new LightNode(x, y, z, block.lightEmission()));
                    }
                }
            }
        }
    }

    private record LightNode(int x, int y, int z, int light) {
    }
}
