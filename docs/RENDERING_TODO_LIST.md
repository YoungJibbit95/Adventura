# Adventura – Rendering TODO List

Stand: 2026-04-30

Letzter Rendering-Pass: P0/P1/P2/P3 wurden am 2026-04-30 im Code abgearbeitet. Kernpunkte:

- `RenderMaterial` + `TerrainMaterialLut` ersetzen die alten großen Shader-Uniform-Arrays für Terrainmaterialien.
- `chunk.vert`/`chunk.frag` lesen Materialdaten über `materialIndex` aus der LUT.
- `WorldRenderer` rendert Terrain in klaren Pässen: `terrain.opaque`, `terrain.cutout`, `terrain.translucent`.
- Debug-HUD zeigt Materialanzahl, LUT-Größe, fehlende Materialdaten, Vertexgröße und Dreiecke pro Layer.
- `/debugmaterial` gibt für den anvisierten Block Material-ID, Layer, Alpha, Glow, Fluid-Flag und Flags aus.
- `ChunkMesher` nutzt wiederverwendete primitive Mesh-Buffer, misst Buffer-Wachstum/Retained-Buffer und kann Greedy Meshing per `/greedymesh` toggeln.
- Greedy Meshing berücksichtigt Material, Light und AO pro Merge-Zelle; Cutout und Wasser bleiben bewusst im Simple-Mesh-Pfad.
- Chunk-Meshes tragen eigene Bounds pro Layer; Frustum-Culling unterscheidet Distance- und Bounds-Culls, `/debugbounds` zeichnet Mesh-Bounds.
- CPU-Mesh-Budget, GPU-Upload-Budget, sichtbare Chunk-Priorität, Preview-Nachziehen, Spieleraktions-Priorität und adaptive Budget-Senkung sind gekoppelt.
- `BlockTextureAtlas` validiert Missing Textures, Duplicate-Mappings, Atlasgröße, Materialanzahl und UV-Rects; `/debugatlas` zeigt eine kompakte Report-Zeile.
- Atlas-Tiles haben extrudiertes Pixel-Art-Padding, UV-Inset ist dokumentiert, Filter ist bewusst `nearest-no-mip`, und ein optionaler PNG-Debug-Export existiert.

Verifikation 2026-04-30: Mit `JAVA_HOME=/home/youngjibbit/.local/share/adventura-jdk/jdk-21.0.11+10` liefen die fokussierten Client-Tests erfolgreich: `./gradlew :client:test --no-daemon --max-workers=1 --rerun-tasks --tests 'dev.voxelgame.client.render.*' --tests 'dev.voxelgame.client.render.assets.*' --tests 'dev.voxelgame.client.GameSettingsTest' --tests 'dev.voxelgame.client.world.ClientWorldMeshInvalidationTest'`. Der vollständige Check `./gradlew buildGame --no-daemon --max-workers=1` ist ebenfalls grün.

## Ziel

Diese Liste ist bewusst detaillierter als die anderen Listen, weil der Rendering-Kern früh sauber aufgebaut werden sollte. Ein guter Rendering-Kern ermöglicht später schöneres Gameplay: bessere Biome, klarere Items, lebendige Entities, stimmungsvolle Höhlen, cozy Basebuilding, gutes Wasser, Partikel, Glow, Nacht, Ruinen und Performance auf schwächeren PCs.

Adventura soll visuell in Richtung **cozy voxel adventure** gehen:

- warme Farben
- klare Blockformen
- pixel-art-kompatible Texturen
- weiche Fog-/Day-Night-Stimmung
- lesbares Terrain
- schöne, aber günstige Beleuchtung
- einfache, stabile Shader
- gute Debug-Möglichkeiten
- keine harte Abhängigkeit von Block-IDs in Shadern

## Bestehende Basis

Vorhanden oder begonnen:

- `WorldRenderer` verwaltet Chunk Rendering, Shader, Atlas, Render Distance und Culling.
- `ChunkMesher` baut sichtbare Faces, Cutout-Sprites, AO, Vertex-Light und UVs.
- `BlockTextureAtlas` baut Atlasdaten aus Einzeltexturen und Sprite-Sheet-Fallbacks.
- Chunk Meshes sind in SOLID, CUTOUT und TRANSLUCENT aufgeteilt.
- Transparente Chunks können sortiert werden.
- Shader-Materialdaten wurden teilweise entkoppelt.
- UV-Inset/Fix gegen Block-Face-Seams existiert.
- Bloom/Glow Toggle ist begonnen.
- Entity Renderer und Particle System existieren in erster Form.

Jetzt soll daraus eine bewusst strukturierte Rendering-Basis entstehen.

---

# P0 – Rendering-Grundregeln

## P0.1 Keine harten Block-ID-Sonderfälle

### Ziel

Shader dürfen nicht wissen, dass bestimmte Block-IDs Wasser, Glow, Alpha oder Spezialfarben sind. Alle Render-Informationen kommen aus Daten/Registries.

### Offen

- ~~harte Block-ID-Abfragen im Shader entfernen oder endgültig absichern.~~
- ~~`BlockRenderProperties` vollständig nutzen.~~
- ~~Materialdaten zentralisieren:~~
  - ~~alpha~~
  - ~~emissive~~
  - ~~animatedFluid~~
  - ~~cutoutThreshold~~
  - ~~biomeTintMode~~
  - ~~fogAffectMode~~
  - ~~faceGapFix~~
  - ~~roughness/visualStyle optional~~
