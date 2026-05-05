package dev.voxelgame.client.world;

import dev.voxelgame.common.block.BlockRenderLayer;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkSection;
import dev.voxelgame.common.world.InMemoryWorld;

import java.util.Objects;

final class ChunkRenderLayerPresence {
    private final boolean solid;
    private final boolean cutout;
    private final boolean translucent;

    private ChunkRenderLayerPresence(boolean solid, boolean cutout, boolean translucent) {
        this.solid = solid;
        this.cutout = cutout;
        this.translucent = translucent;
    }

    static ChunkRenderLayerPresence scan(InMemoryWorld world, Chunk chunk) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(chunk, "chunk");
        boolean solid = false;
        boolean cutout = false;
        boolean translucent = false;
        for (int sectionIndex = 0; sectionIndex < chunk.sectionCount(); sectionIndex++) {
            ChunkSection section = chunk.sectionByIndex(sectionIndex);
            if (section.isEmpty()) {
                continue;
            }
            for (int localY = 0; localY < ChunkSection.SIZE; localY++) {
                for (int localZ = 0; localZ < ChunkSection.SIZE; localZ++) {
                    for (int localX = 0; localX < ChunkSection.SIZE; localX++) {
                        short blockId = section.blockId(localX, localY, localZ);
                        if (blockId == Blocks.AIR) {
                            continue;
                        }
                        switch (world.blockType(blockId).renderLayer()) {
                            case SOLID -> solid = true;
                            case CUTOUT -> cutout = true;
                            case TRANSLUCENT -> translucent = true;
                        }
                        if (solid && cutout && translucent) {
                            return new ChunkRenderLayerPresence(true, true, true);
                        }
                    }
                }
            }
        }
        return new ChunkRenderLayerPresence(solid, cutout, translucent);
    }

    boolean contains(BlockRenderLayer layer) {
        return switch (Objects.requireNonNull(layer, "layer")) {
            case SOLID -> solid;
            case CUTOUT -> cutout;
            case TRANSLUCENT -> translucent;
        };
    }
}
