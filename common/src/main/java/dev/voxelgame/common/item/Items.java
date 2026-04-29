package dev.voxelgame.common.item;

import dev.voxelgame.common.block.ToolType;
import dev.voxelgame.common.registry.Registry;

public final class Items {
    private Items() {
    }

    public static Registry<ItemType> createDefaultRegistry() {
        Registry<ItemType> registry = new Registry<>("item");
        block(registry, (short) 1, "voxel:stone");
        block(registry, (short) 2, "voxel:dirt");
        block(registry, (short) 3, "voxel:grass_block");
        block(registry, (short) 4, "voxel:sand");
        block(registry, (short) 5, "voxel:skyroot_log");
        block(registry, (short) 6, "voxel:skyroot_leaves");
        block(registry, (short) 7, "voxel:torch");
        block(registry, (short) 8, "voxel:wild_grass");
        block(registry, (short) 9, "voxel:sun_bloom");
        block(registry, (short) 10, "voxel:clay");
        block(registry, (short) 11, "voxel:cactus");
        block(registry, (short) 12, "voxel:mossy_stone");
        block(registry, (short) 13, "voxel:gravel");
        block(registry, (short) 14, "voxel:snow");
        block(registry, (short) 15, "voxel:ice");
        block(registry, (short) 16, "voxel:pine_log");
        block(registry, (short) 17, "voxel:pine_leaves");
        block(registry, (short) 18, "voxel:red_mushroom");
        block(registry, (short) 19, "voxel:skyroot_planks");
        block(registry, (short) 50, "voxel:flower_pot");
        block(registry, (short) 51, "voxel:lantern");
        block(registry, (short) 52, "voxel:woven_rug");
        block(registry, (short) 53, "voxel:small_table");
        block(registry, (short) 54, "voxel:wooden_chair");
        block(registry, (short) 55, "voxel:storage_crate");
        block(registry, (short) 56, "voxel:mossy_path");
        block(registry, (short) 57, "voxel:garden_fence");
        block(registry, (short) 58, "voxel:berry_bush");
        block(registry, (short) 59, "voxel:herb_planter");
        block(registry, (short) 60, "voxel:campfire");
        block(registry, (short) 61, "voxel:small_stone");
        block(registry, (short) 62, "voxel:tree_stump");
        registry.register((short) 20, "voxel:coal", new ItemType((short) 20, "voxel:coal", 64, ToolType.NONE, 0, null));
        registry.register((short) 21, "voxel:raw_iron", new ItemType((short) 21, "voxel:raw_iron", 64, ToolType.NONE, 0, null));
        registry.register((short) 22, "voxel:raw_copper", new ItemType((short) 22, "voxel:raw_copper", 64, ToolType.NONE, 0, null));
        registry.register((short) 23, "voxel:stick", new ItemType((short) 23, "voxel:stick", 64, ToolType.NONE, 0, null));
        registry.register((short) 24, "voxel:apple", new ItemType((short) 24, "voxel:apple", 16, ToolType.NONE, 0, null, 4, 1));
        registry.register((short) 25, "voxel:berries", new ItemType((short) 25, "voxel:berries", 16, ToolType.NONE, 0, null, 3, 0));
        registry.register((short) 26, "voxel:fiber", new ItemType((short) 26, "voxel:fiber", 64, ToolType.NONE, 0, null));
        registry.register((short) 27, "voxel:twig", new ItemType((short) 27, "voxel:twig", 64, ToolType.NONE, 0, null));
        registry.register((short) 28, "voxel:pebble", new ItemType((short) 28, "voxel:pebble", 64, ToolType.NONE, 0, null));
        registry.register((short) 29, "voxel:resin", new ItemType((short) 29, "voxel:resin", 32, ToolType.NONE, 0, null));
        registry.register((short) 30, "voxel:feathers", new ItemType((short) 30, "voxel:feathers", 32, ToolType.NONE, 0, null));
        registry.register((short) 31, "voxel:wild_herbs", new ItemType((short) 31, "voxel:wild_herbs", 32, ToolType.NONE, 0, null, 1, 0));
        registry.register((short) 32, "voxel:simple_rope", new ItemType((short) 32, "voxel:simple_rope", 32, ToolType.NONE, 0, null));
        registry.register((short) 33, "voxel:healing_snack", new ItemType((short) 33, "voxel:healing_snack", 16, ToolType.NONE, 0, null, 5, 4));
        registry.register((short) 40, "voxel:stone_pickaxe", new ItemType((short) 40, "voxel:stone_pickaxe", 1, ToolType.PICKAXE, 132, null));
        registry.register((short) 41, "voxel:stone_shovel", new ItemType((short) 41, "voxel:stone_shovel", 1, ToolType.SHOVEL, 132, null));
        registry.register((short) 42, "voxel:stone_axe", new ItemType((short) 42, "voxel:stone_axe", 1, ToolType.AXE, 156, null));
        registry.register((short) 43, "voxel:stone_sword", new ItemType((short) 43, "voxel:stone_sword", 1, ToolType.NONE, 96, null));
        registry.register((short) 44, "voxel:stone_knife", new ItemType((short) 44, "voxel:stone_knife", 1, ToolType.KNIFE, 72, null));
        return registry;
    }

    private static void block(Registry<ItemType> registry, short id, String key) {
        registry.register(id, key, new ItemType(id, key, 64, ToolType.NONE, 0, key));
    }
}
