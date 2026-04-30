# Adventura – Lighting TODO List

## Ziel

Lighting soll cozy, weich und technisch stabil werden. Adventura braucht warme Tagesfarben, klares Nachtgefühl, leuchtende Campfires/Lanterns/Glow Mushrooms und später dunklere Höhlen ohne harte Performance-Spikes.

## P0 – Lighting-Begriffe

Diese Liste behandelt Lighting/Beleuchtung, nicht Lightning/Blitze. Weather/Lightning kann später separat kommen.

Light-Arten:
- Global Daylight.
- Sky Light.
- Block Light.
- Emissive Material.
- Fog Color.
- Bloom/Glow.
- Particle Glow.

## P1 – Bestehendes Lighting stabilisieren

- Blockupdates müssen Licht über Chunkgrenzen sauber invalidieren.
- Light seams an Chunkgrenzen testen.
- Nachbar-Chunks bei Light-Änderungen dirty markieren.
- Tests für Wasser, transparente Blöcke und Höhlen.
- Sky Light Tests für Höhlen, Überhänge, Chunkgrenzen, Wasser/Eis, transparente Blöcke und hohe Berge.

### Light Debug Overlay
- aktuelles Light Level am Zielblock.
- Sky Light.
- Block Light.
- Emissive Material Flag.
- optional Light Visualization Mode.

## P2 – Block Light System

Light Sources:
- active campfire.
- cozy lantern.
- glow mushroom.
- glow crystal node.
- glow lantern.
- firefly swarm visuell oder später schwaches dynamic light.

Daten:
- Light Value pro Block/Material.
- Block Light Array pro Chunk.
- Update Queue.
- boundary propagation zwischen Chunks.
- fallback full rebuild.

Propagation:
- Add Light.
- Remove Light.
- kleine incremental updates.
- voller Rebuild nur als Fallback.
- Performance messen.

## P3 – Material und Shader Lighting

### Emissive Materials
- emissive nicht mehr über hardcoded Block-IDs.
- Materialdaten aus `BlockRenderProperties` / Registry.
- Shader bekommt emissive flag/value.
- Glow/Bloom Toggle respektieren.

### Terrain Shader
- ✅ UV-Wrapping im Terrain-Shader (`faceUv`) repariert; doppelte/defekte Bedingung entfernt, damit Atlas-UVs stabil normalisiert werden.
- per-face brightness.
- ~~vertex AO.~~ ✅ (AO wird auf greedy-gemeshte Terrain-Flächen pro Vertex angewendet; AO-Sampling für große Merges nutzt jetzt die jeweilige Eck-Blockposition statt nur den Ursprung des Merges)
- ~~fog.~~ ✅ (bereits im `chunk.frag` aktiv; zusätzlich gegen fehlerhafte Fog-Range abgesichert)
- biome tint später.
- ~~day/night global brightness.~~ ✅ (als `uGlobalBrightness` aus Sky-Luminanz im Terrain-Shader verdrahtet)
- ~~block light contribution.~~ ✅ (Mesher kombiniert Sky/Block-Light gewichtet statt nur `max()`, damit lokale Lichtquellen trotz Tageshelligkeit sichtbar beitragen)

### Cutout / Vegetation
- Alpha Cutout bleibt scharf.
- Light/AO trotzdem lesbar.
- Glow mushrooms leicht emissive.

### Water
- ~~Wasser bekommt eigene Helligkeit/Tint.~~ ✅ (Terrain-Shader hebt `animatedFluid` nachts leicht an und gibt einen dezenten kühlen Tint, damit Wasser nicht absäuft)
- underwater tint.
- Nachtwasser nicht komplett schwarz.

## P4 – Day/Night und Atmosphäre

- Morning warm.
- Noon klar.
- Evening amber.
- Night blau/kühl.
- Smooth transitions.
- Fog Color abhängig von Tageszeit und später Biom.
- Fog kaschiert Render Distance.
- Campfire/Lantern sollen nachts sichtbar nützlich sein.
- Glow-Materialien machen Mushroom Grove besonders.

## P5 – Cave Darkness

- Sky Light in Höhlen korrekt reduzieren.
- Block Light sichtbar machen.
- Torch/Lantern/Campfire sinnvoll nutzen.
- Debug-Tests für Cave Light.

## P6 – Bloom / Glow Polish

Glow Sources:
- campfire.
- lantern.
- glow mushroom.
- glow crystal.
- fireflies.
- magic particles.

Regeln:
- Glow cozy und weich.
- nicht überstrahlen.
- Low-End Toggle beachten.
- Bloom-Intensität in Settings.

## P7 – Optional: Weather Lightning später

- Gewitter selten.
- Lightning als kurzer Sky Flash.
- kein Fokus im cozy Core.
- getrennt vom normalen Lighting-System halten.

## Tests

Unit:
- Light propagation add/remove.
- Chunk boundary propagation.
- light source values.
- transparent block behavior.

Manual:
- Campfire leuchtet nachts.
- Lantern leuchtet Base aus.
- Glow Mushroom ist sichtbar.
- Höhle ist dunkler.
- Chunkgrenzen haben keine Light Seams.
- Day/Night Übergang weich.

## Akzeptanzkriterien

- Campfire, Lantern und Glow Blocks wirken sichtbar.
- Nacht ist stimmungsvoll, aber spielbar.
- Höhlen sind dunkler.
- keine großen Frame-Spikes bei Light Updates.
- keine hardcoded Block-ID-Lichtlogik im Shader.
