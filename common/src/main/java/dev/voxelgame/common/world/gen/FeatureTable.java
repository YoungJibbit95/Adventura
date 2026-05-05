package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.block.Blocks;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record FeatureTable(String key, List<FeatureEntry> entries) {
    public FeatureTable {
        Objects.requireNonNull(key, "key");
        entries = List.copyOf(entries);
        if (key.isBlank()) {
            throw new IllegalArgumentException("Feature table key cannot be blank");
        }
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Feature table cannot be empty: " + key);
        }
        Set<String> entryKeys = new LinkedHashSet<>();
        double previousBlockChance = 0.0;
        for (FeatureEntry entry : entries) {
            if (!entryKeys.add(entry.key())) {
                throw new IllegalArgumentException("Duplicate feature entry in " + key + ": " + entry.key());
            }
            if (entry.placesBlock() && entry.placementChance() > 0.0) {
                if (entry.placementChance() <= previousBlockChance) {
                    throw new IllegalArgumentException("Block feature placement chances must be sorted: " + key);
                }
                previousBlockChance = entry.placementChance();
            }
        }
    }

    public List<FeatureEntry> entriesForBiome(String biomeKey) {
        return entries.stream()
                .filter(entry -> entry.allowsBiome(biomeKey))
                .toList();
    }

    public Optional<FeatureEntry> blockEntryForRoll(String biomeKey, double roll) {
        if (!Double.isFinite(roll)) {
            return Optional.empty();
        }
        return entries.stream()
                .filter(FeatureEntry::placesBlock)
                .filter(entry -> entry.allowsBiome(biomeKey))
                .filter(entry -> roll < entry.placementChance())
                .findFirst();
    }

    public short blockForRoll(String biomeKey, double roll) {
        return blockEntryForRoll(biomeKey, roll)
                .map(FeatureEntry::blockId)
                .orElse(Blocks.AIR);
    }
}
