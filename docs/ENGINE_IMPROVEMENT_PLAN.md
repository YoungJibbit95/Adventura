# Adventura Engine Improvement Plan

Stand: 2026-04-29

Rolle: Engine, Rendering, Animation, Worldgen und Physics Engineering fuer Adventura.

## Zielbild

Adventura soll sich wie ein stabiles, cozy Voxel-Adventure anfuehlen: weich lesbare Welt, fluessige Bewegung, glaubwuerdige kleine Animationen, stabile Multiplayer-Grundlagen und eine Engine, die neue Bloecke, Biome, Kreaturen und Strukturen aufnehmen kann, ohne dass `GameClient` oder Shader jedes Mal groesser werden.

Leitlinien:

- Keine Komplett-Neuarchitektur. Bestehende Module und Patterns bleiben: `common`, `client`, `server`, Registries, Packets, `OverworldGenerator`, `ClientWorld`, `ServerWorld`.
- Determinismus bleibt Pflicht: gleicher Seed, gleiche Welt, reproduzierbare Tests.
- Gameplay-Regeln gehoeren nach `common`, Rendering bleibt im `client`, Server bleibt autoritativ.
- Jede groessere Engine-Aenderung bekommt vorher eine kleine Mess- oder Testbasis.
- Optimierungen werden an sichtbaren Problemen gemessen: Framezeit, Mesh-Zeit, Chunk-Ladezeit, Draw Calls, Triangles, VRAM, Stutter.

## Aktueller System-Schnitt

Rendering:

- `client/render/WorldRenderer.java` verwaltet Shader, Blockatlas, opaque/transparent GPU-Meshes, Render-Distance und Frustum-Culling.
- `client/render/ChunkMesher.java` baut sichtbare Faces, Cutout-Cross-Sprites, Vertex-Light, Vertex-AO und UVs.
- `client/render/assets/BlockTextureAtlas.java` baut Atlasdaten aus Einzeltexturen und Sprite-Sheet-Fallbacks.
- `chunk.vert` und `chunk.frag` machen Fog, AO, Soft-Shading, Bloom, Wasser-Wave und Materialfarben.

World und Streaming:

- `common/world/gen/OverworldGenerator.java` erzeugt Terrain, Biome, Hoehen, Fluesse, Hoehlen, Ores, Pflanzen und Strukturen.
- `client/world/ClientWorld.java` erzeugt Offline-Preview-Chunks, verwaltet Dirty-Meshes, Light, lokale Block-Updates und Ambient Entities.
- `server/world/ServerWorld.java` erzeugt Server-Chunks, validiert Block-Mutationen indirekt ueber `ServerConnectionHandler`, tickt Campfires und Storage.
- Chunks werden geladen/gestreamt, aber aktuell nicht wirklich entladen.

Physics:

- `client/Camera.java` enthaelt Maus/Input und delegiert Survival-Movement an `common/physics/PlayerPhysics`; Flying/Freecam-Reste liegen noch im Client.
- `ClientWorld.collidesPlayer(...)` macht AABB gegen collidable Blocks.
- `ServerConnectionHandler.handlePlayerMove(...)` validiert finite Werte, vertikale Bounds und grobe Delta-Grenzen; Kollision und Ground-State sind noch offen.

Animation:

- `client/BlockBreakAnimation.java` trackt Hold-Break-Progress und Handswing.
- `GameClient` rendert Held-Item-Bob, Pickup-Pop, Break-Overlay und UI-Feedback direkt im Main-Loop.
- `client/render/entity/EntityRenderer.java` rendert Box-Modelle mit einfachem Bobbing, aber ohne Interpolation, LOD, Culling oder Pose-System.

Tooling:

- `./gradlew test` ist aktuell lokal geblockt, weil `JAVA_HOME` fehlt und kein `java` im `PATH` ist. Das ist P0, sonst bleibt jeder Engine-Fix schwer beweisbar.

## P0 - Stabilisieren, Messen, Bug-Basis

1. Java/Test-Umgebung reparieren.
   - JDK 21 lokal verfuegbar machen.
   - `./gradlew test` und `./gradlew buildGame` als Standard-Gate verwenden.
   - Optional: README um klare `JAVA_HOME`-Hinweise fuer Linux/Windows/macOS ergaenzen.

2. Engine-Messpunkte festziehen.
   - Bestehendes Debug-Overlay nutzen: FPS, Frame ms, Render ms, Dirty Chunks, Built Chunks, Draw Calls, Triangles, VRAM, Entity Count.
   - Zusaetzlich messen: Chunkgen-Zeit, Meshing-Zeit, Light-Zeit, GPU-Upload-Zeit getrennt.
   - Ein kleiner `EngineFrameStats`-Record verhindert, dass Messlogik weiter im `GameClient` verstreut.

