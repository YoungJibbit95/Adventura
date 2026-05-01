# Adventura - Networking & Persistence TODO List

Stand: 2026-05-01
Owner: Main Networking Dev, mit Schnittstellen zu Project Manager, Lead Game Design Engineer und Lead Engine Developer.

## Ziel

Networking und Persistenz sollen Adventura online sicher und offline dauerhaft spielbar machen. Der Server bleibt autoritativ. Der Client sendet Intents, keine finalen Ergebnisse. Save-/Load-Daten muessen versioniert, migrationsfaehig und fuer groessere Welten skalierbar sein.

Diese Liste ersetzt den alten Platzhalter. Viele Alpha-Grundlagen sind vorhanden, aber die naechsten Core-Bestandteile fehlen noch fuer eine robuste Multiplayer-Alpha.

## Bestehende Basis

- `common` enthaelt sealed `GamePacket`-Records, `PacketType`, `PacketCodec` und Limits.
- Client und Server nutzen Netty mit length-prefixed TCP-Frames.
- Handshake, Login, ChunkData, BlockUpdate, BlockAction, BlockInteract, Storage, Crafting, Cooking, PlayerMove, EntitySnapshots, PlayerStats, ServerStats, Chat und Projectiles existieren.
- Server-World ist autoritativ fuer Blockmutationen, Inventar, Cooking, Storage, Sleep, Survival-State, Entity-Drops und Projectiles.
- Chunk-Interest, Entity-Interest, Bewegungsvalidierung, Intent-Rate-Limits, Async-ChunkStreamer und Netzwerkstats sind begonnen.
- Player-Saves und World-Saves werden atomar als Properties geschrieben; Save-Metadaten und einfache Migration existieren.
- BlockEntities, StorageCrates, Campfires, verbrauchte generated LootCrates und Player-Journal-/Discovery-Daten haben erste Save-Pfade.

## Leitlinien

- Keine Gameplay-Entscheidung wird nur clientseitig getroffen.
- Neue Clientaktionen laufen ueber servervalidierte Intents oder eine zentrale ActionPipeline.
- Packet-Format, Version und Limits werden mit Tests festgehalten.
- Grosse Welt- und Chunkdaten werden budgetiert, komprimiert oder delta-basiert uebertragen.
- Saves sind append-/region-faehig, migrationstauglich und crash-resistent.
- Netzwerk- und Save-Arbeit darf Netty/Event-Loop und Renderloop nicht blockieren.
- Alle neuen Features brauchen Offline-, Online- und Reconnect-Denken.

---

# P0 - Protocol Contracts und Compatibility

Status 2026-05-01: Erledigt durch Main Networking Dev - Packet-Contract-Katalog, Protocol-Doku und Golden-Codec-Tests.

## Offen

- ~~Protocol-Dokument fuer alle Packet-Typen anlegen:~~
  - ~~Richtung Client->Server / Server->Client.~~
  - ~~Authoritaet und Trust-Level.~~
  - ~~Rate-Limit.~~
  - ~~maximale Payload.~~
  - ~~Save-/Replay-Relevanz.~~
- ~~`PROTOCOL_VERSION`-Aenderungen mit Changelog erzwingen.~~
- ~~Golden-Codec-Tests fuer representative Packet-Binaerdaten ergaenzen.~~
- ~~Fuzz-/Boundary-Tests fuer Packet-Limits, Strings, Arraylaengen und unbekannte Packet-IDs ausbauen.~~
- ~~Optional Kompatibilitaetslayer fuer N-1 Client/Server frueh entscheiden.~~
- ~~Fehlertexte fuer LoginRejected/Protocol mismatch nutzerfreundlich machen.~~

Erledigt: 2026-05-01 - `ProtocolContract`, `docs/NETWORK_PROTOCOL.md`, Golden-Fixtures fuer alle aktuellen `PacketType`-IDs und freundlichere Protocol-Mismatch-Rejects.
Verifikation:
- `./gradlew :common:clean :common:test --tests dev.voxelgame.common.net.PacketCodecTest --tests dev.voxelgame.common.net.PacketCodecGoldenTest --tests dev.voxelgame.common.net.ProtocolContractTest --no-daemon --max-workers=1`
- `./gradlew :common:clean :server:cleanTest :server:test --tests dev.voxelgame.server.net.ServerConnectionHandlerTest.handshakeRejectsProtocolMismatchWithActionableReason --tests dev.voxelgame.server.net.NettyPacketCodecTest --no-daemon --max-workers=1`

