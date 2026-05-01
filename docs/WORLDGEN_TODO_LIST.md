# Adventura – Worldgen & Streaming TODO List

Stand: 2026-05-01

## Ziel

Worldgen soll Adventura spielerisch tragen: jedes Biom braucht eigene Ressourcen, klare Silhouette, eigene Stimmung, sinnvolle Structures und reproduzierbare Tests. Gleichzeitig müssen Chunks performant generiert, gestreamt, entladen und später gespeichert werden.

## Leitlinien

- gleicher Seed erzeugt dieselbe Welt.
- Server und Offline-Singleplayer nutzen dieselben Common-Regeln.
- Worldgen bleibt deterministisch.
- Features dürfen keine sichtbaren Chunk-Grenz-Artefakte erzeugen.
- Structures dürfen nicht halb abgeschnitten an Chunkrändern entstehen.
- Spawn muss immer sicher sein.

---

# P0 – Generator-Struktur und Testbarkeit

## P0.1 Generator in klare Passes schneiden

### Ziel

Worldgen soll nicht aus einer großen Methode mit vielen Sonderfällen bestehen. Passes erleichtern Tests, Balancing und spätere Features.

### Empfohlene Passes

1. Climate / Biome Sampling
2. Heightmap / Terrain Shape
3. River / Lake / Cave Masks
4. Terrain Fill
5. Ores / Underground Resources
6. Surface Decoration
7. Structures
8. Loot Markers
9. Ambient Entity Markers
10. Spawn-Safety Pass

### Status 2026-05-01

`OverworldGenerator.planChunk(...)` liefert die zentralen Pass-Produkte für einen Chunk: Terrain-Cache, geplante Structure, optionalen Spawn-Punkt und `GenerationMetrics`. Die eigentliche Chunk-Mutation nutzt diesen Plan und sammelt angewendete Metriken für Biome-/Height-Samples, Feature-Placements, Structure-Versuche/-Erfolge, Loot-/Entity-Marker, abgelehnte Placements und Spawn-Kandidaten. Terrain Sampling, Structure-Auswahl und Spawn Safety sind damit einzeln testbar.

### Akzeptanz

- Worldgen-Fehler können einem Pass zugeordnet werden.
- Tests können einzelne Passes prüfen.
- spätere Biome/Structures lassen sich hinzufügen, ohne Terrain-Basis zu beschädigen.

---

## P0.2 Heightmap- und Biome-Cache

### Problem

Terrainhöhe und Biome werden oft mehrfach gesampelt: Spawn, Structures, Entities, Decorations und Debug brauchen dieselben Informationen.

### Status 2026-05-01

`ChunkTerrainCache` hält pro Chunk immutable Heightmap- und Biome-Spalten. `OverworldGenerator` erzeugt und speichert diesen Cache am Chunk, nutzt ihn für Terrain Fill, Decoration, Structures und Starter-Ressourcen. Ambient-Spawns, Client-Spawnposition, Server-Lootmarker und das Debug-Biome/Height-Overlay lesen dieselben gecachten Spalten, wenn ein geladener Chunk sie bereitstellt.

### Akzeptanz

- weniger doppelte Noise-Samples.
- Spawn/Structure/Entity-Regeln greifen auf gleiche Daten zu.
- Tests für Biome/Height werden einfacher.

---

## P0.3 Spawn Safety

### Ziel

Spieler soll zuverlässig sicher starten.

### Regeln

- bevorzugt Cozy Meadow, Flower Fields oder Lakeside-Rand.
- Füße stehen auf solidem Boden.
- Headroom frei.
- nicht in Wasser.
- nicht in Cave.
- nicht in Structure-Wand.
- nah an Starter-Ressourcen.
- keine gefährliche Entity direkt am Spawn.

### Status 2026-05-01

`OverworldGenerator.safeSpawnPoint()` scannt deterministisch Spawn-Kandidaten um den Startbereich, bevorzugt Cozy Meadow, Flower Fields und Lakeside, erzwingt soliden Support, freie Headroom-/Player-Bounds, keinen Wasserstart, keine blockierende Decoration/Structure-Spalte und Nähe zu Starter-Ressourcen. Der Spawn-Punkt wird als Common-Datenprodukt genutzt; `ClientWorld.spawnPosition()` hängt daran. Smoke-Seed-Tests validieren sicheren Spawn, Starter-Ressourcen-Nähe und keine gefährliche Ambient-Entity direkt am Spawn.

### Akzeptanz

- Spawn ist bei allen Smoke-Test-Seeds sicher.
- Spieler steht nicht mehrere Blöcke in der Luft.
- Spieler startet nahe sichtbarer Early-Game-Ressourcen.

---

# P1 – Biome Identity

## Ziel

Biome sollen sich nicht nur optisch unterscheiden, sondern klare Gameplay-Gründe haben.

## P1.1 Biome Resource Profiles

