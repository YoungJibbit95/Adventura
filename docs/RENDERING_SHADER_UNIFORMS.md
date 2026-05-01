# Rendering Shader Uniforms

## Terrain Shader

Dateien:

- `client/src/main/resources/assets/voxelgame/shaders/chunk.vert`
- `client/src/main/resources/assets/voxelgame/shaders/chunk.frag`

### Matrices und Kamera

- `uProjection`: Perspektivmatrix.
- `uView`: Kameramatrix.
- `uCameraPosition`: Weltposition der Kamera fuer Distanz/Fog/Sorting-Debug.

### Licht und Zeit

- `uSunDirection`: normalisierte Hauptlichtrichtung fuer per-face diffuse shading.
- `uAmbientOcclusionEnabled`: schaltet Vertex-AO ein/aus.
- `uSoftShadowsEnabled`: schaltet weiche Terrain-Schattierung ein/aus.
- `uShadowStrength`: Staerke der subtilen Shadow-Noise.
- `uGlobalBrightness`: Tageszeit-Helligkeit nach Sky-Luma.
- `uTime`: Sekundenzeit fuer Wasserwellen und Cutout-Wind.

### Fog, Wasser, Debug

- `uFogEnabled`: globaler Fog-Schalter.
- `uFogStart`: Distanz, ab der Fog weich beginnt.
- `uFogEnd`: Distanz, bei der Fog voll anliegt.
- `uFogColor`: Tageszeit-/Biome-/Underwater-Fogfarbe.
- `uUnderwater`: aktiviert Unterwasser-Tint und kurze Fog-Reichweite.
- `uSimpleWater`: reduziert Wasserwellen/UV-Animation fuer Low-End.
- `uWindEnabled`: erlaubt materialgesteuerte Cutout-Bewegung.
- `uRenderDebugMode`: `0=off`, `1=material`, `2=light`, `3=ao`, `4=biome`, `5=layer`, `6=uv`, `7=transparent`.

### Texturen

- `uBlockAtlas`: Block-Atlas, Tile-UVs kommen aus der Material-LUT.
- `uAtlasEnabled`: Fallback-Schalter, wenn der Atlas deaktiviert ist.
- `uMaterialLut`: `RGBA32F` Lookup-Tabelle, Breite = Materialanzahl, Hoehe = 6.
- `uMaterialCount`: Anzahl gueltiger Materialslots.
- `uBiomeTintColor`: Runtime-Biome-Farbe der Kamera-Region.

## Material-LUT

Zeile pro Material:

- `0 color`: `rgb=tint`, `a=material alpha`
- `1 effects`: `x=emissive`, `y=animated fluid`, `z=flags`, `w=cutout threshold`
- `2 side uv`: `u0,v0,u1,v1`
- `3 top uv`: `u0,v0,u1,v1`
- `4 bottom uv`: `u0,v0,u1,v1`
- `5 style`: `x=biome tint mode`, `y=fog affect mode`, `z=render layer`, `w=roughness`

Flags:

- `1` translucent
- `2` emissive
- `4` animated fluid
- `8` fill texture gaps
- `16` solid layer
- `32` cutout layer
- `64` translucent layer
- `128` missing material data

## Entity Shader

Dateien:

- `client/src/main/resources/assets/voxelgame/shaders/entity.vert`
- `client/src/main/resources/assets/voxelgame/shaders/entity.frag`

Uniforms:

- `uProjection`, `uView`, `uModel`: Standard-Matrizen.
- `uCameraPosition`: Entity-Distanz fuer Fog.
- `uBaseColor`: Entity-Part-Farbe aus dem Entity-Modell.
- `uEmissive`: Emissive-Mix pro Part.
- `uEntityLight`: Sky/Block-Light-Sample an der Entity-Position.
- `uLightDirection`: diffuse Hauptlichtrichtung.
- `uGlobalBrightness`: Tageszeit-Helligkeit.
- `uFogEnabled`, `uFogStart`, `uFogEnd`, `uFogColor`: Terrain-kompatibler Fog.
- `uUnderwater`: Unterwasser-Tint.

## Particle Shader

Dateien:

- `client/src/main/resources/assets/voxelgame/shaders/particle.vert`
- `client/src/main/resources/assets/voxelgame/shaders/particle.frag`

Uniforms:

- `uProjection`: Perspektivmatrix.
- `uView`: Kameramatrix.

Partikel tragen aktuell Farbe/Alpha direkt in Vertexdaten. Die Runtime-Qualitaet begrenzt die Live-Partikelkapazitaet, ohne die VBO-Groesse pro Preset neu anzulegen.
