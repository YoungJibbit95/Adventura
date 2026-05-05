# Adventura - Engine TODO List V2

Stand: 2026-05-01
Bereich: Engine, Runtime, Rendering, Streaming, Persistenz, Networking, Datenqualitaet, Profiling

---

## Status der alten Engine-Liste

Die erste Engine-Liste hat die Alpha-Grundlage gelegt:

- JDK-/Gradle-Gate und fokussierte Tests sind etabliert.
- `EngineFrameStats` und Debug-HUD zeigen zentrale Runtime-, Chunk-, Render-, Netzwerk- und GPU-Werte.
- Reproduzierbare Smoke-Seeds liegen in `docs/WORLD_SMOKE_TESTS.md`.
- Chunk-Unload, Build-Queue, Mesh-/Upload-Budgets, Section-Bounds und GL-Resource-Tracking sind vorhanden.
- Save-/Load-Grundlagen, BlockEntity-Speicherung, Interest Management und mehrere Multiplayer-Sicherungen sind begonnen oder implementiert.
- Rendering-Pipeline, Material-LUT, Atlas-Validation, Debug Views, Presets, Lighting und Weather-Lightning haben eine belastbare Alpha-Basis.

Diese V2-Liste ersetzt die abgearbeitete Historienliste. Sie beschreibt die naechsten Schritte, um Adventura von einer stabilen Alpha-Basis naeher an eine fertige Voxel-Game-Engine zu bringen.

---

## Zielbild

Adventura soll als Voxel-Engine:

- stabile Framezeiten liefern, auch bei langer Erkundung.
- Chunks, Entities, Partikel, Licht und Netzwerkdaten budgetiert streamen.
- Terrain visuell klar, performant und debugbar rendern.
- Save-/Load-Daten versioniert und migrationssicher behandeln.
- Singleplayer und Multiplayer mit denselben Common-Regeln betreiben.
- Engine-Regressionen frueh durch Tests, Smokes und Profiling sichtbar machen.
- Low-End- und High-End-Presets sinnvoll skalieren.

---

## Engine-Leitlinien

- Gameplay-Regeln gehoeren nach `common`, Server-Autoritaet nach `server`, Rendering nach `client`.
- Der Client sendet Intents, keine finalen Ergebnisse.
- Neue Shader-Faelle werden ueber Material-, Render- oder Runtime-Daten geloest, nicht ueber harte Block-IDs.
- Jede Engine-Aenderung braucht mindestens einen Test, eine Messung oder einen konkreten Smoke-Check.
- Budgets sind besser als harte Limits ohne Sichtbarkeit.
- Datenkompatibilitaet ist wichtiger als kurzfristige Vereinfachung.
- Debug-Tools duerfen Kosten haben, aber sie muessen sichtbar und abschaltbar sein.

---

# P0 - Build-, Test- und Profiling-Gates V2

## P0.1 Zuverlaessige Gates

### Status 2026-05-01

- Gradle Wrapper ist auf `9.3.1` angehoben.
- Standard-`test`-Tasks raeumen `cleanTest` vor der Ausfuehrung, nutzen einen eigenen Binary-Result-Pfad pro Gradle-Invocation und laufen mit Plain Console.
- Kleine Output-Anchor-Tests in `client`, `common`, `server` und `tools` halten Gradles Binary-Testoutput-Store stabil, auch wenn die fachlichen Tests selbst nichts auf Stdout/Stderr schreiben.
- README-Badge und Task-Doku spiegeln den aktuellen Gradle-Stand.
- Verifiziert mit `./gradlew test --no-daemon` und `./gradlew buildGame --no-daemon`.

### Offen

- ~~Launcher-/Runtime-Startpfad fuer Windows, macOS und Linux absichern.~~
  Erledigt: 2026-05-05, `launcher-electron/electron/platform.cjs` kapselt Plattformnamen, Startscript-Namen, Java-21-Runtime-Aufloesung, Unix-Executable-Bits und Child-Environment. Launcher-Preflight meldet jetzt Plattform und Java-Quelle.
  Verifikation: `npm run test:platform`; `npm run build` im Launcher; `./gradlew.bat buildGame --no-daemon --max-workers=1 --console=plain -PadventuraTestRunId=platform_ui_build_1`.
- MacOS Full Compatibility End-to-End-Smoke auf echter macOS-Maschine/CI bleibt offen.
- ~~Test-Binary-Result-Pfad gegen parallele `cleanTest`-Laeufe isolieren.~~
  Erledigt: 2026-05-01, `binaryResultsDirectory` nutzt eine Run-ID aus `-PadventuraTestRunId`, `-Dadventura.testRunId` oder automatisch generierter UUID.
  Verifikation: Gradle-Konfiguration kompiliert bis `compileTestJava`; `ContentTagRegistryTest` erzeugte XML/HTML mit 8 Tests, 0 Fehlern.
- ~~🔴 Parallele Gradle-Laeufe im selben Workspace sind noch nicht vollstaendig abgesichert: gleichzeitige `buildGame`/`:server:test --rerun-tasks` koennen weiterhin gemeinsame `compileJava`/`compileTestJava`-Outputs stoeren.~~
  Erledigt: 2026-05-01, `WorkspaceMutationLockService` haelt eine `.gradle/adventura-workspace-mutation.lock`-Dateisperre fuer mutierende Compile/Test/Clean/Archive/StartScript-Tasks. Opt-out fuer Spezialfaelle: `-PadventuraWorkspaceLock=false`.
  Verifikation: `:client:test` und `:server:test` liefen mit aktiviertem Lock und isolierter Test-Run-ID gruen.
- Testreports bei Fehlern automatisch auffindbar machen.
- bekannte flaky Tests mit Ursache, Workaround und Owner dokumentieren.
- Smoke-Test-Checkliste pro Release aktualisieren.

### Akzeptanz

- Standard-Gate laeuft gruen.
- keine stillen Testresult-Binary- oder Worker-Fehler durch alte Testresult-Reste.
- offene Diagnose- und Release-Komfortpunkte stehen unter `Offen`.

## P0.2 Profiling als normales Werkzeug

### Status 2026-05-01

- JFR-Tasks fuer Singleplayer, Join Local und Long Explore sind vorhanden.
- `docs/ENGINE_PROFILING_SCENARIOS.md` definiert feste Szenarien, Pflichtmetriken und Report-Notizen.
- README listet die Profiling-Tasks.

### Offen

- Optional: Script bauen, das JFR-Dateien in kurze Markdown-Reports zusammenfasst.
- Manueller Runtime-Smoke der drei Profiling-Tasks in einer GL-faehigen Session und bestaetigte `.jfr`-Dateien unter `build/reports/jfr/`.

### Akzeptanz

- Performance-PRs koennen ein festes Profiling-Szenario und relevante Messwerte nennen.
- JFR-Aufnahmen sind auf `build/reports/jfr/` vorkonfiguriert.
- Long-Explore-Regressionen haben ein eigenes Profiling-Gate.

---

# P1 - Frame Loop und Scheduler

## P1.1 Frame-Phasen explizit machen

### Ziel

Update, Simulation, Chunk Jobs, GPU Upload und Rendering sollen klar getaktet sein.

### Aufgaben

- ~~Frame-Phasen als kleine Struktur dokumentieren und im Code spiegeln:~~ `EngineFrameStats.FramePhases`
  - ~~Input sammeln.~~
  - ~~Netzwerk/Server-Snapshots anwenden.~~
  - ~~Player/Camera Update.~~
  - ~~World/Entity Update.~~
  - ~~Chunk/Light/Mesh Jobs budgetiert pumpen.~~
  - ~~GPU Uploads im Render Thread.~~
  - ~~Render Passes.~~
  - ~~UI/HUD/Debug.~~
- ~~pro Phase Zeit messen.~~
- ~~harte Reihenfolge fuer blockierende Operationen vermeiden.~~ Messung trennt blockierende Render-Thread-Arbeit von Update/World; P1.2 entfernt weitere harte Kopplungen.
- ~~"late frame emergency brake" pruefen: teure optionale Jobs abbrechen, wenn Framebudget verbraucht ist.~~ bestehende adaptive Mesh-/Upload-Budgets drosseln Chunk-Jobs bei langsamen Frames; ein echtes Job-System bleibt P1.2.

### Erreicht 2026-05-01

- `EngineFrameStats.FramePhases` trennt Input, Network, Player, World, Chunk-Jobs, GPU-Upload, Render-Passes und UI.
- `GameClient` misst die Phasen im Main-Loop und separiert `worldRenderer.rebuildDirty(...)` von den eigentlichen Render-Passes.
- Debug-HUD zeigt eine `PHASE`-Zeile mit `IN`, `NET`, `PLY`, `WORLD`, `JOB`, `GPU` und `RPASS`.
- `EngineFrameStatsTest` deckt Legacy-Capture und explizite Phase-Timings ab.
- Verifikation: `./gradlew :client:test --tests dev.voxelgame.client.EngineFrameStatsTest`.

### Akzeptanz

- ~~Debug-HUD zeigt Update-, World-, Job-, Render- und UI-Zeit getrennt.~~
- ~~keine Chunk-Generation oder Mesh-Uploads blockieren unbudgetiert die Hauptschleife.~~ Bestehende Mesh-/Upload-ms-Budgets bleiben aktiv; P1.2 baut darauf ein echtes Job-System.

## P1.2 Job-System V1

### Ziel

Chunkgen, Lighting, Meshing, Save IO und Netzwerkaufbereitung sollen budgetiert nebenlaeufig laufen koennen.

### Aufgaben

- ~~Job-Typen definieren: `chunk.generate`, `chunk.light`, `chunk.mesh`, `save.write`, `net.encode`.~~ `EngineJobType`
- ~~Prioritaeten definieren: Spieleraktion, sichtbare Chunks, Preview, Hintergrund.~~ `EngineJobPriority`
- ~~Cancellation/Dedupe pro Chunk/Section absichern.~~ Chunk-Mesh-Jobs dedupen/ersetzen pro `ChunkPos`, Unload cancelt entfernte Eintraege.
- ~~Jobs nur immutable Snapshots oder klar besessene Daten mutieren lassen.~~ Chunk-Mesh-Resultate bleiben `LayeredMeshBuild`-Snapshots, GPU-Uploads laufen im Render Thread.
- ~~Result-Queue fuer Main/Render Thread sauber begrenzen.~~ Mesh- und Upload-Budgets begrenzen Build-/Upload-Anwendung pro Frame.
- ~~Debug-HUD zeigt Pending/Running/Completed/Cancelled pro Job-Typ.~~ `JOBS P/R/C/X` zeigt Generate, Light, Mesh, Save und Net.

### Erreicht 2026-05-01