- ~~Tests/Validation für fehlende Materialdaten.~~
- ~~Debug-Ansicht für Material-ID/Render-Layer.~~

### Akzeptanz

- neuer Glow-Block braucht keine Shader-Änderung.
- neuer Wasser-/Fluid-Block braucht keine Shader-ID-Liste.
- neuer Cutout-Block braucht nur Registry-/Materialdaten.

---

## P0.2 Render-Pipeline klar definieren

### Ziel

Rendering soll in klaren Pässen laufen, damit neue Features später nicht direkt im `WorldRenderer` explodieren.

### Empfohlene Frame-Reihenfolge

1. Frame Stats beginnen.
2. Client interpolation / snapshot smoothing.
3. Camera update.
4. Frustum berechnen.
5. sichtbare Chunks und Entities sammeln.
6. Chunk Build Queue verarbeiten.
7. GPU Upload Budget verarbeiten.
8. Sky/Fog vorbereiten.
9. Opaque Terrain Pass.
10. Cutout Vegetation/Prop Pass.
11. Entity Pass.
12. Transparent/Water Pass.
13. Particle Pass.
14. Selection Outline / Block Break Overlay.
15. HUD.
16. UI Screens.
17. Debug Overlay.
18. Frame Stats abschließen.

### Offen

- ~~kleine `RenderPass`-Struktur einführen, ohne komplette RenderGraph-Neuarchitektur.~~
- ~~`RenderContext` für Kamera, Projection, View, RenderSettings, Time, Fog, Stats.~~
- ~~`WorldRenderer` entlasten:~~
  - ~~Terrain pass~~
  - ~~Entity pass~~
  - ~~Particle pass~~
  - ~~UI/Debug getrennt halten~~
- ~~OpenGL-State pro Pass klar setzen und wiederherstellen.~~

### Akzeptanz

- neue Render-Features landen nicht ungeordnet im Main-Loop.
- jeder Pass hat klare Inputs und Outputs.
- Debug-Stats können pro Pass gemessen werden.

---

# P1 – Material- und Shader-Datenmodell

## P1.1 Material LUT statt großer Uniform-Arrays

### Problem

Große UV-/Material-Uniform-Arrays können auf konservativer GL-3.3-Hardware problematisch werden und sind schwer skalierbar.

### Ziel

Materialdaten werden GPU-freundlich als Texture Buffer, 1D Texture oder kompakte UBO bereitgestellt.

### Optionen

#### Option A: 1D Material LUT Texture

Gut für GL 3.3 und viele Daten:

- pro Material ein paar Texel
- UV Rect
- flags
- alpha
- emissive
- animation params
- tint mode

#### Option B: UBO

Gut, wenn Datenmenge klein bleibt. Risiko: UBO-Limits.

#### Option C: Vertex enthält UV Rect Daten

Einfach, aber erhöht Vertex-Größe.

### Empfehlung

Für Adventura: zuerst **1D Material LUT Texture** oder kleine **Material Metadata Texture**, weil das langfristig am besten skaliert.

### Offen

- ~~Materialdatenformat definieren.~~
- ~~CPU-Seite: `RenderMaterial` Record.~~
- ~~GPU-Seite: Material LUT upload.~~
- ~~Shader liest Material via `materialIndex`.~~
- ~~Fallback für fehlende Materialdaten.~~
- ~~Debug-Ausgabe: Material count, LUT size, missing materials.~~

### Akzeptanz

- ~~mehr als 256 Block-/Materialeinträge sind langfristig möglich.~~
- ~~Shader nutzt Materialindex statt Block-ID.~~
- ~~Atlas-UVs und Renderflags kommen aus Materialdaten.~~

### Umsetzung

- `RenderMaterial` definiert CPU-Materialdaten inkl. Layer, Flags, Alpha, Emissive, Cutout-Threshold, Style-Metadaten und Atlas-UVs.
- `TerrainMaterialLut` lädt die Materialdaten als GPU-Metadata-Texture; Shader lesen per `materialIndex`.
- Debug-HUD zeigt Material Count, LUT-Größe, fehlende Materialdaten und Atlas-Count.

---

## P1.2 Einheitlicher Vertex-Format-Plan

### Ziel

Chunk-, Entity- und Particle-Renderer sollen klare, dokumentierte Vertex-Formate haben.

### Chunk Vertex Attribute

Empfohlen:

- position local/world packed
- normal oder face index
- local UV
- material index
- block light
- sky light
- ambient occlusion
- biome tint index oder biome blend weight
- flags optional

### Offen

- ~~aktuelles Vertexformat dokumentieren.~~
- ~~prüfen, welche Werte wirklich pro Vertex nötig sind.~~
- ~~prüfen, was pro Face oder pro Material gespeichert werden kann.~~
- ~~Vertexgröße im Debug anzeigen.~~
- ~~Mesh memory estimate verbessern.~~

### Akzeptanz

- ~~Vertexformat ist dokumentiert.~~
- ~~Änderungen am Shader brechen Mesher nicht still.~~
- ~~Debug Overlay zeigt Mesh-Speicher grob korrekt.~~

### Umsetzung

- `ChunkMesher` besitzt feste Offsets und `VERTEX_BYTES`; `GpuChunkMesh` nutzt diese Konstanten direkt.
- Debug-HUD zeigt Vertexgröße und VRAM-Schätzung.

