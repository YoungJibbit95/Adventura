package dev.voxelgame.client.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class GameSprites implements AutoCloseable {
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
        UiSpriteSheet ores = sprites.sheet("assets/game/ores_materials_sheet.png", UiSpriteSheet.BackgroundMode.EDGE_CHECKER);

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
        sprites.hud("slot", ui.sprite(809, 142, 70, 66));
        sprites.hud("slot_selected", ui.sprite(905, 142, 70, 66));
        sprites.hud("slot_hotbar", ui.sprite(714, 142, 70, 66));
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

        sprites.item("voxel:grass_block", blocks.sprite(29, 49, 150, 158));
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
        sprites.item("voxel:berries", food.sprite(214, 132, 125, 93));
        sprites.item("voxel:cooked_berries", food.sprite(360, 327, 128, 98));
        sprites.item("voxel:apple", food.sprite(654, 132, 105, 96));
        sprites.item("voxel:healing_snack", food.sprite(638, 324, 130, 100));
        sprites.item("voxel:mushroom_stew", food.sprite(1137, 325, 126, 94));
        sprites.item("voxel:herb_soup", food.sprite(971, 326, 124, 94));
        sprites.item("voxel:roasted_mushroom", food.sprite(222, 326, 116, 90));
        sprites.item("voxel:twig", food.sprite(218, 496, 106, 63));
        sprites.item("voxel:pebble", food.sprite(374, 499, 99, 65));
        sprites.item("voxel:small_stone", food.sprite(374, 499, 99, 65));
        sprites.item("voxel:fiber", food.sprite(779, 493, 112, 82));
        sprites.item("voxel:wild_grass", food.sprite(779, 493, 112, 82));
        sprites.item("voxel:wild_herbs", food.sprite(982, 678, 106, 70));
        sprites.item("voxel:dry_grass", food.sprite(643, 683, 107, 67));
        sprites.item("voxel:bark_strip", food.sprite(218, 853, 104, 45));
        sprites.item("voxel:resin", food.sprite(1171, 677, 78, 84));
        sprites.item("voxel:clay_lump", food.sprite(504, 497, 98, 66));
        sprites.item("voxel:charcoal", ores.sprite(744, 520, 122, 96));
        sprites.item("voxel:coal", ores.sprite(744, 520, 122, 96));
        sprites.item("voxel:raw_copper", ores.sprite(47, 519, 130, 95));
        sprites.item("voxel:raw_iron", ores.sprite(372, 519, 130, 95));
        sprites.item("voxel:copper_ingot", ores.sprite(49, 735, 132, 82));
        sprites.item("voxel:glow_crystal", ores.sprite(961, 519, 132, 116));
        sprites.item("voxel:glow_crystal_node", ores.sprite(1028, 147, 133, 130));
        sprites.item("voxel:copper_ore", ores.sprite(46, 146, 130, 132));
        sprites.item("voxel:iron_ore", ores.sprite(377, 146, 130, 132));
        sprites.item("voxel:coal_ore", ores.sprite(742, 146, 130, 132));
        sprites.item("voxel:clay_deposit", ores.sprite(1306, 147, 130, 132));

        sprites.item("voxel:stone_sword", tools.sprite(76, 123, 150, 132));
        sprites.item("voxel:stone_axe", tools.sprite(254, 116, 145, 145));
        sprites.item("voxel:stone_pickaxe", tools.sprite(438, 116, 145, 145));
        sprites.item("voxel:copper_axe", tools.sprite(621, 115, 145, 145));
        sprites.item("voxel:copper_pickaxe", tools.sprite(805, 116, 145, 145));
        sprites.item("voxel:stone_shovel", tools.sprite(91, 318, 144, 150));
        sprites.item("voxel:stone_knife", tools.sprite(270, 542, 150, 150));
        sprites.item("voxel:simple_rope", tools.sprite(1265, 547, 125, 129));

        sprites.item("voxel:storage_crate", ui.sprite(714, 142, 70, 66));
        sprites.item("voxel:campfire", ui.sprite(1007, 35, 76, 78));
        sprites.item("voxel:campfire_active", ui.sprite(1007, 35, 76, 78));
        sprites.item("voxel:campfire_burned_out", ui.sprite(610, 35, 70, 74));
        sprites.item("voxel:lantern", food.sprite(1170, 854, 82, 86));
        sprites.item("voxel:torch", food.sprite(1170, 854, 82, 86));
        sprites.item("voxel:flower_pot", food.sprite(1137, 132, 126, 98));
        sprites.item("voxel:clay_pot", food.sprite(1137, 132, 126, 98));
        sprites.item("voxel:clay_bowl", food.sprite(972, 326, 124, 94));
        sprites.item("voxel:woven_rug", ui.sprite(33, 870, 182, 80));
        sprites.item("voxel:small_table", ui.sprite(712, 142, 72, 66));
        sprites.item("voxel:wooden_chair", ui.sprite(615, 141, 72, 68));
        sprites.item("voxel:garden_fence", ui.sprite(33, 870, 182, 80));
        sprites.item("voxel:berry_bush", food.sprite(976, 855, 110, 68));
        sprites.item("voxel:herb_planter", food.sprite(1170, 677, 86, 84));
        sprites.item("voxel:cactus", blocks.sprite(1306, 259, 150, 158));

        return sprites;
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

    private UiSpriteSheet sheet(String resourcePath, UiSpriteSheet.BackgroundMode backgroundMode) {
        UiSpriteSheet sheet = UiSpriteSheet.load(resourcePath, backgroundMode);
        sheets.add(sheet);
        return sheet;
    }

    private void item(String key, UiSprite sprite) {
        itemSprites.put(key, sprite);
    }

    private void hud(String key, UiSprite sprite) {
        hudSprites.put(key, sprite);
    }
}