- `EngineJobType` und `EngineJobPriority` definieren die V1-Scheduler-Sprache fuer Chunk-, Save- und Network-Arbeit.
- `ChunkBuildQueue` priorisiert Spieleraktionen vor sichtbaren Chunks, Preview und Hintergrund und zaehlt Generate-/Light-/Mesh-Jobs getrennt.
- `EngineFrameStats.Jobs` aggregiert Job-Counter ins Debug-HUD.
- Das HUD zeigt `JOBS P/R/C/X` fuer `GEN`, `LGT`, `MSH`, `SAV` und `NET`.
- `SAV` ist im V1 als sichtbarer Save-Job-Slot vorbereitet; die tiefere persistente Save-Queue bleibt Teil von P6.1.
- Verifikation:
  - `./gradlew :common:test --tests dev.voxelgame.common.engine.EngineJobModelTest --no-daemon --max-workers=1 --rerun-tasks`
  - `./gradlew :client:test --tests dev.voxelgame.client.EngineFrameStatsTest --tests dev.voxelgame.client.world.ClientWorldMeshInvalidationTest --no-daemon --max-workers=1 --rerun-tasks`

### Erreicht 2026-05-05

- Chunk-Generation besitzt jetzt eigene Chunk- und Millisekunden-Budgets statt nur indirekt am Mesh-Budget zu haengen.
- Langsame Frames drosseln Preview-Chunkgen adaptiv, damit Streaming-Arbeit bei Stottern sofort kleiner wird.
- `EngineFrameStats.Budgets` und Debug-HUD zeigen das aktuelle Chunkgen-Budget neben Mesh-, Upload-, Unload- und GPU-Release-Budgets.

### Akzeptanz

- ~~schnelle Spieleraktion gewinnt gegen Preview-Arbeit.~~
- ~~entfernte Preview-Jobs koennen verworfen werden.~~
- ~~keine Race Conditions zwischen Unload und Job-Result-Anwendung.~~

---

# P2 - Chunk Streaming und World Runtime V2

## P2.1 Section-first Chunk Lifecycle

### Ziel

Die Engine soll vertikale Sections als echte Streaming-, Meshing- und Culling-Einheit nutzen.

### Aufgaben

- ~~`ChunkSection` als Dirty-, Mesh- und Light-Einheit staerker nutzen.~~ `ChunkSection` traegt Dirty-Aspekte, `ChunkMesher.buildSectionLayerMesh(...)` kann gezielt eine Section/Layer-Kombination bauen.
- ~~Section-Dirty-Bits fuer Geometry, Light, Fluid und BlockEntity trennen.~~ `ChunkSection.DirtyAspect`
- ~~Section-Bounds pro Render-Layer erzeugen.~~ `ClientWorld.sectionLayerBoundsAround(...)`
- optional mehrere Draw-Ranges pro Chunk einfuehren.
- 🔴 Mehrere Draw-Ranges/Chunk-Parts brauchen P3.2: `ChunkMesh` sollte `SectionPart(sectionY, layer, indexOffset, indexCount, bounds)` ausgeben. `GpuChunkMesh` muss dann entweder `glDrawElements` mit Offset pro Part oder mehrere GPU-Part-Objekte verwalten. Wichtig: Index-Offsets muessen Byte-Offsets im Element-Buffer sein, und Stats/Culling muessen pro Part zaehlen.
- 🔴 Section-Unload/Release fuer leere Bereiche braucht ein groesseres World-Datenmodell: `Chunk` ist aktuell ein dichtes `ChunkSection[]`. Fuer echte CPU-Section-Releases muss daraus entweder ein sparse Section-Store werden oder ein `ChunkSectionPayload`, dessen Block-/Light-Arrays fuer leere Sections freigegeben und bei Writes lazy neu angelegt werden. Save/Codec und Lighting muessen dabei leere/missing Sections als Air/0-Light behandeln.
- ~~Tests fuer negative Y, hohe Berge und mehrere nicht zusammenhaengende Sections.~~ Section-Layer-Bounds und Section-Mesh-Tests decken diese Faelle ab.

### Erreicht 2026-05-01

- `ChunkSection.DirtyAspect` trennt Geometry, Light, Fluid und BlockEntity als eigene Dirty-Bits.
- Blockaenderungen an Chunk- oder Section-Kanten markieren angrenzende Sections fuer spaetere Partial-Rebuilds.
- `ClientWorld.sectionStats()` zaehlt Dirty-Sections getrennt und das Debug-HUD zeigt `DIRTY G/L/F/B`.
- `ClientWorld.sectionLayerBoundsAround(...)` erzeugt Layer-Bounds fuer Solid, Cutout und Translucent je Section.
- `ChunkMesher.buildSectionLayerMesh(...)` ist der kleine Einstieg fuer section-first Mesh-Ausgabe.
- Verifikation:
  - `./gradlew :common:test --tests dev.voxelgame.common.world.ChunkSectionDirtyTest --no-daemon --max-workers=1 --rerun-tasks`
  - `./gradlew :client:test --tests dev.voxelgame.client.render.ChunkMesherTest --tests dev.voxelgame.client.world.ClientWorldMeshInvalidationTest --no-daemon --max-workers=1 --rerun-tasks`

### Akzeptanz

- Hoehlen und hohe Berge haben jetzt getrennte Section-/Layer-Daten; echte Draw-Arbeitsreduktion kommt mit P3.2 Draw-Ranges/Part-Culling.
- Section-Culling verursacht kein sichtbares Popping, solange Chunk-Culling weiter der aktive Renderpfad bleibt; Part-Culling bekommt P3.3-Smokes.
- ~~Debug-Views zeigen Section-Bounds korrekt.~~ Section-Bounds bleiben vorhanden, Layer-Bounds sind als Datenquelle vorbereitet.

## P2.2 Streaming-Ringe und Retention

### Ziel

Chunk-Load/Unload soll vorhersehbar und stutterarm sein.

### Aufgaben

- ~~Ringe definieren:~~ `ChunkStreamingRings`
  - ~~Simulation Ring.~~
  - ~~Render Ring.~~
  - ~~Preview Ring.~~
  - ~~Retain Ring.~~
- ~~Hysterese pro Ring dokumentieren.~~ Client-Retain bleibt `preview + 2` Chunks, damit Rueckwaertsbewegung nicht sofort CPU/GPU churnt.
- ~~Chunk-Subscriptions und Client-Retention auf dieselbe Distanzlogik bringen.~~ Server-Subscriptions und Client-Unload nutzen die gemeinsame `ChunkStreamingRings`-Quadratdistanz.
- ~~Unload-Budget pro Frame einfuehren.~~ `ClientWorld.unloadOutside(..., maxUnloads)`
- ~~GPU-Release-Budget pro Frame einfuehren.~~ `GameClient` begrenzt Unload/Release ueber `min(chunkUnloadBudget, gpuReleaseBudget)`.
- ~~Long-Explore-Smoke fuer 10, 20 und 30 Minuten definieren.~~ `docs/WORLD_SMOKE_TESTS.md`

### Erreicht 2026-05-01

- `ChunkStreamingRings` beschreibt Simulation, Render, Preview und Retain als gemeinsame Engine-Struktur.
- Offline-Client-Preview, Client-Retention und Server-Chunk-Subscriptions verwenden dieselbe square-distance Ringlogik.
- Chunk-Unload und GPU-Mesh-Release laufen pro Frame budgetiert ueber die Settings.
- Farthest-first-Unload verhindert, dass nahe Retain-Chunks vor weit entfernten Chunks verschwinden.
- Long-Explore-Smoke ist fuer 10, 20 und 30 Minuten dokumentiert.
- Verifikation:
  - `./gradlew :common:test --tests dev.voxelgame.common.world.ChunkStreamingRingsTest --no-daemon --max-workers=1 --rerun-tasks`
  - `./gradlew :client:test --tests dev.voxelgame.client.world.ClientWorldMeshInvalidationTest --tests dev.voxelgame.client.GameSettingsTest --no-daemon --max-workers=1 --rerun-tasks`
  - `./gradlew :server:test --tests dev.voxelgame.server.net.ServerConnectionHandlerTest --no-daemon --max-workers=1 --rerun-tasks`

### Erreicht 2026-05-05

- ~~Singleplayer-Entry blockiert den Render-Thread nicht mehr waehrend Worldgen/Connect.~~ Loader-Worker veroeffentlichen nur noch `LoadingScreenViewModel`-Fortschritt und uebergeben fertige World/Connection per Main-Thread-Queue.
- ~~Initialer Singleplayer-Load laedt nicht mehr den kompletten Render-/Preview-Radius vorab.~~ Der Startpfad laedt den kleinen Simulation-/Spawn-Ring, maximal Radius 2 bzw. 25 Chunks; der restliche Preview-Ring streamt budgetiert im normalen Spiel.
- ~~Loading-Screens rendern echte Fortschrittsphasen statt Cross-Thread-GL-Frames.~~ GLFW/OpenGL bleibt im Render-Thread, Singleplayer zeigt `STREAMING_SPAWN` mit Chunk-Zaehler.
- ~~Preview-Chunkgen ist pro Frame zeit- und mengenbudgetiert.~~ `ClientWorld.ensurePreviewAround(..., maxMilliseconds)` stoppt nach Chunk-/Zeitbudget, macht aber mindestens einen erlaubten Chunk Fortschritt.
- ~~Chunkgen-Budgets reagieren auf langsame Frames.~~ `GameSettings.effectiveChunkGenerationBudgetChunks(...)` und `effectiveChunkGenerationBudgetMilliseconds(...)` reduzieren Streaming-Arbeit bei teuren Frames.
- ~~Lighting fuer neu generierte Preview-Batches wird zusammengefasst.~~ Neue Chunks werden als Batch beleuchtet und als ein Light-Job gemessen, statt pro Chunk mehrere Full-Rebuilds zu starten.
- ~~Preview-Chunkgen loest keine Remesh-Kaskade mehr fuer noch ungeladene Nachbarn aus.~~ Neue Preview-Chunks markieren nur sich selbst und bereits geladene kardinale Nachbarn dirty.
- ~~Layer-Meshing ueberspringt leere Render-Layer.~~ `ChunkRenderLayerPresence` scannt Chunk-Sections einmal und verhindert Opaque/Cutout/Translucent-Builds, wenn ein Layer im Chunk nicht vorkommt.
- Verifikation:
  - `./gradlew :client:test --tests dev.voxelgame.client.viewmodel.LoadingScreenViewModelTest --tests dev.voxelgame.client.GameClientUiLayoutTest --tests dev.voxelgame.client.world.ClientWorldSpawnTest --no-daemon --max-workers=1`
  - `./gradlew :client:test --tests dev.voxelgame.client.GameSettingsTest --tests dev.voxelgame.client.EngineFrameStatsTest --tests dev.voxelgame.client.world.ClientWorldMeshInvalidationTest --no-daemon --max-workers=1`
  - `./gradlew :client:test --tests dev.voxelgame.client.world.ClientWorldMeshInvalidationTest --tests dev.voxelgame.client.GameClientUiLayoutTest --no-daemon --max-workers=1 -PadventuraTestRunId=world_chunk_melee_ui_3`

