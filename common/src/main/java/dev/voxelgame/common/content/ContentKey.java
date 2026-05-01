package dev.voxelgame.common.content;

import java.util.Objects;

public record ContentKey(ContentKind kind, String key) {
    public ContentKey {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Content key cannot be blank");
        }
    }

    public static ContentKey item(String key) {
        return new ContentKey(ContentKind.ITEM, key);
    }

    public static ContentKey block(String key) {
        return new ContentKey(ContentKind.BLOCK, key);
    }

    public static ContentKey entity(String key) {
        return new ContentKey(ContentKind.ENTITY, key);
    }
}
