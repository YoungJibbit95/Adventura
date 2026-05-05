package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.gameplay.AlphaMilestoneKey;
import dev.voxelgame.common.gameplay.JournalProgression;
import dev.voxelgame.common.gameplay.status.StatusEffectType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BiomeProgressionCatalog {
    private static final List<BiomeProgressionProfile> DEFAULT_PROFILES = List.of(
            profile(
                    "voxel:meadow",
                    BiomeProgressionTier.STARTER,
                    true,
                    "Open rolling grass with short sightlines and sparse starter clusters.",
                    "Warm daylight, soft greens and low-contrast shadows.",
                    blocks(Blocks.TWIG_PILE, Blocks.SMALL_STONE, Blocks.BERRY_BUSH, Blocks.HERB_PLANTER),
                    keys("voxel:cozy_sheep", "voxel:forest_bunny"),
                    keys("voxel:campsite", "voxel:simple_house"),
                    keys("Low pressure; only navigation and night preparation matter early."),
                    effects(),
                    milestones(
                            AlphaMilestoneKey.SPAWN_SECURED,
                            AlphaMilestoneKey.FIRST_SUPPLY,
                            AlphaMilestoneKey.FIRST_FOOD,
                            AlphaMilestoneKey.FIRST_RECIPE,
                            AlphaMilestoneKey.FIRST_TOOL,
                            AlphaMilestoneKey.CAMPFIRE_CRAFTED,
                            AlphaMilestoneKey.CAMPFIRE_LIT
                    ),
                    keys("voxel:twig", "voxel:pebble", "voxel:fiber", "voxel:berries", "voxel:wild_herbs", "voxel:campfire"),
                    keys(JournalProgression.COZY_MEADOW, JournalProgression.SPAWN_CAMPSITE, JournalProgression.SIMPLE_HOUSE),
                    keys("Starter gathering loop", "safe camp expansion", "first food and fire"),
                    "Spawn planner may pick meadow/cozy_meadow/flower_fields family near campsite resources."
            ),
            profile(
                    "voxel:cozy_meadow",
                    BiomeProgressionTier.STARTER,
                    true,
                    "Readable home-field biome with flowers, soft paths and settlement hooks.",
                    "Bright cozy greens, warmer flower accents and friendly ambient movement.",
                    blocks(Blocks.TWIG_PILE, Blocks.SMALL_STONE, Blocks.BERRY_BUSH, Blocks.HERB_PLANTER),
                    keys("voxel:cozy_sheep", "voxel:forest_bunny", "voxel:firefly_swarm"),
                    keys("voxel:campsite", "voxel:compact_village", "voxel:simple_house"),
                    keys("Very low danger; base comfort and over-collection are the main pacing risks."),
                    effects(StatusEffectType.COZY),
                    milestones(
                            AlphaMilestoneKey.SPAWN_SECURED,
                            AlphaMilestoneKey.FIRST_SUPPLY,
                            AlphaMilestoneKey.FIRST_FOOD,
                            AlphaMilestoneKey.FIRST_RECIPE,
                            AlphaMilestoneKey.STORAGE_READY,
                            AlphaMilestoneKey.FIRST_COMFORT
                    ),
                    keys("voxel:berries", "voxel:wild_herbs", "voxel:storage_crate", "voxel:woven_rug", "voxel:sleeping_mat"),
                    keys(JournalProgression.COZY_MEADOW, JournalProgression.SPAWN_CAMPSITE, JournalProgression.COMPACT_VILLAGE),
                    keys("Comfort building", "starter food", "settlement discovery"),
                    "Cozy meadow is a primary spawn-family fallback and duplicates meadow starter resources."
            ),
            profile(
                    "voxel:flower_fields",
                    BiomeProgressionTier.STARTER,
                    true,
                    "Wide flower carpets with high readability and denser herb/berry patches.",
                    "Sunlit greens with saturated flower accents and gentle bloom-like contrast.",
                    blocks(Blocks.SUN_BLOOM, Blocks.HERB_PLANTER, Blocks.BERRY_BUSH),
                    keys("voxel:cozy_sheep", "voxel:forest_bunny"),
                    keys("voxel:campsite", "voxel:compact_village", "voxel:simple_house"),
                    keys("Safe biome; visual density must not hide interactable starter resources."),
                    effects(StatusEffectType.COZY),
                    milestones(
                            AlphaMilestoneKey.FIRST_SUPPLY,
                            AlphaMilestoneKey.FIRST_FOOD,
                            AlphaMilestoneKey.FIRST_RECIPE,
                            AlphaMilestoneKey.FIRST_COMFORT
                    ),
                    keys("voxel:sun_bloom", "voxel:wild_herbs", "voxel:berries", "voxel:calming_tea"),
                    keys(JournalProgression.COZY_MEADOW, JournalProgression.COMPACT_VILLAGE),
                    keys("Herb gathering", "comfort decoration", "gentle food route"),
                    "Spawn-family fallback duplicates meadow starter resources and keeps food/herb access common."
            ),
            profile(
                    "voxel:pine_forest",
                    BiomeProgressionTier.STATION_ROUTE,
                    true,
                    "Medium-density trunks and stump clearings that frame short exploration loops.",
                    "Cool greens, darker bark contrast and warm shelter lights from houses.",
                    blocks(Blocks.PINE_LOG, Blocks.PINE_LEAVES, Blocks.TREE_STUMP, Blocks.RED_MUSHROOM, Blocks.SMALL_STONE),
                    keys("voxel:forest_bunny", "voxel:little_boar"),
                    keys("voxel:simple_house", "voxel:small_ruin"),
                    keys("Reduced sightlines and mild creature pressure; avoid making resin/bark feel like a grind."),
                    effects(),
                    milestones(
                            AlphaMilestoneKey.FIRST_FOOD,
                            AlphaMilestoneKey.STORAGE_READY,
                            AlphaMilestoneKey.WORKBENCH_READY,
                            AlphaMilestoneKey.FIRST_RUIN_DISCOVERED
                    ),
                    keys("voxel:resin", "voxel:bark_strip", "voxel:mushroom", "voxel:tool_handle", "voxel:workbench"),
                    keys(JournalProgression.PINE_FOREST, JournalProgression.SIMPLE_HOUSE, JournalProgression.SMALL_RUIN),
                    keys("Workbench route", "mushroom food", "first ruin foreshadowing"),
                    "Workbench sources appear in profile resources, tree-stump features and simple-house structure routes."
            ),
            profile(
                    "voxel:skyroot_forest",
                    BiomeProgressionTier.EARLY_EXPANSION,
                    false,
                    "Tall skyroot canopy with small clearings, paths and village silhouettes.",
                    "Soft green canopy shade with warm wood and lantern accents.",
                    blocks(Blocks.SKYROOT_LOG, Blocks.SKYROOT_LEAVES, Blocks.TREE_STUMP, Blocks.BERRY_BUSH, Blocks.HERB_PLANTER),
                    keys("voxel:forest_bunny", "voxel:little_boar"),
                    keys("voxel:compact_village", "voxel:simple_house"),
                    keys("Navigation density and village spawn contracts are the main complexity."),
                    effects(StatusEffectType.COZY),
                    milestones(AlphaMilestoneKey.STORAGE_READY, AlphaMilestoneKey.FIRST_COMFORT, AlphaMilestoneKey.WORKBENCH_READY),
                    keys("voxel:skyroot_log", "voxel:skyroot_planks", "voxel:berries", "voxel:wild_herbs"),
                    keys(JournalProgression.SIMPLE_HOUSE, JournalProgression.COMPACT_VILLAGE),
                    keys("Building wood", "settlement landmark", "comfort inspiration"),
                    "Optional expansion biome; core workbench route remains available through pine_forest."
            ),
            profile(
                    "voxel:lakeside",
                    BiomeProgressionTier.STATION_ROUTE,
                    true,
                    "Open shorelines, reed bands and clay edges around readable water shapes.",
                    "Cool blue-green light with reflective water and brighter reed silhouettes.",
                    blocks(Blocks.CLAY, Blocks.CLAY_DEPOSIT, Blocks.REEDS, Blocks.BERRY_BUSH, Blocks.SMALL_STONE),
                    keys("voxel:forest_bunny", "voxel:firefly_swarm"),
                    keys("voxel:campsite", "voxel:simple_house"),
                    keys("Water traversal and wet status can slow movement; keep shore exits readable."),
                    effects(StatusEffectType.WET),
                    milestones(AlphaMilestoneKey.COOKING_POT_READY, AlphaMilestoneKey.FIRST_FOOD, AlphaMilestoneKey.FIRST_COMFORT),
                    keys("voxel:clay_lump", "voxel:reed_bundle", "voxel:clay_pot", "voxel:clay_bowl", "voxel:water_container", "voxel:cooking_pot"),
                    keys(JournalProgression.LAKESIDE, JournalProgression.SPAWN_CAMPSITE, JournalProgression.SIMPLE_HOUSE),
                    keys("Cooking pot route", "water containers", "better food loop"),
                    "Lakeside is a named station route and should appear through climate/river placement, not rare structure luck."
            ),
            profile(
                    "voxel:mire",
                    BiomeProgressionTier.OPTIONAL_EXPEDITION,
                    false,
                    "Low wet ground, clay pockets, mushrooms and obscured but shallow routes.",
                    "Muted greens, damp clay tones and small glow accents.",
                    blocks(Blocks.CLAY_DEPOSIT, Blocks.RED_MUSHROOM, Blocks.MUSHROOM_CLUSTER),
                    keys("voxel:moss_snail", "voxel:firefly_swarm"),
                    keys("voxel:small_ruin"),
                    keys("Wet footing, low visibility and future poison hooks; avoid mandatory early traversal."),
                    effects(StatusEffectType.WET),
                    milestones(AlphaMilestoneKey.COOKING_POT_READY, AlphaMilestoneKey.FIRST_RUIN_DISCOVERED),
                    keys("voxel:clay_lump", "voxel:mushroom", "voxel:moss_clump", "voxel:slime_drop"),
                    keys(JournalProgression.MUSHROOM_GROVE, JournalProgression.SMALL_RUIN),
                    keys("Mushroom supply", "clay backup", "ruin side route"),
                    "Optional biome; lakeside and mushroom_grove carry the core cooking/ruin routes."
            ),
            profile(
                    "voxel:mushroom_grove",
                    BiomeProgressionTier.ADVENTURE_ROUTE,
                    true,
                    "Rounded grove pockets, mushroom rings and glowing resource silhouettes.",
                    "Dim green-purple ambience with readable glow nodes and soft landmark contrast.",
                    blocks(Blocks.MUSHROOM_CLUSTER, Blocks.GLOW_MUSHROOM, Blocks.GLOW_CRYSTAL_NODE, Blocks.SPORE_BLOSSOM, Blocks.CLAY_DEPOSIT),
                    keys("voxel:moss_snail", "voxel:firefly_swarm"),
                    keys("voxel:mushroom_circle", "voxel:small_ruin"),
                    keys("Night-like visibility and rare-resource pull; keep glow nodes inspectable, not hidden."),
                    effects(),
                    milestones(AlphaMilestoneKey.FIRST_RUIN_DISCOVERED, AlphaMilestoneKey.FIRST_RARE_FIND, AlphaMilestoneKey.COOKING_POT_READY),
                    keys("voxel:glow_mushroom_cap", "voxel:spore_blossom", "voxel:glow_crystal", "voxel:glow_mushroom_stew", "voxel:spore_tea"),
                    keys(JournalProgression.MUSHROOM_GROVE, JournalProgression.MUSHROOM_CIRCLE, JournalProgression.FIRST_RUIN_MAP),
                    keys("Glow food", "glow crystal source", "ruin foreshadowing"),
                    "Mushroom grove is an alternate ruin-adjacent route; old_ruins remains the direct target."
            ),
            profile(
                    "voxel:old_ruins",
                    BiomeProgressionTier.ADVENTURE_ROUTE,
                    true,
                    "Broken stone fields, watchtower silhouettes and compact mossy ruin clusters.",
                    "Desaturated green-gray with ancient lantern/glow-crystal accents.",
                    blocks(Blocks.MOSSY_STONE, Blocks.GLOW_CRYSTAL_NODE, Blocks.SMALL_STONE, Blocks.HERB_PLANTER, Blocks.ANCIENT_LANTERN),
                    keys("voxel:forest_bunny", "voxel:little_boar"),
                    keys("voxel:watchtower", "voxel:small_ruin"),
                    keys("Higher resource value and rare loot pressure; avoid combat-heavy mandatory gates."),
                    effects(),
                    milestones(AlphaMilestoneKey.FORGE_READY, AlphaMilestoneKey.FIRST_RUIN_DISCOVERED, AlphaMilestoneKey.FIRST_RARE_FIND),
                    keys("voxel:ancient_fragment", "voxel:glow_crystal", "voxel:ruin_key", "voxel:ruin_seal", "voxel:lost_charm"),
                    keys(JournalProgression.OLD_RUINS, JournalProgression.SMALL_RUIN, JournalProgression.WATCHTOWER, JournalProgression.FIRST_RUIN_MAP),
                    keys("First ruin target", "rare loot", "forge-to-ruin bridge"),
                    "Old ruins has both biome resources and small_ruin/watchtower entries; rare loot is marker-driven and saved once."
            ),
            profile(
                    "voxel:highlands",
                    BiomeProgressionTier.STATION_ROUTE,
                    true,
                    "Rocky elevation, sparse grass and long views toward watchtower silhouettes.",
                    "Crisp daylight, pale stone and high-contrast ore/glow accents.",
                    blocks(Blocks.SMALL_STONE, Blocks.GLOW_CRYSTAL_NODE, Blocks.GRAVEL, Blocks.COPPER_ORE, Blocks.IRON_ORE),
                    keys("voxel:forest_bunny", "voxel:little_boar"),
                    keys("voxel:watchtower", "voxel:small_ruin"),
                    keys("Slope readability and ore route pacing; do not make forge access depend on rare peaks."),
                    effects(),
                    milestones(AlphaMilestoneKey.FORGE_READY, AlphaMilestoneKey.FIRST_RUIN_DISCOVERED),
                    keys("voxel:raw_copper", "voxel:copper_ingot", "voxel:raw_iron", "voxel:iron_ingot", "voxel:forge"),
                    keys(JournalProgression.WATCHTOWER, "voxel:journal_recipe_forge"),
                    keys("Forge route", "ore gathering", "map-like lookout"),
                    "Highlands and old_ruins both expose metal/glow routes so forge progress is not one-biome fragile."
            ),
            profile(
                    "voxel:frost_peaks",
                    BiomeProgressionTier.OPTIONAL_EXPEDITION,
                    false,
                    "Snow ridges, ice pockets and sparse ruin silhouettes in hard-to-cross terrain.",
                    "Cold blue-white light with glow crystals as navigation anchors.",
                    blocks(Blocks.SNOW, Blocks.ICE, Blocks.SMALL_STONE, Blocks.GLOW_CRYSTAL_NODE),
                    keys("voxel:forest_bunny"),
                    keys("voxel:small_ruin"),
                    keys("Chilled status and traversal exposure; keep this optional until warm gear exists."),
                    effects(StatusEffectType.CHILLED),
                    milestones(AlphaMilestoneKey.FIRST_RUIN_DISCOVERED, AlphaMilestoneKey.FIRST_RARE_FIND),
                    keys("voxel:glow_crystal", "voxel:ancient_fragment", "voxel:ruin_key"),
                    keys(JournalProgression.SMALL_RUIN, JournalProgression.FIRST_RUIN_MAP),
                    keys("Optional challenge route", "glow crystal supply", "ruin variation"),
                    "Optional expedition biome; first ruin remains reachable through old_ruins/mushroom_grove."
            ),
            profile(
                    "voxel:sun_dunes",
                    BiomeProgressionTier.OPTIONAL_EXPEDITION,
                    false,
                    "Open sand fields, cactus clusters and isolated desert-well silhouettes.",
                    "Bright warm sand, hard sun contrast and sparse cool water accents.",
                    blocks(Blocks.SMALL_STONE, Blocks.CACTUS, Blocks.SAND),
                    keys("voxel:little_boar"),
                    keys("voxel:desert_well"),
                    keys("Exposure, long sightlines and future heat hooks; keep water affordances obvious."),
                    effects(),
                    milestones(AlphaMilestoneKey.FIRST_SUPPLY, AlphaMilestoneKey.COOKING_POT_READY),
                    keys("voxel:cactus", "voxel:water_container", "voxel:clay_lump"),
                    keys(JournalProgression.DESERT_WELL),
                    keys("Cactus supply", "landmark navigation", "future desert encounter route"),
                    "Optional biome; cooking and water progression must remain secure through lakeside."
            )
    );
    private static final Map<String, BiomeProgressionProfile> PROFILES_BY_KEY = profilesByKey(DEFAULT_PROFILES);

    private BiomeProgressionCatalog() {
    }

    public static List<BiomeProgressionProfile> defaultProfiles() {
        return DEFAULT_PROFILES;
    }

    public static Optional<BiomeProgressionProfile> find(String biomeKey) {
        return Optional.ofNullable(PROFILES_BY_KEY.get(biomeKey));
    }

    private static BiomeProgressionProfile profile(
            String biomeKey,
            BiomeProgressionTier tier,
            boolean coreProgression,
            String silhouette,
            String colorLightMood,
            List<Short> resourceBlockIds,
            List<String> ambientEntityKeys,
            List<String> structureKeys,
            List<String> dangerNotes,
            List<StatusEffectType> hazardEffectTypes,
            List<AlphaMilestoneKey> milestoneKeys,
            List<String> itemProgressionKeys,
            List<String> journalEntryKeys,
            List<String> returnReasons,
            String seedRobustnessContract
    ) {
        return new BiomeProgressionProfile(
                biomeKey,
                tier,
                coreProgression,
                silhouette,
                colorLightMood,
                resourceBlockIds,
                ambientEntityKeys,
                structureKeys,
                dangerNotes,
                hazardEffectTypes,
                milestoneKeys,
                itemProgressionKeys,
                journalEntryKeys,
                returnReasons,
                seedRobustnessContract
        );
    }

    private static List<Short> blocks(short... blockIds) {
        java.util.ArrayList<Short> blocks = new java.util.ArrayList<>(blockIds.length);
        for (short blockId : blockIds) {
            blocks.add(blockId);
        }
        return List.copyOf(blocks);
    }

    private static List<String> keys(String... keys) {
        return List.of(keys);
    }

    private static List<StatusEffectType> effects(StatusEffectType... types) {
        return List.of(types);
    }

    private static List<AlphaMilestoneKey> milestones(AlphaMilestoneKey... keys) {
        return List.of(keys);
    }

    private static Map<String, BiomeProgressionProfile> profilesByKey(List<BiomeProgressionProfile> profiles) {
        Map<String, BiomeProgressionProfile> byKey = new LinkedHashMap<>();
        for (BiomeProgressionProfile profile : profiles) {
            BiomeProgressionProfile duplicate = byKey.put(profile.biomeKey(), profile);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate biome progression profile " + profile.biomeKey());
            }
        }
        return Collections.unmodifiableMap(byKey);
    }
}