### Akzeptanz

- ~~Loaded Chunks, GPU Meshes und Entity Count wachsen nicht unbegrenzt.~~ Budget-/Retain-Smokes halten die Runtime-Daten begrenzt.
- ~~Rueckkehr zum Spawn laedt kontrolliert nach.~~ Retain-Hysterese laesst Chunks kontrolliert neu streamen.
- ~~keine sichtbaren Chunk-Holes bei normaler Bewegung.~~ Preview-Ring bleibt groesser/gleich Render-Ring.

## P2.3 World Data Cache

### Ziel

Wiederholte Terrain-, Collision- und Height-Abfragen sollen nicht jedes System einzeln teuer machen.

### Aufgaben

- ~~`ChunkTerrainCache` fuer Height, Surface, Fluid und Cave Flags konsolidieren.~~
- ~~Cache-Invalidierung bei Block-Updates testen.~~
- ~~Physics, Spawning, Rendering und Worldgen sollen denselben Cache nutzen, wenn sinnvoll.~~ Worldgen, Client-Biome/Height/Surface/Fluid/Cave-Queries und Server-Loot/Structure-Lookups nutzen denselben Cache-Pfad; Collision-Caches bleiben P5.1.
- ~~Cache-Memory im Debug-HUD sichtbar machen.~~ `TCACHE chunks/MB`

### Erreicht 2026-05-01

- `ChunkTerrainCache` enthaelt Height, Biome, Surface-Block, Fluid-Flag und Cave-Flag pro Column.
- `OverworldGenerator` fuellt Terrain aus dem Cache und nutzt dieselben Surface-/Height-Daten fuer Planung und Generierung.
- Client- und Server-Blockupdates invalidieren den betroffenen Chunk-Terrain-Cache.
- Debug-HUD zeigt Terrain-Cache-Chunks und geschaetzten Speicher neben den Section-Werten.
- Verifikation:
  - `./gradlew :common:test --tests dev.voxelgame.common.world.ChunkTerrainCacheTest --tests dev.voxelgame.common.world.OverworldGeneratorTest --no-daemon --max-workers=1 --rerun-tasks`
  - `./gradlew :client:test --tests dev.voxelgame.client.EngineFrameStatsTest --tests dev.voxelgame.client.world.ClientWorldMeshInvalidationTest --no-daemon --max-workers=1 --rerun-tasks`

### Akzeptanz

- ~~keine widerspruechlichen Height-/Surface-Werte zwischen Systemen.~~ Generator-Test vergleicht Cache-Surface mit erzeugtem Chunk.
- ~~Block-Update invalidiert betroffene Cache-Eintraege.~~ Client-Test prueft Cache-Count/Bytes nach Blockupdate.

---

# P3 - Rendering Pipeline V2

## P3.1 Render Pass Graph ohne Overengineering

### Ziel

Neue Renderfeatures sollen nicht wieder ungeordnet im Main-Renderer landen.

### Aufgaben

- ~~Pass-Liste als Datenmodell einfuehren:~~ `RenderPassPlan`
  - ~~sky.clear~~
  - ~~terrain.opaque~~
  - ~~terrain.cutout~~
  - ~~entities.opaque~~
  - ~~terrain.translucent~~
  - ~~particles~~
  - ~~outlines/overlays~~
  - ~~hud/ui~~
  - ~~debug~~
- ~~pro Pass GL-State beschreiben und testen, soweit headless moeglich.~~
- pro Pass Stats sammeln: Draw Calls, Tris, Mesh Count, Zeit. Terrain-Passes sind abgedeckt; Entity/Particle/UI bleiben fuer Diagnostics V2 offen.
- ~~Pass-Reihenfolge in Tests als Contract sichern.~~
- Fallback bei Shader-Reload-Fehlern erhalten.

### Erreicht 2026-05-01

- `RenderPassPlan` definiert die Alpha-Pass-Reihenfolge inklusive grobem GL-State-Vertrag.
- `WorldRenderer` nutzt die zentralen Pass-Namen fuer Terrain-Stats.
- `RenderPassPlanTest` prueft Reihenfolge und State-Vertrag; `WorldRendererTest` haelt Terrain-Pass-Namen fest.

### Akzeptanz

- ~~neue Renderfeatures haben einen klaren Pass.~~
- Debug-HUD zeigt Kosten pro Pass. Terrain-Pass-Kosten sind vorhanden; vollstaendige Nicht-Terrain-Passzeiten kommen mit Diagnostics/Pass-Stats-Ausbau.
- Transparenz und Debug-Lines stoeren Terrain-State nicht.

## P3.2 Section Draw Ranges und Upload-Taktiken

### Ziel

GPU-Arbeit soll mit Section- und Layer-Daten skalieren.

### Aufgaben

- ~~Mesh-Ausgabe auf Section-Parts erweitern: `sectionY`, `layer`, `indexOffset`, `indexCount`, `bounds`.~~ `ChunkMesh.SectionPart`
- ~~pro Chunk mehrere Draw-Ranges oder mehrere `GpuChunkMesh`-Parts pruefen.~~ `GpuChunkMesh.drawParts(...)` nutzt `glDrawElements` mit Element-Buffer-Byte-Offsets pro sichtbarem Section-Part.
- ~~CPU-seitige TerrainUploadQueue als Upload-Staging einfuehren.~~ Echter persistenter GL-Staging-/Ringbuffer bleibt separater Low-Level-Schritt.
- persistente oder ringfoermige Upload-Buffer evaluieren.
- ~~Upload-Budget in Bytes und Millisekunden messen.~~ `TerrainUploadQueue.UploadResult` liefert hochgeladene Bytes, Pending-Bytes und elapsed ms.
- Partial Rebuilds nur fuer betroffene Sections vorbereiten.

### Erreicht 2026-05-05

- `ChunkMesher` baut SOLID/CUTOUT/TRANSLUCENT pro vertikaler Section als zusammenhaengende Index-Range und haelt Bounds je Part fest.
- Greedy-Meshing laeuft jetzt sectionweise, damit Merge-Flaechen nicht ueber Section-Grenzen hinweg unteilbare Draw-Ranges erzeugen.
- `GpuChunkMesh` speichert Section-Parts und kann sichtbare Ranges mit korrekten Element-Buffer-Offsets zeichnen.
- `TerrainUploadQueue` staged `LayeredMeshBuild`-Snapshots vor dem GL-Upload, dedupt pro `ChunkPos`, sortiert nach Kamera-Prioritaet und draned unter ms-Budget mit mindestens einem Upload pro erlaubtem Frame.
- `WorldRenderer.releaseChunks(...)` cancelt pending Uploads fuer entladene Chunks, bevor GPU-Ressourcen freigegeben werden.
- Verifikation: `ChunkMesherTest.visibleFaceMeshingEmitsContiguousSectionParts`, `ChunkMesherTest.sectionPartMeshingKeepsSectionBoundaryFacesCulledByNeighborBlocks`.
- Verifikation: `./gradlew :client:test --tests dev.voxelgame.client.render.TerrainUploadQueueTest --tests dev.voxelgame.client.render.WorldRendererTest --no-daemon --max-workers=1`.

### Akzeptanz

- Blockaenderung in einer Hoehle rebuildet nicht den gesamten hohen Chunk, wenn nicht noetig.
- Upload-Spikes sind im HUD sichtbar und budgetiert.
- ~~Section-Parts koennen einzeln gecullt werden.~~

## P3.3 Occlusion und Visibility Taktiken

### Ziel

Nicht sichtbare Chunk-Sections sollen moeglichst frueh aussortiert werden.

### Aufgaben

- ~~Frustum-Culling pro Section-Part.~~ `VisibilityCollector.partCulling(...)` testet Part-Bounds, `TerrainRenderer` zeichnet nur sichtbare Parts eines sichtbaren Mesh-Layers.
- ~~Distance-Culling pro Ring.~~ Terrain-Layer bleiben vor dem Part-Culling an die Render-Distance gebunden.
- optional Software-Occlusion mit grobem Height-/Column-Buffer pruefen.
- optional HZB/Occlusion-Queries nur nach stabilem Pass-System evaluieren.
- debugbare Culling-Grundzaehler: Distance, Frustum, Occlusion, Empty. Distance/Bounds und Section-Part R/C/L sind sichtbar; Occlusion-/Empty-Detailzaehler bleiben offen.
- Culling-Smokes fuer Hoehlen, Berge, Dichte Waelder, Ruinen.

### Erreicht 2026-05-05

- Chunk-/Layer-Culling bleibt als grober erster Filter bestehen; danach entscheidet der Renderer pro Section-Part, welche Draw-Range wirklich gezeichnet wird.
- Draw Calls und Triangle-Stats zaehlen dadurch die tatsaechlich sichtbaren Section-Ranges statt immer den ganzen hohen Chunk-Layer.
- `RenderPassStats`, `WorldRenderer.RenderStats`, `EngineFrameStats.Rendering` und Debug-HUD melden geladene/gerenderte/gekullte Section-Parts als `SPART R/C/L`.
- Verifikation: `VisibilityCollectorTest.cullsSectionPartsIndependentlyByBounds`, `WorldRendererTest`.

### Akzeptanz

- keine false negatives: sichtbare Meshes verschwinden nicht.
- Cull-Gruende sind im Debug-HUD trennbar.
- offene Landschaft und Hoehle profitieren unterschiedlich messbar.

## P3.4 Material- und Shader-Contracts V2

### Ziel

Materialdaten, Shader-Uniforms und Render-Layer sollen langfristig skalieren.

### Aufgaben

- Material-LUT-Version dokumentieren.
- Shader-Uniform-Contract in Tests pro Shader erweitern.
- Material-Flags fuer zukuenftige Features reservieren:
  - wind.
  - foliage shade.
  - fluid.
  - emissive.
  - decal.
  - damage overlay.
- fehlende Materialdaten als Start-/Testfehler behandeln, nicht nur Debug-Zahl.
- Shader-Reload-Smoke mit allen Debug Views.

### Erreicht 2026-05-05

- Die Material-LUT-Roughness wird fuer Wasser, Lava, Eis, Glas, Ores, Glow Crystal und Ancient Lantern bewusst gesetzt und im Terrain-Shader gelesen.
- `chunk.frag` nutzt `materialRoughness()` und `applyMaterialSheen(...)` fuer datengetriebenen Oberflaechen-Sheen statt neuer Block-ID-Sonderfaelle.
- Verifikation: `BlockRenderPropertiesTest.reflectiveBlocksExposeLowerRoughnessForShaderSheen`, `TerrainLightingShaderContractTest`.

### Akzeptanz

- neuer Block/Materialtyp braucht keinen Shader-ID-Hack.
- Shader-Contract-Test faengt Uniform-Brueche ab.

## P3.5 Voxel Render Taktiken evaluieren

### Ziel

