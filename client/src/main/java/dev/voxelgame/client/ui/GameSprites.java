package dev.voxelgame.client.ui;

import dev.voxelgame.common.item.Items;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class GameSprites implements AutoCloseable {
    public static final String ITEM_TEXTURE_ROOT = "assets/game/textures/item/";
    public static final String ITEMS_TEXTURE_ROOT = "assets/game/textures/items/";
    public static final String BLOCK_TEXTURE_ROOT = "assets/game/textures/block/";
    public static final String BLOCKS_TEXTURE_ROOT = "assets/game/textures/blocks/";
    public static final String ASSET_BLOCKS_TEXTURE_ROOT = "assets/game/blocks/";
    public static final String ASSET_MINERALS_TEXTURE_ROOT = "assets/game/minerals/";
    public static final String ASSET_DROP_TEXTURE_ROOT = "assets/game/";
    public static final int INDIVIDUAL_ITEM_MAX_TEXTURE_SIZE = 128;
    private static final List<String> OPTIONAL_ITEM_KEYS = List.of(
            "voxel:feathers",
            "voxel:stick",
            "voxel:snow",
            "voxel:sleeping_mat",
            "voxel:tree_stump"
    );

    private final Map<String, UiSprite> itemSprites = new HashMap<>();
    private final Map<String, UiSprite> hudSprites = new HashMap<>();
    private final Set<UiSpriteSheet> sheets = new LinkedHashSet<>();

    private GameSprites() {
    }

    public static GameSprites loadDefault() {
        GameSprites sprites = new GameSprites();
        UiSpriteSheet ui = sprites.sheet("assets/game/ui_hud_sheet.png", UiSpriteSheet.BackgroundMode.EDGE_CHECKER);
        UiSpriteSheet blocks = sprites.sheet("assets/game/blocks_tiles_sheet.png", UiSpriteSheet.BackgroundMode.EDGE_CHECKER);
        UiSpriteSheet tools = sprites.sheet("assets/game/tools_weapons_sheet.png", UiSpriteSheet.BackgroundMode.EDGE_CHECKER);
        UiSpriteSheet food = sprites.sheet("assets/game/nature_food_sheet.png", UiSpriteSheet.BackgroundMode.OPAQUE);
        UiSpriteSheet ores = sprites.optionalSheet("assets/game/ores_materials_sheet.png", UiSpriteSheet.BackgroundMode.EDGE_CHECKER)
                .orElse(blocks);

        sprites.hud("heart_full", ui.sprite(31, 37, 68, 58));
        sprites.hud("heart_half", ui.sprite(129, 37, 68, 58));
        sprites.hud("heart_empty", ui.sprite(226, 39, 68, 56));
        sprites.hud("hunger_full", ui.sprite(412, 35, 67, 76));
        sprites.hud("hunger_half", ui.sprite(511, 35, 67, 76));
        sprites.hud("hunger_empty", ui.sprite(611, 36, 70, 74));
        sprites.hud("leaf_full", ui.sprite(712, 35, 67, 75));
        sprites.hud("leaf_half", ui.sprite(810, 35, 67, 75));
        sprites.hud("leaf_empty", ui.sprite(908, 35, 67, 75));
        sprites.hud("armor_full", ui.sprite(40, 138, 70, 66));
        sprites.hud("armor_half", ui.sprite(135, 138, 70, 66));
        sprites.hud("armor_empty", ui.sprite(230, 138, 70, 66));
        sprites.hud("air_full", ui.sprite(1097, 35, 72, 72));
        sprites.hud("air_half", ui.sprite(1200, 36, 72, 72));
        sprites.hud("air_empty", ui.sprite(1301, 37, 72, 72));
        sprites.hud("sparkle", ui.sprite(519, 141, 70, 66));
        sprites.hud("backpack_icon", ui.sprite(32, 673, 96, 64));
        sprites.hud("crafting_icon", ui.sprite(150, 673, 96, 64));
        sprites.hud("furnace_icon", ui.sprite(270, 673, 96, 64));
        sprites.hud("book_icon", ui.sprite(390, 673, 96, 64));
        sprites.hud("panel_inventory", ui.sprite(22, 228, 354, 318));
        sprites.hud("panel_crafting", ui.sprite(390, 228, 292, 331));
        sprites.hud("panel_campfire", ui.sprite(706, 228, 264, 331));
        sprites.hud("panel_journal", ui.sprite(990, 228, 236, 331));
        sprites.hud("panel_settings", ui.sprite(1238, 226, 272, 331));
        sprites.hud("frame_moss", ui.sprite(26, 865, 194, 94));
        sprites.hud("frame_wood", ui.sprite(246, 865, 194, 94));
        sprites.hud("frame_stone", ui.sprite(469, 865, 194, 94));
        sprites.hud("button_green", ui.sprite(29, 584, 114, 64));
        sprites.hud("button_close", ui.sprite(386, 584, 91, 64));

        sprites.item("voxel:grass_block", blocks.sprite(1123, 259, 150, 158));
        sprites.item("voxel:dirt", blocks.sprite(211, 50, 150, 158));
        sprites.item("voxel:clay", blocks.sprite(394, 50, 150, 158));
        sprites.item("voxel:stone", blocks.sprite(576, 50, 150, 158));
        sprites.item("voxel:mossy_stone", blocks.sprite(758, 50, 150, 158));
        sprites.item("voxel:sand", blocks.sprite(1123, 50, 150, 158));
        sprites.item("voxel:gravel", blocks.sprite(1306, 50, 150, 158));
        sprites.item("voxel:skyroot_log", blocks.sprite(211, 259, 150, 158));
        sprites.item("voxel:pine_log", blocks.sprite(576, 259, 150, 158));
        sprites.item("voxel:skyroot_leaves", blocks.sprite(758, 259, 150, 158));
        sprites.item("voxel:pine_leaves", blocks.sprite(941, 259, 150, 158));
        sprites.item("voxel:sun_bloom", blocks.sprite(1122, 259, 150, 158));
        sprites.item("voxel:water", blocks.sprite(211, 469, 150, 158));
        sprites.item("voxel:skyroot_planks", blocks.sprite(576, 469, 150, 158));
        sprites.item("voxel:mossy_path", blocks.sprite(941, 469, 150, 158));
        sprites.item("voxel:ice", blocks.sprite(575, 681, 150, 158));
        sprites.item("voxel:red_mushroom", food.sprite(812, 132, 110, 94));
        sprites.item("voxel:mushroom", food.sprite(958, 132, 110, 94));
        sprites.item("voxel:mushroom_cluster", food.sprite(958, 132, 110, 94));
        sprites.item("voxel:glow_mushroom", ores.sprite(961, 519, 132, 116));
        sprites.item("voxel:glow_mushroom_cap", ores.sprite(961, 519, 132, 116));
        sprites.item("voxel:spore_blossom", food.sprite(982, 678, 106, 70));
        sprites.item("voxel:berries", food.sprite(214, 132, 125, 93));
        sprites.item("voxel:cooked_berries", food.sprite(360, 327, 128, 98));
        sprites.item("voxel:berry_jam", food.sprite(360, 327, 128, 98));
        sprites.item("voxel:apple", food.sprite(654, 132, 105, 96));
        sprites.item("voxel:healing_snack", food.sprite(638, 324, 130, 100));
        sprites.item("voxel:mushroom_stew", food.sprite(1137, 325, 126, 94));
        sprites.item("voxel:hearty_stew", food.sprite(1137, 325, 126, 94));
        sprites.item("voxel:glow_mushroom_stew", food.sprite(1137, 325, 126, 94));
        sprites.item("voxel:herb_soup", food.sprite(971, 326, 124, 94));
        sprites.item("voxel:calming_tea", food.sprite(971, 326, 124, 94));
        sprites.item("voxel:spore_tea", food.sprite(971, 326, 124, 94));
        sprites.item("voxel:roasted_mushroom", food.sprite(222, 326, 116, 90));
        sprites.item("voxel:twig", food.sprite(218, 496, 106, 63));
        sprites.item("voxel:pebble", food.sprite(374, 499, 99, 65));
        sprites.item("voxel:small_stone", food.sprite(374, 499, 99, 65));
        sprites.item("voxel:fiber", food.sprite(779, 493, 112, 82));
        sprites.item("voxel:wild_grass", food.sprite(779, 493, 112, 82));
        sprites.item("voxel:moss_clump", food.sprite(779, 493, 112, 82));
        sprites.item("voxel:reeds", food.sprite(643, 683, 107, 67));
        sprites.item("voxel:reed_bundle", food.sprite(643, 683, 107, 67));
        sprites.item("voxel:wild_herbs", food.sprite(982, 678, 106, 70));
        sprites.item("voxel:dry_grass", food.sprite(643, 683, 107, 67));
        sprites.item("voxel:bark_strip", food.sprite(218, 853, 104, 45));
        sprites.item("voxel:tool_handle", food.sprite(218, 853, 104, 45));
        sprites.item("voxel:resin", food.sprite(1171, 677, 78, 84));
        sprites.item("voxel:clay_lump", food.sprite(504, 497, 98, 66));
        sprites.item("voxel:charcoal", ores.sprite(744, 520, 122, 96));
        sprites.item("voxel:coal", ores.sprite(744, 520, 122, 96));
        sprites.item("voxel:raw_copper", ores.sprite(47, 519, 130, 95));
        sprites.item("voxel:raw_iron", ores.sprite(372, 519, 130, 95));
        sprites.item("voxel:copper_ingot", ores.sprite(49, 735, 132, 82));
        sprites.item("voxel:iron_ingot", ores.sprite(374, 735, 132, 82));
        sprites.item("voxel:gold_ingot", ores.sprite(374, 735, 132, 82));
        sprites.item("voxel:platin_ingot", ores.sprite(374, 735, 132, 82));
        sprites.item("voxel:titan_ingot", ores.sprite(374, 735, 132, 82));
        sprites.item("voxel:ruby_shard", ores.sprite(961, 519, 132, 116));
        sprites.item("voxel:sapphire_shard", ores.sprite(961, 519, 132, 116));
        sprites.item("voxel:glow_crystal", ores.sprite(961, 519, 132, 116));
        sprites.item("voxel:ancient_fragment", ores.sprite(961, 519, 132, 116));
        sprites.item("voxel:ruin_key", ores.sprite(961, 519, 132, 116));
        sprites.item("voxel:ruin_seal", ores.sprite(961, 519, 132, 116));
        sprites.item("voxel:lost_charm", ores.sprite(961, 519, 132, 116));
        sprites.item("voxel:slime_drop", ores.sprite(961, 519, 132, 116));
        sprites.item("voxel:glow_crystal_node", ores.sprite(1028, 147, 133, 130));
        sprites.item("voxel:copper_ore", ores.sprite(46, 146, 130, 132));
        sprites.item("voxel:iron_ore", ores.sprite(377, 146, 130, 132));
        sprites.item("voxel:coal_ore", ores.sprite(742, 146, 130, 132));
        sprites.item("voxel:gold_ore", ores.sprite(377, 146, 130, 132));
        sprites.item("voxel:platin_ore", ores.sprite(377, 146, 130, 132));
        sprites.item("voxel:ruby_ore", ores.sprite(1028, 147, 133, 130));
        sprites.item("voxel:sapphire_ore", ores.sprite(1028, 147, 133, 130));
        sprites.item("voxel:titan_ore", ores.sprite(377, 146, 130, 132));
        sprites.item("voxel:clay_deposit", ores.sprite(1306, 147, 130, 132));

        sprites.item("voxel:stone_sword", tools.sprite(76, 123, 150, 132));
        sprites.item("voxel:iron_sword", tools.sprite(76, 123, 150, 132));
        sprites.item("voxel:platin_sword", tools.sprite(76, 123, 150, 132));
        sprites.item("voxel:sapphire_sword", tools.sprite(76, 123, 150, 132));
        sprites.item("voxel:titan_sword", tools.sprite(76, 123, 150, 132));
        sprites.item("voxel:stone_axe", tools.sprite(254, 116, 145, 145));
        sprites.item("voxel:stone_pickaxe", tools.sprite(438, 116, 145, 145));
        sprites.item("voxel:copper_axe", tools.sprite(621, 115, 145, 145));
        sprites.item("voxel:copper_pickaxe", tools.sprite(805, 116, 145, 145));
        sprites.item("voxel:iron_axe", tools.sprite(621, 115, 145, 145));
        sprites.item("voxel:iron_pickaxe", tools.sprite(805, 116, 145, 145));
        sprites.item("voxel:crystal_axe", tools.sprite(621, 115, 145, 145));
        sprites.item("voxel:crystal_pickaxe", tools.sprite(805, 116, 145, 145));
        sprites.item("voxel:crystal_knife", tools.sprite(270, 542, 150, 150));
        sprites.item("voxel:stone_shovel", tools.sprite(91, 318, 144, 150));
        sprites.item("voxel:stone_knife", tools.sprite(270, 542, 150, 150));
        sprites.item("voxel:simple_rope", tools.sprite(1265, 547, 125, 129));

        sprites.item("voxel:storage_crate", ui.sprite(714, 142, 70, 66));
        sprites.item("voxel:workbench", ui.sprite(714, 142, 70, 66));
        sprites.item("voxel:forge", ores.sprite(1306, 147, 130, 132));
        sprites.item("voxel:campfire", ui.sprite(1007, 35, 76, 78));
        sprites.item("voxel:campfire_active", ui.sprite(1007, 35, 76, 78));
        sprites.item("voxel:campfire_burned_out", ui.sprite(610, 35, 70, 74));
        sprites.item("voxel:lantern", food.sprite(1170, 854, 82, 86));
        sprites.item("voxel:ancient_lantern", food.sprite(1170, 854, 82, 86));
        sprites.item("voxel:torch", food.sprite(1170, 854, 82, 86));
        sprites.item("voxel:resin_torch", food.sprite(1170, 854, 82, 86));
        sprites.item("voxel:flower_pot", food.sprite(1137, 132, 126, 98));
        sprites.item("voxel:clay_pot", food.sprite(1137, 132, 126, 98));
        sprites.item("voxel:cooking_pot", food.sprite(1137, 132, 126, 98));
        sprites.item("voxel:water_container", food.sprite(1137, 132, 126, 98));
        sprites.item("voxel:clay_bowl", food.sprite(972, 326, 124, 94));
        sprites.item("voxel:cloth", ui.sprite(33, 870, 182, 80));
        sprites.item("voxel:leather_strip", food.sprite(218, 853, 104, 45));
        sprites.item("voxel:honey", food.sprite(360, 327, 128, 98));
        sprites.item("voxel:woven_rug", ui.sprite(33, 870, 182, 80));
        sprites.item("voxel:small_table", ui.sprite(712, 142, 72, 66));
        sprites.item("voxel:wooden_chair", ui.sprite(615, 141, 72, 68));
        sprites.item("voxel:garden_fence", ui.sprite(33, 870, 182, 80));
        sprites.item("voxel:berry_bush", food.sprite(976, 855, 110, 68));
        sprites.item("voxel:herb_planter", food.sprite(1170, 677, 86, 84));
        sprites.item("voxel:cactus", blocks.sprite(1306, 259, 150, 158));

        sprites.overrideItemSpritesFromIndividualAssets();
        return sprites;
    }

    public static List<String> itemTextureCandidates(String itemKey) {
        if (itemKey == null || itemKey.isBlank()) {
            return List.of();
        }
        String name = itemKey.substring(itemKey.indexOf(':') + 1);
        List<String> baseNames = itemTextureBaseNames(name);
        List<String> candidates = new ArrayList<>();
        for (String baseName : baseNames) {
            for (String root : List.of(ITEM_TEXTURE_ROOT, ITEMS_TEXTURE_ROOT, ASSET_MINERALS_TEXTURE_ROOT)) {
                addUnique(candidates, root + baseName + ".png");
            }
        }
        for (String baseName : baseNames) {
            for (String suffix : itemBlockSuffixes(baseName)) {
                for (String root : List.of(BLOCK_TEXTURE_ROOT, BLOCKS_TEXTURE_ROOT, ASSET_BLOCKS_TEXTURE_ROOT, ASSET_DROP_TEXTURE_ROOT)) {
                    addUnique(candidates, root + baseName + suffix + ".png");
                }
            }
        }
        return candidates;
    }

    public Optional<UiSprite> item(String itemKey) {
        return Optional.ofNullable(itemSprites.get(itemKey));
    }

    public Optional<UiSprite> hud(String spriteKey) {
        return Optional.ofNullable(hudSprites.get(spriteKey));
    }

    public Collection<String> mappedItemKeys() {
        return itemSprites.keySet();
    }

    @Override
    public void close() {
        for (UiSpriteSheet sheet : sheets) {
            sheet.close();
        }
    }

    private UiSpriteSheet sheet(String resourcePath, boolean keyBlackTransparent) {
        return sheet(resourcePath, keyBlackTransparent ? UiSpriteSheet.BackgroundMode.KEY_BLACK : UiSpriteSheet.BackgroundMode.OPAQUE);
    }

    private Optional<UiSpriteSheet> optionalSheet(String resourcePath, UiSpriteSheet.BackgroundMode backgroundMode) {
        try {
            return Optional.of(sheet(resourcePath, backgroundMode));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Optional<UiSpriteSheet> optionalSheet(String resourcePath, UiSpriteSheet.BackgroundMode backgroundMode, int maxDimension) {
        try {
            return Optional.of(sheet(resourcePath, backgroundMode, maxDimension));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private UiSpriteSheet sheet(String resourcePath, UiSpriteSheet.BackgroundMode backgroundMode) {
        return sheet(resourcePath, backgroundMode, 0);
    }

    private UiSpriteSheet sheet(String resourcePath, UiSpriteSheet.BackgroundMode backgroundMode, int maxDimension) {
        UiSpriteSheet sheet = UiSpriteSheet.load(resourcePath, backgroundMode, maxDimension);
        sheets.add(sheet);
        return sheet;
    }

    private void item(String key, UiSprite sprite) {
        itemSprites.put(key, sprite);
    }

    private void hud(String key, UiSprite sprite) {
        hudSprites.put(key, sprite);
    }

    private void overrideItemSpritesFromIndividualAssets() {
        Set<String> keys = new LinkedHashSet<>(itemSprites.keySet());
        Items.createDefaultRegistry().values().forEach(item -> keys.add(item.key()));
        keys.addAll(OPTIONAL_ITEM_KEYS);
        for (String key : keys) {
            for (String candidate : itemTextureCandidates(key)) {
                if (!resourceExists(candidate)) {
                    continue;
                }
                Optional<UiSpriteSheet> sheet = optionalSheet(candidate, UiSpriteSheet.BackgroundMode.EDGE_CHECKER_TRIM, INDIVIDUAL_ITEM_MAX_TEXTURE_SIZE);
                if (sheet.isPresent()) {
                    item(key, sheet.get().fullSprite());
                    break;
                }
            }
        }
    }

    private static List<String> itemTextureBaseNames(String name) {
        List<String> baseNames = new ArrayList<>();
        addUnique(baseNames, name);
        switch (name) {
            case "grass_block" -> {
                addUnique(baseNames, "grass");
                addUnique(baseNames, "mossy_grass");
            }
            case "skyroot_log" -> addUnique(baseNames, "oak_log");
            case "skyroot_leaves" -> addUnique(baseNames, "oak_leaves");
            case "pine_log" -> addUnique(baseNames, "spruce_log");
            case "pine_leaves" -> addUnique(baseNames, "spruce_leaves");
            case "mossy_stone" -> {
                addUnique(baseNames, "cobblestone");
                addUnique(baseNames, "cracked_cobblestone");
            }
            case "mossy_path" -> {
                addUnique(baseNames, "mossy_grass");
                addUnique(baseNames, "myzelium");
            }
            case "ice" -> addUnique(baseNames, "ice_block");
            case "skyroot_planks" -> addUnique(baseNames, "oak_planks");
            case "pine_planks" -> addUnique(baseNames, "spruce_planks");
            case "snowy_grass_block" -> {
                addUnique(baseNames, "snowy_grass");
                addUnique(baseNames, "dirt");
            }
            case "stone_bricks" -> addUnique(baseNames, "stone_brick_block");
            case "mossy_stone_bricks" -> addUnique(baseNames, "mossy_stone_brick_block");
            case "fancy_stone_bricks" -> addUnique(baseNames, "fancy_stone_brick_block");
            case "mossy_fancy_stone_bricks" -> addUnique(baseNames, "mossy_fancy_stone_brick_block");
            case "glass" -> addUnique(baseNames, "glass_block");
            case "platinum_ingot" -> addUnique(baseNames, "platin_ingot");
            case "titanium_ingot" -> addUnique(baseNames, "titan_ingot");
            case "tree_stump" -> addUnique(baseNames, "oak_log");
            default -> {
            }
        }
        return baseNames;
    }

    private static List<String> itemBlockSuffixes(String baseName) {
        if (baseName.endsWith("_log")) {
            return List.of("_side", "_top", "");
        }
        if (baseName.equals("grass_block") || baseName.equals("grass") || baseName.equals("mossy_grass") || baseName.equals("myzelium")) {
            return List.of("_top", "_side", "");
        }
        return List.of("", "_top", "_side", "_bottom");
    }

    private static void addUnique(List<String> values, String value) {
        if (!values.contains(value)) {
            values.add(value);
        }
    }

    private static boolean resourceExists(String path) {
        try (InputStream input = GameSprites.class.getClassLoader().getResourceAsStream(path)) {
            return input != null;
        } catch (IOException e) {
            return false;
        }
    }
}