3. Reproduzierbare Testwelten definieren.
   - Seeds fuer Spawn, River, Forest, Cave, Village, Frost, Desert, Mushroom Grove.
   - Pro Seed kurze Smoke-Checkliste: Spawn frei, keine offensichtlichen Terrain-Seams, Wasser sichtbar, Entities sichtbar, Debugwerte plausibel.

4. Erste Bug-Tests anlegen.
   - `PlayerCollisionTest`: Boden, Wand, Head-Bump, hoher Fall, Chunk-Grenze.
   - `SpawnPositionTest`: Spawn-Fuesse stehen ueber Surface, nicht mehrere Blocks in der Luft.
   - `TransparentRenderOrderTest`: reine Sortierfunktion fuer transparente Chunks back-to-front.
   - `WorldgenFeaturePlacementTest`: keine chunk-gridartigen Baumverbotskanten als Dauerloesung.

## P1 - Rendering Engine

### Sofortige Render-Bugs

1. ~~Transparenz sortieren.~~
   - ~~Aktuell rendert `WorldRenderer` transparente Meshes in `HashMap`-Iteration mit `glDepthMask(false)`.~~
   - ~~Fix: transparente Chunks pro Frame nach Distanz zur Kamera back-to-front sortieren.~~
   - Danach optional Wasser separat als eigener Pass behandeln.

2. ~~Shader-Materialdaten enthaerten.~~
   - ~~`chunk.vert` hardcodet Wasseranimation auf Block-ID `4`.~~
   - ~~`chunk.frag` hardcodet Farben, Alpha und Emissive auf numerische IDs.~~
   - ~~Fix: `BlockRenderProperties` im Client einfuehren: alpha, emissive, animatedFluid, tint, materialFlags.~~
   - ~~Danach Shader nur noch ueber Materialdaten treiben, nicht ueber feste IDs.~~
   - Atlas-Sheet-Slices fuer volle Blockfaces trimmen/fuellen transparente Raender und UVs halbtexelig insetzen, damit Blocktexturen ohne sichtbare Zwischenraeume aneinander liegen.

3. UV-Uniform-Limit absichern.
   - ~~Kurzfristig: UV-Arrays und Material-Arrays auf den validierten Shader-Blockbereich reduzieren, statt ungenutzte 256er-Tabellen zu senden.~~
   - Das ist besser, aber noch kein Endzustand auf sehr konservativer GL-3.3-Hardware.
   - Ziel: UVs als Material-Metadata-Texture, UBO oder direkt als Vertex-UV-Rect/Materialindex packen.

4. ~~Render-Statistik korrigieren.~~
   - ~~`culledChunks` zaehlt aktuell opaque und transparent Layer separat.~~
   - ~~Klarere Stats: `culledMeshes`, `culledChunkPositions`, `renderedLayers`.~~

5. ~~Chunk-Render-Layer trennen.~~
   - ~~SOLID, CUTOUT und TRANSLUCENT werden als eigene Chunk-Meshes gebaut, hochgeladen und gerendert.~~
   - ~~Debug-Overlay zeigt Solid/Cutout/Water getrennt, damit Draw-Call- und Mesh-Regressionen sichtbar bleiben.~~

### Mesh-Performance

1. ~~Boxing im Mesher entfernen.~~
   - ~~`ChunkMesher` nutzt `List<Float>` und `List<Integer>`.~~
   - ~~Ersetzen durch primitive growable buffers (`FloatMeshBuffer`, `IntMeshBuffer`) oder NIO-Buffer-Builder.~~

2. Section-Aware Meshing.
   - Leere Sections werden schon uebersprungen, aber Meshes bleiben chunk-gross.
   - Ziel: sectionweise Bounds und optional sectionweise Meshes, damit Frustum-Culling vertikal sinnvoll wird.

3. Greedy Meshing fuer SOLID einfuehren.
   - ~~Erste Version fuer volle opaque Cubes im SOLID-Layer, wenn AO deaktiviert ist.~~
   - ~~CUTOUT und TRANSLUCENT bleiben im einfachen sichtbare-Face-Pfad.~~
   - Naechster Schritt: AO-kompatible Merge-Regeln, damit Flaechen nur bei identischem AO-/Light-Muster zusammengelegt werden.
   - Akzeptanz: deutlich weniger Triangles in Stein/Dirt-Flaechen, identische visuelle Kanten.

