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
        if (roll > 0.30) {
            return List.of();
        }

        int baseX = pos.x() * ChunkPos.SIZE;
        int baseZ = pos.z() * ChunkPos.SIZE;
        int x = baseX + 2 + (int) Math.floor(normalize(ValueNoise.hashUnit(seed ^ 0xA111BEEFL, pos.x(), pos.z())) * 12.0);
        int z = baseZ + 2 + (int) Math.floor(normalize(ValueNoise.hashUnit(seed ^ 0xB111CA7L, pos.x(), pos.z())) * 12.0);
        BiomeType biome = generator.biomeAt(x, z);
        int y = generator.terrainHeight(x, z, biome) + 1;
        String type = typeFor(seed, biome, pos);
        long entityId = ValueNoise.columnSeed(seed ^ 0xA11E17B0BL, pos.x(), pos.z());
        float yaw = (float) (normalize(ValueNoise.hashUnit(seed ^ 0xDADAL, pos.x(), pos.z())) * 360.0);
        List<EntitySnapshot> snapshots = new ArrayList<>();
        snapshots.add(new EntitySnapshot(entityId, type, null, x + 0.5, y, z + 0.5, yaw, 0.0f, 10));
        double fireflyRoll = normalize(ValueNoise.hashUnit(seed ^ 0xF12EF11EL, pos.x(), pos.z()));
        if (fireflyRoll < 0.08 && ("voxel:lakeside".equals(biome.key()) || "voxel:mushroom_grove".equals(biome.key()) || "voxel:cozy_meadow".equals(biome.key()))) {
            snapshots.add(new EntitySnapshot(entityId ^ 0xF11EF11EL, "voxel:firefly_swarm", null, x + 1.5, y + 1.2, z + 1.5, yaw, 0.0f, 4));
        }
        return snapshots;
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

    private static String typeFor(long seed, BiomeType biome, ChunkPos pos) {
        double variant = normalize(ValueNoise.hashUnit(seed ^ 0xBEE5A11L, pos.x(), pos.z()));
        return switch (biome.key()) {
            case "voxel:frost_peaks" -> "voxel:forest_bunny";
            case "voxel:mire", "voxel:mushroom_grove" -> variant < 0.55 ? "voxel:moss_snail" : "voxel:firefly_swarm";
            case "voxel:sun_dunes" -> "voxel:little_boar";
            case "voxel:skyroot_forest", "voxel:pine_forest" -> variant < 0.60 ? "voxel:forest_bunny" : "voxel:little_boar";
            default -> variant < 0.50 ? "voxel:cozy_sheep" : "voxel:forest_bunny";
        };
    }

    private static double normalize(double value) {
        return (value + 1.0) * 0.5;
    }
}
