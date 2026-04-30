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
    public static final short FLOWER_POT = 24;
    public static final short LANTERN = 25;
    public static final short WOVEN_RUG = 26;
    public static final short SMALL_TABLE = 27;
    public static final short WOODEN_CHAIR = 28;
    public static final short STORAGE_CRATE = 29;
    public static final short MOSSY_PATH = 30;
    public static final short GARDEN_FENCE = 31;
    public static final short BERRY_BUSH = 32;
    public static final short HERB_PLANTER = 33;
    public static final short CAMPFIRE = 34;
    public static final short SMALL_STONE = 35;
    public static final short TREE_STUMP = 36;
    public static final short MUSHROOM_CLUSTER = 37;
    public static final short CLAY_DEPOSIT = 38;
    public static final short GLOW_CRYSTAL_NODE = 39;
    public static final short CAMPFIRE_ACTIVE = 40;
    public static final short CAMPFIRE_BURNED_OUT = 41;
    public static final short SLEEPING_MAT = 42;
    public static final short COOKING_POT = 43;
    public static final short REEDS = 44;
    public static final short TWIG_PILE = 45;
    public static final short ANCIENT_LANTERN = 46;
    public static final short WORKBENCH = 47;
    public static final short FORGE = 48;

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
        register(registry, COAL_ORE, "voxel:coal_ore", 3.0f, ToolType.PICKAXE, 1, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:coal");
        register(registry, TORCH, "voxel:torch", 0.0f, ToolType.NONE, false, false, false, 14, BlockRenderLayer.CUTOUT, "voxel:torch");
        register(registry, WILD_GRASS, "voxel:wild_grass", 0.0f, ToolType.KNIFE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:fiber");
        register(registry, SUN_BLOOM, "voxel:sun_bloom", 0.0f, ToolType.KNIFE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:wild_herbs");
        register(registry, IRON_ORE, "voxel:iron_ore", 3.0f, ToolType.PICKAXE, 2, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:raw_iron");
        register(registry, COPPER_ORE, "voxel:copper_ore", 3.0f, ToolType.PICKAXE, 1, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:raw_copper");
        register(registry, CLAY, "voxel:clay", 0.6f, ToolType.SHOVEL, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:clay_lump");
        register(registry, CACTUS, "voxel:cactus", 0.4f, ToolType.AXE, true, false, true, 0, BlockRenderLayer.CUTOUT, "voxel:cactus");
        register(registry, MOSSY_STONE, "voxel:mossy_stone", 1.6f, ToolType.PICKAXE, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:mossy_stone");
        register(registry, GRAVEL, "voxel:gravel", 0.6f, ToolType.SHOVEL, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:gravel");
        register(registry, SNOW, "voxel:snow", 0.2f, ToolType.SHOVEL, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:snow");
        register(registry, ICE, "voxel:ice", 0.5f, ToolType.PICKAXE, true, false, true, 0, BlockRenderLayer.TRANSLUCENT, "voxel:ice");
        register(registry, PINE_LOG, "voxel:pine_log", 2.0f, ToolType.AXE, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:pine_log");
        register(registry, PINE_LEAVES, "voxel:pine_leaves", 0.2f, ToolType.AXE, true, false, true, 0, BlockRenderLayer.CUTOUT, "voxel:pine_leaves");
        register(registry, RED_MUSHROOM, "voxel:red_mushroom", 0.0f, ToolType.KNIFE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:mushroom");
        register(registry, SKYROOT_PLANKS, "voxel:skyroot_planks", 1.4f, ToolType.AXE, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:skyroot_planks");
        register(registry, FLOWER_POT, "voxel:flower_pot", 0.2f, ToolType.NONE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:flower_pot");
        register(registry, LANTERN, "voxel:lantern", 0.2f, ToolType.NONE, false, false, false, 13, BlockRenderLayer.CUTOUT, "voxel:lantern");
        register(registry, WOVEN_RUG, "voxel:woven_rug", 0.1f, ToolType.NONE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:woven_rug");
        register(registry, SMALL_TABLE, "voxel:small_table", 0.8f, ToolType.AXE, true, false, true, 0, BlockRenderLayer.CUTOUT, "voxel:small_table");
        register(registry, WOODEN_CHAIR, "voxel:wooden_chair", 0.7f, ToolType.AXE, true, false, true, 0, BlockRenderLayer.CUTOUT, "voxel:wooden_chair");
        register(registry, STORAGE_CRATE, "voxel:storage_crate", 1.0f, ToolType.AXE, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:storage_crate");
        register(registry, MOSSY_PATH, "voxel:mossy_path", 0.4f, ToolType.SHOVEL, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:mossy_path");
        register(registry, GARDEN_FENCE, "voxel:garden_fence", 0.9f, ToolType.AXE, true, false, true, 0, BlockRenderLayer.CUTOUT, "voxel:garden_fence");
        register(registry, BERRY_BUSH, "voxel:berry_bush", 0.0f, ToolType.KNIFE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:berries");
        register(registry, HERB_PLANTER, "voxel:herb_planter", 0.2f, ToolType.KNIFE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:wild_herbs");
        register(registry, CAMPFIRE, "voxel:campfire", 0.5f, ToolType.AXE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:campfire");
        register(registry, SMALL_STONE, "voxel:small_stone", 0.1f, ToolType.PICKAXE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:pebble");
        register(registry, TREE_STUMP, "voxel:tree_stump", 1.0f, ToolType.AXE, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:skyroot_log");
        register(registry, MUSHROOM_CLUSTER, "voxel:mushroom_cluster", 0.0f, ToolType.KNIFE, false, false, false, 1, BlockRenderLayer.CUTOUT, "voxel:mushroom");
        register(registry, CLAY_DEPOSIT, "voxel:clay_deposit", 0.3f, ToolType.SHOVEL, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:clay_lump");
        register(registry, GLOW_CRYSTAL_NODE, "voxel:glow_crystal_node", 2.8f, ToolType.PICKAXE, 3, true, false, true, 10, BlockRenderLayer.CUTOUT, "voxel:glow_crystal");
        register(registry, CAMPFIRE_ACTIVE, "voxel:campfire_active", 0.5f, ToolType.AXE, false, false, false, 14, BlockRenderLayer.CUTOUT, "voxel:campfire");
        register(registry, CAMPFIRE_BURNED_OUT, "voxel:campfire_burned_out", 0.4f, ToolType.AXE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:campfire");
        register(registry, SLEEPING_MAT, "voxel:sleeping_mat", 0.1f, ToolType.NONE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:sleeping_mat");
        register(registry, COOKING_POT, "voxel:cooking_pot", 0.6f, ToolType.PICKAXE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:cooking_pot");
        register(registry, REEDS, "voxel:reeds", 0.0f, ToolType.KNIFE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:reed_bundle");
        register(registry, TWIG_PILE, "voxel:twig_pile", 0.0f, ToolType.NONE, false, false, false, 0, BlockRenderLayer.CUTOUT, "voxel:twig");
        register(registry, ANCIENT_LANTERN, "voxel:ancient_lantern", 0.2f, ToolType.NONE, false, false, false, 15, BlockRenderLayer.CUTOUT, "voxel:ancient_lantern");
        register(registry, WORKBENCH, "voxel:workbench", 1.1f, ToolType.AXE, true, false, true, 0, BlockRenderLayer.CUTOUT, "voxel:workbench");
        register(registry, FORGE, "voxel:forge", 2.4f, ToolType.PICKAXE, 1, true, true, true, 0, BlockRenderLayer.SOLID, "voxel:forge");
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
        register(registry, id, key, hardness, tool, 0, solid, opaque, collidable, lightEmission, renderLayer, dropItemKey);
    }

    private static void register(
            Registry<BlockType> registry,
            short id,
            String key,
            float hardness,
            ToolType tool,
            int requiredToolLevel,
            boolean solid,
            boolean opaque,
            boolean collidable,
            int lightEmission,
            BlockRenderLayer renderLayer,
            String dropItemKey
    ) {
        registry.register(id, key, new BlockType(id, key, hardness, tool, requiredToolLevel, solid, opaque, collidable, lightEmission, renderLayer, dropItemKey));
    }
}