4. Mesh-Rebuild aus dem Renderpfad ziehen.
   - Aktuell passieren Preview-Generation, Light und Meshing synchron im Client-Loop.
   - Ziel: `ChunkBuildQueue` mit Prioritaet zur Kamera, Background-CPU-Build, GPU-Upload weiterhin auf Render-Thread.
   - Budget bleibt, aber als Zeitbudget in ms statt nur Chunks/frame.

5. Chunk-Unload und Mesh-Lifetime.
   - ClientWorld und WorldRenderer brauchen aktive Chunk-Sets.
   - Chunks ausserhalb Preview/Render-Puffer werden entladen, GPU-Meshes geschlossen, Entities entfernt oder geparkt.

### Lighting

1. Neighbor-Light sauber machen.
   - Blockupdates markieren Nachbarn schon dirty, aber Lighting ist grob chunk-zentriert.
   - Ziel: Light-Updates ueber Chunkgrenzen mit konsistenten sky/block light seams.

2. Incremental Block Light.
   - Aktuell scannt `LightEngine` betroffene Chunks komplett und cleared Blocklight.
   - Ziel: kleine Propagation-Updates fuer Light-Add/Remove, kompletter Rebuild nur als Fallback.

3. Sky Light verbessern.
   - Sky Light ist column-basiert und einfach.
   - Ziel: Tests fuer Hoehlen, Ueberhaenge, Chunkgrenzen, Wasser/Eis und transparente Bloecke.

## P1 - Physics Engine

### Architektur

1. `Camera` entkoppeln.
   - Input-Lesen bleibt clientseitig.
   - Simulation wandert in `common/physics/PlayerPhysics`.
   - `Camera` wird View/Controller, nicht Physics-Engine.
   - Survival-Step mit Gravity, Jump, Swim, Collision-Substeps und Fallimpact laeuft ueber `PlayerPhysics.stepSurvival`.
   - Kollidierende Flying-Bewegung laeuft ueber `PlayerPhysics.stepFlying`; Noclip/Freecam bleibt als Client-Controller-Rest.

2. ~~Gemeinsame Datenmodelle einfuehren.~~
   - ~~`PlayerInput`: moveX, moveZ, jump, crouch/flyDown, sprint, mode.~~
   - ~~`PlayerState`: position, velocity, onGround, underwater, fallDistance/fallSpeed.~~
   - ~~`PlayerPhysicsConfig`: gravity, jumpSpeed, speeds, drag, stepHeight, playerBounds.~~

3. Server-Validation vorbereiten.
   - Server prueft Max-Speed, Delta-Time, World-Bounds, Kollision und grobe Beschleunigung.
   - Client bekommt Authoritative Snapshots und kann spaeter interpolieren/reconciliaten.
   - Erster Schutz steht: finite Position/Rotation, vertikale World-Bounds, Initial-Sync-Radius und plausible Delta-Grenzen. Kollision, Ground-State und Reconciliation bleiben offen.

### Konkrete Physics-Bugs

1. ~~Discrete Collision tunnelt bei grossen Deltas.~~
   - ~~`deltaSeconds` wird auf 0.05 gecappt, Fallgeschwindigkeit auf -42, dadurch sind bis 2.1 Blocks pro Frame moeglich.~~
   - ~~Fix: swept AABB oder blockweise Substeps entlang jeder Achse.~~

2. ~~Missing Chunks sind aktuell Luft.~~
   - ~~`InMemoryWorld.blockId` gibt fuer ungeladene Chunks AIR zurueck.~~
   - ~~Online kann man dadurch in nicht gestreamte Welt laufen/fallen.~~
   - ~~Fix: Physics unterscheidet `unknown` von `air`; unbekannte Kollisionsbereiche blockieren oder pausieren Bewegung sanft.~~

3. ~~Spawn-Position pruefen.~~
   - ~~`ClientWorld.spawnPosition()` addiert aktuell Terrainhoehe + 3 + EyeHeight.~~
   - ~~Ziel: Fuesse auf `surface + 1`, Eye auf `surface + 1 + eyeHeight`, mit freiem Headroom.~~

4. ~~Block-Placement gegen Player-AABB.~~
   - ~~Platzieren in die eigene Hitbox verhindern.~~
   - ~~Server muss dieselbe Regel validieren.~~

5. ~~Wasserphysik definieren.~~
   - ~~`isUnderwater` prueft nur den Eye-Block.~~
   - ~~Ziel: getrennte Flags fuer feetInWater, bodyInWater, headUnderwater; daraus Swim, Breath und Drag ableiten.~~

### Physics-Tests

