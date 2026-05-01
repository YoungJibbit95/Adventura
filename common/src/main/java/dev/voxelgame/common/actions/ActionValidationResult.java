package dev.voxelgame.common.actions;

import java.util.Objects;

public record ActionValidationResult(boolean accepted, RejectionReason reason, String message) {
    public enum RejectionReason {
        NONE,
        UNKNOWN_ACTION,
        NOT_LOGGED_IN,
        RATE_LIMITED,
        INVALID_SLOT,
        COOLDOWN,
        INVALID_ITEM,
        ACTION_MISMATCH,
        TARGET_MISMATCH,
        MISSING_HELD_ITEM,
        INVALID_HELD_ITEM,
        UNKNOWN_HELD_ITEM,
        MISSING_REQUIRED_TAG,
        INVALID_REQUEST
    }

    public ActionValidationResult {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(message, "message");
        if (accepted && reason != RejectionReason.NONE) {
            throw new IllegalArgumentException("Accepted validation result must use NONE");
        }
        if (!accepted && reason == RejectionReason.NONE) {
            throw new IllegalArgumentException("Rejected validation result needs a reason");
        }
        if (!accepted && message.isBlank()) {
            throw new IllegalArgumentException("Rejected validation result needs a message");
        }
    }

    public static ActionValidationResult acceptedResult() {
        return new ActionValidationResult(true, RejectionReason.NONE, "");
    }

    public static ActionValidationResult rejected(RejectionReason reason, String message) {
        return new ActionValidationResult(false, reason, message);
    }
}
