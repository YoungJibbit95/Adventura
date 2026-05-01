package dev.voxelgame.common.entity;

import dev.voxelgame.common.world.BiomeType;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkTerrainCache;
import dev.voxelgame.common.world.gen.BiomeResourceProfile;
import dev.voxelgame.common.world.gen.BiomeResourceProfiles;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.common.world.gen.ValueNoise;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AmbientEntitySpawner {
    private AmbientEntitySpawner() {
    }

    public static List<EntitySnapshot> spawnForChunk(long seed, OverworldGenerator generator, ChunkPos pos) {
        return spawnForChunk(seed, generator, pos, 12 * 60);
    }

    public static List<EntitySnapshot> spawnForChunk(long seed, OverworldGenerator generator, ChunkPos pos, int dayMinute) {
        int baseX = pos.x() * ChunkPos.SIZE;
        int baseZ = pos.z() * ChunkPos.SIZE;
        int x = baseX + 2 + (int) Math.floor(normalize(ValueNoise.hashUnit(seed ^ 0xA111BEEFL, pos.x(), pos.z())) * 12.0);
        int z = baseZ + 2 + (int) Math.floor(normalize(ValueNoise.hashUnit(seed ^ 0xB111CA7L, pos.x(), pos.z())) * 12.0);
        ChunkTerrainCache terrainCache = generator.terrainCacheForChunk(pos);
        BiomeType biome = terrainCache.biomeAtWorld(x, z);
        double roll = normalize(ValueNoise.hashUnit(seed ^ 0xE17171E5L, pos.x(), pos.z()));
        if (!AmbientSpawnRules.baseSpawnAllowed(seed, pos, biome, dayMinute, roll)) {
            return fireflySpawn(seed, pos, x, terrainCache.heightAtWorld(x, z) + 1, z, dayMinute, biome);
        }
        int y = terrainCache.heightAtWorld(x, z) + 1;
        Optional<String> maybeType = typeFor(seed, biome, pos, dayMinute);
        if (maybeType.isEmpty()) {
            return fireflySpawn(seed, pos, x, y, z, dayMinute, biome);
        }
        String type = maybeType.get();
        long entityId = ValueNoise.columnSeed(seed ^ 0xA11E17B0BL, pos.x(), pos.z());
        float yaw = (float) (normalize(ValueNoise.hashUnit(seed ^ 0xDADAL, pos.x(), pos.z())) * 360.0);
        List<EntitySnapshot> snapshots = new ArrayList<>();
        snapshots.add(new EntitySnapshot(
                entityId,
                type,
                null,
                x + 0.5,
                "voxel:firefly_swarm".equals(type) ? y + 1.35 : y,
                z + 0.5,
                yaw,
                0.0f,
                AmbientSpawnRules.healthFor(type),
                AmbientSpawnRules.initialStateFor(type)
        ));
        if (!"voxel:firefly_swarm".equals(type)) {
            snapshots.addAll(fireflySpawn(seed, pos, x, y, z, dayMinute, biome));
        }
        return snapshots;
    }

    public static List<EntitySnapshot> spawnAroundSpawn(long seed, int radius) {
        return spawnAroundSpawn(seed, radius, 12 * 60);
    }

    public static List<EntitySnapshot> spawnAroundSpawn(long seed, int radius, int dayMinute) {
        OverworldGenerator generator = new OverworldGenerator(seed);
        List<EntitySnapshot> snapshots = new ArrayList<>();
        for (int z = -radius; z <= radius; z++) {
            for (int x = -radius; x <= radius; x++) {
                snapshots.addAll(spawnForChunk(seed, generator, new ChunkPos(x, z), dayMinute));
            }
        }
        addSpawnAnchors(seed, generator, snapshots);
        return snapshots;
    }

    private static void addSpawnAnchors(long seed, OverworldGenerator generator, List<EntitySnapshot> snapshots) {
        addAnchor(seed, generator, snapshots, 12, 10, "voxel:cozy_sheep", 10);
        addAnchor(seed, generator, snapshots, -7, 13, "voxel:forest_bunny", 6);
        addAnchor(seed, generator, snapshots, 18, -5, "voxel:little_boar", 12);
        addAnchor(seed, generator, snapshots, 4, 23, "voxel:moss_snail", 6);
        addAnchor(seed, generator, snapshots, 16, 17, "voxel:firefly_swarm", 4);
    }

    private static void addAnchor(long seed, OverworldGenerator generator, List<EntitySnapshot> snapshots, int x, int z, String type, int health) {
        ChunkTerrainCache terrainCache = generator.terrainCacheForChunk(ChunkPos.fromBlock(x, z));
        int y = terrainCache.heightAtWorld(x, z) + 1;
        long entityId = ValueNoise.columnSeed(seed ^ 0x5AFE5A10L, x, z);
        double entityY = "voxel:firefly_swarm".equals(type) ? y + 1.35 : y;
        float yaw = (float) (normalize(ValueNoise.hashUnit(seed ^ 0xC05EFA11L, x, z)) * 360.0);
        snapshots.add(new EntitySnapshot(entityId, type, null, x + 0.5, entityY, z + 0.5, yaw, 0.0f, health, AmbientSpawnRules.initialStateFor(type)));
    }

    private static Optional<String> typeFor(long seed, BiomeType biome, ChunkPos pos, int dayMinute) {
        BiomeResourceProfile profile = BiomeResourceProfiles.forBiome(biome.key());
        return AmbientSpawnRules.selectType(profile.ambientEntityKeys(), seed, pos, dayMinute);
    }

    private static List<EntitySnapshot> fireflySpawn(long seed, ChunkPos pos, int x, int y, int z, int dayMinute, BiomeType biome) {
        double fireflyRoll = normalize(ValueNoise.hashUnit(seed ^ 0xF12EF11EL, pos.x(), pos.z()));
        if (!AmbientSpawnRules.fireflySwarmAllowed(seed, pos, biome, dayMinute, fireflyRoll)) {
            return List.of();
        }
        float yaw = (float) (normalize(ValueNoise.hashUnit(seed ^ 0xDADAL, pos.x(), pos.z())) * 360.0);
        long entityId = ValueNoise.columnSeed(seed ^ 0xA11E17B0BL, pos.x(), pos.z()) ^ 0xF11EF11EL;
        return List.of(new EntitySnapshot(
                entityId,
                "voxel:firefly_swarm",
                null,
                x + 1.5,
                y + 1.2,
                z + 1.5,
                yaw,
                0.0f,
                AmbientSpawnRules.healthFor("voxel:firefly_swarm"),
                AmbientSpawnRules.initialStateFor("voxel:firefly_swarm")
        ));
    }

    private static double normalize(double value) {
        return (value + 1.0) * 0.5;
    }
}