- ~~Grounded jump startet nur bei `onGround`.~~
- ~~Wandkollision stoppt X/Z, laesst Sliding entlang anderer Achse zu.~~
- ~~Head-Bump setzt vertikale Velocity auf 0.~~
- ~~Fall Impact entsteht nur beim Landen und nicht beim seitlichen Kontakt.~~
- ~~Wasser reduziert Gravity/Speed und erlaubt Auftauchen.~~
- ~~Server lehnt extreme Teleports und zu schnelle Moves ab.~~

## P1 - World Generation und Streaming

1. Generator in klare Paesse schneiden.
   - Pass 1: climate/biome maps.
   - Pass 2: heightmap und river/cave masks.
   - Pass 3: terrain fill.
   - Pass 4: ores.
   - Pass 5: decoration.
   - Pass 6: structures/entities/loot markers.

2. Heightmap und Biome-Cache pro Chunk.
   - Terrainhoehe wird sehr oft neu gesampelt, auch fuer Spawn, Structures und Entities.
   - Cache reduziert CPU und macht Tests einfacher.

3. Biome-Uebergaenge glatter machen.
   - Biome-spezifische Hoehenmodifikationen koennen harte Kanten erzeugen.
   - Ziel: blended terrain parameters statt abrupter if/else-Hoehen.

4. Decoration ohne Chunk-Rand-Artefakte.
   - Baeume werden aktuell vom Chunkrand ferngehalten, damit sie nicht ueber Grenzen schreiben.
   - Das kann gridartige Luecken erzeugen.
   - Ziel: deterministische Feature-Placer mit Nachbar-Chunk-Schreibplan oder deferred placements.

5. Structures robuster machen.
   - Multi-Chunk-Strukturen, Platzierungsregeln, Ground-Fit, Clearance, Wasser/Cave-Schutz.
   - Structure-Marker fuer Loot und Entity-Spawns nicht nur testen, sondern in Runtime-Persistence fuehren.

6. Spawn-Safety.
   - Spawn-Biom bevorzugen: Cozy Meadow/Flower Fields/Lakeside-Rand.
   - Garantieren: freier Kopf, Boden unter Fuessen, keine Wasser-/Cave-/Structure-Kollision.

7. Streaming und Unload.
   - Client: ringfoermiges Chunk-Set um Kamera, mit hysteresis.
   - Server: Chunks pro Verbindung nach Bedarf streamen, aber Entity/Block-Updates nur fuer relevante Zuschauer.
   - Langfristig: Persistenz fuer geaenderte Chunks und Storage.

## P1 - Animation Engine

1. Animationen aus `GameClient` ziehen.
   - Held-Item-Bob, Punch, Pickup-Pop und Break-Progress in kleine Animation-Komponenten.
   - `AnimationClock` und `Easing` zentralisieren.

2. Entity-Interpolation.
   - Snapshots puffern, bei Renderzeit interpolieren.
   - Online-Spieler und Kreaturen bewegen sich dadurch nicht im 10-Hz-Raster.

3. Pose-System fuer Box-Modelle.
   - `EntityRenderer` bekommt Modellteile mit Namen: body, head, legs, arms.
   - Pose-Funktionen: idle, walk, swim, jump/fall, interact.
   - Kreaturen bekommen kleine artspezifische Bewegungen ohne neue Renderer-Klasse pro Typ.

4. Break-Animation in die Welt bringen.
   - Das HUD-Crack-Overlay bleibt als Feedback, aber zusaetzlich Face-Overlay am Zielblock.
   - Akzeptanz: Spieler sieht exakt, welcher Block gerade bricht.

5. Render-Culling fuer Entities.
   - Frustum und Distanz-Culling vor `EntityRenderer.render`.
   - Debug-Hitbox muss dieselben Bounds nutzen wie Physics/EntityBounds.

## P2 - Multiplayer und Server-Simulation

1. Player-Movement authoritative machen.
   - Server simuliert oder validiert Bewegungen ueber gemeinsame `PlayerPhysics`.
   - Client sendet Input oder Movement mit Sequenznummern, Server antwortet mit Authoritative State.

2. Entity-Snapshots drosseln und partitionieren.
   - Aktuell werden bei Movement komplette Snapshot-Listen broadcastet.
   - Ziel: Interest-Management nach Chunk/Radius und feste Snapshot-Tickrate.

3. Async Chunk Generation auf Server.
   - Chunk-Erzeugung darf Netty-Handler nicht lange blockieren.
   - Generation in Worker, Packet-Auslieferung nach Fertigstellung.

4. Persistenz vorbereiten.
   - Geaenderte Chunks, Campfires, StorageCrates und Structure-Loot brauchen Save/Load.

## Bekannte Bug-/Risiko-Liste