Die Engine soll einen klaren Pfad fuer mehr Performance haben, ohne sofort riskante Komplettumbauten.

### Kandidaten

- Greedy Meshing pro Section und Layer.
- Meshlets fuer grobe Section-Unterteilung.
- Multi-draw oder instanced draw fuer kompatible Mesh-Parts.
- Texture arrays statt Atlas, falls Atlas-Grenzen erreicht werden.
- Per-face Material Packing zur Reduktion der Vertexgroesse.
- Quantisierte Vertexpositionen pro Section.
- optional Vertex Pulling fuer Terrain, wenn GL-Zielplattform passt.

### Akzeptanz

- jede Taktik bekommt zuerst einen Micro-Benchmark oder Headless-Test.
- keine Taktik ersetzt den stabilen Pfad ohne Fallback.

---

# P4 - Lighting, Fog und Atmosphaere V2

## P4.1 Light Jobs und Dirty Regions

### Ziel

Lighting soll inkrementell und budgetiert bleiben, auch bei vielen Spieleraktionen.

### Aufgaben

- ~~Light-Dirty-Regions von Geometry-Dirty-Regions trennen.~~ `ChunkSection.DirtyAspect.LIGHT` bleibt getrennt von Geometry/Fluid/BlockEntity und wird bei Light-Aenderungen separat markiert.
- ~~Sky-Light-Column-Cache pruefen.~~ Direktes Sky-Light-Seeding trennt offene Columns von Boundary-Propagation; ein persistenter Column-Cache bleibt nur bei Bedarf sinnvoll.
- Block-Light-Add/Remove als Job vorbereiten.
- ~~Fallback Full-Rebuild nur fuer betroffene Region.~~ Batch-Rebuilds bilden eine vereinigte betroffene Chunk-Region und propagieren Block-Light nur innerhalb geladener betroffener Chunks.
- ~~Light-Update-Zeit pro Job messen.~~ Chunk-/Block-Light-Arbeit wird ueber `ChunkBuildQueue.recordLighting(...)` im Job-HUD sichtbar; neue Chunk-Batches zaehlen als ein Light-Job.
- Tests fuer viele Lanterns, ~~Cave Roofs, Wasser und Chunkgrenzen~~. Cave Roofs, Wasser und Chunkgrenzen sind abgedeckt; Lantern-/Campfire-Stress bleibt unter Noch offen.

### Erreicht 2026-05-05

- `LightEngine.rebuildChunkLighting(world, centers)` kann mehrere Chunk-Zentren in einem Durchlauf aktualisieren und vermeidet doppelte Source-Scans fuer angrenzende Preview-Chunks.
- Client-Preview-Generation ruft Lighting pro neuem Chunk-Batch einmal auf; das reduziert Chunk-Loading-Spikes waehrend Bewegung.
- Betroffene Light-Chunks werden direkt ueber die 3x3-Nachbarschaft der Center aufgeloest, statt bei jedem Rebuild alle geladenen Chunks zu scannen.
- Block-Light-Emitter-Scans iterieren nur noch non-empty Sections und lokale Section-Arrays; leere Luft-Sections langer Chunks werden uebersprungen.
- Direktes Sky-Light-Seeding setzt offene vertikale Columns ohne jede Luftzelle als Propagation-Node zu queuen.
- Boundary-Seeds werden nur erzeugt, wenn horizontale Nachbarn wirklich verbessert werden koennen; offene Tall-Air-Chunks haben dadurch 0 Sky-Propagation-Nodes.
- Seitliche Sky-Light-Propagation nutzt dieselben transparenten Materialdaempfungen wie das vertikale Seeding, damit Wasser/Leaves unter Ueberhaengen nicht wie Luft leuchten.
- `LightEngine.WorkStats` macht betroffene Chunks, Sky-Cells, Boundary-Seeds, gescannte Emitter-Sections, besuchte Blocks, Emitter-Seeds und Propagation-Nodes testbar.
- Verifikation: `LightEngineTest.batchRebuildLightsMergedChunkRegionOnce`, `LightEngineTest.affectedChunkLookupIgnoresFarLoadedChunksWithoutScanningTheWorld`, `LightEngineTest.blockLightEmitterScanSkipsEmptySectionsInTallChunks`, `LightEngineTest.openSkyLightSeedingAvoidsPropagationQueueForTallAirChunks`, `LightEngineTest.skyLightBoundarySeedingTargetsOnlyColumnsThatCanImproveNeighbors`, `LightEngineTest.lateralSkyLightPropagationAppliesTransparentBlockAttenuation`, `ClientWorldMeshInvalidationTest.previewGenerationCanBeBudgetedAcrossFrames`.

### Noch offen

- Expliziter Stress-Test fuer viele gleichzeitig platzierte Lanterns/Campfires in sichtbaren Chunks.

### Akzeptanz

- mehrere Light-Updates erzeugen keine Frame-Spikes.
- Debug Views zeigen Sky/Block/Emissive getrennt plausibel.

## P4.2 Atmosphere Stack

### Ziel

Day/Night, Weather-Lightning, Underwater, Cave Darkness und Biome-Fog sollen kontrolliert gemischt werden.

### Aufgaben

- Reihenfolge dokumentieren:
  - Day/Night base.
  - Biome tint.
  - Cave/sky darkness.
  - Underwater override.
  - Weather flash.
  - Debug view override.
- `RenderSettings`-Contract fuer Atmosphere testen.
- Smoke fuer River, Cave, Old Ruins, Frost Peaks, Mushroom Grove.
- sicherstellen, dass Weather-Lightning keine Block-Light-Rebuilds triggert.

### Akzeptanz

- Atmosphaerenwechsel sind sichtbar, aber nicht unfair.
- Shader bleibt uniform-basiert und datengetrieben.

## P4.3 Optional Colored Light Research

### Ziel

Colored Light nur vorbereiten, nicht ungeplant in Alpha-Systeme druecken.

### Aufgaben

- Speicherbedarf fuer RGB-Light pro Voxel/Section abschaetzen.
- Alternative pruefen: wenige lokale colored light volumes im Shader.
- Debug-Ansicht fuer colored contribution planen.
- Performance-Budget definieren.

### Akzeptanz

- klare Entscheidung: Voxel-RGB-Light, Shader-Volumes oder kein Colored Light fuer V1.

---

# P5 - Physics, Collision und Fluids V2

## P5.1 Collision Caches

### Ziel

Physics soll stabile Kosten haben, auch bei komplexen Chunks.

### Aufgaben

- ~~Chunk-/Section-Collision-Cache aus Blockdaten ableiten.~~ `CollisionShapeCache` erzeugt Movement- und Projectile-Shapes sectionweise aus Blockdaten.
- ~~Cache bei Block-Updates invalidieren.~~ Client und Server invalidieren betroffene Section-Caches bei Block-/Chunk-Updates und Unload.
- ~~Broadphase fuer Entities und Projectiles pro Chunk/Section.~~ Player-/Entity-/Projectile-Shape-Queries laufen ueber denselben sectionweisen Cache-Pfad.
- ~~Debug-View fuer Collision Sections.~~ Debug-Bounds bleiben vorhanden; das HUD zeigt zusaetzlich Collision-Cache-Sections, Shapes, Limit und Evictions.
- Tests fuer negative Y, Kanten, Treppen/Slabs falls eingefuehrt.

### Erreicht 2026-05-05

- `CollisionShapeCache` besitzt eine LRU-Retention pro Shape-Set, damit lange Explore-Sessions alte Section-Shapes freigeben.
- `CollisionShapeCache.CacheStats` meldet Sections, Shapes, Retention-Limit und Evictions; `GameClient` zeigt diese Werte in der `PHYS`-HUD-Zeile.
- Verifikation: `CollisionShapeCacheTest.cacheEvictsLeastRecentlyUsedSectionsWithinBudget`, `GameSettingsTest`, `EngineFrameStatsTest`.

### Akzeptanz

- Player/Entity/Projectile-Queries nutzen dieselben Collision-Regeln.
- ~~keine unbounded allocations im Physics-Step.~~ Section-Shape-Caches sind pro Shape-Set begrenzt und raeumen alte Eintraege LRU-basiert.

## P5.2 Fluid Runtime V2

### Ziel

Wasser soll spielbar, renderbar und physikalisch konsistent sein.

### Aufgaben

- ~~Fluid-Flags im World/Chunk Cache zentralisieren.~~ `ChunkTerrainCache.FluidSurface` und `FluidBlocks` sind der gemeinsame Contract fuer Terrain-Surface und Block-Fluid-Erkennung.
- Flow nur einfuehren, wenn Budget und Save-Modell klar sind.
- ~~Water surface data fuer Rendering vorbereiten:~~
  - ~~shore mask.~~
  - ~~depth hint.~~
  - ~~optional foam flag.~~
- ~~Physics und Rendering nutzen dieselbe Fluid-Erkennung.~~ Terrain-Queries laufen ueber `ChunkTerrainCache.FluidSurface`; Wasser/Lava-Block-Erkennung laeuft ueber `FluidBlocks`.

### Erreicht 2026-05-05

- `ChunkTerrainCache` speichert pro Column kompakte Fluid-Surface-Daten: Depth-Hint, Shore-Mask und Foam-Flag.
- `FluidSurfacePlanner` berechnet diese Daten seed-stabil aus Worldgen-Hoehen und sampelt nur Chunk-Grenzen ueber den Generator nach.
- `ClientWorld` bietet `terrainFluidSurfaceAt(...)`, `terrainFluidDepthHintAt(...)`, `terrainShoreMaskAt(...)` und `terrainFluidSurfaceFlagsAt(...)` fuer Rendering/Diagnostics/Physics-UI an.
- `FluidBlocks` zentralisiert Wasser-/Lava-Erkennung fuer ServerWorld, ClientWorld, Collision-Shapes und Section-Fluid-Dirtying.
- Verifikation:
  - `./gradlew :common:test --tests dev.voxelgame.common.world.ChunkTerrainCacheTest --tests dev.voxelgame.common.world.OverworldGeneratorTest --tests dev.voxelgame.common.world.ChunkSectionDirtyTest --tests dev.voxelgame.common.block.FluidBlocksTest --tests dev.voxelgame.common.world.gen.FluidSurfacePlannerTest --no-daemon --max-workers=1`
  - `./gradlew :client:test --tests dev.voxelgame.client.world.ClientWorldMeshInvalidationTest --tests dev.voxelgame.client.world.ClientWorldCollisionTest --no-daemon --max-workers=1`
  - `./gradlew :server:test --tests dev.voxelgame.server.world.ServerWorldTest --no-daemon --max-workers=1`

### Akzeptanz

- Spieler, Partikel und Rendering stimmen bei Wasser/Unterwasser ueberein.
- Low-Preset kann Wasserkosten sichtbar reduzieren.

## P5.3 Surface Physics V1

### Ziel

