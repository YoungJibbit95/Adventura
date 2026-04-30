package dev.voxelgame.common.entity;

import java.util.Objects;
import java.util.Optional;

public final class ItemDropType {
    public static final String PREFIX = "voxel:item_drop:";

    private ItemDropType() {
    }

    public static String typeKey(String itemKey) {
        Objects.requireNonNull(itemKey, "itemKey");
        if (itemKey.isBlank() || itemKey.indexOf(':') < 1) {
            throw new IllegalArgumentException("Item key must be namespaced");
        }
        return PREFIX + itemKey.replace(':', '.');
    }

    public static boolean isTypeKey(String typeKey) {
        return typeKey != null && typeKey.startsWith(PREFIX) && typeKey.length() > PREFIX.length();
    }

    public static Optional<String> itemKey(String typeKey) {
        if (!isTypeKey(typeKey)) {
            return Optional.empty();
        }
        return Optional.of(typeKey.substring(PREFIX.length()).replace('.', ':'));
    }
}
