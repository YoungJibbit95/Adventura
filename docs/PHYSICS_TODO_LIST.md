# Adventura - Physics TODO List

Stand: 2026-05-01

Diese Liste enthaelt nur noch offene technische Physik-Arbeit. Erledigte Alpha-Punkte aus Movement Validation, Collision Shapes, Placement, Fall Damage, Projectile Foundation, Physics-Diagnostics, Fuzzing, Golden-Replays und der Regression-Testmatrix wurden entfernt.

## Ziel

Physics soll stabil, vorhersehbar, cozy und multiplayer-sicher bleiben. Server-Autoritaet hat Vorrang; Client-Polish kommt danach. Neue Physik-Features sollen zuerst als Common-Regeln modelliert und dann serverseitig validiert werden.

## Leitlinien

- Simulation gehoert nach `common/physics`.
- Server entscheidet, ob Movement, Placement, Interaction und Hits plausibel sind.
- Client darf vorhersagen und weich darstellen, aber nicht die Wahrheit besitzen.
- Unbekannte Chunks, fehlende Daten und NaN/Infinity gelten immer als blockierend oder invalid.
- Jede neue Physik-Regel braucht mindestens einen Missbrauchs- oder Edge-Case-Test.
- Schneller Regression-Gate: `./gradlew physicsRegression`.

---

# P0 - Physics Kernel Hardening

## Offen

- ~~Einheitlichen `PhysicsStepContext` fuer Delta, Tick, Dimension, Mode, Wasserzustand und Debug-Metadaten einfuehren.~~
- ~~Gemeinsame Numeric Guards fuer Position, Velocity, Delta, Bounds und Projectile-State zentralisieren, statt sie pro Record/Regel zu wiederholen.~~
- ~~Common-Testfixtures fuer kleine Blockwelten bauen, damit Movement-, Collision-, Placement- und Projectile-Tests weniger handverdrahtet sind.~~
- ~~Physics-Konfigurationswerte versionieren oder snapshotbar machen, damit Client/Server-Mismatch leichter sichtbar wird.~~
- ~~Replay-Harness ausbauen: mehrere Steps, Player-Movement, Entity-Movement und Projectile-Schritte aus Golden-Dateien nachspielen.~~

## Umgesetzt

- `PhysicsStepContext` ist der gemeinsame Step-Carrier fuer Player-, Flying- und Projectile-Steps inklusive Tick, Dimension, Mode, Wasserzustand und Debug-Quelle.
- `PhysicsNumericGuard` zentralisiert Finite-/Delta-/Bounds-Pruefungen fuer Player, Projectiles, Entity-Snapshots, Damage-Knockback und Collision-Bounds.
- `PhysicsConfigSnapshot` erzeugt versionierte Fingerprints fuer Player- und Projectile-Konfigurationen; `wireId()` ist fuer Client/Server-Vergleich vorbereitet.
- `PhysicsTestWorld` und `PhysicsReplayHarness` decken kleine Blockwelten, Multi-Step-Player-Replays, Entity-Separation-Replays und Projectile-Replays aus Golden-Dateien ab.

## Akzeptanz

- Neue Physikmodule nehmen denselben Step-Kontext statt verstreuter Einzelparameter.
- Invalid-Werte koennen nicht durch Movement, Entities oder Projectiles sickern.
- Ein aufgezeichneter Physikablauf laesst sich deterministisch im Test nachspielen.
- Client und Server koennen ihre Physics-Konfigurationsversion vergleichen.

---

# P1 - Entity Physics V2

## Offen

- ~~Dynamische Entity-vs-Entity-Separation fuer nahe Tiere, Drops und Spieler einfuehren, statt nur blockierende Overlaps zu stoppen.~~
- ~~Ambient-Entities mit einfachen Recovery-Steps aus Block-/Entity-Blockaden holen.~~
- ~~Per-Entity Physics-Profil einfuehren: ground, flyer, swimmer, tiny, heavy.~~
- ~~Item-Drop-Merge/Stacking physikalisch sicher machen, ohne Pickup-Dupe zu riskieren.~~
- ~~Knockback gegen Terrain sweepen und gleiten lassen, statt blockierte Knockbacks nur zu nullen.~~
- ~~Entity-Wasserverhalten pro Profil definieren: meiden, schwimmen, treiben, ignorieren.~~

## Umgesetzt

