package dev.voxelgame.common.world;

public record ChunkPos(int x, int z) {
    public static final int SIZE = 16;

    public static ChunkPos fromBlock(int blockX, int blockZ) {
        return new ChunkPos(Math.floorDiv(blockX, SIZE), Math.floorDiv(blockZ, SIZE));
    }

    public static int localCoord(int blockCoord) {
        return Math.floorMod(blockCoord, SIZE);
    }
}