## Akzeptanz

- Jede Packet-Aenderung hat einen Contract-Test.
- Alte oder manipulierte Frames werden sauber abgewiesen, ohne Server-Crash.
- Protocol-Versionen sind nachvollziehbar.

---

# P1 - Authoritative Action Pipeline

Status 2026-05-01: P1.1 erledigt durch Main Networking Dev - Common-Action-Modell und Projectile-Shoot-Server-Slice.

## Problem

`ServerConnectionHandler` validiert aktuell viele Intents direkt. Das funktioniert fuer Alpha, wird aber mit Essen, Fuettern, Tools, Projectiles, Spells, Stations, Status-Effekten und Quests zu monolithisch.

## P1.1 Common-Action-Modell und erster Server-Slice

Aktueller Teil:

- Common-Modell fuer `ActionDefinition`, `ActionRequest`, `ActionValidationResult`, `ActionExecutionResult`, `ActionCost`, `ActionCooldown` und `ActionTarget` einfuehren.
- Projectile-Shoot als bestehende serverkritische Aktion ueber ein kleines Server-Action-Modul routen.
- Bestehendes Verhalten fuer Validierung, Cooldown, Durability, Inventory-Snapshot und Entity-Snapshot beibehalten.

Aktueller Fortschritt 2026-05-01:

- ~~🔴 In Arbeit: `common.actions` mit `ActionDefinition`, `ActionRequest`, `ActionValidationResult`, `ActionExecutionResult`, `ActionCost`, `ActionCooldown`, `ActionTarget`, `ActionDefinitions` und `ActionPipeline`.~~
  Erledigt: 2026-05-01, Common-Action-Contract steht inklusive Default-Definitionen fuer Projectile-Shoot, Block-Interact und Eat.
  Verifikation: `./gradlew :common:test --tests dev.voxelgame.common.actions.ActionPipelineTest --tests dev.voxelgame.common.content.ContentTagRegistryTest -PadventuraTestRunId=action_pipeline_1 --no-daemon --max-workers=1`.
- ~~🔴 `ServerConnectionHandler` Projectile-Shoot auf den Pipeline-Contract routen, ohne bestehendes Verhalten zu aendern.~~
  Erledigt: 2026-05-01, `ServerProjectileShootAction` routet Projectile-Shoot ueber `common.actions.ActionPipeline`; der Handler ist nur noch Adapter fuer diese Aktion.
  Verifikation: `./gradlew :common:clean :server:cleanTest :server:test --tests dev.voxelgame.server.action.ServerProjectileShootActionTest --tests dev.voxelgame.server.net.ServerConnectionHandlerTest.projectileShootSpawnsServerAuthorizedProjectileAndDamagesLauncher --tests dev.voxelgame.server.net.ServerConnectionHandlerTest.projectileShootRejectsNonLauncherItem --tests dev.voxelgame.server.net.ServerConnectionHandlerTest.projectileShootRejectsForgedSlotOutsideInventory --tests dev.voxelgame.server.net.ServerConnectionHandlerTest.projectileShootSpamIsRateLimited --no-daemon --max-workers=1`.
- 🔴 Offen: Ergebnisse spaeter in `GameplayEventStream` ueberfuehren.

## Offen

- ~~Common-Modell einfuehren:~~
  - ~~`ActionDefinition`~~
  - ~~`ActionRequest`~~
  - ~~`ActionValidationResult`~~
  - ~~`ActionExecutionResult`~~
  - ~~`ActionCost`~~
  - ~~`ActionCooldown`~~
  - ~~`ActionTarget`~~
  Erledigt: 2026-05-01.
  Verifikation: `ActionPipelineTest`.
- Bestehende Aktionen schrittweise migrieren:
  - Block break/place.
  - Block interact.
  - Entity interact/feed/attack.
  - Eat/drink.
  - ~~Projectile shoot.~~
  - Craft/cook.
  - Sleep.
- ServerConnectionHandler auf Routing reduzieren.
- Action-Ergebnisse in GameplayEventStream ueberfuehren.
- Transaction-IDs fuer inventory-kritische Actions vereinheitlichen.

## Akzeptanz

- Neue serverkritische Aktion braucht keine neue grosse Handler-Verzweigung.
- Cooldowns, Costs, Durability und Result-Events sind testbar.
- Client kann Feedback anzeigen, ohne selbst Erfolg zu behaupten.

---

# P2 - Gameplay Event Stream

