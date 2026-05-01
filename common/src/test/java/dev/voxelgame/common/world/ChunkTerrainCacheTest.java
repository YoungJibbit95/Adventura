package dev.voxelgame.common.world;

import dev.voxelgame.common.block.Blocks;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkTerrainCacheTest {
    @Test
    void cacheCopiesInputsAndOutputs() {
        BiomeType meadow = Biomes.createDefaultRegistry().requireByKey("voxel:cozy_meadow");
        BiomeType highlands = Biomes.createDefaultRegistry().requireByKey("voxel:highlands");
        int[] heights = new int[ChunkTerrainCache.COLUMN_COUNT];
        BiomeType[] biomes = new BiomeType[ChunkTerrainCache.COLUMN_COUNT];
        short[] surfaceBlocks = new short[ChunkTerrainCache.COLUMN_COUNT];
        boolean[] fluidColumns = new boolean[ChunkTerrainCache.COLUMN_COUNT];
        boolean[] caveColumns = new boolean[ChunkTerrainCache.COLUMN_COUNT];
        Arrays.fill(heights, 72);
        Arrays.fill(biomes, meadow);
        Arrays.fill(surfaceBlocks, Blocks.GRASS);
        fluidColumns[0] = true;
        caveColumns[1] = true;

        ChunkTerrainCache cache = new ChunkTerrainCache(new ChunkPos(0, 0), heights, biomes, surfaceBlocks, fluidColumns, caveColumns);
        heights[0] = 12;
        biomes[0] = highlands;
        surfaceBlocks[0] = Blocks.SAND;
        fluidColumns[0] = false;
        caveColumns[1] = false;

        assertEquals(72, cache.heightAtLocal(0, 0));
        assertEquals(meadow, cache.biomeAtLocal(0, 0));
        assertEquals(Blocks.GRASS, cache.surfaceBlockAtLocal(0, 0));
        assertTrue(cache.hasFluidAtLocal(0, 0));
        assertTrue(cache.hasCaveAtLocal(1, 0));

        int[] copiedHeights = cache.copyHeights();
        BiomeType[] copiedBiomes = cache.copyBiomes();
        short[] copiedSurfaceBlocks = cache.copySurfaceBlockIds();
        boolean[] copiedFluidColumns = cache.copyFluidColumns();
        boolean[] copiedCaveColumns = cache.copyCaveColumns();
        copiedHeights[0] = 10;
        copiedBiomes[0] = highlands;
        copiedSurfaceBlocks[0] = Blocks.SAND;
        copiedFluidColumns[0] = false;
        copiedCaveColumns[1] = false;

        assertEquals(72, cache.heightAtLocal(0, 0));
        assertEquals(meadow, cache.biomeAtLocal(0, 0));
        assertEquals(Blocks.GRASS, cache.surfaceBlockAtLocal(0, 0));
        assertTrue(cache.hasFluidAtLocal(0, 0));
        assertTrue(cache.hasCaveAtLocal(1, 0));
        assertEquals(ChunkTerrainCache.COLUMN_COUNT * 12, cache.estimatedBytes());
    }

    @Test
    void worldLookupsMustBelongToCacheChunk() {
        BiomeType biome = Biomes.createDefaultRegistry().requireByKey("voxel:cozy_meadow");
        int[] heights = new int[ChunkTerrainCache.COLUMN_COUNT];
        BiomeType[] biomes = new BiomeType[ChunkTerrainCache.COLUMN_COUNT];
        Arrays.fill(heights, 80);
        Arrays.fill(biomes, biome);
        ChunkTerrainCache cache = new ChunkTerrainCache(new ChunkPos(-1, 2), heights, biomes);

        assertEquals(80, cache.heightAtWorld(-16, 32));
        assertEquals(biome, cache.biomeAtWorld(-1, 47));
        assertThrows(IllegalArgumentException.class, () -> cache.heightAtWorld(0, 32));
        assertThrows(IndexOutOfBoundsException.class, () -> cache.heightAtLocal(ChunkPos.SIZE, 0));
    }
}
