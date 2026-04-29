package dev.voxelgame.common.item;

import dev.voxelgame.common.block.ToolType;

import java.util.Objects;

public record ItemType(
        short id,
        String key,
        int maxStackSize,
        ToolType toolType,
        int durability,
        String placesBlockKey,
        int foodValue,
        int healValue,
        int toolLevel,
        float toolSpeed
) {
    public ItemType(short id, String key, int maxStackSize, ToolType toolType, int durability, String placesBlockKey) {
        this(id, key, maxStackSize, toolType, durability, placesBlockKey, 0, 0);
    }

    public ItemType(short id, String key, int maxStackSize, ToolType toolType, int durability, String placesBlockKey, int foodValue, int healValue) {
        this(
                id,
                key,
                maxStackSize,
                toolType,
                durability,
                placesBlockKey,
                foodValue,
                healValue,
                defaultToolLevel(key, toolType),
                defaultToolSpeed(key, toolType)
        );
    }

    public ItemType {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(toolType, "toolType");
        if (maxStackSize < 1 || maxStackSize > 99) {
            throw new IllegalArgumentException("Item max stack size must be in 1..99");
        }
        if (durability < 0) {
            throw new IllegalArgumentException("Item durability must be >= 0");
        }
        if (foodValue < 0 || healValue < 0) {
            throw new IllegalArgumentException("Food and heal values must be >= 0");
        }
        if (toolLevel < 0) {
            throw new IllegalArgumentException("Tool level must be >= 0");
        }
        if (toolType == ToolType.NONE && toolLevel != 0) {
            throw new IllegalArgumentException("Non-tools must have tool level 0");
        }
        if (toolType != ToolType.NONE && toolLevel < 1) {
            throw new IllegalArgumentException("Tools must have tool level >= 1");
        }
        if (toolSpeed <= 0.0f) {
            throw new IllegalArgumentException("Tool speed must be > 0");
        }
    }

    public boolean isTool() {
        return toolType != ToolType.NONE;
    }

    public boolean isFood() {
        return foodValue > 0 || healValue > 0;
    }

    private static int defaultToolLevel(String key, ToolType toolType) {
        if (toolType == null || toolType == ToolType.NONE) {
            return 0;
        }
        String itemKey = key == null ? "" : key;
        if (itemKey.contains("ancient") || itemKey.contains("crystal")) {
            return 4;
        }
        if (itemKey.contains("iron")) {
            return 3;
        }
        if (itemKey.contains("copper")) {
            return 2;
        }
        return 1;
    }

    private static float defaultToolSpeed(String key, ToolType toolType) {
        if (toolType == null || toolType == ToolType.NONE) {
            return 1.0f;
        }
        String itemKey = key == null ? "" : key;
        if (itemKey.contains("ancient") || itemKey.contains("crystal")) {
            return 1.75f;
        }
        if (itemKey.contains("iron")) {
            return 1.45f;
        }
        if (itemKey.contains("copper")) {
            return 1.25f;
        }
        return 1.0f;
    }
}
