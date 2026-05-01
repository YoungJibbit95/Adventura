package dev.voxelgame.common.gameplay.status;

import java.util.HashMap;
import java.util.Map;

public enum StatusEffectType {
    BURNING("voxel:burning"),
    CHILLED("voxel:chilled"),
    WET("voxel:wet"),
    RESTED("voxel:rested"),
    COZY("voxel:cozy"),
    POISON("voxel:poison");

    private static final Map<String, StatusEffectType> BY_KEY = new HashMap<>();

    static {
        for (StatusEffectType type : values()) {
            BY_KEY.put(type.key, type);
        }
    }

    private final String key;

    StatusEffectType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public String saveKey() {
        return "player.status." + key.substring("voxel:".length());
    }

    public static StatusEffectType fromKey(String key) {
        StatusEffectType type = BY_KEY.get(key);
        if (type == null) {
            throw new IllegalArgumentException("Unknown status effect key: " + key);
        }
        return type;
    }
}