- `EntityPhysicsProfile` definiert Ground/Flyer/Swimmer/Tiny/Heavy sowie Wasserverhalten `AVOID`, `SWIM`, `FLOAT`, `IGNORE` pro Entity-Typ.
- `EntityPhysics` uebernimmt Separation, Knockback-Impulse, Sweep/Slide und Recovery-Kandidaten als Common-Regeln.
- `ServerEntityTracker` nutzt diese Regeln fuer Ambient-Movement, lokale Separation gegen Spieler/Entities/Drops, Knockback-Sliding und Item-Drop-Merging.
- Item-Drops mergen nur gleiche Items mit gleichem Damage und respektieren `ItemType.maxStackSize`; entfernte Drops koennen danach nicht doppelt geclaimt werden.
- `ServerWorld.entityPlacementClear` nutzt die Profile fuer Ground-Support und Wasserplatzierung statt Firefly-/Water-Sonderfaellen.

## Akzeptanz

- Entities bleiben nicht dauerhaft ineinander oder in Terrain stecken.
- Knockback und Follow/Flee respektieren dieselben Kollisionsregeln.
- Neue Entity-Typen brauchen ein Profil statt Sonderlogik im Tracker.
- Item-Drops koennen nicht durch Merge/Pickup-Rennen dupliziert werden.

---

# P2 - Projectiles And Hit Resolution

## Offen

- ~~Bow-/Tool-Input und Server-Spawn-Packet an die Projectile-Foundation anbinden.~~
- ~~Pfeile nach Blocktreffer optional stecken lassen oder als kurzes Impact-Snapshot senden.~~
- ~~Client-Modell, Trail, Hit-Partikel und Hit-Audio fuer Projektile bauen.~~
- ~~Projectile-Lag-Kompensation fuer bewegte Ziele evaluieren.~~
- ~~Damage-/Friendly-Fire-/Owner-Regeln zentral definieren.~~
- ~~Projectile-vs-Projectile oder Projectile-vs-Door/Furniture-Regeln entscheiden.~~
- 🔴 Impact-Face/Normal fuer komplexere Partial-Shapes weiter verfeinern.

## Umgesetzt

- `ProjectileShoot` ist ein echtes Client-Intent-Packet. Der Server validiert Slot, Rate-Limit, Cooldown und `ProjectileItemRules`, spawned Projectiles autoritativ und synced Inventory-Damage.
- `ProjectileImpact` ist ein kurzes serverseitiges Impact-Snapshot mit Impact-Position, Block-Koordinate, BlockFace/Normal, Entity-Target und `stuck`-Flag fuer Client-Polish.
- `ProjectileDamageRules` entscheidet Owner-Immunity, Friendly-Fire-Default, Item-Drop-/Projectile-Ignore und Objekt-Regeln zentral in Common.
- `ProjectileLagCompensation` definiert ein klares 180-ms-Rewind-Fenster und lineares Snapshot-Rewind als Grundlage fuer spaetere bewegte Ziel-Historien.
- Client rendert `voxel:arrow_projectile` mit eigenem Modell, Trail-Partikeln, Impact-Partikeln und Projectile-Audio-Cues.

## 🔴 Braucht Engine/Tool

- ~~`PartialShapeImpactResolver`: Common-Tool/API fuer exakte Projectile-Impacts gegen Nicht-Wuerfel. Es sollte aus `BlockCollisionShape` eine Methode wie `raycastProjectile(fromX, fromY, fromZ, toX, toY, toZ, ProjectileBounds)` liefern und `ImpactResult(blockX, blockY, blockZ, impactX, impactY, impactZ, normalX, normalY, normalZ, face, fraction)` zurueckgeben. Full-Cubes nutzen Slab-AABB, Furniture/Cutout-Blocks mehrere lokale AABBs. `ServerWorld.collidesProjectile` sollte danach nicht nur `boolean`, sondern den naechsten Hit liefern. Tests: Pfeil gegen Fence/Table/Chair/Campfire, Treffer an Kanten, kein Tunneling bei hohen Geschwindigkeiten.~~
  Erledigt: 2026-05-01 - `PartialShapeImpactResolver`, `BlockCollisionShape.raycastProjectile(...)`, `BlockCollisionShapes.projectileShape(...)`, `ProjectilePhysics.BlockImpactQuery` und `ServerWorld.projectileImpact(...)` eingefuehrt.
  Verifikation: `./gradlew :common:test --tests dev.voxelgame.common.physics.BlockCollisionShapesTest --tests dev.voxelgame.common.physics.ProjectilePhysicsTest --no-daemon --max-workers=1 --rerun-tasks`; `./gradlew :server:test --tests dev.voxelgame.server.world.ServerWorldTest --no-daemon --max-workers=1 --rerun-tasks`; `./gradlew :server:test --tests dev.voxelgame.server.entity.ServerEntityTrackerTest --tests dev.voxelgame.server.net.ServerConnectionHandlerTest --no-daemon --max-workers=1 --rerun-tasks`; `./gradlew :common:physicsRegression :server:physicsRegression --no-daemon --max-workers=1 --rerun-tasks`.
