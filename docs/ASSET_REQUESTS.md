# Asset Requests

Generate original, non-Minecraft assets with a consistent cozy voxel-survival style. Keep filenames lowercase with underscores and provide a license note for every batch.

Preferred delivery for future replacements: individual PNG files under `client/src/main/resources/assets/game/`. For block textures use `textures/block/<block_name>.png`; add `_top`, `_side` or `_bottom` suffixes only when the faces need different art. The current bundled sprite sheets are supported as fallback sources, so you can also keep adding sheets when that is more convenient.

## Asset QA Pass 2026-05-01

Scope checked against `docs/UI_TODO_LIST.md`, `docs/RENDERING_TODO_LIST.md`, `docs/GAMEPLAY_TODO_LIST.md`, `docs/HUD_TODO_LIST.md` and `docs/ANIMATIONS_TODO_LIST.md`.

Do not delete asset TODOs just because they are specified here. A TODO can only be closed when the asset exists, is mapped in code, and has been verified in-game.

### Existing Assets

- `client/src/main/resources/assets/game/ui_hud_sheet.png`, 1536x1024 RGBA: HUD hearts, hunger/energy/air/armor icons, panels, frames and broad button art.
- `client/src/main/resources/assets/game/blocks_tiles_sheet.png`, 1536x1024 RGBA: terrain/block fallback slices.
- `client/src/main/resources/assets/game/tools_weapons_sheet.png`, 1536x1024 RGBA: tool/weapon fallback icons.
- `client/src/main/resources/assets/game/nature_food_sheet.png`, 1536x1024 RGBA: food, plants, fuel and cozy resource fallback icons.
- `client/src/main/resources/assets/game/ores_materials_sheet.png`, 1536x1024 RGBA: ore, ingot, crystal and forge fallback icons.
- `client/src/main/resources/assets/game/license_readme.txt`: current bundled sheets are marked CC0/Public Domain.

There are currently no individual PNG overrides under `textures/block/`, `textures/item/`, `textures/entity/`, `ui/` or `audio/`. All block visuals are therefore sheet fallback based.

### Verified Atlas Status

- `BlockTextureAtlas` validation passes for registered blocks: 53 texture slices, 51/4096 material slots, atlas 1472x1288, tile 182 plus 1px padding, `UV_INSET_PIXELS = 0.5`, filter `nearest-no-mip`.
- Missing block textures: none.
- Duplicate atlas mappings: none.
- Missing material metadata: none.
- Atlas QA tool reference was stale and has been aligned with the current fallback sheets.

### Current Missing or Broken References

- Missing item icon mappings in `GameSprites`: `voxel:feathers`, `voxel:stick`, `voxel:snow`, `voxel:sleeping_mat`, `voxel:tree_stump`.
- Extra sprite mappings not registered as standalone inventory items: `voxel:campfire_active`, `voxel:campfire_burned_out`, `voxel:coal_ore`, `voxel:copper_ore`, `voxel:glow_mushroom`, `voxel:iron_ore`, `voxel:water`. These are block/state helpers, not currently broken.
- No individual entity skins exist. Entities are procedural colored box models.
- No particle sprite atlas exists. Particles are colored quads, so "particle asset" TODOs are currently specification-only until a particle atlas loader exists.
- UI components are partially sheet-backed but not complete as reusable sprites: slot normal/hover/selected/disabled, tabs, tooltip, progress bars, scrollbars, text fields and modal surfaces need proper states.

### Asset Pipeline Rules

- Block override path: `client/src/main/resources/assets/game/textures/block/<block_name>.png`.
- Face-specific block path: `_top`, `_side`, `_bottom`; example `textures/block/grass_block_top.png`, `textures/block/grass_block_side.png`, `textures/block/grass_block_bottom.png`.
- Compatibility alias `textures/blocks/` is supported, but new work should use `textures/block/`.
- Use lowercase snake_case filenames matching registry keys after `voxel:`.
- Block textures should be 16x16 or 32x32, full-tile PNGs unless the block is intentionally cutout.
- Item icons should be 32x32 PNG with transparent background. Current code still needs `GameSprites` mapping or a future item-icon loader.
- UI sprites should use transparent PNGs, crisp pixel edges, and explicit states. 9-slice capable surfaces should keep a 1px clean border and enough corner padding.
- Cutout assets need clean alpha, no colored matte around transparent pixels.
- Avoid adding duplicate filenames in both `textures/block/` and `textures/blocks/`.
- Every delivered batch needs a license note in `docs/ASSET_LICENSES.md` or the asset folder.