P0:

- Tests koennen lokal nicht laufen, weil kein Java/JDK im PATH ist.
- ~~Server akzeptiert beliebige endliche PlayerMove-Positionen.~~
- ~~Client-Physics behandelt ungeladene Chunks als Luft.~~
- ~~Diskrete Collision kann bei hohen Deltas/fallen tunneln.~~

P1:

- ~~Transparente Meshes sind nicht back-to-front sortiert.~~
- Shader nutzt harte Block-IDs fuer Wasser, Alpha, Emissive und Farben.
- Blockatlas-UVs als grosse Uniform-Arrays koennen auf schwacher GL-3.3-Hardware problematisch sein.
- Keine Chunk-Unloads: World, GPU-Meshes und Ambient Entities wachsen beim Erkunden.
- Frustum-Culling nutzt volle Dimension-Hoehe pro Chunk, nicht echte Mesh-Bounds.
- ~~Spawn-Position ist wahrscheinlich hoeher als noetig.~~
- Entity-Rendering rendert alle sichtbaren Snapshots ohne Interpolation/Culling.
- Worldgen-Dekoration vermeidet Chunkraender und kann sichtbare Raster-Luecken erzeugen.
- Lighting-Rebuilds sind teuer und Chunkgrenzen brauchen bessere Tests.

P2:

- `GameClient` ist mit Input, Gameplay, UI, Rendering-Feedback und State noch zu gross.
- Structures und Worldgen-Regeln sind noch stark hardcoded.
- Material-/Block-Renderdaten sind zwischen `Blocks`, Atlas und Shader verstreut.
- Multiplayer-Interest-Management fehlt.

## Reihenfolge fuer die naechsten Arbeitsbloecke

Sprint 1: Tooling und kleine Engine-Bugs

- JDK 21 verfuegbar machen, Tests gruen bekommen.
- ~~Spawn-Position testen und korrigieren.~~
- ~~Transparenz-Sortierfunktion extrahieren, testen und in `WorldRenderer` nutzen.~~
- ~~Render-Stats klarer benennen.~~

Sprint 2: Physics-Core

- ~~`common/physics` mit `PlayerState`, `PlayerInput`, `PlayerPhysicsConfig`, `PlayerPhysics`.~~
- ~~Camera-Survival-Simulation auf `PlayerPhysics.stepSurvival` umstellen.~~
- ~~Camera-Flying-Simulation auf `PlayerPhysics.stepFlying` umstellen.~~
- Freecam/Noclip-Controller-Rest weiter entkoppeln.
- ~~Collision-Substeps oder swept AABB.~~
- ~~Placement gegen Player-AABB client- und serverseitig.~~
- ~~Erste Server-Move-Validation fuer finite/Y/Delta-Grenzen.~~

Sprint 3: Render-Materialien und Mesher

- ~~`BlockRenderProperties` einfuehren.~~
- ~~Shader-ID-Hardcoding reduzieren.~~
- ~~Primitive Mesh-Buffer statt boxed Lists.~~
- ~~Chunk meshes in SOLID/CUTOUT/TRANSLUCENT splitten.~~
- ~~Erste Greedy-Meshing-Version fuer opaque Cubes.~~
- Greedy Meshing mit AO-kompatiblen Merge-Regeln weiter ausbauen.

Sprint 4: Streaming und Chunk-Lifetime

- Client active chunk ring mit unload hysteresis.
- WorldRenderer entfernt Meshes fuer entladene Chunks.
- Ambient Entity unload/park policy.
- Chunkgen/Meshing-Zeitbudgets messen.

Sprint 5: Worldgen-Qualitaet

- Chunk-Heightmap/Biome-Cache.
- Spawn-Safety.
- Decoration-Placer ohne Randraster.
- River/biome blending verbessern.

Sprint 6: Animation und Multiplayer-Polish

- Entity snapshot interpolation.
- Pose-System fuer Box-Modelle.
- Block-break face overlay.
- Server movement validation und snapshot interest management.

## Definition of Done fuer Engine-Aenderungen

- Tests laufen lokal mit `./gradlew test`.
- Neue Engine-Regel hat mindestens einen Unit-Test oder einen dokumentierten Smoke-Test.
- Debug-Overlay zeigt keine offensichtliche Regression bei Framezeit, Draw Calls, Triangles oder VRAM.
- Singleplayer und Join-Local bleiben startbar.
- Multiplayer-relevante Gameplay-Aktionen bleiben servervalidiert.
- Kein neuer harter Block-ID-Sonderfall im Shader, wenn er als Block- oder Materialmetadatum modelliert werden kann.
