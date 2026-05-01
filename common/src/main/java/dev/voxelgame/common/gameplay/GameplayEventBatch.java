package dev.voxelgame.common.gameplay;

import java.util.List;
import java.util.Objects;

public record GameplayEventBatch(int schemaVersion, List<GameplayEvent> events) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int MAX_EVENTS = 256;

    public GameplayEventBatch {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported gameplay event schema: " + schemaVersion);
        }
        Objects.requireNonNull(events, "events");
        if (events.size() > MAX_EVENTS) {
            throw new IllegalArgumentException("Too many gameplay events in one batch: " + events.size());
        }
        events = List.copyOf(events);
        for (GameplayEvent event : events) {
            Objects.requireNonNull(event, "event");
        }
    }

    public static GameplayEventBatch of(List<GameplayEvent> events) {
        return new GameplayEventBatch(CURRENT_SCHEMA_VERSION, events);
    }
}
