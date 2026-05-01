# Adventura – Engine TODO List

Stand: 2026-05-01
Bereich: Engine, Runtime, Streaming, Persistenz, Networking, Datenqualität, Entity-Runtime, Combat-Grundlagen

---

## Ziel

Diese Liste bündelt alle offenen Engine-Aufgaben aus Gameplay-Roadmap und Engine-Plan.

Der Fokus liegt auf:

- Stabilität
- Performance
- Messbarkeit
- sauberer Runtime-Struktur
- Save-/Load-Persistenz
- Multiplayer-Sicherheit
- Datenqualität
- besserer Erweiterbarkeit
- robustem Chunk-/Entity-/BlockEntity-Lifecycle

Wichtig: Diese Liste ist keine komplette Neuarchitektur. Bestehende Module und Patterns bleiben erhalten:

- `common`
- `client`
- `server`
- `launcher`
- `tools`
- zentrale Registries
- `GamePacket`
- `PacketCodec`
- `ClientWorld`
- `ServerWorld`
- `OverworldGenerator`
- `GameClient`
- `ServerConnectionHandler`

Neue Systeme sollen vorhandene Strukturen erweitern, nicht blind ersetzen.

---

# 0. Leitlinien

## Engine-Leitlinien

- Gameplay-Regeln gehören möglichst nach `common`.
- Rendering bleibt im `client`.
- Autoritative Welt-, Inventar-, BlockEntity- und Entity-Regeln gehören auf den `server`.
- Der Client sendet Intents, aber keine finalen Ergebnisse.
- Neue Features müssen in Singleplayer und Multiplayer konsistent funktionieren.
- Singleplayer darf lokal optimiert sein, soll aber dieselben Common-Regeln nutzen.
- Save-/Load-Kompatibilität ist wichtiger als kurzfristige Code-Vereinfachung.
- Keine neuen Shader-Sonderfälle über harte Block-IDs.
- Keine Engine-Änderung ohne Debug-, Test- oder Smoke-Test-Grundlage.

## Nicht-Ziele

Diese Engine-Liste soll nicht primär behandeln:

- Balancing einzelner Items
- finale Rezeptwerte
- Story-Inhalte
- reine UI-Optik
- Asset-Design
- Quest-Design

Diese Themen gehören in Gameplay-, UI- oder Content-Listen.

---

# P0 – Tooling, Tests und Messbarkeit

P0 ist kritisch. Ohne diese Grundlage sind spätere Engine-Änderungen schwer beweisbar.

---

## P0.1 JDK/Test-Gate reparieren

### Problem

Die lokale Testumgebung ist aktuell nicht zuverlässig, weil Java/JDK nicht sauber verfügbar ist. Dadurch können Engine-Fixes nicht sicher geprüft werden.

### Status

Bereits umgesetzt:

- Lokales JDK-21-Gate verifiziert.
- `./gradlew test` läuft mit sauberem Testoutput erfolgreich.
- `./gradlew buildGame` läuft nach sauberem Testoutput erfolgreich.
- Server-Netzwerk-Unit-Tests nutzen einen kleinen Test-Stream-Radius, damit Login-Tests nicht pro Test den vollen Produktions-Chunkradius generieren.
- Packet-Decoder-Tests prüfen Netty-`DecoderException` inklusive Ursache und vermeiden doppelte `ByteBuf`-Freigaben.

Offen:

- keine offenen P0.1-Punkte; Shell-Export-Hinweis unten beachten.

### Aufgaben

- ~~JDK 21 lokal verfügbar machen.~~
- ~~`JAVA_HOME` korrekt setzen.~~
- ~~Sicherstellen, dass `java` und `javac` im `PATH` verfügbar sind.~~
- ~~`./gradlew test` als Pflicht-Gate etablieren.~~
- ~~`./gradlew buildGame` als vollständiges Build-Gate nutzen.~~
- ~~README um klare Setup-Hinweise für Linux, Windows und macOS ergänzen.~~
- ~~Optional: kleines Troubleshooting für:~~
  - ~~fehlendes `JAVA_HOME`~~
  - ~~falsche Java-Version~~
  - ~~LWJGL/macOS `-XstartOnFirstThread`~~
  - ~~Gradle Wrapper Probleme~~

~~Hinweis: JDK 21 liegt lokal unter `/home/youngjibbit/.local/share/adventura-jdk/jdk-21.0.11+10`; die nicht vorbereitete Shell findet `java`/`javac` weiterhin erst nach `JAVA_HOME`/`PATH`-Export.~~

Status 2026-05-01: `gradlew` nutzt den lokalen Adventura-JDK-21-Pfad automatisch, wenn `JAVA_HOME` fehlt oder auf kein ausführbares `java` zeigt. Override bleibt über `ADVENTURA_JAVA_HOME` möglich.

### Akzeptanzkriterien

- ~~`./gradlew test` läuft lokal erfolgreich.~~
- ~~`./gradlew buildGame` läuft lokal erfolgreich.~~
- ~~Neue PRs können mit Tests geprüft werden.~~
- ~~README erklärt JDK 21 eindeutig.~~

---

## P0.2 Engine-Messpunkte zentralisieren

### Problem

Engine-Messwerte sind teilweise verstreut. Dadurch ist schwer zu erkennen, ob Rendering, Meshing, Chunkgen, Lighting oder Networking regressieren.

### Ziel

Ein zentraler Runtime-Stats-Datensatz soll alle wichtigen Messpunkte bündeln.

### Aufgaben

- ~~`EngineFrameStats` oder ähnlichen Record einführen.~~
- ~~Messlogik aus `GameClient` herausziehen.~~
- ~~Debug-Overlay nur noch aus zentralen Stats lesen lassen.~~
- ~~Stats sauber pro Frame aktualisieren.~~
- ~~Optional gleitende Durchschnittswerte anzeigen.~~

Status 2026-04-30: `ClientWorld`, `WorldRenderer`, `ParticleSystem`, `ClientPacketDecoder` und `RenderResourceTracker` liefern zentrale Frame-/Queue-/Timing-/Netzwerk-/GPU-Werte an `EngineFrameStats`; das Debug-Overlay zeigt Queue, Section, Timing, Partikelbudget, Packetgröße, ungültige Packets und GPU-Ressourcen.

### Zu messende Werte

#### Frame / Runtime

- ~~FPS~~
- ~~Framezeit in ms~~
- ~~Updatezeit~~
- ~~Renderzeit~~
- ~~UI-Zeit optional~~
- ~~Client-Tickzeit~~
- ~~Server-Tickzeit bei lokalem Singleplayer optional~~

#### Chunk / World

- ~~Loaded Chunks~~
- ~~Visible Chunks~~
- ~~Dirty Chunks~~
- ~~Queued Chunks~~
- ~~Built Chunks pro Frame~~
- ~~Unloaded Chunks~~
- ~~Chunkgen-Zeit~~
- ~~Meshing-Zeit~~
- ~~Lighting-Zeit~~
- ~~GPU-Upload-Zeit~~

#### Rendering

- ~~Draw Calls~~
- ~~Triangles~~
- ~~Rendered Mesh Layers~~
- ~~Culled Chunks~~
- ~~Culled Meshes~~
- ~~Solid Mesh Count~~
- ~~Cutout Mesh Count~~
- ~~Transparent Mesh Count~~
- ~~geschätzter VRAM-Verbrauch~~

#### Entities / Particles

- ~~Entity Count~~
- ~~Visible Entity Count~~
- ~~Culled Entity Count~~
- ~~Particle Count~~
- ~~Particle Spawn Rate~~
- ~~Particle Budget Usage~~

Status 2026-04-30: `ParticleSystem.RenderStats` zählt Spawn-Rate, Budget Usage und verdrängte Partikel.

#### Netzwerk

