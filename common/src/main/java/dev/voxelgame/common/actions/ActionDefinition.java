package dev.voxelgame.common.actions;

import dev.voxelgame.common.content.ContentKind;
import dev.voxelgame.common.content.ContentTag;
import dev.voxelgame.common.content.ContentTagRegistry;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record ActionDefinition(
        String key,
        ActionTargetType targetType,
        Set<ContentTag> requiredHeldItemTags,
        ActionCost cost,
        ActionCooldown cooldown,
        boolean inventoryTransaction,
        String resultEventKey
) {
    public ActionDefinition {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(requiredHeldItemTags, "requiredHeldItemTags");
        Objects.requireNonNull(cost, "cost");
        Objects.requireNonNull(cooldown, "cooldown");
        Objects.requireNonNull(resultEventKey, "resultEventKey");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Action key cannot be blank");
        }
        if (resultEventKey.isBlank()) {
            throw new IllegalArgumentException("Result event key cannot be blank");
        }
        EnumSet<ContentTag> tags = requiredHeldItemTags.isEmpty()
                ? EnumSet.noneOf(ContentTag.class)
                : EnumSet.copyOf(requiredHeldItemTags);
        requiredHeldItemTags = Set.copyOf(tags);
    }

    public ActionValidationResult validate(ActionRequest request, ContentTagRegistry contentTags) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(contentTags, "contentTags");
        if (!key.equals(request.actionKey())) {
            return ActionValidationResult.rejected(
                    ActionValidationResult.RejectionReason.ACTION_MISMATCH,
                    "Request action does not match definition: " + request.actionKey()
            );
        }
        if (request.target().type() != targetType) {
            return ActionValidationResult.rejected(
                    ActionValidationResult.RejectionReason.TARGET_MISMATCH,
                    "Action " + key + " requires target " + targetType + " but got " + request.target().type()
            );
        }
        if (!requiredHeldItemTags.isEmpty()) {
            if (request.heldItemKey().isEmpty()) {
                return ActionValidationResult.rejected(
                        ActionValidationResult.RejectionReason.MISSING_HELD_ITEM,
                        "Action " + key + " requires a held item"
                );
            }
            if (request.heldItemKey().orElseThrow().kind() != ContentKind.ITEM) {
                return ActionValidationResult.rejected(
                        ActionValidationResult.RejectionReason.INVALID_HELD_ITEM,
                        "Action " + key + " requires an item key"
                );
            }
            Set<ContentTag> heldTags = contentTags.tagsFor(request.heldItemKey().orElseThrow());
            if (heldTags.isEmpty()) {
                return ActionValidationResult.rejected(
                        ActionValidationResult.RejectionReason.UNKNOWN_HELD_ITEM,
                        "Unknown held item for action " + key
                );
            }
            for (ContentTag tag : requiredHeldItemTags) {
                if (!heldTags.contains(tag)) {
                    return ActionValidationResult.rejected(
                            ActionValidationResult.RejectionReason.MISSING_REQUIRED_TAG,
                            "Action " + key + " requires held item tag " + tag.key()
                    );
                }
            }
        }
        return ActionValidationResult.acceptedResult();
    }
}
