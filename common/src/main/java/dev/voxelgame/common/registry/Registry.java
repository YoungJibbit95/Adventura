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
    private final Map<String, String> aliases = new LinkedHashMap<>();
    private final Map<Short, T> byId = new LinkedHashMap<>();

    public Registry(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public T register(short id, String key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (byKey.containsKey(key) || aliases.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate key in " + name + ": " + key);
        }
        if (byId.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate id in " + name + ": " + id);
        }
        byKey.put(key, value);
        byId.put(id, value);
        return value;
    }

    public void registerAlias(String aliasKey, String targetKey) {
        Objects.requireNonNull(aliasKey, "aliasKey");
        Objects.requireNonNull(targetKey, "targetKey");
        if (byKey.containsKey(aliasKey) || aliases.containsKey(aliasKey)) {
            throw new IllegalArgumentException("Duplicate key in " + name + ": " + aliasKey);
        }
        if (!byKey.containsKey(targetKey)) {
            throw new IllegalArgumentException("Missing " + name + " key for alias target: " + targetKey);
        }
        aliases.put(aliasKey, targetKey);
    }

    public Optional<T> findByKey(String key) {
        T value = byKey.get(key);
        if (value != null) {
            return Optional.of(value);
        }
        String targetKey = aliases.get(key);
        return targetKey == null ? Optional.empty() : Optional.ofNullable(byKey.get(targetKey));
    }

    public Optional<String> canonicalKey(String key) {
        if (byKey.containsKey(key)) {
            return Optional.of(key);
        }
        return Optional.ofNullable(aliases.get(key));
    }

    public Map<String, String> aliases() {
        return Collections.unmodifiableMap(aliases);
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
