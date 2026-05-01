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

- `PartialShapeImpactResolver`: Common-Tool/API fuer exakte Projectile-Impacts gegen Nicht-Wuerfel. Es sollte aus `BlockCollisionShape` eine Methode wie `raycastProjectile(fromX, fromY, fromZ, toX, toY, toZ, ProjectileBounds)` liefern und `ImpactResult(blockX, blockY, blockZ, impactX, impactY, impactZ, normalX, normalY, normalZ, face, fraction)` zurueckgeben. Full-Cubes nutzen Slab-AABB, Furniture/Cutout-Blocks mehrere lokale AABBs. `ServerWorld.collidesProjectile` sollte danach nicht nur `boolean`, sondern den naechsten Hit liefern. Tests: Pfeil gegen Fence/Table/Chair/Campfire, Treffer an Kanten, kein Tunneling bei hohen Geschwindigkeiten.
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
- 🔴 Buoyancy fuer leichte Ambient-Entities ausbauen.
- 🔴 Lava als eigener Fluid-/Hazard-Block fehlt noch.

## Umgesetzt

- `FluidPhysics` modelliert `FluidSample` mit Velocity, Drag und Buoyancy. `ServerWorld.fluidSample` liefert fuer Wasser optionale Current-Vektoren.
- Projectiles nutzen Fluid-Samples statt reiner `inWater`-Sonderlogik; Items bekommen Buoyancy und Current-Drift ueber `DroppedItemEntity.tick(..., FluidQuery)`.
- `EnvironmentHazardRules` klassifiziert aktive Campfires als Hot-Block, Cactus als Thorn-Hazard und Ice/Snow als Cold-Hazard. Server-Spieler bekommen Umwelt-Damage autoritativ mit Cooldown.
- Client-Audio bekommt einen echten `underwater`-State aus `headUnderwater`; Render-Tint/Fog bleiben an denselben State gebunden.
- Tests decken Projectile-Current, Item-Buoyancy, Hot/Cold/Thorn-Hazards und Wasser-Fallimpact ab.

## 🔴 Braucht Engine/Tool

- `EntityFluidForceStep`: Ambient-Entities brauchen als naechstes Fluid-Forces im gemeinsamen `EntityPhysics`-Pfad. API-Vorschlag: `EntityPhysics.applyFluidForces(EntitySnapshot current, EntityPhysicsProfile profile, FluidSample sample, double deltaSeconds)`. Profile sollten Masse/Buoyancy-Faktor tragen, z.B. `tiny=0.9`, `heavy=0.15`, `swimmer=kontrolliert`, `flyer=ignoriert`. ServerEntityTracker ruft das vor Sweep/Slide auf. Tests: Bunny treibt leicht, Boar kaum, Swimmer bleibt steuerbar, Flyer ignoriert Wasser.
- `LavaMaterialBlock`: Fuer Lava braucht die Engine einen Block in `Blocks`/`Items`, RenderMaterial mit emissive/animated fluid, `FluidPhysics.lava(...)` mit hoher Viskositaet, `EnvironmentHazardRules` mit Fire-Damage und ServerWorld-FluidSample. Client braucht Tint/Audio getrennt von Wasser. Tests: Player nimmt Damage, Projectiles werden stark gedampft, Items sinken/verbrennen erst spaeter wenn Item-Damage existiert.

## Akzeptanz

- Fluids beeinflussen Bewegung konsistent statt nur ueber Sonderfaelle.
- Umweltgefahren laufen serverseitig autoritativ.
- Wasseruebergaenge bleiben lesbar und nicht unfair.

---

# Naechste Game-Base-Bloecke

Diese Punkte sind nicht nur Physics, aber sie wuerden die Base deutlich besser fuer ein groesseres Spiel machen.

- 🔴 `ActionPipeline`: Ein gemeinsames Ability-/Use-System fuer Essen, Fuettern, Block-Interact, Projectile-Shoot, Tools, spaeter Spells. Es sollte in Common `ActionDefinition`, `ActionRequest`, `ActionValidationResult` und `ActionExecutionResult` geben. ServerConnectionHandler wuerde nur noch Requests routen; Regeln, Cooldowns, Kosten, Durability und Result-Events waeren datengetrieben testbar.
- 🔴 `GameplayEventStream`: Server-authoritative Event-Packets fuer Damage, Heal, Pickup, Craft, ProjectileImpact, Sleep, Weather, Quest. Coding-Vorschlag: Common sealed `GameplayEvent`, Packet `GameplayEvents(List<GameplayEvent>)`, Client-Consumer fuer Audio/Particles/UI. Vorteil: weniger ad-hoc Packet-Typen und sauberere Replays.
- 🔴 `ContentTagRegistry`: JSON- oder Codegen-Tags fuer Items/Blocks/Entities wie `ranged`, `flammable`, `cold`, `floaty`, `heavy`, `comfort_source`, `station`. API: Registry laedt Tags beim Start, Tests pruefen unbekannte Keys und zyklische Aliase. Dann muessen neue Items nicht in zig Java-Switches landen.
- 🔴 `StatusEffectSystem`: Kleine serverseitige Effects mit Dauer, Stack-Regel, Tick-Rate und Save-State: burning, chilled, wet, rested, cozy, poison. Common-Regeln, Server-State, Client-Event fuer HUD/Audio. Das macht Hazards und Biome spaeter viel lebendiger.
- 🔴 `PhysicsReplayRecorder`: Tool zum Mitschneiden echter Server-Steps als Golden-Replay. Format: JSONL pro Tick mit Input, World-Sample-Hashes, Entity-Snapshots, Projectile-Hits und Stats. CLI: `./gradlew recordPhysicsReplay --scenario ...` und `./gradlew physicsRegression`.
