package dev.voxelgame.common.world;

import dev.voxelgame.common.block.BlockType;

public interface WorldView {
    DimensionSettings dimension();

    short blockId(int x, int y, int z);

    BlockType blockType(short blockId);
}
