package dev.voxelgame.common.item;

import dev.voxelgame.common.block.ToolType;

import java.util.Objects;

public record ItemType(
        short id,
        String key,
        int maxStackSize,
        ToolType toolType,
        int durability,
        String placesBlockKey
) {
    public ItemType {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(toolType, "toolType");
        if (maxStackSize < 1 || maxStackSize > 99) {
            throw new IllegalArgumentException("Item max stack size must be in 1..99");
        }
        if (durability < 0) {
            throw new IllegalArgumentException("Item durability must be >= 0");
        }
    }

    public boolean isTool() {
        return toolType != ToolType.NONE;
    }
}
