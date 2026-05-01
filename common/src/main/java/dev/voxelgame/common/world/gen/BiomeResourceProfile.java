package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.block.Blocks;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record BiomeResourceProfile(
        String biomeKey,
        Set<Short> surfaceBlocks,
        Set<Short> vegetationBlocks,
        Set<Short> resourceBlocks,
        Set<Short> oreBlocks,
        List<String> structureKeys,
        List<String> ambientEntityKeys,
        Set<Short> rareFeatureBlocks,
        String tintKey,
        List<ResourceEntry> detailResources
) {
    public BiomeResourceProfile {
        Objects.requireNonNull(biomeKey, "biomeKey");
        Objects.requireNonNull(tintKey, "tintKey");
        surfaceBlocks = Set.copyOf(surfaceBlocks == null ? Set.of() : surfaceBlocks);
        vegetationBlocks = Set.copyOf(vegetationBlocks == null ? Set.of() : vegetationBlocks);
        resourceBlocks = Set.copyOf(resourceBlocks == null ? Set.of() : resourceBlocks);
        oreBlocks = Set.copyOf(oreBlocks == null ? Set.of() : oreBlocks);
        structureKeys = List.copyOf(structureKeys == null ? List.of() : structureKeys);
        ambientEntityKeys = List.copyOf(ambientEntityKeys == null ? List.of() : ambientEntityKeys);
        rareFeatureBlocks = Set.copyOf(rareFeatureBlocks == null ? Set.of() : rareFeatureBlocks);
        detailResources = List.copyOf(detailResources == null ? List.of() : detailResources);
        if (biomeKey.isBlank()) {
            throw new IllegalArgumentException("biome key must not be blank");
        }
        double previous = 0.0;
        for (ResourceEntry entry : detailResources) {
            if (entry.maxRollExclusive() <= previous) {
                throw new IllegalArgumentException("detail resource thresholds must be sorted ascending");
            }
            previous = entry.maxRollExclusive();
        }
    }

    public short detailResource(double roll) {
        if (!Double.isFinite(roll)) {
            return Blocks.AIR;
        }
        for (ResourceEntry entry : detailResources) {
            if (roll < entry.maxRollExclusive()) {
                return entry.blockId();
            }
        }
        return Blocks.AIR;
    }

    public record ResourceEntry(short blockId, double maxRollExclusive) {
        public ResourceEntry {
            if (!Double.isFinite(maxRollExclusive) || maxRollExclusive <= 0.0 || maxRollExclusive > 1.0) {
                throw new IllegalArgumentException("resource threshold must be within 0..1");
            }
        }
    }
}