- `RangedActionContent`: Wenn echte Boegen/Ammo kommen sollen, braucht das Content-System Item-Tags wie `action=ranged_projectile`, `projectile=voxel:arrow_projectile`, `ammo=voxel:arrow`, `cooldown`, `durabilityDamage`, `chargeTime`. Aktuell ist Tool-Input fuer Messer als server-autorisierter Throw-Intent implementiert; Bow/Ammo sollte datengetrieben werden, nicht hart im Client.

## Akzeptanz

- Spieler koennen Projektile nutzen, ohne Client-Hit-Trust.
- Treffer sind reproduzierbar und fuer Client-Effekte genau genug lokalisiert.
- Despawn, Damage und Visuals bleiben synchron.
- Bewegte Ziele bleiben innerhalb eines klaren Lag-Fensters fair treffbar.

---

# P3 - Fluid And Environmental Physics

## Offen

- ~~Stroemungen als optionale Fluid-Velocity-Felder modellieren.~~
- ~~Buoyancy fuer Items und Projectiles definieren.~~
- ~~Hot-Blocks/Cold-Blocks als Environment-Physics-Hazards pruefen.~~
- ~~Wasseroberflaechen-Transitions weiter testen: Reinfallen, Auftauchen, Springen an Kante.~~
- ~~Underwater Audio/Tint an echten `headUnderwater`-State binden.~~
- ~~Tests fuer fallende Items und Projectiles durch Wasser/Luft-Grenzen ergaenzen, sobald Buoyancy/Fluid-Forces existieren.~~
- ~~🔴 Buoyancy fuer leichte Ambient-Entities ausbauen.~~
  Erledigt: 2026-05-02 - Ambient-Entities nutzen `EntityPhysics.applyFluidForces(...)` mit profilbasiertem Buoyancy-Faktor vor Sweep/Slide.
  Verifikation: `./gradlew :common:test --tests dev.voxelgame.common.physics.EntityPhysicsTest :server:test --tests dev.voxelgame.server.entity.ServerEntityTrackerTest.ambientEntitiesApplyFluidForcesBeforeSweep :common:physicsRegression :server:physicsRegression -PadventuraTestRunId=p3_entity_fluid_final_1 --no-daemon --max-workers=1`.
- ~~🔴 Lava als eigener Fluid-/Hazard-Block fehlt noch.~~
  Erledigt: 2026-05-02 - `voxel:lava` ist als leuchtender, nicht kollidierender Translucent-Fluid-Block mit Item-Key, RenderMaterial/Atlas-Mapping, `FluidPhysics.lava(...)`, ServerWorld-Fluid-Sample und `EnvironmentHazardRules`-Hazard umgesetzt.
  Verifikation: `./gradlew :common:test --tests dev.voxelgame.common.physics.FluidPhysicsTest --tests dev.voxelgame.common.physics.EnvironmentHazardRulesTest --tests dev.voxelgame.common.content.ContentTagRegistryTest --tests dev.voxelgame.common.block.BlockRegistryDataTest --tests dev.voxelgame.common.block.BlockDropRegistryTest --tests dev.voxelgame.common.item.ItemRegistryDataTest :server:test --tests dev.voxelgame.server.world.ServerWorldTest --tests dev.voxelgame.server.entity.DroppedItemEntityTest :client:test --tests dev.voxelgame.client.render.BlockRenderPropertiesTest --tests dev.voxelgame.client.render.assets.BlockTextureAtlasTest -PadventuraTestRunId=p3_lava_contract_3 --no-daemon --max-workers=1`; `./gradlew :common:physicsRegression :server:physicsRegression :client:physicsRegression -PadventuraTestRunId=p3_lava_regression_2 --no-daemon --max-workers=1`.
