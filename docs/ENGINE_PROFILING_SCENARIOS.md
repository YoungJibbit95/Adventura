# Adventura - Engine Profiling Scenarios

Stand: 2026-05-01

Diese Szenarien ergaenzen `docs/WORLD_SMOKE_TESTS.md` und geben Performance-PRs feste Messpunkte. JFR-Dateien liegen nach manueller Beendigung des Clients unter `build/reports/jfr/`.

## Gradle Tasks

- `./gradlew profileSingleplayerJfr`: kurzer Spawn-/Baseline-Lauf mit Preview-Radius 3 und Render-Distance 8.
- `./gradlew profileJoinLocalJfr`: Client-Join gegen einen laufenden lokalen Server auf `127.0.0.1:25565`.
- `./gradlew profileLongExploreJfr`: Singleplayer mit Seed `424242`, Preview-Radius 5 und Render-Distance 12 fuer laengere Chunk-/Streaming-Profile.

## Pflichtmetriken

- CPU: `GameClient` Main Loop, `ClientWorld`, `WorldRenderer`, `ChunkMesher`, `LightEngine`.
- Allocation: Chunkgen, Meshing, Lighting, Packet Decode, Partikel.
- GC: Pausen, Allocation Rate, Old-Gen-Wachstum.
- Chunk Build: Queue-Laenge, Wait-Zeit, Build-Zeit, abgebrochene oder ersetzte Builds.
- Lighting: Sky-Light, Block-Light, Dirty-Region-Zeit.
- GPU Upload: Upload-Zeit, Upload-Bytes, freigegebene Mesh-Bytes.
- Rendering: Framezeit, Draw Calls, Triangles, Pass-Zeiten, VRAM-Schaetzung.
- Netzwerk: Packets/s, Bytes/s, durchschnittliche Packetgroesse, Rejects.

## Szenarien

| Szenario | Task | Seed / Setup | Fokus |
| --- | --- | --- | --- |
| Spawn Baseline | `profileSingleplayerJfr` | Default Singleplayer | Frame Loop, Initial Chunkgen, erste Mesh-Uploads |
| River / Lakeside | `profileSingleplayerJfr` | Koordinaten aus `WORLD_SMOKE_TESTS.md` | Wasser, Transparenz, Fluid-Erkennung |
| Mushroom Grove | `profileSingleplayerJfr` | Koordinaten aus `WORLD_SMOKE_TESTS.md` | Cutout/Foliage, Partikel, Ambient Entities |
| Cave Pocket | `profileSingleplayerJfr` | Cave-Seed aus `WORLD_SMOKE_TESTS.md` | Lighting, Section-Bounds, dunkle Bereiche |
| Long Explore | `profileLongExploreJfr` | Seed `424242` | Chunk Lifetime, Unload, Build Queue, GPU Release |
| Join Local | `profileJoinLocalJfr` | Server vorher mit `./gradlew runServer` starten | Chunk Streaming, Packet Decode, Entity Snapshots |

## Report-Notizen

Ein Profiling-Report sollte kurz nennen:

- verwendeter Task und Szenario.
- Laufdauer und ungefaehre Route.
- auffaellige Top-Methoden nach CPU und Allocation.
- hoechste Framezeitspitzen und zugehoerige Debug-HUD-Werte.
- Chunk-/Lighting-/Upload-Budgetwerte vor und nach der Aenderung.
- offene Risiken oder manuelle Smoke-Schritte.
