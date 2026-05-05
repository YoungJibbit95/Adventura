package dev.voxelgame.common.world;

import java.util.Objects;

public final class ChunkTerrainCache {
    public static final int COLUMN_COUNT = ChunkPos.SIZE * ChunkPos.SIZE;
    public static final int MAX_FLUID_DEPTH_HINT = 15;
    public static final int SHORE_NORTH = 1;
    public static final int SHORE_EAST = 1 << 1;
    public static final int SHORE_SOUTH = 1 << 2;
    public static final int SHORE_WEST = 1 << 3;
    public static final int SHORE_MASK_ALL = SHORE_NORTH | SHORE_EAST | SHORE_SOUTH | SHORE_WEST;
    public static final int FLUID_SURFACE_FOAM = 1;
    public static final int FLUID_SURFACE_FLAGS_ALL = FLUID_SURFACE_FOAM;

    private final ChunkPos pos;
    private final int[] heights;
    private final BiomeType[] biomes;
    private final short[] surfaceBlockIds;
    private final boolean[] fluidColumns;
    private final boolean[] caveColumns;
    private final byte[] fluidDepthHints;
    private final byte[] shoreMasks;
    private final byte[] fluidSurfaceFlags;

    public ChunkTerrainCache(ChunkPos pos, int[] heights, BiomeType[] biomes) {
        this(pos, heights, biomes, new short[COLUMN_COUNT], new boolean[COLUMN_COUNT], new boolean[COLUMN_COUNT]);
    }

    public ChunkTerrainCache(
            ChunkPos pos,
            int[] heights,
            BiomeType[] biomes,
            short[] surfaceBlockIds,
            boolean[] fluidColumns,
            boolean[] caveColumns
    ) {
        this(
                pos,
                heights,
                biomes,
                surfaceBlockIds,
                fluidColumns,
                caveColumns,
                defaultFluidDepthHints(fluidColumns),
                new byte[COLUMN_COUNT],
                new byte[COLUMN_COUNT]
        );
    }

    public ChunkTerrainCache(
            ChunkPos pos,
            int[] heights,
            BiomeType[] biomes,
            short[] surfaceBlockIds,
            boolean[] fluidColumns,
            boolean[] caveColumns,
            byte[] fluidDepthHints,
            byte[] shoreMasks
    ) {
        this(pos, heights, biomes, surfaceBlockIds, fluidColumns, caveColumns, fluidDepthHints, shoreMasks, new byte[COLUMN_COUNT]);
    }

    public ChunkTerrainCache(
            ChunkPos pos,
            int[] heights,
            BiomeType[] biomes,
            short[] surfaceBlockIds,
            boolean[] fluidColumns,
            boolean[] caveColumns,
            byte[] fluidDepthHints,
            byte[] shoreMasks,
            byte[] fluidSurfaceFlags
    ) {
        this.pos = Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(heights, "heights");
        Objects.requireNonNull(biomes, "biomes");
        Objects.requireNonNull(surfaceBlockIds, "surfaceBlockIds");
        Objects.requireNonNull(fluidColumns, "fluidColumns");
        Objects.requireNonNull(caveColumns, "caveColumns");
        Objects.requireNonNull(fluidDepthHints, "fluidDepthHints");
        Objects.requireNonNull(shoreMasks, "shoreMasks");
        Objects.requireNonNull(fluidSurfaceFlags, "fluidSurfaceFlags");
        if (heights.length != COLUMN_COUNT) {
            throw new IllegalArgumentException("Height cache must contain " + COLUMN_COUNT + " columns");
        }
        if (biomes.length != COLUMN_COUNT) {
            throw new IllegalArgumentException("Biome cache must contain " + COLUMN_COUNT + " columns");
        }
        if (surfaceBlockIds.length != COLUMN_COUNT) {
            throw new IllegalArgumentException("Surface cache must contain " + COLUMN_COUNT + " columns");
        }
        if (fluidColumns.length != COLUMN_COUNT) {
            throw new IllegalArgumentException("Fluid cache must contain " + COLUMN_COUNT + " columns");
        }
        if (caveColumns.length != COLUMN_COUNT) {
            throw new IllegalArgumentException("Cave cache must contain " + COLUMN_COUNT + " columns");
        }
        if (fluidDepthHints.length != COLUMN_COUNT) {
            throw new IllegalArgumentException("Fluid depth cache must contain " + COLUMN_COUNT + " columns");
        }
        if (shoreMasks.length != COLUMN_COUNT) {
            throw new IllegalArgumentException("Shore mask cache must contain " + COLUMN_COUNT + " columns");
        }
        if (fluidSurfaceFlags.length != COLUMN_COUNT) {
            throw new IllegalArgumentException("Fluid surface flag cache must contain " + COLUMN_COUNT + " columns");
        }
        this.heights = heights.clone();
        this.biomes = biomes.clone();
        this.surfaceBlockIds = surfaceBlockIds.clone();
        this.fluidColumns = fluidColumns.clone();
        this.caveColumns = caveColumns.clone();
        this.fluidDepthHints = fluidDepthHints.clone();
        this.shoreMasks = shoreMasks.clone();
        this.fluidSurfaceFlags = fluidSurfaceFlags.clone();
        for (int i = 0; i < this.biomes.length; i++) {
            Objects.requireNonNull(this.biomes[i], "biomes[" + i + "]");
            validateFluidSurface(i);
        }
    }