---

## P1.3 Render-Layer sauber halten

### Layer

- `SOLID`: volle opaque Cubes und opaque Models.
- `CUTOUT`: Pflanzen, Zäune, kleine Props, Alpha-Test.
- `TRANSLUCENT`: Wasser, Glass, halbtransparente Effekte.
- optional später `EMISSIVE_OVERLAY` oder `DECAL`.

### Offen

- ~~jedes Blockmaterial validiert Layer-Kompatibilität.~~
- ~~Cutout nutzt Alpha-Test, nicht normales Alpha-Blending.~~
- ~~Translucent wird sortiert.~~
- ~~Wasser kann als eigener Sub-Pass behandelt werden.~~
- ~~Debug-HUD zeigt Meshes/Triangles pro Layer.~~

### Akzeptanz

- ~~Vegetation sortiert nicht falsch.~~
- ~~Wasser rendert stabil.~~
- ~~Solid Draw Calls bleiben gut batchbar.~~
- ~~Layer-Fehler fallen in Validation auf.~~

### Umsetzung

- Terrain läuft in getrennten `terrain.opaque`, `terrain.cutout` und `terrain.translucent` Passes.
- Cutout bleibt Alpha-Test im Shader ohne Blend-Pass, Translucent wird back-to-front sortiert.
- Debug-HUD zeigt Meshes und Triangles je Layer.

---

# P2 – Chunk Meshing und Performance

## P2.1 Mesh Builder endgültig primitive halten

### Basis

Boxing im Mesher wurde bereits reduziert/begonnen.

### Offen

- ~~sicherstellen, dass keine `List<Float>`/`List<Integer>` im heißen Meshing-Pfad übrig sind.~~
- ~~primitive growable buffers verwenden.~~
- ~~Buffer wiederverwenden, wo möglich.~~
- ~~MeshBuilder reset statt neu allokieren.~~
- ~~Allokationen pro Chunk Build messen.~~

### Akzeptanz

- ~~Chunk Meshing erzeugt weniger GC-Spikes.~~
- ~~Mesh Build Time ist im Debug sichtbar.~~

### Umsetzung

- `ChunkMesher` hält `FloatMeshBuffer` und `IntMeshBuffer` als wiederverwendete Instanzbuffer.
- Jeder Build ruft nur `reset()` auf und kopiert am Ende in kompakte Output-Arrays für `ChunkMesh`.
- `ChunkMesher.MeshBuildStats` meldet Output-Bytes, temporäres Buffer-Wachstum, Retained-Buffer-Bytes und Greedy-Status.
- `ChunkBuildQueue`, `EngineFrameStats` und Debug-HUD zeigen Meshing-Zeit, Mesh-Buffer-Wachstum und Retained-Buffer.

---

## P2.2 Greedy Meshing ausbauen

### Basis

Greedy Meshing für opaque Cubes ohne AO ist begonnen/geplant.

### Offen

- ~~AO-kompatible Merge-Regeln definieren.~~
- ~~Light-kompatible Merge-Regeln definieren.~~
- ~~nur Faces mit identischem Material, Light, AO und Tint mergen.~~
- ~~Cutout/Water nicht greedy mergen, bis Regeln klar sind.~~
- ~~Debug Toggle: simple mesh vs greedy mesh.~~
- ~~Tests für identische visuelle Kanten.~~

### Akzeptanz

- ~~Dirt/Stone-Flächen haben deutlich weniger Triangles.~~
- ~~keine kaputten AO-Kanten.~~
- ~~keine Textur-Stretches bei Pixel-Art-Blocks.~~
- ~~Toggle erlaubt Vergleich.~~

### Umsetzung

- `GreedyCell` enthält Block-/Material-ID, quantisiertes Light und vier AO-Werte.
- Merges passieren nur bei identischer `GreedyCell`; AO-Kanten werden dadurch nicht über unterschiedliche Licht-/AO-Situationen hinweg verschmolzen.
- Tiled Face-UVs bleiben > 1.0 und werden im Shader per `fract()` wiederholt, damit Pixel-Art-Texturen nicht gestreckt werden.
- `CUTOUT` und `TRANSLUCENT` laufen weiter über Simple Faces/Cross Sprites.
- `/greedymesh` toggelt Greedy Meshing und markiert geladene Chunks neu dirty.

---

## P2.3 Section-Aware Mesh Bounds

### Ziel

Frustum Culling soll nicht immer ganze Welt-/Chunk-Höhe behandeln.

### Offen

- ~~Mesh Bounds pro Layer berechnen.~~
- ~~optional Bounds pro Section berechnen.~~
- ~~Frustum testet realistische Mesh Bounds.~~
- ~~Debug Toggle für Bounds anzeigen.~~
- ~~Metrics: culled by chunk und rendered layers.~~
Hinweis 2026-05-01: Section-Bounds/Culling sind headless abgesichert (`sectionBoundsAroundUsesRadiusAndOnlyNonEmptySections`). Echte `culled by section`-Draw-Range-Metriken bleiben ein späteres Renderer-Upgrade: Dafür muss der Mesher SOLID/CUTOUT/TRANSLUCENT pro vertikaler Section oder Draw-Range ausgeben, z. B. über `SectionMeshPart` mit `sectionY`, `indexOffset`, `indexCount`, `Bounds` oder mehrere `ChunkMesh`-Objekte pro Section.

