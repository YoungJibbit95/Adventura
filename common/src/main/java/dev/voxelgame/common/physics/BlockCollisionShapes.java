package dev.voxelgame.common.physics;

import dev.voxelgame.common.block.Blocks;

public final class BlockCollisionShapes {
    private static final BlockCollisionShape PATH = BlockCollisionShape.box(0.0, 0.0, 0.0, 1.0, 0.125, 1.0);
    private static final BlockCollisionShape LOW_STUMP = BlockCollisionShape.box(0.12, 0.0, 0.12, 0.88, 0.72, 0.88);
    private static final BlockCollisionShape CRATE = BlockCollisionShape.box(0.08, 0.0, 0.08, 0.92, 0.92, 0.92);
    private static final BlockCollisionShape TABLE = BlockCollisionShape.box(0.08, 0.0, 0.08, 0.92, 0.78, 0.92);
    private static final BlockCollisionShape CHAIR = BlockCollisionShape.box(0.18, 0.0, 0.18, 0.82, 0.92, 0.82);
    private static final BlockCollisionShape FENCE = BlockCollisionShape.box(0.36, 0.0, 0.36, 0.64, 1.0, 0.64);
    private static final BlockCollisionShape WORKBENCH = BlockCollisionShape.box(0.04, 0.0, 0.04, 0.96, 0.92, 0.96);
    private static final BlockCollisionShape DECORATION = BlockCollisionShape.box(0.25, 0.0, 0.25, 0.75, 0.42, 0.75);
    private static final BlockCollisionShape FORGE = BlockCollisionShape.box(0.02, 0.0, 0.02, 0.98, 1.0, 0.98);
    private static final BlockCollisionShape GLOW_NODE = BlockCollisionShape.box(0.15, 0.0, 0.15, 0.85, 0.82, 0.85);

    private BlockCollisionShapes() {
    }

    public static BlockCollisionShape collisionShape(short blockId) {
        return switch (blockId) {
            case Blocks.AIR, Blocks.WATER,
                 Blocks.TORCH, Blocks.WILD_GRASS, Blocks.SUN_BLOOM, Blocks.RED_MUSHROOM,
                 Blocks.FLOWER_POT, Blocks.LANTERN, Blocks.WOVEN_RUG, Blocks.BERRY_BUSH,
                 Blocks.HERB_PLANTER, Blocks.CAMPFIRE, Blocks.SMALL_STONE, Blocks.MUSHROOM_CLUSTER,
                 Blocks.CLAY_DEPOSIT, Blocks.CAMPFIRE_ACTIVE, Blocks.CAMPFIRE_BURNED_OUT,
                 Blocks.SLEEPING_MAT, Blocks.COOKING_POT, Blocks.REEDS, Blocks.TWIG_PILE,
                 Blocks.ANCIENT_LANTERN, Blocks.GLOW_MUSHROOM, Blocks.SPORE_BLOSSOM -> BlockCollisionShape.NONE;
            case Blocks.MOSSY_PATH -> PATH;
            case Blocks.GARDEN_FENCE -> FENCE;
            case Blocks.STORAGE_CRATE -> CRATE;
            case Blocks.SMALL_TABLE -> TABLE;
            case Blocks.WOODEN_CHAIR -> CHAIR;
            case Blocks.TREE_STUMP -> LOW_STUMP;
            case Blocks.WORKBENCH -> WORKBENCH;
            case Blocks.FORGE -> FORGE;
            case Blocks.GLOW_CRYSTAL_NODE -> GLOW_NODE;
            default -> BlockCollisionShape.FULL;
        };
    }

    public static BlockCollisionShape placementShape(short blockId) {
        return switch (blockId) {
            case Blocks.TORCH, Blocks.FLOWER_POT, Blocks.LANTERN, Blocks.CAMPFIRE,
                 Blocks.CAMPFIRE_ACTIVE, Blocks.CAMPFIRE_BURNED_OUT, Blocks.COOKING_POT,
                 Blocks.ANCIENT_LANTERN, Blocks.SMALL_STONE, Blocks.CLAY_DEPOSIT,
                 Blocks.MUSHROOM_CLUSTER, Blocks.GLOW_MUSHROOM, Blocks.SPORE_BLOSSOM,
                 Blocks.HERB_PLANTER -> DECORATION;
            default -> collisionShape(blockId);
        };
    }

    public static BlockCollisionShape projectileShape(short blockId) {
        return switch (blockId) {
            case Blocks.CAMPFIRE, Blocks.CAMPFIRE_ACTIVE, Blocks.CAMPFIRE_BURNED_OUT,
                 Blocks.COOKING_POT, Blocks.SMALL_STONE, Blocks.CLAY_DEPOSIT,
                 Blocks.MUSHROOM_CLUSTER, Blocks.GLOW_MUSHROOM, Blocks.SPORE_BLOSSOM -> placementShape(blockId);
            default -> collisionShape(blockId);
        };
    }

    public static boolean hasPartialShape(short blockId) {
        BlockCollisionShape shape = collisionShape(blockId);
        return !shape.empty() && shape != BlockCollisionShape.FULL;
    }
}
