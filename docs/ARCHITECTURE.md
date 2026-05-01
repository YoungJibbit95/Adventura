# Architecture

## Runtime Shape

The server is authoritative. It owns world generation, chunk mutation, entities, inventory state and simulation ticks. Clients send intent packets and receive chunk data, block updates and entity snapshots.

The launcher is a thin desktop entrypoint around the existing client and server mains. It does not own game logic; it just collects seed, render distance, preview radius, host, port and username, then starts the selected runtime path.

## Tick Model

- Server: fixed 20 TPS.
- Client: variable render loop with future interpolation against server snapshots.
- Worldgen and persistence are deterministic by world seed.

## Chunk Model

- Chunk footprint: 16x16 blocks in X/Z.
- Vertical storage: 16x16x16 sections.
- Default dimension: Y -64 through 319.
- Light storage: separate sky light and block light nibble values stored as bytes for clarity in V1.

## Networking

Packet payloads live in `common` and are transport-agnostic. Netty codecs in `server` adapt those packets to length-prefixed TCP frames. Clients send gameplay intents such as movement, block actions, craft requests and chat messages; the server answers with chunk data, accepted block updates, entity snapshots, chat broadcasts and authoritative inventory snapshots. The auth boundary is an interface so OAuth/Steam can replace the current whitelist/dev provider later.

## Rendering

The first renderer target is conservative OpenGL via LWJGL. Chunk meshing starts with visible-face mesh generation and is structured so greedy meshing, transparency sorting and shader passes can replace the simple path without changing world storage.

The client keeps opaque and transparent GPU meshes per loaded chunk, rebuilds dirty chunks after block or chunk updates, and culls meshes by horizontal render distance plus camera frustum before drawing. Empty chunk sections are skipped during meshing, empty transparent meshes are not uploaded, and runtime mesh build budget spreads chunk rebuilds across frames. Runtime render settings control field of view, render distance, fog, vertex ambient occlusion, directional soft-shadow shading, transparent water and VSync. The current shader pass adds block tinting, fog, vertex light/AO, soft side shading, animated water displacement and simple color shaping. If individual block PNGs exist in `assets/game/textures/block/`, the client builds a block atlas and samples it in the chunk shader. Missing individual files fall back to known slices from the bundled game sprite sheets, and finally to shader colors.

## Streaming

Offline preview generation loads chunks around the camera as it moves. Online clients send periodic movement snapshots; the authoritative server streams each connection's surrounding chunks once and later sends block updates for accepted world mutations.

## World Content

The Overworld generator combines continent, erosion, ridge, river and detail noise. Biomes choose surface blocks, trees, plants, structures and ambient entity types. Current content covers meadows, cozy meadows, flower fields, pine forests, mushroom groves, lakesides, old ruins, skyroot forests, sun dunes, highlands, frost peaks, mires, caves, ore veins, cacti, small stones, berry bushes, herb patches, tree stumps, boulders, campsites, watchtowers, ruins, desert wells, decorated houses and compact village structures.

## Player Simulation

The client currently owns local-feel movement while the server remains the authority for networked world edits and inventories. Survival mode uses collision, gravity, jumping, stamina-limited sprinting, swimming water movement, food regeneration and fall damage; creative keeps collision but allows flying; spectator has free no-clip movement. HUD stats track health, hunger, stamina, armor and underwater breath so the survival loop has a place to grow without changing the UI shape again.

## Gameplay Content Contracts

Alpha progression is described in common data through `AlphaMilestones`, starting at safe spawn and ending at first ruin/rare find. Station progression is described through `StationProgression`, covering inventory, campfire, workbench, cooking pot, forge and a future ancient altar contract. Journal/lore/goal progression is described through `JournalProgression`, so discovery events, recipe history, ruin lore, map fragments and lightweight goals share persistable keys. Cozy creature design is described through `CozyLifeProgression` and `CreatureFriendshipRules`, covering creature roles, feeding/friendship limits, comfort, hints, resources and server/save/network contracts. Content classification is shared through `ContentTagRegistry` for items, blocks and entities, so UI, server validation, loot, physics and rendering can consume stable tags instead of adding separate key switches. The design-facing table for critical path items lives in `AlphaItemDesigns`; the readable contract is mirrored in `docs/ALPHA_GAMEPLAY_CONTRACTS.md`.

Server-authoritative gameplay intents get a common contract through `common.actions`. `ActionPipeline` validates `ActionRequest`s against `ActionDefinition`s, `ActionTarget`s, cooldown/cost metadata and `ContentTagRegistry` requirements before server execution. The first default definitions cover projectile shoot, block interact and eat; `ServerProjectileShootAction` is the first server-side slice that routes an existing critical action through this contract while preserving authority and inventory side effects.

Server-confirmed feedback uses the common `GameplayEvent` model. Events carry a monotone sequence, stable type and debug key; `GameplayEventBatch` is schema-versioned and bounded, and `GamePacket.GameplayEvents` transports batches as Protocol Version 24 payloads. The first model covers damage, heal, stat warnings, pickup, craft, cook complete, projectile impact, sleep, thunder, journal entry, recipe unlock and structure discovery. The client has an initial consumer hook that maps events to FeedbackLog, AudioCue and small UI animation signals; deeper HUD, journal and particle consumers remain separate client work.

Status effects are modeled as pure common gameplay state in `common.gameplay.status`. `StatusEffectSystem` is the narrow facade for server and physics call sites. `StatusEffectDefinition` describes duration, stack rule, tick interval, client visibility, tick effects and stat/physics modifiers; `StatusEffectState` applies, ticks, combines modifiers and exposes stable `StatusEffectSaveState` rows for persistence. The first definitions cover burning, chilled, wet, rested, cozy and poison. Server application, save ownership and status-specific GameplayEvents remain server/networking work.

## Client UI

The client has main, pause, settings, crafting/inventory and chat states plus the separate launcher app. Chat opens on `T`; slash-prefixed commands are handled client-side for debug, gamemode and runtime settings, while normal messages can be sent through the existing network chat packet. The HUD includes crosshair, tabbed crafting/inventory surfaces, improved hotbar selection, stack counts, durability bars, selected-item tooltips, health, hunger, stamina, armor, breath, mode and optional debug information. UI rendering now has a separate sprite pass for item icons, so hotbar/crafting/inventory can render sheet-based icons while the bitmap text and panels stay in the lightweight immediate UI layer. Right-click interactions now use a separate block-interact packet for harvestable blocks, giving the server a path toward BlockEntities and Workstations without overloading place/break actions.

## Feedback Hooks

The client has a small no-op `GameAudio` hook with named cues for inventory clicks, block break/place, crafting, eating, footsteps, campfires and ambience. It intentionally does not bind to an audio backend yet, but gameplay code can now fire stable cue names when real sound loading is added.