- ~~eingehende Packets pro Sekunde~~
- ~~ausgehende Packets pro Sekunde~~
- ~~durchschnittliche Packetgröße~~
- ~~verworfene ungültige Packets~~
- ~~Chunk-Stream Queue Länge~~

Status 2026-04-30: Client-Netzwerkstats erfassen Payload-Bytes, Decoder-Rejects und die aktuell wartende Chunk-Stream-/Build-Queue-Länge.

### Akzeptanzkriterien

- ~~Debug-Overlay zeigt Engine-Stats sauber und verständlich.~~
- ~~Regressionen bei Chunkload, Meshing oder Rendering sind sichtbar.~~
- ~~Stats-Code liegt nicht verstreut im Main-Loop.~~
- ~~Keine spürbare Performance-Belastung durch Messung.~~

---

## P0.3 Reproduzierbare Testwelten definieren

### Ziel

Engine-Änderungen brauchen feste Seeds, damit Fehler wiederholbar sind.

### Aufgaben

Eine feste Liste von Test-Seeds definieren für:

- ~~Spawn / Cozy Meadow~~
- ~~River / Lakeside~~
- ~~Pine Forest~~
- ~~Mushroom Grove~~
- ~~Old Ruins~~
- ~~Highlands / Ores~~
- ~~Frost Peaks~~
- ~~Desert / Dunes~~
- ~~Cave-heavy World~~
- ~~Village / Structure-heavy World~~
- ~~Water-heavy World~~
- ~~Performance-Stresstest~~

### Pro Testwelt dokumentieren

- ~~Seed~~
- ~~erwartetes Startbiom~~
- ~~relevante Koordinaten~~
- ~~welche Engine-Funktion geprüft wird~~
- ~~erwartete Probleme oder bekannte Edge Cases~~

### Smoke-Checkliste pro Seed

- ~~Spawn ist sicher.~~
- ~~Spieler steht nicht in Blöcken.~~
- ~~Spieler spawnt nicht im Wasser.~~
- ~~Terrain hat keine sichtbaren Seams.~~
- ~~Wasser rendert korrekt.~~
- ~~Biome sind sichtbar unterschiedlich.~~
- ~~Structures erscheinen deterministisch.~~
- ~~Entities erscheinen plausibel.~~
- ~~Debugwerte sind plausibel.~~
- ~~Chunk-Unload erzeugt keine Mesh-Leaks.~~

### Akzeptanzkriterien

- ~~Mindestens 8 feste Testwelten sind dokumentiert.~~
- ~~Jeder größere Engine-PR kann gegen diese Seeds geprüft werden.~~
- ~~Seeds werden nicht willkürlich geändert.~~

---

## P0.4 Basis-Bugtests erweitern

### Bereits sinnvoll angelegte oder geplante Tests

- ~~`PlayerCollisionTest`~~ (`ClientWorldCollisionTest`, `ServerWorldTest`)
- ~~`SpawnPositionTest`~~ (`ClientWorldSpawnTest`)
- ~~`TransparentRenderOrderTest`~~ (`WorldRendererTest`)
- ~~`WorldgenFeaturePlacementTest`~~ (`OverworldContentTest`, `ReproducibleWorldSeedsTest`)

### Neue empfohlene Tests

#### Chunk Lifecycle

- ~~Chunk wird geladen.~~
- ~~Chunk wird sichtbar.~~
- ~~Chunk wird dirty.~~
- ~~Mesh wird neu gebaut.~~
- ~~Chunk wird entladen.~~
- ~~GPU-Mesh wird freigegeben.~~
- ~~erneutes Laden erzeugt keine doppelten Meshes.~~

Hinweis 2026-05-01: Echter GPU-Release bleibt zusätzlich ein manueller GL-Smoke-Test, weil Unit-Tests ohne OpenGL-Kontext nur Release-Stats/Tracker prüfen.

#### BlockEntity Lifecycle

- ~~BlockEntity wird erstellt.~~
- ~~BlockEntity wird geändert.~~
- ~~BlockEntity wird gespeichert.~~
- ~~BlockEntity wird geladen.~~
- ~~BlockEntity wird entfernt, wenn Block zerstört wird.~~
- ~~BlockEntity bleibt nicht als Zombie-Datenstruktur erhalten.~~

Status 2026-04-30: `ServerWorld.BlockEntitySnapshot` speichert/lädt Storage-Crates, Campfire-Fuel-Restzeit und verbrauchte generierte Loot-Crates; `ServerWorldTest` deckt Storage/Campfire-Save-Load ab. Vollständige Dateipersistenz bleibt P2.

#### Packet Safety

- ~~zu kleine Packets werden abgelehnt.~~
- ~~zu große Packets werden abgelehnt.~~
- ~~trailing bytes werden abgelehnt.~~
- ~~zu lange Strings werden abgelehnt.~~
- ~~zu große Listen werden abgelehnt.~~

### Akzeptanzkriterien

- ~~Kritische Engine-Regeln haben Tests.~~
- ~~Fehler lassen sich lokal reproduzieren.~~
- ~~Tests laufen im normalen Gradle-Gate.~~

---

# P1 – Client-Engine und Chunk-Lifetime

P1 sorgt dafür, dass längeres Erkunden stabil bleibt und die Client-Engine nicht mit jedem Feature unkontrolliert wächst.

---

## P1.1 Chunk-Unload finalisieren

### Status

Bereits umgesetzt:

- Singleplayer-Client entlädt Chunks außerhalb eines Kamera-Retain-Radius mit Render-/Preview-Hysterese.
- Geänderte Chunks bleiben bis Save/Load gepinnt, damit lokale Block-Edits nicht durch Regeneration verloren gehen.
- `WorldRenderer` kann GPU-Meshes entladener Chunks gezielt freigeben.
- Ambient-/Runtime-Daten außerhalb geladener Chunks werden beim Unload bereinigt.
- Debug Overlay zeigt Loaded/Rendered/Unloaded Chunks sowie freigegebene Mesh-Layer.
- Geschlossene `GpuChunkMesh`-Instanzen werfen bei `draw()` sofort einen Fehler statt still auf gelöschte GL-Handles zuzugreifen.
- `ClientWorldMeshInvalidationTest` deckt Sprint-/Teleport-ähnliche Retain-Radius-Wechsel, Rückkehr/Rebuild, Runtime-Entity-Cleanup und gepinnte lokale Änderungen ab.
- Multiplayer-Unload bleibt absichtlich deaktiviert, bis ein Interest-/Resend-Protokoll existiert.

### Offene Aufgaben

- ~~Multiplayer-Unload erst aktivieren, wenn Server-Resend/Interest-Protokoll abgesichert ist.~~
- ~~Langer manueller Explore-Smoke-Test nach grünem Full-Gate.~~
- ~~Unload-Hysterese balancen.~~
- ~~Verhalten bei schnellem Fliegen/Sprinten testen.~~
- ~~Verhalten bei Teleport testen.~~
- ~~Verhalten bei Chunk-Rückkehr testen.~~
- ~~prüfen, ob Ambient Entities korrekt entfernt oder neu synchronisiert werden.~~
- ~~prüfen, ob lokale geänderte Chunks korrekt gepinnt bleiben.~~

### Risiken

- ~~Multiplayer-Client entlädt Chunk und bekommt danach kein korrektes Resend.~~
- ~~lokal geänderte Chunks werden versehentlich regeneriert.~~
- ~~GPU-Meshes bleiben trotz Unload im Speicher.~~
- ~~Entity-Snapshots referenzieren entladene Chunks.~~

### Akzeptanzkriterien

- ~~20 Minuten Erkunden erhöht Loaded Chunks nicht unbegrenzt.~~
- ~~GPU-Mesh-Anzahl wächst nicht unbegrenzt.~~
- ~~Rückkehr in altes Gebiet lädt Chunks korrekt neu.~~
- ~~lokale Blockänderungen bleiben erhalten.~~
- ~~keine GL-Fehler beim Unload/Reload.~~

