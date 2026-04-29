package dev.voxelgame.common.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.registry.Registry;

public final class Biomes {
    private Biomes() {
    }

    public static Registry<BiomeType> createDefaultRegistry() {
        Registry<BiomeType> registry = new Registry<>("biome");
        registry.register((short) 1, "voxel:meadow", new BiomeType("voxel:meadow", Blocks.GRASS, Blocks.DIRT, Blocks.STONE, 0.7f, 0.6f, 0.015f, 0.12f, 0.006f));
        registry.register((short) 2, "voxel:skyroot_forest", new BiomeType("voxel:skyroot_forest", Blocks.GRASS, Blocks.DIRT, Blocks.STONE, 0.65f, 0.8f, 0.08f, 0.2f, 0.005f));
        registry.register((short) 3, "voxel:sun_dunes", new BiomeType("voxel:sun_dunes", Blocks.SAND, Blocks.SAND, Blocks.STONE, 1.0f, 0.15f, 0.0f, 0.01f, 0.004f));
        registry.register((short) 4, "voxel:highlands", new BiomeType("voxel:highlands", Blocks.GRASS, Blocks.DIRT, Blocks.STONE, 0.4f, 0.45f, 0.01f, 0.05f, 0.004f));
        registry.register((short) 5, "voxel:frost_peaks", new BiomeType("voxel:frost_peaks", Blocks.SNOW, Blocks.GRAVEL, Blocks.STONE, 0.1f, 0.35f, 0.018f, 0.02f, 0.003f));
        registry.register((short) 6, "voxel:mire", new BiomeType("voxel:mire", Blocks.GRASS, Blocks.CLAY, Blocks.STONE, 0.58f, 0.95f, 0.035f, 0.24f, 0.006f));
        return registry;
    }
}
