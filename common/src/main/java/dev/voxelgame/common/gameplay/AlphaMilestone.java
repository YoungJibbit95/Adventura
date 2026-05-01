package dev.voxelgame.common.gameplay;

import java.util.List;
import java.util.Objects;

public record AlphaMilestone(
        AlphaMilestoneKey key,
        int order,
        String title,
        String trigger,
        List<String> requiredItemKeys,
        List<String> requiredBlockKeys,
        List<String> requiredBiomeKeys,
        List<String> requiredStructureKeys,
        String uiFeedback,
        String saveStateKey,
        String serverValidation,
        String smokeCheck
) {
    public AlphaMilestone {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(trigger, "trigger");
        requiredItemKeys = List.copyOf(requiredItemKeys);
        requiredBlockKeys = List.copyOf(requiredBlockKeys);
        requiredBiomeKeys = List.copyOf(requiredBiomeKeys);
        requiredStructureKeys = List.copyOf(requiredStructureKeys);
        Objects.requireNonNull(uiFeedback, "uiFeedback");
        Objects.requireNonNull(saveStateKey, "saveStateKey");
        Objects.requireNonNull(serverValidation, "serverValidation");
        Objects.requireNonNull(smokeCheck, "smokeCheck");
        if (order < 1) {
            throw new IllegalArgumentException("Milestone order must be positive");
        }
        if (title.isBlank() || trigger.isBlank() || saveStateKey.isBlank()) {
            throw new IllegalArgumentException("Milestone text fields cannot be blank");
        }
    }
}