---

## P1.2 Chunk Build Queue

### Problem

Chunkgen, Lighting und Meshing dürfen nicht dauerhaft synchron im Client-Loop hängen, sonst entstehen Frame-Spikes.

### Ziel

Ein priorisiertes Build-System für Chunks.

### Aufgaben

- ~~`ChunkBuildQueue` einführen.~~
- ~~Priorität nach Kamera-/Spielerposition.~~
- ~~Priorität für sichtbare Chunks höher als entfernte Preview-Chunks.~~
- ~~CPU-Build optional backgroundfähig machen.~~
- ~~GPU-Upload bleibt ausschließlich im Render Thread.~~
- ~~Budget nicht nur als Chunks pro Frame, sondern als ms-Zeitbudget steuerbar machen.~~
- ~~Debug Overlay um Queue-Stats erweitern.~~

Status 2026-04-30: `ChunkBuildQueue` dedupliziert Rebuilds, priorisiert nach Spieler/Kamera und Render-/Preview-Band, respektiert Chunk- und ms-Budget, misst Wait/Build/Gen/Light/Upload und lässt GPU-Uploads im `WorldRenderer`-Renderpfad.

### Queue-Prioritäten

1. Chunk direkt unter/um Spieler
2. Chunks im Sichtfeld
3. nahe Chunks außerhalb Sichtfeld
4. Preview-/Horizon-Chunks
5. niedrig priorisierte Rebuilds

### Zu messende Werte

- ~~Queue Länge~~
- ~~Build-Zeit~~
- ~~Upload-Zeit~~
- ~~abgebrochene Builds~~
- ~~ersetzte Builds~~
- ~~durchschnittliche Wartezeit~~
- ~~Chunks built per second~~

### Akzeptanzkriterien

- ~~neue Chunks verursachen weniger Frame-Spikes.~~
- ~~sichtbare Chunks werden priorisiert.~~
- ~~GPU-Uploads passieren nur im Render Thread.~~
- ~~Debug Overlay zeigt Build Queue Zustand.~~

---

## P1.3 Section-Aware Chunk-Daten

### Problem

Chunks werden aktuell zu grob behandelt. Hohe Chunks, Höhlen und leere Bereiche erzeugen unnötige Bounds und potenziell unnötige Arbeit.

### Ziel

Chunks intern stärker in Sections/Layers denken.

### Aufgaben

- ~~leere Sections schneller überspringen.~~
- ~~Mesh-Bounds pro Section vorbereiten.~~
- ~~optional Meshes pro Section statt nur pro Chunk prüfen.~~
- ~~Frustum-Culling mit realistischeren Bounds vorbereiten.~~
- ~~Section-Daten für Lighting und Meshing nutzbar machen.~~
- ~~Debug Overlay optional um section stats erweitern.~~

Status 2026-04-30: `ChunkMesher` überspringt leere `ChunkSection`s, `ClientWorld.verticalBounds()` liefert Section-basierte Bounds, `WorldRenderer` nutzt diese für Frustum-AABBs, und das Overlay zeigt Section-Zähler.

### Vorteile

- ~~weniger unnötige Draw Calls~~
- ~~besseres Culling bei Bergen/Höhlen~~
- ~~präzisere Mesh-Bounds~~
- ~~bessere Vorbereitung für größere Welthöhen~~

### Akzeptanzkriterien

- ~~leere vertikale Bereiche verursachen weniger Mesh-/Render-Arbeit.~~
- ~~Section-Bounds sind korrekt.~~
- ~~keine sichtbaren Chunk-Holes.~~
- ~~keine Regression bei normalen flachen Gebieten.~~

---

## P1.4 GL Resource Tracking

### Status

Bereits umgesetzt:

- Geschlossene `GpuChunkMesh`-Instanzen werfen bei `draw()` einen Fehler.
- `RenderResourceTracker` zählt Chunk-VAOs/-VBOs, Entity-VAOs/-Buffers, Particle-VAOs/-Buffers, Texturen, Shader-Programme und vorbereitete Framebuffer-Zähler.
- Chunk-, Entity-, Particle-, Texture-Atlas-, Material-LUT- und Shader-Lifetimes registrieren Release/Dispose.
- Debug Overlay zeigt aktive GPU-Ressourcen inklusive Chunk-, Entity-, Particle-, Texture-, Shader- und Framebuffer-Zählern.

### Offene Aufgaben

- ~~VAO-Lebenszeit tracken.~~
- ~~VBO-Lebenszeit tracken.~~
- ~~Texture-Lebenszeit tracken.~~
- ~~Shader-Program-Lebenszeit tracken.~~
- ~~Debug-Zähler für aktive GPU-Ressourcen anzeigen.~~
- ~~Mesh dispose im Chunk-Unload hart prüfen.~~
- ~~Smoke-Test für längeres Erkunden mit Chunk-Unload.~~
- ~~optional Leak-Warnung bei Shutdown.~~

### Zu trackende Ressourcen

- ~~Chunk VAOs~~
- ~~Chunk VBOs~~
- ~~Entity Mesh Buffers~~
- ~~Particle Buffers~~
- ~~Texture Atlases~~
- ~~Shader Programs~~
- ~~Framebuffers falls später genutzt~~

### Akzeptanzkriterien

- ~~aktive GPU-Ressourcen sind im Debug sichtbar.~~
- ~~Unload verringert Mesh-/Buffer-Zähler.~~
- ~~Shutdown meldet keine offensichtlichen Leaks.~~
- ~~versehentliche Draws auf disposed Meshes fallen sofort auf.~~

---

# P2 – Save/Load und Persistenz

P2 ist eines der wichtigsten Themen für ein echtes Spielgefühl. Ohne Persistenz fühlen sich Storage, Loot, Campfires, Basebuilding und Exploration unfertig an.

---

## P2.1 World Save

### Ziel

Die Welt muss dauerhaft veränderbar werden.

### Zu speichern

- ~~World Seed~~
- ~~World Version~~
- ~~geänderte Blöcke~~
- ~~entfernte generierte Blöcke~~
- ~~platzierte Spielerblöcke~~
- ~~Block Entities~~
- ~~Storage Crates~~
- ~~Campfires~~
- ~~Cooking Stations~~
- ~~Forges~~
- ~~Loot Chests~~
- ~~geöffnete Loot States~~
- Structure State
- Structure IDs
- ~~erzeugte Loot Marker~~
- optional wichtige Entities

### Empfohlenes Modell

~~Nicht jeden generierten Block speichern.~~

Stattdessen:

- ~~generierte Welt bleibt aus Seed reproduzierbar~~
- ~~nur Diffs speichern:~~
  - ~~Block geändert~~
  - ~~Block entfernt~~
  - ~~Block platziert~~
  - ~~BlockEntity verändert~~
  - ~~Loot generiert/geöffnet~~
  - Structure State verändert

### Erreicht

- ~~`WorldSaveStore` speichert/lädt Seed, Save-/World-Version, Daytime, Block-Diffs und BlockEntity-Snapshots.~~
- ~~Block-Diffs nutzen Registry-Keys statt instabiler IDs und laden unbekannte Blocks sicher als `voxel:air`.~~
- ~~Storage-Crate-Inhalte, Campfire-Restzeit und verbrauchte generierte Loot-Crates werden roundtrip-getestet.~~
- ~~Serverstart unterstützt `--save <datei>`; Shutdown schreibt den World-Save.~~

### Risiken

- ~~alte Weltstände brechen bei Registry-Änderungen.~~
- ~~unbekannte Blocks/Items machen Saves unladbar.~~
- ~~Loot wird nach Neustart neu generiert.~~
- ~~BlockEntities bleiben ohne Block bestehen.~~
- aktive In-Flight-Cooking-Jobs sind aktuell noch Connection-Runtime und werden nicht als Station-Job fortgesetzt.
- allgemeiner Structure-State außer Loot-Verbrauch ist vorbereitet, aber noch nicht als eigener Save-Bereich modelliert.

