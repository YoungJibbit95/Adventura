package dev.voxelgame.client.render.entity;

import java.util.List;

public record EntityModel(String key, List<EntityModelPart> parts, boolean ambientBob) {
    public EntityModel {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Entity model key must not be blank");
        }
        parts = List.copyOf(parts);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Entity model must contain at least one part");
        }
    }

    public int partCount() {
        return parts.size();
    }
}
