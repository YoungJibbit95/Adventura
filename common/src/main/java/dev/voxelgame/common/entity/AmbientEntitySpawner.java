package dev.voxelgame.common.entity;

import dev.voxelgame.common.world.BiomeType;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.common.world.gen.ValueNoise;

import java.util.ArrayList;
import java.util.List;

public final class AmbientEntitySpawner {
    private AmbientEntitySpawner() {
    }

    public static List<EntitySnapshot> spawnForChunk(long seed, OverworldGenerator generator, ChunkPos pos) {
        double roll = normalize(ValueNoise.hashUnit(seed ^ 0xE17171E5L, pos.x(), pos.z()));
        if (roll > 0.22) {
            return List.of();
        }

        int baseX = pos.x() * ChunkPos.SIZE;
        int baseZ = pos.z() * ChunkPos.SIZE;
        int x = baseX + 2 + (int) Math.floor(normalize(ValueNoise.hashUnit(seed ^ 0xA111BEEFL, pos.x(), pos.z())) * 12.0);
        int z = baseZ + 2 + (int) Math.floor(normalize(ValueNoise.hashUnit(seed ^ 0xB111CA7L, pos.x(), pos.z())) * 12.0);
        BiomeType biome = generator.biomeAt(x, z);
        int y = generator.terrainHeight(x, z, biome) + 1;
        String type = typeFor(biome);
        long entityId = ValueNoise.columnSeed(seed ^ 0xA11E17B0BL, pos.x(), pos.z());
        float yaw = (float) (normalize(ValueNoise.hashUnit(seed ^ 0xDADAL, pos.x(), pos.z())) * 360.0);
        return List.of(new EntitySnapshot(entityId, type, null, x + 0.5, y, z + 0.5, yaw, 0.0f, 10));
    }

    public static List<EntitySnapshot> spawnAroundSpawn(long seed, int radius) {
        OverworldGenerator generator = new OverworldGenerator(seed);
        List<EntitySnapshot> snapshots = new ArrayList<>();
        for (int z = -radius; z <= radius; z++) {
            for (int x = -radius; x <= radius; x++) {
                snapshots.addAll(spawnForChunk(seed, generator, new ChunkPos(x, z)));
            }
        }
        return snapshots;
    }

    private static String typeFor(BiomeType biome) {
        return switch (biome.key()) {
            case "voxel:frost_peaks" -> "voxel:snow_hare";
            case "voxel:mire" -> "voxel:mire_wisp";
            case "voxel:sun_dunes" -> "voxel:dune_crawler";
            case "voxel:skyroot_forest" -> "voxel:forest_grazer";
            default -> "voxel:meadow_grazer";
        };
    }

    private static double normalize(double value) {
        return (value + 1.0) * 0.5;
    }
}
