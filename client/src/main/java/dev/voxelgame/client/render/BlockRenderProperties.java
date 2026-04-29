package dev.voxelgame.client.render;

import dev.voxelgame.common.block.BlockRenderLayer;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.registry.Registry;

import java.util.Arrays;

public record BlockRenderProperties(
        float tintR,
        float tintG,
        float tintB,
        float alpha,
        float emissive,
        boolean animatedFluid
) {
    public static final int MAX_BLOCK_ID = 256;
    public static final int SHADER_BLOCK_ID_LIMIT = 64;
    public static final int FLAG_TRANSLUCENT = 1;
    public static final int FLAG_EMISSIVE = 1 << 1;
    public static final int FLAG_ANIMATED_FLUID = 1 << 2;
    public static final int FLAG_FILL_TEXTURE_GAPS = 1 << 3;

    private static final BlockRenderProperties FALLBACK = new BlockRenderProperties(0.70f, 0.30f, 0.70f, 1.0f, 0.0f, false);
    private static final BlockRenderProperties[] DEFAULTS = createDefaultTable();
    private static final boolean[] FILLS_TEXTURE_GAPS = createFillTextureGapTable();

    public BlockRenderProperties {
        if (!Float.isFinite(tintR) || !Float.isFinite(tintG) || !Float.isFinite(tintB)
                || !Float.isFinite(alpha) || !Float.isFinite(emissive)) {
            throw new IllegalArgumentException("Render properties must be finite");
        }
        if (!unit(tintR) || !unit(tintG) || !unit(tintB) || !unit(alpha) || !unit(emissive)) {
            throw new IllegalArgumentException("Render property values must be within 0..1");
        }
    }

    public static BlockRenderProperties fallback() {
        return FALLBACK;
    }

    public static BlockRenderProperties forBlock(short blockId) {
        if (blockId < 0 || blockId >= DEFAULTS.length) {
            return FALLBACK;
        }
        return DEFAULTS[blockId];
    }

    public static BlockRenderProperties[] defaultsByBlockId() {
        return DEFAULTS.clone();
    }

    public static void validateRegisteredBlocks(Registry<BlockType> blocks) {
        for (BlockType block : blocks.values()) {
            if (block.id() < 0 || block.id() >= MAX_BLOCK_ID) {
                throw new IllegalStateException("Block id outside render material table: " + block.key());
            }
            if (block.id() >= SHADER_BLOCK_ID_LIMIT) {
                throw new IllegalStateException("Block id outside shader material table: " + block.key());
            }
        }
    }

    public static boolean fillsTextureGaps(short blockId) {
        if (blockId < 0 || blockId >= FILLS_TEXTURE_GAPS.length) {
            return false;
        }
        return FILLS_TEXTURE_GAPS[blockId];
    }

    public int materialFlags() {
        int flags = 0;
        if (alpha < 1.0f) {
            flags |= FLAG_TRANSLUCENT;
        }
        if (emissive > 0.0f) {
            flags |= FLAG_EMISSIVE;
        }
        if (animatedFluid) {
            flags |= FLAG_ANIMATED_FLUID;
        }
        return flags;
    }

    public static float[] colorAlphaTable() {
        return colorAlphaTable(MAX_BLOCK_ID);
    }

    public static float[] shaderColorAlphaTable() {
        return colorAlphaTable(SHADER_BLOCK_ID_LIMIT);
    }

    public static float[] effectsTable() {
        return effectsTable(MAX_BLOCK_ID);
    }

    public static float[] shaderEffectsTable() {
        return effectsTable(SHADER_BLOCK_ID_LIMIT);
    }

    private static float[] colorAlphaTable(int size) {
        float[] table = new float[size * 4];
        for (int id = 0; id < size; id++) {
            BlockRenderProperties properties = DEFAULTS[id];
            int offset = id * 4;
            table[offset] = properties.tintR();
            table[offset + 1] = properties.tintG();
            table[offset + 2] = properties.tintB();
            table[offset + 3] = properties.alpha();
        }
        return table;
    }

    private static float[] effectsTable(int size) {
        float[] table = new float[size * 4];
        for (int id = 0; id < size; id++) {
            BlockRenderProperties properties = DEFAULTS[id];
            int offset = id * 4;
            table[offset] = properties.emissive();
            table[offset + 1] = properties.animatedFluid() ? 1.0f : 0.0f;
            table[offset + 2] = properties.materialFlags() | (fillsTextureGaps((short) id) ? FLAG_FILL_TEXTURE_GAPS : 0);
            table[offset + 3] = fillsTextureGaps((short) id) ? 1.0f : 0.0f;
        }
        return table;
    }

    private static boolean[] createFillTextureGapTable() {
        boolean[] table = new boolean[MAX_BLOCK_ID];
        Registry<BlockType> blocks = Blocks.createDefaultRegistry();
        validateRegisteredBlocks(blocks);
        for (BlockType block : blocks.values()) {
            table[block.id()] = block.id() != Blocks.AIR
                    && (block.renderLayer() != BlockRenderLayer.CUTOUT || block.collidable());
        }
        return table;
    }

    private static BlockRenderProperties[] createDefaultTable() {
        BlockRenderProperties[] table = new BlockRenderProperties[MAX_BLOCK_ID];
        Arrays.fill(table, FALLBACK);
        set(table, Blocks.AIR, 0.0f, 0.0f, 0.0f);
        set(table, Blocks.STONE, 0.48f, 0.48f, 0.47f);
        set(table, Blocks.DIRT, 0.42f, 0.27f, 0.15f);
        set(table, Blocks.GRASS, 0.33f, 0.62f, 0.24f);
        set(table, Blocks.WATER, 0.20f, 0.42f, 0.82f, 0.58f, 0.0f, true);
        set(table, Blocks.SAND, 0.78f, 0.70f, 0.45f);
        set(table, Blocks.SKYROOT_LOG, 0.42f, 0.28f, 0.16f);
        set(table, Blocks.SKYROOT_LEAVES, 0.20f, 0.48f, 0.24f);
        set(table, Blocks.COAL_ORE, 0.28f, 0.27f, 0.26f);
        set(table, Blocks.TORCH, 1.00f, 0.72f, 0.30f, 1.0f, 0.85f, false);
        set(table, Blocks.WILD_GRASS, 0.25f, 0.58f, 0.20f);
        set(table, Blocks.SUN_BLOOM, 0.95f, 0.75f, 0.20f);
        set(table, Blocks.IRON_ORE, 0.58f, 0.50f, 0.43f);
        set(table, Blocks.COPPER_ORE, 0.62f, 0.36f, 0.20f);
        set(table, Blocks.CLAY, 0.46f, 0.55f, 0.62f);
        set(table, Blocks.CACTUS, 0.18f, 0.50f, 0.25f);
        set(table, Blocks.MOSSY_STONE, 0.34f, 0.43f, 0.32f);
        set(table, Blocks.GRAVEL, 0.45f, 0.43f, 0.38f);
        set(table, Blocks.SNOW, 0.86f, 0.91f, 0.91f);
        set(table, Blocks.ICE, 0.50f, 0.74f, 0.88f, 0.70f, 0.0f, false);
        set(table, Blocks.PINE_LOG, 0.38f, 0.23f, 0.13f);
        set(table, Blocks.PINE_LEAVES, 0.12f, 0.31f, 0.25f);
        set(table, Blocks.RED_MUSHROOM, 0.62f, 0.20f, 0.16f);
        set(table, Blocks.SKYROOT_PLANKS, 0.58f, 0.39f, 0.20f);
        set(table, Blocks.FLOWER_POT, 0.63f, 0.36f, 0.22f);
        set(table, Blocks.LANTERN, 0.95f, 0.68f, 0.28f, 1.0f, 0.78f, false);
        set(table, Blocks.WOVEN_RUG, 0.54f, 0.24f, 0.24f);
        set(table, Blocks.SMALL_TABLE, 0.50f, 0.31f, 0.18f);
        set(table, Blocks.WOODEN_CHAIR, 0.48f, 0.29f, 0.17f);
        set(table, Blocks.STORAGE_CRATE, 0.46f, 0.29f, 0.16f);
        set(table, Blocks.MOSSY_PATH, 0.30f, 0.44f, 0.28f);
        set(table, Blocks.GARDEN_FENCE, 0.43f, 0.31f, 0.17f);
        set(table, Blocks.BERRY_BUSH, 0.28f, 0.45f, 0.24f);
        set(table, Blocks.HERB_PLANTER, 0.36f, 0.52f, 0.30f);
        set(table, Blocks.CAMPFIRE, 0.95f, 0.45f, 0.20f, 1.0f, 0.55f, false);
        set(table, Blocks.SMALL_STONE, 0.42f, 0.42f, 0.40f);
        set(table, Blocks.TREE_STUMP, 0.38f, 0.24f, 0.13f);
        set(table, Blocks.MUSHROOM_CLUSTER, 0.58f, 0.32f, 0.26f);
        set(table, Blocks.CLAY_DEPOSIT, 0.50f, 0.58f, 0.62f);
        set(table, Blocks.GLOW_CRYSTAL_NODE, 0.34f, 0.78f, 0.92f, 1.0f, 0.92f, false);
        set(table, Blocks.CAMPFIRE_ACTIVE, 1.00f, 0.55f, 0.20f, 1.0f, 1.0f, false);
        set(table, Blocks.CAMPFIRE_BURNED_OUT, 0.24f, 0.22f, 0.20f);
        set(table, Blocks.SLEEPING_MAT, 0.55f, 0.30f, 0.28f);
        return table;
    }

    private static void set(BlockRenderProperties[] table, short blockId, float tintR, float tintG, float tintB) {
        set(table, blockId, tintR, tintG, tintB, 1.0f, 0.0f, false);
    }

    private static void set(
            BlockRenderProperties[] table,
            short blockId,
            float tintR,
            float tintG,
            float tintB,
            float alpha,
            float emissive,
            boolean animatedFluid
    ) {
        table[blockId] = new BlockRenderProperties(tintR, tintG, tintB, alpha, emissive, animatedFluid);
    }

    private static boolean unit(float value) {
        return value >= 0.0f && value <= 1.0f;
    }
}
