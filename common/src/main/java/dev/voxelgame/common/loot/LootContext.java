package dev.voxelgame.common.loot;

import java.util.Objects;

public record LootContext(long worldSeed, String structureKey, String markerKey, int worldX, int worldY, int worldZ) {
    public LootContext {
        Objects.requireNonNull(structureKey, "structureKey");
        Objects.requireNonNull(markerKey, "markerKey");
        if (structureKey.isBlank()) {
            throw new IllegalArgumentException("Structure key must not be blank");
        }
        if (markerKey.isBlank()) {
            throw new IllegalArgumentException("Marker key must not be blank");
        }
    }
}