- 🟡 Lava-Folgepolish: echte Item-Verbrennung/Despawn-Entscheidung braucht ein eigenes Item-Damage-/Destroy-System; dedizierte Kamera-in-Lava-/Audio-Cues gehoeren spaeter in Client/UI-Audio-Polish.

## Umgesetzt

- `FluidPhysics` modelliert `FluidSample` mit Velocity, Drag und Buoyancy. `ServerWorld.fluidSample` liefert fuer Wasser optionale Current-Vektoren.
- Projectiles nutzen Fluid-Samples statt reiner `inWater`-Sonderlogik; Items bekommen Buoyancy und Current-Drift ueber `DroppedItemEntity.tick(..., FluidQuery)`.
- Ambient-Entities bekommen Fluid-Kraefte ueber den gemeinsamen `EntityPhysics`-Pfad. `EntityPhysicsProfile` traegt profilbasierte Buoyancy-Faktoren: tiny/float stark, heavy schwach, swimmer kontrolliert, flyer ignoriert.
- Lava nutzt denselben Fluid-Sample-Pfad wie Wasser, aber mit hoher Viskositaet, niedriger Buoyancy und autoritativem Hot-Hazard. Player-Water-State bleibt bewusst wasser-spezifisch.
- `EnvironmentHazardRules` klassifiziert aktive Campfires als Hot-Block, Cactus als Thorn-Hazard und Ice/Snow als Cold-Hazard. Server-Spieler bekommen Umwelt-Damage autoritativ mit Cooldown.
- Client-Audio bekommt einen echten `underwater`-State aus `headUnderwater`; Render-Tint/Fog bleiben an denselben State gebunden.
- Tests decken Projectile-Current, Item-Buoyancy, Hot/Cold/Thorn-Hazards und Wasser-Fallimpact ab.

## 🔴 Braucht Engine/Tool

- ~~`EntityFluidForceStep`: Ambient-Entities brauchen als naechstes Fluid-Forces im gemeinsamen `EntityPhysics`-Pfad. API-Vorschlag: `EntityPhysics.applyFluidForces(EntitySnapshot current, EntityPhysicsProfile profile, FluidSample sample, double deltaSeconds)`. Profile sollten Masse/Buoyancy-Faktor tragen, z.B. `tiny=0.9`, `heavy=0.15`, `swimmer=kontrolliert`, `flyer=ignoriert`. ServerEntityTracker ruft das vor Sweep/Slide auf. Tests: Bunny treibt leicht, Boar kaum, Swimmer bleibt steuerbar, Flyer ignoriert Wasser.~~
  Erledigt: 2026-05-02 - Contract umgesetzt und an `ServerEntityTracker.tickAmbient(..., FluidQuery)` angeschlossen.
  Verifikation: `EntityPhysicsTest` deckt Bunny/Boar/Swimmer/Flyer ab; `ServerEntityTrackerTest.ambientEntitiesApplyFluidForcesBeforeSweep` prueft den Server-Step.
- ~~`LavaMaterialBlock`: Fuer Lava braucht die Engine einen Block in `Blocks`/`Items`, RenderMaterial mit emissive/animated fluid, `FluidPhysics.lava(...)` mit hoher Viskositaet, `EnvironmentHazardRules` mit Fire-Damage und ServerWorld-FluidSample. Client braucht Tint/Audio getrennt von Wasser. Tests: Player nimmt Damage, Projectiles werden stark gedampft, Items sinken/verbrennen erst spaeter wenn Item-Damage existiert.~~
  Erledigt: 2026-05-02 - Core-Contract umgesetzt; Item-Drops werden in Lava stark gebremst und noch nicht verbrannt, bis ein explizites Item-Damage-System existiert.
  Verifikation: `FluidPhysicsTest`, `EnvironmentHazardRulesTest`, `ServerWorldTest`, `DroppedItemEntityTest`, `BlockRenderPropertiesTest`, `BlockTextureAtlasTest`.

## Akzeptanz

- Fluids beeinflussen Bewegung konsistent statt nur ueber Sonderfaelle.
- Umweltgefahren laufen serverseitig autoritativ.
- Wasseruebergaenge bleiben lesbar und nicht unfair.

---

# Naechste Game-Base-Bloecke

Diese Punkte sind nicht nur Physics, aber sie wuerden die Base deutlich besser fuer ein groesseres Spiel machen.

