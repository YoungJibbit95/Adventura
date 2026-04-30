# Adventura – Implementation Sprint Plan

Stand: 2026-04-30

Diese Datei schlägt eine Reihenfolge für die nächsten Arbeitsblöcke vor. Ziel ist, zuerst technische Grundlagen zu stabilisieren, damit spätere Gameplay-Features sauber und schön wirken.

---

# Sprint 1 – Tooling, Tests und Messbarkeit

## Ziel

Ohne stabile Tests und Messpunkte ist jeder Engine-Fix schwer beweisbar.

## PRs

### PR 1: JDK/Test Gate Cleanup

Aufgaben:

- JDK 21 Setup dokumentieren.
- `./gradlew test` lokal prüfen.
- `./gradlew buildGame` lokal prüfen.
- README Troubleshooting ergänzen.

Akzeptanz:

- Tests laufen lokal.
- Build läuft lokal.

### PR 2: EngineFrameStats V1

Aufgaben:

- `EngineFrameStats` Record.
- Werte aus `GameClient` ziehen.
- Debug-HUD liest zentrale Stats.
- FPS, Frame, Update, Render, Chunks, Draw Calls, Entities, Particles.

Akzeptanz:

- Debug-HUD zeigt zentrale Stats.
- keine große Overhead-Regression.

### PR 3: Smoke Test Seeds Documentation

Aufgaben:

- Seed-Tabelle übernehmen.
- Testorte dokumentieren.
- Smoke-Checklisten in Repo aufnehmen.

Akzeptanz:

- jeder Entwickler kann dieselben Orte prüfen.

---

# Sprint 2 – Rendering-Kern

## Ziel

Rendering sauber, datengetrieben und erweiterbar machen.

## PRs

### PR 4: Render Material LUT V1

Aufgaben:

- `RenderMaterial` definieren.
- Materialdaten aus Block/Atlas ableiten.
- Shader liest Materialindex.
- harte Block-ID-Sonderfälle weiter entfernen.

Akzeptanz:

- neuer Glow/Water/Cutout-Block braucht keine Shader-ID-Liste.

### PR 5: Texture Atlas Validation

Aufgaben:

- missing textures melden.
- duplicate mappings melden.
- atlas report erzeugen.
- UV Rects debugbar machen.
- seam/padding rules dokumentieren.

Akzeptanz:

- Assetfehler fallen früh auf.

### PR 6: Render Pass Cleanup V1

Aufgaben:

- Terrain, Entity, Particle, UI/Debug logisch trennen.
- `RenderContext` einführen.
- OpenGL-State pro Pass klar setzen.

Akzeptanz:

- `WorldRenderer` wird übersichtlicher.
- neue Renderfeatures haben klaren Ort.

---

# Sprint 3 – ChunkBuildQueue und Mesh Performance

## Ziel

Chunk Loading soll weniger stuttern und langfristig skalieren.

## PRs

### PR 7: ChunkBuildQueue V1

Aufgaben:

- Queue mit Kamera-Priorität.
- ms-Budget.
- Dirty chunks deduplizieren.
- GPU upload im Render Thread.
- Debug stats.

Akzeptanz:

- sichtbare Chunks werden priorisiert.
- weniger Frame-Spikes.

### PR 8: Greedy Meshing AO-Compatible Rules

Aufgaben:

- Merge-Regeln für Material/Light/AO.
- Toggle simple vs greedy.
- Tests für Merge-Korrektheit.

Akzeptanz:

- weniger Triangles ohne kaputte Kanten.

### PR 9: GL Resource Tracking V1

Aufgaben:

- VAO/VBO/Texture zählen.
- Mesh dispose prüfen.
- Debug-HUD zeigt GPU resource counts.
- Shutdown leak warning optional.

Akzeptanz:

- langes Erkunden zeigt keine Mesh-Leaks.

---

# Sprint 4 – Worldgen und Streaming

## Ziel

Worldgen soll reproduzierbarer, schöner und chunk-border-sicherer werden.

## PRs

### PR 10: Heightmap/Biome Cache

Aufgaben:

- Cache pro Chunk.
- Spawn/Structures/Entities nutzen Cache.
- Tests für deterministische Werte.

Akzeptanz:

- weniger doppelte Noise-Samples.

### PR 11: Spawn Safety V2

Aufgaben:

- Spawn-Kandidaten scannen.
- freier Headroom.
- kein Wasser/Cave.
- nahe Starter-Ressourcen.

Akzeptanz:

- Smoke-Test-Seeds spawnen sicher.

### PR 12: Chunk-Border Feature Placement

Aufgaben:

- region-basierte Feature Seeds oder deferred placements.
- Trees/Structures an Chunkrändern erlauben.
- Tests gegen Grid-Lücken.

Akzeptanz:

- keine sichtbaren chunk-gridartigen Baumverbotskanten.

---

# Sprint 5 – Lighting

## Ziel

Lichtquellen, Höhlen und Nacht sollen gameplay-relevant und performant sein.

## PRs

### PR 13: Light Debug View V2

Aufgaben:

- Sky/Block/Combined/Emissive debug.
- Light visualization mode.
- looked-at block light info.

Akzeptanz:

- Light-Seams sind schnell prüfbar.

### PR 14: Incremental Block Light Updates

Aufgaben:

- Add/Remove queues.
- boundary propagation.
- fallback rebuild.
- performance stats.

Akzeptanz:

- Campfire/Lantern updates ohne große Spikes.

### PR 15: Day/Night/Fog Color Curves

Aufgaben:

- morning/noon/evening/night colors.
- fog color transitions.
- night minimum brightness.

Akzeptanz:

- Tageswechsel wirkt cozy und lesbar.

---

# Sprint 6 – Save/Load und BlockEntities

## Ziel

Spielerfortschritt und Weltänderungen dauerhaft machen.

## PRs

### PR 16: Save Format V1

Aufgaben:

- save version.
- metadata.
- safe write.
- unknown item/block fallback.

Akzeptanz:

- Save/Load crasht nicht bei unbekannten Keys.

### PR 17: World Diff Save

Aufgaben:

- placed/removed/modified blocks speichern.
- dirty chunks persistieren.
- load overlay auf generated world.

Akzeptanz:

- abgebaute und platzierte Blöcke bleiben nach Neustart.

### PR 18: BlockEntityStore Persistence

Aufgaben:

- StorageCrate.
- Campfire.
- CookingPot.
- Forge.
- LootCrate.

Akzeptanz:

- Kisten/Fuel/Loot bleiben erhalten.

---

# Sprint 7 – Multiplayer Interest und Async Server

## Ziel

Multiplayer soll skalieren und sicher bleiben.

## PRs

### PR 19: Chunk Subscriptions V1

Aufgaben:

- pro Client aktive Chunks.
- Block updates filtern.
- chunk resend nach unload.

Akzeptanz:

- Clients bekommen nur relevante Chunkupdates.

### PR 20: Entity Interest V1

Aufgaben:

- Entity snapshots nach Radius/Chunk filtern.
- Snapshot counts messen.
- culling serverseitig.

Akzeptanz:

- entfernte Entities werden nicht unnötig gesendet.

### PR 21: Async Server Chunk Generation

Aufgaben:

- Worker queue.
- dedupe requests.
- priority by player.
- backpressure.

Akzeptanz:

- Netty wird nicht durch Chunkgen blockiert.

---

# Sprint 8 – Gameplay auf stabiler Basis

## Ziel

Jetzt Content/Features ausbauen, wenn Engine/Save/Networking stabiler sind.

## PRs

### PR 22: Cooking Pot V1

Aufgaben:

- BlockEntity.
- UI.
- recipes.
- server validation.

Akzeptanz:

- bessere Foods brauchen Cooking Pot.

### PR 23: Forge V1

Aufgaben:

- BlockEntity.
- smelting recipes.
- iron ingot.
- forge UI.

Akzeptanz:

- Iron Progression funktioniert.

### PR 24: Journal Persistence V1

Aufgaben:

- notes.
- discovered biomes.
- discovered recipes.
- save/load.

Akzeptanz:

- Exploration-Fortschritt bleibt erhalten.

---

# Sprint 9 – Entity Life und Animation

## Ziel

Die Welt wirkt lebendiger, ohne Server-Tick oder Renderloop zu sprengen.

## PRs

### PR 25: Entity Lifecycle V1

Aufgaben:

- spawn/tick/despawn/park.
- bounds debug.
- snapshot filtering.

Akzeptanz:

- Entities verschwinden nicht falsch und werden nicht endlos getickt.

### PR 26: Pose System V1

Aufgaben:

- named model parts.
- idle/walk/flee/follow/graze.
- state transitions.

Akzeptanz:

- Tiere wirken lebendig.

### PR 27: Particle Quality Presets

Aufgaben:

- Low/Medium/High budgets.
- fireflies/spores/campfire budgets.
- debug stats.

Akzeptanz:

- Partikel bleiben performant.

---

# Sprint 10 – Polish und Combat Foundation

## Ziel

Adventure-Features vorbereiten, ohne Cozy-Fokus zu verlieren.

## PRs

### PR 28: Damage System V1

Aufgaben:

- DamageSource.
- DamageResult.
- entity health.
- server validation.

Akzeptanz:

- Schaden ist serverseitig sicher.

### PR 29: Bow/Projectile V1

Aufgaben:

- Shoot intent.
- Arrow projectile.
- server hit/collision.
- client interpolation.

Akzeptanz:

- Pfeile sind serverautoritativ.

### PR 30: Ruin Loot/Lore Expansion

Aufgaben:

- rare loot tables.
- old notes.
- map fragments.
- ruin key/seal setup.

Akzeptanz:

- Ruinen lohnen sich spielerisch.

---

# Sprint-Regel

Jeder Sprint sollte mindestens enthalten:

- 1 technischer Test oder Smoke-Test.
- 1 Debug-/Messverbesserung, wenn Engine betroffen ist.
- 1 klare Akzeptanzprüfung.
- keine stillen Shader-Sonderfälle.
- keine neuen clientseitigen Trust-Lücken.