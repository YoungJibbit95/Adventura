package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.world.WorldView;

public final class ComfortRules {
    public static final int SCAN_RADIUS_BLOCKS = 8;
    public static final int EARLY_CAP = 25;

    private ComfortRules() {
    }

    public static int comfortValue(short blockId) {
        return switch (blockId) {
            case Blocks.ANCIENT_LANTERN -> 4;
            case Blocks.CAMPFIRE_ACTIVE -> 5;
            case Blocks.SLEEPING_MAT -> 4;
            case Blocks.LANTERN, Blocks.WOVEN_RUG -> 3;
            case Blocks.WOODEN_CHAIR, Blocks.SMALL_TABLE, Blocks.CAMPFIRE -> 2;
            case Blocks.FLOWER_POT, Blocks.STORAGE_CRATE, Blocks.GARDEN_FENCE, Blocks.WORKBENCH -> 1;
            default -> 0;
        };
    }

    public static int scan(WorldView world, double centerX, double centerY, double centerZ) {
        return scan(world, centerX, centerY, centerZ, SCAN_RADIUS_BLOCKS, EARLY_CAP);
    }

    public static int scan(WorldView world, double centerX, double centerY, double centerZ, int radius, int cap) {
        int cx = (int) Math.floor(centerX);
        int cy = (int) Math.floor(centerY);
        int cz = (int) Math.floor(centerZ);
        int r = Math.max(0, radius);
        int total = 0;
        for (int y = cy - r; y <= cy + r; y++) {
            if (!world.dimension().containsY(y)) {
                continue;
            }
            for (int z = cz - r; z <= cz + r; z++) {
                for (int x = cx - r; x <= cx + r; x++) {
                    int distance = Math.abs(x - cx) + Math.abs(y - cy) + Math.abs(z - cz);
                    if (distance > r * 2) {
                        continue;
                    }
                    total += comfortValue(world.blockId(x, y, z));
                    if (total >= cap) {
                        return cap;
                    }
                }
            }
        }
        return Math.min(total, cap);
    }

    public static float hungerDrainMultiplier(int comfort) {
        return 1.0f - Math.min(0.25f, Math.max(0, comfort) * 0.0125f);
    }

    public static float staminaRegenMultiplier(int comfort) {
        return 1.0f + Math.min(0.40f, Math.max(0, comfort) * 0.02f);
    }
}