### Akzeptanz

- ~~Höhlen/Berge verursachen weniger unnötige Draw Calls.~~
- ~~Bounds sind sichtbar korrekt.~~
- ~~keine Meshes werden falsch gecullt.~~

### Umsetzung

- `ChunkMesh` berechnet Bounds aus Vertexpositionen; `GpuChunkMesh` hält diese Bounds für den Renderpfad.
- `WorldRenderer` cullt zuerst per Render Distance und danach per Mesh-AABB gegen den Frustum.
- `RenderPassStats` und `EngineFrameStats.Rendering` unterscheiden `culledByDistance` und `culledByBounds`.
- `ClientWorld` liefert Section-/Vertical-Bounds-Stats; Debug-HUD zeigt Sections, Bounds, Cull-Distance und Cull-Bounds.
- `/debugbounds` zeichnet die tatsächlichen Mesh-Bounds per Debug-Line-Renderer.

---

## P2.4 ChunkBuildQueue mit Render-Budget koppeln

### Ziel

Meshing und Upload sollen nicht gegen FPS kämpfen.

### Offen

- ~~CPU-Build Budget in ms.~~
- ~~GPU Upload Budget in ms.~~
- ~~sichtbare Chunks zuerst.~~
- ~~entfernte Preview-Chunks langsam nachziehen.~~
- ~~Rebuilds priorisieren, die durch Spieleraktion entstanden sind.~~
- ~~bei starker Last Budget adaptiv senken.~~

### Akzeptanz

- ~~weniger Stutter beim Erkunden.~~
- ~~Blockabbau/-platzierung aktualisiert Mesh schnell.~~
- ~~entfernte Chunks bauen ohne Frame-Spikes nach.~~

### Umsetzung

- `GameSettings` trennt Chunk-Count-Budget, CPU-Mesh-ms-Budget und GPU-Upload-ms-Budget.
- Render-Presets setzen beide ms-Budgets; `/meshms n` und `/uploadms n` steuern sie zur Laufzeit.
- `WorldRenderer` verarbeitet CPU-Mesh-Builds und GPU-Uploads getrennt, mit eigener Pending-GPU-Upload-Queue.
- `ChunkBuildQueue` priorisiert urgent Spieleraktionen vor normalen/Preview-Rebuilds und sortiert danach nach Sicht-/Preview-Band.
- `GameSettings.effectiveMeshBuildBudgetMilliseconds()` und `effectiveGpuUploadBudgetMilliseconds()` senken Budgets adaptiv, wenn der vorherige Frame teuer war.

---

# P3 – Texture Atlas und Asset-Pipeline

## P3.1 Atlas Validation

### Offen

- ~~alle Block-Texture-Keys validieren.~~
- ~~fehlende Texturen melden.~~
- ~~duplicate mappings melden.~~
- ~~Atlas-Größe melden.~~
- ~~UV Rects als Debug-Liste exportieren.~~
- ~~Material count melden.~~
- ~~optional PNG-Debug-Ausgabe des generierten Atlas.~~

### Akzeptanz

- ~~fehlende Textur fällt beim Start/Build auf.~~
- ~~Atlas-Probleme sind nicht erst im Spiel sichtbar.~~

### Umsetzung

- `BlockTextureAtlas.AtlasValidationReport` meldet Atlasgröße, Tilegröße, Padding, Filtermode, Texture Count, Material Count, Missing Textures, Duplicate Mappings und UV-Debug-Zeilen.
- `WorldRenderer` bricht beim Start mit klarer Fehlermeldung ab, wenn Texture-Mappings fehlen.
- `/debugatlas` zeigt eine kompakte Atlas-Report-Zeile im Chat.
- `BlockTextureAtlas.writeDebugAtlas(...)` kann den generierten Atlas optional als PNG schreiben.

---

## P3.2 Pixel-Art-sichere Atlas-Regeln

### Ziel

Block-Faces dürfen keine sichtbaren Nähte, Bleeding oder transparente Ränder erzeugen.

### Offen

- ~~Texture padding/extrusion pro Tile.~~
- ~~UV inset pro Tile dokumentieren.~~
- ~~Mipmapping für Pixel-Art bewusst entscheiden:~~
  - ~~nearest ohne mip für crisp pixel~~
  - ~~oder mip mit extruded padding gegen shimmering~~
- ~~Atlas-Filter global kontrollieren.~~
- ~~Sprite-Sheets und Einzel-PNGs gleich behandeln.~~
- ~~Debug Test für seam-heavy Blocks.~~

### Akzeptanz

- ~~keine hellen/dunklen Nähte zwischen Blockfaces.~~
- ~~Wasser/Glass erzeugt keine falschen Ränder.~~
- ~~Pixel-Art bleibt scharf.~~

### Umsetzung

- `TILE_PADDING_PIXELS = 1` extrudiert die äußeren Tile-Pixel in den Atlas-Rand.
- `UV_INSET_PIXELS = 0.5f` hält UVs bewusst innerhalb der Content-Fläche.
- `ATLAS_FILTER_MODE = nearest-no-mip`; GL nutzt `GL_NEAREST` für Min/Mag.
- Sheet-Slices und Einzel-PNGs gehen durch dieselbe `buildAtlas`-/`paddedTile`-Pipeline.
- Tests prüfen Padding-Extrusion, Missing-Texture-Validation und Atlas-Reportdaten.

---

