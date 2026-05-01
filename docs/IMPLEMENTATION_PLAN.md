# Adventura - Global Implementation Plan

Stand: 2026-05-01

## Ziel

Adventura soll von einem starken Prototype in eine belastbare Alpha wachsen: serverautoritativ, testbar, performant, visuell eigenstaendig und mit einem klaren cozy Survival-Adventure-Loop. Dieser Plan ist die globale Arbeitsanweisung fuer alle Agenten. Jede Rolle arbeitet nach diesem Plan plus ihren zugewiesenen TODO-Listen.

## Projektbild

Adventura ist ein Java-21/Gradle-Projekt mit:

- `common`: Blocks, Items, Crafting, World Data, Worldgen, Physics, Gameplay Rules, Packets.
- `client`: LWJGL/OpenGL-Client, Renderer, Input, UI/HUD, Audio-Hooks, Client Networking.
- `server`: autoritativer Netty-Server, ServerWorld, Persistence, Entity Tracker, Player State.
- `tools`: Asset-/Atlas-Reports.
- `launcher-electron`: React/Electron-Launcher.

Der Server bleibt die Gameplay-Autoritaet. Clients senden Intents. Common enthaelt die Regeln, die auf Client und Server gleich sein muessen. Rendering, UI und lokale Preview gehoeren in den Client.

## Wichtigste Schwachstellen

- `GameClient` ist mit ueber 5700 Zeilen zu monolithisch: Loop, Input, Screens, HUD, Chat, Commands, lokale Gameplay-Aktionen und Session-Management sind vermischt.
- `ServerConnectionHandler` ist mit ueber 2000 Zeilen zu breit: Protocol Routing, Auth, Movement, Actions, Inventory, Storage, Cooking, Saves, Interest und Stats muessen schrittweise getrennt werden.
- `OverworldGenerator`, `CraftingRecipes` und mehrere Content-Registries sind noch stark codegetrieben und werden mit mehr Content schwer wartbar.
- Rendering hat eine solide Basis, braucht aber fuer Shadows, Sprite Rendering, Pass-Ausbau, Post/Framebuffer und Draw-Ranges klarere Module.
- Networking/Persistence hatte bisher keinen echten V2-Plan; Protocol Contracts, ActionPipeline, GameplayEventStream, Region Storage, Save Queue und BlockEntity Transactions sind jetzt Pflichtthemen.
- UI/HUD wachsen noch im GameClient und brauchen Screen-/Component-Architektur.
- Physics ist gut getestet, braucht aber Collision Cache, Partial-Shape Impact, Replay Recorder und StatusEffect-Schnittstellen.

## Gemeinsame Arbeitsregeln

- Arbeite so, als waere Adventura eine echte Alpha-Engine, die Geld und Zeit nicht verschwenden darf.
- Lies vor jeder Aufgabe `docs/IMPLEMENTATION_PLAN.md`, `docs/ARCHITECTURE.md` und deine zugewiesenen TODO-Listen.
- Baue keine minimalen Placebo-Fixes, wenn ein stabiler kleiner Architektur-Schnitt noetig ist.
- Vermeide neue Monolithen. Wenn eine Datei stark waechst, extrahiere erst klare Services, Controller, ViewModels oder Contracts.
- Neue serverkritische Logik gehoert nicht nur in den Client.
- Neue Gameplay-Regeln gehoeren nach `common`, Server-Autoritaet nach `server`, Rendering/UI nach `client`.
- Neue Shader-Faelle laufen ueber Material-/Renderdaten, nicht ueber harte Block-IDs.
- Jede relevante Aenderung braucht Test, Smoke-Check, Profiling-Messung oder eine kurze dokumentierte Begruendung.
- Dokumentiere offene Risiken in der passenden TODO-Liste, statt sie im Code zu verstecken.
- Cross-Owner-Aenderungen zuerst ueber kleine Contracts abstimmen: Packet, Action, ViewModel, Tag, RenderMaterial, Event oder Save-Schema.

## Rollen und Ownership