### Akzeptanzkriterien

- ~~platzierte Blöcke bleiben nach Neustart.~~
- ~~abgebaute Blöcke bleiben entfernt.~~
- ~~Storage-Inhalte bleiben erhalten.~~
- ~~Loot-Chests resetten nicht.~~
- ~~Campfire/Station State bleibt korrekt.~~
- ~~unbekannte Items/Blocks crashen Save nicht.~~

---

## P2.2 Player Save

### Ziel

Spielerfortschritt muss dauerhaft erhalten bleiben.

### Zu speichern

- ~~Spieler-ID / Name~~
- ~~Position~~
- ~~Rotation optional~~
- ~~Inventory~~
- ~~Hotbar Selection~~
- ~~Health~~
- ~~Hunger~~
- ~~Stamina~~
- ~~Breath~~
- ~~Spawn Point~~
- ~~Gamemode~~
- ~~entdeckte Rezepte~~
- ~~entdeckte Biome~~
- ~~Journal Entries~~
- bekannte Structures optional
- ~~zuletzt besuchte Welt~~
- persönliche Settings optional getrennt

### Wichtig

~~Comfort sollte nicht dauerhaft gespeichert werden, sondern aus Umgebung neu berechnet werden. Dadurch verhindert man veraltete oder manipulierte Comfort-Werte.~~

### Erreicht

- ~~`PlayerSaveCodec` speichert/lädt Position, Rotation, Inventory, Hotbar, Survival-Werte, Spawn/Gamemode und Progressionslisten.~~
- ~~`PlayerSaveStore` lädt Spieler beim Login und schreibt Snapshots bei Disconnect/Server-Close.~~
- ~~Comfort wird absichtlich nicht persistiert und beim Login aus der Welt neu berechnet.~~
- ~~Ungültige/fehlende Items werden beim Laden sicher entfernt; Item-Aliase werden angewendet.~~

### Akzeptanzkriterien

- ~~Spieler startet nach Neustart an korrekter Position.~~
- ~~Inventory bleibt erhalten.~~
- ~~Survival-Werte bleiben erhalten.~~
- ~~entdeckte Rezepte bleiben erhalten.~~
- ~~Journal bleibt erhalten.~~
- ~~ungültige Items werden sicher ersetzt oder entfernt.~~

---

## P2.3 Versioniertes Save-Format

### Ziel

Saves müssen zukünftige Updates überleben.

### Aufgaben

- ~~Save-Version einführen.~~
- ~~Save-Metadaten speichern:~~
  - ~~Spielversion~~
  - ~~Save-Version~~
  - ~~World Seed~~
  - ~~Erstellungszeit~~
  - ~~letzte Ladezeit~~
- Backup vor Migration.
- ~~Migration Hooks vorbereiten.~~
- ~~Unknown Block Fallback.~~
- ~~Unknown Item Fallback.~~
- ~~Unknown BlockEntity Fallback.~~
- ~~BlockEntity Type IDs definieren.~~
- ~~Registry-Aliase beim Laden anwenden.~~

### Empfohlene Fallbacks

- ~~unbekanntes Item → sicher löschen beim Laden~~
- ~~unbekannter Block → `air`~~
- ~~unbekannte BlockEntity → Daten behalten, aber nicht aktivieren~~
- ~~unbekanntes Rezept → ignorieren, Progression nicht crashen~~

### Akzeptanzkriterien

- ~~Save-Datei hat Versionsnummer.~~
- ~~Migration kann später ergänzt werden.~~
- ~~alte Item-Keys können auf neue Keys gemappt werden.~~
- ~~Save-Load crasht nicht bei unbekannten Keys.~~

---

# P3 – BlockEntity-System vereinheitlichen

P3 macht alle interaktiven Blöcke robuster.

---

## P3.1 Gemeinsamen BlockEntityStore ausbauen

### Ziel

Alle Blöcke mit Zustand sollen ein einheitliches Runtime- und Save-Modell nutzen.

### BlockEntity-Typen

- ~~`StorageCrate`~~
- ~~`Campfire`~~
- ~~`CookingPot`~~
- ~~`Forge`~~
- `LootCrate`
- `Workbench` optional
- `Bookshelf` optional
- `SleepingMat` optional mit Spawn-Daten
- `MarketCrate` optional

### Aufgaben

- ~~gemeinsamen `BlockEntityStore` ausbauen.~~
- ~~BlockEntity Type IDs definieren.~~
- ~~BlockEntity Position eindeutig speichern.~~
- ~~Create/Update/Delete Lifecycle definieren.~~
- ~~BlockEntity wird gelöscht, wenn Block zerstört wird.~~
- ~~BlockEntity wird erstellt, wenn passender Block platziert wird.~~
- ~~Server bleibt autoritativ.~~
- ~~Client bekommt nur Snapshots/UI-Daten.~~
- ~~Save/Load für alle BlockEntities vorbereiten.~~

### Erreicht

- ~~`BlockEntityType` definiert stabile Type-Keys für Storage, Campfire, CookingPot, Forge, Workbench und SleepingMat.~~
- ~~`BlockEntityStore` synchronisiert Create/Update/Delete aus Block-Änderungen und kann unbekannte zukünftige Typen inert erhalten.~~
- ~~`ServerWorld` serialisiert Store-Snapshots zusammen mit Storage-/Campfire-/Loot-State.~~

### Akzeptanzkriterien

- ~~StorageCrate, Campfire und CookingStation nutzen dasselbe Grundsystem.~~
- ~~zerstörter Block entfernt BlockEntity.~~
- ~~platzierter Block erstellt BlockEntity.~~
- ~~Client kann keine BlockEntity-Daten fälschen.~~
- ~~Save/Load kann BlockEntities serialisieren.~~

---

## P3.2 BlockEntity UI-Sync

### Ziel

Interaktive Blöcke brauchen sichere und klare UI-Synchronisierung.

### Aufgaben

- ~~Open Request pro BlockEntity.~~
- ~~Server prüft:~~
  - ~~Block existiert.~~
  - ~~BlockEntity existiert.~~
  - ~~Spieler ist in Reichweite.~~
  - ~~Spieler darf interagieren.~~
- ~~Server sendet Snapshot.~~
- ~~Client zeigt UI.~~
- ~~Transfer Requests enthalten:~~
  - ~~source container~~
  - ~~source slot~~
  - ~~target container~~
  - ~~target slot optional~~
  - ~~count~~
  - ~~transaction id~~
- ~~Server antwortet mit aktualisiertem Snapshot.~~
- ~~ungültige Transaktion wird abgelehnt.~~

### Erreicht

- ~~Storage-Open/Transfer nutzt Transaktions-IDs, Range-Checks und serverautorisierte Snapshots.~~
- ~~CookingPot- und Forge-CookRequests prüfen Block, BlockEntity-Typ, Reichweite und Rezeptstation serverseitig.~~
- ~~Far-away Storage/Cooking/Forge-Interaktionen und Replay-Transaktionen sind getestet.~~

### Akzeptanzkriterien

- ~~Kisten-UI synchronisiert sicher.~~
- ~~Cooking UI synchronisiert sicher.~~
- ~~Forge UI synchronisiert sicher.~~
- ~~ungültige Slot Moves erzeugen keine Dupes.~~
- ~~far away Interaction wird abgelehnt.~~

---

# P4 – Multiplayer Engine

P4 stärkt Server-Autorität, Streaming, Movement und Netzwerk-Sicherheit.

---

## P4.1 Interest Management

### Bereits erreicht

