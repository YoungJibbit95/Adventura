package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.world.Chunk;

public interface WorldGenerator {
    void generate(Chunk chunk);
}