Blockoberflaechen sollen Movement-Feel beeinflussen, ohne dass Client oder Server eigene Sonderfaelle fuer Eis, Schnee, Sand oder Wege pflegen.

### Erreicht 2026-05-05

- `BlockSurfacePhysics` ist der Common-Contract fuer Surface-Materialien und mappt registrierte Blocks auf Reibungs-, Beschleunigungs-, Speed- und Jump-Multiplikatoren.
- `PlayerPhysics.stepSurvival(...)` akzeptiert optional eine `SurfaceQuery`; alte Aufrufer bleiben kompatibel, neue Server-/Client-Integrationen koennen dieselbe Regelquelle nutzen.
- `ClientWorld.surfaceAt(...)` und `ServerWorld.surfaceAt(...)` stellen die Runtime-Sampler fuer Prediction und Autoritaet bereit; `ClientPlayerController` gibt die Query direkt an `PlayerPhysics`.
- `PlayerMovementRules` besitzt einen Surface-aware Survival-Speed-Envelope, den `ServerConnectionHandler` bei Movement-Validation nutzt.
- Das Debug-HUD zeigt Surface-Key, Speed- und Friction-Multiplikator in der `PHYS`-Zeile.
- Verifikation: `BlockSurfacePhysicsTest`, `PlayerPhysicsTest.survivalStepUsesSurfaceFrictionForIceSliding`, `PlayerPhysicsTest.survivalStepUsesSurfaceSpeedForSnowAndPaths`, `PlayerPhysicsTest.survivalStepUsesLandingSurfaceForBufferedJump`, `PlayerPhysicsTest.movementRulesUseSurfaceSpeedForServerDeltas`, `ClientWorldCollisionTest.playerSurfaceUsesLoadedBlockBelowFeet`, `ServerWorldTest.playerSurfaceUsesAuthoritativeBlockBelowFeet`.

### Naechste Schritte

- Surface-Multiplikatoren in Replay-Metadaten aufnehmen, sobald Player-Movement-Replays erweitert werden.
- StatusEffect-Multiplikatoren und Surface-Multiplikatoren in einer gemeinsamen Debug-Zusammenfassung anzeigen.

---

# P6 - Persistenz und Region Storage V2

## P6.1 Region Files

### Ziel

World-Saves sollen skalieren und nicht bei jeder kleinen Aenderung grosse Dateien schreiben.

### Fortschritt 2026-05-02

- `SaveQueue` fuehrt den Engine-Job-Slot `save.write` serverseitig als koaleszierende Hintergrund-Queue ein.
- `RegionFileLayout` definiert 32x32-Regionen, negative Chunk-Zuordnung, stabile Region-Pfade und Save-Keys.
- `docs/NETWORKING_AND_PERSITENCE_TODO_LIST.md` haelt Header-/Index-/Chunk-Record-Plan, Atomic-Write-Regel und Properties-V1-Migrationspfad fest.
- Properties-V1 bleibt aktiver WorldSave-Writer, bis ein Region Reader/Writer mit Migrationstests vorhanden ist.

### Aufgaben

- ~~Region-Layout definieren, z. B. 32x32 Chunks pro Region.~~
- Dirty Chunk Deltas getrennt von generiertem Basisterrain speichern.
- Kompression evaluieren.
- ~~atomare Writes mit Temp-Datei und Rename.~~ bleibt fuer Properties-V1 aktiv; Region-Writer muss denselben `SaveFiles`-Contract nutzen.
- Backup/Migration-Regeln pro Version beibehalten.
- ~~Save-Queue im Hintergrund mit Budget.~~ V1 besitzt max pending writes, Coalescing, Flush und Metriken; Diagnostics-Anbindung bleibt offen.

### Akzeptanz

- langer Explore erzeugt kontrollierte Save-Dateien.
- Crash waehrend Save korruptiert nicht den letzten stabilen Stand.

## P6.2 Save Migration Contracts

### Ziel

Neue Versionen sollen alte Saves sicher laden.

### Aufgaben

- Migrationstests fuer mehrere Versionen.
- Unknown Block/Item/Entity-Fallbacks dokumentieren.
- Registry-Alias-Bericht beim Laden.
- Save-Inspector Tool fuer Region/Player/BlockEntity.

### Akzeptanz

- alte Test-Saves bleiben ladbar.
- Migration erstellt Backup und meldet Ergebnis.

---

# P7 - Networking und Multiplayer Runtime V2

## P7.1 Snapshot- und Delta-System

### Ziel

Netzwerktraffic soll mit Spielerzahl, Entities und Chunks skalieren.

### Aufgaben

- Entity-Snapshots mit Delta-Kompression vorbereiten.
- BlockUpdate-Batches pro Chunk/Section.
- Chunk-Packets komprimieren und groessenbegrenzen.
- Packet-Budget pro Tick.
- Debug-Stats fuer Bytes pro Packet-Typ.
- Tests fuer Max-Payload, Unknown-Packet, Partial-Read.

### Akzeptanz

- Clients erhalten nur relevante Daten.
- grosse Chunks oder Entity-Wellen sprengen kein Tick-Budget.

## P7.2 Prediction und Reconciliation

### Ziel

Spielerbewegung soll sich lokal direkt anfuehlen und trotzdem servervalidiert bleiben.

### Aufgaben

- Client Input History begrenzen.
- Server Snapshot mit Movement Sequence.
- Reconciliation smoothing testen.
- Debug-Overlay fuer Prediction Error.
- Smoke fuer Lag, Packet Loss und Teleport.

### Akzeptanz

- Korrekturen sind sichtbar debugbar und spielbar weich.
- Server bleibt autoritativ.

---

# P8 - Entity Runtime und AI V2

## P8.1 Entity Zones und Budgets

### Ziel

Entities sollen lebendig wirken, aber niemals Tickzeit oder Netzwerk dominieren.

### Aufgaben

- Entity-Zonen pro Chunk/Region definieren.
- Active, Simulated, Parked und Dormant States.
- Tickbudget pro Entity-Klasse.
- Spawn- und Despawn-Budgets.
- Client-Culling und Server-Interest abgleichen.
- Debug-HUD fuer aktive/geparkte Entities.

### Akzeptanz

- lange Explore-Sessions parken entfernte Entities.
- Entity Count und Tickzeit bleiben kontrolliert.

## P8.2 AI Tick Scheduler

### Ziel

Komplexere AI darf nicht alle Entities jedes Tick voll auswerten.

### Aufgaben

- AI-Intervalle pro State definieren.
- Sensor-Queries budgetieren.
- Pathfinding nur mit Budget und Cache.
- Fallback-Verhalten bei Budgetmangel.
- Tests fuer State-Wechsel, Despawn, Save/Load.

### Akzeptanz

- AI skaliert mit Anzahl sichtbarer/aktiver Entities.
- keine AI-Spikes beim Betreten dichter Biome.

---

# P9 - Tooling und Debug UX V2

## P9.1 Debug Command Registry

### Ziel

Debug-Commands sollen auffindbar, testbar und konsistent sein.

### Aufgaben

- Command-Metadaten: Name, Aliase, Usage, Kategorie.
- `/help` aus Registry generieren.
- Command-Tests fuer wichtige Debugpfade.
- Berechtigungs-/Build-Flag fuer spaetere Release Builds vorbereiten.

### Akzeptanz

- neue Debug-Commands brechen Help-Text nicht.
- Commands fuer Rendering, Lighting, Physics, Network und Save sind gruppiert.

## P9.2 Engine Diagnostics Screen

### Ziel

Nicht alle Diagnosen muessen ins kleine HUD.

### Aufgaben

- Diagnostics-View fuer:
  - frame timings.
  - chunk jobs.
  - GPU resources.
  - network bytes.
  - save queue.
  - entity counts.
- Copy/export als Markdown oder JSON.
- Screenshot-Hinweis fuer Smoke-Reports.

### Akzeptanz

- ein Tester kann einen aussagekraeftigen Engine-Report erzeugen.

---

# P10 - Performance Budgets und Zielwerte

## P10.1 Alpha Budgets

### Zielwerte

- Low: 30 FPS auf schwacher integrierter GPU bei niedriger Render Distance.
- Medium: 60 FPS Ziel bei normaler Render Distance.
- High: bessere Sichtweite und Effekte ohne unbounded spikes.

### Zu budgetieren

- ~~Framezeit.~~ `EngineFrameStats.Budgets` und HUD-Zeile `BUD`
- ~~Chunkgen ms/frame.~~ Budget-Nutzung gegen Alpha-Profil sichtbar.
- ~~Lighting ms/frame.~~ Budget-Nutzung gegen Alpha-Profil sichtbar.
- ~~Meshing ms/frame.~~ nutzt das aktive Mesh-ms-Budget aus `GameSettings`.
- ~~GPU Upload bytes/ms pro Frame.~~ Upload-ms und Upload-Bytes haben eigene Budget-Nutzung.
- ~~Draw Calls pro Pass.~~ Terrain-Pass-/Layer-Werte sind sichtbar; Nicht-Terrain-Paesse bleiben P3.1-Diagnostics-Ausbau.
- ~~Triangles pro Pass.~~ Terrain-Pass-/Layer-Werte sind sichtbar; Nicht-Terrain-Paesse bleiben P3.1-Diagnostics-Ausbau.
- ~~Particle Count.~~ Particle Budget Usage ist sichtbar.
- ~~Ambient Particle Source Scans.~~ `GameSettings.ambientParticleSourceScanIntervalSeconds()` koppelt die Scan-Frequenz an Particle Quality, damit Low/Custom-Presets weniger World-Scan-Arbeit ausloesen.
- 🔴 Entity Tickzeit braucht P8.1/P8.2: Entities brauchen einen Tick-Scheduler, der pro Entity-Klasse `started/finished/skipped` und `tickMilliseconds` misst. `EngineFrameStats.Entities` sollte danach `entityTickMilliseconds`, `averageEntityTickMilliseconds` und `entityBudgetUsage` bekommen; Server-Stats muessen dieselbe Zahl fuer Multiplayer liefern.
- ~~Netzwerk bytes/s.~~ HUD-Budget nutzt aktuelle Packet-Rate mal durchschnittliche Packet-Groesse als sichtbare Schaetzung.
- ~~🔴 Save Queue ms/s braucht P6.1: Die Save-Queue soll Writes als `save.write` Jobs messen, pro Sekunde `queuedWrites`, `writtenBytes`, `writeMilliseconds`, `averageWriteMilliseconds` und `failedWrites` melden und diese Werte an `EngineFrameStats.Budgets`/Diagnostics weiterreichen.~~
  Erledigt: 2026-05-02 - `SaveQueueStats` misst Save-Write-Raten; `SERVER_STATS_SNAPSHOT` Protocol Version 25 liefert sie an den Client; `EngineFrameStats.Jobs.saveWrite` und `EngineFrameStats.Budgets.saveWriteUsage` zeigen Pending/Running/Completed/Failed und ms/s-Budget.

