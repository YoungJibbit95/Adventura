# Adventura - Alpha Gameplay Contracts

Stand: 2026-05-01

Diese Datei ist der Design-/Code-Vertrag fuer die erste Alpha-Progression. Die maschinenlesbare Basis liegt in:

- `common/src/main/java/dev/voxelgame/common/gameplay/AlphaMilestones.java`
- `common/src/main/java/dev/voxelgame/common/gameplay/CozyLifeProgression.java`
- `common/src/main/java/dev/voxelgame/common/gameplay/CreatureFriendshipRules.java`
- `common/src/main/java/dev/voxelgame/common/gameplay/JournalProgression.java`
- `common/src/main/java/dev/voxelgame/common/gameplay/StationProgression.java`
- `common/src/main/java/dev/voxelgame/common/gameplay/status/StatusEffectSystem.java`
- `common/src/main/java/dev/voxelgame/common/gameplay/status/StatusEffectState.java`
- `common/src/main/java/dev/voxelgame/common/content/ContentTagRegistry.java`
- `common/src/main/java/dev/voxelgame/common/content/AlphaItemDesigns.java`

## Alpha-Milestone-Kette

| Order | Milestone | Trigger | Kern-Content | UI/HUD Feedback | Save/Server Contract | Smoke |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | Spawn secured | Safe spawn near campsite/resources | Cozy Meadow, Campsite, Campfire, Storage | short found-camp hint | server/worldgen owns safe spawn and initial chunk stream | Spawn baseline |
| 2 | First supply | first starter pickup | Twig, Pebble, Fiber, Berries, Herbs, Mushroom | first-supply toast | server pickup/block-interact and inventory snapshot | Spawn baseline |
| 3 | First food | collect/eat weak food | Berries, Mushroom, Herbs | hunger/food feedback | server eat/drink action clamps stats | Spawn baseline |
| 4 | First recipe | enough starter supplies | Twig, Pebble, Fiber | first recipe unlock | recipe availability from canonical item keys | Spawn baseline |
| 5 | First tool | craft stone tool | Stone Knife/Axe/Pickaxe | first-tool toast, tool tooltip | server craft, durability item state | Spawn baseline |
| 6 | Campfire crafted | craft campfire | Stone, Twig, Campfire | place-before-night hint | server craft plus later place validation | Night/Campfire smoke |
| 7 | Campfire lit | fuel/activate campfire | Twig/Stick/Dry Grass/Charcoal | warmth/light/comfort hint | server fuel consume and block entity state | Night/Campfire smoke |
| 8 | Storage ready | craft/open crate | Storage Crate, Planks, Fiber | storage interaction HUD | revisioned storage block entity | Storage persistence smoke |
| 9 | Workbench ready | build/use workbench | Resin, Bark Strip, Tool Handle | missing-station craft feedback | CraftingStationRules server validation | Pine Forest smoke |
| 10 | First comfort | reach visible comfort source set | Campfire, Storage, Mat, Lantern, Rug | comfort meter/source hint | server/player stats consume ComfortRules | Spawn baseline |
| 11 | Cooking pot ready | reach clay/reeds/copper path | Clay, Bowl, Pot, Water Container, Copper | water/container requirement | server pot recipe/fuel/output validation | Lakeside + Cooking smoke |
| 12 | Forge ready | build forge and smelt iron | Copper, Charcoal, Raw Iron, Iron Ingot | heat/fuel/output feedback | server forge recipe and no-dupe output | Highlands/Forge smoke |
| 13 | First ruin discovered | enter ruin/grove hook | Small Ruin, Ancient Fragment, Glow Crystal | journal structure discovery | server discovery + idempotent loot marker | Old Ruins smoke |
| 14 | First rare find | obtain rare ruin item | Fragment, Ruin Key/Seal, Lantern, Lost Charm | rare loot/journal feedback | generated loot consumed once and saved | Old Ruins + Loot persistence |

## ContentTagRegistry-Anforderungen

`ContentTagRegistry` klassifiziert `ITEM`, `BLOCK` und `ENTITY` ueber stabile Tags. Der erste Alpha-Schnitt ist codebasiert und darf spaeter in JSON/Codegen ueberfuehrt werden, ohne die Consumer-API zu aendern.

Pflicht-Consumer:

- Gameplay: fuel, food, tools, station gates, comfort, ruin progression.
- UI/HUD: tooltips, locked recipe reasons, missing station/fuel/container feedback.
- Server/Networking: validation, transaction reject reasons, event payload filtering.
- Worldgen/Loot: resource nodes, ore, rare loot and structure markers.
- Physics/Rendering: heavy/floaty/cold/hot/transparent/emissive tags instead of block-ID special cases.

Tag-Gruppen:

- Progression: `starter`, `crafting_ingredient`, `cooking_ingredient`, `ruin_progression`, `rare_loot`, `lore`.
- Use: `food`, `raw_food`, `cooked_food`, `healing`, `fuel`, `flammable`, `placeable`, `storage`, `station`, `comfort_source`.
- Tools/materials: `tool`, `tool_axe`, `tool_pickaxe`, `tool_shovel`, `tool_knife`, `wood`, `stone`, `clay`, `metal`, `copper`, `iron`, `crystal`, `ancient`, `textile`, `pottery`.
- World/engine: `resource_node`, `ore`, `plant`, `light_source`, `emissive`, `transparent`, `cold`, `hot`, `floaty`, `heavy`.
- Entities/actions: `ambient`, `cozy`, `friendly`, `creature_resource`, `no_kill`, `night_visible`, `adventure_danger`, `projectile`, `ranged`, `ammo`.

Validation:

- Unknown item/block keys fail while building tags.
- Item and block aliases resolve to canonical tag data.
- Tests verify milestone-required items have tags and tagged item/block keys exist.

## Item-Design-Daten

`AlphaItemDesigns` deckt den kritischen Pfad von Spawn bis erstem Ruin ab. Jede Design-Zeile enthaelt:

- role
- biome/entity/structure source
- primary use
- unlock
- UI feedback
- save/network contract
- UI icon key
- loot table references
- stack size, food/heal values, tool tier, durability
- tags from `ContentTagRegistry`

Neue Alpha-Items duerfen erst als "fertig" gelten, wenn sie in `Items`, Recipes/Loot/Worldgen oder einer anderen Quelle, `ContentTagRegistry`, `AlphaItemDesigns`, UI-Feedback und einem fokussierten Test auftauchen.

## Station Progression

Die maschinenlesbare Station-Kette liegt in `common/src/main/java/dev/voxelgame/common/gameplay/StationProgression.java`.

| Order | Station | Crafting Role | Unlock | BlockEntity | UI Contract | Server/Save Contract |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | Inventory | Starter tools, campfire, storage and simple decor | Spawn resources | none | Inventory/Crafting tab with starter filters | `CraftRequest` without station; milestone state in PlayerSave |
| 2 | Campfire | Safety, fuel, early cooking, pottery firing and copper smelting | Campfire + starter fuel | `voxel:campfire` active/fuel/cook state | Input/Fuel/Output with burn/cook progress | server owns fuel consume, timing and output; state should persist when station state expands |
| 3 | Workbench | Stronger tools, forge setup, textiles and ancient restoration | Pine resin/bark/tool handle | `voxel:workbench`, currently mostly stateless | station-present recipe filter and component source hints | `CraftRequest` at station validates radius, recipe and output |
| 4 | Cooking Pot | Soups, stews, tea, jam and utility food | Lakeside clay/reeds plus copper | `voxel:cooking_pot` input/fuel/output/cook state | water/container requirements and recipe options | `CookRequest` validates station, slots, containers, timing and output |
| 5 | Forge | Iron, ruin seal and crystal tools | Highlands/old ruins metal path | `voxel:forge` heat/fuel/input/output state | ore/fuel/output/heat screen | server validates heat/fuel and no-dupe smelt/bind output |
| 6 | Ancient Altar | Future sealed ruins and ancient upgrades | Ruin Key/Seal, Fragments, Glow Crystal | future idempotent structure-linked state | discovery/restoration screen | one-shot offerings, journal/lore events and generated-structure save state |

P9.3 acceptance rules:

- Every default recipe station must have a `StationProgression` contract.
- Implemented stations with blocks must satisfy `CraftingStationRules.accepts(...)`.
- Station definitions must state BlockEntity, UI, save, server transaction, feedback and loot/unlock contracts.

## Journal, Lore und Goals

Die maschinenlesbare Journal-/Lore-/Goal-Struktur liegt in `common/src/main/java/dev/voxelgame/common/gameplay/JournalProgression.java`.

Contract-Scope:

- Journal entries haben `key`, `kind`, `discoveryEventKeys`, referenzierte Item-/Block-/Biome-/Structure-/Entity-/Recipe-Keys, Milestone-Bezug, `persistenceKey`, UI-Feedback und Server-Event-Contract.
- Abgedeckte Entry-Typen: Biome Notes, Creature Notes, Structure Notes, Recipe History, Lore Pages und Map Fragments.
- Ruinenprogression ist als Lore/rare-find Pfad modelliert: Ancient Fragment, Ruin Key, Ruin Seal, Lost Charm und Ancient Lantern.
- Goals bleiben leichtgewichtig: sie gruppieren Alpha-Milestones und Journal Entries, ohne eigene MMO-Questlogik oder clientseitige Autoritaet.

Owner-Contracts:

- UI konsumiert `JournalProgression.defaultEntries()` und `defaultGoals()` fuer Tabs, neue Eintraege, Lore, Recipe History, Collectibles und Map-Fragments.
- Server/Save persistiert Entry-/Goal-/Milestone-Keys idempotent pro Player und emittiert Discovery/Recipe/Loot Events nur nach autoritativer Welt-, Inventar- oder Station-State-Aenderung.
- Worldgen/Loot nutzt Structure-/Loot-Marker, um Journal-Events fuer Campsites, Ruinen, Mushroom Circles und rare Finds nur einmal auszugeben.