Status 2026-05-01: P2.1 ist fuer Packet/Codec, ersten Client-Consumer-Hook und ProjectileImpact-Server-Emission erledigt; Interest-Filter und weitere Gameplay-Producer bleiben Anschlussarbeit.

## Ziel

Audio, Partikel, UI-Feedback, Journal-Unlocks und Combat-Hits sollen serverseitig bestaetigte Ereignisse bekommen statt vieler ad-hoc Packet-Sonderfaelle.

## P2.1 Event-Stream Packet und Codec-Contract

Aktueller Teil:

- ~~🔴 In Arbeit: vorhandenes Common-`GameplayEvent`/`GameplayEventBatch` als `GamePacket.GameplayEvents` anbinden.~~
  Erledigt: 2026-05-01, `GamePacket.GameplayEvents` transportiert versionierte `GameplayEventBatch`-Payloads.
  Verifikation: `PacketCodecTest`, `PacketCodecGoldenTest`, `ProtocolContractTest`.
- ~~🔴 In Arbeit: `PacketType.GAMEPLAY_EVENTS`, Protocol-Version, `ProtocolContract`, Protocol-Doku und Golden-Codec-Fixture aktualisieren.~~
  Erledigt: 2026-05-01, `PacketType.GAMEPLAY_EVENTS` ist ID 27 und Protocol Version 24 dokumentiert den neuen Event-Stream.
  Verifikation: `ProtocolContractTest`.
- ~~🔴 In Arbeit: `PacketCodec` Encode/Decode inklusive Batch-Limit und focused Boundary-Test absichern.~~
  Erledigt: 2026-05-01, alle aktuellen `GameplayEvent`-Typen roundtrippen und Batch-Limits werden vor Allocation geprueft.
  Verifikation: `PacketCodecTest.roundTripsGameplayEvents` und `PacketCodecTest.rejectsGameplayEventCountBeyondCodecLimitBeforeAllocation`.
- ~~🟠 In Arbeit: Client-seitigen Consumer-Hook ergaenzen, damit das neue Server-Event-Packet nicht still im Default-Zweig verschwindet.~~
  Erledigt: 2026-05-01, `ClientConnectionHandler` reicht `GameplayEvents` bis `GameClientConnection`/`GameClient` weiter; `GameplayEventFeedback` mapped erste Events auf FeedbackLog, AudioCue und Pop-/Flash-Hooks.
  Verifikation: `ClientConnectionHandlerTest`, `GameplayEventFeedbackTest`.
- ~~🟠 ersten Server-Producer fuer autoritative ProjectileImpact-Events anbinden.~~
  Erledigt: 2026-05-01, `GameServer.tickEntities` sendet terminale Projectile-Hits zusaetzlich als `GameplayEvents`-Batch.
  Verifikation: `ServerConnectionHandlerTest.gameServerBroadcastsProjectileImpactFromAuthoritativeTick`.

## Offen

- ~~Common sealed `GameplayEvent` definieren.~~
  Erledigt: 2026-05-01, `GameplayEvent`, `GameplayEventType` und versionierter `GameplayEventBatch` stehen in `common.gameplay`.
  Verifikation: `./gradlew :common:test --tests dev.voxelgame.common.actions.ActionPipelineTest --tests dev.voxelgame.common.gameplay.GameplayEventTest --tests dev.voxelgame.common.content.ContentTagRegistryTest -PadventuraTestRunId=action_event_1 --no-daemon --max-workers=1`.
- ~~Packet `GameplayEvents(List<GameplayEvent>)` hinzufuegen.~~
  Erledigt: 2026-05-01.
  Verifikation: `PacketCodecTest`.
- ~~Event-Typen als Common-Records definieren:~~
  - ~~Damage.~~
  - ~~Heal.~~
  - ~~Hunger/Stamina/Breath critical.~~
  - ~~Pickup.~~
  - ~~Craft success/fail.~~
  - ~~Cook complete.~~
  - ~~Projectile impact.~~
  - ~~Sleep start/end.~~
  - ~~Weather thunder.~~
  - ~~Journal entry discovered.~~
  - ~~Recipe unlocked.~~
  - ~~Structure discovered.~~
  Erledigt: 2026-05-01 fuer Common- und Codec-Contract; serverseitige Producer pro Gameplay-System bleiben Anschlussarbeit.
  Verifikation: `GameplayEventTest`, `PacketCodecTest`.
- Client-Consumer fuer:
  - ~~AudioCue.~~
  - ParticleSystem.
  - ~~FeedbackLog.~~
  - HUD markers.
  - Journal update.
