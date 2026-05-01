package dev.voxelgame.common.gameplay;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AlphaMilestones {
    private static final List<AlphaMilestone> DEFAULT_CHAIN = List.of(
            milestone(
                    AlphaMilestoneKey.SPAWN_SECURED,
                    1,
                    "Spawn secured",
                    "Safe spawn point selected near starter campsite/resources.",
                    List.of(),
                    List.of("voxel:campfire", "voxel:storage_crate"),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields", "voxel:lakeside"),
                    List.of("voxel:campsite"),
                    "Show short found-camp hint and starter resource affordances.",
                    "player.milestones.spawn_secured",
                    "Server/worldgen owns safe spawn, campsite plan and initial chunk stream.",
                    "WORLD_SMOKE_TESTS.md Spawn baseline"
            ),
            milestone(
                    AlphaMilestoneKey.FIRST_SUPPLY,
                    2,
                    "First supply",
                    "Player collects any starter supply from campsite or safe meadow.",
                    List.of("voxel:twig", "voxel:pebble", "voxel:fiber", "voxel:berries", "voxel:wild_herbs", "voxel:mushroom"),
                    List.of("voxel:twig_pile", "voxel:small_stone", "voxel:wild_grass", "voxel:berry_bush", "voxel:herb_planter", "voxel:red_mushroom"),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields"),
                    List.of("voxel:campsite"),
                    "Feedback log: first supply gathered, then recipe unlocks stay short.",
                    "player.milestones.first_supply",
                    "Server validates pickup/block interaction and authoritative inventory snapshot.",
                    "Spawn baseline: gather twig/pebble/fiber/berries"
            ),
            milestone(
                    AlphaMilestoneKey.FIRST_FOOD,
                    3,
                    "First food",
                    "Player collects or eats a weak early food before night pressure matters.",
                    List.of("voxel:berries", "voxel:mushroom", "voxel:wild_herbs", "voxel:cooked_berries", "voxel:roasted_mushroom"),
                    List.of("voxel:berry_bush", "voxel:red_mushroom", "voxel:herb_planter"),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields", "voxel:pine_forest"),
                    List.of("voxel:campsite"),
                    "HUD hunger feedback and first-food journal hint.",
                    "player.milestones.first_food",
                    "Server validates eat/drink action and clamps hunger/heal result.",
                    "Spawn baseline: collect and consume berries or mushroom"
            ),
            milestone(
                    AlphaMilestoneKey.FIRST_RECIPE,
                    4,
                    "First recipe unlock",
                    "Player has enough starter supplies to reveal a first recipe.",
                    List.of("voxel:twig", "voxel:pebble", "voxel:fiber"),
                    List.of(),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields"),
                    List.of("voxel:campsite"),
                    "Feedback log announces the first unlocked recipe once.",
                    "player.milestones.first_recipe",
                    "Recipe availability is derived from canonical item keys and station rules.",
                    "Spawn baseline: recipe unlock appears after starter pickup"
            ),
            milestone(
                    AlphaMilestoneKey.FIRST_TOOL,
                    5,
                    "First tool",
                    "Player crafts Stone Knife, Stone Axe or Stone Pickaxe.",
                    List.of("voxel:stone_knife", "voxel:stone_axe", "voxel:stone_pickaxe"),
                    List.of(),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields"),
                    List.of("voxel:campsite"),
                    "Feedback log names the first crafted tool and tooltips show effective use.",
                    "player.milestones.first_tool",
                    "Server validates recipe ingredients, inventory capacity and durability item state.",
                    "Spawn baseline: craft first stone tool"
            ),
            milestone(
                    AlphaMilestoneKey.CAMPFIRE_CRAFTED,
                    6,
                    "Campfire crafted",
                    "Player crafts a campfire from stone/twigs and receives placement intent.",
                    List.of("voxel:campfire", "voxel:stone", "voxel:twig"),
                    List.of("voxel:campfire"),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields"),
                    List.of("voxel:campsite"),
                    "Feedback log: place it before nightfall.",
                    "player.milestones.campfire_crafted",
                    "Server validates craft request and later validates place range/collision.",
                    "Night / Campfire / Lighting smoke"
            ),
            milestone(
                    AlphaMilestoneKey.CAMPFIRE_LIT,
                    7,
                    "Campfire lit",
                    "Player fuels or activates an in-world campfire.",
                    List.of("voxel:campfire", "voxel:twig", "voxel:stick", "voxel:dry_grass", "voxel:charcoal"),
                    List.of("voxel:campfire_active"),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields"),
                    List.of("voxel:campsite"),
                    "HUD/status: warmth, light and comfort source are active.",
                    "player.milestones.campfire_lit",
                    "Server validates fuel, campfire state transition and inventory removal.",
                    "Night / Campfire / Lighting smoke"
            ),
            milestone(
                    AlphaMilestoneKey.STORAGE_READY,
                    8,
                    "Storage ready",
                    "Player crafts/opens the first storage crate near base.",
                    List.of("voxel:storage_crate", "voxel:skyroot_planks", "voxel:fiber"),
                    List.of("voxel:storage_crate"),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields", "voxel:pine_forest"),
                    List.of("voxel:campsite"),
                    "Interaction HUD identifies storage and inventory feedback handles full cases.",
                    "player.milestones.storage_ready",
                    "Server owns storage slots, transfer validation and block entity save revision.",
                    "Storage / Loot Persistence smoke"
            ),
            milestone(
                    AlphaMilestoneKey.WORKBENCH_READY,
                    9,
                    "Workbench ready",
                    "Player uses Pine Forest resin/bark path to build a Workbench.",
                    List.of("voxel:workbench", "voxel:tool_handle", "voxel:resin", "voxel:bark_strip", "voxel:skyroot_planks"),
                    List.of("voxel:workbench", "voxel:tree_stump"),
                    List.of("voxel:pine_forest"),
                    List.of("voxel:campsite", "voxel:simple_house"),
                    "Crafting UI shows Workbench-gated recipes and missing-station state.",
                    "player.milestones.workbench_ready",
                    "Server validates station radius through CraftingStationRules.",
                    "Pine forest smoke plus craft Workbench"
            ),
            milestone(
                    AlphaMilestoneKey.FIRST_COMFORT,
                    10,
                    "First comfort level",
                    "Player reaches a visible base comfort threshold from campfire/decor/storage.",
                    List.of("voxel:campfire", "voxel:storage_crate", "voxel:sleeping_mat", "voxel:lantern", "voxel:woven_rug"),
                    List.of("voxel:campfire_active", "voxel:storage_crate", "voxel:sleeping_mat", "voxel:lantern", "voxel:woven_rug"),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields"),
                    List.of("voxel:campsite"),
                    "HUD comfort meter explains one concrete source without grind language.",
                    "player.milestones.first_comfort",
                    "Server/player stats consume ComfortRules output; client only displays.",
                    "Spawn baseline: comfort HUD changes near campfire/storage"
            ),
            milestone(
                    AlphaMilestoneKey.COOKING_POT_READY,
                    11,
                    "Cooking pot ready",
                    "Player reaches Lakeside clay/reeds and builds Cooking Pot/water container path.",
                    List.of("voxel:cooking_pot", "voxel:clay_lump", "voxel:clay_pot", "voxel:clay_bowl", "voxel:water_container", "voxel:copper_ingot"),
                    List.of("voxel:cooking_pot", "voxel:clay_deposit", "voxel:reeds"),
                    List.of("voxel:lakeside"),
                    List.of("voxel:campsite"),
                    "Cooking UI shows water/container requirement and better food outputs.",
                    "player.milestones.cooking_pot_ready",
                    "Server validates pot station, ingredients, fuel/time and inventory output.",
                    "River / lakeside plus Cooking Station smoke"
            ),
            milestone(
                    AlphaMilestoneKey.FORGE_READY,
                    12,
                    "Forge ready",
                    "Player converts copper/clay/charcoal into Forge access and smelts iron.",
                    List.of("voxel:forge", "voxel:raw_copper", "voxel:copper_ingot", "voxel:charcoal", "voxel:raw_iron", "voxel:iron_ingot"),
                    List.of("voxel:forge", "voxel:copper_ore", "voxel:iron_ore"),
                    List.of("voxel:highlands", "voxel:old_ruins"),
                    List.of("voxel:watchtower"),
                    "Forge UI shows heat/fuel, metal progression and rejected outputs.",
                    "player.milestones.forge_ready",
                    "Server validates station type, smelt recipe, fuel and no-dupe output.",
                    "Highlands/old ruins smoke plus forge recipe"
            ),
            milestone(
                    AlphaMilestoneKey.FIRST_RUIN_DISCOVERED,
                    13,
                    "First ruin discovered",
                    "Player enters an old ruin/small ruin and sees loot/lore affordance.",
                    List.of("voxel:stone_pickaxe", "voxel:iron_pickaxe", "voxel:ancient_fragment", "voxel:glow_crystal"),
                    List.of("voxel:mossy_stone", "voxel:ancient_lantern", "voxel:glow_crystal_node"),
                    List.of("voxel:old_ruins", "voxel:mushroom_grove"),
                    List.of("voxel:small_ruin", "voxel:mushroom_circle"),
                    "Journal records structure discovery and points to ancient fragments.",
                    "player.milestones.first_ruin_discovered",
                    "Server marks structure discovery and idempotent loot markers.",
                    "Old ruins smoke"
            ),
            milestone(
                    AlphaMilestoneKey.FIRST_RARE_FIND,
                    14,
                    "First rare find",
                    "Player obtains Ancient Fragment, Ruin Key, Ruin Seal, Ancient Lantern or Lost Charm.",
                    List.of("voxel:ancient_fragment", "voxel:ruin_key", "voxel:ruin_seal", "voxel:ancient_lantern", "voxel:lost_charm"),
                    List.of("voxel:storage_crate", "voxel:ancient_lantern"),
                    List.of("voxel:old_ruins", "voxel:mushroom_grove"),
                    List.of("voxel:small_ruin"),
                    "Feedback log and Journal treat rare loot as progression, not just pickup spam.",
                    "player.milestones.first_rare_find",
                    "Server fills generated loot once and persists consumed loot marker.",
                    "Old ruins plus Storage / Loot Persistence smoke"
            )
    );

    private static final Map<AlphaMilestoneKey, AlphaMilestone> BY_KEY = byKey(DEFAULT_CHAIN);

    private AlphaMilestones() {
    }

    public static List<AlphaMilestone> defaultChain() {
        return DEFAULT_CHAIN;
    }

    public static Optional<AlphaMilestone> find(AlphaMilestoneKey key) {
        return Optional.ofNullable(BY_KEY.get(key));
    }

    public static Set<String> requiredItemKeys() {
        Set<String> itemKeys = new LinkedHashSet<>();
        for (AlphaMilestone milestone : DEFAULT_CHAIN) {
            itemKeys.addAll(milestone.requiredItemKeys());
        }
        return Collections.unmodifiableSet(itemKeys);
    }

    private static AlphaMilestone milestone(
            AlphaMilestoneKey key,
            int order,
            String title,
            String trigger,
            List<String> requiredItemKeys,
            List<String> requiredBlockKeys,
            List<String> requiredBiomeKeys,
            List<String> requiredStructureKeys,
            String uiFeedback,
            String saveStateKey,
            String serverValidation,
            String smokeCheck
    ) {
        return new AlphaMilestone(
                key,
                order,
                title,
                trigger,
                requiredItemKeys,
                requiredBlockKeys,
                requiredBiomeKeys,
                requiredStructureKeys,
                uiFeedback,
                saveStateKey,
                serverValidation,
                smokeCheck
        );
    }

    private static Map<AlphaMilestoneKey, AlphaMilestone> byKey(List<AlphaMilestone> milestones) {
        Map<AlphaMilestoneKey, AlphaMilestone> byKey = new EnumMap<>(AlphaMilestoneKey.class);
        for (AlphaMilestone milestone : milestones) {
            AlphaMilestone duplicate = byKey.put(milestone.key(), milestone);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate alpha milestone " + milestone.key());
            }
        }
        return Collections.unmodifiableMap(byKey);
    }
}
