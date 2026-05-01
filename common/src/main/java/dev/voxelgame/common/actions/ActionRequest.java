package dev.voxelgame.common.actions;

import dev.voxelgame.common.content.ContentKey;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ActionRequest(
        String actionKey,
        UUID actorId,
        Optional<ContentKey> heldItemKey,
        int selectedSlot,
        ActionTarget target,
        int transactionId,
        long sequence
) {
    public ActionRequest {
        Objects.requireNonNull(actionKey, "actionKey");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(heldItemKey, "heldItemKey");
        Objects.requireNonNull(target, "target");
        if (actionKey.isBlank()) {
            throw new IllegalArgumentException("Action key cannot be blank");
        }
        if (selectedSlot < -1) {
            throw new IllegalArgumentException("Selected slot must be >= -1");
        }
        heldItemKey = heldItemKey.map(key -> {
            Objects.requireNonNull(key, "heldItemKey value");
            return key;
        });
    }

    public static ActionRequest of(
            String actionKey,
            UUID actorId,
            ContentKey heldItemKey,
            int selectedSlot,
            ActionTarget target,
            int transactionId,
            long sequence
    ) {
        return new ActionRequest(actionKey, actorId, Optional.ofNullable(heldItemKey), selectedSlot, target, transactionId, sequence);
    }
}
