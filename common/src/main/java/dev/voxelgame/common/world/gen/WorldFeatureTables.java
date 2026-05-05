package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.block.Blocks;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.voxelgame.common.world.gen.FeatureEntry.biomes;
import static dev.voxelgame.common.world.gen.FeatureEntry.block;
import static dev.voxelgame.common.world.gen.FeatureEntry.structure;
import static dev.voxelgame.common.world.gen.FeatureEntry.surfaces;

public final class WorldFeatureTables {
    public static final String MEADOW_SURFACE_FEATURES = "meadow_surface_features";
    public static final String FLOWER_FIELDS_FEATURES = "flower_fields_features";
    public static final String PINE_FOREST_FEATURES = "pine_forest_features";
    public static final String SKYROOT_FOREST_FEATURES = "skyroot_forest_features";
    public static final String LAKESIDE_FEATURES = "lakeside_features";
    public static final String MIRE_FEATURES = "mire_features";
    public static final String MUSHROOM_GROVE_FEATURES = "mushroom_grove_features";
    public static final String OLD_RUINS_FEATURES = "old_ruins_features";
    public static final String HIGHLANDS_FEATURES = "highlands_features";
    public static final String FROST_FEATURES = "frost_features";
    public static final String SUN_DUNES_FEATURES = "sun_dunes_features";