- Broadcasts gehen nur noch an authentifizierte Verbindungen.
- Broadcasts für Weltzustand/Entities sind auf die jeweilige `ServerWorld` begrenzt.
- Client-Netzwerkdecoder begrenzt eingehende Packet-Größe auf 2 MiB.
- Client- und Server-Decoder verwerfen zu kleine ungültige Packet-Längen.
- Decoder-Grenzwerte sind im Shared-Code `PacketLimits` zentralisiert.
- Netty-Decoder nutzen `PacketLimits.MIN_PACKET_SIZE` und `PacketLimits.MAX_PACKET_SIZE`.
- `PacketCodec` prüft Listen-/Array-Längen gegen Maximalwerte und verbleibendes Payload-Budget.
- Packet-Textfelder für Login, Recipe Keys und Chat haben domänenspezifische Blank-/Längenlimits.
- Protokoll-Decoder lehnt trailing bytes nach Paketdecode ab.
- Entity-Snapshots werden pro Verbindung auf eigene Spieler-Entity und Radius-Interest gefiltert.
- Chunk-Subscriptions werden pro Client geführt und Blockupdates daran gefiltert.
- Server-Interest-Stats erfassen Snapshots, Chunk-Packets, Blockupdates, Drops außerhalb Interest, Packet-Schätzung und Packet-Rate.
- `GameServer.interestStats()` stellt die aktuellen Server-Interest-Werte für Logs/Debug-Auswertung bereit.
- `ServerStatsSnapshot` überträgt Interest-/Packet-Stats pro Verbindung an den Client.
- Client-Debug-HUD zeigt Serverwerte in der `SRVSTAT`-Zeile.

Status 2026-05-01: Interest-Filterung ist aktiv, serverseitig messbar, per Debug-Packet im Client sichtbar und durch `ServerConnectionHandlerTest` abgesichert.

~~Hinweis: Für Anzeige im Client-Debug-Overlay fehlt noch ein dediziertes ServerStats-/Debug-Packet. Die Engine-Seite liefert die Daten bereits serverseitig.~~

### Aufgaben

- ~~Entity-Snapshots nur an relevante Spieler senden.~~
- ~~Chunk-Updates nur an Spieler senden, die Chunk geladen haben.~~
- ~~Block-Updates nach Chunk-/Radius-Interest filtern.~~
- ~~Partielle Snapshot-Updates statt immer vollständiger Listen prüfen.~~
- ~~Chunk-Subscription pro Client verwalten.~~
- ~~Interest-Bereich mit Render-/Simulation-Distance koppeln.~~
- ~~Debug Overlay / Server Log um Interest-Stats erweitern.~~

Hinweis zu partiellen Snapshots: Für das bestehende Protokoll bleiben gefilterte Full-Snapshot-Listen absichtlich erhalten; Delta-Snapshots lohnen erst mit einem separaten Entity-Diff-Packet.

### Zu messende Werte

- ~~gesendete Entity Snapshots pro Client~~
- ~~gesendete Chunk Packets pro Client~~
- ~~verworfene Updates außerhalb Interest~~
- ~~durchschnittliche Packetgröße~~
- ~~Packetrate pro Client~~
- ~~Chunk subscriptions pro Client~~

### Akzeptanzkriterien

- ~~Spieler erhalten keine unnötigen Entity-Snapshots aus weit entfernten Gebieten.~~
- ~~Spieler erhalten keine Blockupdates aus nicht geladenen Chunks.~~
- ~~Multiplayer skaliert besser mit mehreren Spielern.~~
- ~~Debugwerte zeigen Interest-Filterung.~~

---

## P4.2 Async Server Chunk Generation

### Problem

Chunk-Erzeugung darf Netty-Handler nicht blockieren.

### Ziel

Server-Chunkgen läuft über Worker Queue.

### Aufgaben

- ~~Server Chunk Request Queue einführen.~~
- ~~Worker Threads für Chunkgen nutzen.~~
- ~~Packet-Auslieferung nach Fertigstellung.~~
- ~~Priorität nach Spielerposition.~~
- ~~Requests deduplizieren.~~
- ~~bereits laufende Generation nicht mehrfach starten.~~
- ~~Fehler sauber behandeln.~~
- ~~maximale Queue-Größe definieren.~~
- ~~Backpressure bei zu vielen Requests.~~

Status 2026-05-01: `ServerChunkStreamer` streamt Chunks über priorisierte Worker-Queue, dedupliziert In-Flight-Requests, begrenzt Queue/In-Flight-Druck und liefert fertige Packets zurück auf den Channel-EventLoop. Tests prüfen Dedupe und Backpressure.

### Risiken

- ~~Race Conditions bei gleichzeitigen Chunk Requests.~~
- ~~doppelte Chunkgenerierung.~~
- ~~Chunk wird an Spieler gesendet, der ihn nicht mehr braucht.~~
- ~~Server schickt veralteten Chunk nach World Edit.~~

### Akzeptanzkriterien

- ~~Netty-Handler bleibt responsiv.~~
- ~~Chunkgen blockiert nicht den Netzwerkthread.~~
- ~~doppelte Requests werden zusammengeführt.~~
- ~~Chunks werden nur an relevante Clients gesendet.~~
- ~~Server bleibt bei schnellem Bewegen stabil.~~

---

## P4.3 Authoritative Movement vorbereiten

### Ziel

Movement soll langfristig server-validiert oder server-simuliert werden.

### Aktueller Stand

Server prüft bereits grundlegende Plausibilität:

- ~~finite Position/Rotation~~
- ~~vertikale World-Bounds~~
- ~~Initial-Sync-Radius~~
- ~~grobe Delta-Grenzen~~

Zusätzlich erreicht:

- ~~Kollision~~
- ~~Ground-State~~
- ~~Wasserzustand~~
- ~~Speed anhand von Movement Mode~~
- ~~Reconciliation~~
- ~~Sequenznummern~~
- ~~Movement-Rate-Limit~~
- ~~Fallschaden serverseitig~~
- ~~Creative/Flying- und Spectator-Movement-Mode-Validierung~~

### Aufgaben

- ~~Server nutzt gemeinsame Physics-Regeln zur Validierung.~~
- ~~Client sendet Movement mit Sequenznummer.~~
- ~~später optional Input-State statt finaler Position.~~
- ~~Server antwortet mit autoritativem State.~~
- ~~Client korrigiert Position sanft.~~
- ~~Teleport/Spawn/Respawn sauber vom normalen Movement unterscheiden.~~
- ~~Movement Modes validieren:~~
  - ~~survival~~
  - ~~creative~~
  - ~~spectator~~
- ~~Movement-Rate limitieren.~~
- ~~Speed-Hacks erkennen.~~
- ~~Fallschaden serverseitig sicher berechnen.~~

Status 2026-05-01: Server-Movement nutzt `PlayerMovementRules`, Sequenzen, authoritative snapshots, Rate-Limits, Kollisions-/Ground-/Water-Checks, Mode-Delta-Regeln und serverseitigen Fallschaden. Creative wird als Flying validiert, Spectator darf Kollisionen durchqueren.

### Akzeptanzkriterien

- ~~extreme Teleports werden abgelehnt.~~
- ~~zu schnelle Bewegung wird abgelehnt oder korrigiert.~~
- ~~survival movement kann nicht durch creative speed gefälscht werden.~~
- ~~Client-Korrekturen fühlen sich nicht hart an.~~
- ~~Movement bleibt im Singleplayer unverändert flüssig.~~

---

# P5 – Registry- und Datenqualität

P5 verhindert kaputte Daten, fehlende Assets und Save-Probleme.

---

## P5.1 Canonical Keys und Aliase

### Problem

Einige Items/Blocks haben potenziell uneinheitliche Namen.

Beispiele:

- `berries` vs `wild_berries`
- `clay` vs `clay_lump`
- `raw_copper` vs `copper_ore`
- `raw_iron` vs `iron_ore`
- `planks` vs `wooden_plank`

