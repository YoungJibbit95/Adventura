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

## Offen

- HUD Layout zentralisieren.
- gemeinsame Stat-Strip-Komponente für Health/Hunger/Stamina/Breath/Comfort.
- gleiche Abstände, gleiche Skalierung, gleiche Icon-Logik.
- HUD-Elemente mit UI Scale testen.
- Elemente bei kleinen Fenstern priorisieren.
- Minimal HUD für Screenshots/ruhiges Spielen.

## Akzeptanz

- HUD ist nicht verstreut im Code aufgebaut.
- alle Modi sind sauber schaltbar.
- UI Scale bricht keine Positionen.

---

# P1 – Survival HUD

## Health

### Offen

- volle/halbe/leere Herzen.
- Damage Flash kurz animieren.
- Regeneration subtil anzeigen.
- Death State klar anzeigen.
- Respawn-Hinweis verständlich.

## Hunger

### Offen

- volle/halbe/leere Beeren- oder Food-Icons.
- Food Tooltip zeigt erwartete Wirkung.
- niedriger Hunger dezent warnen.
- Comfort-Effekt optional im Tooltip erklären.

## Stamina / Energy

### Offen

- klar als Sprint-/Action-Ressource darstellen.
- niedrigen Wert kurz pulsen lassen.
- Comfort-Boost subtil markieren.
- Sprint disabled state klar machen.

## Breath

### Offen

- nur prominent zeigen, wenn relevant.
- volle/halbe/leere Luftblasen.
- Warnung kurz vor Ertrinken.
- Underwater Overlay mit Rendering abstimmen.

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

## Offen

- cozy Meter mit Icon.
- Tooltip erklärt aktuellen Level.
- optional Quellen anzeigen:
  - Campfire +5
  - Rug +3
  - Lantern +3
  - Sleeping Mat +4
  - Friendly Animal +1
- bei neuem Comfort-Level einmalige Meldung.
- Comfort nicht im Debug-Style anzeigen, sondern warm/cozy.

## Akzeptanz

- Spieler versteht, warum Comfort steigt.
- Comfort bleibt angenehm, nicht dominant.

---

# P3 – Weltinformationen

## Offen

- Current Biome schöner darstellen.
- bei Biome-Wechsel kurze Meldung.
- Day/Time mit Icon statt nur Text.
- Day Number anzeigen.
- Tageszeit labeln:
  - Morning
  - Noon
  - Evening
  - Night
- Temperature nur anzeigen, wenn Temperature-System aktiv/relevant ist.
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

### Block Interaction

- Blockname beim Anvisieren.
- benötigtes Tool anzeigen.
- required tool level anzeigen.
- mining progress am Block und optional im HUD.
- invalid tool direkt erklären.
- valid/invalid placement hint.

### Campfire

- fuel value des gehaltenen Items.
- active/inactive klar anzeigen.
- burn time.
- current recipe.
- cook progress.
- output ready.
- no fuel / no recipe / inventory full Feedback.

### Cooking Pot

- input needed.
- water/container needed.
- ready recipes count.
- output preview.
- press key hint.

### Forge

- ore input needed.
- fuel input needed.
- heat/burn progress.
- output ready.
- required station line.

### Storage

- Open Hint.
- Range Hint.
- locked/unreachable state.

### Entity

- observe.
- feed.
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

## Offen

- Prioritätssystem.
- stacking gleicher Meldungen.
- cooldown gegen Spam.
- „New recipe unlocked“ hervorheben.
- Lore/Journal Unlock besonders anzeigen.

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