    private static final Map<String, FeatureTable> TABLES = tablesByKey(List.of(
            table(MEADOW_SURFACE_FEATURES,
                    block("voxel:feature_meadow_small_stone", FeatureKind.ROCK, Blocks.SMALL_STONE, 0.008, 1.0, 1, 1, 3, 1,
                            biomes("voxel:meadow", "voxel:cozy_meadow"), surfaces(Blocks.GRASS, Blocks.DIRT), true, true, 0.0, ""),
                    block("voxel:feature_meadow_berry_bush", FeatureKind.PLANT_CLUSTER, Blocks.BERRY_BUSH, 0.015, 1.0, 1, 2, 4, 1,
                            biomes("voxel:meadow", "voxel:cozy_meadow"), surfaces(Blocks.GRASS, Blocks.DIRT), true, true, 0.0, ""),
                    block("voxel:feature_meadow_herbs", FeatureKind.PLANT_CLUSTER, Blocks.HERB_PLANTER, 0.022, 1.0, 1, 2, 4, 1,
                            biomes("voxel:meadow", "voxel:cozy_meadow"), surfaces(Blocks.GRASS, Blocks.DIRT), true, true, 0.0, ""),
                    structure("voxel:feature_campsite_meadow", FeatureKind.CAMPSITE, "voxel:campsite", 0.002, 1.0, 28,
                            biomes("voxel:meadow", "voxel:cozy_meadow"), surfaces(Blocks.GRASS, Blocks.DIRT), true,
                            "voxel:campsite_crate", "", "voxel:journal_spawn_campsite"),
                    structure("voxel:feature_village_meadow", FeatureKind.VILLAGE_PART, "voxel:compact_village", 0.0008, 0.35, 64,
                            biomes("voxel:cozy_meadow"), surfaces(Blocks.GRASS, Blocks.DIRT), true,
                            "voxel:village_house_crate", "voxel:villager_spawn", "voxel:journal_structure_village")
            ),
            table(FLOWER_FIELDS_FEATURES,
                    block("voxel:feature_flower_sun_bloom", FeatureKind.PLANT_CLUSTER, Blocks.SUN_BLOOM, 0.018, 1.0, 2, 5, 2, 1,
                            biomes("voxel:flower_fields"), surfaces(Blocks.GRASS, Blocks.DIRT), true, true, 0.0, ""),
                    block("voxel:feature_flower_herbs", FeatureKind.PLANT_CLUSTER, Blocks.HERB_PLANTER, 0.030, 0.8, 1, 3, 3, 1,
                            biomes("voxel:flower_fields"), surfaces(Blocks.GRASS, Blocks.DIRT), true, true, 0.0, ""),
                    block("voxel:feature_flower_berries", FeatureKind.PLANT_CLUSTER, Blocks.BERRY_BUSH, 0.035, 0.6, 1, 2, 4, 1,
                            biomes("voxel:flower_fields"), surfaces(Blocks.GRASS, Blocks.DIRT), true, true, 0.0, ""),
                    structure("voxel:feature_campsite_flowers", FeatureKind.CAMPSITE, "voxel:campsite", 0.0015, 0.7, 30,
                            biomes("voxel:flower_fields"), surfaces(Blocks.GRASS, Blocks.DIRT), true,
                            "voxel:campsite_crate", "", "voxel:journal_spawn_campsite")
            ),
            table(PINE_FOREST_FEATURES,
                    block("voxel:feature_pine_stump", FeatureKind.TREE, Blocks.TREE_STUMP, 0.012, 1.0, 1, 1, 5, 2,
                            biomes("voxel:pine_forest"), surfaces(Blocks.GRASS, Blocks.DIRT), true, true, 0.0, ""),
                    block("voxel:feature_pine_red_mushroom", FeatureKind.PLANT_CLUSTER, Blocks.RED_MUSHROOM, 0.022, 0.9, 1, 3, 3, 2,
                            biomes("voxel:pine_forest"), surfaces(Blocks.GRASS, Blocks.DIRT), true, true, 0.0, ""),
                    block("voxel:feature_pine_small_stone", FeatureKind.ROCK, Blocks.SMALL_STONE, 0.032, 0.7, 1, 2, 3, 2,
                            biomes("voxel:pine_forest"), surfaces(Blocks.GRASS, Blocks.DIRT), true, true, 0.0, ""),
                    structure("voxel:feature_simple_house_pine", FeatureKind.VILLAGE_PART, "voxel:simple_house", 0.0014, 0.55, 42,
                            biomes("voxel:pine_forest"), surfaces(Blocks.GRASS, Blocks.DIRT), true,
                            "", "", "voxel:journal_structure_simple_house"),
                    structure("voxel:feature_small_ruin_pine", FeatureKind.RUIN, "voxel:small_ruin", 0.0008, 0.35, 48,
                            biomes("voxel:pine_forest"), surfaces(Blocks.GRASS, Blocks.DIRT, Blocks.STONE), true,
                            "voxel:ruin_crate", "", "voxel:journal_structure_small_ruin")
            ),
            table(SKYROOT_FOREST_FEATURES,
                    block("voxel:feature_skyroot_stump", FeatureKind.TREE, Blocks.TREE_STUMP, 0.012, 1.0, 1, 1, 5, 2,
                            biomes("voxel:skyroot_forest"), surfaces(Blocks.GRASS, Blocks.DIRT), true, true, 0.0, ""),
                    block("voxel:feature_skyroot_berries", FeatureKind.PLANT_CLUSTER, Blocks.BERRY_BUSH, 0.022, 0.75, 1, 2, 4, 1,
                            biomes("voxel:skyroot_forest"), surfaces(Blocks.GRASS, Blocks.DIRT), true, true, 0.0, ""),
                    block("voxel:feature_skyroot_herbs", FeatureKind.PLANT_CLUSTER, Blocks.HERB_PLANTER, 0.032, 0.75, 1, 2, 4, 1,
                            biomes("voxel:skyroot_forest"), surfaces(Blocks.GRASS, Blocks.DIRT), true, true, 0.0, ""),
                    structure("voxel:feature_village_skyroot", FeatureKind.VILLAGE_PART, "voxel:compact_village", 0.0010, 0.6, 64,
                            biomes("voxel:skyroot_forest"), surfaces(Blocks.GRASS, Blocks.DIRT), true,
                            "voxel:village_house_crate", "voxel:villager_spawn", "voxel:journal_structure_village")
            ),
            table(LAKESIDE_FEATURES,
                    block("voxel:feature_lakeside_clay", FeatureKind.RESOURCE_NODE, Blocks.CLAY_DEPOSIT, 0.014, 1.0, 1, 2, 3, 1,
                            biomes("voxel:lakeside"), surfaces(Blocks.GRASS, Blocks.CLAY, Blocks.SAND), false, true, 0.0, ""),
                    block("voxel:feature_lakeside_reeds", FeatureKind.PLANT_CLUSTER, Blocks.REEDS, 0.028, 1.0, 2, 5, 2, 1,
                            biomes("voxel:lakeside"), surfaces(Blocks.GRASS, Blocks.CLAY, Blocks.SAND), false, true, 0.0, ""),
                    block("voxel:feature_lakeside_berries", FeatureKind.PLANT_CLUSTER, Blocks.BERRY_BUSH, 0.036, 0.6, 1, 2, 4, 1,
                            biomes("voxel:lakeside"), surfaces(Blocks.GRASS, Blocks.CLAY), true, true, 0.0, ""),
                    block("voxel:feature_lakeside_stone", FeatureKind.ROCK, Blocks.SMALL_STONE, 0.044, 0.5, 1, 2, 3, 1,
                            biomes("voxel:lakeside"), surfaces(Blocks.GRASS, Blocks.CLAY, Blocks.GRAVEL), true, true, 0.0, ""),
                    structure("voxel:feature_campsite_lakeside", FeatureKind.CAMPSITE, "voxel:campsite", 0.0015, 0.6, 32,
                            biomes("voxel:lakeside"), surfaces(Blocks.GRASS, Blocks.CLAY), true,
                            "voxel:campsite_crate", "", "voxel:journal_spawn_campsite")
            ),
            table(MIRE_FEATURES,
                    block("voxel:feature_mire_clay", FeatureKind.RESOURCE_NODE, Blocks.CLAY_DEPOSIT, 0.018, 1.0, 1, 2, 3, 1,
                            biomes("voxel:mire"), surfaces(Blocks.GRASS, Blocks.CLAY), false, true, 0.0, ""),
                    block("voxel:feature_mire_red_mushroom", FeatureKind.PLANT_CLUSTER, Blocks.RED_MUSHROOM, 0.030, 0.8, 1, 3, 3, 1,
                            biomes("voxel:mire"), surfaces(Blocks.GRASS, Blocks.CLAY), true, true, 0.0, ""),
                    block("voxel:feature_mire_mushroom_cluster", FeatureKind.PLANT_CLUSTER, Blocks.MUSHROOM_CLUSTER, 0.040, 0.7, 1, 3, 3, 1,
                            biomes("voxel:mire"), surfaces(Blocks.GRASS, Blocks.CLAY), true, true, 0.0, ""),
                    structure("voxel:feature_small_ruin_mire", FeatureKind.RUIN, "voxel:small_ruin", 0.0010, 0.5, 48,
                            biomes("voxel:mire"), surfaces(Blocks.GRASS, Blocks.CLAY, Blocks.STONE), true,
                            "voxel:ruin_crate", "", "voxel:journal_structure_small_ruin")
            ),
            table(MUSHROOM_GROVE_FEATURES,
                    block("voxel:feature_grove_spore_blossom", FeatureKind.PLANT_CLUSTER, Blocks.SPORE_BLOSSOM, 0.004, 0.35, 1, 1, 6, 2,
                            biomes("voxel:mushroom_grove"), surfaces(Blocks.GRASS, Blocks.CLAY, Blocks.MOSSY_STONE), true, true, 0.004, "voxel:journal_biome_mushroom_grove"),
                    block("voxel:feature_grove_glow_mushroom", FeatureKind.PLANT_CLUSTER, Blocks.GLOW_MUSHROOM, 0.016, 0.85, 1, 3, 3, 2,
                            biomes("voxel:mushroom_grove"), surfaces(Blocks.GRASS, Blocks.CLAY, Blocks.MOSSY_STONE), true, true, 0.0, ""),
                    block("voxel:feature_grove_mushroom_cluster", FeatureKind.PLANT_CLUSTER, Blocks.MUSHROOM_CLUSTER, 0.032, 1.0, 1, 4, 3, 2,
                            biomes("voxel:mushroom_grove"), surfaces(Blocks.GRASS, Blocks.CLAY, Blocks.MOSSY_STONE), true, true, 0.0, ""),
                    block("voxel:feature_grove_red_mushroom", FeatureKind.PLANT_CLUSTER, Blocks.RED_MUSHROOM, 0.044, 0.75, 1, 3, 3, 2,
                            biomes("voxel:mushroom_grove"), surfaces(Blocks.GRASS, Blocks.CLAY, Blocks.MOSSY_STONE), true, true, 0.0, ""),
                    block("voxel:feature_grove_clay", FeatureKind.RESOURCE_NODE, Blocks.CLAY_DEPOSIT, 0.052, 0.45, 1, 2, 4, 2,
                            biomes("voxel:mushroom_grove"), surfaces(Blocks.GRASS, Blocks.CLAY), false, true, 0.0, ""),
                    structure("voxel:feature_mushroom_circle", FeatureKind.RUIN, "voxel:mushroom_circle", 0.0012, 0.65, 48,
                            biomes("voxel:mushroom_grove"), surfaces(Blocks.GRASS, Blocks.CLAY, Blocks.MOSSY_STONE), true,
                            "", "voxel:firefly_swarm", "voxel:journal_structure_mushroom_circle")
            ),
            table(OLD_RUINS_FEATURES,
                    block("voxel:feature_ruins_glow_crystal", FeatureKind.RESOURCE_NODE, Blocks.GLOW_CRYSTAL_NODE, 0.014, 0.8, 1, 1, 7, 3,
                            biomes("voxel:old_ruins"), surfaces(Blocks.MOSSY_STONE, Blocks.GRAVEL, Blocks.STONE), true, true, 0.010, "voxel:journal_biome_old_ruins"),
                    block("voxel:feature_ruins_small_stone", FeatureKind.ROCK, Blocks.SMALL_STONE, 0.030, 0.7, 1, 3, 3, 2,
                            biomes("voxel:old_ruins"), surfaces(Blocks.MOSSY_STONE, Blocks.GRAVEL, Blocks.STONE), true, true, 0.0, ""),
                    block("voxel:feature_ruins_herbs", FeatureKind.PLANT_CLUSTER, Blocks.HERB_PLANTER, 0.038, 0.4, 1, 2, 5, 2,
                            biomes("voxel:old_ruins"), surfaces(Blocks.MOSSY_STONE, Blocks.GRAVEL, Blocks.STONE), true, true, 0.0, ""),
                    structure("voxel:feature_small_ruin_old_ruins", FeatureKind.RUIN, "voxel:small_ruin", 0.0020, 1.0, 44,
                            biomes("voxel:old_ruins"), surfaces(Blocks.MOSSY_STONE, Blocks.GRAVEL, Blocks.STONE), true,
                            "voxel:ruin_crate", "voxel:little_boar", "voxel:journal_structure_small_ruin"),
                    structure("voxel:feature_watchtower_old_ruins", FeatureKind.RUIN, "voxel:watchtower", 0.0012, 0.55, 48,
                            biomes("voxel:old_ruins"), surfaces(Blocks.MOSSY_STONE, Blocks.GRAVEL, Blocks.STONE), true,
                            "", "voxel:little_boar", "voxel:journal_structure_watchtower")
            ),
            table(HIGHLANDS_FEATURES,
                    block("voxel:feature_highlands_small_stone", FeatureKind.ROCK, Blocks.SMALL_STONE, 0.018, 1.0, 1, 3, 3, 3,
                            biomes("voxel:highlands"), surfaces(Blocks.GRASS, Blocks.DIRT, Blocks.STONE, Blocks.GRAVEL), true, true, 0.0, ""),
                    block("voxel:feature_highlands_glow_crystal", FeatureKind.RESOURCE_NODE, Blocks.GLOW_CRYSTAL_NODE, 0.026, 0.4, 1, 1, 8, 4,
                            biomes("voxel:highlands"), surfaces(Blocks.STONE, Blocks.GRAVEL), true, true, 0.008, "voxel:journal_recipe_forge"),
                    structure("voxel:feature_watchtower_highlands", FeatureKind.VILLAGE_PART, "voxel:watchtower", 0.0015, 0.65, 52,
                            biomes("voxel:highlands"), surfaces(Blocks.GRASS, Blocks.STONE, Blocks.GRAVEL), true,
                            "", "voxel:forest_bunny", "voxel:journal_structure_watchtower")
            ),
            table(FROST_FEATURES,
                    block("voxel:feature_frost_small_stone", FeatureKind.ROCK, Blocks.SMALL_STONE, 0.016, 0.8, 1, 3, 3, 4,
                            biomes("voxel:frost_peaks"), surfaces(Blocks.SNOW, Blocks.GRAVEL, Blocks.ICE, Blocks.STONE), true, true, 0.0, ""),
                    block("voxel:feature_frost_glow_crystal", FeatureKind.RESOURCE_NODE, Blocks.GLOW_CRYSTAL_NODE, 0.026, 0.45, 1, 1, 8, 4,
                            biomes("voxel:frost_peaks"), surfaces(Blocks.SNOW, Blocks.GRAVEL, Blocks.ICE, Blocks.STONE), true, true, 0.010, "voxel:journal_map_first_ruin"),
                    structure("voxel:feature_small_ruin_frost", FeatureKind.RUIN, "voxel:small_ruin", 0.0010, 0.5, 56,
                            biomes("voxel:frost_peaks"), surfaces(Blocks.SNOW, Blocks.GRAVEL, Blocks.STONE), true,
                            "voxel:ruin_crate", "voxel:snow_hare", "voxel:journal_structure_small_ruin")
            ),
            table(SUN_DUNES_FEATURES,
                    block("voxel:feature_dunes_small_stone", FeatureKind.ROCK, Blocks.SMALL_STONE, 0.016, 1.0, 1, 3, 4, 2,
                            biomes("voxel:sun_dunes"), surfaces(Blocks.SAND, Blocks.STONE), true, true, 0.0, ""),
                    block("voxel:feature_dunes_cactus", FeatureKind.PLANT_CLUSTER, Blocks.CACTUS, 0.026, 0.5, 1, 2, 6, 2,
                            biomes("voxel:sun_dunes"), surfaces(Blocks.SAND), true, true, 0.0, ""),
                    structure("voxel:feature_desert_well", FeatureKind.VILLAGE_PART, "voxel:desert_well", 0.0012, 0.65, 48,
                            biomes("voxel:sun_dunes"), surfaces(Blocks.SAND, Blocks.STONE), false,
                            "", "voxel:dune_crawler", "voxel:journal_structure_desert_well")
            )
    ));

