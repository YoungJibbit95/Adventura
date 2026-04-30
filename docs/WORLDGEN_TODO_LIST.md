# Adventura – Worldgen & Streaming TODO List

Stand: 2026-04-30

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

### Offen

- bestehende `OverworldGenerator`-Logik nach Passes dokumentieren.
- pro Pass reine Datenprodukte definieren.
- Seiteneffekte minimieren.
- pro Pass Debug-Metriken erfassen:
  - biome samples
  - height samples
  - feature placements
  - structure attempts/successes
  - rejected placements

### Akzeptanz

- Worldgen-Fehler können einem Pass zugeordnet werden.
- Tests können einzelne Passes prüfen.
- spätere Biome/Structures lassen sich hinzufügen, ohne Terrain-Basis zu beschädigen.

---

## P0.2 Heightmap- und Biome-Cache

### Problem

Terrainhöhe und Biome werden oft mehrfach gesampelt: Spawn, Structures, Entities, Decorations und Debug brauchen dieselben Informationen.

### Offen

- pro Chunk Heightmap cachen.
- pro Chunk Biome map cachen.
- Cache immutable oder generation-stage-safe halten.
- Cache für Spawn/Structures/Entities verwenden.
- Debug Overlay kann aktuelle Height/Biome aus Cache lesen.

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

### Offen

- Spawn-Kandidaten scannen.
- Spawn-Punkt aus Surface + freiem Headroom berechnen.
- Fallback-Kandidaten nutzen.
- Starter-Campsite sauber platzieren.
- Spawn Safety Unit Tests.

### Akzeptanz

- Spawn ist bei allen Smoke-Test-Seeds sicher.
- Spieler steht nicht mehrere Blöcke in der Luft.
- Spieler startet nahe sichtbarer Early-Game-Ressourcen.

---

# P1 – Biome Identity

## Ziel

Biome sollen sich nicht nur optisch unterscheiden, sondern klare Gameplay-Gründe haben.

## P1.1 Biome Resource Profiles

### Offen

Für jedes Biom eigene Profile definieren:

- Surface blocks
- Vegetation table
- Resource table
- Ore table
- Structure table
- Ambient entity table
- Fog/color tint optional
- rare feature table

### Biome-Ziele

#### Cozy Meadow

- sichere Startressourcen
- twigs, pebbles, fiber, berries, herbs
- Hasen, Schafe
- kleine Campsites
- warmer Look

#### Pine Forest

- resin
- bark strip
- mushrooms
- pine wood
- abandoned cabins
- boars
- dichter, aber lesbar

#### Mushroom Grove

- mushroom clusters
- glow mushrooms
- glow crystal chance
- moss snails
- fireflies
- spore particles
- magischer Look

#### Lakeside

- clay
- reeds
- water container resources
- herbs
- lakeside shack
- fireflies
- cooking/pottery progression

#### Old Ruins

- ruin bricks
- ancient fragments
- ancient tiles
- loot crates
- lore notes
- watchtowers/market ruins
- adventure progression

#### Highlands

- copper/iron
- stone/gravel
- old mine entrances
- watchtowers
- wind ambience
- mining progression

#### Frost Peaks

- crystals
- rare herbs
- snow/ice
- frozen shrines
- late-game exploration
- cold system optional

### Akzeptanz

- Spieler erkennt Biome an Ressourcen und Silhouette.
- jedes Biom hat mindestens einen Gameplay-Grund.
- Resource distribution ist deterministisch getestet.

---

## P1.2 Biome Transitions glätten

### Problem

Harte Biome-Parameter können harte Kanten im Terrain erzeugen.

### Offen

- Height-Parameter über Nachbarsamples blenden.
- Moisture/Temperature weich blenden.
- Surface-Block-Wechsel optional über Übergangsblöcke.
- River/Lake-Nähe in Biome-Auswahl berücksichtigen.
- Debug View für Biome Boundaries.

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