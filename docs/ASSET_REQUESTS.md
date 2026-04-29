# Asset Requests

Generate original, non-Minecraft assets with a consistent cozy voxel-survival style. Keep filenames lowercase with underscores and provide a license note for every batch.

Preferred delivery for future replacements: individual PNG files under `client/src/main/resources/assets/game/`. For block textures use `textures/block/<block_name>.png`; add `_top`, `_side` or `_bottom` suffixes only when the faces need different art. The current bundled sprite sheets are supported as fallback sources, so you can also keep adding sheets when that is more convenient.

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
