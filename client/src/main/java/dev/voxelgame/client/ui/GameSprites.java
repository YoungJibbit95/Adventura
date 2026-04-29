package dev.voxelgame.client.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class GameSprites implements AutoCloseable {
    private final Map<String, UiSprite> itemSprites = new HashMap<>();
    private final Set<UiSpriteSheet> sheets = new LinkedHashSet<>();

    private GameSprites() {
    }

    public static GameSprites loadDefault() {
        GameSprites sprites = new GameSprites();
        UiSpriteSheet items = sprites.sheet("assets/game/items_inventory.png", true);
        UiSpriteSheet blocks = sprites.sheet("assets/game/bloecke_blocks.png", true);
        UiSpriteSheet nature = sprites.sheet("assets/game/natursachen_nature.png", true);
        UiSpriteSheet decor = sprites.sheet("assets/game/deko_decor.png", true);

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

        return sprites;
    }

    public Optional<UiSprite> item(String itemKey) {
        return Optional.ofNullable(itemSprites.get(itemKey));
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
        UiSpriteSheet sheet = UiSpriteSheet.load(resourcePath, keyBlackTransparent);
        sheets.add(sheet);
        return sheet;
    }

    private void item(String key, UiSprite sprite) {
        itemSprites.put(key, sprite);
    }
}
