package dev.voxelgame.common.gameplay;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class JournalProgression {
    public static final String SPAWN_CAMPSITE = "voxel:journal_spawn_campsite";
    public static final String COZY_MEADOW = "voxel:journal_biome_cozy_meadow";
    public static final String PINE_FOREST = "voxel:journal_biome_pine_forest";
    public static final String LAKESIDE = "voxel:journal_biome_lakeside";
    public static final String MUSHROOM_GROVE = "voxel:journal_biome_mushroom_grove";
    public static final String OLD_RUINS = "voxel:journal_biome_old_ruins";
    public static final String SMALL_RUIN = "voxel:journal_structure_small_ruin";
    public static final String MUSHROOM_CIRCLE = "voxel:journal_structure_mushroom_circle";
    public static final String SIMPLE_HOUSE = "voxel:journal_structure_simple_house";
    public static final String WATCHTOWER = "voxel:journal_structure_watchtower";
    public static final String COMPACT_VILLAGE = "voxel:journal_structure_village";
    public static final String DESERT_WELL = "voxel:journal_structure_desert_well";
    public static final String ANCIENT_FRAGMENT = "voxel:journal_lore_ancient_fragment";
    public static final String RUIN_KEY = "voxel:journal_lore_ruin_key";
    public static final String RUIN_SEAL = "voxel:journal_lore_ruin_seal";
    public static final String LOST_CHARM = "voxel:journal_lore_lost_charm";
    public static final String ANCIENT_LANTERN = "voxel:journal_lore_ancient_lantern";
    public static final String FIRST_RUIN_MAP = "voxel:journal_map_first_ruin";

    private static final List<JournalEntryDefinition> DEFAULT_ENTRIES = List.of(
            entry(
                    SPAWN_CAMPSITE,
                    JournalEntryKind.STRUCTURE_NOTE,
                    1,
                    "Starter Campsite",
                    "Records the safe spawn campsite, early supplies and the first place the player can return to.",
                    List.of("discovery.structure.voxel:campsite", "milestone.voxel:spawn_secured"),
                    List.of("voxel:campfire", "voxel:storage_crate", "voxel:berries", "voxel:twig", "voxel:pebble"),
                    List.of("voxel:campfire", "voxel:storage_crate", "voxel:twig_pile", "voxel:small_stone"),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields", "voxel:lakeside"),
                    List.of("voxel:campsite"),
                    List.of(),
                    List.of("voxel:campfire", "voxel:storage_crate"),
                    List.of(AlphaMilestoneKey.SPAWN_SECURED, AlphaMilestoneKey.FIRST_SUPPLY),
                    "player.journal.entries.spawn_campsite",
                    "Show one new-note badge and a short return-to-camp hint.",
                    "Server/worldgen emits discovery once after safe spawn and campsite chunk stream."
            ),
            entry(
                    COZY_MEADOW,
                    JournalEntryKind.BIOME_NOTE,
                    2,
                    "Cozy Meadow",
                    "Safe starter biome for berries, herbs, twigs, pebbles and gentle ambient creatures.",
                    List.of("discovery.biome.voxel:cozy_meadow"),
                    List.of("voxel:berries", "voxel:wild_herbs", "voxel:twig", "voxel:pebble"),
                    List.of("voxel:berry_bush", "voxel:herb_planter", "voxel:twig_pile", "voxel:small_stone"),
                    List.of("voxel:cozy_meadow"),
                    List.of(),
                    List.of("voxel:cozy_sheep", "voxel:forest_bunny"),
                    List.of("voxel:healing_snack"),
                    List.of(AlphaMilestoneKey.FIRST_FOOD, AlphaMilestoneKey.FIRST_RECIPE),
                    "player.journal.entries.cozy_meadow",
                    "Journal tab highlights starter resources without pushing urgency.",
                    "Server marks first biome discovery from authoritative player position."
            ),
            entry(
                    PINE_FOREST,
                    JournalEntryKind.BIOME_NOTE,
                    3,
                    "Pine Forest",
                    "Mid-starter biome for resin, bark strips, mushrooms and the workbench path.",
                    List.of("discovery.biome.voxel:pine_forest"),
                    List.of("voxel:resin", "voxel:bark_strip", "voxel:mushroom", "voxel:tool_handle"),
                    List.of("voxel:pine_log", "voxel:tree_stump", "voxel:red_mushroom"),
                    List.of("voxel:pine_forest"),
                    List.of("voxel:simple_house"),
                    List.of("voxel:little_boar", "voxel:forest_bunny"),
                    List.of("voxel:tool_handle", "voxel:workbench"),
                    List.of(AlphaMilestoneKey.WORKBENCH_READY),
                    "player.journal.entries.pine_forest",
                    "Journal shows resin/bark as workbench sources after discovery.",
                    "Server emits biome discovery and recipe-history unlocks from inventory/crafting facts."
            ),
            entry(
                    LAKESIDE,
                    JournalEntryKind.BIOME_NOTE,
                    4,
                    "Lakeside",
                    "Clay, reeds and water containers make this the cooking-pot route.",
                    List.of("discovery.biome.voxel:lakeside"),
                    List.of("voxel:clay_lump", "voxel:reed_bundle", "voxel:water_container", "voxel:clay_pot", "voxel:clay_bowl"),
                    List.of("voxel:clay_deposit", "voxel:reeds", "voxel:cooking_pot"),
                    List.of("voxel:lakeside"),
                    List.of(),
                    List.of("voxel:moss_snail"),
                    List.of("voxel:clay_bowl", "voxel:clay_pot", "voxel:water_container", "voxel:cooking_pot"),
                    List.of(AlphaMilestoneKey.COOKING_POT_READY),
                    "player.journal.entries.lakeside",
                    "Journal note ties clay, reeds and water to better food.",
                    "Server emits biome discovery and later station-ready milestone from authoritative inventory/station state."
            ),
            entry(
                    MUSHROOM_GROVE,
                    JournalEntryKind.BIOME_NOTE,
                    5,
                    "Mushroom Grove",
                    "Glow mushrooms, spores and quiet light guide food and ruin-adjacent exploration.",
                    List.of("discovery.biome.voxel:mushroom_grove"),
                    List.of("voxel:mushroom", "voxel:glow_mushroom_cap", "voxel:spore_blossom", "voxel:glow_crystal"),
                    List.of("voxel:mushroom_cluster", "voxel:glow_mushroom", "voxel:spore_blossom", "voxel:glow_crystal_node"),
                    List.of("voxel:mushroom_grove"),
                    List.of("voxel:mushroom_circle"),
                    List.of("voxel:firefly_swarm", "voxel:moss_snail"),
                    List.of("voxel:glow_mushroom_stew", "voxel:spore_tea"),
                    List.of(AlphaMilestoneKey.FIRST_RUIN_DISCOVERED),
                    "player.journal.entries.mushroom_grove",
                    "Journal flags glow food and ancient-light sources after first grove discovery.",
                    "Server marks biome discovery and one-shot structure/lore events for grove markers."
            ),
            entry(
                    OLD_RUINS,
                    JournalEntryKind.BIOME_NOTE,
                    6,
                    "Old Ruins",
                    "Ancient stone, glow crystals and rare loot make this the first exploration target.",
                    List.of("discovery.biome.voxel:old_ruins"),
                    List.of("voxel:ancient_fragment", "voxel:glow_crystal", "voxel:ruin_key", "voxel:ruin_seal"),
                    List.of("voxel:mossy_stone", "voxel:ancient_lantern", "voxel:glow_crystal_node"),
                    List.of("voxel:old_ruins"),
                    List.of("voxel:small_ruin"),
                    List.of(),
                    List.of("voxel:ancient_lantern", "voxel:ruin_key", "voxel:ruin_seal"),
                    List.of(AlphaMilestoneKey.FIRST_RUIN_DISCOVERED, AlphaMilestoneKey.FIRST_RARE_FIND),
                    "player.journal.entries.old_ruins",
                    "Journal creates a low-pressure ruin page with rare-find slots.",
                    "Server marks discovery from biome/structure authority and persists rare loot events idempotently."
            ),
            entry(
                    "voxel:journal_creature_cozy_sheep",
                    JournalEntryKind.CREATURE_NOTE,
                    7,
                    "Cozy Sheep",
                    "Gentle ambient creature for comfort identity and future cloth/friendship hooks.",
                    List.of("discovery.entity.voxel:cozy_sheep"),
                    List.of("voxel:cloth"),
                    List.of("voxel:woven_rug"),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields"),
                    List.of(),
                    List.of("voxel:cozy_sheep"),
                    List.of("voxel:cloth", "voxel:woven_rug"),
                    List.of(AlphaMilestoneKey.FIRST_COMFORT),
                    "player.journal.entries.cozy_sheep",
                    "Journal note should read as observation, not hunting instruction.",
                    "Server emits entity discovery from interest/entity tracking, not client-only sight checks."
            ),
            entry(
                    "voxel:journal_creature_fireflies",
                    JournalEntryKind.CREATURE_NOTE,
                    8,
                    "Firefly Swarm",
                    "Ambient light cue for Mushroom Grove, comfort mood and safe night readability.",
                    List.of("discovery.entity.voxel:firefly_swarm"),
                    List.of("voxel:glow_crystal", "voxel:glow_mushroom_cap"),
                    List.of("voxel:glow_mushroom", "voxel:glow_crystal_node"),
                    List.of("voxel:mushroom_grove"),
                    List.of("voxel:mushroom_circle"),
                    List.of("voxel:firefly_swarm"),
                    List.of(),
                    List.of(AlphaMilestoneKey.FIRST_RUIN_DISCOVERED),
                    "player.journal.entries.fireflies",
                    "Journal uses a soft new-creature badge only once.",
                    "Server marks discovery when tracked entity enters player interest for the first time."
            ),
            entry(
                    SMALL_RUIN,
                    JournalEntryKind.STRUCTURE_NOTE,
                    9,
                    "Small Ruin",
                    "First ruin structure with mossy stone, loot markers and rare lore affordances.",
                    List.of("discovery.structure.voxel:small_ruin"),
                    List.of("voxel:ancient_fragment", "voxel:ruin_key", "voxel:ruin_seal", "voxel:lost_charm"),
                    List.of("voxel:mossy_stone", "voxel:storage_crate", "voxel:lantern"),
                    List.of("voxel:old_ruins"),
                    List.of("voxel:small_ruin"),
                    List.of(),
                    List.of("voxel:ruin_key", "voxel:ruin_seal"),
                    List.of(AlphaMilestoneKey.FIRST_RUIN_DISCOVERED, AlphaMilestoneKey.FIRST_RARE_FIND),
                    "player.journal.entries.small_ruin",
                    "Journal opens a ruin checklist with discovered/unknown rare slots.",
                    "Server emits structure discovery and rare loot journal events from generated structure markers."
            ),
            entry(
                    MUSHROOM_CIRCLE,
                    JournalEntryKind.STRUCTURE_NOTE,
                    10,
                    "Mushroom Circle",
                    "A quiet grove landmark that foreshadows glow crystals, spores and ancient paths.",
                    List.of("discovery.structure.voxel:mushroom_circle"),
                    List.of("voxel:glow_crystal", "voxel:glow_mushroom_cap", "voxel:spore_blossom"),
                    List.of("voxel:mossy_path", "voxel:glow_crystal_node", "voxel:glow_mushroom", "voxel:spore_blossom"),
                    List.of("voxel:mushroom_grove"),
                    List.of("voxel:mushroom_circle"),
                    List.of("voxel:firefly_swarm"),
                    List.of("voxel:glow_mushroom_stew", "voxel:spore_tea"),
                    List.of(AlphaMilestoneKey.FIRST_RUIN_DISCOVERED),
                    "player.journal.entries.mushroom_circle",
                    "Journal note hints at glow resources without hard quest pressure.",
                    "Server emits structure discovery from worldgen marker proximity."
            ),
            entry(
                    "voxel:journal_recipe_campfire",
                    JournalEntryKind.RECIPE_HISTORY,
                    11,
                    "Campfire Recipes",
                    "Tracks the first cooked foods, charcoal and pottery firing path.",
                    List.of("recipe.unlock.voxel:cooked_berries", "recipe.unlock.voxel:roasted_mushroom", "milestone.voxel:campfire_lit"),
                    List.of("voxel:cooked_berries", "voxel:roasted_mushroom", "voxel:charcoal", "voxel:clay_bowl", "voxel:clay_pot"),
                    List.of("voxel:campfire", "voxel:campfire_active"),
                    List.of("voxel:cozy_meadow", "voxel:pine_forest", "voxel:lakeside"),
                    List.of("voxel:campsite"),
                    List.of(),
                    List.of("voxel:cooked_berries", "voxel:roasted_mushroom", "voxel:charcoal", "voxel:clay_bowl", "voxel:clay_pot"),
                    List.of(AlphaMilestoneKey.CAMPFIRE_LIT),
                    "player.journal.entries.recipe_campfire",
                    "Recipe history shows why the campfire matters beyond light.",
                    "Server emits recipe-history events from accepted craft/cook transactions."
            ),
            entry(
                    "voxel:journal_recipe_workbench",
                    JournalEntryKind.RECIPE_HISTORY,
                    12,
                    "Workbench Path",
                    "Tracks resin, bark, handles and the first station-gated assembly recipes.",
                    List.of("recipe.unlock.voxel:tool_handle", "recipe.unlock.voxel:workbench", "milestone.voxel:workbench_ready"),
                    List.of("voxel:tool_handle", "voxel:workbench", "voxel:resin", "voxel:bark_strip"),
                    List.of("voxel:workbench", "voxel:tree_stump"),
                    List.of("voxel:pine_forest"),
                    List.of("voxel:simple_house"),
                    List.of(),
                    List.of("voxel:tool_handle", "voxel:workbench", "voxel:forge"),
                    List.of(AlphaMilestoneKey.WORKBENCH_READY),
                    "player.journal.entries.recipe_workbench",
                    "Recipe history points to Pine Forest sources only after discovery.",
                    "Server emits recipe-history events from canonical recipe unlock state."
            ),
            entry(
                    "voxel:journal_recipe_cooking_pot",
                    JournalEntryKind.RECIPE_HISTORY,
                    13,
                    "Cooking Pot Meals",
                    "Tracks soups, stews, tea and jam as comfort-forward food progression.",
                    List.of("recipe.unlock.voxel:mushroom_stew", "recipe.unlock.voxel:hearty_stew", "milestone.voxel:cooking_pot_ready"),
                    List.of("voxel:mushroom_stew", "voxel:herb_soup", "voxel:berry_jam", "voxel:calming_tea", "voxel:hearty_stew"),
                    List.of("voxel:cooking_pot", "voxel:reeds", "voxel:clay_deposit"),
                    List.of("voxel:lakeside", "voxel:mushroom_grove"),
                    List.of(),
                    List.of(),
                    List.of("voxel:mushroom_stew", "voxel:herb_soup", "voxel:berry_jam", "voxel:calming_tea", "voxel:hearty_stew"),
                    List.of(AlphaMilestoneKey.COOKING_POT_READY),
                    "player.journal.entries.recipe_cooking_pot",
                    "Recipe history separates better meals from basic survival snacks.",
                    "Server emits recipe-history events from accepted cooking-pot transactions."
            ),
            entry(
                    "voxel:journal_recipe_forge",
                    JournalEntryKind.RECIPE_HISTORY,
                    14,
                    "Forge Work",
                    "Tracks ingots, stronger tools and ancient binding as a station history.",
                    List.of("recipe.unlock.voxel:forge", "recipe.unlock.voxel:iron_ingot", "milestone.voxel:forge_ready"),
                    List.of("voxel:forge", "voxel:copper_ingot", "voxel:iron_ingot", "voxel:iron_pickaxe", "voxel:ruin_seal"),
                    List.of("voxel:forge", "voxel:copper_ore", "voxel:iron_ore"),
                    List.of("voxel:highlands", "voxel:old_ruins"),
                    List.of("voxel:watchtower", "voxel:small_ruin"),
                    List.of(),
                    List.of("voxel:forge", "voxel:iron_ingot", "voxel:iron_pickaxe", "voxel:ruin_seal"),
                    List.of(AlphaMilestoneKey.FORGE_READY, AlphaMilestoneKey.FIRST_RARE_FIND),
                    "player.journal.entries.recipe_forge",
                    "Recipe history presents forge progress as restoration, not grind.",
                    "Server emits accepted forge transaction and rare-binding journal events."
            ),
            entry(
                    ANCIENT_FRAGMENT,
                    JournalEntryKind.LORE_PAGE,
                    15,
                    "Ancient Fragment",
                    "The first readable shard that links ruins, glow crystals and future sealed paths.",
                    List.of("pickup.item.voxel:ancient_fragment"),
                    List.of("voxel:ancient_fragment", "voxel:glow_crystal"),
                    List.of("voxel:mossy_stone", "voxel:glow_crystal_node"),
                    List.of("voxel:old_ruins", "voxel:mushroom_grove"),
                    List.of("voxel:small_ruin", "voxel:mushroom_circle"),
                    List.of(),
                    List.of("voxel:ancient_lantern", "voxel:ruin_key"),
                    List.of(AlphaMilestoneKey.FIRST_RUIN_DISCOVERED, AlphaMilestoneKey.FIRST_RARE_FIND),
                    "player.journal.entries.ancient_fragment",
                    "Rare-find feedback opens a lore page and updates ruin progress.",
                    "Server emits item pickup event from authoritative loot/inventory mutation."
            ),
            entry(
                    RUIN_KEY,
                    JournalEntryKind.LORE_PAGE,
                    16,
                    "Ruin Key",
                    "A restored key that should unlock future sealed ruin interactions once altar content exists.",
                    List.of("pickup.item.voxel:ruin_key", "recipe.unlock.voxel:ruin_key"),
                    List.of("voxel:ruin_key", "voxel:ancient_fragment", "voxel:glow_crystal"),
                    List.of("voxel:mossy_stone"),
                    List.of("voxel:old_ruins"),
                    List.of("voxel:small_ruin"),
                    List.of(),
                    List.of("voxel:ruin_key"),
                    List.of(AlphaMilestoneKey.FIRST_RARE_FIND),
                    "player.journal.entries.ruin_key",
                    "Journal marks the key as future-facing, not a required grind chain.",
                    "Server emits rare loot or accepted craft event once and persists it by player."
            ),
            entry(
                    RUIN_SEAL,
                    JournalEntryKind.LORE_PAGE,
                    17,
                    "Ruin Seal",
                    "A forge-bound seal that foreshadows ancient station acceptance/rejection rules.",
                    List.of("pickup.item.voxel:ruin_seal", "recipe.unlock.voxel:ruin_seal"),
                    List.of("voxel:ruin_seal", "voxel:iron_ingot", "voxel:glow_crystal"),
                    List.of("voxel:forge", "voxel:ancient_lantern"),
                    List.of("voxel:old_ruins", "voxel:highlands"),
                    List.of("voxel:small_ruin"),
                    List.of(),
                    List.of("voxel:ruin_seal"),
                    List.of(AlphaMilestoneKey.FORGE_READY, AlphaMilestoneKey.FIRST_RARE_FIND),
                    "player.journal.entries.ruin_seal",
                    "Journal links the seal to forge progress and future altar contracts.",
                    "Server emits accepted forge/loot event and persists entry idempotently."
            ),
            entry(
                    LOST_CHARM,
                    JournalEntryKind.LORE_PAGE,
                    18,
                    "Lost Charm",
                    "A personal rare find that keeps ruins cozy and mysterious instead of purely mechanical.",
                    List.of("pickup.item.voxel:lost_charm"),
                    List.of("voxel:lost_charm"),
                    List.of("voxel:storage_crate"),
                    List.of("voxel:old_ruins"),
                    List.of("voxel:small_ruin"),
                    List.of(),
                    List.of(),
                    List.of(AlphaMilestoneKey.FIRST_RARE_FIND),
                    "player.journal.entries.lost_charm",
                    "Journal gives a lore page and tiny rare-find badge.",
                    "Server emits loot pickup once per generated loot marker."
            ),
            entry(
                    ANCIENT_LANTERN,
                    JournalEntryKind.LORE_PAGE,
                    19,
                    "Ancient Lantern",
                    "A rare light source that ties comfort, ruin identity and future ancient restoration together.",
                    List.of("pickup.item.voxel:ancient_lantern", "recipe.unlock.voxel:ancient_lantern"),
                    List.of("voxel:ancient_lantern", "voxel:ancient_fragment", "voxel:glow_crystal"),
                    List.of("voxel:ancient_lantern"),
                    List.of("voxel:old_ruins", "voxel:mushroom_grove"),
                    List.of("voxel:small_ruin"),
                    List.of("voxel:firefly_swarm"),
                    List.of("voxel:ancient_lantern"),
                    List.of(AlphaMilestoneKey.FIRST_COMFORT, AlphaMilestoneKey.FIRST_RARE_FIND),
                    "player.journal.entries.ancient_lantern",
                    "Journal and comfort UI should both treat this as a rare cozy reward.",
                    "Server emits loot/craft event and later comfort systems consume the block/item contract."
            ),
            entry(
                    FIRST_RUIN_MAP,
                    JournalEntryKind.MAP_FRAGMENT,
                    20,
                    "First Ruin Sketch",
                    "A map fragment entry that can point back to discovered ruins without adding a physical item yet.",
                    List.of("discovery.map_fragment.voxel:first_ruin", "discovery.structure.voxel:small_ruin"),
                    List.of("voxel:ancient_fragment", "voxel:ruin_key"),
                    List.of("voxel:mossy_stone", "voxel:ancient_lantern"),
                    List.of("voxel:old_ruins"),
                    List.of("voxel:small_ruin"),
                    List.of(),
                    List.of(),
                    List.of(AlphaMilestoneKey.FIRST_RUIN_DISCOVERED),
                    "player.journal.entries.first_ruin_map",
                    "Map tab shows discovered marker only after server-confirmed structure discovery.",
                    "Server emits map fragment events from generated structure metadata, no client-only map reveal."
            ),
            entry(
                    SIMPLE_HOUSE,
                    JournalEntryKind.STRUCTURE_NOTE,
                    21,
                    "Simple House",
                    "A small furnished shelter that teaches home-building parts without turning the biome into a town.",
                    List.of("discovery.structure.voxel:simple_house"),
                    List.of("voxel:skyroot_planks", "voxel:resin", "voxel:bark_strip", "voxel:tool_handle"),
                    List.of("voxel:skyroot_planks", "voxel:small_table", "voxel:wooden_chair", "voxel:flower_pot", "voxel:woven_rug"),
                    List.of("voxel:cozy_meadow", "voxel:pine_forest", "voxel:skyroot_forest"),
                    List.of("voxel:simple_house"),
                    List.of(),
                    List.of("voxel:workbench", "voxel:small_table", "voxel:wooden_chair"),
                    List.of(AlphaMilestoneKey.WORKBENCH_READY, AlphaMilestoneKey.FIRST_COMFORT),
                    "player.journal.entries.simple_house",
                    "Journal note points from found shelter pieces to player-built comfort.",
                    "Server emits structure discovery from generated structure metadata and keeps comfort recipes milestone-backed."
            ),
            entry(
                    WATCHTOWER,
                    JournalEntryKind.STRUCTURE_NOTE,
                    22,
                    "Watchtower",
                    "A vertical landmark for highlands and old ruins that helps orientation before deeper ruin content exists.",
                    List.of("discovery.structure.voxel:watchtower"),
                    List.of("voxel:skyroot_log", "voxel:skyroot_planks", "voxel:glow_crystal"),
                    List.of("voxel:skyroot_log", "voxel:skyroot_planks", "voxel:torch", "voxel:glow_crystal_node"),
                    List.of("voxel:highlands", "voxel:old_ruins"),
                    List.of("voxel:watchtower"),
                    List.of("voxel:little_boar", "voxel:forest_bunny"),
                    List.of("voxel:forge"),
                    List.of(AlphaMilestoneKey.FORGE_READY, AlphaMilestoneKey.FIRST_RUIN_DISCOVERED),
                    "player.journal.entries.watchtower",
                    "Journal adds an orientation note and can later feed map markers.",
                    "Server emits discovery from structure bounds/marker authority, not from client-only camera checks."
            ),
            entry(
                    COMPACT_VILLAGE,
                    JournalEntryKind.STRUCTURE_NOTE,
                    23,
                    "Compact Village",
                    "A cozy settlement template that gives Worldgen a social landmark, storage loot anchors and future NPC spawn markers.",
                    List.of("discovery.structure.voxel:compact_village"),
                    List.of("voxel:berries", "voxel:wild_herbs", "voxel:cloth"),
                    List.of("voxel:mossy_path", "voxel:storage_crate", "voxel:herb_planter", "voxel:berry_bush", "voxel:water"),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields", "voxel:skyroot_forest"),
                    List.of("voxel:compact_village"),
                    List.of(),
                    List.of("voxel:storage_crate", "voxel:woven_rug", "voxel:garden_fence"),
                    List.of(AlphaMilestoneKey.STORAGE_READY, AlphaMilestoneKey.FIRST_COMFORT),
                    "player.journal.entries.compact_village",
                    "Journal shows a gentle settlement note and leaves NPC behavior to future server contracts.",
                    "Server emits discovery and loot-marker events idempotently per generated village instance."
            ),
            entry(
                    DESERT_WELL,
                    JournalEntryKind.STRUCTURE_NOTE,
                    24,
                    "Desert Well",
                    "A sun-dunes landmark that ties sand, water access and future desert encounters together.",
                    List.of("discovery.structure.voxel:desert_well"),
                    List.of("voxel:cactus", "voxel:water_container", "voxel:clay_lump"),
                    List.of("voxel:sand", "voxel:water", "voxel:stone", "voxel:cactus"),
                    List.of("voxel:sun_dunes"),
                    List.of("voxel:desert_well"),
                    List.of("voxel:dune_crawler"),
                    List.of("voxel:water_container"),
                    List.of(AlphaMilestoneKey.FIRST_SUPPLY, AlphaMilestoneKey.COOKING_POT_READY),
                    "player.journal.entries.desert_well",
                    "Journal frames the well as a landmark and resource clue, not a survival timer.",
                    "Server emits structure discovery from generated structure metadata and encounter hooks stay authoritative."
            )
    );

    private static final List<GoalDefinition> DEFAULT_GOALS = List.of(
            goal(
                    "voxel:goal_find_camp",
                    1,
                    "Find a safe rhythm",
                    "Secure spawn, gather first supplies and learn the nearest friendly biome.",
                    List.of(AlphaMilestoneKey.SPAWN_SECURED, AlphaMilestoneKey.FIRST_SUPPLY, AlphaMilestoneKey.FIRST_FOOD),
                    List.of(SPAWN_CAMPSITE, COZY_MEADOW),
                    "player.goals.find_camp",
                    "Short objective nudge may appear in Journal/HUD, then collapse once complete.",
                    "Server derives goal completion from milestone state only."
            ),
            goal(
                    "voxel:goal_tools_and_fire",
                    2,
                    "Tools and fire",
                    "Reveal first recipes, craft a tool, place and light the first campfire.",
                    List.of(AlphaMilestoneKey.FIRST_RECIPE, AlphaMilestoneKey.FIRST_TOOL, AlphaMilestoneKey.CAMPFIRE_CRAFTED, AlphaMilestoneKey.CAMPFIRE_LIT),
                    List.of("voxel:journal_recipe_campfire"),
                    "player.goals.tools_and_fire",
                    "Journal groups starter crafting without MMO quest copy.",
                    "Server derives goal completion from accepted craft/campfire state."
            ),
            goal(
                    "voxel:goal_small_home",
                    3,
                    "Make it feel like home",
                    "Add storage, a workbench and first comfort so the base becomes useful and cozy.",
                    List.of(AlphaMilestoneKey.STORAGE_READY, AlphaMilestoneKey.WORKBENCH_READY, AlphaMilestoneKey.FIRST_COMFORT),
                    List.of(PINE_FOREST, "voxel:journal_recipe_workbench"),
                    "player.goals.small_home",
                    "Journal/comfort UI names the next useful source, not a checklist grind.",
                    "Server derives goal completion from station/storage/comfort milestones."
            ),
            goal(
                    "voxel:goal_better_food_and_metal",
                    4,
                    "Cook, then forge",
                    "Reach lakeside clay, make better meals and bridge into metal progression.",
                    List.of(AlphaMilestoneKey.COOKING_POT_READY, AlphaMilestoneKey.FORGE_READY),
                    List.of(LAKESIDE, "voxel:journal_recipe_cooking_pot", "voxel:journal_recipe_forge"),
                    "player.goals.better_food_and_metal",
                    "Journal groups food and metal as preparation for exploration.",
                    "Server derives completion from station milestones and accepted transactions."
            ),
            goal(
                    "voxel:goal_first_ruin",
                    5,
                    "Read the first ruin",
                    "Discover a ruin and recover one rare ancient find.",
                    List.of(AlphaMilestoneKey.FIRST_RUIN_DISCOVERED, AlphaMilestoneKey.FIRST_RARE_FIND),
                    List.of(OLD_RUINS, SMALL_RUIN, ANCIENT_FRAGMENT, FIRST_RUIN_MAP),
                    "player.goals.first_ruin",
                    "Journal opens ruin progress and lore pages only from confirmed discoveries.",
                    "Server derives completion from structure discovery and rare loot journal events."
            )
    );

    private static final Map<String, JournalEntryDefinition> ENTRIES_BY_KEY = entriesByKey(DEFAULT_ENTRIES);
    private static final Map<JournalEntryKind, List<JournalEntryDefinition>> ENTRIES_BY_KIND = entriesByKind(DEFAULT_ENTRIES);
    private static final Map<String, GoalDefinition> GOALS_BY_KEY = goalsByKey(DEFAULT_GOALS);

    private JournalProgression() {
    }

    public static List<JournalEntryDefinition> defaultEntries() {
        return DEFAULT_ENTRIES;
    }

    public static List<GoalDefinition> defaultGoals() {
        return DEFAULT_GOALS;
    }

    public static Optional<JournalEntryDefinition> findEntry(String key) {
        return Optional.ofNullable(ENTRIES_BY_KEY.get(key));
    }

    public static Optional<GoalDefinition> findGoal(String key) {
        return Optional.ofNullable(GOALS_BY_KEY.get(key));
    }

    public static List<JournalEntryDefinition> entriesOfKind(JournalEntryKind kind) {
        return ENTRIES_BY_KIND.getOrDefault(kind, List.of());
    }

    public static Set<String> ruinProgressionItemKeys() {
        return Set.of(
                "voxel:ancient_fragment",
                "voxel:ruin_key",
                "voxel:ruin_seal",
                "voxel:lost_charm",
                "voxel:ancient_lantern"
        );
    }

    private static JournalEntryDefinition entry(
            String key,
            JournalEntryKind kind,
            int order,
            String title,
            String summary,
            List<String> discoveryEventKeys,
            List<String> relatedItemKeys,
            List<String> relatedBlockKeys,
            List<String> relatedBiomeKeys,
            List<String> relatedStructureKeys,
            List<String> relatedEntityKeys,
            List<String> relatedRecipeKeys,
            List<AlphaMilestoneKey> milestoneKeys,
            String persistenceKey,
            String uiFeedback,
            String serverEventContract
    ) {
        return new JournalEntryDefinition(
                key,
                kind,
                order,
                title,
                summary,
                discoveryEventKeys,
                relatedItemKeys,
                relatedBlockKeys,
                relatedBiomeKeys,
                relatedStructureKeys,
                relatedEntityKeys,
                relatedRecipeKeys,
                milestoneKeys,
                persistenceKey,
                uiFeedback,
                serverEventContract
        );
    }

    private static GoalDefinition goal(
            String key,
            int order,
            String title,
            String summary,
            List<AlphaMilestoneKey> milestoneKeys,
            List<String> journalEntryKeys,
            String persistenceKey,
            String uiFeedback,
            String serverEventContract
    ) {
        return new GoalDefinition(
                key,
                order,
                title,
                summary,
                milestoneKeys,
                journalEntryKeys,
                persistenceKey,
                uiFeedback,
                serverEventContract
        );
    }

    private static Map<String, JournalEntryDefinition> entriesByKey(List<JournalEntryDefinition> entries) {
        Map<String, JournalEntryDefinition> byKey = new LinkedHashMap<>();
        for (JournalEntryDefinition entry : entries) {
            JournalEntryDefinition duplicate = byKey.put(entry.key(), entry);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate journal entry key " + entry.key());
            }
        }
        return Collections.unmodifiableMap(byKey);
    }

    private static Map<JournalEntryKind, List<JournalEntryDefinition>> entriesByKind(List<JournalEntryDefinition> entries) {
        Map<JournalEntryKind, List<JournalEntryDefinition>> byKind = new EnumMap<>(JournalEntryKind.class);
        for (JournalEntryKind kind : JournalEntryKind.values()) {
            byKind.put(kind, entries.stream()
                    .filter(entry -> entry.kind() == kind)
                    .toList());
        }
        return Collections.unmodifiableMap(byKind);
    }

    private static Map<String, GoalDefinition> goalsByKey(List<GoalDefinition> goals) {
        Map<String, GoalDefinition> byKey = new LinkedHashMap<>();
        for (GoalDefinition goal : goals) {
            GoalDefinition duplicate = byKey.put(goal.key(), goal);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate goal key " + goal.key());
            }
        }
        return Collections.unmodifiableMap(byKey);
    }

    public static Set<String> referencedJournalEntryKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (GoalDefinition goal : DEFAULT_GOALS) {
            keys.addAll(goal.journalEntryKeys());
        }
        return Collections.unmodifiableSet(keys);
    }
}