### Status 2026-05-01

`BiomeResourceProfile` und `BiomeResourceProfiles` definieren pro Default-Biom Surface-, Vegetation-, Resource-, Ore-, Structure-, Ambient-Entity-, Rare-Feature- und Tint-Profile. Der Generator nutzt diese Profile für Detailressourcen, und `AmbientEntitySpawner` liest die Ambient-Entity-Tabelle daraus. Die priorisierten Alpha-Biome haben damit explizite Gameplay-Gründe: Cozy Meadow für sichere Starterressourcen, Pine Forest für Holz/Mushrooms/Boars, Mushroom Grove für Glow/Mushrooms/Fireflies, Lakeside für Clay/Reeds, Old Ruins für Ruin-/Glow-Hooks, Highlands für Stone/Ore-Progression und Frost Peaks für Snow/Ice/Crystal-Exploration.

### Verifikation 2026-05-01

- `BiomeResourceProfilesTest` prüft Profilabdeckung für alle Default-Biome und die priorisierten Alpha-Ressourcen.
- `OverworldContentTest` prüft deterministische sichtbare Ressourcen in den Kernbiomen und an reproduzierbaren Smoke-Koordinaten.

### Akzeptanz

- Spieler erkennt Biome an Ressourcen und Silhouette.
- jedes Biom hat mindestens einen Gameplay-Grund.
- Resource distribution ist deterministisch getestet.

---

## P1.2 Biome Transitions glätten

### Status 2026-05-01

Biome-Sampling glättet Temperature/Moisture über Nachbarsamples, `terrainHeight(...)` blendet die biome-spezifischen Höhenanpassungen über angrenzende Biome, und River/Lake-Nähe bleibt Teil der Biome-Auswahl. `OverworldGenerator.biomeTransitionAt(...)` liefert einen Boundary-/Edge-Faktor für Tests und Debugging; `/debugbiome` zeigt diesen Edge-Wert neben Biom und Terrainhöhe an.

### Verifikation 2026-05-01

- `OverworldGeneratorTest` findet deterministisch eine Biome-Grenze, prüft den Edge-Faktor und begrenzt extreme Ein-Schritt-Höhensprünge an der Boundary.
- `ReproducibleWorldSeedsTest` hält die Smoke-Biome nach dem geglätteten Sampling stabil.

### Akzeptanz

- keine abrupten Wand-/Hügelkanten nur wegen Biome-Wechsel.
- Übergänge wirken natürlicher.
- Biome bleiben trotzdem klar erkennbar.

---

# P2 – Decoration und Feature Placement

## P2.1 Chunk-Rand-Artefakte entfernen

### Problem

Wenn Bäume/Features Chunkränder meiden, entstehen sichtbare gridartige Lücken.

### Ziel

Features dürfen chunkübergreifend platziert werden, ohne Nachbar-Chunks nondeterministisch zu beschädigen.

### Lösungsoptionen

#### Option A: Region-basierte Feature-Seeds

- Features pro Region statt pro Chunk planen.
- jeder Chunk liest relevante Nachbar-Regionen.
- deterministische Platzierung auch über Chunkgrenzen.

#### Option B: Deferred Placements

- Feature-Placer erzeugt geplante Blocks mit globalen Koordinaten.
- Zielchunk übernimmt geplante Blocks beim Generieren.
- pending placements werden deterministisch aus Seed berechnet.

#### Option C: Template-Bounds

- Feature wird nur platziert, wenn alle betroffenen Chunks berechenbar sind.
- bei Nachbar fehlend wird Placement später wiederholt.

### Empfehlung

Für Adventura: Region-basierte Feature-Seeds oder deterministic deferred placements. Das passt gut zu Trees, Rocks, Cabins und Ruins.

### Akzeptanz

- Bäume können an Chunkrändern stehen.
- keine sichtbaren Raster-Lücken.
- gleiche Seed/Koordinaten ergeben gleiche Features.

---

## P2.2 Feature Tables

### Offen

Feature Tables definieren:

- `meadow_surface_features`
- `pine_forest_features`
- `mushroom_grove_features`
- `lakeside_features`
- `old_ruins_features`
- `highlands_features`
- `frost_features`

### Feature Entry

- feature key
- weight
- min/max count
- spacing
- slope rules
- allowed biomes
- required surface
- avoid water
- avoid structure
- rare chance

### Akzeptanz

- Feature-Verteilung ist datengetriebener.
- neue Pflanzen/Rocks/Decorations brauchen weniger Generator-Sonderfälle.

---

# P3 – Structures

## P3.1 Structure Templates

### Ziel

Structures sollen schöner, größer und sicherer werden.

### Template-Daten

- size
- block palette
- local block map
- anchors
- rotations
- loot markers
- entity markers
- interaction markers
- metadata markers
- ground fit rules
- clearance rules

### Offen

