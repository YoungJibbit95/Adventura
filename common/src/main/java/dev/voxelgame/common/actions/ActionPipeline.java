package dev.voxelgame.common.actions;

import dev.voxelgame.common.content.ContentTagRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ActionPipeline {
    private final Map<String, ActionDefinition> definitions;
    private final ContentTagRegistry contentTags;

    public ActionPipeline(Map<String, ActionDefinition> definitions, ContentTagRegistry contentTags) {
        Objects.requireNonNull(definitions, "definitions");
        this.contentTags = Objects.requireNonNull(contentTags, "contentTags");
        Map<String, ActionDefinition> copy = new LinkedHashMap<>();
        for (Map.Entry<String, ActionDefinition> entry : definitions.entrySet()) {
            ActionDefinition definition = Objects.requireNonNull(entry.getValue(), "definition");
            if (!entry.getKey().equals(definition.key())) {
                throw new IllegalArgumentException("Action definition map key does not match definition key: " + entry.getKey());
            }
            copy.put(entry.getKey(), definition);
        }
        this.definitions = Map.copyOf(copy);
    }

    public static ActionPipeline defaults(ContentTagRegistry contentTags) {
        return new ActionPipeline(ActionDefinitions.defaultMap(), contentTags);
    }

    public ActionValidationResult validate(ActionRequest request) {
        Objects.requireNonNull(request, "request");
        ActionDefinition definition = definitions.get(request.actionKey());
        if (definition == null) {
            return ActionValidationResult.rejected(
                    ActionValidationResult.RejectionReason.UNKNOWN_ACTION,
                    "Unknown action: " + request.actionKey()
            );
        }
        return definition.validate(request, contentTags);
    }

    public ActionExecutionResult acceptValidated(ActionRequest request, List<String> eventKeys) {
        ActionValidationResult validation = validate(request);
        if (!validation.accepted()) {
            return ActionExecutionResult.rejected(request, validation);
        }
        return ActionExecutionResult.accepted(request, eventKeys);
    }

    public Map<String, ActionDefinition> definitions() {
        return definitions;
    }
}
