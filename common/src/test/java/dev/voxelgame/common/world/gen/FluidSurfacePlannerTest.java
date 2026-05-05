package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkTerrainCache;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FluidSurfacePlannerTest {
    @Test
    void fillsDepthShoreAndFoamForInteriorColumns() {
        int[] heights = new int[ChunkTerrainCache.COLUMN_COUNT];
        boolean[] fluids = new boolean[ChunkTerrainCache.COLUMN_COUNT];
        byte[] depthHints = new byte[ChunkTerrainCache.COLUMN_COUNT];
        byte[] shoreMasks = new byte[ChunkTerrainCache.COLUMN_COUNT];
        byte[] flags = new byte[ChunkTerrainCache.COLUMN_COUNT];
        Arrays.fill(heights, 60);
        int index = index(8, 8);
        heights[index] = 61;
        heights[index(8, 7)] = 70;
        heights[index(9, 8)] = 64;
        fluids[index] = true;

        FluidSurfacePlanner.fillChunk(
                63,
                new ChunkPos(0, 0),
                heights,
                fluids,
                depthHints,
                shoreMasks,
                flags,
                (x, z) -> 60,
                (x, z) -> 0.0
        );

        assertEquals(2, Byte.toUnsignedInt(depthHints[index]));
        assertEquals(ChunkTerrainCache.SHORE_NORTH | ChunkTerrainCache.SHORE_EAST, Byte.toUnsignedInt(shoreMasks[index]));
        assertEquals(ChunkTerrainCache.FLUID_SURFACE_FOAM, Byte.toUnsignedInt(flags[index]));
        assertEquals(0, Byte.toUnsignedInt(depthHints[index(8, 9)]));
    }

    @Test
    void samplesOnlyChunkBordersFromWorldHeightSampler() {
        int[] heights = new int[ChunkTerrainCache.COLUMN_COUNT];
        boolean[] fluids = new boolean[ChunkTerrainCache.COLUMN_COUNT];
        byte[] depthHints = new byte[ChunkTerrainCache.COLUMN_COUNT];
        byte[] shoreMasks = new byte[ChunkTerrainCache.COLUMN_COUNT];
        byte[] flags = new byte[ChunkTerrainCache.COLUMN_COUNT];
        Arrays.fill(heights, 60);
        int index = index(0, 0);
        heights[index] = 48;
        heights[index(1, 0)] = 72;
        heights[index(0, 1)] = 60;
        fluids[index] = true;

        FluidSurfacePlanner.fillChunk(
                63,
                new ChunkPos(3, -2),
                heights,
                fluids,
                depthHints,
                shoreMasks,
                flags,
                (x, z) -> x == 47 || z == -33 ? 80 : 60,
                (x, z) -> 0.72
        );

        assertEquals(ChunkTerrainCache.MAX_FLUID_DEPTH_HINT, Byte.toUnsignedInt(depthHints[index]));
        assertEquals(
                ChunkTerrainCache.SHORE_NORTH | ChunkTerrainCache.SHORE_EAST | ChunkTerrainCache.SHORE_WEST,
                Byte.toUnsignedInt(shoreMasks[index])
        );
        assertEquals(ChunkTerrainCache.FLUID_SURFACE_FOAM, Byte.toUnsignedInt(flags[index]));
    }

    private static int index(int localX, int localZ) {
        return localZ * ChunkPos.SIZE + localX;
    }
}
