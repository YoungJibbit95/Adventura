package dev.voxelgame.common.world;

import dev.voxelgame.common.block.FluidBlocks;

import java.util.Arrays;

public final class ChunkSection {
    public static final int SIZE = 16;
    public static final int VOLUME = SIZE * SIZE * SIZE;

    private final int sectionY;
    private final short[] blockIds = new short[VOLUME];
    private final byte[] skyLight = new byte[VOLUME];
    private final byte[] blockLight = new byte[VOLUME];
    private int nonAirBlockCount;
    private int dirtyFlags;

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
        if (old == blockId) {
            return;
        }
        if (old == 0 && blockId != 0) {
            nonAirBlockCount++;
        } else if (old != 0 && blockId == 0) {
            nonAirBlockCount--;
        }
        blockIds[index] = blockId;
        markDirty(DirtyAspect.GEOMETRY);
        markDirty(DirtyAspect.LIGHT);
        if (isFluidBlock(old) || isFluidBlock(blockId)) {
            markDirty(DirtyAspect.FLUID);
        }
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
        int index = index(x, y, z);
        byte checked = checkedLight(light);
        if (skyLight[index] != checked) {
            skyLight[index] = checked;
            markDirty(DirtyAspect.LIGHT);
        }
    }

    public int blockLight(int x, int y, int z) {
        return blockLight[index(x, y, z)] & 0x0F;
    }

    public void setBlockLight(int x, int y, int z, int light) {
        int index = index(x, y, z);
        byte checked = checkedLight(light);
        if (blockLight[index] != checked) {
            blockLight[index] = checked;
            markDirty(DirtyAspect.LIGHT);
        }
    }

    public void clearBlockLight() {
        if (!allZero(blockLight)) {
            markDirty(DirtyAspect.LIGHT);
        }
        Arrays.fill(blockLight, (byte) 0);
    }

    public int dirtyFlags() {
        return dirtyFlags;
    }

    public boolean isDirty(DirtyAspect aspect) {
        return aspect != null && (dirtyFlags & aspect.bit()) != 0;
    }

    public void markDirty(DirtyAspect aspect) {
        if (aspect != null) {
            dirtyFlags |= aspect.bit();
        }
    }

    public void clearDirty(DirtyAspect aspect) {
        if (aspect != null) {
            dirtyFlags &= ~aspect.bit();
        }
    }

    public void clearDirtyFlags() {
        dirtyFlags = 0;
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

    private static boolean isFluidBlock(short blockId) {
        return FluidBlocks.isFluid(blockId);
    }

    private static boolean allZero(byte[] values) {
        for (byte value : values) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    public enum DirtyAspect {
        GEOMETRY(1),
        LIGHT(1 << 1),
        FLUID(1 << 2),
        BLOCK_ENTITY(1 << 3);

        private final int bit;

        DirtyAspect(int bit) {
            this.bit = bit;
        }

        public int bit() {
            return bit;
        }
    }
}
