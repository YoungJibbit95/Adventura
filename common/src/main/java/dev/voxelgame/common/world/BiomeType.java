package dev.voxelgame.common.world;

import java.util.Objects;

public record BiomeType(
        String key,
        short surfaceBlock,
        short subsurfaceBlock,
        short fillerBlock,
        float temperature,
        float moisture,
        float treeChance,
        float plantChance,
        float structureChance
) {
    public BiomeType {
        Objects.requireNonNull(key, "key");
        if (treeChance < 0 || plantChance < 0 || structureChance < 0) {
            throw new IllegalArgumentException("Biome chances must be >= 0");
        }
    }
}