## Cozy-Life und Creature-Design

Die maschinenlesbare Creature-Struktur liegt in `common/src/main/java/dev/voxelgame/common/gameplay/CozyLifeProgression.java`; harte Feeding-/Friendship-Grenzen liegen in `CreatureFriendshipRules`.

Contract-Scope:

- Jede Alpha-Creature hat `entityKey`, `disposition`, Design-Rollen, Biome, Lieblingsitems, Hint-Ziele, Ressourcen, Comfort-Beitrag, Friendship-Schritte und klare Feeding-/Interaction-/Animation-/Network-/Save-/UI-Vertraege.
- Abgedeckte Rollen: Atmosphaere, Hinweisgeber, Ressource, Base-Comfort, Friendship und seltene Gefahr.
- Feeding/Friendship bleibt klein: 4 Block Reach, mindestens 180 Sekunden Cooldown, maximal drei akzeptierte Feedings pro Tag, maximal drei Trust-Schritte und keine Zucht-/Farm-Pflicht im Alpha-Schnitt.
- Friedliche Tiere sollen Beobachtung, Hinweise, Comfort oder sanfte Ressourcen liefern; Combat-/Danger-Rollen bleiben selten, telegrafiert und ausserhalb des Cozy-Kerns.

Core Alpha Creatures:

| Entity | Disposition | Rollen | Feeding/Friendship | Alpha-Vertrag |
| --- | --- | --- | --- | --- |
| `voxel:cozy_sheep` | Friendly | Atmosphere, Friendship, Base Comfort, Resource | berries/herbs, two trust steps | peaceful comfort/resource hooks, no kill requirement |
| `voxel:forest_bunny` | Skittish | Atmosphere, Hint Giver, Friendship | berries/herbs, one trust step | herb hints and flee/readability, no drops |
| `voxel:firefly_swarm` | Ambient Swarm | Atmosphere, Hint Giver, Base Comfort | not feedable | night readability, glow hints and one-shot discovery |
| `voxel:moss_snail` | Friendly | Atmosphere, Resource, Friendship | mushrooms, two trust steps | peaceful resource hook; deterministic drops stay inside resource contract |
| `voxel:little_boar` | Neutral | Atmosphere, Hint Giver, Resource, Rare Danger | mushroom lure, one calm step | neutral mushroom hint with telegraphed shove/startle risk |

Owner-Contracts:

- UI nutzt `CozyLifeProgression.defaultCreatures()` fuer creature notes, feeding hints, comfort source copy and future friendship surfaces.
- Server validiert feeding, trust, calm/lure cooldowns and any resource interaction authoritatively; clients only request intent.
- Networking exposes creature state through existing/future entity snapshots: idle, graze, flee, wander, agitated, warning and optional trust/cooldown buckets.
- Save persists only touched per-player trust/cooldown facts or structure-linked encounter markers, not ambient population churn.

## Status Effects

Die maschinenlesbare Status-Effect-Basis liegt in `common.gameplay.status`.

Contract-Scope:

- `StatusEffectDefinition` beschreibt Dauer, maximale Dauer, Tick-Intervall, maximale Intensitaet, Stack-Regel, Sichtbarkeit, Tick-Effekt und Modifier.
- `StatusEffectSystem` ist die schmale Facade fuer Server-/Physics-Callsites; `StatusEffectState` ist der reine Common-State: apply, remove, tick, kombinierte Modifier und `saveStates()`.
- `StatusEffectSaveState` persistiert `effectKey`, `remainingSeconds`, `intensity` und `tickProgressSeconds`.
- Modifier sind reine Multiplikatoren fuer Movement, Jump, Stamina-Regen, Hunger-Drain und Health-Regen.

Core Alpha Effects:

| Effect | Rolle | Stack | Tick | Modifier-Vertrag |
| --- | --- | --- | --- | --- |
| `voxel:burning` | Hot/Hazard damage | intensity refresh | health damage each second | no movement bonus |
| `voxel:chilled` | Cold biome/hazard pressure | duration extend | none | slower movement/jump/stamina |
| `voxel:wet` | Water/weather bridge to cold | duration refresh | none | light movement/stamina dampening |
| `voxel:rested` | Sleep reward | duration extend | none | better stamina/health, lower hunger drain |
| `voxel:cozy` | Base comfort reward | duration refresh | none | better stamina/health, lower hunger drain |
| `voxel:poison` | Future adventure danger | intensity refresh | health damage every two seconds | slight movement/stamina pressure |

Owner-Contracts:

- Server applies and ticks effects authoritatively from Hazards, Biomes, Sleep and Comfort.
- Physics consumes only `StatusEffectModifiers`, never effect-specific block or biome switches.
- Networking/Save persists `StatusEffectSaveState` and emits applied/refreshed/expired feedback through GameplayEvents once packet semantics are extended.
- HUD explains active effects from stable `effectKey`s and never invents success/failure client-side.
