# Adventura – Lighting TODO List

Stand: 2026-05-01

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

### Umsetzung

- `RenderDebugView` bietet jetzt `light`, `sky`, `block` und `emissive` als getrennte Shader-Debug-Modi.
- `ChunkMesher` schreibt Combined-, Sky- und Block-Light separat in das Chunk-Vertexformat; normales Terrain-Lighting nutzt weiterhin den Combined-Wert.
- `/debuglight` meldet neben Sky/Block/Combined auch Block-Key, Light-Source-Wert, Emissive-Stärke und Occlusion-Typ.
- Verifiziert am 2026-05-01 mit fokussierten Client-Tests für `ChunkMesherTest`, `LightDebugInfoTest` und `GameSettingsTest`.

### Akzeptanz

- Light-Seams lassen sich sichtbar prüfen.
- Campfire/Lantern/Glow-Block-Werte sind schnell debugbar.

---

## P0.2 Light-Seams an Chunkgrenzen testen

### Basis vorhanden

- Chunkgrenzen für Block-Light-Propagation sind teilweise getestet.
- Wasser/opaque Verhalten ist teilweise getestet.

### Umsetzung

- `LightEngineTest` deckt Chunkgrenzen jetzt mit hohen Berg-/Roof-Columns, Water/Ice/Leaves als transparente Blocker, nicht geladene Neighbor-Chunks, Boundary-Emitter auf beiden Seiten, Remove an der Grenze und korrigierende Full-Rebuilds gegen stale Light-Werte ab.
- Verifiziert am 2026-05-01 mit `:common:test --tests 'dev.voxelgame.common.world.light.LightEngineTest'`.

### Akzeptanz

- keine sichtbaren Light-Kanten an Chunkgrenzen.
- Add/Remove von Emittern bleibt konsistent.

---

# P1 – Sky Light

## P1.1 Sky-Light-Regeln definieren

### Umsetzung

- `LightRules` definiert zentrale Sky-Light-Reduktion und Occlusion-Typen: Opaque blockt vollständig, Water/Ice reduzieren leicht, Cutout bleibt offen, Leaves dämpfen stärker.
- `LightEngine` nutzt diese Regeln beim Sky-Light-Seeding und bei der Propagation, damit Wasser/Leaves nicht wie reine Luft wirken.
- `LightEngineTest` deckt offene Spalten, Roofs, Höhlen, Überhänge, Wasser, Cutout, Leaves und Chunkgrenzen ab.

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

### Umsetzung

- `CozyColorPipeline` definiert jetzt neben Sky-/Fog-Farben auch `globalBrightnessForMinute`, `fogDistanceScaleForMinute` und `nightLightBoostForMinute`.
- `RenderSettings` trägt Global-Brightness, Night-Light-Boost und Cave-Darkness explizit in den Renderpfad.
- `WorldRenderer` und Entity-Rendering nutzen die RenderSettings-Helligkeit statt ad hoc Sky-Luma-Schätzung.

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

### Umsetzung

- `LightSourceRegistry` sammelt Light-Emitter aus der Block-Registry und validiert Light-Werte.
- `LightEngine` seedet Block-Light über diese Registry statt direkt über verstreute Blockabfragen.
- Tests halten Registry-Werte und die Trennung von Material-Emissive und World-Light fest.

### Akzeptanz

- neue Light Blocks brauchen nur Registry-/Materialdaten.
- Shader hat keine hardcoded Light-IDs.

---

## P2.2 Incremental Block Light Updates

### Problem

Komplette Light Rebuilds sind teuer, wenn nur ein Campfire/Lantern geändert wird.

### Umsetzung

- `LightEngine.updateBlockLight(...)` führt Add-/Remove-Queues für Block-Light und liefert betroffene Chunks plus Fallback-Signal.
- `ClientWorld.applyBlock(...)` nutzt Incremental Block-Light, rebuildet Sky-Light nur bei geänderten Sky-Occlusion-Regeln und zeichnet Lighting-Zeit weiter in `ChunkBuildQueue` auf.
- Betroffene Light-Chunks werden zusätzlich dirty markiert; Geometrieänderungen behalten die bestehenden Neighbor-Invalidierungen.

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

### Umsetzung

- `chunk.frag` nutzt getrennte Sky-/Block-Light-Kanäle, Day/Night-Brightness und Night-Light-Boost.
- AO wird durch Block-Light und Emissive sichtbar entschärft, Glow wird nach Fog anteilig erneut addiert.
- Shader-Debug-Modi zeigen Combined, Sky, Block und Emissive getrennt.

### Akzeptanz

- Terrain hat Tiefe, aber bleibt pixel-art-lesbar.
- AO wirkt weich, nicht dreckig.
- Glow Blocks bleiben sichtbar.

---

## P3.2 Cutout / Vegetation Lighting

### Umsetzung

- Cutout-Layer erhält im Terrain-Shader einen kleinen Light-Lift, Foliage wird subtil abgedunkelt.
- Glow-Mushrooms/Spore-Blossoms bleiben über Material-Emissive sichtbar.
- Wind verschiebt nur Positionen, die separaten Light-Attribute bleiben stabil.

### Akzeptanz

- Pflanzen sind bei Tag gut lesbar.
- nachts nicht komplett schwarz, wenn Umgebung beleuchtet ist.