- Event-Relevanz nach Interest filtern.
- ~~Replay-/Smoke-Faehigkeit vorbereiten.~~
  Erledigt: 2026-05-01 fuer das Common-Modell durch monotone `sequence`, `debugKey()` und `GameplayEventBatch` Schema-Version.
  Verifikation: `GameplayEventTest`.

## Akzeptanz

- Server kann ein Ereignis einmal senden und mehrere Client-Subsysteme reagieren sauber darauf.
- Doppelte UI-/Audio-Trigger durch lokale Spekulation verschwinden.
- Events sind klein, versioniert und debugbar.

---

# P3 - Snapshot, Delta und Reconciliation

## Offen

- EntitySnapshots delta-faehig machen:
  - baseline sequence.
  - changed components.
  - despawn markers.
  - teleport/snap flag.
- PlayerPositionSnapshot mit Reconciliation-Grund und Severity sauber dokumentieren.
- Client-Snapshotbuffer debugbar machen:
  - raw vs interpolated.
  - buffer delay.
  - dropped/out-of-order snapshots.
- ChunkData langfristig section-/palette-/compression-faehig machen.
- BlockUpdate batches einfuehren, damit Burst-Mutationen nicht hunderte einzelne Packets erzeugen.
- InventorySnapshot durch transaction-aware deltas ergaenzen, ohne Full-Snapshot-Fallback zu verlieren.
- Netzwerkstats fuer bytes/sec, packets/sec, dropped/invalid, chunk queue, event queue und avg packet size ausbauen.

## Akzeptanz

- Bewegte Entities bleiben weich, ohne Server-Autoritaet aufzugeben.
- Grosse Block-/Entity-Aenderungen bleiben unter Budget.
- Debug-HUD zeigt, ob Netzwerk oder Simulation stottert.

---

# P4 - Interest Management V2

## Offen

- Chunk-, Entity-, Event- und BlockUpdate-Interest in einer gemeinsamen Interest-Struktur modellieren.
- Per-Client Subscriptions serialisieren:
  - loaded chunks.
  - visible chunks.
  - active block entities.
  - nearby entities.
  - open UI targets.
- Resend bei Client-Unload/Reload sauber testen.
- Prioritaeten fuer Chunk-Streaming:
  - spawn/login.
  - player movement direction.
  - visible ring.
  - retain ring.
  - background.
- Backpressure:
  - max chunks in flight.
  - max bytes per tick.
  - stale request cancellation.
  - disconnect-sichere Futures.
- Interest-Debug im ServerStatsSnapshot erweitern.

## Akzeptanz

- Server sendet keine entfernten Block-/Entity-/Eventdaten.
- Login und schnelle Bewegung verursachen keine unkontrollierten Chunk-Queues.
- Disconnect waehrend Chunkgen hinterlaesst keine offenen Jobs.

---

# P5 - Auth, Session und Reconnect

## Offen

- AuthProvider-Vertrag fuer spaetere OAuth/Steam-Identitaet konkretisieren.
- Username vs stable playerId sauber trennen.
- Session token / reconnect token planen.
- Duplicate login mit gleichem Account definieren:
  - altes Session kicken.
  - reconnect uebernehmen.
  - save flush.
- Whitelist/dev-auth klar als Dev-Modus markieren.
- Kick-/Ban-/Maintenance-Reasons im Protocol vorbereiten.
- Client Reconnect UX:
  - failed connect reason.
  - retry.
  - return to launcher/menu.
  - stale session message.

## Akzeptanz

- Player-Saves haengen nicht am frei waehlbaren Anzeigenamen.
- Reconnect fuehrt nicht zu Inventory- oder Position-Dupes.
- Auth-Ausbau braucht keinen Protocol-Neustart.

---

# P6 - Region Storage und Save Queue

## Problem

World-Saves sind derzeit Properties mit BlockChanges und BlockEntity-Snapshots. Das ist gut fuer Alpha-Smoke, skaliert aber nicht fuer lange Welten, viele Mutationen, Chunk-Sections, Lichtdaten oder Migrationen.

## Offen

- Region-File-Format planen:
  - Region-Koordinaten.
  - Chunk/Section records.
  - palette/coded block ids.
  - light arrays optional.
  - block entity payloads.
  - dirty flags.
  - compression flag.
- Atomic write und temp-file pattern beibehalten.
- Async Save Queue:
  - `save.write` Jobs.
  - max pending writes.
  - coalescing pro Region/Player.
  - shutdown flush.
  - failure retry/log.