- 🟠 `ActionPipeline`: Ein gemeinsames Ability-/Use-System fuer Essen, Fuettern, Block-Interact, Projectile-Shoot, Tools, spaeter Spells. Es sollte in Common `ActionDefinition`, `ActionRequest`, `ActionValidationResult` und `ActionExecutionResult` geben. ServerConnectionHandler wuerde nur noch Requests routen; Regeln, Cooldowns, Kosten, Durability und Result-Events waeren datengetrieben testbar.
  Status: Common-Contract und erster Server-Slice erledigt am 2026-05-01 mit `common.actions`, Pipeline-Tests und `ServerProjectileShootAction`; weitere Aktionen und Result-Events bleiben Anschlussarbeit.
  Verifikation: `./gradlew :common:test --tests dev.voxelgame.common.actions.ActionPipelineTest --tests dev.voxelgame.common.content.ContentTagRegistryTest -PadventuraTestRunId=action_pipeline_1 --no-daemon --max-workers=1`; Server-Slice laut Networking-Liste via `ServerProjectileShootActionTest`/`ServerConnectionHandlerTest`.
- 🟠 `GameplayEventStream`: Server-authoritative Event-Packets fuer Damage, Heal, Pickup, Craft, ProjectileImpact, Sleep, Weather, Quest. Coding-Vorschlag: Common sealed `GameplayEvent`, Packet `GameplayEvents(List<GameplayEvent>)`, Client-Consumer fuer Audio/Particles/UI. Vorteil: weniger ad-hoc Packet-Typen und sauberere Replays.
  Status: Common-Event-Modell, Packet/Codec-Contract, erster Client-Feedback-Hook und ProjectileImpact-Server-Emission erledigt am 2026-05-01; Interest-Filter und weitere Gameplay-Producer/HUD-/Journal-/Particle-Consumer bleiben Anschlussarbeit.
  Verifikation: `./gradlew :common:clean :common:test --tests dev.voxelgame.common.net.PacketCodecTest --tests dev.voxelgame.common.net.PacketCodecGoldenTest --tests dev.voxelgame.common.net.ProtocolContractTest --tests dev.voxelgame.common.gameplay.GameplayEventTest --no-daemon --max-workers=1`; `./gradlew :common:clean :server:cleanTest :server:test --tests dev.voxelgame.server.net.ServerConnectionHandlerTest.gameServerBroadcastsProjectileImpactFromAuthoritativeTick --no-daemon --max-workers=1`; zusaetzlich `GameplayEventFeedbackTest`.
- ~~🔴 `ContentTagRegistry` V1: Tags fuer Items/Blocks/Entities wie `ranged`, `flammable`, `cold`, `floaty`, `heavy`, `comfort_source`, `station`.~~
  Erledigt: codebasierte V1 in `common.content` mit Alias-Aufloesung, Unknown-Key-Verhalten, Coverage-Report und Common-Tests. Offen fuer Physics: bestehende Projectile-/Hazard-/Entity-Profil-Switches schrittweise auf Read-Queries umstellen; JSON/Codegen und zyklische Aliaspruefung bleiben spaeter.
  Verifikation: `ContentTagRegistryTest` XML/HTML meldet 8 Tests, 0 Failures.
- 🟠 `StatusEffectSystem`: Common-Contract fuer Dauer, Stack-Regel, Tick-Rate, Movement-/Stamina-Modifier und Save-State.
  Status: Common-Regeln erledigt am 2026-05-01 in `common.gameplay.status` mit Definitionen fuer burning, chilled, wet, rested, cozy und poison. Server-State, Save-Felder und Client-Events bleiben Anschlussarbeit.
  Verifikation: `./gradlew :common:test --tests dev.voxelgame.common.gameplay.status.StatusEffectSystemTest -PadventuraTestRunId=status_effect_common_2 --no-daemon --max-workers=1`.