### P0 Asset Briefs: Missing Current Icons

| Asset | Target path | Size | Style and material | Variants/states | Used by |
| --- | --- | --- | --- | --- | --- |
| Feathers icon | `client/src/main/resources/assets/game/textures/item/feathers.png` | 32x32 transparent | soft off-white feathers with tan shadow pixels | single icon | inventory, loot, future animal drops |
| Stick icon | `client/src/main/resources/assets/game/textures/item/stick.png` | 32x32 transparent | short brown branch, visually distinct from `twig` | single icon | early crafting, tool recipes |
| Snow item icon | `client/src/main/resources/assets/game/textures/item/snow.png` | 32x32 transparent | compact snowy block/drop, cool blue shadows | single icon | inventory icon for `voxel:snow` |
| Sleeping mat icon | `client/src/main/resources/assets/game/textures/item/sleeping_mat.png` | 32x32 transparent | rolled or folded woven mat, warm cloth/fiber palette | single icon | inventory, crafting, comfort tooltip |
| Tree stump icon | `client/src/main/resources/assets/game/textures/item/tree_stump.png` | 32x32 transparent | squat stump with top rings and bark side | single icon | inventory and pickup pop for `voxel:tree_stump` |

After delivery, map these keys in `GameSprites` or add the item-icon loader. Until then the files alone will not render in item UI.

### P0 Asset Briefs: UI and HUD

| Asset | Target path | Size | Style and material | Variants/states | Used by |
| --- | --- | --- | --- | --- | --- |
| Inventory slot set | `client/src/main/resources/assets/game/ui/slot_normal.png`, `slot_hover.png`, `slot_selected.png`, `slot_disabled.png` | 32x32 or 48x48 transparent | cozy pixel frame, dark inner well, no heavy outline | normal, hover, selected, disabled | Inventory, hotbar, station slots |
| Button set | `client/src/main/resources/assets/game/ui/button_normal.png`, `button_hover.png`, `button_pressed.png`, `button_disabled.png` | 96x32 or 128x32, 9-slice-friendly | warm green/wood UI material, readable contrast | normal, hover, pressed, disabled | menus, settings, crafting controls |
| Tabs | `client/src/main/resources/assets/game/ui/tab_active.png`, `tab_inactive.png`, `tab_hover.png` | 48x24 or 64x24 | compact pixel tab with shared panel border | active, inactive, hover | Journal, crafting categories, settings |
| Tooltip panel | `client/src/main/resources/assets/game/ui/tooltip_panel.png` | 64x64, 9-slice-friendly | dark warm translucent panel with 1px bright rim | one scalable surface | item/tool/entity tooltips |
| Progress bars | `client/src/main/resources/assets/game/ui/progress_track.png`, `progress_fill_heat.png`, `progress_fill_cook.png`, `progress_fill_burn.png` | 64x8 or 96x8 | pixel bar, clear fill colors | track, heat, cook, burn | Campfire, Cooking Pot, Forge |
| Scrollbar | `client/src/main/resources/assets/game/ui/scroll_track.png`, `scroll_thumb.png` | 8x32/8x16 | low-contrast pixel UI, clear hover shape | track, thumb, hover optional | Recipe book, Journal, Settings |
| Comfort icon | `client/src/main/resources/assets/game/ui/icon_comfort.png` | 16x16 and optional 32x32 | warm hearth/leaf/blanket symbol, not debug-like | low/cozy/warm/restful/homey optional | HUD comfort meter, tooltip, level-up pop |
| Feedback category icons | `client/src/main/resources/assets/game/ui/icon_pickup.png`, `icon_craft_success.png`, `icon_craft_fail.png`, `icon_recipe_unlock.png`, `icon_lore_found.png`, `icon_station_missing.png`, `icon_fuel_missing.png` | 16x16 transparent | tiny readable silhouettes | one per category | feedback log |
| Day/time icons | `client/src/main/resources/assets/game/ui/icon_morning.png`, `icon_noon.png`, `icon_evening.png`, `icon_night.png` | 16x16 transparent | small sun/moon cycle icons | morning, noon, evening, night | world-info HUD |
| Station icons | `client/src/main/resources/assets/game/ui/icon_campfire.png`, `icon_cooking_pot.png`, `icon_forge.png`, `icon_workbench.png`, `icon_storage.png` | 16x16 or 32x32 transparent | simplified silhouettes matching block assets | one per station | crafting filters, interaction HUD |

