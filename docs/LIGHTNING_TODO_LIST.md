# Adventura – Lighting TODO List

Stand: 2026-04-30

## Ziel

Lighting soll Adventura cozy, weich und technisch stabil machen. Diese Liste behandelt **Lighting/Beleuchtung**, nicht Lightning/Blitze. Weather-Lightning kann später als eigenes Feature kommen.

Adventura braucht:

- warme Tagesfarben
- lesbare Nacht
- sichtbare Campfires/Lanterns/Glow Blocks
- dunklere Höhlen
- keine harten Light-Seams an Chunkgrenzen
- keine großen Performance-Spikes bei Light Updates

## Light-Arten

- Global Daylight
- Sky Light
- Block Light
- Emissive Material
- Fog Color
- Bloom/Glow
- Particle Glow
- optional später Colored Light

---

# P0 – Lighting Debug und Stabilität

## P0.1 Light Debug View

### Basis vorhanden

- `/debuglight` zeigt Sky Light, Block Light, Combined Light und Emissive-Wert an.
- Light-Debug-Formatierung ist testbar.

### Offen

- Light Visualization Mode als Overlay oder blockweise Farbdarstellung.
- Toggle für Sky Light only.
- Toggle für Block Light only.
- Toggle für Emissive only.
- Debug Anzeige am anvisierten Block:
  - block key
  - sky light
  - block light
  - combined
  - light source value
  - occlusion type

### Akzeptanz

- Light-Seams lassen sich sichtbar prüfen.
- Campfire/Lantern/Glow-Block-Werte sind schnell debugbar.

---

## P0.2 Light-Seams an Chunkgrenzen testen

### Basis vorhanden

- Chunkgrenzen für Block-Light-Propagation sind teilweise getestet.
- Wasser/opaque Verhalten ist teilweise getestet.

### Offen

- hohe Berge testen.
- transparente Blöcke testen.
- unloaded neighbor behavior testen.
- Light Source direkt an Chunkgrenze testen.
- Light Remove direkt an Chunkgrenze testen.
- Full Rebuild Fallback testen.

### Akzeptanz

- keine sichtbaren Light-Kanten an Chunkgrenzen.
- Add/Remove von Emittern bleibt konsistent.

---

# P1 – Sky Light

## P1.1 Sky-Light-Regeln definieren

### Offen

- Opaque Blocks blocken Sky Light.
- Water/Glass/Cutout behandeln:
  - Water schwächt Sky Light optional.
  - Cutout blockt nicht vollständig.
  - Leaves können leicht abdunkeln.
- Höhlen und Überhänge abdunkeln.
- Chunkgrenzen konsistent.

### Tests

- offene Fläche hat helles Sky Light.
- Höhle ist dunkel.
- Überhang reduziert Licht.
- Wasser verhält sich erwartbar.
- Chunkgrenze zeigt keinen Sprung.

### Akzeptanz

- Tageslicht wirkt stabil.
- Höhlen sind sichtbar dunkler.
- keine starken Seams.

---

## P1.2 Day/Night Global Light

### Ziel

Tageszeit soll stimmungsvoll sein, ohne Gameplay zu blockieren.

### Phasen

- Morning: warm und weich.
- Noon: klar und hell.
- Evening: amber/orange.
- Night: blau/kühl, aber spielbar.

### Offen

- Farbkurven definieren.
- Sky/Fog/Fog Distance koppeln.
- global brightness smooth interpolieren.
- Night minimum brightness definieren.
- Campfire/Lantern nachts stärker lesbar machen.

### Akzeptanz

- Übergänge sind weich.
- Nacht ist atmosphärisch, aber nicht frustrierend.
- Base-Licht fühlt sich nützlich an.

---

# P2 – Block Light

## P2.1 Light Source Registry

### Light Sources

- active campfire
- cozy lantern
- glow mushroom
- glow crystal node
- glow lantern
- ancient lantern
- firefly swarm optional dynamisch/visuell

### Offen

- Light Value pro Block/Material zentral definieren.
- Validation: Light Source Blocks existieren.
- Light Value 0..15 prüfen.
- Emissive und Light Value getrennt halten:
  - emissive = sieht selbst leuchtend aus
  - lightValue = beleuchtet Umgebung

### Akzeptanz

- neue Light Blocks brauchen nur Registry-/Materialdaten.
- Shader hat keine hardcoded Light-IDs.

---

## P2.2 Incremental Block Light Updates

