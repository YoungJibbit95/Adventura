package dev.voxelgame.common.gameplay;

import java.util.List;
import java.util.Objects;

public record JournalEntryDefinition(
        String key,
        JournalEntryKind kind,
        int order,
        String title,
        String summary,
        List<String> discoveryEventKeys,
        List<String> relatedItemKeys,
        List<String> relatedBlockKeys,
        List<String> relatedBiomeKeys,
        List<String> relatedStructureKeys,
        List<String> relatedEntityKeys,
        List<String> relatedRecipeKeys,
        List<AlphaMilestoneKey> milestoneKeys,
        String persistenceKey,
        String uiFeedback,
        String serverEventContract
) {
    public JournalEntryDefinition {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(summary, "summary");
        discoveryEventKeys = List.copyOf(discoveryEventKeys);
        relatedItemKeys = List.copyOf(relatedItemKeys);
        relatedBlockKeys = List.copyOf(relatedBlockKeys);
        relatedBiomeKeys = List.copyOf(relatedBiomeKeys);
        relatedStructureKeys = List.copyOf(relatedStructureKeys);
        relatedEntityKeys = List.copyOf(relatedEntityKeys);
        relatedRecipeKeys = List.copyOf(relatedRecipeKeys);
        milestoneKeys = List.copyOf(milestoneKeys);
        Objects.requireNonNull(persistenceKey, "persistenceKey");
        Objects.requireNonNull(uiFeedback, "uiFeedback");
        Objects.requireNonNull(serverEventContract, "serverEventContract");
        if (order < 1) {
            throw new IllegalArgumentException("Journal entry order must be positive");
        }
        if (key.isBlank() || title.isBlank() || summary.isBlank() || persistenceKey.isBlank()) {
            throw new IllegalArgumentException("Journal entry identity fields cannot be blank");
        }
        if (discoveryEventKeys.isEmpty()) {
            throw new IllegalArgumentException("Journal entry needs at least one discovery event: " + key);
        }
        if (uiFeedback.isBlank() || serverEventContract.isBlank()) {
            throw new IllegalArgumentException("Journal entry contract fields cannot be blank: " + key);
        }
    }
}