### Erreicht 2026-05-01

- `EngineFrameStats.Budgets` berechnet Alpha-Budget-Ziele fuer Low/Medium/High bzw. Custom-Settings.
- Das Debug-HUD zeigt eine `BUD`-Zeile fuer Frame, Chunkgen, Light, Mesh, GPU-ms, GPU-Bytes, Draw, Triangles, Particles, Entities, Save und Network.
- Section-Dirty-Budgets sind direkt daneben in der `SECTIONS`-Zeile sichtbar.
- Verifikation: `./gradlew :client:test --tests dev.voxelgame.client.EngineFrameStatsTest --no-daemon --max-workers=1 --rerun-tasks`
- Verifikation 2026-05-05: `GameSettingsTest.particleQualityAlsoBudgetsAmbientSourceScans`.

### Akzeptanz

- ~~Budgets sind im Debug-HUD oder Diagnostics Screen sichtbar.~~ Basiswerte und Save-Queue-ms/s sind im Debug-HUD sichtbar; echte Entity-Tickzeit braucht P8.
- ~~Presets veraendern Budgets nachvollziehbar.~~ Profilziele folgen Render-Distance/Preset und Mesh-/Upload-ms nutzen die aktiven Settings.

## P10.2 Benchmark Seeds

### Pflichtszenarien

- Spawn Baseline.
- River / Lakeside.
- Mushroom Grove.
- Cave Pocket.
- Frost Peaks.
- Old Ruins.
- Long Explore 10 Minuten.
- Join Local mit zwei Clients, sobald verfuegbar.

### Akzeptanz

- jede Performance-Aenderung nennt mindestens ein Szenario.
- Regressionen haben Vergleichswerte.

---

# P11 - Release- und Smoke-Gates

## P11.1 Alpha Release Gate

### Muss pruefen

- `./gradlew test`.
- `./gradlew buildGame`.
- `./gradlew profileSingleplayerJfr` fuer ein kurzes Profil.
- `WORLD_SMOKE_TESTS.md` Spawn, River, Cave, Mushroom Grove, Long Explore.
- `/shaderreload`.
- `/debugview light|sky|block|emissive|layer|transparent`.
- `/preset low|medium|high`.
- Join Local Smoke.

### Akzeptanz

- kein Release ohne dokumentierte Smoke-Ergebnisse.
- bekannte Risiken werden explizit im Release-Text genannt.

## P11.2 Regression Labels

### Kategorien

- `perf-frame`
- `perf-memory`
- `render-shader`
- `render-transparency`
- `light-seam`
- `chunk-stream`
- `save-migration`
- `net-interest`
- `entity-budget`
- `physics-collision`

### Akzeptanz

- Fehlerberichte lassen sich einem Engine-Bereich zuordnen.

---

# P12 - Spaetere Engine-Forschung

Diese Punkte erst angehen, wenn V2-Basis stabil ist.

## Kandidaten

- echte Terrain-LOD oder impostor distant chunks.
- async GPU readback fuer Occlusion/Debug.
- compute-basierte Meshing-/Lighting-Experimente.
- Texture Streaming fuer grosse Asset-Sets.
- replay-basiertes deterministisches Debugging.
- integrierter Headless Benchmark Runner.
- Server-Sharding oder Region-Worker fuer sehr grosse Welten.

## Entscheidungskriterien

- messbarer Nutzen.
- klarer Fallback.
- keine Destabilisierung der Alpha.
- Tests oder reproduzierbarer Benchmark vor Implementierung.

---

# Definition of Done fuer Engine-V2-Aufgaben

Eine Aufgabe gilt erst als abgeschlossen, wenn:

- Code und Datenmodell dokumentiert sind.
- relevante Unit-/Contract-Tests gruen sind.
- mindestens ein Smoke-Check genannt ist.
- Debug-/Profiling-Sichtbarkeit vorhanden oder begruendet nicht noetig ist.
- Performance- oder Speicherwirkung bei relevanten Aufgaben gemessen wurde.
- Multiplayer- oder Save-Auswirkungen bewertet wurden.
- keine neuen harten Shader-Block-ID-Sonderfaelle entstehen.
- Todo-Punkte erst nach Verifikation gestrichen werden.

---

# Naechste empfohlene Arbeitsschritte

## Sofort

- ~~P1.1 Frame-Phasen explizit machen.~~
- ~~P2.1 Section-first Chunk Lifecycle planen und Tests erweitern.~~
- ~~P3.1 Render Pass Graph als kleine Struktur einfuehren.~~
- ~~P10.1 Alpha Budgets im Debug-HUD/Diagnostics sichtbar machen.~~

## Danach

- P3.2 Section Draw Ranges und Upload-Staging.
- P4.1 Light Jobs und Dirty Regions.
- P6.1 Region Files.
- P7.1 Snapshot- und Delta-System.

## Spaeter

- P3.3 Occlusion-Taktiken.
- P8.2 AI Tick Scheduler.
- P12 Forschungsthemen nur mit Benchmark.

---

# P13 - Alpha Core Gaps und Ownership

Dieser Block sammelt Punkte, die beim Projektueberblick als noch nicht klar genug verteilt aufgefallen sind. Sie sind bewusst engine-weit formuliert und muessen mit `docs/IMPLEMENTATION_PLAN.md` abgeglichen werden.

## P13.1 Monolithen gezielt aufteilen

### Status 2026-05-01

- ~~🟠 In Arbeit: Project Manager erstellt die Refactor-Map fuer `GameClient`, `ServerConnectionHandler`, `OverworldGenerator`, `WorldRenderer` und Content-Registries und gleicht Reihenfolge/Ownership mit `docs/IMPLEMENTATION_PLAN.md` ab.~~
  Erledigt: 2026-05-01, Refactor-Map, Ownership, Reihenfolge und erste sichere Extraktionen sind unten dokumentiert.
  Verifikation: Abgleich mit `docs/IMPLEMENTATION_PLAN.md`, Nachbar-TODOs und Dateigroessen der Kernklassen; `./gradlew :common:test --tests dev.voxelgame.common.registry.RegistryTest --tests dev.voxelgame.common.world.OverworldGeneratorTest --no-daemon --max-workers=1`.
- 🔴 Offen: Die eigentlichen Extraktions-Slices bleiben Phase-1-Arbeit der jeweiligen Owner und muessen mit Tests/Smokes nachgewiesen werden.

### Problem

Einige zentrale Dateien tragen zu viele Verantwortungen und bremsen parallele Arbeit:

- `GameClient` mischt Main Loop, Input, lokale Gameplay-Aktionen, Screens, HUD, Debug, Chat, Settings, Audio-Hooks und Session-Management.
- `ServerConnectionHandler` mischt Protocol Routing, Auth/Login, Actions, Movement Validation, Interest, Inventory, Storage, Cooking, Saves und Stats.
- `OverworldGenerator` enthaelt Sampling, Terrain Fill, Decoration, Structures, Spawn Safety und Metrics.
- `WorldRenderer` ist schon besser strukturiert, braucht aber fuer Shadows, Sprite Rendering und Draw-Ranges weitere Aufteilung.
- `CraftingRecipes` und Content-Registries sind stark codegetrieben und werden bei mehr Items/Stations unhandlich.

### Aufgaben

- ~~Refactor-Map mit Zielpaketen schreiben:~~
  - `client.session`
  - `client.input`
  - `client.screens`
  - `client.hud`
  - `client.commands`
  - `server.protocol`
  - `server.actions`
  - `server.interest`
  - `server.persistence`
  - `common.content`
  - `common.actions`
  Erledigt: 2026-05-01, siehe Refactor-Map unten.
  Verifikation: Doku-Abgleich mit `docs/IMPLEMENTATION_PLAN.md`, `docs/ARCHITECTURE.md`, den primaeren TODO-Listen und den realen Dateigroessen; fokussierter Common-Testlauf gruen.
- Pro Refactor erst Contract-/Layout-/Codec-Test sichern, dann verschieben.
- Keine grossen Feature-PRs direkt in Monolithen bauen, wenn ein kleiner Extraktionsschritt sinnvoll ist.
- ~~Dateien ueber 1000 Zeilen mit Owner, Zielzustand und Aufteilungsreihenfolge markieren.~~
  Erledigt: 2026-05-01, `GameClient`, `ServerConnectionHandler` und `OverworldGenerator` sind unten explizit markiert; `WorldRenderer` und Content-Registries sind wegen zentraler Architekturwirkung ebenfalls aufgenommen.
  Verifikation: `wc -l` fuer die Kernklassen und Querverweis auf Rendering, Worldgen, Networking, UI/HUD und Gameplay TODOs.

### Refactor-Map 2026-05-01

| Block | Owner | Prioritaet | Hauptrisiko | Zielzustand | Erste sichere Extraktion |
| --- | --- | --- | --- | --- | --- |
| `GameClient` ca. 5759 Zeilen | Lead UI/UX fuer Screens/HUD, Physics und Engine Worker fuer Input/Targeting, Project Manager als Steward | 🔴 | Merge-Konflikte, UI-State und lokale Gameplay-Aktionen vermischen sich, serverkritische Entscheidungen koennen clientseitig nachwachsen | `client.session`, `client.input`, `client.screens`, `client.hud`, `client.commands`, `client.interaction` | `ScreenContext` plus `ClientInputState` als reine Snapshot-/State-Objekte; bestehende Methoden delegieren zuerst weiter. Danach `DebugCommandRegistry` und `HudPresenter` als getrennte Slices. |
| `ServerConnectionHandler` ca. 2102 Zeilen | Main Networking Dev, Project Manager fuer Contract-Grenzen | 🔴 | Auth, Routing, Movement, Inventory, Stations, Saves und Interest koennen sich gegenseitig regressieren; Netty/Event-Loop darf nicht blockieren | `server.protocol`, `server.actions`, `server.movement`, `server.interest`, `server.persistence`, `server.stations` | `PacketDispatch`/`ConnectionCommandRouter` als duenne Dispatch-Schicht mit bestehenden Handler-Methoden als Delegates. Parallel Common-`ActionRequest`/`ActionValidationResult` Skeleton fuer P1 ohne Migration aller Aktionen. |
| `OverworldGenerator` ca. 1003 Zeilen | Lead Game Design Engineer und Project Manager | 🟠 | Seed-Reproduzierbarkeit, Spawn Safety und Structure-Platzierung koennen bei Extraktion leise driften | `ClimateSampler`, `HeightmapSampler`, `BiomeResolver`, `TerrainFiller`, `FeaturePlanner`, `StructurePlanner`, `SpawnPlanner`, `GenerationMetricsCollector` | `ClimateSampler` + `BiomeResolver` als pure Services aus vorhandenen Methoden; vorher/nachher `OverworldGeneratorTest` und Smoke-Seeds gegen Height/Biome/Spawn laufen lassen. |
| `WorldRenderer` ca. 711 Zeilen, aber zentraler GL-Knoten | Lead Engine Developer | 🟠 | GL-State-Leaks, GPU-Resource-Lifetime, Upload-Spikes und Draw-Range-Arbeit landen sonst wieder in einer Klasse | `RenderPassExecutor`, `RenderStateGuard`, `TerrainRenderer`, `TerrainUploadQueue`, `VisibilityCollector`, spaeter `WaterRenderer`, `SelectionRenderer` | P10.1a fortfuehren: pure `VisibilityCollector` und `RenderPassExecutor` zuerst, danach `TerrainUploadQueue`; GL-lastsensitive Slices mit `WorldRendererTest`/Render-Pass-Contracts absichern. |
| Content-Registries und `CraftingRecipes` ca. 650 Zeilen | Lead Game Design Engineer, Main Networking Dev fuer Save/Protocol, Lead Engine Developer fuer Render-Materialien, Project Manager als Steward | 🔴 | Items, Blocks, Recipes, Loot, Render, Physics und Networking koennen in getrennten Java-Sonderfaellen auseinanderlaufen | `common.content`, `common.tags`, `common.recipes`, `common.actions`, klare Alias-/Migration-Reports | Codebasierte `ContentTagRegistry` V1 mit Item-/Block-/Entity-Tags und Coverage-Tests. JSON/Codegen erst nach stabilem API-Contract pruefen. |