| Rolle | Primaere Listen | Referenzen | Fokus |
| --- | --- | --- | --- |
| Lead Engine Developer | `docs/RENDERING_TODO_LIST.md`, `docs/LIGHTNING_TODO_LIST.md` | `docs/ENGINE_TODO_LIST.md`, `docs/RENDERING_SHADER_UNIFORMS.md` | Shader, Lighting, Shadows, Sprite Rendering, Render Pipeline, GPU/GL, Profiling |
| Lead Game Design Engineer | `docs/GAMEPLAY_TODO_LIST.md`, `docs/WORLDGEN_TODO_LIST.md` | `docs/ASSET_REQUESTS.md`, `docs/WORLD_SMOKE_TESTS.md` | Core Loop, Items, Stations, Biome-Gameplay, Lore, Creature Design |
| Lead UI/UX Frontend Developer | `docs/UI_TODO_LIST.md`, `docs/HUD_TODO_LIST.md` | `docs/ANIMATIONS_TODO_LIST.md`, launcher docs | Screens, Components, HUD, Menus, Journal, UX, Launcher/Game Stil |
| Physics und Engine Worker | `docs/PHYSICS_TODO_LIST.md`, `docs/ENGINE_TODO_LIST.md` | `docs/ANIMATIONS_TODO_LIST.md` | Physics, Collision, Fluids, Replay, Engine-Oberflaechen, Debug |
| Project Manager | `docs/ENGINE_TODO_LIST.md`, `docs/IMPLEMENTATION_PLAN.md` | alle TODO-Listen | Refactor-Plan, Architektur, Bug-Triage, DoD, Test-Gates, Konfliktvermeidung |
| Main Networking Dev | `docs/NETWORKING_AND_PERSITENCE_TODO_LIST.md`, `docs/ENGINE_TODO_LIST.md` | `docs/GAMEPLAY_TODO_LIST.md`, `docs/UI_TODO_LIST.md` | Protocol, Server Actions, Interest, Reconnect, Persistence, BlockEntity Sync |

## Arbeitsreihenfolge

# Phase 0 - Stabilisieren und Arbeitsgrenzen sichern

Owner: Project Manager.

Aufgaben:

- ~~Refactor-Map fuer `GameClient`, `ServerConnectionHandler`, `OverworldGenerator`, `WorldRenderer` und Content-Registries erstellen.~~
  Erledigt: 2026-05-01, siehe `docs/ENGINE_TODO_LIST.md` P13.1/P13.2.
- ~~Pro Monolith Zielpakete und Extraktionsreihenfolge definieren.~~
  Erledigt: 2026-05-01, Zielpakete und Reihenfolge stehen in `docs/ENGINE_TODO_LIST.md` P13.1.
- ~~DoD fuer Agentenarbeit durchsetzen:~~
  - Tests oder Smoke.
  - keine neuen monolithischen Methoden.
  - klare Owner-Schnittstellen.
  - TODO-Status aktualisiert.
  Erledigt: 2026-05-01, Monolith-Extraktions-DoD steht in `docs/ENGINE_TODO_LIST.md` P13.1.
- Bestehende Test-/Build-Gates pruefen und bekannte Runner-Probleme dokumentieren.

Status 2026-05-01:

- 🔴 `ServerConnectionHandler`: Main Networking Dev startet mit `server.protocol`/duennem Packet-Dispatch und Common-`ActionRequest`/`ActionValidationResult` Skeleton, bevor weitere serverkritische Aktionen in den Handler wachsen.
  Fortschritt: 2026-05-01, `common.actions` Skeleton mit `ActionPipeline` ist umgesetzt und getestet; `ServerProjectileShootAction` ist erster Server-Slice, weitere Aktionen/Event-Ergebnisse bleiben offen.
- 🔴 `GameClient`: Lead UI/UX und Physics/Engine Worker schneiden zuerst `ScreenContext`, `ClientInputState`, `HudPresenter` und spaeter `DebugCommandRegistry`, waehrend bestehende Methoden zunaechst delegiert bleiben.
- ~~🔴 Content-Registries: Lead Game Design Engineer plant `common.content`/`ContentTagRegistry` V1 codebasiert; Networking, Physics und Rendering nutzen Tags erst ueber stabile Common-Queries.~~
  Erledigt: 2026-05-01, `ContentTagRegistry` V1 ist als Common-Read-Contract vorhanden und getestet; Folgearbeit ist die Migration von ActionPipeline, Physics, Rendering und Save-Diagnose auf diese Queries.
  Verifikation: `ContentTagRegistryTest` XML/HTML meldet 8 Tests, 0 Failures; Gradle-Task-Race ist in `docs/ENGINE_TODO_LIST.md` P0.1 dokumentiert.
- 🟠 `WorldRenderer`: Lead Engine Developer fuehrt P10.1a mit `RenderPassExecutor`, `RenderStateGuard`, `TerrainRenderer`, `TerrainUploadQueue` und `VisibilityCollector` fort, bevor Shadows/Draw-Ranges/Water-Ausbau groesser werden.
- 🟠 `OverworldGenerator`: Lead Game Design Engineer und Project Manager schneiden P7.1 in `ClimateSampler`, `BiomeResolver`, `HeightmapSampler`, `TerrainFiller`, `FeaturePlanner`, `StructurePlanner`, `SpawnPlanner` und Metrics, mit Seed-/Spawn-Tests vor jedem Move.

Akzeptanz:

- Jeder Agent weiss, welche Dateien er primaer anfassen darf.
- Cross-Owner-Arbeit hat Contract-Schnittstellen.
- Refactors werden nicht als riesige unreviewbare Umbauten gestartet.

---

# Phase 1 - Monolithen in sichere Schnittstellen schneiden

Owner: Project Manager mit allen Leads.

Aufgaben:

- `GameClient` schrittweise entlasten:
  - Screen Controller.
  - HUD Presenter.
  - Command Handler.
  - Input State.
  - Interaction Targeting.
  - Session Lifecycle.
- `ServerConnectionHandler` schrittweise entlasten:
  - protocol routing.
  - action validation.
  - movement validation.
  - interest management.
  - persistence hooks.
  - storage/station transactions.
- `OverworldGenerator` in Pass-Services vorbereiten.
- `WorldRenderer` weiter in Pass-/Upload-/Visibility-Verantwortungen schneiden.

Akzeptanz:

- Verhalten bleibt gleich oder besser.
- Tests sichern die Extraktionen.
- Neue Featurearbeit kann parallel konfliktarmer laufen.

---

# Phase 2 - Rendering, Shader, Lighting und Shadows

Owner: Lead Engine Developer.

Aufgaben:

- Render Pipeline V3 aus `RENDERING_TODO_LIST.md` umsetzen.
- `WorldRenderer` weiter modularisieren:
  - Terrain.
  - Water.
  - Selection.
  - Sprite/Billboard.
  - Upload Queue.
  - Render State.
- Sprite Rendering fuer UI/Items/Particles/Entity-Fallbacks planen und als Atlas-Vertrag bauen.
- Light Jobs und Dirty Regions aus `LIGHTNING_TODO_LIST.md` vorbereiten.
- Shadow-Strategie fuer Alpha entscheiden und prototypisieren, nur mit Preset-Fallback und Debug View.
- Uniform-Doku aktuell halten.

Akzeptanz:

- Neue Shader-/Render-Features sind datengetrieben.
- Low/Medium/High Presets kontrollieren Kosten.
- Render-Stats zeigen Pass-, Mesh-, Upload- und Shader-Kosten.

---

# Phase 3 - Networking, Server Actions und Persistence V2

Owner: Main Networking Dev.

Status 2026-05-01:

- Protocol Contract/Golden-Codec-Basis steht.
- ActionPipeline ist als Common-Contract vorhanden; Projectile-Shoot nutzt den ersten Server-Action-Slice.
- GameplayEventStream hat Common-Modell, Packet/Codec-Contract, ersten Client-Feedback-Hook und erste Server-Emission fuer ProjectileImpact; Interest-Filter und vollstaendige HUD-/Journal-Consumer bleiben offen.

Aufgaben:

- Protocol Contracts und Golden-Codec-Tests fuer Packet-Aenderungen.
- ActionPipeline als Common/Server-Schnitt definieren.
- GameplayEventStream fuer serverbestaetigte UI/Audio/Particle Events bauen.
- Interest Management V2:
  - chunks.
  - entities.
  - block updates.
  - events.
  - open block entities.
- Region Storage und Async Save Queue planen und inkrementell einfuehren.
- BlockEntity Transactions fuer Storage, Campfire, CookingPot und Forge.
- Reconnect/Auth/Session-Grundlage vorbereiten.

Akzeptanz:

- ServerConnectionHandler wird kleiner und klarer.
- Saves skalieren ueber Properties-V1 hinaus.
- Multiplayer-UI kann Pending/Accepted/Rejected sauber darstellen.

---

# Phase 4 - Physics, Collision und Replay

Owner: Physics und Engine Worker.

Status 2026-05-01:

- StatusEffectSystem hat einen Common-Contract fuer burning/chilled/wet/rested/cozy/poison, Modifier, Tick-Pulses und Save-State; Server-Anwendung, Save-Felder und Client-Events bleiben offen.

Aufgaben:

- Collision Cache und Shape Query API planen.
- PartialShapeImpactResolver fuer Projectile/Interaction-Hits vorbereiten.
- EntityFluidForceStep und Lava/Hazard-Schnittstellen ausarbeiten.
- PhysicsReplayRecorder planen und erste Golden-Replay-Erweiterung bauen.
- Physics-Debug-Overlays in HUD/Diagnostics integrieren.
- Kleine Engine-Oberflaechen-Extraktionen unterstuetzen, ohne in Shader/Render-Pipeline des Lead Engine Developers zu greifen.

Akzeptanz:

- Player, Entity und Projectile nutzen dieselbe Collision-Wahrheit.
- Physics-Bugs werden reproduzierbar.
- Engine-Arbeit verteilt sich auf mehr als eine Person.

---

