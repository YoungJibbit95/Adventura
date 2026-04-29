package dev.voxelgame.common.world.light;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.DimensionSettings;
import dev.voxelgame.common.world.InMemoryWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LightEngineTest {
    @Test
    void torchLightPropagatesOutward() {
        InMemoryWorld world = new InMemoryWorld(new DimensionSettings(0, 16), Blocks.createDefaultRegistry());
        Chunk chunk = world.getOrCreateChunk(new ChunkPos(0, 0));
        chunk.setBlockId(8, 8, 8, Blocks.TORCH);

        new LightEngine().rebuildChunkLighting(world, new ChunkPos(0, 0));

        assertEquals(14, world.blockLight(8, 8, 8));
        assertEquals(13, world.blockLight(9, 8, 8));
    }
}