## P3.3 Material Metadata für Assets

### Optionales Metadata-Modell

Pro Textur/Material:

- `renderLayer`
- `alpha`
- `cutoutThreshold`
- `emissive`
- `animated`
- `animationFrames`
- `biomeTint`
- `normalMode` optional
- `isFluid`
- `lightValue`

### Offen

- ~~Metadata-Datei oder Registry-Mapping definieren.~~
- ~~Fallbacks für fehlende Metadata.~~
- ~~Validation gegen Blocks Registry.~~

### Akzeptanz

- ~~neue Assets können Eigenschaften bekommen, ohne Shader anzupassen.~~
- ~~Light/Glow/Water/Vegetation skalieren besser.~~

### Umsetzung

- `BlockRenderProperties` ist aktuell das Registry-Mapping für Material-Metadata.
- `RenderMaterial.tableFor(...)` erzeugt aus Block-Registry, Render Properties und Atlas-UVs die Materialtabelle.
- Fehlende explizite Properties werden über Fallbacks markiert und im Material Count/Missing Count sichtbar.
- `BlockRenderProperties.validateRegisteredBlocks(...)` validiert Layer/Alpha/Fluid-Kompatibilität gegen die Blocks Registry.

---

# P4 – Shader Plan

## P4.1 Terrain Shader

### Muss können

- Atlas sampling
- material index lookup
- sky light
- block light
- vertex AO
- per-face brightness
- fog
- day/night color
- emissive add
- optional biome tint

### Offen

- ~~Shader Uniforms dokumentieren.~~ Siehe `docs/RENDERING_SHADER_UNIFORMS.md`.
- ~~Material LUT integrieren.~~ Terrain liest Farbe, Effekte, Atlas-UVs, Style, Fog-Mode und Render-Layer aus `uMaterialLut`.
- ~~Lightwerte sauber normalisieren.~~ Vertex-Light wird im Shader geklemmt und mit Tageszeit-Helligkeit gemischt.
- ~~Fog nicht pro Block-ID, sondern global/materialbasiert.~~ `FogAffectMode` aus der LUT reduziert oder deaktiviert Fog pro Materialklasse.
- ~~Debug-Modi:~~
  - ~~show material index~~
  - ~~show light~~
  - ~~show AO~~
  - ~~show biome tint~~
  - ~~show render layer~~

### Umsetzung

- `/debugview off|material|light|ao|biome|layer` schaltet die Terrain-Debugausgabe.
- `RenderDebugView` wird ueber `RenderSettings` an den Chunk-Shader uebergeben.
- Biome-Tint, Tageszeit-Fog, Underwater-Tint und Bloom laufen ohne Block-ID-Hacks ueber Runtime-Settings plus Material-LUT.

### Akzeptanz

- Terrain sieht bei Tag/Nacht lesbar aus.
- Debug-Modi helfen beim Fehlerfinden.
- neue Materialien brauchen keinen Shader-Hack.

---

## P4.2 Cutout Shader

### Ziel

Vegetation, kleine Props und Zäune sollen scharf und günstig rendern.

### Offen

- ~~Alpha-Test statt sortiertem Alpha-Blending.~~ Cutout-Materialien verwerfen Alpha unter Threshold im Terrain-Shader.
- ~~Cutout Threshold aus Materialdaten.~~ `ROW_EFFECTS.w` ist der Material-Cutout-Threshold.
- ~~leichte Windbewegung optional und materialgesteuert.~~ Cutout-Materialien mit Biome-Tint bekommen shaderseitig sanften Wind.
- ~~biome tint für Gras/Leaves optional.~~ Gras, Leaves, Reeds, Bushes und verwandte Vegetation nutzen `BiomeTintMode`.
- ~~distance fade optional, aber nicht gegen Lesbarkeit.~~ Bewusst nicht als Extra-Fade umgesetzt; Fog uebernimmt Distanzkaschierung ohne Vegetation auszufransen.

### Akzeptanz

- Pflanzen sortieren nicht falsch.
- Cutout sieht scharf aus.
- Wind/Bewegung kann später ohne neue Renderer ergänzt werden.

---

## P4.3 Water Shader

### Ziel

Wasser soll cozy und klar lesbar sein, ohne teure Refraction/Reflection-Pipeline.

### Offen

- ~~eigener Water/Translucent Pass.~~ Wasser laeuft im `terrain.translucent` Pass.
- ~~Wasserflächen back-to-front sortieren.~~ Translucent Meshes werden bounds-basiert von hinten nach vorne sortiert.
- ~~Depth test an, depth write aus.~~ Translucent Pass rendert mit aktivem Depth Test und deaktiviertem Depth Write.
- ~~animated UV/wave offset materialgesteuert.~~ `animatedFluid` aus der LUT steuert Vertex-Welle und UV-Offset.
- ~~underwater tint/overlay.~~ `uUnderwater` verkuerzt Fog und tintet Terrain/Entities.
- ~~Wasserfarbe nach Biome optional.~~ Wasser/ICE nutzen `BiomeTintMode.WATER`; Runtime-Biome-Tint kommt aus der Kamera-Region.
- ~~Low-End Toggle für simple water.~~ `/simplewater` und Low-Preset reduzieren Wasserwellen/UV-Bewegung.
Optionaler Engine-Hinweis: Edge foam braucht spaeter pro Wasserface eine Shore-Maske oder wenigstens einen `edgeFoam`-Vertexkanal. Der Mesher sollte bei Wasserfaces Nachbarn in X/Z pruefen und `foam = 1.0` setzen, wenn neben dem Face ein Nicht-Wasser-Block oder ein niedrigeres Wasserlevel liegt. Der Vertex-Buffer braucht dann ein zusaetzliches Attribut oder die Info muss in ungenutzte Material-/AO-Daten gepackt werden.

