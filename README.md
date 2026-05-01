<div align="center">

# Adventura

### Cozy voxel survival-adventure prototype in Java, LWJGL and Netty

Build, gather, craft, explore biomes, host a server, and slowly turn a rough world into a warm little base.

<br>

![Java](https://img.shields.io/badge/Java-21-f89820?style=for-the-badge&logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.3.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![LWJGL](https://img.shields.io/badge/LWJGL-3.4.1-c32222?style=for-the-badge)
![Netty](https://img.shields.io/badge/Netty-4.1-4f8cc9?style=for-the-badge)
![JUnit](https://img.shields.io/badge/JUnit-5-25A162?style=for-the-badge&logo=junit5&logoColor=white)
![Status](https://img.shields.io/badge/status-prototype-8b5cf6?style=for-the-badge)

![Last Commit](https://img.shields.io/github/last-commit/YoungJibbit95/Adventura?style=flat-square)
![Repo Size](https://img.shields.io/github/repo-size/YoungJibbit95/Adventura?style=flat-square)
![Issues](https://img.shields.io/github/issues/YoungJibbit95/Adventura?style=flat-square)
![Visitors](https://komarev.com/ghpvc/?username=YoungJibbit95-Adventura&label=repo%20views&color=8b5cf6&style=flat-square)

</div>

---

> [!NOTE]
> Adventura is an early prototype. Expect fast-moving systems, unfinished gameplay loops and dev-friendly debug tools.

## Table Of Contents

- [What It Is](#what-it-is)
- [Quick Start](#quick-start)
- [Run Modes](#run-modes)
- [Electron Launcher](#electron-launcher)
- [Controls](#controls)
- [Chat Commands](#chat-commands)
- [Project Map](#project-map)
- [Current Features](#current-features)
- [Configuration](#configuration)
- [Assets](#assets)
- [Checks](#checks)
- [Roadmap](#roadmap)
- [Docs](#docs)

## What It Is

**Adventura** is a cozy voxel survival-adventure prototype with a server-authoritative architecture from day one. The project is built around deterministic world generation, a standalone LWJGL client, a Netty-based dedicated server, and a launcher that makes singleplayer and local multiplayer easier to start.

The current direction is a gentler survival loop: collect small resources, craft tools, cook food, discover biomes, build a comfortable base, and uncover ruins and lore over time.

## Quick Start

Requirements:

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | 21 | The Gradle toolchain and source target expect Java 21. |
| Gradle | Wrapper included | Prefer `./gradlew` or `gradlew.bat` over a system install. |
| Node.js/npm | Node 18+ for dev, Node 22+ recommended for packaging | Needed only when running/building the Electron launcher from source. |
| GPU/OpenGL | LWJGL-capable | Needed for the client renderer. |

```sh
java -version
javac -version
./gradlew --version
```

If Java is installed outside the system PATH, export it before running Gradle:

```sh
export JAVA_HOME="$HOME/.local/share/adventura-jdk/jdk-21.0.11+10"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
javac -version
```

Recommended first launch:

```sh
cd launcher-electron
npm install
cd ..
./gradlew runLauncher
```

Fast singleplayer launch:

```sh
./gradlew runSingleplayer
```

Windows:

```powershell
cd launcher-electron
npm install
cd ..
.\gradlew.bat runLauncher
```

<details>
<summary>JDK 21 setup notes</summary>

Debian/Ubuntu:

```sh
sudo apt update
sudo apt install openjdk-21-jdk
export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
export PATH="$JAVA_HOME/bin:$PATH"
```

macOS with Homebrew:

```sh
brew install openjdk@21
export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
export PATH="$(brew --prefix openjdk@21)/bin:$PATH"
```

Windows with winget:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

Then set `JAVA_HOME` to the JDK folder and add `%JAVA_HOME%\bin` to `Path`.

</details>

<details>
<summary>JDK and Gradle troubleshooting</summary>

- `java: command not found`: install JDK 21, then set `JAVA_HOME` and prepend `$JAVA_HOME/bin` to `PATH`.
- `javac: command not found`: a JRE is not enough; install a full JDK 21.
- Wrong Java version: run `java -version` and make sure it reports `21.x`; then restart the shell or terminal used by the IDE.
- Gradle wrapper problems: run `./gradlew --version` first. On Linux/macOS, use `chmod +x gradlew` if the wrapper is not executable.
- macOS LWJGL startup issues: run the launcher/client through the Gradle tasks so the project JVM args are applied; if needed, add `-XstartOnFirstThread` to the client run configuration.

</details>

## Run Modes

| Command | What it does |
| --- | --- |
| `./gradlew setupLauncher` | Installs Electron launcher dependencies with npm. Run this once after checkout or dependency changes. |
| `./gradlew runLauncher` | Opens the React/Electron launcher from the workspace. |
| `./gradlew runElectronLauncher` | Alias for `runLauncher`. |
| `./gradlew packageLauncher` | Builds the Electron desktop package and bundles the client/server launch scripts. |
| `cd launcher-electron && npm run dev` | Direct npm variant of the workspace launcher command. |
| `cd launcher-electron && npm run dist` | Direct npm variant of the desktop package build. |
| `./gradlew runSingleplayer` | Starts the client directly in a generated singleplayer world. |
| `./gradlew runClient` | Starts the client and opens the main menu. |
| `./gradlew runServer` | Starts a dedicated server on port `25565` with seed `1337`. |
| `./gradlew joinLocal` | Starts the client and joins `127.0.0.1:25565` as `Player`. |
| `./gradlew profileSingleplayerJfr` | Records a short singleplayer JFR profile under `build/reports/jfr/`. |
| `./gradlew profileJoinLocalJfr` | Records a client join-local JFR profile; start `runServer` first. |
| `./gradlew profileLongExploreJfr` | Records a higher-distance long-explore JFR profile. |
| `./gradlew buildGame` | Builds all game modules and runs their checks. |
| `./gradlew test` | Runs the JUnit test suite. |

Useful examples:

```sh
./gradlew runClient --args="--auto-singleplayer --seed 4242 --preview-radius 4 --render-distance 10"
./gradlew runServer --args="--port 25565 --seed 1337 --whitelist Player,Friend"
./gradlew runClient --args="--auto-join --connect 127.0.0.1 --port 25565 --username Player"
```

## Electron Launcher

The launcher lives in [`launcher-electron/`](launcher-electron/). It uses React for the UI and Electron for the desktop shell. The old Swing launcher module has been removed; this is now the supported launcher path.

The launcher saves settings to:

```text
~/.adventura/launcher.properties
```

Development mode:

```sh
./gradlew setupLauncher
./gradlew runLauncher
```

Direct npm mode:

```sh
cd launcher-electron
npm install
npm run dev
```

In the Electron window:

| Action | What it launches |
| --- | --- |
| `Singleplayer` | Starts the client with `--auto-singleplayer`, the configured seed, preview radius and render distance. |
| `Server beitreten` | Starts the client with `--auto-join`, host, port, username and render settings. |
| `Server starten` | Starts the local dedicated server with the configured port, seed and whitelist user. |
| `Auto Server` enabled | Starts the local server before joining multiplayer. |

In development mode those buttons use the current local workspace. The launcher checks whether the generated Gradle distributions are still fresh; if the client, server, common module or Gradle build files changed, it runs `installDist` first. If nothing changed, it skips Gradle and starts the existing scripts under `client/build/install/client/bin/` and `server/build/install/server/bin/` directly. In a packaged desktop build it starts the bundled scripts under `resources/game/`.

The `Terminal` tab shows live launcher, Gradle, client and server output. Use `Preflight` there to check Java, Gradle and the generated game starters before launching.

Game source/update model:

| Mode | Where the game comes from | Launch behavior |
| --- | --- | --- |
| Workspace launcher | Local checkout on disk | Builds only when local sources are newer than the generated launcher scripts. |
| Packaged AppImage/installer | Snapshot bundled during `npm run dist` / `./gradlew packageLauncher` | Starts the bundled client/server scripts directly. |
| GitHub latest | Not automatic by default | Pull or download updates first, then rebuild/package. |

Building the game from GitHub on every launcher start is possible, but it is intentionally not the default: it would make startup much slower, require a network connection, and could pull an unstable commit. A safer update flow is to update the workspace with Git, then open the launcher; the launcher will detect changed sources and rebuild only what is needed.

Linux notes:

- `npm run dev` starts Electron through `scripts/start-electron.cjs`, which removes inherited Electron/VS Code environment flags, disables the Linux sandbox for local development and lets the app disable GPU acceleration when needed.
- `bash: fastfetch: Kommando nicht gefunden` and `bash: starship: Kommando nicht gefunden` come from the local shell startup config, not from Adventura.
- `npm warn Unknown global config "tmp"` comes from a global npm config entry. It is only a warning; remove it with `npm config delete tmp --global` if it becomes annoying.

Preview only:

```sh
cd launcher-electron
npm run build
npm run preview
```

The browser preview is useful for checking layout, but only the Electron window can start the game client.

Build a desktop package with the game client bundled:

```sh
./gradlew packageLauncher
```

Direct npm variant:

```sh
cd launcher-electron
npm run dist
```

`npm run dist` runs the React build, builds the Java game distributions with `../gradlew :client:installDist :server:installDist`, then packages everything with `electron-builder`.

Release outputs are written to `launcher-electron/release/`:

| Platform | Output |
| --- | --- |
| Linux | `Adventura Launcher-<version>-x86_64.AppImage` |
| Windows | `Adventura Launcher-Setup-<version>.exe` when `npm run dist` is run on Windows |
| macOS | `Adventura Launcher-<version>-<arch>.dmg` when `npm run dist` is run on macOS |

Linux install examples:

```sh
chmod +x "launcher-electron/release/Adventura Launcher-0.1.0-x86_64.AppImage"
"launcher-electron/release/Adventura Launcher-0.1.0-x86_64.AppImage"
```

Optional Linux `.deb` build:

```sh
cd launcher-electron
npm run dist:deb
```

```sh
sudo apt install "./release/Adventura Launcher-0.1.0-amd64.deb"
```

If the `.deb` build reports `libcrypt.so.1`, install the distro package that provides the legacy libcrypt compatibility library or use the AppImage build.

For a fast packaging smoke test without creating installers:

```sh
cd launcher-electron
npm run dist:dir
```

Packaged desktop builds include:

```text
resources/game/client/bin/client
resources/game/server/bin/server
```

That means the installed Electron launcher can start the bundled game client from the desktop app. Java 21 still needs to be installed on the machine because the bundled Gradle application scripts call `java`.

## Controls

| Input | Action |
| --- | --- |
| `W` `A` `S` `D` | Move |
| Mouse | Look around |
| `Space` | Jump, swim up or fly up |
| `Left Shift` | Sprint |
| `Left Ctrl` | Fly down / descend |
| Left mouse | Break or use the targeted block/action |
| Right mouse | Place, interact or use selected item |
| Mouse wheel / `1`-`9` | Select hotbar slot |
| `E` | Crafting and inventory |
| `O` | Settings |
| `T` | Chat |
| `/` | Open command input |
| `R` | Return to spawn |
| `F1` | Toggle HUD |
| `F3` | Toggle debug overlay |
| `F4` | Cycle game mode |
| `Esc` | Pause/menu |

## Chat Commands

Open chat with `/` and use `/help` in-game for the authoritative list.

<details>
<summary>Current command highlights</summary>

| Command | Purpose |
| --- | --- |
| `/help` | Print all available commands. |
| `/keys` | Show keybind hints. |
| `/seed` | Show the current world seed. |
| `/pos` | Show player position. |
| `/tp x y z` | Teleport locally to coordinates. |
| `/spawn` | Return to spawn. |
| `/gamemode survival\|creative\|spectator` | Switch mode. |
| `/preset low\|medium\|high` | Apply render preset. |
| `/renderdistance n` | Change render distance. |
| `/preview n` | Change preview chunk radius. |
| `/meshbudget n` | Change chunk mesh build budget. |
| `/fov n` | Change field of view. |
| `/fog`, `/ao`, `/shadows`, `/bloom`, `/water` | Toggle rendering features. |
| `/hud`, `/debug`, `/debugchunks`, `/debuglight`, `/debugbiome` | Toggle or inspect debug UI. |
| `/settings` | Print current settings. |
| `/say text` | Send chat text or local fallback text. |
| `/clear` | Clear chat log. |

</details>

## Project Map

| Module | Path | Responsibility |
| --- | --- | --- |
| `common` | [`common/`](common/) | Blocks, items, recipes, packets, physics, world data, generation and shared rules. |
| `client` | [`client/`](client/) | LWJGL client, renderer, UI, input, hotbar, audio hooks and client networking. |
| `server` | [`server/`](server/) | Authoritative world state, Netty server, auth, entities, ticking and survival state. |
| `tools` | [`tools/`](tools/) | Asset/reporting utilities. |
| `launcher-electron` | [`launcher-electron/`](launcher-electron/) | React/Electron desktop launcher for singleplayer, multiplayer and local server startup. |

Runtime shape:

```text
Launcher -> ClientMain -> GameClient
         -> GameServerMain -> Netty server -> ServerWorld

Client <-> shared packets in common <-> Server
```

## Current Features

| Area | Current state |
| --- | --- |
| World generation | Deterministic overworld with meadows, pine forests, mushroom groves, lakesides, ruins, highlands, frost peaks, dunes, caves, ores, trees, plants and structures. |
| Survival | Health, hunger, stamina, breath, fall damage, swimming, sprinting and multiple game modes. |
| Gameplay loop | Gathering, block breaking/placing, crafting recipes, inventory/hotbar, item durability, drops, campfires, storage and early comfort hooks. |
| Multiplayer | Dedicated server, whitelist auth, chunk streaming, block updates, entity snapshots, inventory sync and chat packets. |
| Rendering | LWJGL/OpenGL chunk rendering, mesh rebuild budget, fog, AO, soft shadows, bloom toggle, transparent water and debug overlays. |
| UI | Main menu, pause menu, settings, crafting/inventory, compact storage, chat, HUD stats, tooltips and feedback log. |
| Assets | Original sprite sheets plus optional per-block texture overrides. |

## Configuration

Client arguments:

| Argument | Default | Description |
| --- | --- | --- |
| `--auto-singleplayer` | off | Start directly in local singleplayer. |
| `--auto-join` | off | Join a server directly. |
| `--connect <host>` | none | Server host for online mode. |
| `--port <number>` | `25565` | Server port. |
| `--username <name>` | `Player` | Multiplayer display/auth name. |
| `--seed <number>` | `1337` | Singleplayer world seed. |
| `--preview-radius <n>` | `3` | Local preview chunk radius. |
| `--render-distance <n>` | `8` | Render distance in chunks. |

Server arguments:

| Argument | Default | Description |
| --- | --- | --- |
| `--port <number>` | `25565` | Listen port. |
| `--seed <number>` | `1337` | Server world seed. |
| `--whitelist <a,b,c>` | empty | Optional comma-separated usernames. Empty means dev-open auth. |

Runtime settings include render distance, preview radius, FOV, mouse sensitivity, UI scale, fog, AO, soft shadows, bloom, VSync, HUD, debug overlays, chat and transparent water.

## Assets

Game assets live in [`client/src/main/resources/assets/game/`](client/src/main/resources/assets/game/).

| Asset | Used for |
| --- | --- |
| `blocks_tiles_sheet.png` | Terrain and block-like item fallback sprites. |
| `tools_weapons_sheet.png` | Tools, weapons and held-item icons. |
| `nature_food_sheet.png` | Food, plants, fuel and cozy resources. |
| `ores_materials_sheet.png` | Ores, raw materials, ingots and crystals. |
| `ui_hud_sheet.png` | HUD icons, panels, menu surfaces and selected decor icons. |

Individual texture overrides can be added under:

```text
client/src/main/resources/assets/game/textures/block/
client/src/main/resources/assets/game/textures/item/
client/src/main/resources/assets/game/textures/entity/
client/src/main/resources/assets/game/ui/
client/src/main/resources/assets/game/audio/
```

See [`docs/ASSET_LICENSES.md`](docs/ASSET_LICENSES.md) and [`docs/ASSET_REQUESTS.md`](docs/ASSET_REQUESTS.md) before adding new files.

## Checks

Use these as normal local gates before pushing:

```sh
./gradlew test
./gradlew buildGame
```

Focused module checks also work:

```sh
./gradlew :common:test
./gradlew :client:test
./gradlew :server:test
```

## Roadmap

- [x] Multi-module Java project with shared protocol and deterministic world data.
- [x] LWJGL client with world rendering, HUD, inventory, crafting and settings.
- [x] Dedicated Netty server with chunk streaming and authoritative edits.
- [x] Electron launcher with bundled client/server desktop builds.
- [ ] Sharpen the early survival loop and first-night pacing.
- [ ] Expand cooking, forge and station-driven progression.
- [ ] Deepen comfort, furniture and base-building rewards.
- [ ] Add journal, lore, biome discovery and structure progression.
- [ ] Finish copper, iron and crystal tool tiers.
- [ ] Add stronger audio, ambience and visual polish passes.

## Docs

| Document | Description |
| --- | --- |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Runtime shape, rendering, networking and simulation notes. |
| [`docs/GAMEPLAY_TODO_LIST.md`](docs/GAMEPLAY_TODO_LIST.md) | Gameplay loop, progression, biomes, animals and lore roadmap. |
| [`docs/UI_TODO_LIST.md`](docs/UI_TODO_LIST.md) | UI quality, inventory, crafting, cooking, storage and settings plan. |
| [`docs/HUD_TODO_LIST.md`](docs/HUD_TODO_LIST.md) | Survival HUD, comfort display, feedback and debug overlay plan. |
| [`docs/ENGINE_TODO_LIST.md`](docs/ENGINE_TODO_LIST.md) | Engine-level follow-up work. |
| [`docs/WORLD_SMOKE_TESTS.md`](docs/WORLD_SMOKE_TESTS.md) | Manual smoke checks for world features. |
| [`RENDERING_FILES_REFERENCE.md`](RENDERING_FILES_REFERENCE.md) | Rendering-related file reference. |

---

<div align="center">

Made with Java, pixels, chunk meshes and a suspicious amount of comfort planning.

</div>
