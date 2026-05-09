package dev.voxelgame.common.entity;

import dev.voxelgame.common.world.BiomeType;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.gen.ValueNoise;

import java.util.List;
import java.util.Optional;

public final class AmbientSpawnRules {
    public static final int REGION_SIZE_CHUNKS = 4;
    public static final int MAX_BASE_SPAWNS_PER_REGION = 8;
    public static final int MAX_FIREFLY_SWARMS_PER_REGION = 2;
    public static final int DAY_START_MINUTE = 6 * 60;
    public static final int DUSK_START_MINUTE = 17 * 60;
    public static final int NIGHT_START_MINUTE = 19 * 60;

    private static final long BASE_BUDGET_SALT = 0x5F4A7710BADC0DEL;
    private static final long FIREFLY_BUDGET_SALT = 0xF11EF17E5A11L;

    private AmbientSpawnRules() {
    }

    public static boolean baseSpawnAllowed(long seed, ChunkPos pos, BiomeType biome, int dayMinute, double roll) {
        return withinRegionBudget(seed, pos, MAX_BASE_SPAWNS_PER_REGION, BASE_BUDGET_SALT)
                && roll < baseSpawnChance(biome, dayMinute);
    }

    public static boolean fireflySwarmAllowed(long seed, ChunkPos pos, BiomeType biome, int dayMinute, double roll) {
        return isNight(dayMinute)
                && supportsFireflies(biome)
                && withinRegionBudget(seed, pos, MAX_FIREFLY_SWARMS_PER_REGION, FIREFLY_BUDGET_SALT)
                && roll < 0.16;
    }

    public static Optional<String> selectType(List<String> candidates, long seed, ChunkPos pos, int dayMinute) {
        if (candidates == null || candidates.isEmpty()) {
            return isDayOrEdge(dayMinute) ? Optional.of("voxel:cozy_sheep") : Optional.of("voxel:moss_snail");
        }
        int start = Math.min(candidates.size() - 1, (int) Math.floor(normalize(ValueNoise.hashUnit(seed ^ 0xBEE5A11L, pos.x(), pos.z())) * candidates.size()));
        for (int offset = 0; offset < candidates.size(); offset++) {
            String candidate = candidates.get((start + offset) % candidates.size());
            if (allowedAtMinute(candidate, dayMinute)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public static int healthFor(String typeKey) {
        return switch (typeKey) {
            case "voxel:firefly_swarm" -> 4;
            case "voxel:forest_bunny", "voxel:moss_snail" -> 6;
            case "voxel:little_boar" -> 12;
            default -> 10;
        };
    }

    public static String initialStateFor(String typeKey) {
        return switch (typeKey) {
            case "voxel:firefly_swarm" -> EntitySnapshot.STATE_WANDER;
            case "voxel:cozy_sheep" -> EntitySnapshot.STATE_GRAZE;
            default -> EntitySnapshot.STATE_IDLE;
        };
    }

    public static double spawnCooldownSeconds(String typeKey) {
        return switch (typeKey) {
            case "voxel:firefly_swarm" -> 45.0;
            case "voxel:little_boar" -> 36.0;
            case "voxel:moss_snail" -> 28.0;
            default -> 18.0;
        };
    }

    static double baseSpawnChance(BiomeType biome, int dayMinute) {
        String key = biome == null ? "" : biome.key();
        double chance = 0.34;
        if (key.contains("meadow") || key.contains("flower")) {
            chance = 0.46;
        } else if (key.contains("forest")) {
            chance = 0.40;
        } else if (key.contains("mire") || key.contains("mushroom") || key.contains("lakeside")) {
            chance = 0.32;
        } else if (key.contains("ruins") || key.contains("dunes") || key.contains("frost")) {
            chance = 0.22;
        }
        if (isNight(dayMinute)) {
            chance *= key.contains("mire") || key.contains("mushroom") || key.contains("lakeside") ? 0.82 : 0.36;
        }
        return chance;
    }

    static boolean allowedAtMinute(String typeKey, int dayMinute) {
        if ("voxel:firefly_swarm".equals(typeKey)) {
            return isNight(dayMinute);
        }
        if ("voxel:moss_snail".equals(typeKey)) {
            return true;
        }
        return isDayOrEdge(dayMinute);
    }

    static boolean withinRegionBudget(long seed, ChunkPos pos, int maxSpawns, long salt) {
        if (maxSpawns <= 0) {
            return false;
        }
        int regionX = Math.floorDiv(pos.x(), REGION_SIZE_CHUNKS) * REGION_SIZE_CHUNKS;
        int regionZ = Math.floorDiv(pos.z(), REGION_SIZE_CHUNKS) * REGION_SIZE_CHUNKS;
        double score = budgetScore(seed, pos, salt);
        int rank = 0;
        for (int z = regionZ; z < regionZ + REGION_SIZE_CHUNKS; z++) {
            for (int x = regionX; x < regionX + REGION_SIZE_CHUNKS; x++) {
                ChunkPos candidate = new ChunkPos(x, z);
                if (budgetScore(seed, candidate, salt) < score) {
                    rank++;
                }
            }
        }
        return rank < maxSpawns;
    }

    private static boolean supportsFireflies(BiomeType biome) {
        if (biome == null) {
            return false;
        }
        String key = biome.key();
        return key.contains("lakeside") || key.contains("mushroom") || key.contains("mire") || key.contains("cozy_meadow");
    }

    private static boolean isNight(int dayMinute) {
        int minute = Math.floorMod(dayMinute, 24 * 60);
        return minute < DAY_START_MINUTE || minute >= NIGHT_START_MINUTE;
    }

    private static boolean isDayOrEdge(int dayMinute) {
        int minute = Math.floorMod(dayMinute, 24 * 60);
        return minute >= DAY_START_MINUTE && minute < DUSK_START_MINUTE;
    }

    private static double budgetScore(long seed, ChunkPos pos, long salt) {
        return normalize(ValueNoise.hashUnit(seed ^ salt, pos.x(), pos.z()));
    }

    private static double normalize(double value) {
        return (value + 1.0) * 0.5;
    }
}
