package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.item.CraftingStationType;

public final class CraftingStationRules {
    public static final int STATION_RADIUS_BLOCKS = 4;

    private CraftingStationRules() {
    }

    public static boolean accepts(CraftingStationType stationType, short blockId) {
        return switch (stationType) {
            case INVENTORY -> false;
            case CAMPFIRE -> CampfireRules.isActiveCampfire(blockId);
            case COOKING_POT -> blockId == Blocks.COOKING_POT;
            case WORKBENCH -> blockId == Blocks.WORKBENCH;
            case FORGE -> blockId == Blocks.FORGE;
            case CRAFTING_TABLE -> false;
        };
    }
}