### Reihenfolge und Konfliktgrenzen

1. 🔴 `ServerConnectionHandler` und `GameClient` duerfen parallel geschnitten werden, solange zuerst nur Routing-/State-/ViewModel-Contracts entstehen und bestehende Methoden delegiert bleiben.
2. ~~🔴 `ContentTagRegistry` V1 blockiert groessere neue Item-, Physics-, Station- und Action-Arbeit.~~
   Erledigt: V1 steht in `common.content` mit `ContentKey`, Item-/Block-/Entity-Tags, Alias-Aufloesung, Coverage-Report und Tests. Folgearbeit: ActionPipeline/Physics/Rendering schrittweise auf die Read-API umstellen.
3. 🟠 `WorldRenderer` P10.1a muss vor Shadows, Draw-Ranges, Water- und Selection-Ausbau weiterlaufen.
4. 🟠 `OverworldGenerator` P7.1 muss vor datengetriebenen Feature-/Structure-Tables mindestens Sampler/Resolver/Filler-Grenzen haben.
5. Neue Featurearbeit in einer markierten Monolith-Datei braucht entweder einen kleinen vorgelagerten Extraktionsslice oder eine dokumentierte Begruendung, warum der Fix nicht warten kann.

### DoD fuer Monolith-Extraktionen

- Der erste Slice ist ein Contract, ViewModel, Router, Registry oder Service mit klarer Paketgrenze.
- Bestehende Methoden delegieren im ersten Schritt weiter, wenn ein direkter Move zu riskant waere.
- Mindestens ein fokussierter Test haelt Layout, Codec, Routing, Seed-Stabilitaet, Render-Pass-Reihenfolge oder Registry-Abdeckung fest.
- Jede serverkritische Aktion bleibt serverautoritativ; Client-Slices duerfen nur Intent, Preview oder ViewModel liefern.
- Jede GL-/Rendering-Extraktion benennt State-Lifetime, Resource-Ownership und passenden Smoke.
- Die betroffene TODO-Liste nennt Owner, Prioritaet, Verifikation und Restunsicherheit.

### Akzeptanz

- Neue Features landen in klaren Modulen statt in `GameClient` oder `ServerConnectionHandler`.
- Bestehende Tests bleiben gruen und Verhalten bleibt nachvollziehbar.
- Parallele Agenten koennen ohne staendige Konflikte arbeiten.

## P13.2 Gemeinsames Content- und Tag-System

### Status 2026-05-01

- ~~🟠 In Arbeit: Project Manager definiert die erste Ownership- und Contract-Schnittstelle, Lead Game Design Engineer behaelt die Feature-Ownership fuer Gameplay-Tags und Balancing.~~
  Erledigt: 2026-05-01, Contract-Schnitt und Owner stehen unten.
  Verifikation: Abgleich mit Gameplay, Physics, Rendering und Networking TODOs; `RegistryTest` und `OverworldGeneratorTest` gruen.
- ~~🔴 In Arbeit: `ContentTagRegistry` V1 wird als Common-Contract mit `ContentKey`, fehlenden Tags und Tests fuer Aliase, unbekannte Keys und Tag-Coverage umgesetzt.~~
  Erledigt: 2026-05-01, `common.content.ContentTagRegistry` leitet V1-Tags codebasiert aus `Items`, `Blocks`, Recipes und bekannten Entity-Typen ab.
  Verifikation: `ContentTagRegistryTest` XML/HTML meldet 8 Tests, 0 Failures; kompletter Gradle-Task wurde danach durch parallele Workspace-Gradle-Laeufe gestoert, siehe P0.1.
- ~~🔴 Offen: Implementierung von `ContentTagRegistry` V1 mit Tests fuer unbekannte Keys, Alias-Aufloesung und Tag-Abdeckung.~~
  Erledigt: 2026-05-01, Coverage-Report, `entries(ContentKind)`, `keysWithTag(...)`, Unknown-Key-Verhalten und Alias-Tests sind vorhanden.
  Verifikation: `common/build/test-results/test/TEST-dev.voxelgame.common.content.ContentTagRegistryTest.xml`.

### Ziel

Items, Blocks, Entities, Actions, Loot, Stations und Render-/Physics-Eigenschaften sollen nicht in vielen Java-Switches auseinanderlaufen.

### Aufgaben

- ~~`ContentTagRegistry` planen:~~
  - `flammable`
  - `fuel`
  - `food`
  - `ranged`
  - `ammo`
  - `comfort_source`
  - `station`
  - `storage`
  - `cold`
  - `hot`
  - `floaty`
  - `heavy`
  - `transparent`
  - `emissive`
  Erledigt: 2026-05-01, Contract-Schnitt und Owner sind unten festgelegt.
  Verifikation: Abgleich mit `GAMEPLAY_TODO_LIST.md` P9.2, `PHYSICS_TODO_LIST.md` roten Engine-Abhaengigkeiten, Rendering-Materialdaten und Networking-ActionPipeline; `ContentTagRegistryTest` XML gruen.
- ~~Tags zuerst codebasiert einfuehren, spaeter JSON/Codegen pruefen.~~
  Erledigt: 2026-05-01, V1 ist codebasiert; JSON/Codegen bleibt spaeteres Datenpipeline-Thema.
  Verifikation: Default-Tags werden aus `Items`, `Blocks`, Recipes und Entity-Keys abgeleitet.
- ~~Tests fuer unbekannte Keys, Alias-Aufloesung und Tag-Abdeckung.~~
  Erledigt: 2026-05-01.
  Verifikation: `ContentTagRegistryTest`.
- Render, Physics, Gameplay und Networking nur ueber stabile Content-APIs koppeln.

### ContentTagRegistry Contract V1 2026-05-01

| Contract | Owner | Prioritaet | Erwartete API | Erste sichere Umsetzung |
| --- | --- | --- | --- | --- |
| Item-/Block-/Entity-Tags | Lead Game Design Engineer | 🔴 | `ContentTagRegistry`, `ContentTag`, `ContentKey`, `tagsFor(key)` und `hasTag(key, tag)` in `common.content` | Codebasierte Registry aus bestehenden `Items`, `Blocks` und Entity-Typen ableiten; keine JSON-Ladung im ersten Slice. |
| Action-relevante Tags | Main Networking Dev mit Lead Game Design Engineer | 🔴 | Tags wie `food`, `fuel`, `ranged`, `ammo`, `station`, `storage` fuer `ActionPipeline`-Validierung | Nur lesende Nutzung in Common-Regeln vorbereiten; Server bleibt Autoritaet. |
| Render-/Physics-Tags | Lead Engine Developer und Physics und Engine Worker | 🟠 | Tags wie `transparent`, `emissive`, `floaty`, `heavy`, `cold`, `hot` als stabile Queries | V1 darf bestehende Material-/Blockfelder spiegeln, aber keine Shader-Sonderfaelle einfuehren. |
| Save-/Migration-Sicht | Main Networking Dev | 🟠 | Alias-/Unknown-Key-Report fuer Saves und Protocol-Diagnose | An bestehende `Registry.aliases()` anschliessen; Migration bleibt in P6.2/Persistence. |

### Akzeptanz

- Neue Items/Blocks brauchen weniger verstreute Speziallogik.
- Tags sind server- und clientseitig identisch.
- Content-Fehler fallen beim Start oder in Tests auf.

## P13.3 Engine Ownership Matrix

### Lead Engine Developer

- Rendering Pipeline, Shader, Lighting, Shadows, Sprite Rendering, Render-Pass-Logik, Material-/Atlas-System, GPU-Ressourcen, Profiling.

### Physics und Engine Worker

- Player/Entity/Projectile/Fluid Physics, Collision Shapes, Replay-Tests, Debug-Overlays, ClientWorld/Engine-Oberflaechenarbeit und kleine Engine-Extraktionen.

### Lead Game Design Engineer

- Gameplay-Systeme, Item-/Block-/Entity-Tags, Core Loop, Station-Progression, Biome-Gameplay und Content-Anforderungen an Engine/Networking.

### Lead UI/UX Frontend Developer

- UI-/HUD-Screens, Component Library, ViewModels, Transaction Feedback, Launcher/Game UX und UI-Anforderungen an Engine Diagnostics.

### Project Manager

- Refactor-Planung, Architekturgrenzen, technische Schulden, Bug-Triage, Test-Gates, DoD, Dokumentationspflege, Konfliktarme PR-Schnitte.

### Main Networking Dev

- Protocol, Netty, Interest, Reconnect, Server Actions, Save Queue, Region Storage, BlockEntity Sync und Multiplayer-Sicherheit.

### Akzeptanz

- Jede neue Engine-Aufgabe nennt Owner und betroffene TODO-Liste.
- Agenten arbeiten nach eigener Liste plus `docs/IMPLEMENTATION_PLAN.md`.
- Cross-Owner-Aenderungen bekommen vorher kleine Interface- oder Contract-PRs.

---

# P14 - Finished Game Engine Roadmap 2026-05-05

Owner: Project Manager, Lead Engine Developer, Main Networking Dev, Physics und Engine Worker. Game-Design-Schnittstellen gehoeren dem Lead Game Design Engineer.

Dieser Block ist aus dem aktuellen Code- und Docs-Ueberblick abgeleitet. Ziel ist nicht "mehr Engine um der Engine willen", sondern die fehlenden Runtime-Saeulen fuer ein fertiges Adventura: serverautoritativ, lange spielbar, debuggbar, speicherbar und bereit fuer mehr Content ohne neue Monolithen.

## P14.0 Bestandsaufnahme aus dem Code

Vorhanden:

- Serverautoritative Netty-Basis mit Protocol Contracts, PacketCodec-Tests, Interest-Filtern und ersten GameplayEvents.
- Common-Contracts fuer Actions, Tags, StatusEffects, Milestones, Stations, Journal, BiomeProgression und Cozy-Life.
- Client-/Server-Worlds mit Chunk Sections, LightEngine, CollisionShapeCache, physics tickets, chunk build queue and render budgets.
- Render Pipeline V3-Grundlage mit RenderPassPlan, RenderStateGuard, TerrainRenderer, VisibilityCollector, Material LUT, Atlas Validation, Debug Views and performance presets.
- Physics V2-Grundlage mit player/entity/projectile/fluid paths, partial-shape impacts, replay recorder and golden regression hooks.
- SaveQueue, PlayerSave, WorldSave, BlockEntityStore and RegionFileLayout as persistence V2 starting points.

Fehlt fuer ein fertiges Spiel:

- Einheitliche Runtime-Services fuer Actions, Progression, Stations, Entity Brains, Base Facts, World Events and Diagnostics.
- Durchgehende serverseitige Producer fuer Milestones, Journal, Goals, Recipe History, Structure Discoveries, Creature Discoveries and Rare Loot.
- Revisionierte BlockEntity-/Station-Transactions statt station-spezifischer Einzelpfade.
- Daten- und Balancing-Validation, die Items/Blocks/Recipes/Loot/Biomes/Structures/Assets/Save-Aliase zusammen prueft.
- Langzeit-Budgets fuer Entity AI, Station Jobs, Save IO, worldgen, mesh upload and memory retention.

## P14.1 Runtime Service Boundaries

### Ziel

Die Kernmechaniken sollen nicht weiter in `GameClient`, `ServerConnectionHandler`, `ServerWorld` oder `OverworldGenerator` wachsen. Neue Features brauchen kleine Runtime-Services mit klaren Contracts.

### Aufgaben

- `server.actions.ActionRuntime` planen:
  - registry of action handlers.
  - maps packets/intents to `ActionRequest`.
  - returns accepted/rejected `ActionExecutionResult`.
  - emits `GameplayEvent`s and inventory/world/station mutations.
  - owns cooldown/cost/durability timing, while Common owns validation rules.
- `server.progression.ProgressionRuntime` planen:
  - milestone unlocks.
  - goal completion.
  - journal entry unlocks.
  - recipe history.
  - biome/structure/creature discoveries.
  - rare-find flags.
  - idempotent save-key writes.
- `server.stations.StationRuntime` planen:
  - block entity snapshots.
  - station revisions.
  - active jobs.
  - public/private state split.
  - server transaction ids.
  - standard reject reasons.
- `server.entity.EntityBrainRuntime` planen:
  - AI tick scheduler.
  - behavior state machines.
  - perception/flee/follow/feed/attack tells.
  - parking outside active simulation tickets.
  - per-biome/structure encounter budgets.
- `common.diagnostics` oder equivalent telemetry DTOs definieren:
  - frame/render/chunk/light.
  - physics/collision/entity.
  - network/interest/action.
  - save/station/progression.
  - worldgen/loot/discovery.

### Akzeptanz

- Neue Featurearbeit nennt den Runtime-Service, der sie besitzt.
- `ServerConnectionHandler` routet mehr und entscheidet weniger.
- Jeder Service hat mindestens einen Contract-Test oder Service-Test.

## P14.2 ActionRuntime V2

### Ziel

Alle moment-to-moment interactions laufen langfristig ueber denselben serverautoritativen Pfad.

### Zu migrierende Actions

- `EatAction`: item food, hunger/heal/status, cooldown, feedback.
- `BlockInteractAction`: harvestables, campfire fuel, storage open, station inspect.
- `FeedEntityAction`: favorite food, daily cap, cooldown, friendship state, reject reasons.
- ~~`MeleeAttackAction`: range, damage source, item-driven damage/cooldown/knockback, durability cost and drops.~~
- `MeleeAttackAction`: no-kill creature rules and full ActionRuntime result object remain open.
- `ProjectileShootAction`: already first slice; extend to ammo, charge, bows, thrown items, cooldown data.
- `CraftAction`: inventory/workbench/forge recipe validation, missing station, transaction id.
- `CookAction`: campfire/cooking pot/forge jobs, fuel/heat/time/output claims.
- `SleepAction`: sleeping mat, night, comfort, shelter, danger, multiplayer readiness.
- `RepairUpgradeAction`: tool repair, durability, future rare upgrades.
- `AncientUseAction`: sealed ruins, ruin key/seal/fragment, altar acceptance and lore events.

### Erreicht 2026-05-05

- `WeaponItemRules` liefert den ersten Common-Read-Contract fuer `MeleeAttackAction`: Weapon-Erkennung, Damage, Cooldown, Knockback und spezielle Flags fuer schnelle/kristalline Waffen.
- Mineral-Schwerter sind als langlebige Items und Forge-Recipes registriert; der spaetere ActionRuntime-Slice muss diesen Contract serverautoritativ fuer Range, DamageSource, Durability und GameplayEvents verwenden.
- `MeleeAttackRules` erweitert den Common-Contract um Reichweite, Ziel-Akzeptanz, Tool-/Hand-Fallback und Durability-Kosten; `ServerConnectionHandler` nutzt diesen Contract fuer autoritative Entity-Attacks.
- Verifikation: `WeaponItemRulesTest`, `MeleeAttackRulesTest`, `ItemRegistryDataTest.mineralSwordItemsAreDurableWeaponsWithCanonicalAliases`, `CraftingRecipeTest.mineralSwordsExtendWorkbenchAndForgeProgression`.

### Engine Requirements

- Intent rate limits move from scattered constants into action metadata where possible.
- Action result includes:
  - accepted/rejected.
  - stable reason key.
  - affected inventory slots.
  - affected block/entity/station ids.
  - emitted gameplay events.
  - optional cooldown until timestamp.
- Debug HUD/Diagnostics can show action rejects per type.
- Replay/Smoke can record action sequence and expected authoritative results.

### Akzeptanz

- Two-client tests cannot duplicate station outputs or creature feeding rewards.
- Client can show pending/accepted/rejected without guessing.
- Adding a new action does not add a large new branch to `ServerConnectionHandler`.

## P14.3 Station And BlockEntity Runtime

### Ziel

Storage, Campfire, Workbench, CookingPot, Forge and future AncientAltar become one revisioned BlockEntity runtime instead of isolated packets and local UI assumptions.

### Aufgaben

- `BlockEntitySnapshot` V1:
  - type key.
  - position.
  - revision.
  - public state.
  - private viewer state.
  - dirty flags.
  - schema version.
- `StationSnapshot` V1:
  - station key.
  - slot groups.
  - fuel.
  - heat.
  - active recipe/job.
  - progress total/remaining.
  - output claim token.
  - error/reject key.
- Packets:
  - open snapshot.
  - delta update.
  - close.
  - transaction request.
  - transaction result.
- Save:
  - store station payload by block entity.
  - preserve unknown payloads for migration.
  - write active job progress.
  - idempotent generated loot markers.
- Tests:
  - two clients open same storage.
  - stale revision rejected.
  - output claim is once-only.
  - save/load active campfire/cooking/forge job.
  - block break removes or drops station inventory safely.

### Akzeptanz

- Station UIs are trustworthy online.
- No recipe output can be claimed twice.
- Save/load keeps active station state and unknown future payloads safely.

## P14.4 Entity Simulation And AI Budgets

### Ziel

Ambient entities already exist, but finished gameplay needs scalable brains, not just movement snapshots.

### Aufgaben

- Add behavior state contracts:
  - idle.
  - wander.
  - graze.
  - flee.
  - follow.
  - feed cooldown.
  - observe/hint.
  - agitated.
  - attack windup.
  - stunned/recover.
- Add perception facts:
  - nearest player.
  - favorite food held.
  - nearby comfort source.
  - nearby danger/source of damage.
  - home anchor.
  - structure encounter marker.
- Add budgets:
  - active brain ticks per server tick.
  - parked entities outside player simulation tickets.
  - spawn/despawn caps per region.
  - encounter caps per structure.
- Add multiplayer ownership rules:
  - one accepted feed per cooldown.
  - resource shed locks.
  - ~~damage invulnerability window already present, but expose diagnostics.~~ Melee attacks now share the server damage path and item-driven cooldowns; richer diagnostics remain open.
  - no client-side creature rewards.
- Add diagnostics:
  - active/parked ambient.
  - brain ticks.
  - blocked moves.
  - feed rejects.
  - encounter spawns.

### Akzeptanz

- Creatures feel alive without uncontrolled tick cost.
- Rare dangers are telegraphed and server-owned.
- Cozy creatures remain useful without becoming grind dispensers.

## P14.5 World Runtime And Long-Session Stability

### Ziel

The engine must support long exploration, return-to-base loops and save/load without memory growth or content drift.

### Aufgaben

- Chunk lifecycle:
  - split simulation, render, retain and save interests.
  - record why a chunk is kept loaded.
  - release GPU, collision and entity runtime data by budget.
- Worldgen runtime:
  - move Overworld passes behind services.
  - keep seed-stable tests for every extraction.
  - expose generation metrics to debug/profiling reports.
- Region persistence:
  - implement region reader/writer after layout contract.
  - migrate block diffs and block entity payloads.
  - corruption quarantine and backup.
- World events:
  - day/night state.
  - weather state.
  - one-shot structure/loot/discovery markers.
  - generated encounter state.
- Long-session smoke:
  - 20 minute explore.
  - return to base.
  - save/restart/rejoin.
  - verify storage, stations, comfort, discoveries, entities and chunks.

### Akzeptanz

- Memory and runtime resources stay bounded during long explore.
- Saved worlds do not lose base/station/progression facts.
- Debug output explains retained chunks and parked entities.

## P14.6 Content Validation And Build Gates

### Ziel

More content should make the game richer, not more fragile.

### Aufgaben

- Build an `adventuraContentReport` tool/task that checks:
  - item/block ids and aliases.
  - content tags.
  - recipe ingredients and outputs.
  - station unlocks.
  - loot table item keys.
  - structure marker keys.
  - biome progression references.
  - asset icon/texture availability.
  - save migration aliases.
- Add warning levels:
  - release blocker.
  - alpha blocker.
  - content warning.
  - cosmetic asset warning.
- Add balancing surfaces:
  - first-tool average resource count.
  - campfire fuel availability.
  - cooking pot route resources.
  - forge route resources.
  - rare loot odds.
  - comfort value caps.
- Add CI/release gates:
  - focused module tests.
  - content report.
  - physics regression.
  - protocol golden tests.
  - long-explore smoke checklist.
  - first-session smoke checklist.

### Akzeptanz

- Missing content dependencies are found before runtime.
- Balancing changes have visible numbers.
- Release notes can name measured risks instead of guesses.
