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
        UiSpriteSheet items = sprites.sheet("assets/game/items_inventory.png", true);
        UiSpriteSheet blocks = sprites.sheet("assets/game/bloecke_blocks.png", true);
        UiSpriteSheet nature = sprites.sheet("assets/game/natursachen_nature.png", true);
        UiSpriteSheet decor = sprites.sheet("assets/game/deko_decor.png", true);
        UiSpriteSheet hud = sprites.sheet("assets/game/misc_wasser_ui_paletten.png", true);

        sprites.hud("heart_full", hud.sprite(19, 103, 21, 19));
        sprites.hud("heart_empty", hud.sprite(133, 103, 23, 19));
        sprites.hud("hunger_full", hud.sprite(176, 103, 19, 19));
        sprites.hud("hunger_empty", hud.sprite(300, 103, 22, 18));
        sprites.hud("armor_full", hud.sprite(19, 154, 19, 17));
        sprites.hud("air_full", hud.sprite(176, 154, 18, 16));

        sprites.item("voxel:stone", items.sprite(27, 40, 44, 41));
        sprites.item("voxel:dirt", items.sprite(88, 37, 49, 46));
        sprites.item("voxel:grass_block", items.sprite(151, 35, 47, 47));
        sprites.item("voxel:skyroot_planks", items.sprite(275, 35, 47, 47));
        sprites.item("voxel:stick", items.sprite(339, 38, 44, 43));
        sprites.item("voxel:coal", items.sprite(396, 34, 48, 48));
        sprites.item("voxel:torch", items.sprite(518, 32, 50, 54));
        sprites.item("voxel:raw_iron", items.sprite(85, 105, 52, 43));
        sprites.item("voxel:raw_copper", items.sprite(273, 107, 49, 40));
        sprites.item("voxel:apple", items.sprite(276, 172, 43, 44));
        sprites.item("voxel:berries", items.sprite(459, 173, 44, 43));
        sprites.item("voxel:red_mushroom", items.sprite(26, 175, 40, 41));
        sprites.item("voxel:cactus", items.sprite(88, 174, 44, 41));
        sprites.item("voxel:stone_pickaxe", items.sprite(24, 392, 42, 45));
        sprites.item("voxel:stone_axe", items.sprite(88, 392, 42, 45));
        sprites.item("voxel:stone_shovel", items.sprite(148, 387, 50, 53));
        sprites.item("voxel:stone_sword", items.sprite(212, 389, 48, 51));

        sprites.item("voxel:sand", blocks.sprite(19, 104, 59, 53));
        sprites.item("voxel:skyroot_log", blocks.sprite(19, 174, 58, 54));
        sprites.item("voxel:skyroot_leaves", blocks.sprite(430, 174, 56, 54));
        sprites.item("voxel:clay", blocks.sprite(362, 104, 57, 53));
        sprites.item("voxel:mossy_stone", blocks.sprite(226, 29, 57, 55));
        sprites.item("voxel:gravel", blocks.sprite(294, 104, 58, 53));
        sprites.item("voxel:snow", blocks.sprite(430, 104, 57, 53));
        sprites.item("voxel:ice", blocks.sprite(630, 104, 57, 53));
        sprites.item("voxel:pine_log", blocks.sprite(157, 174, 59, 53));
        sprites.item("voxel:pine_leaves", blocks.sprite(497, 174, 56, 54));
        sprites.item("voxel:coal_ore", blocks.sprite(20, 246, 57, 53));
        sprites.item("voxel:iron_ore", blocks.sprite(88, 246, 58, 53));
        sprites.item("voxel:copper_ore", blocks.sprite(157, 245, 59, 54));

        sprites.item("voxel:wild_grass", nature.sprite(4, 32, 39, 36));
        sprites.item("voxel:sun_bloom", nature.sprite(291, 29, 33, 27));
        sprites.item("voxel:fiber", nature.sprite(59, 33, 36, 35));
        sprites.item("voxel:torch_wall", decor.sprite(535, 11, 21, 36));

        sprites.optionalSheet("assets/game/generated_item_icons_sheet.png", UiSpriteSheet.BackgroundMode.EDGE_CHECKER).ifPresent(sheet -> {
            sprites.item("voxel:stone", sheet.sprite(35, 156, 109, 120));
            sprites.item("voxel:dirt", sheet.sprite(175, 157, 110, 119));
            sprites.item("voxel:grass_block", sheet.sprite(315, 157, 112, 119));
            sprites.item("voxel:skyroot_planks", sheet.sprite(595, 158, 109, 119));
            sprites.item("voxel:stick", sheet.sprite(737, 165, 101, 104));
            sprites.item("voxel:coal", sheet.sprite(860, 170, 99, 99));
            sprites.item("voxel:torch", sheet.sprite(1133, 164, 36, 105));
            sprites.item("voxel:raw_iron", sheet.sprite(168, 347, 112, 93));
            sprites.item("voxel:raw_copper", sheet.sprite(569, 341, 106, 92));
            sprites.item("voxel:red_mushroom", sheet.sprite(35, 674, 100, 95));
            sprites.item("voxel:cactus", sheet.sprite(171, 656, 97, 114));
            sprites.item("voxel:apple", sheet.sprite(573, 660, 87, 109));
            sprites.item("voxel:berries", sheet.sprite(948, 667, 99, 96));
            sprites.item("voxel:twig", sheet.sprite(737, 165, 101, 104));
            sprites.item("voxel:pebble", sheet.sprite(35, 156, 109, 120));
            sprites.item("voxel:resin", sheet.sprite(1322, 341, 88, 95));
            sprites.item("voxel:feathers", sheet.sprite(1072, 660, 98, 109));
            sprites.item("voxel:wild_herbs", sheet.sprite(1072, 660, 98, 109));
            sprites.item("voxel:simple_rope", sheet.sprite(737, 165, 101, 104));
            sprites.item("voxel:healing_snack", sheet.sprite(948, 667, 99, 96));
            sprites.item("voxel:small_stone", sheet.sprite(35, 156, 109, 120));
        });

        sprites.optionalSheet("assets/game/generated_tools_sheet.png", UiSpriteSheet.BackgroundMode.EDGE_CHECKER).ifPresent(sheet -> {
            sprites.item("voxel:stone_pickaxe", sheet.sprite(57, 152, 160, 165));
            sprites.item("voxel:stone_axe", sheet.sprite(684, 147, 150, 176));
            sprites.item("voxel:stone_shovel", sheet.sprite(1066, 155, 162, 168));
            sprites.item("voxel:stone_sword", sheet.sprite(36, 425, 187, 192));
            sprites.item("voxel:stone_knife", sheet.sprite(296, 700, 197, 206));
            sprites.item("voxel:torch", sheet.sprite(1100, 426, 85, 191));
        });

        sprites.optionalSheet("assets/game/generated_plants_sheet.png", UiSpriteSheet.BackgroundMode.EDGE_CHECKER).ifPresent(sheet -> {
            sprites.item("voxel:wild_grass", sheet.sprite(50, 137, 131, 108));
            sprites.item("voxel:fiber", sheet.sprite(213, 121, 129, 124));
            sprites.item("voxel:sun_bloom", sheet.sprite(872, 118, 93, 127));
            sprites.item("voxel:berry_bush", sheet.sprite(710, 126, 122, 119));
            sprites.item("voxel:herb_planter", sheet.sprite(1150, 131, 95, 114));
            sprites.item("voxel:tree_stump", sheet.sprite(551, 132, 120, 113));
        });

        sprites.optionalSheet("assets/game/generated_decor_props_sheet.png", UiSpriteSheet.BackgroundMode.EDGE_CHECKER).ifPresent(sheet -> {
            sprites.item("voxel:torch_wall", sheet.sprite(1308, 119, 68, 178));
            sprites.item("voxel:flower_pot", sheet.sprite(585, 127, 118, 166));
            sprites.item("voxel:lantern", sheet.sprite(765, 140, 102, 153));
            sprites.item("voxel:campfire", sheet.sprite(921, 136, 159, 165));
            sprites.item("voxel:storage_crate", sheet.sprite(45, 130, 153, 167));
            sprites.item("voxel:small_table", sheet.sprite(1086, 351, 139, 164));
            sprites.item("voxel:wooden_chair", sheet.sprite(556, 360, 161, 145));
            sprites.item("voxel:woven_rug", sheet.sprite(742, 365, 150, 143));
            sprites.item("voxel:garden_fence", sheet.sprite(55, 352, 125, 163));
            sprites.item("voxel:mossy_path", sheet.sprite(556, 360, 161, 145));
        });

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
