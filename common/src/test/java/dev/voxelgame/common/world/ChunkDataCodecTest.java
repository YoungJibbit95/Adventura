package dev.voxelgame.common.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.net.GamePacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkDataCodecTest {
    @Test
    void roundTripsChunkBlocksAndLight() {
        InMemoryWorld source = new InMemoryWorld(new DimensionSettings(0, 32), Blocks.createDefaultRegistry());
        Chunk sourceChunk = source.getOrCreateChunk(new ChunkPos(-1, 2));
        sourceChunk.setBlockId(-16, 3, 32, Blocks.STONE);
        sourceChunk.setSkyLight(-16, 3, 32, 11);
        sourceChunk.setBlockLight(-16, 3, 32, 7);
        sourceChunk.setBlockId(-1, 18, 47, Blocks.TORCH);
        sourceChunk.setSkyLight(-1, 18, 47, 4);
        sourceChunk.setBlockLight(-1, 18, 47, 14);

        GamePacket.ChunkData packet = ChunkDataCodec.toPacket(sourceChunk);

        InMemoryWorld target = new InMemoryWorld(new DimensionSettings(0, 32), Blocks.createDefaultRegistry());
        ChunkDataCodec.applyToWorld(target, packet);

        assertEquals(Blocks.STONE, target.blockId(-16, 3, 32));
        assertEquals(11, target.skyLight(-16, 3, 32));
        assertEquals(7, target.blockLight(-16, 3, 32));
        assertEquals(Blocks.TORCH, target.blockId(-1, 18, 47));
        assertEquals(4, target.skyLight(-1, 18, 47));
        assertEquals(14, target.blockLight(-1, 18, 47));
    }
}