### Akzeptanz

- Wasser sieht nicht wie harte blaue Glasplatte aus.
- Wasser verdeckt Terrain nicht komplett.
- Unterwasserzustand ist visuell klar.
- Wasser verursacht keine großen Draw-/Sortierprobleme.

---

## P4.4 Entity Shader

### Ziel

Entities sollen visuell konsistent zur Blockwelt wirken und später mehr Animationen tragen.

### Offen

- ~~Entity Materialdaten definieren.~~ Entity-Part-Farben und Emissive-Flags aus `EntityModelRegistry`/Parts werden an den Shader gegeben.
- ~~einfache diffuse Beleuchtung.~~ Entity-Shader nutzt gewrapptes Lambert-Licht.
- ~~optional block/sky light sample an Entity Position.~~ `EntityRenderer` sampled Sky-/Block-Light an der Entity-Mitte.
- ~~distance fade optional.~~ Entity-Fog nutzt dieselben Fog-Settings wie Terrain.
- ~~debug bounds kompatibel mit Physics.~~ Bestehende Entity-Hitbox/Bounds-Debugausgabe bleibt unabhaengig vom Entity-Shader.
Optionaler Engine-Hinweis: Hit flash braucht spaeter ein zeitlich begrenztes Hit-/Damage-Signal im `EntitySnapshot`, z. B. `lastHurtTimeSeconds` oder `hurtFlashTicks`. Der Server/Client-Entity-State muss dieses Feld setzen, der Snapshot muss es serialisieren, und `EntityRenderer` kann daraus `uHitFlash = max(0, 1 - age / duration)` pro Entity setzen.

### Akzeptanz

- Tiere wirken nicht vom Terrain getrennt.
- Entity-Lighting passt grob zu Tag/Nacht.
- Animation/Pose-System kann Shader weiter nutzen.

---

## P4.5 Particle Shader

### Ziel

Partikel sollen cozy Effekte tragen, aber budgetierbar bleiben.

### Offen

- ~~alpha blending.~~ Partikel rendern mit `SRC_ALPHA / ONE_MINUS_SRC_ALPHA`.
- ~~depth test an.~~ Der Particle-Renderer aktiviert Depth Test vor dem Draw.
- ~~depth write aus.~~ Particle-Draws laufen mit `glDepthMask(false)`.
- ~~soft fade optional.~~ Lifetime-Alpha fadet Partikel weich aus.
- ~~additive glow optional für Fireflies/Glow Spores.~~ Helle Partikelfarben bekommen einen subtilen Shader-Glow-Boost ohne separaten Blend-Pass.
- ~~Particle Quality Settings.~~ Presets und `/particles 0.25-1.0` begrenzen die Live-Partikelkapazitaet.
Optionaler Engine-Hinweis: Particle-Billboards mit Atlas brauchen spaeter `ParticleSpriteAtlas` analog zum Block-Atlas: JSON oder Registry mit Sprite-Key, Tile-Rect, Pivot und optional Blend-Modus. `Particle` braucht `spriteId`/`uvRect`, der VBO muss `aUv` und ggf. `aSpriteFlags` tragen, und `particle.frag` muss `uParticleAtlas` sampeln.

### Akzeptanz

- Campfire/Fireflies/Harvest wirken lebendig.
- Low-End Preset reduziert Partikel sichtbar.
- keine massiven Overdraw-Probleme.

---

## P4.6 Sky/Fog/Post

### Offen

- ~~Sky gradient mit Tageszeit.~~ Clear-/Fog-Farbe folgt dem lokalen Tageszyklus.
- ~~Horizon fog.~~ Render-Distance-Fog kaschiert den Horizont mit Tageszeitfarbe.
- ~~Fog-Farbe nach Tageszeit.~~ Morgen, Mittag, Abend und Nacht haben eigene Farbverlaeufe.
- ~~Biome-Fog optional.~~ Kamera-Biome tintet Sky/Fog leicht.
- ~~Bloom/Glow nur auf High/Medium oder Toggle.~~ Presets schalten Bloom; `/bloom` bleibt Runtime-Toggle.
- ~~Color grading subtil.~~ Terrain-Shader nutzt leichte Gamma-/Saturation-Korrektur.
Optionaler Engine-Hinweis: Sun/Moon Disc und Stars brauchen spaeter einen eigenen Sky-Pass vor Terrain, z. B. `SkyRenderer` mit fullscreen/hemisphere geometry, `uDayPhase`, `uSunDirection`, `uMoonDirection`, Star-Seed und Depth Write aus. Aktuell gibt es nur Clear-Color plus Fog; echte Himmelskoerper gehoeren nicht in den Chunk-Shader.

### Akzeptanz

- Render Distance wird weich kaschiert.
- Morgen/Abend/Nacht fühlen sich unterschiedlich an.
- Bloom überstrahlt Pixel-Art nicht.

---

# P5 – Transparenz und Wasser

## P5.1 Sortierung

### Basis

Transparente Chunk-Sortierung ist begonnen.

