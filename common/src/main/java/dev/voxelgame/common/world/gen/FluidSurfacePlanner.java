package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkTerrainCache;

import java.util.Objects;

final class FluidSurfacePlanner {
    private FluidSurfacePlanner() {
    }

    static void fillChunk(
            int seaLevel,
            ChunkPos pos,
            int[] heights,
            boolean[] fluidColumns,
            byte[] fluidDepthHints,
            byte[] shoreMasks,
            byte[] fluidSurfaceFlags,
            ColumnHeightSampler heightSampler,
            RiverStrengthSampler riverStrengthSampler
    ) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(heights, "heights");
        Objects.requireNonNull(fluidColumns, "fluidColumns");
        Objects.requireNonNull(fluidDepthHints, "fluidDepthHints");
        Objects.requireNonNull(shoreMasks, "shoreMasks");
        Objects.requireNonNull(fluidSurfaceFlags, "fluidSurfaceFlags");
        Objects.requireNonNull(heightSampler, "heightSampler");
        Objects.requireNonNull(riverStrengthSampler, "riverStrengthSampler");
        requireColumnArray("heights", heights.length);
        requireColumnArray("fluidColumns", fluidColumns.length);
        requireColumnArray("fluidDepthHints", fluidDepthHints.length);
        requireColumnArray("shoreMasks", shoreMasks.length);
        requireColumnArray("fluidSurfaceFlags", fluidSurfaceFlags.length);

        int baseX = pos.x() * ChunkPos.SIZE;
        int baseZ = pos.z() * ChunkPos.SIZE;
        for (int localZ = 0; localZ < ChunkPos.SIZE; localZ++) {
            for (int localX = 0; localX < ChunkPos.SIZE; localX++) {
                int index = localZ * ChunkPos.SIZE + localX;
                if (!fluidColumns[index]) {
                    continue;
                }
                int x = baseX + localX;
                int z = baseZ + localZ;
                int depthHint = fluidDepthHint(seaLevel, heights[index]);
                int shoreMask = shoreMaskForColumn(seaLevel, pos, localX, localZ, heights, heightSampler);
                fluidDepthHints[index] = (byte) depthHint;
                shoreMasks[index] = (byte) shoreMask;
                fluidSurfaceFlags[index] = (byte) fluidSurfaceFlags(riverStrengthSampler.riverStrengthAt(x, z), depthHint, shoreMask);
            }
        }
    }

    private static int fluidDepthHint(int seaLevel, int height) {
        return clamp(seaLevel - height, 1, ChunkTerrainCache.MAX_FLUID_DEPTH_HINT);
    }

    private static int shoreMaskForColumn(
            int seaLevel,
            ChunkPos pos,
            int localX,
            int localZ,
            int[] heights,
            ColumnHeightSampler heightSampler
    ) {
        int mask = 0;
        if (isDryNeighborColumn(seaLevel, pos, localX, localZ - 1, heights, heightSampler)) {
            mask |= ChunkTerrainCache.SHORE_NORTH;
        }
        if (isDryNeighborColumn(seaLevel, pos, localX + 1, localZ, heights, heightSampler)) {
            mask |= ChunkTerrainCache.SHORE_EAST;
        }
        if (isDryNeighborColumn(seaLevel, pos, localX, localZ + 1, heights, heightSampler)) {
            mask |= ChunkTerrainCache.SHORE_SOUTH;
        }
        if (isDryNeighborColumn(seaLevel, pos, localX - 1, localZ, heights, heightSampler)) {
            mask |= ChunkTerrainCache.SHORE_WEST;
        }
        return mask;
    }

    private static boolean isDryNeighborColumn(
            int seaLevel,
            ChunkPos pos,
            int localX,
            int localZ,
            int[] heights,
            ColumnHeightSampler heightSampler
    ) {
        if (localX >= 0 && localX < ChunkPos.SIZE && localZ >= 0 && localZ < ChunkPos.SIZE) {
            return heights[localZ * ChunkPos.SIZE + localX] >= seaLevel;
        }
        int x = pos.x() * ChunkPos.SIZE + localX;
        int z = pos.z() * ChunkPos.SIZE + localZ;
        return heightSampler.heightAt(x, z) >= seaLevel;
    }

    private static int fluidSurfaceFlags(double riverStrength, int depthHint, int shoreMask) {
        boolean highEnergyShore = shoreMask != 0 && depthHint <= 2;
        boolean fastRiverSurface = riverStrength > 0.66;
        return highEnergyShore || fastRiverSurface ? ChunkTerrainCache.FLUID_SURFACE_FOAM : 0;
    }

    private static void requireColumnArray(String name, int length) {
        if (length != ChunkTerrainCache.COLUMN_COUNT) {
            throw new IllegalArgumentException(name + " must contain " + ChunkTerrainCache.COLUMN_COUNT + " columns");
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @FunctionalInterface
    interface ColumnHeightSampler {
        int heightAt(int x, int z);
    }

    @FunctionalInterface
    interface RiverStrengthSampler {
        double riverStrengthAt(int x, int z);
    }
}
