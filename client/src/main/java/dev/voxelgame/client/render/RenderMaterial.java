package dev.voxelgame.client.render;

import dev.voxelgame.client.render.assets.BlockTextureAtlas;
import dev.voxelgame.common.block.BlockRenderLayer;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.registry.Registry;

import java.util.Arrays;
import java.util.Objects;

public record RenderMaterial(
        int materialIndex,
        String key,
        BlockRenderLayer renderLayer,
        float tintR,
        float tintG,
        float tintB,
        float alpha,
        float emissive,
        boolean animatedFluid,
        float cutoutThreshold,
        BlockRenderProperties.BiomeTintMode biomeTintMode,
        BlockRenderProperties.FogAffectMode fogAffectMode,
        boolean faceGapFix,
        float roughness,
        boolean missingMaterialData,
        float sideU0,
        float sideV0,
        float sideU1,
        float sideV1,
        float topU0,
        float topV0,
        float topU1,
        float topV1,
        float bottomU0,
        float bottomV0,
        float bottomU1,
        float bottomV1
) {
    public static final int TEXELS_PER_MATERIAL = 6;
    public static final int FLOATS_PER_MATERIAL = TEXELS_PER_MATERIAL * 4;
    public static final float MISSING_UV = -1.0f;
    public static final int FLAG_MISSING_MATERIAL_DATA = 1 << 7;

    private static final float[] MISSING_UV_RECT = {MISSING_UV, MISSING_UV, MISSING_UV, MISSING_UV};

    public RenderMaterial {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(renderLayer, "renderLayer");
        Objects.requireNonNull(biomeTintMode, "biomeTintMode");
        Objects.requireNonNull(fogAffectMode, "fogAffectMode");
        if (materialIndex < 0 || materialIndex >= BlockRenderProperties.MATERIAL_INDEX_LIMIT) {
            throw new IllegalArgumentException("Material index outside material LUT: " + materialIndex);
        }
        if (!finite(tintR, tintG, tintB, alpha, emissive, cutoutThreshold, roughness,
                sideU0, sideV0, sideU1, sideV1, topU0, topV0, topU1, topV1, bottomU0, bottomV0, bottomU1, bottomV1)) {
            throw new IllegalArgumentException("Material values must be finite");
        }
    }

    public static RenderMaterial fallback() {
        return fallback(0);
    }

    public static RenderMaterial fallback(int materialIndex) {
        return fromProperties(
                materialIndex,
                "voxel:fallback",
                BlockRenderLayer.SOLID,
                BlockRenderProperties.fallback(),
                false,
                true,
                MISSING_UV_RECT,
                MISSING_UV_RECT,
                MISSING_UV_RECT
        );
    }

    public static RenderMaterial forBlock(BlockType block) {
        return forBlock(block, null);
    }

    public static RenderMaterial forBlock(BlockType block, BlockTextureAtlas atlas) {
        Objects.requireNonNull(block, "block");
        boolean explicit = BlockRenderProperties.hasExplicitProperties(block.id());
        BlockRenderProperties properties = explicit
                ? BlockRenderProperties.forBlock(block.id())
                : fallbackPropertiesFor(block.renderLayer());
        return fromProperties(
                block.id(),
                block.key(),
                block.renderLayer(),
                properties,
                fillsTextureGaps(block),
                !explicit,
                uvRect(atlas, block.id(), BlockTextureAtlas.TextureFace.SIDE),
                uvRect(atlas, block.id(), BlockTextureAtlas.TextureFace.TOP),
                uvRect(atlas, block.id(), BlockTextureAtlas.TextureFace.BOTTOM)
        );
    }

    public static RenderMaterial[] fromRegistry(Registry<BlockType> blocks) {
        return tableFor(blocks, null).materials();
    }

    public static Table tableFor(Registry<BlockType> blocks, BlockTextureAtlas atlas) {
        Objects.requireNonNull(blocks, "blocks");
        BlockRenderProperties.validateRegisteredBlocks(blocks);
        int materialCount = materialCount(blocks);
        RenderMaterial[] materials = new RenderMaterial[materialCount];
        for (int index = 0; index < materials.length; index++) {
            materials[index] = fallback(index);
        }
        int missingMaterialCount = 0;
        for (BlockType block : blocks.values()) {
            RenderMaterial material = forBlock(block, atlas);
            materials[block.id()] = material;
            if (material.missingMaterialData()) {
                missingMaterialCount++;
            }
        }
        validateLayerCompatibility(blocks, materials);
        return new Table(materials, missingMaterialCount);
    }

    public static int materialCount(Registry<BlockType> blocks) {
        Objects.requireNonNull(blocks, "blocks");
        int maxId = 0;
        for (BlockType block : blocks.values()) {
            if (block.id() < 0 || block.id() >= BlockRenderProperties.MATERIAL_INDEX_LIMIT) {
                throw new IllegalStateException("Block id outside render material table: " + block.key());
            }
            maxId = Math.max(maxId, block.id());
        }
        return Math.max(1, maxId + 1);
    }

    public static void validateLayerCompatibility(Registry<BlockType> blocks, RenderMaterial[] materials) {
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(materials, "materials");
        for (BlockType block : blocks.values()) {
            if (block.id() < 0 || block.id() >= materials.length) {
                throw new IllegalStateException("Missing material slot for " + block.key());
            }
            RenderMaterial material = materials[block.id()];
            if (material.renderLayer() != block.renderLayer()) {
                throw new IllegalStateException("Material layer mismatch for " + block.key());
            }
            if (material.renderLayer() == BlockRenderLayer.SOLID && material.alpha() < 1.0f) {
                throw new IllegalStateException("SOLID material must be opaque: " + block.key());
            }
            if (material.renderLayer() == BlockRenderLayer.CUTOUT && material.alpha() < 1.0f) {
                throw new IllegalStateException("CUTOUT material uses alpha-test and must not use blend alpha: " + block.key());
            }
            if (material.renderLayer() == BlockRenderLayer.TRANSLUCENT && material.alpha() >= 1.0f) {
                throw new IllegalStateException("TRANSLUCENT material needs alpha below 1.0: " + block.key());
            }
            if (material.animatedFluid() && material.renderLayer() != BlockRenderLayer.TRANSLUCENT) {
                throw new IllegalStateException("Animated fluid material must render in TRANSLUCENT layer: " + block.key());
            }
        }
    }

    public static float[] toLutData(RenderMaterial[] materials) {
        Objects.requireNonNull(materials, "materials");
        if (materials.length == 0) {
            return fallback(0).toLutData();
        }
        float[] values = new float[materials.length * FLOATS_PER_MATERIAL];
        for (int index = 0; index < materials.length; index++) {
            RenderMaterial material = materials[index] == null ? fallback(index) : materials[index];
            material.writeLutData(values, index * FLOATS_PER_MATERIAL);
        }
        return values;
    }

    public float[] toLutData() {
        float[] values = new float[FLOATS_PER_MATERIAL];
        writeLutData(values, 0);
        return values;
    }

    public int flags() {
        int flags = 0;
        if (alpha < 1.0f) {
            flags |= BlockRenderProperties.FLAG_TRANSLUCENT;
        }
        if (emissive > 0.0f) {
            flags |= BlockRenderProperties.FLAG_EMISSIVE;
        }
        if (animatedFluid) {
            flags |= BlockRenderProperties.FLAG_ANIMATED_FLUID;
        }
        if (faceGapFix) {
            flags |= BlockRenderProperties.FLAG_FILL_TEXTURE_GAPS;
        }
        flags |= switch (renderLayer) {
            case SOLID -> BlockRenderProperties.FLAG_LAYER_SOLID;
            case CUTOUT -> BlockRenderProperties.FLAG_LAYER_CUTOUT;
            case TRANSLUCENT -> BlockRenderProperties.FLAG_LAYER_TRANSLUCENT;
        };
        if (missingMaterialData) {
            flags |= FLAG_MISSING_MATERIAL_DATA;
        }
        return flags;
    }

    public float[] uvRect(BlockTextureAtlas.TextureFace face) {
        return switch (face) {
            case SIDE -> new float[]{sideU0, sideV0, sideU1, sideV1};
            case TOP -> new float[]{topU0, topV0, topU1, topV1};
            case BOTTOM -> new float[]{bottomU0, bottomV0, bottomU1, bottomV1};
        };
    }

    private void writeLutData(float[] values, int offset) {
        values[offset] = tintR;
        values[offset + 1] = tintG;
        values[offset + 2] = tintB;
        values[offset + 3] = alpha;
        values[offset + 4] = emissive;
        values[offset + 5] = animatedFluid ? 1.0f : 0.0f;
        values[offset + 6] = flags();
        values[offset + 7] = cutoutThreshold;
        values[offset + 8] = sideU0;
        values[offset + 9] = sideV0;
        values[offset + 10] = sideU1;
        values[offset + 11] = sideV1;
        values[offset + 12] = topU0;
        values[offset + 13] = topV0;
        values[offset + 14] = topU1;
        values[offset + 15] = topV1;
        values[offset + 16] = bottomU0;
        values[offset + 17] = bottomV0;
        values[offset + 18] = bottomU1;
        values[offset + 19] = bottomV1;
        values[offset + 20] = biomeTintMode.ordinal();
        values[offset + 21] = fogAffectMode.ordinal();
        values[offset + 22] = renderLayer.ordinal();
        values[offset + 23] = roughness;
    }

    private static RenderMaterial fromProperties(
            int materialIndex,
            String key,
            BlockRenderLayer renderLayer,
            BlockRenderProperties properties,
            boolean faceGapFix,
            boolean missingMaterialData,
            float[] sideUv,
            float[] topUv,
            float[] bottomUv
    ) {
        return new RenderMaterial(
                materialIndex,
                key,
                renderLayer,
                properties.tintR(),
                properties.tintG(),
                properties.tintB(),
                properties.alpha(),
                properties.emissive(),
                properties.animatedFluid(),
                properties.cutoutThreshold(),
                properties.biomeTintMode(),
                properties.fogAffectMode(),
                faceGapFix,
                properties.roughness(),
                missingMaterialData,
                sideUv[0],
                sideUv[1],
                sideUv[2],
                sideUv[3],
                topUv[0],
                topUv[1],
                topUv[2],
                topUv[3],
                bottomUv[0],
                bottomUv[1],
                bottomUv[2],
                bottomUv[3]
        );
    }

    private static BlockRenderProperties fallbackPropertiesFor(BlockRenderLayer layer) {
        BlockRenderProperties fallback = BlockRenderProperties.fallback();
        float alpha = layer == BlockRenderLayer.TRANSLUCENT ? 0.65f : 1.0f;
        return new BlockRenderProperties(
                fallback.tintR(),
                fallback.tintG(),
                fallback.tintB(),
                alpha,
                fallback.emissive(),
                false,
                fallback.cutoutThreshold(),
                fallback.biomeTintMode(),
                fallback.fogAffectMode(),
                fallback.roughness()
        );
    }

    private static float[] uvRect(BlockTextureAtlas atlas, short blockId, BlockTextureAtlas.TextureFace face) {
        return atlas == null ? MISSING_UV_RECT : atlas.uvRect(blockId, face);
    }

    private static boolean fillsTextureGaps(BlockType block) {
        return block.id() != Blocks.AIR
                && (block.renderLayer() != BlockRenderLayer.CUTOUT || block.collidable());
    }

    private static boolean finite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    public record Table(RenderMaterial[] materials, int missingMaterialCount) {
        public Table {
            materials = materials == null || materials.length == 0
                    ? new RenderMaterial[]{RenderMaterial.fallback(0)}
                    : Arrays.copyOf(materials, materials.length);
            missingMaterialCount = Math.max(0, missingMaterialCount);
        }

        @Override
        public RenderMaterial[] materials() {
            return Arrays.copyOf(materials, materials.length);
        }

        public int materialCount() {
            return materials.length;
        }

        public int texelCount() {
            return materialCount() * TEXELS_PER_MATERIAL;
        }

        public float[] toLutData() {
            return RenderMaterial.toLutData(materials);
        }
    }
}
