package dev.voxelgame.common.block;

import java.util.Objects;

public record BlockType(
        short id,
        String key,
        float hardness,
        ToolType preferredTool,
        boolean solid,
        boolean opaque,
        boolean collidable,
        int lightEmission,
        BlockRenderLayer renderLayer,
        String dropItemKey
) {
    public BlockType {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(preferredTool, "preferredTool");
        Objects.requireNonNull(renderLayer, "renderLayer");
        if (hardness < 0.0f) {
            throw new IllegalArgumentException("Block hardness must be >= 0");
        }
        if (lightEmission < 0 || lightEmission > 15) {
            throw new IllegalArgumentException("Block light emission must be in 0..15");
        }
    }

    public boolean emitsLight() {
        return lightEmission > 0;
    }
}