Existing HUD icons for health, hunger, energy, armor and breath are present in `ui_hud_sheet.png`, but future individual versions should preserve full/half/empty states.

### P1 Asset Briefs: Semantic Replacements

These assets currently render, but many are reused slices that do not communicate progression clearly.

| Asset group | Target path pattern | Size | Needed distinction | Used by |
| --- | --- | --- | --- | --- |
| Iron tools | `textures/item/iron_axe.png`, `iron_pickaxe.png` | 32x32 transparent | silver-gray metal head, darker handle, clearly stronger than copper | tool tier progression |
| Crystal tools | `textures/item/crystal_axe.png`, `crystal_pickaxe.png`, `crystal_knife.png` | 32x32 transparent | cyan/teal crystal edge, subtle glow pixels, still readable at 16px | late-game crystal tier |
| Ancient progression | `ancient_fragment.png`, `ruin_key.png`, `ruin_seal.png`, `lost_charm.png` | 32x32 transparent | distinct silhouettes: shard, key, seal tablet, charm pendant | ruin loot, recipes, journal |
| Glow/slime resources | `glow_crystal.png`, `slime_drop.png`, `glow_mushroom_cap.png` | 32x32 transparent | crystal shard, gel droplet, mushroom cap must not share the same icon | Mushroom Grove, crystal progression |
| Food variants | `berry_jam.png`, `honey.png`, `mushroom_stew.png`, `hearty_stew.png`, `glow_mushroom_stew.png`, `herb_soup.png`, `calming_tea.png`, `spore_tea.png` | 32x32 transparent | bowls, jars and cups need different colors and silhouettes | Cooking UI, recipe list, inventory |
| Clay/container items | `clay_bowl.png`, `clay_pot.png`, `cooking_pot.png`, `water_container.png`, `flower_pot.png` | 32x32 transparent | bowl, pot, kettle/container and planter must be visually separate | Cooking Pot and pottery progression |
| Torch/lantern family | `torch.png`, `resin_torch.png`, `lantern.png`, `ancient_lantern.png` | 32x32 transparent | resin torch warmer/stickier, lantern framed, ancient lantern glowing cyan/gold | lighting, comfort, ruins |
| Textile/wood decor | `cloth.png`, `woven_rug.png`, `garden_fence.png`, `small_table.png`, `wooden_chair.png`, `storage_crate.png`, `workbench.png` | 32x32 transparent | separate material reads: fabric roll, rug, fence, table, chair, crate, bench | Comfort, storage and station UI |

### P1 Asset Briefs: Individual Block Textures

Use 16x16 or 32x32 full-tile PNGs. For cutout props, keep transparent pixels clean and avoid matte halos.

| Block | Target path | Variants/states | Notes |
| --- | --- | --- | --- |
| Grass block | `textures/block/grass_block_top.png`, `grass_block_side.png`, `grass_block_bottom.png` | top/side/bottom | side should show grass lip over dirt |
| Logs | `textures/block/skyroot_log_side.png`, `skyroot_log_top.png`, `pine_log_side.png`, `pine_log_top.png` | side/top | top rings and bark should differ by tree type |
| Core terrain | `stone.png`, `dirt.png`, `sand.png`, `gravel.png`, `clay.png`, `mossy_stone.png`, `mossy_path.png`, `snow.png`, `ice.png`, `water.png` | water may be 4-frame sheet later | cozy readable terrain, no noisy contrast |
| Ores | `coal_ore.png`, `copper_ore.png`, `iron_ore.png`, `glow_crystal_node.png`, `clay_deposit.png` | single tiles | ore color must match item icons |
| Plants | `wild_grass.png`, `sun_bloom.png`, `red_mushroom.png`, `mushroom_cluster.png`, `glow_mushroom.png`, `spore_blossom.png`, `reeds.png`, `berry_bush.png`, `cactus.png` | cutout sprites | alpha edges must be clean, transparent background |
| Stations | `campfire.png`, `campfire_active.png`, `campfire_burned_out.png`, `cooking_pot.png`, `workbench.png`, `forge.png` | active/burned states where listed | in-world blocks plus interaction HUD clarity |
| Comfort/decor | `lantern.png`, `ancient_lantern.png`, `flower_pot.png`, `sleeping_mat.png`, `woven_rug.png`, `small_table.png`, `wooden_chair.png`, `garden_fence.png`, `storage_crate.png` | single or lit variant | support Comfort tooltip and basebuilding readability |
| Ground pickups | `twig_pile.png`, `small_stone.png`, `tree_stump_side.png`, `tree_stump_top.png` | stump side/top | early resources must read from first-person view |

