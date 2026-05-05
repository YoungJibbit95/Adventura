package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.block.Blocks;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record FeatureEntry(
        String key,
        FeatureKind kind,
        double weight,
        int minCount,
        int maxCount,
        int spacingBlocks,
        int maxSlopeBlocks,
        Set<String> allowedBiomeKeys,
        Set<Short> requiredSurfaceBlockIds,
        boolean avoidWater,
        boolean avoidStructure,
        double placementChance,
        double rareChance,
        short blockId,
        String structureKey,
        String lootTableKey,
        String encounterTableKey,
        String journalKey
) {
    public FeatureEntry {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(kind, "kind");
        allowedBiomeKeys = Set.copyOf(allowedBiomeKeys);
        requiredSurfaceBlockIds = Set.copyOf(requiredSurfaceBlockIds);
        structureKey = structureKey == null ? "" : structureKey;
        lootTableKey = lootTableKey == null ? "" : lootTableKey;
        encounterTableKey = encounterTableKey == null ? "" : encounterTableKey;
        journalKey = journalKey == null ? "" : journalKey;
        if (key.isBlank()) {
            throw new IllegalArgumentException("Feature key cannot be blank");
        }
        if (!Double.isFinite(weight) || weight <= 0.0) {
            throw new IllegalArgumentException("Feature weight must be positive: " + key);
        }
        if (minCount < 0 || maxCount < minCount) {
            throw new IllegalArgumentException("Feature count range is invalid: " + key);
        }
        if (spacingBlocks < 0 || maxSlopeBlocks < 0) {
            throw new IllegalArgumentException("Feature spacing/slope must be non-negative: " + key);
        }
        if (allowedBiomeKeys.isEmpty() || requiredSurfaceBlockIds.isEmpty()) {
            throw new IllegalArgumentException("Feature needs allowed biomes and surfaces: " + key);
        }
        if (!Double.isFinite(placementChance) || placementChance < 0.0 || placementChance > 1.0
                || !Double.isFinite(rareChance) || rareChance < 0.0 || rareChance > 1.0) {
            throw new IllegalArgumentException("Feature chances must be within 0..1: " + key);
        }
        if (blockId == Blocks.AIR && structureKey.isBlank()) {
            throw new IllegalArgumentException("Feature needs a block or structure target: " + key);
        }
    }

    public boolean allowsBiome(String biomeKey) {
        return allowedBiomeKeys.contains(biomeKey);
    }

    public boolean placesBlock() {
        return blockId != Blocks.AIR;
    }

    public boolean placesStructure() {
        return !structureKey.isBlank();
    }

    public static FeatureEntry block(
            String key,
            FeatureKind kind,
            short blockId,
            double placementChance,
            double weight,
            int minCount,
            int maxCount,
            int spacingBlocks,
            int maxSlopeBlocks,
            Set<String> allowedBiomeKeys,
            Set<Short> requiredSurfaceBlockIds,
            boolean avoidWater,
            boolean avoidStructure,
            double rareChance,
            String journalKey
    ) {
        return new FeatureEntry(
                key,
                kind,
                weight,
                minCount,
                maxCount,
                spacingBlocks,
                maxSlopeBlocks,
                allowedBiomeKeys,
                requiredSurfaceBlockIds,
                avoidWater,
                avoidStructure,
                placementChance,
                rareChance,
                blockId,
                "",
                "",
                "",
                journalKey
        );
    }

    public static FeatureEntry structure(
            String key,
            FeatureKind kind,
            String structureKey,
            double placementChance,
            double weight,
            int spacingBlocks,
            Set<String> allowedBiomeKeys,
            Set<Short> requiredSurfaceBlockIds,
            boolean avoidWater,
            String lootTableKey,
            String encounterTableKey,
            String journalKey
    ) {
        return new FeatureEntry(
                key,
                kind,
                weight,
                1,
                1,
                spacingBlocks,
                2,
                allowedBiomeKeys,
                requiredSurfaceBlockIds,
                avoidWater,
                true,
                placementChance,
                placementChance,
                Blocks.AIR,
                structureKey,
                lootTableKey,
                encounterTableKey,
                journalKey
        );
    }

    public static Set<String> biomes(String... keys) {
        return Set.copyOf(List.of(keys));
    }

    public static Set<Short> surfaces(short... blockIds) {
        Set<Short> surfaces = new java.util.LinkedHashSet<>();
        for (short blockId : blockIds) {
            surfaces.add(blockId);
        }
        return Set.copyOf(surfaces);
    }
}