- ~~🔴 `PhysicsReplayRecorder`: Tool zum Mitschneiden echter Server-Steps als Golden-Replay. Format: JSONL pro Tick mit Input, World-Sample-Hashes, Entity-Snapshots, Projectile-Hits und Stats. CLI: `./gradlew recordPhysicsReplay --scenario ...` und `./gradlew physicsRegression`.~~
  Erledigt: 2026-05-02 - Common-Format/Recorder plus serverseitiger `PhysicsReplayScenarioRecorder` fuer das deterministische `projectile-impact`-Szenario. Gradle-Task: `./gradlew recordPhysicsReplay -Pscenario=projectile-impact -Pseed=12345 -Pticks=8 -Pout=build/physics-replays/test-projectile-impact.jsonl`.
  Verifikation: `./gradlew :common:test --tests dev.voxelgame.common.physics.PhysicsReplayRecorderTest -PadventuraTestRunId=p42a_replay_5 --no-daemon --max-workers=1`; `./gradlew :common:physicsRegression -PadventuraTestRunId=p42a_replay_regression_1 --no-daemon --max-workers=1`; `./gradlew :server:test --tests dev.voxelgame.server.physics.PhysicsReplayScenarioRecorderTest -PadventuraTestRunId=p42b_server_replay_1 --no-daemon --max-workers=1 --rerun-tasks`; `./gradlew recordPhysicsReplay -Pscenario=projectile-impact -Pseed=12345 -Pticks=8 -Pout=build/physics-replays/test-projectile-impact.jsonl -PadventuraTestRunId=p42b_record_task_1 --no-daemon --max-workers=1 --rerun-tasks`.
  Nachverifikation: `./gradlew :common:test --tests dev.voxelgame.common.physics.EntityPhysicsTest :server:test --tests dev.voxelgame.server.entity.ServerEntityTrackerTest.ambientEntitiesApplyFluidForcesBeforeSweep :common:physicsRegression :server:physicsRegression -PadventuraTestRunId=p3_entity_fluid_final_1 --no-daemon --max-workers=1` laeuft am 2026-05-02 erfolgreich.

---

# P4 - Physics und Engine Surface Worker Track

Owner: Physics und Engine Worker.

Dieser Block ist fuer parallele Engine-Arbeit gedacht, damit der Lead Engine Developer nicht allein alle Engine-Oberflaechen tragen muss.

## P4.1 Collision Cache und Shape Queries

### Aktueller Schritt

- ~~🟠 In Arbeit 2026-05-01: `P4.1a PartialShapeImpactResolver` fuer exakte Projectile-Impacts gegen Partial-Shapes und gemeinsame Server-Query.~~
  Erledigt: 2026-05-01 - Projectile-Impacts nutzen gesweepte Shape-Raycasts mit Face/Normal/Fraction; Server-Tick verdrahtet die autoritative Impact-Query.
  Verifikation: `./gradlew :common:physicsRegression :server:physicsRegression --no-daemon --max-workers=1 --rerun-tasks`.
- ~~🟠 In Arbeit 2026-05-01: `P4.1b CollisionShapeCache` fuer gemeinsame Player-/Entity-/Projectile-Shape-Queries und BlockUpdate-Invalidierung.~~
  Erledigt: 2026-05-01 - `CollisionShapeCache` cached Movement- und Projectile-Shapes pro Chunk-Section; Server und Client nutzen ihn fuer Player-/Entity-/Projectile-Kollisionen, Projectile-Impacts und Client-Debug-Bounds.
  Verifikation: `./gradlew :common:test --tests dev.voxelgame.common.physics.CollisionShapeCacheTest --tests dev.voxelgame.common.physics.BlockCollisionShapesTest --tests dev.voxelgame.common.physics.ProjectilePhysicsTest --no-daemon --max-workers=1 --rerun-tasks`; `./gradlew :server:test --tests dev.voxelgame.server.world.ServerWorldTest --tests dev.voxelgame.server.entity.ServerEntityTrackerTest --tests dev.voxelgame.server.net.ServerConnectionHandlerTest --no-daemon --max-workers=1 --rerun-tasks`; `./gradlew :client:test --tests dev.voxelgame.client.world.ClientWorldCollisionTest --no-daemon --max-workers=1 --rerun-tasks`; `./gradlew :common:physicsRegression :server:physicsRegression :client:physicsRegression --no-daemon --max-workers=1 --rerun-tasks`.

### Offen

- ~~Collision-Cache pro Chunk/Section planen.~~
  Erledigt: 2026-05-01 - `CollisionShapeCache` speichert Section-Caches getrennt fuer `MOVEMENT` und `PROJECTILE`.
  Verifikation: `CollisionShapeCacheTest.cachesSectionShapesAndInvalidatesChangedBlock`.
- Partial-Shapes zentral raycast- und sweep-faehig machen.
  - ~~Projectile-Raycasts fuer Full-Cubes, Fence, Table, Chair und Campfire/Decoration-Shape zentralisiert.~~
    Erledigt: 2026-05-01
    Verifikation: `BlockCollisionShapesTest`, `ProjectilePhysicsTest`, `ServerWorldTest`.
