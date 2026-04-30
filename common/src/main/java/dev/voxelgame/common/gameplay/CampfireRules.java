package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.Blocks;

import java.util.OptionalDouble;

public final class CampfireRules {
    public static final int STATION_RADIUS_BLOCKS = 4;

    private CampfireRules() {
    }

    public static boolean isCampfire(short blockId) {
        return blockId == Blocks.CAMPFIRE || blockId == Blocks.CAMPFIRE_ACTIVE || blockId == Blocks.CAMPFIRE_BURNED_OUT;
    }

    public static boolean isActiveCampfire(short blockId) {
        return blockId == Blocks.CAMPFIRE_ACTIVE;
    }

    public static OptionalDouble fuelSeconds(String itemKey) {
        return switch (itemKey) {
            case "voxel:dry_grass" -> OptionalDouble.of(18.0);
            case "voxel:bark_strip" -> OptionalDouble.of(22.0);
            case "voxel:reed_bundle" -> OptionalDouble.of(24.0);
            case "voxel:twig", "voxel:stick" -> OptionalDouble.of(35.0);
            case "voxel:skyroot_log", "voxel:pine_log" -> OptionalDouble.of(120.0);
            case "voxel:skyroot_planks" -> OptionalDouble.of(80.0);
            case "voxel:coal", "voxel:charcoal" -> OptionalDouble.of(180.0);
            default -> OptionalDouble.empty();
        };
    }
}
