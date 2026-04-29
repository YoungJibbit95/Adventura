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
        registry.register((short) 7, "voxel:cozy_meadow", new BiomeType("voxel:cozy_meadow", Blocks.GRASS, Blocks.DIRT, Blocks.STONE, 0.72f, 0.58f, 0.018f, 0.18f, 0.006f));
        registry.register((short) 8, "voxel:pine_forest", new BiomeType("voxel:pine_forest", Blocks.GRASS, Blocks.DIRT, Blocks.STONE, 0.42f, 0.72f, 0.105f, 0.14f, 0.005f));
        registry.register((short) 9, "voxel:mushroom_grove", new BiomeType("voxel:mushroom_grove", Blocks.GRASS, Blocks.CLAY, Blocks.STONE, 0.54f, 0.88f, 0.045f, 0.32f, 0.009f));
        registry.register((short) 10, "voxel:lakeside", new BiomeType("voxel:lakeside", Blocks.GRASS, Blocks.CLAY, Blocks.STONE, 0.66f, 0.86f, 0.028f, 0.20f, 0.006f));
        registry.register((short) 11, "voxel:old_ruins", new BiomeType("voxel:old_ruins", Blocks.MOSSY_STONE, Blocks.GRAVEL, Blocks.STONE, 0.50f, 0.60f, 0.012f, 0.10f, 0.018f));
        registry.register((short) 12, "voxel:flower_fields", new BiomeType("voxel:flower_fields", Blocks.GRASS, Blocks.DIRT, Blocks.STONE, 0.74f, 0.62f, 0.010f, 0.36f, 0.005f));
        return registry;
    }
}
