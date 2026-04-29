package dev.voxelgame.common.loot;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class LootTableRegistry {
    private final Map<String, LootTable> byKey = new LinkedHashMap<>();

    public LootTable register(LootTable table) {
        Objects.requireNonNull(table, "table");
        if (byKey.containsKey(table.key())) {
            throw new IllegalArgumentException("Duplicate loot table key: " + table.key());
        }
        byKey.put(table.key(), table);
        return table;
    }

    public Optional<LootTable> findByKey(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    public LootTable requireByKey(String key) {
        return findByKey(key).orElseThrow(() -> new IllegalArgumentException("Missing loot table key: " + key));
    }

    public Collection<LootTable> values() {
        return Collections.unmodifiableCollection(byKey.values());
    }
}
