package dev.voxelgame.common.loot;

import java.util.List;

public final class LootTables {
    private LootTables() {
    }

    public static LootTableRegistry createDefaultRegistry() {
        LootTableRegistry registry = new LootTableRegistry();
        registry.register(new LootTable("voxel:ruin_crate", 1, List.of(
                new LootEntry("voxel:coal", 1, 3, 1.0),
                new LootEntry("voxel:raw_copper", 1, 2, 0.45),
                new LootEntry("voxel:glow_crystal", 1, 1, 0.16),
                new LootEntry("voxel:stone_pickaxe", 1, 1, 0.08)
        )));
        registry.register(new LootTable("voxel:campsite_crate", 1, List.of(
                new LootEntry("voxel:stick", 2, 5, 1.0),
                new LootEntry("voxel:berries", 1, 3, 0.65),
                new LootEntry("voxel:torch", 1, 2, 0.55),
                new LootEntry("voxel:charcoal", 1, 2, 0.35)
        )));
        registry.register(new LootTable("voxel:village_house_crate", 1, List.of(
                new LootEntry("voxel:apple", 1, 3, 1.0),
                new LootEntry("voxel:berries", 1, 4, 0.70),
                new LootEntry("voxel:fiber", 2, 6, 0.45),
                new LootEntry("voxel:clay_bowl", 1, 1, 0.22)
        )));
        return registry;
    }
}
