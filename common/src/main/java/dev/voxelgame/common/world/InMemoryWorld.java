package dev.voxelgame.common.world;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.registry.Registry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryWorld implements WorldView {
    private final DimensionSettings dimension;
    private final Registry<BlockType> blocks;
    private final Map<ChunkPos, Chunk> chunks = new LinkedHashMap<>();

    public InMemoryWorld(DimensionSettings dimension, Registry<BlockType> blocks) {
        this.dimension = dimension;
        this.blocks = blocks;
    }

    @Override
    public DimensionSettings dimension() {
        return dimension;
    }

    public Registry<BlockType> blocks() {
        return blocks;
    }

    public Chunk getOrCreateChunk(ChunkPos pos) {
        return chunks.computeIfAbsent(pos, key -> new Chunk(key, dimension));
    }

    public Optional<Chunk> findChunk(ChunkPos pos) {
        return Optional.ofNullable(chunks.get(pos));
    }

    public Collection<Chunk> loadedChunks() {
        return chunks.values();
    }

    @Override
    public short blockId(int x, int y, int z) {
        if (!dimension.containsY(y)) {
            return 0;
        }
        return findChunk(ChunkPos.fromBlock(x, z))
                .map(chunk -> chunk.blockId(x, y, z))
                .orElse((short) 0);
    }

    public void setBlockId(int x, int y, int z, short blockId) {
        if (!dimension.containsY(y)) {
            throw new IndexOutOfBoundsException("Y outside dimension: " + y);
        }
        getOrCreateChunk(ChunkPos.fromBlock(x, z)).setBlockId(x, y, z, blockId);
    }

    @Override
    public BlockType blockType(short blockId) {
        return blocks.requireById(blockId);
    }

    public int skyLight(int x, int y, int z) {
        if (!dimension.containsY(y)) {
            return 15;
        }
        return findChunk(ChunkPos.fromBlock(x, z)).map(chunk -> chunk.skyLight(x, y, z)).orElse(0);
    }

    public void setSkyLight(int x, int y, int z, int light) {
        if (dimension.containsY(y)) {
            getOrCreateChunk(ChunkPos.fromBlock(x, z)).setSkyLight(x, y, z, light);
        }
    }

    public int blockLight(int x, int y, int z) {
        if (!dimension.containsY(y)) {
            return 0;
        }
        return findChunk(ChunkPos.fromBlock(x, z)).map(chunk -> chunk.blockLight(x, y, z)).orElse(0);
    }

    public void setBlockLight(int x, int y, int z, int light) {
        if (dimension.containsY(y)) {
            getOrCreateChunk(ChunkPos.fromBlock(x, z)).setBlockLight(x, y, z, light);
        }
    }
}
