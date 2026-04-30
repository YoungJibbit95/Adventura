# Adventura – UI TODO List

Stand: 2026-04-30

## Ziel

Die UI soll cozy, klar, pixel-art-kompatibel und mausfreundlich werden. Sie soll Inventory, Crafting, Cooking, Forge, Storage, Journal, Recipe Unlocks und Settings sauber unterstützen.

## Leitlinien

- UI Scale überall anwenden.
- einheitliche Komponenten statt viele Sonderzeichnungen.
- Pixel-Art-kompatible Rahmen und Icons.
- klare Hover/Pressed/Disabled States.
- serverkritische Aktionen nie nur clientseitig entscheiden.
- UI darf keine Dupes ermöglichen.

---

# P0 – UI-Grundqualität

## Basis vorhanden

- Compact Inventory skaliert teilweise konsistent.
- Inventory Panel wurde neu aufgebaut.
- Slots, Header, Buttons, Hover und Durability Bar sind verbessert.

## Offen

- zentrale Komponenten definieren:
  - Button
  - Panel
  - Slot
  - Tab
  - Chip
  - Scrollbar
  - Tooltip
  - Progress Bar
  - Text Field
  - Modal/Dialog
- Hover/Pressed/Disabled States vereinheitlichen.
- UI Assets sauber mappen.
- UI Scale für alle Screens testen.
- Pixel-Perfect Regeln dokumentieren.
- Input handling zentraler machen.

## Akzeptanz

- Inventory, Crafting, Storage, Journal und Settings wirken aus einem Stil.
- UI Scale funktioniert überall.
- Komponenten sind wiederverwendbar.

---

# P1 – Inventory UI

## Basis vorhanden

- 3x9 Backpack + getrennte Hotbar.
- Tool-Hover vergleicht Level, Haltbarkeit, Speed und Repair Materials.
- Drag/drop und Shift-click existieren/werden getestet.

## Offen

- Drag/drop weiter testen.
- Shift-click quick move weiter testen.
- right-click split stack edge cases.
- stack merge/swap edge cases.
- inventory full feedback.
- creative trash klar von survival trennen.
- sort button Animation/Feedback.
- item compare tooltip optional.

## Akzeptanz

- keine Item-Dupes durch UI-Gesten.
- alle Slot-Aktionen sind verständlich.
- Tooltips helfen bei Tools/Food/Blocks.

---

# P2 – Crafting UI

## Offen

- Recipe Book verbessern.
- Unlock-Historie anzeigen.
- locked recipes klar erklären.
- Rezept-Suche weiter verbessern.
- Filter speichern.
- fehlende Station prominent anzeigen.
- fehlende Zutaten gruppiert anzeigen.
- craft count controls:
  - craft 1
  - craft 5
  - craft max
- Rezeptdetails anzeigen:
  - Name
  - Output
  - Zutaten
  - Station
  - Kategorie
  - Unlock Source
  - Crafting Time
  - Beschreibung

## Akzeptanz

- Spieler versteht, warum ein Rezept locked ist.
- Spieler sieht sofort, was fehlt.
- Crafting bleibt server-validiert.

---

# P3 – Cooking UI

## P3.1 Campfire UI

### Basis vorhanden

- `CampfireStatus` Snapshots.
- Input/Fuel/Output/Burn/Cook Status wird angezeigt.
- Active/Inactive-Hinweis existiert.

### Offen

- Input Slot final.
- Fuel Slot final.
- Output Slot final.
- Burn-Time-Anzeige polishen.
- Cook-Time-Anzeige polishen.
- Fehlerfeedback:
  - no fuel
  - no matching recipe
  - inventory full
  - campfire inactive
- output-ready Feedback.

### Akzeptanz

- Campfire ist ohne Crafting-Screen-Verwirrung bedienbar.
- Spieler sieht Fuel und Cook Progress.

---

## P3.2 Cooking Pot UI

### Basis vorhanden

- Cooking-Pot-Nähe priorisiert Pot-Rezepte.
- Zutaten-/Output-/Cook-Time-Preview existiert.
- fehlendes Wasser/Bowl/Zutaten/Inventar wird erklärt.
- HUD zeigt Pot-Ready.

### Offen

- eigene Cooking-Pot-Screen-Variante.
- mehrere Rezeptoptionen.
- Wasser-/Container-Anforderung anzeigen.
- bessere Food Outputs.
- Fortschrittsbalken.
- Pot-Inventory/Slots falls nötig.
- Server-Snapshot statt nur lokaler Preview.

### Akzeptanz

- Cooking Pot fühlt sich wie eigene Station an.
- bessere Foods sind klar station-gated.