---

## P3.3 Water Lighting

### Umsetzung

- Water nutzt weiter Underwater-Tint und bekommt im Shader eine Mindesthelligkeit plus Block-Light-Anteil.
- Sky-Light-Regeln lassen Wasser leicht abdunkeln, Block-Light bleibt durch Wasser/Ice/Leaves propagierbar.
- Die Wasseroberfläche behält die günstige animierte Farb-/Wellenanhebung ohne Reflection/Refraction-Pass.

### Akzeptanz

- Lakeside bleibt nachts lesbar.
- Unterwasserzustand ist klar erkennbar.

---

# P4 – Cave Darkness

## Ziel

Höhlen und Ruinen sollen dunkler sein, damit Lanterns/Campfires/Glow Items Bedeutung haben.

### Umsetzung

- Terrain-Cave-Darkness wird im Shader aus niedrigem Sky-Light und `RenderSettings.caveDarkness` abgeleitet.
- Block-Light und Emissive reduzieren die Abdunklung, damit Lanterns/Campfires in Höhlen wichtig und lesbar bleiben.
- Ruins-Biome-Fog/Tint läuft über die bestehende `CozyColorPipeline.biomeTint("ruins")`-Kopplung.
- Cave-Smoke nutzt den vorhandenen Debug-Seed aus `docs/WORLD_SMOKE_TESTS.md`: Seed `1337`, Position `-56 68 -504`.

### Verifikation 2026-05-01

- `:common:test --tests dev.voxelgame.common.world.light.LightEngineTest` ist grün.
- Fokussierte Client-Tests für `CozyColorPipelineTest`, `WorldRendererTest` und `TerrainLightingShaderContractTest` sind grün.

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

## Umsetzung

- Bloom hat einen Low-End-Schalter über `/bloom` und Render-Presets; das Low-Preset deaktiviert Bloom.
- `RenderSettings` trägt Bloom-Stärke und Emissive-Bloom-Threshold als clampbare Render-Verträge.
- `chunk.frag` nutzt den Threshold für Glow-Beiträge und begrenzt die additive Bloom-Komponente, damit Lanterns, Campfires und Glow-Mushrooms nicht überstrahlen.
- Firefly-/Mire-Wisp-Partikel bleiben über den Particle-Shader weich additiv.
- Der Mushroom-Grove-Showcase ist als reproduzierbarer Smoke in `docs/WORLD_SMOKE_TESTS.md` verankert.

## Verifikation 2026-05-01

- `TerrainLightingShaderContractTest` prüft Bloom-Threshold, Glow-Limit und weiche Particle-Glow-Regeln.
- `WorldRendererTest` hält die Default-Bloom-Stärke und den Threshold fest.

## Akzeptanz

- Glow ist warm und weich.
- Pixel-Art wird nicht matschig.
- Bloom kann deaktiviert werden.

---

# P6 – Optional Weather Lightning

## Ziel

Weather-Lightning bleibt bewusst getrennt vom normalen Lighting-System: keine Block-Light-Updates, keine Gameplay-Pflicht und kein Einfluss auf Light-Seams.

## Umsetzung

- `WeatherLightningController` steuert seltene, deterministische Gewitter-Blitze abhängig von Seed, Tageszeit und aktuellem Biome.
- Storm-Eligibility ist aktuell auf Abend/Nacht und feuchte bzw. wetterige Biome begrenzt: Mire, Lakeside, Highlands und sehr feuchte Biomes.
- `RenderSettings.weatherFlash` trägt den kurzen Flash als eigenen Render-Vertrag.
- `WorldRenderer`, `EntityRenderer`, `chunk.frag` und `entity.frag` nutzen `uWeatherFlash` getrennt von Sky-Light, Block-Light, Bloom und Entity-Light.
- Sky-/Fog-Farbe werden kurz in Richtung kühles Blitzlicht gezogen; Unterwasser wird der Flash deutlich gedämpft.
- `AudioCue.THUNDER` ist als verzögerter Sound-Hook angebunden, ohne echte Sound-Engine-Abhängigkeit.
- `/lightning` triggert einen manuellen Test-Blitz für Smoke-Tests.

## Verifikation 2026-05-01

- `WeatherLightningControllerTest` prüft kurzen Flash, einmaligen verzögerten Thunder-Cue, Storm-Eligibility und Flash-Farbmischung.
- `TerrainLightingShaderContractTest` prüft, dass Terrain- und Entity-Shader `uWeatherFlash` separat führen.
- `WorldRendererTest` hält den Default `weatherFlash = 0.0` fest.
- `RenderingSmokeCoverageTest` hält `/lightning` und die Weather-Lightning-Smoke-Erwartung in `WORLD_SMOKE_TESTS.md` fest.
- `RENDERING_SHADER_UNIFORMS.md` dokumentiert `uWeatherFlash` für Terrain- und Entity-Shader.

## Akzeptanz

- Gewitter sind selten und atmosphärisch.
- kurzer Sky/Fog/Shader-Flash ist sichtbar, aber nicht gameplay-blockierend.
- Donner ist als Hook vorhanden und zeitlich nach dem Flash gekoppelt.
- normales Lighting-System bleibt unangetastet.
- kein roter Kreis offen: Engine und Tooling reichen für diese Stufe aus.

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
