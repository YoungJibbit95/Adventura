# Adventura – UI TODO List

## Ziel

Die UI soll cozy, klar, pixel-art-kompatibel und mausfreundlich werden. Sie soll Cooking, Forge, Journal, Recipe Unlocks und Storage sauber unterstützen.

## P0 – UI-Grundqualität

- warmer Pixel-Art-Stil.
- klare Rahmen und gute Lesbarkeit.
- UI Scale überall anwenden.
- Hover/Pressed/Disabled States vereinheitlichen.
- zentrale Komponenten: Buttons, Panels, Slots, Tabs, Chips, Scrollbars, Tooltips, Progress Bars, Textfelder.

## P1 – Inventory UI

- Drag/drop weiter testen.
- Shift-click quick move weiter testen.
- Right-click split stack weiter testen.
- Sort Button klarer beschriften.
- Trash nur Creative sichtbar/aktiv.
- Slot Hover Feedback verbessern.
- Item Comparison bei Tools.
- Repair Material im Tooltip.
- Rarity-Farben.
- Stack overflow feedback.

## P2 – Crafting UI

- Recipe Book verbessern.
- Unlock-Historie anzeigen.
- locked recipes klar erklären.
- Rezept-Suche weiter verbessern.
- Filter speichern.
- fehlende Station prominent anzeigen.
- fehlende Zutaten gruppiert anzeigen.

Recipe Details:
- Name, Output, Zutaten, Station, Kategorie, Unlock Source, Crafting Time, Beschreibung.

## P3 – Cooking UI

### Campfire UI
- Input Slot.
- Fuel Slot.
- Output Slot.
- Burn-Time-Anzeige.
- Cook-Time-Anzeige.
- Active/Inactive State.
- Fehlerfeedback: no fuel, no matching recipe, inventory full, campfire inactive.

### Cooking Pot UI
- mehrere Rezeptoptionen.
- Wasser-/Container-Anforderung anzeigen.
- bessere Food Outputs.
- Fortschrittsbalken.

### Forge UI
- Ore Input.
- Fuel Input.
- Output.
- Heat/Burn Progress.
- Rezeptanforderungen.

## P4 – Storage UI

- Titel und Entfernung/Blockname.
- Spieler-Inventar und Crate-Inventar klar trennen.
- Shift-click zwischen Inventaren.
- Right-click Split.
- Drag/drop.
- Sort crate optional.
- Transfer failure feedback.
- spätere Storage-Typen: größere Chests, Market Crates, Loot Crates.

## P5 – Journal UI

Seiten:
- Notes
- Biomes
- Structures
- Creatures
- Recipes
- Collectibles

Features:
- neue Einträge markieren.
- gefundene Lore lesen.
- Biome mit kurzem Text.
- Creature Infos.
- Recipe Unlock Source.
- Progress-Anzeige.

## P6 – Settings UI

Bestehend/erweitern:
- Render Distance, Preview Radius, FOV, Mouse Sensitivity, Water, AO, Shadows, Bloom/Glow, VSync, UI Scale.

Neu:
- Master Volume, Music Volume, Ambience Volume, SFX Volume, UI Volume.
- Particle Quality.
- Graphics Presets: Low, Medium, High.
- Keybind Screen.

## P7 – Menüs

### Main Menu
- Singleplayer klar.
- Join Server klar.
- Settings erreichbar.
- Seed/World Options schöner.
- cozy Hintergrund optional.

### Pause Menu
- Resume, Settings, Save/Exit, Return to Main Menu.
- Debug options nur optional.

## P8 – UI Assets

Benötigt:
- inventory slot, selected slot frame, hover slot, disabled slot.
- button normal/hover/pressed.
- panel background.
- tab active/inactive.
- tooltip background.
- progress bar.
- icons: health, hunger, stamina, comfort, breath, armor optional, coin, recipe unlocked.

## Akzeptanzkriterien

- Inventory, Crafting, Storage und Settings nutzen konsistente Komponenten.
- UI Scale funktioniert überall.
- Locked Recipes sind verständlich.
- Cooking/Forge UI verhindert Verwirrung und Dupes.