- Save-Metriken:
  - queued writes.
  - written bytes/sec.
  - write ms.
  - failed writes.
  - last save age.
- Crash-Recovery:
  - migration backups.
  - partial temp cleanup.
  - corrupt region quarantine.
- PlayerSave und WorldSave Migrationen mit Testdaten fixieren.

## Akzeptanz

- Lange Erkundung erzeugt keine riesige einzelne Properties-Datei.
- Save IO blockiert Server-Ticks nicht.
- Welt kann nach Crash oder Migration nachvollziehbar geladen werden.

---

# P7 - BlockEntity Sync und Transactions

## Offen

- Gemeinsames BlockEntity-Snapshotmodell:
  - type.
  - position.
  - revision.
  - public state.
  - private UI state.
- Packets fuer BlockEntity open/update/close definieren.
- Storage/Campfire/CookingPot/Forge auf revisionierte Transaktionen bringen.
- 🔴 Braucht Main Networking Dev: Station-BlockEntity-Snapshots an `StationProgression` anbinden.
  Kontext: Gameplay P9.3 definiert BlockEntity-, Save- und Transaction-Contracts fuer Campfire, Workbench, Cooking Pot und Forge.
  Erwarteter Contract: revisionierter Snapshot/Update fuer Stationen mit `stationKey`, `type`, `position`, `revision`, `publicState`, `privateUiState` und Reject-Gruenden.
  Akzeptanz: UI kann je Station pending/accepted/rejected anzeigen, Forge/Pot/Campfire duplizieren Outputs nicht und Save/Load erhaelt aktive Station-State.
- Client-UI zeigt pending, accepted und rejected Transaktionen.
- Transaction-Reject-Gruende standardisieren:
  - out of range.
  - stale revision.
  - slot blocked.
  - missing item.
  - no fuel.
  - inventory full.
  - invalid station.
- Tests fuer Online-Storage und Stationen mit zwei Clients.

## Akzeptanz

- Keine Dupes durch parallele Storage-/Station-Interaktionen.
- UI kann einen Server-Reject klar erklaeren.
- BlockEntity-Sync laesst sich auf neue Stationen erweitern.

---

# P8 - Persistence fuer Game Design State

## Offen

- Journal-Fortschritt versioniert speichern:
  - notes.
  - discovered biomes.
  - discovered creatures.
  - discovered structures.
  - recipe unlock history.
  - lore pages.
  - map fragments.
- StatusEffects persistieren:
  - rested/cozy.
  - chilled/wet.
  - burning/poison optional.
- 🟠 Common-StatusEffect-Contract vorhanden: `StatusEffectState.saveStates()` liefert stabile `effectKey`, `remainingSeconds`, `intensity` und `tickProgressSeconds`; Persistenz/Packet/Server-Anwendung bleiben offen.
  Verifikation: `StatusEffectSystemTest.saveStatesRoundTripAndRejectUnknownKeys`.
- Quest-/Milestone-State vorbereiten.
- ~~🔴 Braucht Main Networking Dev: Persistenten `AlphaMilestoneKey`-State definieren.~~
  Kontext: Gameplay P9.1 liefert `AlphaMilestones` mit Save-State-Keys; lokale Client-Hints reichen fuer Reconnect/Multiplayer/Journal nicht aus.
  Erwarteter Contract: PlayerSave-Feld fuer erreichte Milestones, optional `GameplayEvent` fuer Milestone unlocked, Packet-/Codec-Test oder Save-Migration-Test.
  Akzeptanz: Milestones bleiben nach Neustart/Reconnect erhalten, werden nur serverbestaetigt freigeschaltet und koennen von Journal/HUD eindeutig angezeigt werden.
  Erledigt: 2026-05-01, `PlayerSave.achievedMilestones()` persistiert erreichte Alpha-Milestone-Keys, `ServerConnectionHandler` laedt/speichert sie im Player-Snapshot.
  Offen: Server-Producer, Unlock-Regeln und optionales `GameplayEvent` fuer Milestone unlocked.
  Verifikation: `PlayerSaveCodecTest`, `PlayerSaveStoreTest`.
