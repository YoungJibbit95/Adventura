package dev.voxelgame.common.block;

public final class FluidBlocks {
    private FluidBlocks() {
    }

    public static boolean isWater(short blockId) {
        return blockId == Blocks.WATER;
    }

    public static boolean isLava(short blockId) {
        return blockId == Blocks.LAVA;
    }

    public static boolean isFluid(short blockId) {
        return isWater(blockId) || isLava(blockId);
    }

    public static FluidKind kind(short blockId) {
        if (isWater(blockId)) {
            return FluidKind.WATER;
        }
        if (isLava(blockId)) {
            return FluidKind.LAVA;
        }
        return FluidKind.NONE;
    }

    public enum FluidKind {
        NONE,
        WATER,
        LAVA;

        public boolean fluid() {
            return this != NONE;
        }
    }
}
