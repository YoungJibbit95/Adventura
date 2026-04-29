package dev.voxelgame.common.world;

import java.util.Arrays;

public final class ChunkSection {
    public static final int SIZE = 16;
    public static final int VOLUME = SIZE * SIZE * SIZE;

    private final int sectionY;
    private final short[] blockIds = new short[VOLUME];
    private final byte[] skyLight = new byte[VOLUME];
    private final byte[] blockLight = new byte[VOLUME];
    private int nonAirBlockCount;

    public ChunkSection(int sectionY) {
        this.sectionY = sectionY;
    }

    public int sectionY() {
        return sectionY;
    }

    public short blockId(int x, int y, int z) {
        return blockIds[index(x, y, z)];
    }

    public void setBlockId(int x, int y, int z, short blockId) {
        int index = index(x, y, z);
        short old = blockIds[index];
        if (old == 0 && blockId != 0) {
            nonAirBlockCount++;
        } else if (old != 0 && blockId == 0) {
            nonAirBlockCount--;
        }
        blockIds[index] = blockId;
    }

    public boolean isEmpty() {
        return nonAirBlockCount == 0;
    }

    public int nonAirBlockCount() {
        return nonAirBlockCount;
    }

    public int skyLight(int x, int y, int z) {
        return skyLight[index(x, y, z)] & 0x0F;
    }

    public void setSkyLight(int x, int y, int z, int light) {
        skyLight[index(x, y, z)] = checkedLight(light);
    }

    public int blockLight(int x, int y, int z) {
        return blockLight[index(x, y, z)] & 0x0F;
    }

    public void setBlockLight(int x, int y, int z, int light) {
        blockLight[index(x, y, z)] = checkedLight(light);
    }

    public void clearBlockLight() {
        Arrays.fill(blockLight, (byte) 0);
    }

    public short[] copyBlockIds() {
        return blockIds.clone();
    }

    public byte[] copySkyLight() {
        return skyLight.clone();
    }

    public byte[] copyBlockLight() {
        return blockLight.clone();
    }

    public static int index(int x, int y, int z) {
        if ((x | y | z) < 0 || x >= SIZE || y >= SIZE || z >= SIZE) {
            throw new IndexOutOfBoundsException("Local block coordinate outside 0..15: " + x + "," + y + "," + z);
        }
        return (y << 8) | (z << 4) | x;
    }

    private static byte checkedLight(int light) {
        if (light < 0 || light > 15) {
            throw new IllegalArgumentException("Light must be in 0..15: " + light);
        }
        return (byte) light;
    }
}