- ~~🔴 Braucht Main Networking Dev: Persistenten `JournalProgression`-State definieren.~~
  Kontext: Gameplay P9.4 liefert Entry-/Goal-/Discovery-Keys fuer Journal, Lore, Map Fragments und Ruinenprogression; Client-only Journal-Unlocks waeren bei Reconnect und Multiplayer falsch.
  Erwarteter Contract: PlayerSave-Felder fuer erreichte `JournalEntryDefinition.key()`/`GoalDefinition.key()`, `GameplayEvent` fuer Journal entry unlocked, recipe history, map fragment und rare loot; Codec-/Save-Migration-Test.
  Akzeptanz: Journal/Lore/Goals bleiben nach Neustart/Reconnect erhalten, werden idempotent vom Server freigeschaltet und koennen von UI ohne eigene Discovery-Logik dargestellt werden.
  Erledigt: 2026-05-01, `PlayerSave.journalEntries()` bleibt der Entry-State; `PlayerSave.completedGoals()` ergaenzt persistierte Goal-Keys und roundtrippt im Codec.
  Offen: Discovery-/Recipe-/Loot-Server-Producer und idempotente Unlock-Regeln an `GameplayEventStream` anbinden.
  Verifikation: `PlayerSaveCodecTest`, `PlayerSaveStoreTest`.
- 🔴 Braucht Main Networking Dev: Persistenten `CozyLifeProgression`-/Friendship-State und serverautoritatives Feeding definieren.
  Kontext: Gameplay P9.5 liefert Creature-Rollen, Feed-Items, Friendship-Limits, Comfort-/Resource-/Danger-Contracts; Client-only Feeding wuerde Friendship, Cooldowns und passive Ressourcen duplizierbar machen.
  Erwarteter Contract: `FeedEntityAction` oder gleichwertiger Intent, PlayerSave/WorldSave-Felder fuer Friendship-Stufe, Tageslimit, Cooldown und optionale passive Shed-Ressourcen, `GameplayEvent` fuer feed accepted/rejected/friendship step.
  Akzeptanz: Feeding/Friendship bleibt nach Neustart/Reconnect erhalten, Tageslimit/Cooldown ist serverseitig, zwei Clients koennen dieselbe Creature nicht doppelt belohnen.
- Comfort- und Base-Zonen speichern, sobald eingefuehrt.
- World time, weather seed/state und one-shot events persistieren.
- LootTables fuer generated Chests idempotent speichern.

## Akzeptanz

- Exploration- und Progression-State geht nach Neustart nicht verloren.
- Generated Loot kann nicht mehrfach gepluendert werden.
- Game-Design-Systeme koennen speichern, ohne eigene Dateiformate zu erfinden.

---

# P9 - Security, Abuse und Operational Basics

## Offen

- Packet budget per connection.
- Intent-specific rate limits als Datenmodell statt verstreuter Konstanten.
- Movement cheat telemetry mit Strike-Fenster.
- Chat-Spam-Limit und spaetere Moderation hooks.
- Server command/admin channel planen.
- Metrics endpoint oder periodischer ServerStats dump fuer Headless-Server.
- Dedicated-server config file statt nur CLI args.
- Graceful shutdown:
  - kick reason.
  - save flush.
  - chunk job cancellation.
  - event-loop close.

## Akzeptanz

- Ein fehlerhafter oder manipulierter Client bringt den Server nicht aus dem Takt.
- Admin/Debug-Daten sind sichtbar genug fuer Alpha-Tests.
- Shutdown verliert keine offensichtlichen Daten.

---

# Tests / Smoke Checks

## Unit / Contract

- PacketCodec golden fixtures.
- invalid frame length and unknown packet id rejection.
- protocol mismatch.
- movement reject reasons.
- intent rate limit.
- inventory/storage transaction order.
- save migration v1->current.
- unknown item/block fallback.
- block entity unknown payload retention.
- region save write/read once P6 exists.

## Integration

- two clients breaking/placing in overlapping chunks.
- two clients interacting with same storage crate.
- reconnect same player after disconnect.
- server restart preserves player inventory, world time, block changes and block entities.
- chunk unload/resend after client movement.
- async chunk streamer disconnect while request in flight.

## Manual Smoke

- host server, join local client, gather/craft/store/cook, disconnect, reconnect.
- explore for 20 minutes, restart server, verify modified base and inventory.
- high movement speed around chunk borders, verify no missing chunks and no stale updates.
- intentionally wrong protocol client/test packet, verify clean reject.

## Akzeptanz gesamt

- Multiplayer bleibt serverautoritativ.
- Saves sind crash- und migrationsbewusst.
- Packet- und Save-Aenderungen sind getestet.
- Netzwerk-/Persistenzsysteme sind nicht in einem einzigen Handler gefangen.