### Aufgaben

- ~~eindeutige Canonical Keys festlegen.~~
- ~~alte Keys als Aliase erhalten.~~
- ~~Tooltips und UI-Namen vereinheitlichen.~~
- ~~Rezepte auf Canonical Keys mappen.~~
- ~~Save-Ladepfad nutzt Aliase.~~
- ~~keine stillen Duplikate in Registries.~~

Status 2026-05-01: `Registry.aliases()` macht Aliase validierbar; Item- und Block-Aliase decken alte Keys für Berries/Planks/Ores/Grass/Campfire-Varianten ab. Rezepte und Placeables nutzen Canonical Keys.

### Akzeptanzkriterien

- ~~alte Saves bleiben kompatibel.~~
- ~~UI zeigt saubere Namen.~~
- ~~Rezepte nutzen eindeutige Keys.~~
- ~~Aliase sind dokumentiert.~~

---

## P5.2 Registry Validation Tests

### Bereits umgesetzt

- Tests: alle Recipe-Inputs/Outputs existieren.
- Tests: placeable Items referenzieren existierende Blocks.
- Tests: Drops existieren.
- Tests: Loot-Entries referenzieren existierende Items.
- Tests: fehlende Texturen werden gemeldet.

### Offene Ergänzungen

- ~~Test: alle BlockEntity Type IDs sind eindeutig.~~
- ~~Test: alle Item-Aliase zeigen auf existierende Items.~~
- ~~Test: alle Block-Aliase zeigen auf existierende Blocks.~~
- ~~Test: alle StationType-Recipes haben gültige Station Blocks.~~
- ~~Test: alle Tools haben sinnvolle ToolType-/ToolLevel-Werte.~~
- ~~Test: alle Ores haben passende requiredToolLevel-Werte.~~
- ~~Test: alle Comfort Blocks haben gültige Comfort-Werte.~~
- ~~Test: alle Loot Tables haben mindestens einen gültigen Eintrag.~~
- ~~Test: keine Registry-ID-Kollisionen.~~
- ~~Test: keine leeren Display-Namen.~~

Status 2026-05-01: Registry-Validation ist über `BlockAliasTest`, `ItemAliasTest`, `BlockRegistryDataTest`, `ItemRegistryDataTest`, `CraftingRecipeTest`, `ComfortRulesTest`, `LootTableTest` und `BlockEntityStoreTest` abgesichert.

### Akzeptanzkriterien

- ~~kaputte Daten fallen im Test auf, nicht erst im Spiel.~~
- ~~fehlende Texturen sind sichtbar gemeldet.~~
- ~~Registry-Änderungen bleiben save-kompatibel.~~

---

# P6 – Entity Runtime und AI-Grundlagen

Dieser Bereich liegt zwischen Engine und Gameplay. Die Engine stellt Laufzeit, Synchronisierung, Culling und sichere Server-Entscheidungen bereit. Das konkrete Balancing gehört in die Gameplay-Liste.

---

## P6.1 Entity Runtime stabilisieren

### Ziel

Entities sollen sauber gespawnt, getickt, synchronisiert, gecullt und entfernt werden.

### Aufgaben

- ~~Entity IDs stabil vergeben.~~ Player-, Ambient-, ItemDrop- und Projectile-IDs laufen deterministisch bzw. aus getrennten Server-ID-Bereichen.
- Entity Lifecycle definieren:
  - ~~spawn~~ Ambient, ItemDrops und Projectiles haben zentrale Spawnpfade.
  - ~~tick~~ Ambient-, ItemDrop- und Projectile-Ticks laufen serverseitig.
  - ~~sleep/inactive~~ Ambient-Entities parken außerhalb aktiver Player-Chunk-Tickets.
  - ~~despawn~~ tote Ambient-Entities, eingesammelte ItemDrops und terminale Projectiles werden entfernt.
  - ~~unload/park~~ Client bereinigt Entity-Runtime außerhalb geladener Chunks; Server parkt entfernte Ambient-Entities.
  - save optional
- Entity Snapshots differenzieren:
  - ~~Position~~
  - ~~Rotation/Yaw~~
  - ~~Velocity optional~~
  - ~~State~~
  - ~~Health~~
  - Target optional
  - Animation State optional über `stateKey`/Velocity vorbereitet
- ~~Entity Updates nach Interest Management filtern.~~ Server sendet Entity-Snapshots pro Viewer-Interest.
- ~~Entity Culling clientseitig verbessern.~~ Client filtert eigenen Player, entladene Chunks und Renderer-Frustum; eigene Projectiles bleiben sichtbar.
- ~~Entity Debug-Hitboxen mit Physics Bounds abgleichen.~~ Debug-Hitboxen nutzen `EntityBounds.forType(...)`.
- Entity Spawnrate budgetieren.

Status 2026-05-01: Entity Runtime hat getrennte Snapshot-Pfade für Player, Ambient, ItemDrops und Projectiles. `AmbientTickStats` misst aktive, geparkte, geblockte und emittierte Entity-Updates; Damage entfernt tote Ambient-Entities inklusive Follow-/Cooldown-State.

### Akzeptanzkriterien

- Entities verschwinden nicht falsch beim Chunk-Unload.
- Entities werden nicht endlos weitergetickt, wenn niemand in der Nähe ist.
- Entity-Snapshots sind klein genug.
- Client rendert nur relevante Entities.

---

## P6.2 Komplexere Entity AI

### Ziel

Ambient Entities sollen lebendiger wirken, ohne Server-Performance zu gefährden.

### AI States

- `IDLE`
- `WANDER`
- `FLEE`
- `FOLLOW`
- `GRAZE`
- `SLEEP`
- `EAT`
- `INVESTIGATE`
- `RETURN_HOME`
- `AVOID_WATER`
- `AVOID_DANGER`

### Aufgaben

- AI-State-Machine in Common oder Server sauber modellieren.
- Sensoren einführen:
  - ~~Spieler in Nähe~~ Bunny/Sheep/Boar-Flee und Sleep-Danger prüfen Player-/Danger-Nähe.
  - ~~Gefahr in Nähe~~ `hasDangerNear` nutzt gefährliche Ambient-Typen für Schlafsicherheit.
  - ~~Futter in Nähe~~ Feed-Interaction setzt Follow-Target serverseitig.
  - ~~Wasser/Abgrund vor Entity~~ Server-World validiert Ambient-Movement gegen Solid/Water/Support.
  - ~~Heimat-/Spawnpunkt~~ Ambient-Anchors begrenzen Wander/Flee-Bewegung.
- einfache Pfadlogik einbauen:
  - kein großes Pathfinding nötig
  - ~~lokale Avoidance reicht zuerst~~ Player-/Entity-AABB-Overlap blockiert lokale Ambient-Bewegung.
  - ~~Blocked-Movement erkennen~~ `AmbientTickStats.blockedAmbientMoves` macht geblockte Moves sichtbar.
- Verhalten pro Entity-Typ konfigurieren:
  - ~~Bunny flieht schneller.~~
  - ~~Sheep folgt Futter und grast.~~
  - Boar sucht Mushrooms.
  - ~~Snail bewegt sich langsam~~ und meidet Sonne optional.
  - ~~Firefly bleibt in Schwärmen/luftigem Movement.~~ Nacht-Bindung bleibt Polish.
- ~~Server tickt AI autoritativ.~~
- ~~Client interpoliert nur.~~

Status 2026-05-01: `IDLE`, `WANDER`, `FLEE`, `FOLLOW` und `GRAZE` sind serverseitig aktiv; `SLEEP`, `EAT`, `INVESTIGATE`, `RETURN_HOME`, `AVOID_WATER` und `AVOID_DANGER` bleiben als spätere AI-Ausbaustufen.

### Akzeptanzkriterien

