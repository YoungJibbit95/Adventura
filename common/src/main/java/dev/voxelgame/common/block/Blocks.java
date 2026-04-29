package dev.voxelgame.common.block;

import dev.voxelgame.common.registry.Registry;

public final class Blocks {
    public static final short AIR = 0;
    public static final short STONE = 1;
    public static final short DIRT = 2;
    public static final short GRASS = 3;
    public static final short WATER = 4;
    public static final short SAND = 5;
    public static final short SKYROOT_LOG = 6;
    public static final short SKYROOT_LEAVES = 7;
    public static final short COAL_ORE = 8;
    public static final short TORCH = 9;
    public static final short WILD_GRASS = 10;
    public static final short SUN_BLOOM = 11;
    public static final short IRON_ORE = 12;
    public static final short COPPER_ORE = 13;
    public static final short CLAY = 14;
    public static final short CACTUS = 15;
    public static final short MOSSY_STONE = 16;
    public static final short GRAVEL = 17;
    public static final short SNOW = 18;
    public static final short ICE = 19;
    public static final short PINE_LOG = 20;
    public static final short PINE_LEAVES = 21;
    public static final short RED_MUSHROOM = 22;
    public static final short SKYROOT_PLANKS = 23;

    private Blocks() {
    }

    public static Registry<BlockType> createDefaultRegistry() {
        Registry<BlockType> registry = new Registry<>("block");
        register(registry, AIR, "voxel:air", 0.0f, ToolType.NONE, false, false, false, 0, BlockRenderLayer.CUTOUT, null);
        register(registry, STONE, "voxel:stone", 1.5f, ToolType.PICKAXE, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:stone");
        register(registry, DIRT, "voxel:dirt", 0.5f, ToolType.SHOVEL, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:dirt");
        register(registry, GRASS, "voxel:grass_block", 0.6f, ToolType.SHOVEL, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:dirt");
        register(registry, WATER, "voxel:water", 100.0f, ToolType.NONE, false, false, false, 0, BlockRenderLayer.TRANSLUCENT, null);
        register(registry, SAND, "voxel:sand", 0.5f, ToolType.SHOVEL, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:sand");
        register(registry, SKYROOT_LOG, "voxel:skyroot_log", 2.0f, ToolType.AXE, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:skyroot_log");
        register(registry, SKYROOT_LEAVES, "voxel:skyroot_leaves", 0.2f, ToolType.AXE, true, false, true, 0, BlockRenderLayer.CUTOUT, "voxel:skyroot_leaves");
        register(registry, COAL_ORE, "voxel:coal_ore", 3.0f, ToolType.PICKAXE, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:coal");
        register(registry, TORCH, "voxel:torch", 0.0f, ToolType.NONE, false, false, false, 14, BlockRenderLayer.CUTOUT, "voxel:torch");
        register(registry, WILD_GRASS, "voxel:wild_grass", 0.0f, ToolType.NONE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:wild_grass");
        register(registry, SUN_BLOOM, "voxel:sun_bloom", 0.0f, ToolType.NONE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:sun_bloom");
        register(registry, IRON_ORE, "voxel:iron_ore", 3.0f, ToolType.PICKAXE, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:raw_iron");
        register(registry, COPPER_ORE, "voxel:copper_ore", 3.0f, ToolType.PICKAXE, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:raw_copper");
        register(registry, CLAY, "voxel:clay", 0.6f, ToolType.SHOVEL, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:clay");
        register(registry, CACTUS, "voxel:cactus", 0.4f, ToolType.AXE, true, false, true, 0, BlockRenderLayer.CUTOUT, "voxel:cactus");
        register(registry, MOSSY_STONE, "voxel:mossy_stone", 1.6f, ToolType.PICKAXE, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:mossy_stone");
        register(registry, GRAVEL, "voxel:gravel", 0.6f, ToolType.SHOVEL, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:gravel");
        register(registry, SNOW, "voxel:snow", 0.2f, ToolType.SHOVEL, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:snow");
        register(registry, ICE, "voxel:ice", 0.5f, ToolType.PICKAXE, true, false, true, 0, BlockRenderLayer.TRANSLUCENT, "voxel:ice");
        register(registry, PINE_LOG, "voxel:pine_log", 2.0f, ToolType.AXE, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:pine_log");
        register(registry, PINE_LEAVES, "voxel:pine_leaves", 0.2f, ToolType.AXE, true, false, true, 0, BlockRenderLayer.CUTOUT, "voxel:pine_leaves");
        register(registry, RED_MUSHROOM, "voxel:red_mushroom", 0.0f, ToolType.NONE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:red_mushroom");
        register(registry, SKYROOT_PLANKS, "voxel:skyroot_planks", 1.4f, ToolType.AXE, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:skyroot_planks");
        return registry;
    }

    private static void register(
            Registry<BlockType> registry,
            short id,
            String key,
            float hardness,
            ToolType tool,
            boolean solid,
            boolean opaque,
            boolean collidable,
            int lightEmission,
            BlockRenderLayer renderLayer,
            String dropItemKey
    ) {
        registry.register(id, key, new BlockType(id, key, hardness, tool, solid, opaque, collidable, lightEmission, renderLayer, dropItemKey));
    }
}
