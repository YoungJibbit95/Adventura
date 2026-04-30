# Adventura – Engine TODO List

## Ziel

Diese Liste bündelt alle offenen Engine-Aufgaben aus Gameplay-Roadmap und Engine-Plan. Fokus: Stabilität, Performance, saubere Laufzeitdaten, Persistenz, Multiplayer-Sicherheit und bessere Erweiterbarkeit ohne komplette Neuarchitektur.

## P0 – Tooling, Tests und Messbarkeit

### JDK/Test-Gate reparieren
- JDK 21 lokal verfügbar machen.
- `JAVA_HOME` und `PATH` dokumentieren.
- `./gradlew test` als Pflicht-Gate etablieren.
- `./gradlew buildGame` als vollständiges Build-Gate nutzen.
- README um klare Setup-Hinweise für Windows, Linux und macOS ergänzen.

### Engine-Messpunkte zentralisieren
- `EngineFrameStats` oder ähnlichen Record einführen.
- Messwerte aus `GameClient` herausziehen.
- getrennt messen: Framezeit, Renderzeit, Updatezeit, Chunkgen, Meshing, Lighting, GPU-Upload, Draw Calls, Triangles, VRAM, Loaded/Visible/Dirty Chunks, Entity Count, Particle Count.
- Debug Overlay muss Regressionen schnell sichtbar machen.

### Reproduzierbare Testwelten
- feste Seeds für Spawn, River, Pine Forest, Mushroom Grove, Cave, Village/Market, Frost Peaks, Desert/Dunes und Old Ruins definieren.
- pro Seed Smoke-Checkliste: Spawn frei, keine Terrain-Seams, Wasser sichtbar, Biome korrekt, Entities sichtbar, FPS plausibel, Debugwerte plausibel.

## P1 – Client-Engine und Chunk-Lifetime

### Chunk-Unload einführen
- ClientWorld braucht ein aktives Chunk-Set um Kamera/Spieler.
- Chunks außerhalb Render-/Preview-Hysterese entladen.
- WorldRenderer muss GPU-Meshes entladener Chunks freigeben.
- Ambient Entities außerhalb aktiver Chunks entfernen oder parken.
- Debug Overlay zeigt Loaded/Rendered/Unloaded Chunks.

### Chunk Build Queue
- Chunkgen, Lighting und Meshing nicht dauerhaft synchron im Client-Loop ausführen.
- `ChunkBuildQueue` mit Kamera-Priorität einführen.
- CPU-Build optional backgroundfähig machen.
- GPU-Upload bleibt im Render Thread.
- Budget nicht nur als Chunks/frame, sondern als ms-Zeitbudget steuerbar machen.

### Section-Aware Chunk-Daten
- leere Sections schneller überspringen.
- Mesh-Bounds pro Section oder Layer vorbereiten.
- Frustum-Culling genauer machen.
- Ziel: weniger unnötige Draw Calls bei hohen Chunks, Höhlen und Bergen.

### GL Resource Tracking
- VAO/VBO/Texture-Lebenszeit tracken.
- Debug-Zähler für aktive GPU-Ressourcen anzeigen.
- Mesh dispose sicherstellen.
- Smoke-Test für längeres Erkunden mit Chunk-Unload.

## P2 – Save/Load und Persistenz

### World Save
Speichern: World Seed, geänderte Blöcke, Block Entities, Storage Crates, Campfires, Cooking Stations, Loot Chests, geöffnete Loot States, Structure State und später wichtige Entities.

### Player Save
Speichern: Position, Inventory, Health, Hunger, Stamina, Breath, Spawn Point, entdeckte Rezepte, entdeckte Biome und Journal Entries.

### Versioniertes Save-Format
- Save-Version einführen.
- Backup vor Migration.
- Unknown Block/Item Fallback.
- BlockEntity Type IDs.
- Migration Hooks vorbereiten.

## P3 – BlockEntity-System vereinheitlichen

- gemeinsamen `BlockEntityStore` ausbauen.
- BlockEntity-Typen: StorageCrate, Campfire, CookingPot, Forge, LootCrate, optional Workbench.
- BlockEntity-Daten serverseitig autoritativ halten.
- Client bekommt nur Snapshots/UI-Daten.
- Save/Load für alle BlockEntities.
- ~~Multiplayer-Transfers und Station-Operationen serverseitig prüfen.~~ ✅ Basis-Validierung für Storage-Transfers/Crafting/Cooking ist serverseitig aktiv.
- Transaktionsvalidierung weiter ausbauen: Sequenznummern auch für Craft/Cook/Open-Requests vereinheitlichen.

## P4 – Multiplayer Engine

### Interest Management
- Entity-Snapshots nicht global an alle Spieler senden.
- Relevanz nach Chunk/Radius berechnen.
- feste Snapshot-Tickrate für Entities.
- Block Updates nur an relevante Zuschauer.

### Async Server Chunk Generation
- Chunk-Erzeugung darf Netty-Handler nicht blockieren.
- Worker Queue für Chunkgen.
- Packet-Auslieferung nach Fertigstellung.
- Priorität nach Spielerposition.

### Authoritative Movement vorbereiten
- Server nutzt gemeinsame Physics-Regeln zur Validierung.
- Client sendet Movement mit Sequenznummer oder später Input-State.
- Server antwortet mit autoritativem State.
- Reconciliation später optional.

## P5 – Registry- und Datenqualität

- eindeutige Item Keys festlegen.
- alte Keys als Aliase behalten.
- Rezepte auf Canonical Keys mappen.
- Tooltips und UI-Namen vereinheitlichen.
- Save-Kompatibilität sichern.
- Tests: alle Recipe-Inputs/Outputs existieren, placeable Items referenzieren existierende Blocks, Drops existieren, fehlende Texturen werden gemeldet.

## Akzeptanzkriterien

- `./gradlew test` und `./gradlew buildGame` laufen.
- Singleplayer und Join Local starten.
- Debug Overlay zeigt Engine-Stats sauber.
- längeres Erkunden erhöht Speicher/GPU-Meshes nicht unbegrenzt.
- keine neuen Shader-Sonderfälle über harte Block-IDs.