- Player-, Entity- und Projectile-Kollision sollen dieselbe Shape-Quelle nutzen.
  - ~~Projectile nutzt jetzt `BlockCollisionShapes.projectileShape(...)`; Player/Entity bleiben auf `collisionShape(...)`, alle aus derselben Shape-Registry.~~
    Erledigt: 2026-05-01
    Verifikation: `BlockCollisionShapesTest.projectileRaycastUsesSamePartialShapeSource`.
  - ~~Server und Client fragen Player-/Entity-/Projectile-Kollisionen ueber denselben `CollisionShapeCache` ab.~~
    Erledigt: 2026-05-01
    Verifikation: `ServerWorldTest`, `ClientWorldCollisionTest`, `CollisionShapeCacheTest`.
- Debug-Overlay fuer:
  - player bounds.
  - entity bounds.
  - projectile sweep.
  - block shape.
  - stuck/separation events.
- ~~Cache-Invalidierung bei BlockUpdate testen.~~
  Erledigt: 2026-05-01 - `ServerWorld.setBlock`, `ClientWorld.applyBlock`, `ClientWorld.applyChunk` und Chunk-Unload invalidieren Shape-Caches.
  Verifikation: `ServerWorldTest.blockUpdatesInvalidateServerCollisionCache`, `ClientWorldCollisionTest.blockUpdatesInvalidateClientCollisionCache`, `CollisionShapeCacheTest.cachesSectionShapesAndInvalidatesChangedBlock`.

### Akzeptanz

- Keine separate Collision-Wahrheit fuer Player, Entity und Projectile.
- Partial-Shapes liefern genaue Hit-Normalen.
- Kollisionen bleiben bei Chunkgrenzen stabil.

## P4.2 Deterministic Physics Replay

### Aktueller Schritt

- ~~🟠 In Arbeit 2026-05-01: `P4.2a PhysicsReplayRecorder` mit versioniertem JSONL-Frame-Format, Common-Recorder/Parser und Regression-Testresource.~~
  Erledigt: 2026-05-01 - JSONL-Schema V1 deckt Tick, Input, Player-State, Entity-State, Projectile-State, Block-Samples, Event-Output und Stats ab; Common-Recorder erzwingt monoton steigende Ticks und liefert `recordStep(...)` als Server-Anschluss.
  Verifikation: `./gradlew :common:test --tests dev.voxelgame.common.physics.PhysicsReplayRecorderTest -PadventuraTestRunId=p42a_replay_5 --no-daemon --max-workers=1`; `./gradlew :common:physicsRegression -PadventuraTestRunId=p42a_replay_regression_1 --no-daemon --max-workers=1`.
- ~~🔴 In Arbeit 2026-05-02: `P4.2b ServerPhysicsReplayScenarioRecorder` fuer echte Server-Step-Aufzeichnung und `recordPhysicsReplay`-Gradle-Task.~~
  Erledigt: 2026-05-02 - Server-Runner zeichnet `projectile-impact` ueber `ServerWorld.projectileImpact`, `ServerWorld.fluidSample` und `ServerEntityTracker.tickProjectiles` auf; Frames enthalten Block-Samples, `worldSampleHash`, Entity-/Projectile-Snapshots und terminale Projectile-Hit-Events.
  Verifikation: `./gradlew :server:test --tests dev.voxelgame.server.physics.PhysicsReplayScenarioRecorderTest -PadventuraTestRunId=p42b_server_replay_1 --no-daemon --max-workers=1 --rerun-tasks`; `./gradlew recordPhysicsReplay -Pscenario=projectile-impact -Pseed=12345 -Pticks=8 -Pout=build/physics-replays/test-projectile-impact.jsonl -PadventuraTestRunId=p42b_record_task_1 --no-daemon --max-workers=1 --rerun-tasks`.

### Offen

- ~~Replay-Format festlegen:~~
  Erledigt: 2026-05-01 - `PhysicsReplayFrame` Schema V1.
  Verifikation: `PhysicsReplayRecorderTest.jsonlRoundTripKeepsStableOrderingAndAllCoreSections`.
  - ~~tick.~~
  - ~~input.~~
  - ~~player state.~~
  - ~~entity state.~~
  - ~~projectile state.~~
  - ~~relevant block samples.~~
  - ~~event output.~~
