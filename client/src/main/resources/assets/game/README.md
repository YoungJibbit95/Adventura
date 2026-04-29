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

The renderer also checks `textures/blocks/` as a compatibility alias. For each block, `<block_name>.png` is enough; add `<block_name>_top.png`, `<block_name>_side.png` or `<block_name>_bottom.png` when a block needs different faces. If no individual file exists, known terrain sprites are sliced from the current sheets below.

Good next folders:

- `textures/item/` for future item icons.
- `textures/entity/` for future entity skins.
- `ui/` for HUD and menu sprites.
- `audio/` for future sounds.

Current sprite-sheet wiring:

- `ui_hud_sheet.png` feeds HUD hearts, hunger icons, menu surfaces and selected decor icons.
- `blocks_tiles_sheet.png` feeds terrain/block texture atlas fallbacks and block-like item icons.
- `tools_weapons_sheet.png` feeds tool and held-item icons.
- `nature_food_sheet.png` feeds food, plant, fuel and cozy resource icons.
- `ores_materials_sheet.png` feeds ore blocks, raw materials, ingots and crystal icons.
