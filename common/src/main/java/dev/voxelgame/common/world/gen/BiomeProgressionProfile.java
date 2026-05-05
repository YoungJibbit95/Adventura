package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.gameplay.AlphaMilestoneKey;
import dev.voxelgame.common.gameplay.status.StatusEffectType;

import java.util.List;
import java.util.Objects;

public record BiomeProgressionProfile(
        String biomeKey,
        BiomeProgressionTier tier,
        boolean coreProgression,
        String silhouette,
        String colorLightMood,
        List<Short> resourceBlockIds,
        List<String> ambientEntityKeys,
        List<String> structureKeys,
        List<String> dangerNotes,
        List<StatusEffectType> environmentEffectTypes,
        List<AlphaMilestoneKey> milestoneKeys,
        List<String> itemProgressionKeys,
        List<String> journalEntryKeys,
        List<String> returnReasons,
        String seedRobustnessContract
) {
    public BiomeProgressionProfile {
        Objects.requireNonNull(biomeKey, "biomeKey");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(silhouette, "silhouette");
        Objects.requireNonNull(colorLightMood, "colorLightMood");
        Objects.requireNonNull(seedRobustnessContract, "seedRobustnessContract");
        resourceBlockIds = List.copyOf(resourceBlockIds);
        ambientEntityKeys = List.copyOf(ambientEntityKeys);
        structureKeys = List.copyOf(structureKeys);
        dangerNotes = List.copyOf(dangerNotes);
        environmentEffectTypes = List.copyOf(environmentEffectTypes);
        milestoneKeys = List.copyOf(milestoneKeys);
        itemProgressionKeys = List.copyOf(itemProgressionKeys);
        journalEntryKeys = List.copyOf(journalEntryKeys);
        returnReasons = List.copyOf(returnReasons);
        if (biomeKey.isBlank() || silhouette.isBlank() || colorLightMood.isBlank()) {
            throw new IllegalArgumentException("Biome progression cards need key, silhouette and mood");
        }
        if (resourceBlockIds.isEmpty() || ambientEntityKeys.isEmpty() || structureKeys.isEmpty()) {
            throw new IllegalArgumentException("Biome progression cards need resource, creature and structure anchors: " + biomeKey);
        }
        if (dangerNotes.isEmpty() || returnReasons.isEmpty()) {
            throw new IllegalArgumentException("Biome progression cards need danger notes and return reasons: " + biomeKey);
        }
        if (coreProgression && (milestoneKeys.isEmpty() || seedRobustnessContract.isBlank())) {
            throw new IllegalArgumentException("Core progression biomes need milestones and seed-robustness contract: " + biomeKey);
        }
    }
}