### Problem

Komplette Light Rebuilds sind teuer, wenn nur ein Campfire/Lantern geändert wird.

### Offen

- Update Queue für Light Add.
- Update Queue für Light Remove.
- Propagation über Chunkgrenzen.
- Fallback Full Rebuild.
- Dirty Meshes nur für betroffene Bereiche.
- Performance-Messung pro Light Update.

### Akzeptanz

- Campfire an/aus verursacht keinen großen Spike.
- Lantern platzieren aktualisiert Nachbarschaft.
- Light Remove hinterlässt keine Geisterlichter.

---

# P3 – Shader Lighting

## P3.1 Terrain Lighting

### Muss können

- face-direction brightness
- sky light
- block light
- vertex AO
- global day/night factor
- fog color
- emissive add

### Offen

- Lightwerte sauber normalisieren.
- Ambient Occlusion mit block light kompatibel machen.
- emissive nicht vom Fog komplett verschlucken.
- optional smooth light interpolation.
- debug mode: show light values.

### Akzeptanz

- Terrain hat Tiefe, aber bleibt pixel-art-lesbar.
- AO wirkt weich, nicht dreckig.
- Glow Blocks bleiben sichtbar.

---

## P3.2 Cutout / Vegetation Lighting

### Offen

- Pflanzen hell genug halten.
- Leaves/Pine Needles leicht abdunkeln.
- Glow mushrooms emissive.
- Wind/Animation darf Light nicht kaputt machen.

### Akzeptanz

- Pflanzen sind bei Tag gut lesbar.
- nachts nicht komplett schwarz, wenn Umgebung beleuchtet ist.

---

## P3.3 Water Lighting

### Offen

- underwater tint.
- Wasser nachts nicht komplett schwarz.
- Shallow Water heller als Deep Water optional.
- Block Light durch/auf Wasser prüfen.
- Wasseroberfläche leicht glänzend, aber nicht realistisch/teuer.

### Akzeptanz

- Lakeside bleibt nachts lesbar.
- Unterwasserzustand ist klar erkennbar.

---

# P4 – Cave Darkness

## Ziel

Höhlen und Ruinen sollen dunkler sein, damit Lanterns/Campfires/Glow Items Bedeutung haben.

### Offen

- Cave Darkness aus Sky Light ableiten.
- Mindesthelligkeit definieren.
- Campfire/Lantern/Held-Item-Licht optional.
- Old Ruins leicht dunkler/foggy machen.
- Debug-Seeds für Cave Pocket nutzen.

### Akzeptanz

- Höhlen fühlen sich anders an als Oberfläche.
- Spieler versteht, warum Lichtquellen nützlich sind.
- Darkness ist cozy-adventurous, nicht unfair.

---

# P5 – Bloom / Glow

## Glow Sources

- campfire
- lantern
- glow mushroom
- glow crystal
- ancient lantern
- fireflies
- magic particles

## Offen

- Bloom Intensity Setting.
- Low-End Toggle.
- emissive threshold definieren.
- Glow nicht überstrahlen lassen.
- Fireflies eher soft additive.
- Glow Mushroom Grove als visueller Showcase.

## Akzeptanz

- Glow ist warm und weich.
- Pixel-Art wird nicht matschig.
- Bloom kann deaktiviert werden.

---

# P6 – Optional Weather Lightning später

Nicht im Core priorisieren.

Falls später:

- seltene Gewitter.
- kurzer Sky Flash.
- Sound Hook.
- kein Gameplay-Zwang.
- getrennt von normalem Lighting-System.

---

# Tests

## Unit Tests

- Light source registry values.
- Sky Light open column.
- Sky Light under overhang.
- Water/transparent behavior.
- Block Light add.
- Block Light remove.
- Chunk boundary propagation.
- Full rebuild fallback.

## Manual Smoke Tests

- Campfire leuchtet nachts.
- Lantern leuchtet Base aus.
- Glow Mushroom sichtbar im Grove.
- Höhle dunkler als Oberfläche.
- Chunkgrenzen ohne Light Seams.
- Day/Night Übergang weich.
- Bloom off/on vergleichbar.

## Akzeptanz

- Campfire, Lantern und Glow Blocks wirken sichtbar.
- Nacht ist stimmungsvoll und spielbar.
- Höhlen sind dunkler.
- Light Updates erzeugen keine großen Spikes.
- keine hardcoded Block-ID-Lichtlogik im Shader.