- Tiere wirken lebendiger.
- AI verursacht keine Tick-Spikes.
- Tiere laufen nicht ständig in Wasser oder Wände.
- Client kann AI-State nicht fälschen.
- Multiplayer-Spieler sehen konsistente Entity-Zustände.

---

## P6.3 Entity Spawning verbessern

### Ziel

Entity-Spawns sollen biome-, zeit- und umgebungsabhängig sein.

### Aufgaben

- Spawn-Regeln pro Biome definieren.
- Tageszeit berücksichtigen.
- Lichtlevel optional berücksichtigen.
- Nähe zu Spieler berücksichtigen.
- Max Entities pro Chunk/Region.
- ~~Despawn-Regeln definieren.~~ Kills entfernen Ambient-Entities, terminale Projectiles despawnen, ItemDrops werden atomar geclaimt.
- Rare Spawn Chancen definieren.
- Spawn-Cooldowns einbauen.
- Spawn Debug Overlay oder Command.

### Beispiel-Regeln

- Bunnies: Meadow, Flower Fields, tagsüber.
- Sheep: Meadow, Nähe zu Grasflächen.
- Boars: Pine Forest, tagsüber, nicht zu nah am Spawn.
- Snails: Mushroom Grove, Mire, eher feucht/dunkel.
- Fireflies: Lakeside/Mushroom Grove, nachts.
- Rare creatures: nur bei bestimmten Biome-/Zeit-Kombinationen.

### Akzeptanzkriterien

- Biome fühlen sich durch Entities unterschiedlich an.
- Entity Count bleibt begrenzt.
- keine Entity-Spam-Probleme.
- Spawns sind deterministisch genug für Debugging, aber nicht starr.

---

# P7 – Combat Runtime Foundation

Combat gehört spielerisch in die Gameplay-Liste. Hier geht es nur um die Engine-/Server-Grundlagen, damit Schaden, Waffen und Projektile sicher funktionieren.

---

## P7.1 Entity Damage System

### Ziel

Entities und Spieler brauchen ein gemeinsames, serverseitiges Damage-System.

### Aufgaben

- ~~`DamageSource` modellieren:~~
  - ~~player_melee~~
  - ~~projectile~~
  - ~~fall~~
  - ~~fire~~
  - ~~drowning~~
  - ~~environment~~
  - ~~unknown~~
- ~~`DamageResult` modellieren:~~
  - ~~accepted~~
  - ~~rejected~~
  - ~~amount~~
  - ~~killed~~
  - ~~knockback optional~~
- ~~Health für relevante Entities serverseitig halten.~~ Ambient-Health bleibt serverseitig im `ServerEntityTracker`.
- ~~Damage Cooldown / invulnerability frames.~~ Ambient-Damage hat serverseitige Invulnerability-Frames.
- Server validiert:
  - ~~Reichweite~~
  - Sichtlinie optional
  - ~~Cooldown~~
  - ~~Waffe~~
  - Gamemode
  - ~~Ziel existiert~~
- ~~Client zeigt nur Feedback.~~ Health-Änderungen kommen über serverseitige Entity-Snapshots.

Status 2026-05-01: `DamageSource`/`DamageResult` liegen in `common/entity`. Melee und Projectile-Schaden laufen serverseitig über den Tracker, ungültige/zu schnelle Treffer werden abgelehnt, Kills räumen Entity-Lifecycle-State auf und Melee-Kills erzeugen Drops.

### Akzeptanzkriterien

- Client kann Entity Health nicht direkt setzen.
- Schaden wird serverseitig berechnet.
- ungültige Angriffe werden abgelehnt.
- Entity Death erzeugt serverseitige Drops.
- Multiplayer-Spieler sehen konsistente Treffer.

---

## P7.2 Bogen und Projektile

### Ziel

Bogen als erstes Fernkampf-/Tool-Projektil-System vorbereiten.

### Aufgaben

- ~~Projectile Entity Type einführen.~~
- ~~Arrow Projectile definieren.~~
- Server autoritativ:
  - ~~Spawn~~
  - ~~Richtung~~
  - ~~Geschwindigkeit~~
  - ~~Lebensdauer~~
  - ~~Collision~~
  - ~~Treffer~~
  - ~~Despawn~~
- Client:
  - Render Projectile
  - ~~Interpolation~~
  - einfache Trail/Particle optional
- Bogen-Item:
  - Draw/Charge-Zeit
  - Pfeilverbrauch
  - Cooldown
  - Durability optional
- Collision:
  - ~~Entity Hit~~
  - ~~Block Hit~~
  - ~~Water slow optional~~
- Networking:
  - Client sendet Shoot Intent.
  - Server validiert Item, Pfeil, Cooldown.
  - Server spawnt Projectile.
  - ~~Snapshots gehen an relevante Clients.~~

Status 2026-05-01: Arrow-Projectiles sind serverseitige Entity-Snapshots mit Swept Block-/Entity-Collision, Gravity, Water-Drag, Lifetime, Despawn und DamageSource-Integration. Offen bleibt die spielbare Bow-/Shoot-Intent-Anbindung.

### Akzeptanzkriterien

- Pfeile werden nicht clientseitig gespawnt.
- Pfeile treffen serverseitig.
- Pfeile despawnen korrekt.
- keine Projectile-Leaks.
- mehrere Spieler sehen Pfeile konsistent.

---

# P8 – Async, Performance und Skalierung

---

## P8.1 Performance Presets vorbereiten

### Ziel

Engine-Features müssen skalierbar bleiben.

### Presets

#### Low

- niedrige Render Distance
- reduzierte Partikel
- kein Bloom
- einfaches Wasser
- kein Soft Shadow
- geringere Mesh-Budgets

#### Medium

- normale Render Distance
- Partikel moderat
- AO an
- einfaches Bloom optional
- normales Wasser

#### High

- größere Render Distance
- mehr Partikel
- Bloom/Glow
- bessere Wasseranimation
- höhere Mesh-Budgets

### Aufgaben

- ~~Preset-Datenmodell.~~
- ~~Settings UI bindet Presets.~~
- ~~einzelne Optionen bleiben überschreibbar.~~
- ~~Debug Overlay zeigt aktives Preset.~~

Status 2026-05-01: `RenderPreset` ist als Low/Medium/High-Datenmodell aktiv, Preset-Wechsel sind per Settings UI und `/preset`-Command nutzbar, manuelle Preset-Optionen setzen den Status auf `Custom`, und das Debug Overlay zeigt das aktive Preset neben Renderdistance, Budgets und Upload-Zeit.

### Akzeptanzkriterien

- ~~Low-End Settings reduzieren messbar Last.~~
- ~~Preset-Wechsel funktioniert zur Laufzeit oder nach Neustart.~~
- ~~manuelle Settings überschreiben Preset sinnvoll.~~

Status 2026-05-01: Low reduziert Render-/Preview-Distanz, Mesh-/Upload-Budgets, Partikelqualität und teure Renderfeatures; High hebt diese Budgets wieder an. `GameSettingsTest` prüft Preset-Anwendung und Custom-Overrides.

---

## P8.2 Memory- und Allocation-Review

### Ziel

Framezeit stabilisieren und GC-Spikes reduzieren.

### Aufgaben

- ~~häufige Allocations im Render-/Update-Loop suchen.~~
- ~~Mesh-Build Buffers wiederverwenden.~~
- ~~Particle-Listen budgetieren.~~
- ~~temporäre Vector/Matrix-Objekte reduzieren.~~
- ~~Debug-Modus für Allocation-Hotspots optional.~~
- ~~lange Explore-Sessions testen.~~