### Offen

- ~~Sortierung nach Kameraabstand stabilisieren.~~ Translucent-Sort nutzt Mesh-Bounds-Center statt nur Chunk-Center.
- ~~nur transparente Layer sortieren.~~ Nur `TerrainPass.TRANSLUCENT` ruft die Sortierung auf.
- ~~Wasserflächen ggf. separat sortieren.~~ Aktuell nicht separat noetig: Wasser/Glass bleiben im Translucent-Layer, bounds-basiert sortiert.
- ~~bei gleicher Distanz stabile Reihenfolge nutzen.~~ Tie-Breaker sortiert deterministisch nach Chunk-X/Z.
- ~~Debug: sorted transparent count.~~ HUD zeigt `SORT` fuer sortierte Translucent-Meshes.

### Akzeptanz

- Wasser/Glass flackert nicht sichtbar.
- Cutout-Pflanzen sind nicht im transparenten Sortierproblem.

---

## P5.2 Translucent Design-Regeln

### Regeln

- ~~so wenig echte Transparenz wie möglich.~~ Material-Layer validieren Alpha/Translucent-Kompatibilitaet.
- ~~Vegetation als Cutout.~~ Pflanzen/Props bleiben im Cutout-Layer mit Alpha-Test.
- ~~Wasser als Spezialfall.~~ Wasser nutzt `animatedFluid`, Translucent Pass, Underwater-Fog und Simple-Water-Toggle.
- ~~Glass nur sparsam.~~ Translucent-Layer ist isoliert und zaehlbar, neue Alpha-Materialien fallen in Stats/Validation auf.
- ~~Partikel separat.~~ Particle-Renderer hat eigene Buffer, Blend-State und Quality-Budget.

### Umsetzung

- `WorldRenderer.transparentRenderOrderByBounds(...)` ist testbar und stabil.
- Debug-HUD trennt `WATER`/Transparent Draw Count von `SORT`-Count.
- Cutout bleibt ausserhalb des Transparenz-Sortierproblems.

### Akzeptanz

- Draw Calls und Sortieraufwand bleiben kontrollierbar.
- Transparenzfehler sind selten und debugbar.

---

# P6 – Rendering Debug Tools

## P6.1 Debug Views

### Offen

- ~~Chunk bounds view.~~ `/debugchunks` zeichnet Chunk-Bounds.
- ~~Section bounds view.~~ `/debugsections` zeichnet non-empty Section-Bounds.
- ~~Render layer view.~~ `/debugview layer` faerbt Solid/Cutout/Transparent.
- ~~Material index view.~~ `/debugview material` faerbt Material-IDs stabil.
- ~~UV atlas view.~~ `/debugview uv` zeigt Face-UV/checker und `/debugatlas uv <block>` listet Atlas-Rects.
- ~~Light level view.~~ `/debugview light` zeigt Terrain-Light.
- ~~AO view.~~ `/debugview ao` zeigt Ambient-Occlusion.
- ~~Biome tint view.~~ `/debugview biome` zeigt die aktive Biome-Tint-Farbe.
- ~~Entity bounds view.~~ Debug-HUD/Hitbox-Debug bleibt per Entity-Bounds-Renderer sichtbar.
- ~~Particle bounds/budget view.~~ `/debugparticles` zeichnet Particle-Bounds; HUD zeigt Particle-Budget und PBOUNDS.
- ~~Overdraw/transparent debug optional.~~ `/debugview transparent` hebt Transparent-/Cutout-Layer sichtbar hervor.

### Umsetzung

- `RenderDebugView` deckt Shader-Modi `off/material/light/ao/biome/layer/uv/transparent` ab und kann per `/debugview` oder F6 durchgeschaltet werden.
- `ChunkBorderRenderer` rendert Chunk-, Mesh-, Section-, Entity-, Particle-, Block- und Mining-Face-Linien ueber denselben Debug-Line-Pfad.
- `ClientWorld.sectionBoundsAround(...)` liefert Section-Bounds aus echten non-empty Sections; `ParticleSystem.particleBounds()` liefert Live-Particle-Bounds.

### Akzeptanz

- Rendering-Bugs können direkt im Spiel eingegrenzt werden.
- Debug Views sind per Command/Hotkey schaltbar.

---

## P6.2 Render Stats

### Offen

- ~~Draw Calls pro Pass.~~ HUD zeigt `PDC S/C/W`.
- ~~Triangles pro Pass.~~ HUD zeigt `TRIS S/C/W/TOTAL`.
- ~~Mesh count pro Layer.~~ HUD zeigt `MESH S/C/W`.
- ~~culled chunks/layers/entities.~~ HUD zeigt `CULLM`, `CULLC`, `CD`, `CB`, `ECULL`.
- ~~upload bytes pro Frame.~~ `WorldRenderer` sammelt Upload-Bytes pro Rebuild-Frame; HUD zeigt `UPB`.
- ~~atlas size.~~ HUD und `/debugatlas` zeigen Atlas-Anzahl, Breite/Hoehe und Bytes.
- ~~material count.~~ HUD zeigt Materialanzahl, LUT-Bytes und Missing-Material-Count.
- ~~shader reload count optional.~~ `ShaderRegistry` registriert alle `ShaderProgram.fromResources(...)`-Programme, `/shaderreload` kompiliert alle Shader neu, laesst bei Fehlern den alten Program-Handle aktiv und schreibt Status/letzten Fehler in den Chat. `RenderResourceTracker.Snapshot` fuehrt `shaderReloadCount`, `failedShaderReloadCount` und `lastShaderReloadMilliseconds`; das Debug-HUD zeigt die Werte neben `SHD`.