- kleine Structure Template API definieren.
- simple hardcoded templates als Übergang erlauben.
- marker-basierte Loot/Entity-Spawns nutzen.
- rotations unterstützen.
- structure id deterministisch speichern.

### Akzeptanz

- neue kleine Structure kann hinzugefügt werden, ohne OverworldGenerator stark zu vergrößern.
- Loot Marker sind eindeutig.
- Entity Marker sind eindeutig.

---

## P3.2 Multi-Chunk Structures

### Problem

Structures können mehrere Chunks betreffen und dürfen nicht halb fehlen.

### Offen

- Placement pro Region planen.
- alle betroffenen Chunks kennen Structure-Plan.
- keine doppelten Loot Marker.
- Structure State speichern.
- Clearance und Ground-Fit vor Platzierung prüfen.

### Akzeptanz

- Watchtower/Cabin/Mine Entrance spawnen vollständig.
- keine abgeschnittenen Structures an Chunkgrenzen.
- Structure-Loot generiert nur einmal.

---

## P3.3 Structure-Liste ausbauen

### Priorisierte Structures

1. cozy_campsite
2. abandoned_cabin
3. lakeside_shack
4. small_ruin
5. old_watchtower
6. old_mine_entrance
7. mushroom_circle
8. hidden_well
9. broken_bridge
10. ruined_market_stall
11. frozen_shrine
12. ancient_gateway

### Jede Structure braucht

- Spawn-Biome
- Spawn-Chance
- Größe
- Terrain constraints
- Blockpalette
- LootTable
- Lore chance
- Entity spawns
- Progression value
- Smoke-Test-Ort oder Debug-Spawn-Command

---

# P4 – Ores, Caves und Underground

## P4.1 Ore Distribution

### Offen

- Copper in Highlands/Caves.
- Iron tiefer oder in Mine Entrances.
- Glow Crystal in Mushroom Grove/Frost/Caves.
- Ancient Fragments eher in Ruins.
- toolLevel requirements validieren.
- ore frequency debugbar machen.

### Akzeptanz

- Mining-Progression ist klar.
- Stone -> Copper -> Iron -> Crystal wird durch Worldgen unterstützt.
- Ore-Spawns sind nicht zu grindy.

---

## P4.2 Caves

### Offen

- Cave noise testen.
- Cave openings in Highlands/Old Ruins optional.
- cave lighting prüfen.
- player collision an cave shells prüfen.
- cave resources platzieren.
- keine Spawn-Safety-Verletzung.

### Akzeptanz

- Cave pockets sind erreichbar und nicht buggy.
- Höhlen sind dunkler, aber debugbar.
- Terrain-Seams an Caves fehlen nicht.

---

# P5 – Chunk Streaming und Runtime

## P5.1 Client Streaming

### Offen

- ringförmiges Chunk-Set um Kamera/Spieler.
- Hysterese für Laden/Entladen.
- aktive Chunk-Liste im Debug.
- dirty chunks bleiben gepinnt.
- unloaded chunks geben Runtime-Daten frei.
- Rückkehr in Gebiet lädt korrekt neu.

### Akzeptanz

- langes Erkunden wächst nicht unbegrenzt.
- Blockänderungen bleiben lokal erhalten.
- keine Mesh-Leaks.

---

## P5.2 Server Streaming

### Offen

- Chunks pro Verbindung nach Bedarf streamen.
- Chunk Subscriptions pro Client.
- Block-/Entity-Updates nur an relevante Clients.
- async chunk generation.
- resend bei Multiplayer-Unload.

### Akzeptanz

- Server sendet nicht die ganze Welt an alle.
- Client kann Chunks entladen und später korrekt neu erhalten.

---

# P6 – Worldgen Debug Tools

## Offen

- `/tpbiome biome_key` oder Debug Helper.
- `/spawnstructure structure_key`.
- Biome map overlay.
- Feature density debug.
- Structure bounds debug.
- Loot marker debug.
- Spawn point debug.
- Ore distribution debug optional.

## Akzeptanz

- Worldgen-Probleme sind im Spiel debugbar.
- QA kann gezielt Biome/Structures prüfen.

---

# Tests

## Unit Tests

- biome selection deterministic.
- heightmap deterministic.
- spawn position safe.
- feature placement deterministic.
- no chunk-border decoration gaps test via sample grid.
- structure placement deterministic.
- structure marker uniqueness.
- ore distribution references valid blocks/items.

## Integration Tests

- generate chunk ring around spawn.
- place/break block then unload/reload.
- structure chest loot generated once.
- multiplayer chunk resend after client unload.

## Manual Smoke Tests

Siehe `11_WORLD_SMOKE_TESTS.md`.

## Akzeptanz

- jedes Biom hat klare Identität.
- Structures sind vollständig und sinnvoll.
- Worldgen bleibt deterministisch.
- Spawn ist sicher.
- Chunk-Grenzen sind nicht sichtbar problematisch.
