# Voxel Survival

A Java/LWJGL voxel survival game prototype with a client/server architecture from day one.

## Prerequisites

- JDK 21
- Gradle 9.3.0 or the project wrapper

## Run

```sh
./gradlew buildGame
./gradlew runLauncher
./gradlew runClient
./gradlew runSingleplayer
./gradlew runServer
./gradlew joinLocal
```

`runLauncher` opens the Swing launcher with seed, render distance, preview radius, host, port and username fields. Use `runServer` in one terminal and `joinLocal` in another terminal for a local multiplayer smoke test. The lower-level module commands still work when custom args are needed:

```sh
./gradlew :server:run --args="--port 25565 --seed 1337"
./gradlew :client:run --args="--auto-singleplayer --preview-radius 4 --render-distance 8"
./gradlew :client:run --args="--auto-join --connect 127.0.0.1 --port 25565 --username Player --render-distance 8"
```

The client opens on the main menu. Choose `Singleplayer` for an offline generated world or `Join Server` to use the `--connect` target. If no host is provided, `Join Server` uses `127.0.0.1`.

Useful client args:

- `--seed N`: deterministic world seed used for offline generation.
- `--preview-radius N`: offline chunk generation radius around the camera.
- `--render-distance N`: client-side distance/frustum culling distance in chunks.
- `--connect HOST --port PORT`: server target used by the main menu's `Join Server` button.
- `--auto-singleplayer`: skip the main menu and start an offline world immediately.
- `--auto-join`: skip the main menu and immediately join the configured server.

On macOS, LWJGL may need the JVM argument `-XstartOnFirstThread` for the client run configuration.

Client controls in the current preview:

- Mouse: look around
- `WASD`: move
- `Space`: jump in survival, fly up in creative/spectator
- `Left Ctrl`: fly down in creative/spectator
- `Left Shift`: faster movement
- `E`: open/close crafting and inventory screen
- `O`: open settings while in game
- `R`: return to spawn
- `F1`: toggle HUD
- `F3`: toggle debug overlay
- `F4`: cycle survival/creative/spectator
- Left mouse: break block
- Right mouse: place selected block
- `1`-`9`: select hotbar slot
- `T`: open chat
- `/`: open chat with command prefix
- `Esc`: pause/resume while in game; crafting buttons are in the pause menu

Chat commands currently include `/help`, `/keys`, `/seed`, `/pos`, `/tp x y z`, `/spawn`, `/gamemode survival|creative|spectator`, `/renderdistance n`, `/preview n`, `/meshbudget n`, `/fov n`, `/fog`, `/ao`, `/shadows`, `/hud`, `/debug`, `/water`, `/settings`, `/clear` and `/say text`.

The main menu and pause menu include a settings screen for render distance, offline preview radius, mesh build budget, field of view, mouse sensitivity, fog, ambient occlusion, soft shadows, transparent water, HUD, chat, debug overlay and VSync. If performance stutters while chunks load, lower `Mesh Budget`, `Render Distance`, `World Preview`, or disable `Ambient AO`/`Water`.

The current survival loop has inventory stacks, starter items, richer cozy resources, expanded crafting, placeable decor, food, hunger, stamina, regeneration, tool durability, block drops, placement consumption, survival/creative/spectator movement modes, collision, gravity, jumping, swimming water physics, underwater breath and fall damage. Offline play applies those locally; online play sends block and crafting intents to the authoritative server, which validates inventory, applies accepted world edits, and returns full inventory snapshots.

Current world content includes expanded terrain shaping, rivers, caves, ores, cozy meadows, flower fields, pine forests, mushroom groves, lakesides, old ruins, dunes, highlands, frost peaks, mires, trees, pines, mushrooms, berry bushes, herb patches, small stones, tree stumps, cacti, boulders, campsites, watchtowers, houses, desert wells, compact villages with a market stall and ambient entities such as cozy sheep, bunnies, snails, fireflies and little boars. Rendering has culling, transparent water, fog, vertex AO, soft directional shading, animated water and simple animated entity rendering.

Block textures can be dropped as individual PNG files into `client/src/main/resources/assets/game/textures/block/`. The renderer builds an atlas automatically and keeps the old shader colors as fallbacks for missing files. The bundled sprite sheets in `client/src/main/resources/assets/game/` are also wired as fallback sources, so `bloecke_blocks.png`, `natursachen_nature.png`, `deko_decor.png` and `misc_wasser_ui_paletten.png` already feed the chunk shader. Individual files still override sheet slices when both exist.

Inventory and HUD item sprites are rendered from the new `generated_*` sheets first, then from `items_inventory.png` plus fallback slices from the block/nature sheets. Additional icons can be mapped in `GameSprites` without changing inventory or hotbar UI code.

## Modules

- `common`: registries, world/chunk data, packets, generation, lighting, entity snapshots and ambient spawn rules.
- `server`: authoritative tick loop, whitelist auth hook, Netty transport, world streaming.
- `client`: LWJGL window and render/client foundation.
- `launcher`: Swing launcher for singleplayer, local server and multiplayer startup.
- `tools`: data and asset-pack utility entrypoint.

## Asset Needs

See [docs/ASSET_REQUESTS.md](docs/ASSET_REQUESTS.md) for the current short asset generation list.
# AdventureCraft
