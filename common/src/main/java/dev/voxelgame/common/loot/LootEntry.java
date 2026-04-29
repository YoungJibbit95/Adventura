package dev.voxelgame.common.loot;

import java.util.Objects;

public record LootEntry(String itemKey, int minCount, int maxCount, double chance) {
    public LootEntry {
        Objects.requireNonNull(itemKey, "itemKey");
        if (itemKey.isBlank()) {
            throw new IllegalArgumentException("Loot item key must not be blank");
        }
        if (minCount < 1) {
            throw new IllegalArgumentException("Loot min count must be >= 1");
        }
        if (maxCount < minCount) {
            throw new IllegalArgumentException("Loot max count must be >= min count");
        }
        if (chance < 0.0 || chance > 1.0) {
            throw new IllegalArgumentException("Loot chance must be in 0..1");
        }
    }
}
