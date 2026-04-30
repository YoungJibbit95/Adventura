package dev.voxelgame.server.world;

import dev.voxelgame.common.block.Blocks;

import java.util.Arrays;
import java.util.Optional;

public enum BlockEntityType {
    STORAGE_CRATE("voxel:storage_crate"),
    CAMPFIRE("voxel:campfire"),
    COOKING_POT("voxel:cooking_pot"),
    FORGE("voxel:forge"),
    WORKBENCH("voxel:workbench"),
    SLEEPING_MAT("voxel:sleeping_mat");

    private final String typeKey;

    BlockEntityType(String typeKey) {
        this.typeKey = typeKey;
    }

    public String typeKey() {
        return typeKey;
    }

    public boolean supportsBlock(short blockId) {
        return switch (this) {
            case STORAGE_CRATE -> blockId == Blocks.STORAGE_CRATE;
            case CAMPFIRE -> blockId == Blocks.CAMPFIRE
                    || blockId == Blocks.CAMPFIRE_ACTIVE
                    || blockId == Blocks.CAMPFIRE_BURNED_OUT;
            case COOKING_POT -> blockId == Blocks.COOKING_POT;
            case FORGE -> blockId == Blocks.FORGE;
            case WORKBENCH -> blockId == Blocks.WORKBENCH;
            case SLEEPING_MAT -> blockId == Blocks.SLEEPING_MAT;
        };
    }

    public static Optional<BlockEntityType> fromBlockId(short blockId) {
        return Arrays.stream(values())
                .filter(type -> type.supportsBlock(blockId))
                .findFirst();
    }

    public static Optional<BlockEntityType> fromTypeKey(String typeKey) {
        return Arrays.stream(values())
                .filter(type -> type.typeKey.equals(typeKey))
                .findFirst();
    }
}
