package dev.voxelgame.common.content;

import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.registry.Registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AlphaItemDesigns {
    private AlphaItemDesigns() {
    }

    public static Map<String, ItemDesign> createDefault() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        return createDefault(items, ContentTagRegistry.createDefault());
    }

    public static Map<String, ItemDesign> createDefault(Registry<ItemType> items, ContentTagRegistry tags) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(tags, "tags");
        Map<String, ItemDesign> designs = new LinkedHashMap<>();

        add(designs, row(items, tags, "voxel:twig", "starter supply", biomes("voxel:cozy_meadow", "voxel:flower_fields"), List.of(), structures("voxel:campsite"), "first crafting and fuel", "visible at spawn", "pickup toast and recipe unlock seed", "authoritative pickup/inventory stack", "voxel:twig", List.of()));
        add(designs, row(items, tags, "voxel:pebble", "starter supply", biomes("voxel:cozy_meadow", "voxel:flower_fields"), List.of(), structures("voxel:campsite"), "stone tools", "visible at spawn", "pickup toast and tool recipe affordance", "authoritative pickup/inventory stack", "voxel:pebble", List.of()));
        add(designs, row(items, tags, "voxel:fiber", "starter binding", biomes("voxel:cozy_meadow", "voxel:flower_fields"), List.of(), structures("voxel:campsite"), "rope, tools and storage", "harvest wild grass", "pickup toast and missing ingredient tooltip", "authoritative harvest/drop/inventory stack", "voxel:fiber", List.of()));
        add(designs, row(items, tags, "voxel:berries", "early food", biomes("voxel:cozy_meadow", "voxel:flower_fields"), List.of(), structures("voxel:campsite"), "weak food, jam and comfort decor dye", "harvest berry bush", "hunger feedback and recipe unlock source", "server eat action and inventory stack", "voxel:berries", loot("voxel:campsite_crate")));
        add(designs, row(items, tags, "voxel:wild_herbs", "early utility food", biomes("voxel:cozy_meadow", "voxel:flower_fields", "voxel:lakeside"), List.of(), structures("voxel:campsite"), "healing snack, soup and tea", "harvest herb planter/sun bloom", "heal/food tooltip plus recipe source", "server eat action and inventory stack", "voxel:wild_herbs", loot("voxel:campsite_crate")));
        add(designs, row(items, tags, "voxel:mushroom", "early alternate food", biomes("voxel:pine_forest", "voxel:mushroom_grove"), List.of(), structures("voxel:mushroom_circle"), "roasted mushroom and stew", "find in forest/grove", "food tooltip and cooking recipe source", "server eat/cook action and inventory stack", "voxel:mushroom", List.of()));
        add(designs, row(items, tags, "voxel:stick", "crafted handle", biomes("voxel:cozy_meadow", "voxel:pine_forest"), List.of(), structures("voxel:campsite"), "torches, tools and decor", "craft from planks or loot", "ingredient tooltip", "craft transaction output", "voxel:stick", loot("voxel:campsite_crate")));
        add(designs, row(items, tags, "voxel:stone", "basic building material", biomes("voxel:highlands", "voxel:old_ruins"), List.of(), List.of(), "campfire, forge and stone tools", "mine stone", "mining/tool hint", "block drop and inventory stack", "voxel:stone", List.of()));
        add(designs, row(items, tags, "voxel:dry_grass", "quick fuel", biomes("voxel:cozy_meadow", "voxel:flower_fields"), List.of(), structures("voxel:campsite"), "firestarter and sleeping mat", "harvest grass", "fuel tooltip", "authoritative harvest and fuel consume", "voxel:dry_grass", List.of()));
        add(designs, row(items, tags, "voxel:charcoal", "reliable fuel", biomes("voxel:cozy_meadow", "voxel:pine_forest"), List.of(), structures("voxel:campsite"), "smelting and forge recipes", "char logs at campfire", "fuel duration tooltip", "campfire cook output and fuel consume", "voxel:charcoal", List.of()));

        add(designs, row(items, tags, "voxel:stone_knife", "first precision tool", biomes("voxel:cozy_meadow"), List.of(), structures("voxel:campsite"), "harvest plants and early resources", "starter recipe", "first tool toast and effective-against tooltip", "server craft and durability state", "voxel:stone_knife", List.of()));
        add(designs, row(items, tags, "voxel:stone_axe", "first wood tool", biomes("voxel:cozy_meadow", "voxel:pine_forest"), List.of(), structures("voxel:campsite"), "wood, stumps and station materials", "starter recipe", "first tool toast and wood hint", "server craft and durability state", "voxel:stone_axe", List.of()));
        add(designs, row(items, tags, "voxel:stone_pickaxe", "first mining tool", biomes("voxel:cozy_meadow", "voxel:highlands"), List.of(), List.of(), "stone, copper and first ruin access", "starter recipe", "tool tier tooltip", "server craft and durability state", "voxel:stone_pickaxe", List.of()));
        add(designs, row(items, tags, "voxel:campfire", "first safety station", biomes("voxel:cozy_meadow"), List.of(), structures("voxel:campsite"), "fuel, light, comfort and early cooking", "starter recipe", "place/fuel milestone feedback", "server place/fuel/block entity state", "voxel:campfire", List.of()));
        add(designs, row(items, tags, "voxel:storage_crate", "first persistence object", biomes("voxel:cozy_meadow", "voxel:pine_forest"), List.of(), structures("voxel:campsite", "voxel:small_ruin"), "base storage and loot containers", "basic building recipe", "interaction HUD and inventory-full feedback", "revisioned block entity storage", "voxel:storage_crate", List.of()));
        add(designs, row(items, tags, "voxel:sleeping_mat", "early comfort decor", biomes("voxel:cozy_meadow"), List.of(), structures("voxel:campsite"), "comfort and future sleep quality", "dry grass plus fiber", "comfort source tooltip", "placed block plus future sleep save state", "voxel:sleeping_mat", List.of()));
        add(designs, row(items, tags, "voxel:lantern", "early comfort light", biomes("voxel:cozy_meadow", "voxel:pine_forest"), List.of(), structures("voxel:campsite"), "base light and comfort", "torch plus copper/pebble recipe", "light/comfort tooltip", "server craft/place and light block state", "voxel:lantern", List.of()));
        add(designs, row(items, tags, "voxel:woven_rug", "early cozy decor", biomes("voxel:cozy_meadow"), List.of(), structures("voxel:campsite", "voxel:simple_house"), "comfort and base identity", "fiber plus berry recipe", "comfort source tooltip", "server craft/place state", "voxel:woven_rug", List.of()));

        add(designs, row(items, tags, "voxel:skyroot_planks", "wood processing", biomes("voxel:cozy_meadow", "voxel:pine_forest"), List.of(), structures("voxel:campsite"), "storage, workbench and decor", "craft from logs", "ingredient tooltip", "craft transaction output", "voxel:skyroot_planks", loot("voxel:campsite_crate")));
        add(designs, row(items, tags, "voxel:bark_strip", "pine progression material", biomes("voxel:pine_forest"), List.of(), List.of(), "tool handles, resin torches and leather strips", "harvest tree stump", "biome reason tooltip", "authoritative harvest/drop", "voxel:bark_strip", List.of()));
        add(designs, row(items, tags, "voxel:resin", "pine progression binder", biomes("voxel:pine_forest"), List.of(), List.of(), "workbench, handles and copper tools", "harvest pine resources", "biome reason tooltip", "authoritative harvest/drop", "voxel:resin", List.of()));
        add(designs, row(items, tags, "voxel:tool_handle", "midgame tool component", biomes("voxel:pine_forest"), List.of(), List.of(), "workbench and metal tools", "craft from sticks and bark", "missing-component tooltip", "craft transaction output", "voxel:tool_handle", List.of()));
        add(designs, row(items, tags, "voxel:workbench", "progression station", biomes("voxel:pine_forest"), List.of(), structures("voxel:campsite", "voxel:simple_house"), "station-gated tools, forge and ancient recipes", "pine materials recipe", "missing-station crafting feedback", "server station radius validation", "voxel:workbench", List.of()));

        add(designs, row(items, tags, "voxel:clay_lump", "lakeside pottery material", biomes("voxel:lakeside", "voxel:mire"), List.of(), List.of(), "bowls, pots and forge path", "harvest clay deposit", "biome reason tooltip", "authoritative harvest/drop", "voxel:clay_lump", List.of()));
        add(designs, row(items, tags, "voxel:clay_bowl", "food container", biomes("voxel:lakeside"), List.of(), structures("voxel:campsite"), "soups, stews and jam", "fire clay at campfire", "container requirement tooltip", "campfire recipe output", "voxel:clay_bowl", List.of()));
        add(designs, row(items, tags, "voxel:clay_pot", "pottery step", biomes("voxel:lakeside"), List.of(), structures("voxel:campsite"), "cooking pot, flower pot and forge path", "fire clay at campfire", "station recipe tooltip", "campfire recipe output", "voxel:clay_pot", List.of()));
        add(designs, row(items, tags, "voxel:water_container", "cooking liquid container", biomes("voxel:lakeside"), List.of(), structures("voxel:campsite"), "tea, soup and stew recipes", "clay pot plus reeds", "water/container missing feedback", "server cooking ingredient consume", "voxel:water_container", List.of()));
        add(designs, row(items, tags, "voxel:cooked_berries", "first cooked food", biomes("voxel:cozy_meadow"), List.of(), structures("voxel:campsite"), "better early food", "campfire recipe", "cooking complete feedback", "server cook transaction output", "voxel:cooked_berries", List.of()));
        add(designs, row(items, tags, "voxel:roasted_mushroom", "early cooked alternative", biomes("voxel:pine_forest", "voxel:mushroom_grove"), List.of(), List.of(), "better early food and hearty stew ingredient", "campfire recipe", "cooking complete feedback", "server cook transaction output", "voxel:roasted_mushroom", List.of()));
        add(designs, row(items, tags, "voxel:cooking_pot", "food progression station", biomes("voxel:lakeside"), List.of(), structures("voxel:campsite"), "soups, stews, tea and jam", "clay/copper recipe", "station-ready HUD card", "server station block and cooking validation", "voxel:cooking_pot", List.of()));

        add(designs, row(items, tags, "voxel:raw_copper", "midgame ore", biomes("voxel:highlands", "voxel:old_ruins"), List.of(), List.of(), "copper ingots, pot and tools", "mine copper ore", "ore/tool-level tooltip", "server block drop and smelt input", "voxel:raw_copper", loot("voxel:village_house_crate")));
        add(designs, row(items, tags, "voxel:copper_ingot", "midgame metal", biomes("voxel:highlands"), List.of(), List.of(), "cooking pot, forge and copper tools", "smelt raw copper", "smelting complete feedback", "server cook/smelt transaction output", "voxel:copper_ingot", List.of()));
        add(designs, row(items, tags, "voxel:raw_iron", "late midgame ore", biomes("voxel:highlands", "voxel:old_ruins"), List.of(), List.of(), "iron ingots and ruin progression", "mine iron ore", "tool-level tooltip", "server block drop and smelt input", "voxel:raw_iron", loot("voxel:ruin_rare_crate")));
        add(designs, row(items, tags, "voxel:iron_ingot", "late metal", biomes("voxel:highlands", "voxel:old_ruins"), List.of(), structures("voxel:small_ruin"), "iron tools and ruin key", "forge smelting", "forge output feedback", "server forge transaction output", "voxel:iron_ingot", loot("voxel:ruin_rare_crate")));
        add(designs, row(items, tags, "voxel:iron_pickaxe", "late mining gate", biomes("voxel:highlands"), List.of(), List.of(), "glow crystal mining and ruin progression", "workbench iron tool recipe", "tool tier tooltip", "server craft and durability state", "voxel:iron_pickaxe", List.of()));
        add(designs, row(items, tags, "voxel:forge", "metal progression station", biomes("voxel:highlands"), List.of(), structures("voxel:watchtower"), "iron smelting and ancient binding", "workbench recipe", "heat/fuel station feedback", "server station block and forge validation", "voxel:forge", List.of()));

        add(designs, row(items, tags, "voxel:glow_crystal", "rare glow resource", biomes("voxel:mushroom_grove", "voxel:old_ruins", "voxel:frost_peaks"), List.of(), structures("voxel:mushroom_circle", "voxel:small_ruin"), "ancient lantern, ruin key and seal", "mine crystal node or loot ruins", "rare pickup and journal hint", "server block drop/loot transaction", "voxel:glow_crystal", loot("voxel:ruin_crate", "voxel:ruin_rare_crate")));
        add(designs, row(items, tags, "voxel:ancient_fragment", "ruin progression shard", biomes("voxel:old_ruins"), List.of(), structures("voxel:small_ruin"), "ancient lantern, ruin key and seal", "loot ruins", "lore pickup feedback", "idempotent generated loot state", "voxel:ancient_fragment", loot("voxel:ruin_crate", "voxel:ruin_rare_crate")));
        add(designs, row(items, tags, "voxel:ruin_key", "ruin gate item", biomes("voxel:old_ruins"), List.of(), structures("voxel:small_ruin"), "future sealed ruin access and seal recipe", "craft at workbench or rare loot", "journal progression feedback", "server recipe/loot state", "voxel:ruin_key", loot("voxel:ruin_crate", "voxel:ruin_rare_crate")));
        add(designs, row(items, tags, "voxel:ruin_seal", "ancient binding item", biomes("voxel:old_ruins"), List.of(), structures("voxel:small_ruin"), "crystal tools and future ancient station unlocks", "forge binding recipe or rare loot", "rare progression toast", "server forge/loot state", "voxel:ruin_seal", loot("voxel:ruin_rare_crate")));
        add(designs, row(items, tags, "voxel:ancient_lantern", "rare comfort light", biomes("voxel:old_ruins"), List.of(), structures("voxel:small_ruin"), "strong light, comfort and ruin identity", "restore at workbench or rare loot", "rare find feedback and comfort tooltip", "placeable block plus generated loot state", "voxel:ancient_lantern", loot("voxel:ruin_crate")));
        add(designs, row(items, tags, "voxel:lost_charm", "collectible rare", biomes("voxel:old_ruins"), List.of(), structures("voxel:small_ruin"), "journal collectible and future lore turn-in", "rare ruin loot", "collectible journal feedback", "idempotent generated loot state", "voxel:lost_charm", loot("voxel:ruin_rare_crate")));

        return Collections.unmodifiableMap(designs);
    }

    private static void add(Map<String, ItemDesign> designs, ItemDesign design) {
        ItemDesign duplicate = designs.put(design.itemKey(), design);
        if (duplicate != null) {
            throw new IllegalStateException("Duplicate item design " + design.itemKey());
        }
    }

    private static ItemDesign row(
            Registry<ItemType> items,
            ContentTagRegistry tags,
            String itemKey,
            String role,
            List<String> biomeSources,
            List<String> entitySources,
            List<String> structureSources,
            String primaryUse,
            String unlock,
            String uiFeedback,
            String saveNetworkContract,
            String uiIconKey,
            List<String> lootTableKeys
    ) {
        ItemType item = items.requireByKey(itemKey);
        Set<ContentTag> itemTags = tags.tagsFor(ContentKind.ITEM, item.key());
        return new ItemDesign(
                item.key(),
                role,
                biomeSources,
                entitySources,
                structureSources,
                primaryUse,
                unlock,
                uiFeedback,
                saveNetworkContract,
                uiIconKey,
                lootTableKeys,
                item.maxStackSize(),
                item.foodValue(),
                item.healValue(),
                item.toolLevel(),
                item.durability(),
                itemTags
        );
    }

    private static List<String> biomes(String... keys) {
        return List.of(keys);
    }

    private static List<String> structures(String... keys) {
        return List.of(keys);
    }

    private static List<String> loot(String... keys) {
        return List.of(keys);
    }
}
