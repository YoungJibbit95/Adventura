package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.item.CraftingStationType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StationProgression {
    public static final String INVENTORY = "voxel:station_inventory";
    public static final String CAMPFIRE = "voxel:station_campfire";
    public static final String WORKBENCH = "voxel:station_workbench";
    public static final String COOKING_POT = "voxel:station_cooking_pot";
    public static final String FORGE = "voxel:station_forge";
    public static final String ANCIENT_ALTAR = "voxel:station_ancient_altar";

    private static final List<StationDefinition> DEFAULT_CHAIN = List.of(
            station(
                    INVENTORY,
                    1,
                    "Inventory",
                    CraftingStationType.INVENTORY.name(),
                    true,
                    "",
                    "",
                    List.of(),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields"),
                    List.of("voxel:stone_knife", "voxel:stone_pickaxe", "voxel:stone_axe", "voxel:campfire", "voxel:storage_crate"),
                    "Starter handcrafting for first tools, campfire, storage and simple decor.",
                    "No block entity. Uses player inventory only.",
                    "Inventory/Crafting tab with starter recipe filters and short missing-ingredient feedback.",
                    "Recipe unlock/milestone state belongs to PlayerSave; no station save payload.",
                    "CraftRequest without station; server validates recipe, ingredients, count and inventory capacity.",
                    "First recipe and first tool feedback stay short and contextual.",
                    "Spawn resources and starter campsite teach the first recipes."
            ),
            station(
                    CAMPFIRE,
                    2,
                    "Campfire",
                    CraftingStationType.CAMPFIRE.name(),
                    true,
                    "voxel:campfire",
                    "voxel:campfire_active",
                    List.of("voxel:campfire", "voxel:twig", "voxel:stick", "voxel:dry_grass", "voxel:charcoal"),
                    List.of("voxel:cozy_meadow", "voxel:flower_fields"),
                    List.of("voxel:cooked_berries", "voxel:roasted_mushroom", "voxel:charcoal", "voxel:clay_bowl", "voxel:clay_pot", "voxel:copper_ingot"),
                    "Safety, warmth, fuel, early cooked food, pottery firing and copper smelting.",
                    "BlockEntity type `voxel:campfire`; public state is active/fuel/cook progress.",
                    "Campfire panel with input, fuel, output, burn progress and cook progress.",
                    "World save keeps campfire block entity plus fuel/cook state when expanded beyond current status packets.",
                    "CookRequest at campfire plus fuel intent; server owns fuel consume, timing and output.",
                    "Fuel missing, cooking complete, campfire lit and comfort-source feedback.",
                    "Starter campsite exposes the first campfire; fuel comes from starter and Pine resources."
            ),
            station(
                    WORKBENCH,
                    3,
                    "Workbench",
                    CraftingStationType.WORKBENCH.name(),
                    true,
                    "voxel:workbench",
                    "voxel:workbench",
                    List.of("voxel:skyroot_planks", "voxel:tool_handle", "voxel:resin", "voxel:bark_strip"),
                    List.of("voxel:pine_forest"),
                    List.of("voxel:forge", "voxel:iron_axe", "voxel:iron_pickaxe", "voxel:cloth", "voxel:leather_strip", "voxel:ancient_lantern", "voxel:ruin_key"),
                    "Midgame assembly station for stronger tools, forge setup, textiles and ancient restoration.",
                    "BlockEntity type `voxel:workbench`; current state can stay stateless until recipe queues exist.",
                    "Workbench crafting filter with station-present badge and component source hints.",
                    "Station placement persists as block entity; recipe history belongs to PlayerSave.",
                    "CraftRequest at workbench; server validates station radius, recipe, ingredients and output slots.",
                    "Workbench recipes available, missing component/source and station range feedback.",
                    "Pine Forest resin/bark path unlocks the station; simple houses can foreshadow it."
            ),
            station(
                    COOKING_POT,
                    4,
                    "Cooking Pot",
                    CraftingStationType.COOKING_POT.name(),
                    true,
                    "voxel:cooking_pot",
                    "voxel:cooking_pot",
                    List.of("voxel:clay_pot", "voxel:clay_bowl", "voxel:water_container", "voxel:copper_ingot", "voxel:reed_bundle"),
                    List.of("voxel:lakeside", "voxel:mushroom_grove"),
                    List.of("voxel:mushroom_stew", "voxel:herb_soup", "voxel:berry_jam", "voxel:calming_tea", "voxel:hearty_stew", "voxel:glow_mushroom_stew", "voxel:spore_tea", "voxel:honey"),
                    "Food progression station for soups, stews, tea, jam and comfort/night utility foods.",
                    "BlockEntity type `voxel:cooking_pot`; needs input/fuel/output/cook state before multiplayer UI is final.",
                    "Dedicated Cooking Pot screen with water/container requirements and recipe options.",
                    "Save should persist active recipe, slots, fuel/heat if station inventory becomes real.",
                    "CookRequest at cooking pot; server validates station, input slots, containers, timing and output.",
                    "Needs water/container, no matching recipe, output ready and inventory full feedback.",
                    "Lakeside clay/reeds and Mushroom Grove ingredients make the station worth seeking."
            ),
            station(
                    FORGE,
                    5,
                    "Forge",
                    CraftingStationType.FORGE.name(),
                    true,
                    "voxel:forge",
                    "voxel:forge",
                    List.of("voxel:stone", "voxel:clay_pot", "voxel:copper_ingot", "voxel:charcoal"),
                    List.of("voxel:highlands", "voxel:old_ruins"),
                    List.of("voxel:iron_ingot", "voxel:ruin_seal", "voxel:crystal_axe", "voxel:crystal_pickaxe", "voxel:crystal_knife"),
                    "Metal and ancient-binding station for iron progression, ruin seals and crystal tools.",
                    "BlockEntity type `voxel:forge`; needs heat/fuel/input/output state before final online UX.",
                    "Forge screen with ore input, fuel, output, heat and blocked-output states.",
                    "Save should persist active smelt/bind recipe, heat/fuel and output revision.",
                    "CookRequest/CraftRequest at forge; server validates heat/fuel, station, ingredients and no-dupe output.",
                    "Insufficient heat/fuel, smelt complete, blocked output and rare binding feedback.",
                    "Highlands ore path and ruin loot make the forge the late-midgame bridge."
            ),
            station(
                    ANCIENT_ALTAR,
                    6,
                    "Ancient Altar",
                    "FUTURE_ANCIENT_ALTAR",
                    false,
                    "",
                    "",
                    List.of("voxel:ruin_key", "voxel:ruin_seal", "voxel:ancient_fragment", "voxel:glow_crystal"),
                    List.of("voxel:old_ruins", "voxel:frost_peaks"),
                    List.of(),
                    "Future late-game station for sealed ruins, map fragments and non-grindy ancient upgrades.",
                    "Future BlockEntity type should expose sealed-state, offerings, discovered lore and revision.",
                    "Future Ancient Station screen should read like discovery/restoration, not MMO quest turn-in.",
                    "Future save state must be one-shot/idempotent per generated structure.",
                    "Future server transaction must consume offerings once and emit journal/lore events.",
                    "Rare discovery, seal accepted/rejected and lore unlocked feedback.",
                    "Rare ruins/frozen shrines should introduce it after first ruin progression is stable."
            )
    );

    private static final Map<String, StationDefinition> BY_KEY = byKey(DEFAULT_CHAIN);
    private static final Map<CraftingStationType, StationDefinition> BY_STATION_TYPE = byStationType(DEFAULT_CHAIN);

    private StationProgression() {
    }

    public static List<StationDefinition> defaultChain() {
        return DEFAULT_CHAIN;
    }

    public static List<StationDefinition> implementedStations() {
        return DEFAULT_CHAIN.stream()
                .filter(StationDefinition::implemented)
                .toList();
    }

    public static Optional<StationDefinition> find(String key) {
        return Optional.ofNullable(BY_KEY.get(key));
    }

    public static Optional<StationDefinition> forStationType(CraftingStationType stationType) {
        return Optional.ofNullable(BY_STATION_TYPE.get(stationType));
    }

    private static StationDefinition station(
            String key,
            int order,
            String title,
            String craftingStationTypeKey,
            boolean implemented,
            String placeableItemKey,
            String blockKey,
            List<String> unlockItemKeys,
            List<String> unlockBiomeKeys,
            List<String> primaryRecipeKeys,
            String craftingRole,
            String blockEntityStateContract,
            String uiScreenContract,
            String saveStateContract,
            String serverTransactionContract,
            String feedbackContract,
            String lootUnlockSource
    ) {
        return new StationDefinition(
                key,
                order,
                title,
                craftingStationTypeKey,
                implemented,
                placeableItemKey,
                blockKey,
                unlockItemKeys,
                unlockBiomeKeys,
                primaryRecipeKeys,
                craftingRole,
                blockEntityStateContract,
                uiScreenContract,
                saveStateContract,
                serverTransactionContract,
                feedbackContract,
                lootUnlockSource
        );
    }

    private static Map<String, StationDefinition> byKey(List<StationDefinition> stations) {
        Map<String, StationDefinition> byKey = new LinkedHashMap<>();
        for (StationDefinition station : stations) {
            StationDefinition duplicate = byKey.put(station.key(), station);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate station key " + station.key());
            }
        }
        return Collections.unmodifiableMap(byKey);
    }

    private static Map<CraftingStationType, StationDefinition> byStationType(List<StationDefinition> stations) {
        Map<CraftingStationType, StationDefinition> byType = new EnumMap<>(CraftingStationType.class);
        for (StationDefinition station : stations) {
            station.craftingStationType().ifPresent(type -> {
                StationDefinition duplicate = byType.put(type, station);
                if (duplicate != null) {
                    throw new IllegalStateException("Duplicate station type " + type);
                }
            });
        }
        return Collections.unmodifiableMap(byType);
    }
}
