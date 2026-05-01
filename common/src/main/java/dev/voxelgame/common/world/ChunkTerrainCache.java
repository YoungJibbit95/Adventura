package dev.voxelgame.common.world;

import java.util.Objects;

public final class ChunkTerrainCache {
    public static final int COLUMN_COUNT = ChunkPos.SIZE * ChunkPos.SIZE;

    private final ChunkPos pos;
    private final int[] heights;
    private final BiomeType[] biomes;
    private final short[] surfaceBlockIds;
    private final boolean[] fluidColumns;
    private final boolean[] caveColumns;

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
        this.pos = Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(heights, "heights");
        Objects.requireNonNull(biomes, "biomes");
        Objects.requireNonNull(surfaceBlockIds, "surfaceBlockIds");
        Objects.requireNonNull(fluidColumns, "fluidColumns");
        Objects.requireNonNull(caveColumns, "caveColumns");
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
        this.heights = heights.clone();
        this.biomes = biomes.clone();
        this.surfaceBlockIds = surfaceBlockIds.clone();
        this.fluidColumns = fluidColumns.clone();
        this.caveColumns = caveColumns.clone();
        for (int i = 0; i < this.biomes.length; i++) {
            Objects.requireNonNull(this.biomes[i], "biomes[" + i + "]");
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
                + caveColumns.length;
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
}