# Phase 5 - Core Gameplay und Content-Systeme

Owner: Lead Game Design Engineer.

Aufgaben:

- Core Loop in messbare Milestones schneiden.
- ContentTagRegistry mit Engine/Networking/Physics abstimmen.
- Station Progression fuer Workbench, Campfire, Cooking Pot, Forge und spaetere Ancient Stations definieren.
- Item-/Recipe-/Loot-Balancing als Design-Daten pflegen.
- Cozy-Life, Tiere, Lore, Journal und Ruinenprogression systematisch ausbauen.
- Worldgen-Biome-Progression mit Ressourcen und Structures synchronisieren.

Akzeptanz:

- Jede neue Ressource hat einen Gameplay-Grund.
- Early/Mid/Late Game fuehren logisch ineinander.
- Design-Systeme sind speicher- und netzwerkfaehig.

---

# Phase 6 - UI/UX, HUD und Launcher/Game Flow

Owner: Lead UI/UX Frontend Developer.

Aufgaben:

- Screen Architecture V1 bauen.
- Component Library V1 bauen:
  - Button.
  - Slot.
  - Tab.
  - Tooltip.
  - ProgressBar.
  - ScrollList.
  - Modal.
  - TextInput.
- HUD Presenter aus `GameClient` extrahieren.
- Transaction-aware UI fuer Storage/Crafting/Cooking/Forge vorbereiten.
- Journal und Settings als echte Screens mit ViewModels.
- Launcher und Ingame-UI stilistisch angleichen.

Akzeptanz:

- UI Scale funktioniert.
- Neue Screens wachsen nicht in GameClient.
- Online-Rejects und Pending-Aktionen sind verstaendlich.

---

# Phase 7 - Worldgen und Alpha Content Production

Owner: Lead Game Design Engineer mit Project Manager.

Aufgaben:

- Worldgen-Services aus `WORLDGEN_TODO_LIST.md` schneiden.
- Feature- und Structure-Tables datengetriebener machen.
- Biome Design Cards pflegen.
- Debug-/QA-Teleports fuer Biome und Structures planen.
- Smoke-Seeds fuer Core-Progression und Rendering-/Networking-Risiken aktuell halten.

Akzeptanz:

- Neue Biome/Structures koennen ohne Generator-Monolith wachsen.
- Spawn und Progression bleiben seed-robust.
- QA kann neue Inhalte gezielt finden.

---

# Phase 8 - Alpha Gate und Integration

Owner: Project Manager, alle Leads liefern Nachweise.

Pflicht-Gates:

- `./gradlew test` oder fokussierte Modul-Tests.
- `./gradlew buildGame`, wenn Build-/Packaging-/Cross-Modul-Code betroffen ist.
- Manual Smoke aus `docs/WORLD_SMOKE_TESTS.md`, wenn GL/Rendering/Gameplay-Flow betroffen ist.
- Networking-Smoke bei Packet-, Server- oder Persistence-Aenderungen.
- Save-/Load-Smoke bei World/Player/BlockEntity-Aenderungen.
- Profiling-Szenario bei Engine-/Render-/Chunk-/Entity-Performance-Aenderungen.

Akzeptanz:

- Alpha-Build startet lokal.
- Singleplayer und Join Local bleiben nutzbar.
- Kein neues Feature erzeugt bekannte Dupes, Save-Verlust, Shader-Sonderfaelle oder unbudgetierte Runtime-Arbeit.

## Sofort empfohlene erste Arbeitspakete

1. Project Manager: ~~Refactor-Map und DoD-Checkliste fuer Monolithen anlegen.~~ Erledigt am 2026-05-01 in `docs/ENGINE_TODO_LIST.md` P13.1/P13.2; naechster PM-Schritt ist Gate-/Runner-Risiken aus P0.1/P0.2 nachhalten.
2. Main Networking Dev: `docs/NETWORKING_AND_PERSITENCE_TODO_LIST.md` P0/P1 starten, Protocol Contract und ActionPipeline-Skeleton.
3. Lead UI/UX Frontend Developer: ScreenContext und ersten Screen aus `GameClient` extrahieren.
4. Lead Engine Developer: Render Pipeline V3-Schnitt fuer Terrain/Water/Sprite/State vorbereiten.
5. Physics und Engine Worker: PartialShapeImpactResolver-Design und Collision-Cache-Tests vorbereiten.
6. Lead Game Design Engineer: Alpha-Milestone-Tabelle und ContentTagRegistry-Anforderungen definieren.

## Agent Prompts

Die copy-paste-faehigen Prompts fuer alle sechs Rollen stehen in `docs/AGENT_PROMPTS.md`. Jeder Prompt enthaelt dieselbe Grundarbeitsanweisung und eine andere konkrete Aufgabe.
