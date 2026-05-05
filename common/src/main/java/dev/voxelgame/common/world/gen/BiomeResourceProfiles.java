package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.block.Blocks;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BiomeResourceProfiles {
    private static final Map<String, BiomeResourceProfile> PROFILES = Map.ofEntries(
            entry(profile(
                    "voxel:meadow",
                    Set.of(Blocks.GRASS, Blocks.DIRT, Blocks.STONE),
                    Set.of(Blocks.WILD_GRASS, Blocks.SUN_BLOOM),
                    Set.of(Blocks.TWIG_PILE, Blocks.SMALL_STONE, Blocks.BERRY_BUSH, Blocks.HERB_PLANTER),
                    Set.of(Blocks.COAL_ORE),
                    List.of("voxel:campsite", "voxel:simple_house"),
                    List.of("voxel:cozy_sheep", "voxel:forest_bunny"),
                    Set.of(Blocks.SUN_BLOOM),
                    "voxel:cozy_meadow",
                    details(
                            detail(Blocks.SMALL_STONE, 0.008),
                            detail(Blocks.BERRY_BUSH, 0.015),
                            detail(Blocks.HERB_PLANTER, 0.022)
                    )
            )),
            entry(profile(
                    "voxel:cozy_meadow",
                    Set.of(Blocks.GRASS, Blocks.DIRT, Blocks.STONE),
                    Set.of(Blocks.WILD_GRASS, Blocks.SUN_BLOOM),
                    Set.of(Blocks.TWIG_PILE, Blocks.SMALL_STONE, Blocks.BERRY_BUSH, Blocks.HERB_PLANTER),
                    Set.of(Blocks.COAL_ORE),
                    List.of("voxel:campsite", "voxel:compact_village", "voxel:simple_house"),
                    List.of("voxel:cozy_sheep", "voxel:forest_bunny", "voxel:firefly_swarm"),
                    Set.of(Blocks.SUN_BLOOM),
                    "voxel:cozy_meadow",
                    details(
                            detail(Blocks.SMALL_STONE, 0.008),
                            detail(Blocks.BERRY_BUSH, 0.015),
                            detail(Blocks.HERB_PLANTER, 0.022)
                    )
            )),
            entry(profile(
                    "voxel:flower_fields",
                    Set.of(Blocks.GRASS, Blocks.DIRT, Blocks.STONE),
                    Set.of(Blocks.WILD_GRASS, Blocks.SUN_BLOOM),
                    Set.of(Blocks.SUN_BLOOM, Blocks.HERB_PLANTER, Blocks.BERRY_BUSH),
                    Set.of(Blocks.COAL_ORE),
                    List.of("voxel:campsite", "voxel:compact_village", "voxel:simple_house"),
                    List.of("voxel:cozy_sheep", "voxel:forest_bunny"),
                    Set.of(Blocks.SUN_BLOOM),
                    "voxel:flower_fields",
                    details(
                            detail(Blocks.SUN_BLOOM, 0.018),
                            detail(Blocks.HERB_PLANTER, 0.030),
                            detail(Blocks.BERRY_BUSH, 0.035)
                    )
            )),
            entry(profile(
                    "voxel:pine_forest",
                    Set.of(Blocks.GRASS, Blocks.DIRT, Blocks.STONE),
                    Set.of(Blocks.PINE_LOG, Blocks.PINE_LEAVES, Blocks.TREE_STUMP, Blocks.RED_MUSHROOM),
                    Set.of(Blocks.PINE_LOG, Blocks.PINE_LEAVES, Blocks.TREE_STUMP, Blocks.RED_MUSHROOM, Blocks.SMALL_STONE),
                    Set.of(Blocks.COAL_ORE, Blocks.COPPER_ORE),
                    List.of("voxel:simple_house", "voxel:small_ruin"),
                    List.of("voxel:forest_bunny", "voxel:little_boar"),
                    Set.of(Blocks.RED_MUSHROOM),
                    "voxel:pine_forest",
                    details(
                            detail(Blocks.TREE_STUMP, 0.012),
                            detail(Blocks.RED_MUSHROOM, 0.022),
                            detail(Blocks.SMALL_STONE, 0.032)
                    )
            )),
            entry(profile(
                    "voxel:skyroot_forest",
                    Set.of(Blocks.GRASS, Blocks.DIRT, Blocks.STONE),
                    Set.of(Blocks.SKYROOT_LOG, Blocks.SKYROOT_LEAVES, Blocks.TREE_STUMP),
                    Set.of(Blocks.TREE_STUMP, Blocks.BERRY_BUSH, Blocks.HERB_PLANTER),
                    Set.of(Blocks.COAL_ORE),
                    List.of("voxel:compact_village", "voxel:simple_house"),
                    List.of("voxel:forest_bunny", "voxel:little_boar"),
                    Set.of(Blocks.HERB_PLANTER),
                    "voxel:skyroot_forest",
                    details(
                            detail(Blocks.TREE_STUMP, 0.012),
                            detail(Blocks.BERRY_BUSH, 0.022),
                            detail(Blocks.HERB_PLANTER, 0.032)
                    )
            )),
            entry(profile(
                    "voxel:lakeside",
                    Set.of(Blocks.GRASS, Blocks.CLAY, Blocks.SAND, Blocks.GRAVEL, Blocks.STONE),
                    Set.of(Blocks.REEDS, Blocks.WILD_GRASS, Blocks.BERRY_BUSH),
                    Set.of(Blocks.CLAY, Blocks.CLAY_DEPOSIT, Blocks.REEDS, Blocks.BERRY_BUSH, Blocks.SMALL_STONE),
                    Set.of(Blocks.COAL_ORE),
                    List.of("voxel:campsite", "voxel:simple_house"),
                    List.of("voxel:forest_bunny", "voxel:firefly_swarm"),
                    Set.of(Blocks.REEDS),
                    "voxel:lakeside",
                    details(
                            detail(Blocks.CLAY_DEPOSIT, 0.014),
                            detail(Blocks.REEDS, 0.028),
                            detail(Blocks.BERRY_BUSH, 0.036),
                            detail(Blocks.SMALL_STONE, 0.044)
                    )
            )),
            entry(profile(
                    "voxel:mire",
                    Set.of(Blocks.GRASS, Blocks.CLAY, Blocks.STONE),
                    Set.of(Blocks.RED_MUSHROOM, Blocks.MUSHROOM_CLUSTER, Blocks.WILD_GRASS),
                    Set.of(Blocks.CLAY_DEPOSIT, Blocks.RED_MUSHROOM, Blocks.MUSHROOM_CLUSTER),
                    Set.of(Blocks.COAL_ORE, Blocks.COPPER_ORE),
                    List.of("voxel:small_ruin"),
                    List.of("voxel:moss_snail", "voxel:firefly_swarm"),
                    Set.of(Blocks.MUSHROOM_CLUSTER),
                    "voxel:mire",
                    details(
                            detail(Blocks.CLAY_DEPOSIT, 0.018),
                            detail(Blocks.RED_MUSHROOM, 0.030),
                            detail(Blocks.MUSHROOM_CLUSTER, 0.040)
                    )
            )),
            entry(profile(
                    "voxel:mushroom_grove",
                    Set.of(Blocks.GRASS, Blocks.CLAY, Blocks.MOSSY_STONE, Blocks.STONE),
                    Set.of(Blocks.RED_MUSHROOM, Blocks.MUSHROOM_CLUSTER, Blocks.GLOW_MUSHROOM, Blocks.SPORE_BLOSSOM),
                    Set.of(Blocks.MUSHROOM_CLUSTER, Blocks.GLOW_MUSHROOM, Blocks.GLOW_CRYSTAL_NODE, Blocks.RED_MUSHROOM, Blocks.CLAY_DEPOSIT),
                    Set.of(Blocks.GLOW_CRYSTAL_NODE, Blocks.COAL_ORE),
                    List.of("voxel:mushroom_circle", "voxel:small_ruin"),
                    List.of("voxel:moss_snail", "voxel:firefly_swarm"),
                    Set.of(Blocks.SPORE_BLOSSOM, Blocks.GLOW_CRYSTAL_NODE),
                    "voxel:mushroom_grove",
                    details(
                            detail(Blocks.SPORE_BLOSSOM, 0.004),
                            detail(Blocks.GLOW_MUSHROOM, 0.016),
                            detail(Blocks.MUSHROOM_CLUSTER, 0.032),
                            detail(Blocks.RED_MUSHROOM, 0.044),
                            detail(Blocks.CLAY_DEPOSIT, 0.052)
                    )
            )),
            entry(profile(
                    "voxel:old_ruins",
                    Set.of(Blocks.MOSSY_STONE, Blocks.GRAVEL, Blocks.STONE),
                    Set.of(Blocks.HERB_PLANTER, Blocks.WILD_GRASS),
                    Set.of(Blocks.MOSSY_STONE, Blocks.GLOW_CRYSTAL_NODE, Blocks.SMALL_STONE, Blocks.HERB_PLANTER, Blocks.ANCIENT_LANTERN),
                    Set.of(Blocks.COAL_ORE, Blocks.COPPER_ORE, Blocks.IRON_ORE, Blocks.GLOW_CRYSTAL_NODE),
                    List.of("voxel:watchtower", "voxel:small_ruin"),
                    List.of("voxel:forest_bunny", "voxel:little_boar"),
                    Set.of(Blocks.GLOW_CRYSTAL_NODE, Blocks.ANCIENT_LANTERN),
                    "voxel:old_ruins",
                    details(
                            detail(Blocks.GLOW_CRYSTAL_NODE, 0.014),
                            detail(Blocks.SMALL_STONE, 0.030),
                            detail(Blocks.HERB_PLANTER, 0.038)
                    )
            )),
            entry(profile(
                    "voxel:highlands",
                    Set.of(Blocks.GRASS, Blocks.DIRT, Blocks.STONE, Blocks.GRAVEL),
                    Set.of(Blocks.SMALL_STONE, Blocks.WILD_GRASS),
                    Set.of(Blocks.SMALL_STONE, Blocks.GLOW_CRYSTAL_NODE, Blocks.GRAVEL),
                    Set.of(Blocks.COAL_ORE, Blocks.COPPER_ORE, Blocks.IRON_ORE),
                    List.of("voxel:watchtower", "voxel:small_ruin"),
                    List.of("voxel:forest_bunny", "voxel:little_boar"),
                    Set.of(Blocks.GLOW_CRYSTAL_NODE),
                    "voxel:highlands",
                    details(
                            detail(Blocks.SMALL_STONE, 0.018),
                            detail(Blocks.GLOW_CRYSTAL_NODE, 0.026)
                    )
            )),
            entry(profile(
                    "voxel:frost_peaks",
                    Set.of(Blocks.SNOW, Blocks.GRAVEL, Blocks.ICE, Blocks.STONE),
                    Set.of(Blocks.SMALL_STONE),
                    Set.of(Blocks.SNOW, Blocks.ICE, Blocks.SMALL_STONE, Blocks.GLOW_CRYSTAL_NODE),
                    Set.of(Blocks.COAL_ORE, Blocks.COPPER_ORE, Blocks.GLOW_CRYSTAL_NODE),
                    List.of("voxel:small_ruin"),
                    List.of("voxel:forest_bunny"),
                    Set.of(Blocks.GLOW_CRYSTAL_NODE),
                    "voxel:frost_peaks",
                    details(
                            detail(Blocks.SMALL_STONE, 0.016),
                            detail(Blocks.GLOW_CRYSTAL_NODE, 0.026)
                    )
            )),
            entry(profile(
                    "voxel:sun_dunes",
                    Set.of(Blocks.SAND, Blocks.STONE),
                    Set.of(Blocks.CACTUS),
                    Set.of(Blocks.SMALL_STONE, Blocks.CACTUS),
                    Set.of(Blocks.COAL_ORE, Blocks.COPPER_ORE),
                    List.of("voxel:desert_well"),
                    List.of("voxel:little_boar"),
                    Set.of(Blocks.CACTUS),
                    "voxel:sun_dunes",
                    details(detail(Blocks.SMALL_STONE, 0.016))
            ))
    );

    private BiomeResourceProfiles() {
    }

    public static Collection<BiomeResourceProfile> all() {
        return PROFILES.values();
    }

    public static BiomeResourceProfile forBiome(String biomeKey) {
        BiomeResourceProfile profile = PROFILES.get(biomeKey);
        if (profile != null) {
            return profile;
        }
        return PROFILES.get("voxel:cozy_meadow");
    }

    public static short detailResourceFor(String biomeKey, double roll) {
        short featureTableBlock = WorldFeatureTables.detailResourceFor(biomeKey, roll);
        return featureTableBlock == Blocks.AIR ? forBiome(biomeKey).detailResource(roll) : featureTableBlock;
    }

    private static Map.Entry<String, BiomeResourceProfile> entry(BiomeResourceProfile profile) {
        return Map.entry(profile.biomeKey(), profile);
    }

    private static BiomeResourceProfile profile(
            String biomeKey,
            Set<Short> surfaceBlocks,
            Set<Short> vegetationBlocks,
            Set<Short> resourceBlocks,
            Set<Short> oreBlocks,
            List<String> structureKeys,
            List<String> ambientEntityKeys,
            Set<Short> rareFeatureBlocks,
            String tintKey,
            List<BiomeResourceProfile.ResourceEntry> detailResources
    ) {
        return new BiomeResourceProfile(
                biomeKey,
                surfaceBlocks,
                vegetationBlocks,
                resourceBlocks,
                oreBlocks,
                structureKeys,
                ambientEntityKeys,
                rareFeatureBlocks,
                tintKey,
                detailResources
        );
    }

    private static List<BiomeResourceProfile.ResourceEntry> details(BiomeResourceProfile.ResourceEntry... entries) {
        return List.of(entries);
    }

    private static BiomeResourceProfile.ResourceEntry detail(short blockId, double maxRollExclusive) {
        return new BiomeResourceProfile.ResourceEntry(blockId, maxRollExclusive);
    }
}