### Akzeptanz

- Performance-Regressionen sind im HUD sichtbar.
- neue Renderfeatures zeigen ihre Kosten.

---

# P7 – Visual Style Foundation

## P7.1 Cozy Color Pipeline

### Ziel

Adventura soll nicht neutral/grau wirken, sondern warm, weich und lesbar.

### Offen

- ~~globale Daylight-Farben definieren:~~ `CozyColorPipeline.skyColorForMinute(...)` blendet Morning/Noon/Evening/Night.
  - ~~Morning warm~~
  - ~~Noon klar~~
  - ~~Evening amber~~
  - ~~Night soft blue~~
- ~~Fog-Farben definieren.~~ `CozyColorPipeline.fogColorForMinute(...)` trennt Fog-Palette von Sky-Palette.
- ~~Biome Tint optional:~~ `CozyColorPipeline.biomeTint(...)` priorisiert bekannte Biome-Keys und nutzt Climate-Fallback.
  - ~~Meadow warm green~~
  - ~~Pine forest deeper green~~
  - ~~Mushroom grove muted purple/green~~
  - ~~Lakeside cooler blue/green~~
  - ~~Frost peaks pale blue~~
  - ~~Old ruins dusty moss~~
- ~~Color grading im Shader minimal halten.~~ Terrain-Shader bleibt bei leichter Gamma-/Saturation-Korrektur.

### Umsetzung

- `RenderSettings` fuehrt separate Sky-, Fog- und Biome-Tint-Farben.
- Terrain- und Entity-Shader lesen `uFogColor`; Terrain nutzt zusaetzlich `uBiomeTintColor`.
- Underwater ueberschreibt Sky/Fog lokal, damit Wasser lesbar bleibt.

### Akzeptanz

- Biome fühlen sich visuell unterschiedlich an.
- Nacht bleibt spielbar.
- Farben bleiben pixel-art-kompatibel.

---

## P7.2 Selection und Interaction Visuals

### Offen

- ~~Block outline sauber rendern.~~ Zielblock wird per Debug-Line-Box markiert.
- ~~valid placement weich markieren.~~ Placement-Ziel wird gruen markiert.
- ~~invalid placement weich rot markieren.~~ Blockiert/ungueltig wird rot markiert.
- ~~mining progress als Face Overlay.~~ Mining-Fortschritt rendert als wachsendes Face-Overlay.
- ~~tool requirement hint visuell unterstützen.~~ Nicht-harvestbare Zielbloecke bekommen rote Outline plus bestehenden Interaction-Hint.
- ~~distance/range invalid dezent anzeigen.~~ Zu weit entfernte Hits werden mit transparenter roter Outline markiert.

### Umsetzung

- `GameClient.renderSelectionVisuals(...)` zentralisiert Block-Ziel, Placement-Preview, Mining-Face und Range-Feedback.
- `PlacementPreview` prueft Hoehe, Luft/Wasser-Belegung, Player-Kollision und Entity-Kollision vor dem Rendern.

### Akzeptanz

- Spieler sieht exakt, welchen Block er betrifft.
- Block Break Feedback ist klar.
- Placement-Fehler sind verständlich.

---

# P8 – Rendering Performance Presets

## Presets

### Low

- Render Distance niedrig.
- keine Soft Shadows.
- einfaches Wasser.
- Bloom aus.
- reduzierte Partikel.
- AO optional aus.
- niedriger Upload-/Mesh-Budget.

### Medium

- normale Render Distance.
- AO an.
- animiertes Wasser einfach.
- moderate Partikel.
- Bloom optional.

### High

- höhere Render Distance.
- AO an.
- Bloom/Glow an.
- mehr Partikel.
- bessere Wasseranimation.
- höheres Mesh-/Upload-Budget.

### Offen

- Preset-Datenmodell.
- Settings UI bindet Presets.
- einzelne Optionen überschreibbar.
- Debug-HUD zeigt aktives Preset.

### Akzeptanz

- Low reduziert messbar Last.
- Presets beeinflussen sichtbare Kosten.
- Spieler kann Details feinjustieren.

---

# P9 – Rendering Tests und Smoke Checks

## Unit Tests

- Material LUT mapping.
- missing material fallback.
- atlas UV rect validity.
- transparent sort order.
- render layer validation.
- block texture existence.
- greedy mesh merge rules.
- light/AO merge compatibility.

## Manual Tests

- Wasser bei Tag/Nacht.
- Wasser gegen Terrainkanten.
- Cutout-Pflanzen vor/ hinter Wasser.
- Glow Mushrooms nachts.
- Campfire/Lantern bei Nacht.
- Chunk-Unload/Reload.
- UI Scale mit Debug-HUD.
- Low/Medium/High Presets.
- sehr langes Erkunden.

## Akzeptanz für Rendering-Kern

- keine sichtbaren Blockface-Seams.
- keine neuen hardcoded Shader-IDs.
- Wasser ist lesbar und performant.
- Render Debug Views helfen effektiv.
- Chunk Loading erzeugt keine großen Stutter.
- Low-End Preset ist wirklich günstiger.
- Visual Style bleibt cozy und eigenständig.
