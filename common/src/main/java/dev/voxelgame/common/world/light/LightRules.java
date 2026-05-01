package dev.voxelgame.common.world.light;

import dev.voxelgame.common.block.BlockRenderLayer;
import dev.voxelgame.common.block.BlockType;

public final class LightRules {
    public static final int MAX_LIGHT = 15;

    private LightRules() {
    }

    public static int skyLightReduction(BlockType block) {
        if (block.opaque()) {
            return MAX_LIGHT;
        }
        String key = block.key();
        if (key.contains("leaves")) {
            return 2;
        }
        if (block.renderLayer() == BlockRenderLayer.TRANSLUCENT) {
            return 1;
        }
        if (block.renderLayer() == BlockRenderLayer.CUTOUT && block.collidable()) {
            return 1;
        }
        return 0;
    }

    public static boolean passesBlockLight(BlockType block) {
        return !block.opaque();
    }

    public static String occlusionType(BlockType block) {
        if (block.opaque()) {
            return "opaque";
        }
        if (block.renderLayer() == BlockRenderLayer.TRANSLUCENT) {
            return "translucent";
        }
        if (block.key().contains("leaves")) {
            return "foliage";
        }
        if (block.renderLayer() == BlockRenderLayer.CUTOUT) {
            return block.collidable() ? "cutout-partial" : "cutout-open";
        }
        return "open";
    }

    static int outgoingSkyLight(BlockType block, int incomingLight) {
        if (incomingLight <= 0 || block.opaque()) {
            return 0;
        }
        return Math.max(0, incomingLight - skyLightReduction(block));
    }
}
