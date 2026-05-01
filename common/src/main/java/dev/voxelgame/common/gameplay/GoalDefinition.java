package dev.voxelgame.common.gameplay;

import java.util.List;
import java.util.Objects;

public record GoalDefinition(
        String key,
        int order,
        String title,
        String summary,
        List<AlphaMilestoneKey> milestoneKeys,
        List<String> journalEntryKeys,
        String persistenceKey,
        String uiFeedback,
        String serverEventContract
) {
    public GoalDefinition {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(summary, "summary");
        milestoneKeys = List.copyOf(milestoneKeys);
        journalEntryKeys = List.copyOf(journalEntryKeys);
        Objects.requireNonNull(persistenceKey, "persistenceKey");
        Objects.requireNonNull(uiFeedback, "uiFeedback");
        Objects.requireNonNull(serverEventContract, "serverEventContract");
        if (order < 1) {
            throw new IllegalArgumentException("Goal order must be positive");
        }
        if (key.isBlank() || title.isBlank() || summary.isBlank() || persistenceKey.isBlank()) {
            throw new IllegalArgumentException("Goal identity fields cannot be blank");
        }
        if (milestoneKeys.isEmpty()) {
            throw new IllegalArgumentException("Goal needs at least one milestone: " + key);
        }
        if (uiFeedback.isBlank() || serverEventContract.isBlank()) {
            throw new IllegalArgumentException("Goal contract fields cannot be blank: " + key);
        }
    }
}
