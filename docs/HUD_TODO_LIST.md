# Adventura – HUD TODO List

Stand: 2026-04-30

## Ziel

Das HUD soll die wichtigsten Survival-, Comfort-, Welt- und Feedback-Informationen klar, cozy und unaufdringlich anzeigen. Es soll beim Spielen helfen, aber nicht den Bildschirm dominieren.

## Leitlinien

- UI Scale muss überall greifen.
- HUD muss bei 1x, 1.5x und 2x lesbar bleiben.
- Normal HUD, Minimal HUD, Debug HUD und Hidden HUD klar trennen.
- Survival-Werte sofort verständlich.
- Debug-Informationen nur im Debug-Modus.
- Feedback kurz, stapelbar und nicht spammy.

---

# P0 – HUD-Struktur

## HUD-Modi

- Normal HUD
- Minimal HUD
- Debug HUD
- Hidden HUD
- Death HUD
- Underwater HUD
- Interaction Focus HUD optional

## Akzeptanz

- HUD ist nicht verstreut im Code aufgebaut.
- alle Modi sind sauber schaltbar.
- UI Scale bricht keine Positionen.

---

# P1 – Survival HUD

## Akzeptanz

- Spieler erkennt sofort kritische Werte.
- Warnungen sind klar, aber nicht nervig.

---

# P2 – Comfort HUD

## Ziel

Comfort soll sichtbar und verständlich sein, ohne wie ein Pflicht-Meter zu wirken.

## Comfort Levels

- Low
- Cozy
- Warm
- Restful
- Homey

## Akzeptanz

- Spieler versteht, warum Comfort steigt.
- Comfort bleibt angenehm, nicht dominant.

---

# P3 – Weltinformationen

## Offen

- Wetter später optional.

## Akzeptanz

- Spieler erkennt Biome-Wechsel.
- Tag/Nacht ist verständlich.
- unnötige Werte werden nicht dauerhaft angezeigt.

---

# P4 – Interaction HUD

## Basis vorhanden

- anvisierte Blöcke/Entities zeigen UI-skalierten Interaction-Hint.
- Zielname, Primäraktion, Tool-/Tier-Hinweis, Mining-Fortschritt und Storage Range Hint sind begonnen.
- Campfire-HUD zeigt Status/Burn/Cook-Informationen.
- Cooking Pot Ready-Hinweis ist begonnen.

## Offen

### Forge

- heat/burn progress.
- output ready.

### Storage

- locked/unreachable state.

### Entity

- required food.
- too far.
- friendly/flee state optional.

## Akzeptanz

- Spieler weiß immer, warum eine Interaktion geht oder nicht geht.
- Stationen sind ohne Wiki bedienbar.
- HUD bleibt kompakt.

---

# P5 – Feedback Log

## Kategorien

- Pickup
- Craft Success
- Craft Fail
- Recipe Unlock
- Too Far Away
- Need Tool
- Inventory Full
- Comfort Level
- Biome Discovered
- Lore Found
- Station Missing
- Fuel Missing
- Save Complete optional

## Regeln

- kurze Meldungen.
- gleiche Meldungen zusammenfassen.
- wichtige Meldungen länger sichtbar.
- unwichtige Meldungen schneller ausblenden.
- keine Chat-Spam-Optik.
- UI Scale berücksichtigen.
- optional Sound Hook pro Kategorie.

## Akzeptanz

- Spieler bekommt klares Feedback.
- wiederholte Fehlversuche spammen nicht.

---

# P6 – Debug HUD

## Basis vorhanden

- Packets/sec sind sichtbar.
- FPS und einige Engine-Stats sind vorhanden.

## Engine Stats

- FPS
- Frame ms
- Update ms
- Render ms
- Chunkgen ms
- Meshing ms
- Lighting ms
- Upload ms
- Draw Calls
- Triangles
- VRAM estimate
- Loaded/Visible/Dirty Chunks
- Build Queue
- Entity Count
- Particle Count

## Gameplay Stats

- Position
- Chunk Coordinate
- Current Biome
- Light Level
- Looking at Block
- Selected Item
- Gamemode
- raw survival values
- Comfort raw value

## Network Stats

- Ping
- packets/sec TX/RX
- entity snapshot count
- chunk packet count
- packet reject count optional

## Offen

- Debug HUD strukturierter gruppieren.
- nicht alle Werte auf einmal anzeigen, Tabs/Pages optional.
- Debug Hotkeys dokumentieren.

## Akzeptanz

