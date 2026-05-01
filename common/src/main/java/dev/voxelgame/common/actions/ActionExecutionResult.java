package dev.voxelgame.common.actions;

import java.util.List;
import java.util.Objects;

public record ActionExecutionResult(
        String actionKey,
        int transactionId,
        boolean accepted,
        ActionValidationResult validation,
        List<String> eventKeys
) {
    public ActionExecutionResult {
        Objects.requireNonNull(actionKey, "actionKey");
        Objects.requireNonNull(validation, "validation");
        eventKeys = List.copyOf(eventKeys);
        if (actionKey.isBlank()) {
            throw new IllegalArgumentException("Action key cannot be blank");
        }
        if (accepted != validation.accepted()) {
            throw new IllegalArgumentException("Execution result and validation result must agree");
        }
    }

    public static ActionExecutionResult accepted(ActionRequest request, List<String> eventKeys) {
        Objects.requireNonNull(request, "request");
        return new ActionExecutionResult(
                request.actionKey(),
                request.transactionId(),
                true,
                ActionValidationResult.acceptedResult(),
                eventKeys
        );
    }

    public static ActionExecutionResult rejected(ActionRequest request, ActionValidationResult validation) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(validation, "validation");
        if (validation.accepted()) {
            throw new IllegalArgumentException("Rejected execution result needs a rejected validation");
        }
        return new ActionExecutionResult(request.actionKey(), request.transactionId(), false, validation, List.of());
    }
}