    public ChunkPos pos() {
        return pos;
    }

    public int heightAtLocal(int localX, int localZ) {
        return heights[index(localX, localZ)];
    }

    public int heightAtWorld(int x, int z) {
        requireWorldColumn(x, z);
        return heightAtLocal(ChunkPos.localCoord(x), ChunkPos.localCoord(z));
    }

    public BiomeType biomeAtLocal(int localX, int localZ) {
        return biomes[index(localX, localZ)];
    }

    public BiomeType biomeAtWorld(int x, int z) {
        requireWorldColumn(x, z);
        return biomeAtLocal(ChunkPos.localCoord(x), ChunkPos.localCoord(z));
    }

    public short surfaceBlockAtLocal(int localX, int localZ) {
        return surfaceBlockIds[index(localX, localZ)];
    }

    public short surfaceBlockAtWorld(int x, int z) {
        requireWorldColumn(x, z);
        return surfaceBlockAtLocal(ChunkPos.localCoord(x), ChunkPos.localCoord(z));
    }

    public boolean hasFluidAtLocal(int localX, int localZ) {
        return fluidColumns[index(localX, localZ)];
    }

    public boolean hasFluidAtWorld(int x, int z) {
        requireWorldColumn(x, z);
        return hasFluidAtLocal(ChunkPos.localCoord(x), ChunkPos.localCoord(z));
    }

    public int fluidDepthHintAtLocal(int localX, int localZ) {
        return Byte.toUnsignedInt(fluidDepthHints[index(localX, localZ)]);
    }

    public int fluidDepthHintAtWorld(int x, int z) {
        requireWorldColumn(x, z);
        return fluidDepthHintAtLocal(ChunkPos.localCoord(x), ChunkPos.localCoord(z));
    }

    public int shoreMaskAtLocal(int localX, int localZ) {
        return Byte.toUnsignedInt(shoreMasks[index(localX, localZ)]);
    }

    public int shoreMaskAtWorld(int x, int z) {
        requireWorldColumn(x, z);
        return shoreMaskAtLocal(ChunkPos.localCoord(x), ChunkPos.localCoord(z));
    }

    public int fluidSurfaceFlagsAtLocal(int localX, int localZ) {
        return Byte.toUnsignedInt(fluidSurfaceFlags[index(localX, localZ)]);
    }

    public int fluidSurfaceFlagsAtWorld(int x, int z) {
        requireWorldColumn(x, z);
        return fluidSurfaceFlagsAtLocal(ChunkPos.localCoord(x), ChunkPos.localCoord(z));
    }

    public boolean isShoreAtLocal(int localX, int localZ) {
        return shoreMaskAtLocal(localX, localZ) != 0;
    }

    public boolean isShoreAtWorld(int x, int z) {
        requireWorldColumn(x, z);
        return isShoreAtLocal(ChunkPos.localCoord(x), ChunkPos.localCoord(z));
    }

    public FluidSurface fluidSurfaceAtLocal(int localX, int localZ) {
        int index = index(localX, localZ);
        return new FluidSurface(
                fluidColumns[index],
                Byte.toUnsignedInt(fluidDepthHints[index]),
                Byte.toUnsignedInt(shoreMasks[index]),
                Byte.toUnsignedInt(fluidSurfaceFlags[index])
        );
    }

    public FluidSurface fluidSurfaceAtWorld(int x, int z) {
        requireWorldColumn(x, z);
        return fluidSurfaceAtLocal(ChunkPos.localCoord(x), ChunkPos.localCoord(z));
    }

    public boolean hasCaveAtLocal(int localX, int localZ) {
        return caveColumns[index(localX, localZ)];
    }

    public boolean hasCaveAtWorld(int x, int z) {
        requireWorldColumn(x, z);
        return hasCaveAtLocal(ChunkPos.localCoord(x), ChunkPos.localCoord(z));
    }

    public int estimatedBytes() {
        return heights.length * Integer.BYTES
                + biomes.length * Integer.BYTES
                + surfaceBlockIds.length * Short.BYTES
                + fluidColumns.length
                + caveColumns.length
                + fluidDepthHints.length
                + shoreMasks.length
                + fluidSurfaceFlags.length;
    }

