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
        registry.register((short) 20, "voxel:coal", new ItemType((short) 20, "voxel:coal", 64, ToolType.NONE, 0, null));
        registry.register((short) 21, "voxel:raw_iron", new ItemType((short) 21, "voxel:raw_iron", 64, ToolType.NONE, 0, null));
        registry.register((short) 22, "voxel:raw_copper", new ItemType((short) 22, "voxel:raw_copper", 64, ToolType.NONE, 0, null));
        registry.register((short) 23, "voxel:stick", new ItemType((short) 23, "voxel:stick", 64, ToolType.NONE, 0, null));
        registry.register((short) 24, "voxel:apple", new ItemType((short) 24, "voxel:apple", 16, ToolType.NONE, 0, null));
        registry.register((short) 25, "voxel:berries", new ItemType((short) 25, "voxel:berries", 16, ToolType.NONE, 0, null));
        registry.register((short) 26, "voxel:fiber", new ItemType((short) 26, "voxel:fiber", 64, ToolType.NONE, 0, null));
        registry.register((short) 40, "voxel:stone_pickaxe", new ItemType((short) 40, "voxel:stone_pickaxe", 1, ToolType.PICKAXE, 132, null));
        registry.register((short) 41, "voxel:stone_shovel", new ItemType((short) 41, "voxel:stone_shovel", 1, ToolType.SHOVEL, 132, null));
        registry.register((short) 42, "voxel:stone_axe", new ItemType((short) 42, "voxel:stone_axe", 1, ToolType.AXE, 132, null));
        registry.register((short) 43, "voxel:stone_sword", new ItemType((short) 43, "voxel:stone_sword", 1, ToolType.NONE, 96, null));
        return registry;
    }

    private static void block(Registry<ItemType> registry, short id, String key) {
        registry.register(id, key, new ItemType(id, key, 64, ToolType.NONE, 0, key));
    }
}