Status 2026-05-01: Mesh-Builds nutzen wiederverwendete `ChunkMesher`-Buffer mit Growth-/Retained-Stats, Partikel sind qualitätsabhängig budgetiert, und `ParticleSystem` verwendet einen persistenten Direct-Vertex-Upload-Buffer mit Limit auf die genutzte Vertexspanne statt pro Frame ein neues Float-Array samt Quad-`Vector3f`s zu erzeugen. Selection-/Mining-Debugfarben im Client werden ebenfalls wiederverwendet. `ClientWorldMeshInvalidationTest.longExploreSmokeKeepsLoadedChunksAndBuildQueueBounded` deckt lange Explore-Bewegung, Chunk-Unload, dirty Queue und Mesh-Buffer-Budget headless ab.

Status 2026-05-01: Für echte Allocation-Hotspot-Stacks gibt es jetzt `./gradlew profileSingleplayerJfr`; die Aufnahme landet unter `build/reports/jfr/adventura-singleplayer.jfr` und ergänzt den Headless-Smoke um Runtime-Profiling.

Status 2026-05-01: `./gradlew :client:cleanTest :client:test --no-daemon --max-workers=1` läuft wieder grün, wenn Gradle-Aufrufe seriell laufen. Parallele Gradle-Prozesse im selben Workspace können weiterhin `common/build`-Ausgaben gegenseitig invalidieren und werden für Gates nicht verwendet.

### Akzeptanzkriterien

- ~~weniger GC-Spikes.~~
- ~~stabilere Framezeit.~~
- ~~kein unbounded growth bei Chunks/Particles/Entities.~~

Status 2026-05-01: Chunk-, Mesh-, Particle- und GPU-Ressourcen sind budgetiert bzw. im Debug Overlay sichtbar. GC-/Framezeit-Stabilität ist code-seitig verbessert, durch den Long-Explore-Smoke abgesichert und per JFR-Task profilierbar.

---

# 9. Bekannte Risiken

Status 2026-05-01: Risiken nach P8/P9 neu bewertet. Mehrere ältere kritische Punkte sind durch P0-P9 entschärft; rote Tooling-Blocker sind aktuell abgearbeitet.

## Kritisch

- ~~Tests laufen nicht zuverlässig ohne JDK 21.~~
- ~~Save/Load fehlt oder ist nicht vollständig genug.~~
- ~~BlockEntities können ohne Persistenz Fortschritt verlieren.~~
- ~~Multiplayer-Unload kann ohne Server-Resend gefährlich sein.~~
- ~~Authoritative Movement ist noch nicht vollständig.~~
- ~~Gradle 9.3.0/Testresult-Binary-Gate ist nach erfolgreichem XML-Testlauf nicht zuverlässig und muss als Tooling-Blocker behoben werden.~~

## Hoch

- ~~Chunkgen/Meshing im Client-Loop kann Stutter erzeugen.~~
- ~~Async Chunkgen kann Race Conditions erzeugen.~~
- ~~fehlendes Interest Management skaliert schlecht im Multiplayer.~~
- Entity AI kann Tickzeit erhöhen.
- ~~Loot kann ohne persistente IDs dupliziert werden.~~
- ~~Long-Explore-/Profiler-Smoke für GC, Chunk-Unload und Allocation-Hotspots fehlt noch.~~

## Mittel

- ~~Registry-Aliase können Save-Probleme erzeugen.~~
- ~~Shader-/Materialdaten können verstreut bleiben.~~
- Section-Aware Meshing kann Culling-Bugs erzeugen.
- Projectile-System kann Netzwerktraffic erhöhen.
- ~~GL Resource Tracking kann Debug-only Overhead erzeugen.~~
- ~~Section-Culling und Projectile-Traffic brauchen eigene Smoke-/Budget-Tests, sobald größere Explore- und Combat-Szenen laufen.~~

Status 2026-05-01: Section-Bounds/Culling werden durch `sectionBoundsAroundUsesRadiusAndOnlyNonEmptySections` abgesichert; Projektil-Sweep und Snapshot-Traffic haben eigene Client-/Codec-Budget-Tests.

---

# 10. Empfohlene nächste Arbeitsblöcke

## Sprint 1 – Tooling und Testbasis

Aufgaben:

- ~~JDK 21 fixen.~~
- ~~`./gradlew test` grün bekommen.~~
- ~~`./gradlew buildGame` grün bekommen.~~
- ~~Test-Seeds dokumentieren.~~
- ~~EngineFrameStats einführen.~~

Akzeptanz:

- ~~Engine-Änderungen sind messbar und testbar.~~

---

## Sprint 2 – Chunk Build Queue und Resource Tracking

Aufgaben:

- ChunkBuildQueue V1.
- Kamera-Priorität.
- ms-Zeitbudget.
- GPU Upload nur Render Thread.
- GL Resource Tracking erweitern.
- Explore-Smoke-Test.

Akzeptanz:

- weniger Chunk-Stutter.
- keine Mesh-Leaks beim Erkunden.

---

## Sprint 3 – Save/Load Grundlagen

Aufgaben:

- Save-Version.
- World Diff Save.
- Player Save.
- BlockEntity Save-Grundlage.
- Unknown Item/Block Fallback.

Akzeptanz:

- platzierte/abgebaute Blöcke und Inventory überleben Neustart.

---

## Sprint 4 – BlockEntity Vereinheitlichung

Aufgaben:

- gemeinsamer BlockEntityStore.
- StorageCrate/Campfire/CookingPot/Forge Grundmodell.
- UI Snapshot Sync.
- sichere Transfer Requests.

Akzeptanz:

- interaktive Blöcke sind server-autoritativ und persistierbar.

---

## Sprint 5 – Multiplayer Interest Management

Aufgaben:

- Chunk subscriptions.
- Entity radius filtering.
- Block update filtering.
- Packet stats.
- Server debug output.

Akzeptanz:

- Clients erhalten nur relevante Welt-/Entity-Updates.

---

## Sprint 6 – Entity Runtime und AI

Aufgaben:

- Entity lifecycle.
- Entity culling.
- AI states.
- Spawn rules.
- Server-authoritative entity tick.

Akzeptanz:

- Tiere wirken lebendiger und bleiben performant.

---

## Sprint 7 – Combat Foundation

Aufgaben:

- DamageSource/DamageResult.
- Entity Health serverseitig.
- Shoot Intent.
- Projectile Entity.
- Arrow Collision.
- Bow item validation.

Akzeptanz:

- Bogen und Damage funktionieren serverseitig sicher.

---

# 11. Definition of Done für Engine-Aufgaben

Eine Engine-Aufgabe gilt erst als abgeschlossen, wenn:

- `./gradlew test` läuft.
- `./gradlew buildGame` läuft.
- Singleplayer startet.
- Join Local startet.
- Debug Overlay zeigt keine offensichtliche Regression.
- Speicher/GPU-Ressourcen wachsen beim Erkunden nicht unbegrenzt.
- Multiplayer-relevante Aktionen bleiben servervalidiert.
- neue Packets haben Längen-/Payload-Schutz.
- neue Registries haben Validation Tests.
- neue Persistenzdaten haben Save-/Load-Test oder klaren Smoke-Test.
- keine neuen harten Block-ID-Sonderfälle im Shader entstehen.
- Akzeptanzkriterien des jeweiligen Abschnitts erfüllt sind.

---

# 12. Kurzfassung der wichtigsten offenen Engine-TODOs

## Sofort

- ~~JDK 21/Test-Gate fixen.~~
- ~~EngineFrameStats zentralisieren.~~
- ~~Test-Seeds dokumentieren.~~
- ~~ChunkBuildQueue einführen.~~
- ~~GL Resource Tracking erweitern.~~

## Danach

- Save/Load-Grundlage.
- BlockEntityStore vereinheitlichen.
- Multiplayer Interest Management.
- Async Server Chunkgen.
- Authoritative Movement vorbereiten.

## Später

- Entity Runtime verbessern.
- komplexere AI.
- Projectile System.
- Damage System.
- ~~Performance Presets.~~
- ~~Section-Aware Meshing.~~