    private static final Map<String, String> DETAIL_TABLE_BY_BIOME = Map.ofEntries(
            Map.entry("voxel:meadow", MEADOW_SURFACE_FEATURES),
            Map.entry("voxel:cozy_meadow", MEADOW_SURFACE_FEATURES),
            Map.entry("voxel:flower_fields", FLOWER_FIELDS_FEATURES),
            Map.entry("voxel:pine_forest", PINE_FOREST_FEATURES),
            Map.entry("voxel:skyroot_forest", SKYROOT_FOREST_FEATURES),
            Map.entry("voxel:lakeside", LAKESIDE_FEATURES),
            Map.entry("voxel:mire", MIRE_FEATURES),
            Map.entry("voxel:mushroom_grove", MUSHROOM_GROVE_FEATURES),
            Map.entry("voxel:old_ruins", OLD_RUINS_FEATURES),
            Map.entry("voxel:highlands", HIGHLANDS_FEATURES),
            Map.entry("voxel:frost_peaks", FROST_FEATURES),
            Map.entry("voxel:sun_dunes", SUN_DUNES_FEATURES)
    );

    private WorldFeatureTables() {
    }

    public static Map<String, FeatureTable> defaultTables() {
        return TABLES;
    }

    public static Optional<FeatureTable> find(String tableKey) {
        return Optional.ofNullable(TABLES.get(tableKey));
    }

    public static Optional<FeatureTable> detailTableForBiome(String biomeKey) {
        return Optional.ofNullable(DETAIL_TABLE_BY_BIOME.get(biomeKey))
                .flatMap(WorldFeatureTables::find);
    }

    public static short detailResourceFor(String biomeKey, double roll) {
        return detailTableForBiome(biomeKey)
                .map(table -> table.blockForRoll(biomeKey, roll))
                .orElse(Blocks.AIR);
    }

    private static FeatureTable table(String key, FeatureEntry... entries) {
        return new FeatureTable(key, List.of(entries));
    }

    private static Map<String, FeatureTable> tablesByKey(List<FeatureTable> tables) {
        Map<String, FeatureTable> byKey = new LinkedHashMap<>();
        for (FeatureTable table : tables) {
            FeatureTable duplicate = byKey.put(table.key(), table);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate feature table " + table.key());
            }
        }
        return Collections.unmodifiableMap(byKey);
    }
}
