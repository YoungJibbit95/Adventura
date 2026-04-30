# Adventura – Reproducible Smoke Tests

Stand: 2026-04-30

Diese Smoke-Tests sind feste Prüfanker für Rendering, Gameplay, Worldgen, Physics, Lighting, UI/HUD, Networking und Persistenz.

## Vorbereitung

Empfohlen:

```sh
./gradlew buildGame
./gradlew runSingleplayer
```

Für Multiplayer:

```sh
./gradlew runServer
./gradlew joinLocal
```

Nützliche Commands im Spiel:

- `/tp x y z`
- `/debug`
- `/debuglight`
- `/renderdistance n`
- `/preview n`
- `/meshbudget n`
- `/gamemode survival|creative|spectator`
- `/spawn`

Hinweis: Die Y-Werte in der Tabelle sind Kamera-/Zielreferenzen, keine garantiert gespeicherten Spawnpositionen.

---

# Shared Checklist

Bei jedem Szenario prüfen:

## Basis

- Zielposition ist sicher.
- Spieler steht nicht in Blöcken.
- keine sichtbaren Terrain-Seams.
- Biome/Surface-Materialien passen.
- FPS und Debugwerte plausibel.
- keine offensichtlichen Chunk-Holes.

## Rendering

- Blockfaces haben keine sichtbaren Nähte.
- Cutout-Pflanzen rendern korrekt.
- Wasser/Transparenz sortiert plausibel.
- Fog kaschiert Render Distance.
- Glow/Bloom wirkt nicht überstrahlt.

## Lighting

- Tageslicht plausibel.
- Schatten/AO nicht kaputt.
- Campfire/Lantern/Glow Blocks sichtbar.
- keine Light-Seams an Chunkgrenzen.

## Gameplay

- Ressourcen sind sichtbar.
- Interaktionen funktionieren.
- HUD zeigt richtige Hinweise.
- keine falschen Tool-Hints.
- Drops/Pickups funktionieren.

## Performance

- keine großen Frame-Spikes beim Umschauen.
- Chunkgen/Meshing/Upload Werte plausibel.
- Draw Calls/Triangles nicht extrem.
- Partikel bleiben im Budget.

---

# Seed Table

Nutze Seed `1337`, sofern nicht anders angegeben.

| Szenario | Seed | X | Y | Z | Erwartung |
| --- | ---: | ---: | ---: | ---: | --- |
| Spawn baseline | 1337 | 8 | 120 | 8 | Starter campsite und nahe Basic Resources |
| River / lakeside | 1337 | -968 | 65 | -1272 | `voxel:lakeside`, Wasser, Ufer, Clay/Reeds |
| Pine forest | 1337 | -920 | 95 | -1272 | `voxel:pine_forest`, Pines, Resin/Bark, Waldstimmung |
| Mushroom grove | 1337 | -40 | 94 | -936 | `voxel:mushroom_grove`, Glow, Mushrooms, Snails |
| Cave pocket | 1337 | -56 | 68 | -504 | Cave Air unter Terrain, Darkness/Collision |
| Village / market | 1337 | 24 | 120 | 24 | `voxel:compact_village`, roads, market, loot/entity markers |
| Frost peaks | 1337 | 760 | 102 | -952 | `voxel:frost_peaks`, Schnee/Eis/Crystal Kandidaten |
| Desert / dunes | 1337 | 1128 | 80 | -632 | `voxel:sun_dunes`, Dunes, sparse details |
| Old ruins | 1337 | -1144 | 79 | 1016 | `voxel:old_ruins`, Ruin materials, loot/glow candidates |

---

# Scenario Checklists

## 1. Spawn Baseline

Prüfen:

- Singleplayer startet.
- Default Hotbar ist sinnvoll.
- HUD zeigt Health/Hunger/Stamina/Comfort.
- Starter-Campsite sichtbar.
- Campfire/Cooking Pot/Lantern/Storage sichtbar, falls vorhanden.
- Ressourcen sichtbar:
  - twig
  - pebble
  - fiber
  - berries
  - herbs
  - mushroom
- erstes Sammeln triggert Feedback.
- erstes Rezept-Unlock triggert Feedback.
- keine Spawn-Collision.

## 2. River / Lakeside

Prüfen:

- Wasser rendert korrekt.
- Shore materials passen.
- Clay/Reeds sichtbar.
- Wasserphysik funktioniert.
- Breath HUD nur bei Unterwasser relevant.
- Fog/Water bei Tag und Nacht lesbar.
- keine transparent sorting bugs an Uferkante.

## 3. Pine Forest

Prüfen:

- Pines korrekt generiert.
- keine chunk-gridartigen Baum-Lücken.
- Resin/Bark Ressourcen vorhanden.
- Wald ist dichter, aber spielbar.
- Entities sichtbar, aber nicht gespammt.
- Cutout/Leaves rendern korrekt.

## 4. Mushroom Grove

Prüfen:

- Mushroom Resources sichtbar.
- Glow Details sichtbar.
- Spore/Firefly Particles im Budget.
- Snails/Fireflies sichtbar.
- Glow/Bloom nicht zu stark.
- Nachtstimmung besonders.

## 5. Cave Pocket

Prüfen:

- Cave Air korrekt.
- Stone Shell kollidiert.
- Cave ist dunkler.
- `/debuglight` zeigt Lightwerte plausibel.
- keine Lecks ins Void.
- Player fällt nicht durch unbekannte Chunks.

## 6. Village / Market

Prüfen:

- Roads und Häuser stehen vollständig.
- Market Stall sichtbar.
- Loot Marker generieren Loot nur einmal.
- Entity Marker plausibel.
- Structure an Chunkgrenzen nicht abgeschnitten.
- Storage/Crate Interaction funktioniert.

## 7. Frost Peaks

Prüfen:

- Schnee/Eis-Materialien korrekt.
- Höhenprofil sieht natürlich aus.
- Render Distance/Fog versteckt Kanten.
- Crystal Kandidaten sichtbar.
- Movement auf steilem Terrain stabil.

## 8. Desert / Dunes

Prüfen:

- Dune Silhouetten plausibel.
- sparse placement nicht leer/kaputt.
- Sand-Material klar.
- Hitze/Temperature nur anzeigen, wenn System aktiv ist.
- Performance in offener Landschaft stabil.

## 9. Old Ruins

Prüfen:

- Old Ruins Biome klar erkennbar.
- Ruin Bricks/Ancient Tiles korrekt.
- Loot/Lore Kandidaten sichtbar.
- Lantern/Glow/Light in Ruinen plausibel.
- keine abgeschnittenen Ruinen.

---

# Cross-System Smoke Tests

## A. Long Explore / Chunk Unload

Ablauf:

1. `runSingleplayer` starten.
2. Debug HUD aktivieren.
3. 10 Minuten in eine Richtung fliegen/laufen.
4. Loaded Chunks, GPU Meshes und Entity Count beobachten.
5. zurück zum Spawn teleportieren.
6. prüfen, ob Chunks neu laden.

Akzeptanz:

- Loaded Chunks wachsen nicht unbegrenzt.
- GPU Mesh Count wächst nicht unbegrenzt.
- keine GL Errors.
- Rückkehr funktioniert.

## B. Night / Campfire / Lighting

Ablauf:

1. Nacht abwarten oder debug time setzen, falls möglich.
2. Campfire aktivieren.
3. Lantern platzieren.
4. Glow Block prüfen.
5. `/debuglight` nutzen.

Akzeptanz:

- Campfire/Lantern beleuchten sichtbar.
- Nacht ist spielbar.
- Light-Seams fehlen.

## C. Cooking Station

Ablauf:

1. Ressourcen sammeln.
2. Campfire fueln.
3. cooked food starten.
4. Cooking Pot suchen/platzieren.
5. Pot Recipe prüfen.
6. Inventory full edge case testen.

Akzeptanz:

- Server/Singleplayer validiert Zutaten.
- Output wird nicht dupliziert.
- HUD/UI erklärt fehlende Zutaten/Fuel.

## D. Storage / Loot Persistence

Ablauf:

1. Crate öffnen.
2. Items einlagern.
3. Welt verlassen.
4. Welt neu laden.
5. Crate erneut öffnen.
6. Loot Crate öffnen, neu laden, erneut prüfen.

Akzeptanz:

- Storage bleibt erhalten.
- Loot resetet nicht.
- keine Dupes.

## E. Multiplayer Join Local

Ablauf:

1. `runServer` starten.
2. `joinLocal` starten.
3. bewegen.
4. Block abbauen/platzieren.
5. craften.
6. kochen.
7. storage transfer.
8. entity interact.

Akzeptanz:

- keine Desyncs.
- ungültige Range wird abgelehnt.
- Inventory Snapshots bleiben korrekt.

## F. UI/HUD Scale

Ablauf:

1. UI Scale 1x testen.
2. UI Scale 1.5x testen.
3. UI Scale 2x testen.
4. Inventory/Crafting/Storage/Journal/Settings öffnen.
5. HUD Normal/Minimal/Debug testen.

Akzeptanz:

- nichts überlappt kritisch.
- Tooltips bleiben lesbar.
- Drag/drop funktioniert.

---

# Was pro Smoke Test notieren

- Datum
- Git Commit
- Java Version
- OS
- Seed
- Koordinaten
- Render Distance
- Preview Radius
- FPS min/avg grob
- Draw Calls
- Triangles
- Loaded Chunks
- sichtbare Fehler
- Screenshot optional
- Reproduktionsschritte

---

# Smoke Test Akzeptanz gesamt

Ein Build ist smoke-stabil, wenn:

- Spawn funktioniert.
- mindestens 5 Biome-Orte geprüft wurden.
- Long Explore keine Leaks zeigt.
- Campfire/Lighting funktioniert.
- Inventory/Cooking/Storage funktioniert.
- Join Local funktioniert.
- Debug HUD plausible Werte zeigt.