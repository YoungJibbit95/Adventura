package dev.voxelgame.common.world;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryWorldTest {
    @Test
    void removeChunkDropsLoadedChunkData() {
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        InMemoryWorld world = new InMemoryWorld(DimensionSettings.OVERWORLD, blocks);
        ChunkPos pos = new ChunkPos(0, 0);

        world.setBlockId(8, 80, 8, Blocks.STONE);

        assertTrue(world.removeChunk(pos).isPresent());
        assertEquals(0, world.loadedChunks().size());
        assertEquals(Blocks.AIR, world.blockId(8, 80, 8));
        assertTrue(world.removeChunk(pos).isEmpty());
    }
}
