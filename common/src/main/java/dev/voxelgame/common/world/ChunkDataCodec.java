package dev.voxelgame.common.world;

import dev.voxelgame.common.net.GamePacket;

public final class ChunkDataCodec {
    private ChunkDataCodec() {
    }

    public static GamePacket.ChunkData toPacket(Chunk chunk) {
        int sectionVolume = ChunkSection.VOLUME;
        int total = chunk.sectionCount() * sectionVolume;
        short[] blocks = new short[total];
        byte[] sky = new byte[total];
        byte[] block = new byte[total];
        for (int sectionIndex = 0; sectionIndex < chunk.sectionCount(); sectionIndex++) {
            int offset = sectionIndex * sectionVolume;
            System.arraycopy(chunk.sectionByIndex(sectionIndex).copyBlockIds(), 0, blocks, offset, sectionVolume);
            System.arraycopy(chunk.sectionByIndex(sectionIndex).copySkyLight(), 0, sky, offset, sectionVolume);
            System.arraycopy(chunk.sectionByIndex(sectionIndex).copyBlockLight(), 0, block, offset, sectionVolume);
        }
        return new GamePacket.ChunkData(chunk.pos(), chunk.dimension().minY(), blocks, sky, block);
    }

    public static Chunk applyToWorld(InMemoryWorld world, GamePacket.ChunkData data) {
        if (data.minY() != world.dimension().minY()) {
            throw new IllegalArgumentException("Chunk packet minY does not match world dimension: " + data.minY());
        }

        Chunk chunk = world.getOrCreateChunk(data.pos());
        int expected = chunk.sectionCount() * ChunkSection.VOLUME;
        short[] blocks = data.blockIds();
        byte[] sky = data.skyLight();
        byte[] block = data.blockLight();
        if (blocks.length != expected || sky.length != expected || block.length != expected) {
            throw new IllegalArgumentException("Chunk packet arrays must be " + expected + " entries");
        }

        for (int sectionIndex = 0; sectionIndex < chunk.sectionCount(); sectionIndex++) {
            int offset = sectionIndex * ChunkSection.VOLUME;
            ChunkSection section = chunk.sectionByIndex(sectionIndex);
            for (int localY = 0; localY < ChunkSection.SIZE; localY++) {
                int worldY = section.sectionY() * ChunkSection.SIZE + localY;
                for (int localZ = 0; localZ < ChunkSection.SIZE; localZ++) {
                    int worldZ = data.pos().z() * ChunkPos.SIZE + localZ;
                    for (int localX = 0; localX < ChunkSection.SIZE; localX++) {
                        int index = offset + ChunkSection.index(localX, localY, localZ);
                        int worldX = data.pos().x() * ChunkPos.SIZE + localX;
                        section.setBlockId(localX, localY, localZ, blocks[index]);
                        section.setSkyLight(localX, localY, localZ, sky[index] & 0x0F);
                        section.setBlockLight(localX, localY, localZ, block[index] & 0x0F);
                    }
                }
            }
        }
        return chunk;
    }
}
