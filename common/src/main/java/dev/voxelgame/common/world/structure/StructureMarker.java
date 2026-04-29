package dev.voxelgame.common.world.structure;

import java.util.Objects;

public record StructureMarker(String type, String key, int x, int y, int z) {
    public StructureMarker {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(key, "key");
        if (type.isBlank()) {
            throw new IllegalArgumentException("Marker type must not be blank");
        }
        if (key.isBlank()) {
            throw new IllegalArgumentException("Marker key must not be blank");
        }
    }

    public static StructureMarker loot(String key, int x, int y, int z) {
        return new StructureMarker("loot", key, x, y, z);
    }

    public static StructureMarker entity(String key, int x, int y, int z) {
        return new StructureMarker("entity", key, x, y, z);
    }

    public static StructureMarker metadata(String key, int x, int y, int z) {
        return new StructureMarker("metadata", key, x, y, z);
    }
}