- Debug HUD hilft Entwicklern wirklich.
- normale Spieler können es ignorieren/ausblenden.

---

# Tests / Smoke Checks

- HUD bei UI Scale 1x, 1.5x, 2x.
- Minimal HUD.
- Hidden HUD.
- Debug HUD.
- Underwater HUD.
- Death HUD.
- Campfire anvisieren.
- Cooking Pot anvisieren.
- Storage anvisieren.
- Entity feed/observe.
- invalid tool.
- inventory full.
- repeated feedback spam.

## Akzeptanz gesamt

- Survival-Werte sind sofort verständlich.
- Comfort ist sichtbar, aber nicht aufdringlich.
- Interaction HUD erklärt Aktionen.
- Debug HUD hilft beim Entwickeln.
- Feedback-Meldungen spammen nicht.

---

# P7 – HUD Architecture und Telemetry Contracts

Owner: Lead UI/UX Frontend Developer, mit Schnittstellen zu Lead Engine Developer und Main Networking Dev.

## P7.1 HUD aus GameClient herausziehen

### Offen

- 🟠 HUD in eigene Renderer/Presenter aufteilen:
  - SurvivalHud.
  - ComfortHud.
  - InteractionHud.
  - FeedbackHud.
  - DebugHud.
  - ChatHud.
  - HotbarHud.
- ~~🟠 `HudLayout` als stabile Layout-API behalten und erweitern.~~
  Erledigt: 2026-05-01 - `HudLayout` aus `GameClient` nach `client.hud` extrahiert und als testbare API angebunden.
  Verifikation: `./gradlew :client:test --tests dev.voxelgame.client.hud.HudLayoutTest --tests dev.voxelgame.client.GameClientUiLayoutTest`.
- 🟠 HUD-Daten als ViewModels statt direkte Welt-/Client-Zugriffe übergeben.
- 🟡 HUD-Animationen über gemeinsame Animation-Presets laufen lassen.

### Akzeptanz

- 🟠 Neue HUD-Elemente vergrößern `GameClient` nicht weiter.
- ~~🟠 Layout-Tests prüfen kleine Fenster und UI-Scale-Fälle.~~
  Erledigt: 2026-05-01 - HUD-Layout-Tests liegen in `client/src/test/java/dev/voxelgame/client/hud/HudLayoutTest.java`.
  Verifikation: `./gradlew :client:test --tests dev.voxelgame.client.hud.HudLayoutTest --tests dev.voxelgame.client.GameClientUiLayoutTest`.
- 🟠 HUD kann ohne WorldRenderer- oder Netty-Abhängigkeit getestet werden.

## P7.2 Debug-HUD als Engine Diagnostics Surface

### Offen

- Debug-HUD in Kategorien gliedern:
  - Frame.
  - Render.
  - Chunks.
  - Lighting.
  - Physics.
  - Networking.
  - Save.
  - Entities.
  - UI.
- Mehrzeilige Diagnose nicht über Spiel-HUD quetschen; optional eigener Diagnostics Screen.
- ServerStatsSnapshot und lokale EngineFrameStats klar markieren.
- Budgets mit Prozentwerten anzeigen.

### Akzeptanz

- Entwickler sehen sofort, welcher Bereich ein Problem verursacht.
- Normales HUD bleibt ruhig.
- Project Manager kann Regressionen mit Labels aus `ENGINE_TODO_LIST.md` zuordnen.

## P7.3 Gameplay Feedback Contracts

### Status 2026-05-01

- ~~🟠 Common-Event-Modell vorhanden: `GameplayEvent`, `GameplayEventType` und `GameplayEventBatch`; HUD-Consumer und Packet-Anbindung bleiben offen.~~
  Erledigt: 2026-05-01, `GamePacket.GameplayEvents` und erster Client-Consumer-Hook sind vorhanden; tiefe HUD-Marker/Journal-Updates bleiben offen.
  Verifikation: `GameplayEventTest`, `PacketCodecTest`, `GameplayEventFeedbackTest`.

### Offen

- HUD hört langfristig auf `GameplayEventStream`:
  - ~~pickup.~~
  - ~~craft.~~
  - ~~cook.~~
  - ~~damage.~~
  - ~~heal.~~
  - status effect.
  - ~~recipe unlock.~~
  - journal entry.
- Prioritäten definieren, damit Feedback nicht spammt.
- Accessibility/Low Motion für HUD-Popups berücksichtigen.

### Akzeptanz

- Wichtige Events sind sichtbar und serverbestätigt.
- HUD-Feedback ist konsistent mit Audio und Partikeln.