---

## P3.3 Forge UI

### Offen

- Ore Input.
- Fuel Input.
- Output.
- Heat/Burn Progress.
- Recipe Requirements.
- Iron/Copper smelting klar anzeigen.
- insufficient heat/fuel feedback.
- output blocked feedback.

### Akzeptanz

- Forge macht Metal Progression verständlich.
- Server validiert Input/Fuel/Output.

---

# P4 – Storage UI

## Basis vorhanden

- Shift-click zwischen Crate und Backpack.
- Drag/drop zwischen Crate-Slots und Spieler-Slots.
- Online nutzt Zielslot-Transfers.
- lokale Crates können sortiert werden.

## Offen

- größere Chests.
- Market Crates.
- Loot Crates.
- Storage type header.
- locked loot crate state.
- transfer transaction feedback.
- failed transfer explanation.
- sorting animation optional.

## Akzeptanz

- Storage UI ist sicher und verständlich.
- online keine Dupes.
- Loot Crates unterscheiden sich klar von Player Storage.

---

# P5 – Journal UI

## Basis vorhanden

- Journal per `J`.
- Tabs für Notes, Biomes, Creatures, Recipes und Items.
- Structures-Seite für Storage, Campfire, Cooking Pot und Sleeping Mat.

## Offen

- neue Einträge markieren.
- gefundene Lore lesen.
- Biome mit kurzem Text.
- Creature Infos.
- Recipe Unlock Source.
- Progress-Anzeige.
- Collectibles-Seite.
- Map Fragment Hinweise.
- Search/Filter optional.
- Journal Progress persistent speichern.

## Seiten

- Notes
- Biomes
- Structures
- Creatures
- Recipes
- Items
- Collectibles
- Map Fragments optional

## Akzeptanz

- Journal motiviert Exploration.
- gefundene Informationen bleiben gespeichert.
- UI bleibt kompakt und lesbar.

---

# P6 – Settings UI

## Bestehend / erweitern

- Render Distance
- Preview Radius
- FOV
- Mouse Sensitivity
- Water
- AO
- Shadows
- Bloom/Glow
- VSync
- UI Scale
- Controls Overview

## Neu

- Master Volume
- Music Volume
- Ambience Volume
- SFX Volume
- UI Volume
- Particle Quality
- Graphics Presets:
  - Low
  - Medium
  - High
- Keybind Screen
- Graphics Advanced:
  - Water Quality
  - Particle Density
  - Lighting Quality
  - Bloom Strength
  - Fog Distance
- Worldgen Settings nur für neue Welten:
  - seed
  - preview radius
  - world type optional

## Akzeptanz

- Settings sind nicht überladen.
- wichtige Grafikoptionen helfen Low-End-PCs.
- Keybinds sind auffindbar.

---

# P7 – Menüs

## Main Menu

### Offen

- Singleplayer.
- Join Server.
- Settings.
- Quit.
- last world shortcut optional.
- seed field klar.
- connection error state.

## Pause Menu

### Basis vorhanden

- Journal-Einstieg aus Pause-Menü.
- Rückkehrpfad ist korrigiert.

### Offen

- Resume.
- Settings.
- Save/Exit.
- Return to Main Menu.
- Debug options nur optional.
- confirm dialogs bei Exit ohne Save.

## Akzeptanz

- Menüs sind klar und sicher.
- Exit/Save kann nicht versehentlich Fortschritt verlieren.

---

# P8 – UI Assets

## Benötigt

- inventory slot.
- selected slot frame.
- hover slot.
- disabled slot.
- button normal/hover/pressed.
- panel background.
- tab active/inactive.
- tooltip background.
- progress bar.
- scroll thumb/track.
- icons:
  - health
  - hunger
  - stamina
  - comfort
  - breath
  - armor optional
  - coin
  - recipe unlocked
  - journal new
  - station icons

## Akzeptanz

- UI Assets sind konsistent.
- Fallback Code-Drawing bleibt möglich.
- fehlende Assets werden gemeldet.

---

# Tests / Smoke Checks

- UI Scale 1x/1.5x/2x.
- inventory drag/drop.
- shift-click.
- right-click split.
- crafting search.
- locked recipe explanation.
- campfire cooking.
- cooking pot recipe.
- forge preview.
- storage transfer online/local.
- journal tabs.
- settings changes.
- pause -> journal -> back.

## Akzeptanz gesamt

- Inventory, Crafting, Storage und Settings nutzen konsistente Komponenten.
- UI Scale funktioniert überall.
- Locked Recipes sind verständlich.
- Cooking/Forge UI verhindert Verwirrung und Dupes.