    public int[] copyHeights() {
        return heights.clone();
    }

    public BiomeType[] copyBiomes() {
        return biomes.clone();
    }

    public short[] copySurfaceBlockIds() {
        return surfaceBlockIds.clone();
    }

    public boolean[] copyFluidColumns() {
        return fluidColumns.clone();
    }

    public boolean[] copyCaveColumns() {
        return caveColumns.clone();
    }

    public byte[] copyFluidDepthHints() {
        return fluidDepthHints.clone();
    }

    public byte[] copyShoreMasks() {
        return shoreMasks.clone();
    }

    public byte[] copyFluidSurfaceFlags() {
        return fluidSurfaceFlags.clone();
    }

    private void requireWorldColumn(int x, int z) {
        ChunkPos actual = ChunkPos.fromBlock(x, z);
        if (!pos.equals(actual)) {
            throw new IllegalArgumentException("Column " + x + "," + z + " is in " + actual + ", not " + pos);
        }
    }

    private static int index(int localX, int localZ) {
        checkLocal("localX", localX);
        checkLocal("localZ", localZ);
        return localZ * ChunkPos.SIZE + localX;
    }

    private static void checkLocal(String name, int value) {
        if (value < 0 || value >= ChunkPos.SIZE) {
            throw new IndexOutOfBoundsException(name + " outside chunk: " + value);
        }
    }

    private void validateFluidSurface(int index) {
        int depthHint = Byte.toUnsignedInt(fluidDepthHints[index]);
        int shoreMask = Byte.toUnsignedInt(shoreMasks[index]);
        int flags = Byte.toUnsignedInt(fluidSurfaceFlags[index]);
        if ((shoreMask & ~SHORE_MASK_ALL) != 0) {
            throw new IllegalArgumentException("Unknown shore mask bits at column " + index + ": " + shoreMask);
        }
        if ((flags & ~FLUID_SURFACE_FLAGS_ALL) != 0) {
            throw new IllegalArgumentException("Unknown fluid surface flag bits at column " + index + ": " + flags);
        }
        if (depthHint > MAX_FLUID_DEPTH_HINT) {
            throw new IllegalArgumentException("Fluid depth hint must be in 0.." + MAX_FLUID_DEPTH_HINT + " at column " + index + ": " + depthHint);
        }
        if (!fluidColumns[index] && (depthHint != 0 || shoreMask != 0 || flags != 0)) {
            throw new IllegalArgumentException("Dry columns cannot carry fluid surface data at column " + index);
        }
        if (fluidColumns[index] && depthHint == 0) {
            throw new IllegalArgumentException("Fluid columns need a non-zero depth hint at column " + index);
        }
    }

    private static byte[] defaultFluidDepthHints(boolean[] fluidColumns) {
        Objects.requireNonNull(fluidColumns, "fluidColumns");
        if (fluidColumns.length != COLUMN_COUNT) {
            throw new IllegalArgumentException("Fluid cache must contain " + COLUMN_COUNT + " columns");
        }
        byte[] hints = new byte[COLUMN_COUNT];
        for (int i = 0; i < fluidColumns.length; i++) {
            hints[i] = fluidColumns[i] ? (byte) 1 : 0;
        }
        return hints;
    }

    public record FluidSurface(boolean fluid, int depthHint, int shoreMask, int flags) {
        public FluidSurface {
            if (depthHint < 0 || depthHint > MAX_FLUID_DEPTH_HINT) {
                throw new IllegalArgumentException("Fluid depth hint must be in 0.." + MAX_FLUID_DEPTH_HINT + ": " + depthHint);
            }
            if ((shoreMask & ~SHORE_MASK_ALL) != 0) {
                throw new IllegalArgumentException("Unknown shore mask bits: " + shoreMask);
            }
            if ((flags & ~FLUID_SURFACE_FLAGS_ALL) != 0) {
                throw new IllegalArgumentException("Unknown fluid surface flag bits: " + flags);
            }
            if (!fluid && (depthHint != 0 || shoreMask != 0 || flags != 0)) {
                throw new IllegalArgumentException("Dry surfaces cannot carry fluid data");
            }
            if (fluid && depthHint == 0) {
                throw new IllegalArgumentException("Fluid surfaces need a non-zero depth hint");
            }
        }

        public boolean shoreline() {
            return shoreMask != 0;
        }

        public boolean foam() {
            return (flags & FLUID_SURFACE_FOAM) != 0;
        }

        public boolean touchesShore(int shoreDirection) {
            if ((shoreDirection & ~SHORE_MASK_ALL) != 0 || shoreDirection == 0) {
                throw new IllegalArgumentException("Unknown shore direction: " + shoreDirection);
            }
            return (shoreMask & shoreDirection) != 0;
        }
    }
}