### P2 Spec-Only Asset Briefs

These should be prepared as concept sheets or future-ready files, but do not close code TODOs until a loader/render path exists.

- Entity skins/model sheets: `textures/entity/player.png`, `meadow_grazer.png`, `forest_grazer.png`, `forest_bunny.png`, `snow_hare.png`, `moss_snail.png`, `little_boar.png`, `firefly_swarm.png`, `mire_wisp.png`, `dune_crawler.png`; 64x64 or 128x64 reference sheets, pixel-art palette, parts named for body/head/legs/ears/tail/shell/glow core.
- Particle atlas: `textures/particle/particle_atlas.png`; 16x16 tiles on a transparent sheet for smoke, ember, sparkle, leaf, splash, spore, ore sparkle and comfort sparkle; needs a `ParticleSpriteAtlas` loader before it is used.
- Structure material/reference sheets: small campsite, abandoned cabin, old watchtower, lakeside shack, ruined market stall, frozen shrine, ancient gateway; use 16x16/32x32 block tile references plus a small assembled mockup.
- Lore/journal assets: `ui/icon_journal_new.png`, `ui/icon_lore_note.png`, `ui/icon_map_fragment.png`, `textures/item/ancient_coin.png`; 16x16 UI icons or 32x32 item icons.

### Recommended Next Priorities

1. Add the five missing item icon mappings/assets: `feathers`, `stick`, `snow`, `sleeping_mat`, `tree_stump`.
2. Replace reused progression icons for iron/crystal tools and ruin items.
3. Deliver the UI component state set: slots, buttons, tabs, tooltip, progress bars and scrollbars.
4. Convert core terrain and station blocks from large sheet slices to 16x16 or 32x32 individual block textures to reduce atlas tile size.
5. Prepare entity and particle art as spec-only batches after deciding whether to load texture skins/particle atlas in code.

## Highest Priority

- Block textures, 16x16 or 32x32 PNG: stone, dirt, grass top/side, sand, gravel, clay, mossy stone, snow, ice, water, log side/top, pine log side/top, leaves, pine leaves, cactus, torch, coal ore, iron ore, copper ore.
- Block textures, 16x16 or 32x32 PNG: skyroot planks, village road gravel, well stone, market-stall canopy, door-like plank detail, window/glass-like tile.
- Item icons, 32x32 PNG with transparent background: stone, dirt, torch, coal, raw iron, raw copper, stick, apple, berries, fiber, stone pickaxe, stone axe, stone shovel, stone sword, mushroom, cactus.
- UI sprites, PNG: hotbar slot, selected hotbar slot, heart full/empty, hunger full/empty, armor full/empty, breath bubble full/empty, crosshair, chat panel, button panel.
- Entity textures or simple skins: player placeholder, meadow grazer, forest grazer, snow hare, mire wisp, dune crawler.
- Launcher assets: 512x512 app icon, 1280x720 launcher background, small logo/title treatment.

## World/Structure Assets

- Village material variants: roof tile, plank wall, road gravel, well stone, window/glass-like tile, door icon, market table props, crate/barrel texture.
- Plant sprites/textures: wild grass, sun bloom, red mushroom, small bush, reed.
- Structure reference sheets: small house, campsite, watchtower, desert well, compact village market stall.
- Sky/fog reference palette: clear day, dusk, cold biome, swamp biome.

## Audio Later

- Footsteps: grass, stone, sand, snow, water.
- Block sounds: break/place for stone, dirt, wood, leaves, sand.
- UI sounds: click, chat open, inventory move.
