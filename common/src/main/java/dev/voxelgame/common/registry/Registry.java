package dev.voxelgame.common.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Registry<T> {
    private final String name;
    private final Map<String, T> byKey = new LinkedHashMap<>();
    private final Map<Short, T> byId = new LinkedHashMap<>();

    public Registry(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public T register(short id, String key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (byKey.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate key in " + name + ": " + key);
        }
        if (byId.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate id in " + name + ": " + id);
        }
        byKey.put(key, value);
        byId.put(id, value);
        return value;
    }

    public Optional<T> findByKey(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    public Optional<T> findById(short id) {
        return Optional.ofNullable(byId.get(id));
    }

    public T requireByKey(String key) {
        return findByKey(key).orElseThrow(() -> new IllegalArgumentException("Missing " + name + " key: " + key));
    }

    public T requireById(short id) {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Missing " + name + " id: " + id));
    }

    public Collection<T> values() {
        return Collections.unmodifiableCollection(byKey.values());
    }

    public int size() {
        return byKey.size();
    }
}