- ~~Existing `physics-replays/*.properties` zu einem erweiterbaren Format migrieren oder ergaenzen.~~
  Erledigt: 2026-05-01 - JSONL-Resource `physics-replays/projectile_impact_v1.jsonl` ergaenzt, bestehende Properties bleiben kompatibel.
  Verifikation: `PhysicsReplayRecorderTest.loadsJsonlGoldenResource`.
- ~~Server-Step-Recorder vorbereiten.~~
  Erledigt: 2026-05-01 - `PhysicsReplayRecorder.recordStep(PhysicsStepContext, Consumer<PhysicsReplayFrame.Builder>)` als Common-Anschluss fuer serverautoritatives Mitschneiden.
  Verifikation: `PhysicsReplayRecorderTest.recordStepCapturesContextAndOutputEvents`.
- ~~Regression-Task fuer Golden-Replays planen.~~
  Erledigt: 2026-05-01 - Neue Recorder-Tests tragen `@Tag("physicsRegression")` und laufen mit `:common:physicsRegression`.
  Verifikation: `./gradlew :common:physicsRegression -PadventuraTestRunId=p42a_replay_regression_1 --no-daemon --max-workers=1`.
- ~~Server-Szenario-Aufzeichnung und CLI-Task ergaenzen.~~
  Erledigt: 2026-05-02 - `recordPhysicsReplay` schreibt serverseitige JSONL-Replays unter `build/physics-replays/`.
  Verifikation: `PhysicsReplayScenarioRecorderTest`; `./gradlew recordPhysicsReplay -Pscenario=projectile-impact -Pseed=12345 -Pticks=8 -Pout=build/physics-replays/test-projectile-impact.jsonl -PadventuraTestRunId=p42b_record_task_1 --no-daemon --max-workers=1 --rerun-tasks`.

### Akzeptanz

- Physics-Bugs lassen sich mit einem kleinen Replay reproduzieren.
- Bewegungs- und Projectile-PRs koennen deterministisch verifiziert werden.

## P4.3 Status Effects und Movement Modifiers

### Aktueller Schritt

- ~~🔴 In Arbeit 2026-05-01: `P4.3a StatusEffectSystem` Common-Contract fuer Effect-Definitionen, Stack-Regeln, Tick-Pulses, Movement-/Jump-/Stamina-/Hunger-/Health-Modifier und Save-State.~~
  Erledigt: 2026-05-01 - `StatusEffectSystem`, `StatusEffectType`, `StatusEffectDefinition`, `StatusEffectState`, `ActiveStatusEffect`, `StatusEffectModifiers`, `StatusEffectPulse` und `StatusEffectSaveState` liegen in `common.gameplay.status`.
  Verifikation: `./gradlew :common:test --tests dev.voxelgame.common.gameplay.status.StatusEffectSystemTest -PadventuraTestRunId=status_effect_common_2 --no-daemon --max-workers=1`.

### Offen

- ~~StatusEffectSystem mit Physics-Schnittstelle definieren:~~
  - ~~chilled.~~
  - ~~wet.~~
  - ~~burning.~~
  - ~~rested.~~
  - ~~cozy.~~
  - ~~poison spaeter.~~
- ~~Effekte duerfen Movement/Jump/Stamina nur ueber klare Modifier aendern.~~
  Erledigt: 2026-05-01 - `StatusEffectModifiers` kombiniert reine Multiplikatoren fuer Movement, Jump, Stamina-Regen, Hunger-Drain und Health-Regen.
  Verifikation: `StatusEffectSystemTest.combinedModifiersRemainPureCommonContract`.
- Save- und Networking-Auswirkungen mit Main Networking Dev abstimmen.

### Akzeptanz

- Biome/Hazards koennen Bewegung beeinflussen, ohne Physics-Sonderfaelle.
- Effekte sind serverautoritativ und testbar.

## P4.4 Engine Surface Aufgaben

### Offen

- Kleine Extraktionen aus `GameClient` unterstuetzen:
  - Input state.
  - interaction targeting.
  - debug toggles.
  - physics debug rendering.
- ClientWorld-APIs fuer Physics sauber halten.
- Kein tiefer Shader-/Render-Pipeline-Umbau; diese Aufgabe bleibt beim Lead Engine Developer.

### Akzeptanz

- Physics-Features koennen parallel entwickelt werden.
- Lead Engine Developer wird bei Engine-Oberflaechen entlastet.
