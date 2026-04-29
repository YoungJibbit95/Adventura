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

The renderer also checks `textures/blocks/` as a compatibility alias. For each block, `<block_name>.png` is enough; add `<block_name>_top.png`, `<block_name>_side.png` or `<block_name>_bottom.png` when a block needs different faces. If no individual file exists, known sprites are sliced from `bloecke_blocks.png`, `natursachen_nature.png`, `deko_decor.png` and `misc_wasser_ui_paletten.png`.

Good next folders:

- `textures/item/` for future item icons.
- `textures/entity/` for future entity skins.
- `ui/` for HUD and menu sprites.
- `audio/` for future sounds.

Current sprite-sheet wiring:

- `items_inventory.png` feeds hotbar, crafting and inventory icons.
- `bloecke_blocks.png`, `natursachen_nature.png` and `deko_decor.png` provide fallback item icons for block-like items and plants.
- `bloecke_blocks.png`, `natursachen_nature.png`, `deko_decor.png` and `misc_wasser_ui_paletten.png` provide fallback block textures for the world renderer.
- `extra_blocks_sheet.png`, `extra_items_sheet.png`, `extra_tools_sheet.png`, `extra_plants_sheet.png` and `extra_decor_sheet.png` are source sheets ready for future mapping.
