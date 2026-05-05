# Game Asset Drop Folder

Put original, non-Minecraft assets here. The current sprite sheets in this folder are supported by the renderer as fallback sources. Individual PNG files are still useful for overrides because the client can build its own atlas and can fall back per missing block.

Recommended block texture paths:

- `textures/block/stone.png`
- `textures/block/grass_block_top.png`
- `textures/block/grass_block_side.png`
- `textures/block/dirt.png`
- `textures/block/skyroot_log_side.png`
- `textures/block/skyroot_log_top.png`
- `textures/block/skyroot_planks.png`

Current migration folders:

- `blocks/<block_name>.png` for full block faces and block-like item icons.
- `blocks/<block_name>_top.png`, `_side.png`, `_bottom.png` for face-specific blocks.
- `minerals/<item_name>.png` for ore drops, ingots, shards and material icons.

The renderer also checks `textures/blocks/` as a compatibility alias. During the current migration it accepts `blocks/` files first, then legacy root-level drop files such as `stone.png`, `grass_block_top.png`, `oak_log_side.png` and `spruce_log_top.png`. For each block, `<block_name>.png` is enough; add `<block_name>_top.png`, `<block_name>_side.png` or `<block_name>_bottom.png` when a block needs different faces. If no individual file exists, known terrain sprites are sliced from the current sheets below.

Current root-level compatibility aliases:

- `oak_log_*` -> `voxel:skyroot_log`
- `oak_leaves` -> `voxel:skyroot_leaves`
- `spruce_log_*` -> `voxel:pine_log`
- `spruce_leaves` -> `voxel:pine_leaves`
- `mossy_grass_*` -> `voxel:mossy_path`
- `cobblestone` / `cracked_cobblestone` -> `voxel:mossy_stone`
- `ice_block` -> `voxel:ice`
- `oak_planks` -> `voxel:skyroot_planks`
- `spruce_planks` -> `voxel:pine_planks`
- `stone_brick_block` -> `voxel:stone_bricks`
- `mossy_stone_brick_block` -> `voxel:mossy_stone_bricks`
- `fancy_stone_brick_block` -> `voxel:fancy_stone_bricks`
- `mossy_fancy_stone_brick_block` -> `voxel:mossy_fancy_stone_bricks`
- `snowy_grass_block_*` -> `voxel:snowy_grass_block`
- `glass_block` -> `voxel:glass`

Large drop textures are normalized into the runtime atlas at a capped tile size so the client does not allocate a massive GPU atlas. Keep future production textures at 256x256 or lower unless a renderer task explicitly raises that budget.

Good next folders:

- `textures/item/` for finalized item icons.
- `textures/entity/` for future entity skins.
- `ui/` for HUD and menu sprites.
- `audio/` for future sounds.

Current sprite-sheet wiring:

- `ui_hud_sheet.png` feeds HUD hearts, hunger icons, menu surfaces and selected decor icons.
- `blocks_tiles_sheet.png` feeds terrain/block texture atlas fallbacks and block-like item icons.
- `tools_weapons_sheet.png` feeds tool and held-item icons.
- `nature_food_sheet.png` feeds food, plant, fuel and cozy resource icons.
- `ores_materials_sheet.png` is optional legacy fallback. Individual `blocks/` ore textures and `minerals/` item icons are preferred.
