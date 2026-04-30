# Adventura – Offener Gameplay-, Feature- und Engine-Plan

## Ziel

Adventura soll sich weiter von einem technischen Voxel-Prototyp zu einem eigenständigen cozy Survival-Adventure entwickeln.

Der Fokus liegt jetzt nicht mehr auf Grundsystemen, sondern auf:

- mehr Spieltiefe
- klarerer Progression
- besserem World-Feeling
- mehr Content
- besserem Balancing
- mehr Persistenz
- mehr Engine-Polish
- stabiler Multiplayer-Validierung

---

# 1. Wichtigste offene Prioritäten

## Sehr hohe Priorität

1. Save-/Load-Persistenz ausbauen
2. Cooking-System vollständiger machen
3. BlockEntity-System vereinheitlichen
4. Biome stärker voneinander unterscheiden
5. Loot, Ruinen und Exploration sinnvoll erweitern
6. Item-Namen und Registry-Aliase bereinigen
7. Tool-Progression finalisieren
8. Comfort-System spielerisch stärker nutzen

## Mittlere Priorität

1. Recipe-Unlocks wirklich an Progression koppeln
2. Journal-/Lore-System einführen
3. Structures größer und interessanter machen
4. Furniture mit echtem Gameplay-Nutzen erweitern
5. Mehr Tierinteraktionen
6. Audio-Backend statt nur Hooks
7. Particle-Polish und bessere visuelle Effekte

## Niedrigere Priorität

1. Temperature-/Cold-System
2. Trading/Village-System
3. Equipment-/Charm-Slots
4. Fishing
5. Weather
6. Advanced Lighting
7. Greedy Meshing / größere Engine-Optimierungen

---

# 2. Core Gameplay Loop – Ausbau

## Aktueller Ziel-Loop

1. Spieler startet in einer sicheren Cozy Meadow.
2. Spieler sammelt einfache Ressourcen.
3. Spieler craftet erste Werkzeuge.
4. Spieler baut Campfire, Storage und Sleeping Mat.
5. Spieler kocht Nahrung.
6. Spieler erkundet neue Biome.
7. Spieler findet biome-spezifische Ressourcen.
8. Spieler entdeckt Ruinen, Loot und Lore.
9. Spieler baut bessere Werkzeuge.
10. Spieler erweitert seine Base mit Comfort-Deko.
11. Spieler schaltet neue Rezepte frei.
12. Spieler sucht seltene Biome und Materialien.

## Offene Verbesserungen

### Early Game

Noch verbessern:

- Startgebiet klarer lesbar machen
- mehr sichtbare Anfänger-Ressourcen platzieren
- bessere Tutorial-Hinweise ohne echtes Tutorial
- Crafting-Reihenfolge stärker führen
- frühe Hunger-Balance prüfen
- erste Nacht emotional stärker machen

Offene konkrete Features:

- kleine Start-Hinweise über Feedback Log
- „First Campfire“-Feedback
- „First Shelter“-Feedback
- erstes Rezept-Freischalten stärker hervorheben
- einfache Startstruktur optional: kleines verlassenes Camp

---

### Mid Game

Noch verbessern:

- Pine Forest, Lakeside und Mushroom Grove müssen stärkere Gameplay-Gründe haben
- Resin, Clay, Copper und Glow Crystal brauchen klarere Verwendung
- Cooking Pot / Workbench / Forge als echte Progressionsstationen einführen
- Ruinen sollen nicht nur Deko sein, sondern Fortschritt bringen

Offene konkrete Features:

- Cooking Pot als neue Station
- Workbench als bessere Crafting-Station
- Forge für Iron und spätere Materialien
- biome-spezifische Rezept-Unlocks
- Ruinen-Loot mit Progression
- Map-Fragments für Exploration

---

### Late Game

Noch stark offen:

- Iron Progression
- Crystal Progression
- Ancient Fragment Progression
- Ruin Key System
- besondere Ruinen
- seltene Crafting-Rezepte
- bessere Adventure-Belohnungen

Offene konkrete Features:

- Iron Tools finalisieren
- Crystal Knife / Crystal Tools einführen
- Ancient Fragment sinnvoll nutzen
- Ruin Key oder Ruin Seal System
- besondere Loot-Chests
- seltene Lore-Fragmente
- Lost Charm als seltenes Sammelitem

---

# 3. Items und Ressourcen – offene Aufgaben

## Naming Cleanup

Problem:
Einige Items wirken doppelt oder uneinheitlich, z. B.:

- `berries` vs `wild_berries`
- `clay` vs `clay_lump`
- `raw_copper` vs `copper_ore`
- `raw_iron` vs `iron_ore`
- `planks` vs `wooden_plank`

Aufgabe:

- Canonical Item Keys definieren
- alte Keys als Aliase erhalten
- UI-Namen vereinheitlichen
- Rezepte auf Canonical Keys mappen
- Save-Kompatibilität behalten

Akzeptanz:

- alte Saves brechen nicht
- neue Namen sind sauber
- keine doppelten Items im Crafting
- alle Rezepte nutzen einheitliche Keys

---

## Neue oder noch unfertige Basic Resources

Offen:

- `dry_grass`
- `bark_strip`
- `charcoal`
- `clay_bowl`
- `clay_pot`
- `copper_ingot`
- `iron_ingot`
- `glow_crystal`
- `ancient_fragment`
- `cloth`
- `leather_strip`
- `honey`
- `water_container`

Diese Items brauchen jeweils:

- Registry-Eintrag
- Sprite/Icon
- Drop-Quelle
- Crafting-Nutzung
- Tooltip-Beschreibung
- Balance-Wert

---

## Food-Ausbau

Offene Food-Items:

- `cooked_berries`
- `roasted_mushroom`
- `mushroom_stew`
- `herb_soup`
- `berry_jam`
- `honey_snack`
- `calming_tea`
- `hearty_stew`

Jedes Food braucht:

- Hunger-Wert
- Heal-Wert
- eventuell Stamina-/Comfort-Effekt
- Cooking-Rezept
- passende Station
- Tooltip
- Balancing-Test

Empfohlene Werte:

| Item | Hunger | Heal | Besonderheit |
|---|---:|---:|---|
| cooked_berries | 4 | 1 | frühes Cooked Food |
| roasted_mushroom | 4 | 0 | einfache Alternative |
| mushroom_stew | 7 | 2 | Midgame-Nahrung |
| herb_soup | 5 | 1 | Stamina-Regen |
| berry_jam | 6 | 1 | cozy Food |
| honey_snack | 5 | 2 | seltenes Heal Food |
| calming_tea | 3 | 0 | Comfort-/Night-Bonus |
| hearty_stew | 10 | 3 | spätes starkes Food |

---

# 4. Blocks, Furniture und Deko

## Neue Natural Blocks

Offen:

- `soft_grass`
- `pine_needles`
- `flower_grass`
- `mushroom_soil`
- `clay_bank`
- `glowing_mushroom_block`
- `fiber_grass`
- `clay_deposit`
- `mushroom_cluster`
- `glow_crystal_node`

Jeder Block braucht:

- Render Layer
- Collision
- Hardness
- Drops
- Tool Requirement
- Biome Spawn
- Sprite/Texture
- Tooltip bei Itemform
- Server-Harvest-Regeln

---

## Neue Decor Blocks

Offen oder noch ausbaufähig:

- `cozy_lantern`
- `bookshelf`
- `market_crate`
- `ruin_brick`
- `cracked_ruin_brick`
- `mossy_ruin_brick`
- `ancient_tile`
- `old_wood_beam`

Jeder Decor Block braucht:

- Placeable Item
- Comfort-Wert
- Drop-Verhalten
- Tool-Verhalten
- Struktur-Verwendung
- Asset-Mapping
- optional Light Value

---

## Furniture mit Gameplay-Nutzen

Furniture soll nicht nur Deko sein.

Vorschlag:

| Block | Nutzen |
|---|---|
| chair | kleiner Comfort |
| table | Comfort + Workbench-Nähe optional |
| bookshelf | Lore-/Recipe-Unlock Bonus |
| rug | starker Base-Comfort |
| flower pot | kleiner Comfort, Pflanzen-Deko |
| lantern | Licht + Comfort |
| sleeping mat | Schlaf + Respawn später |
| storage crate | Storage |
| market crate | Loot/Trading später |

Offen:

- Furniture-Tooltips verbessern
- Comfort-Werte balancen
- Furniture in Structures nutzen
- optional Rotation/Orientation einbauen

---

# 5. Crafting und Cooking

## Recipe-System weiterentwickeln

Offen:

- alte `CraftingRecipe`s kompatibel behalten
- neues `RecipeDefinition`-Modell ergänzen
- Station Types sauber definieren
- Recipe Types definieren
- Unlock Conditions wirklich verwenden

Benötigte Enums:

```java
StationType:
- INVENTORY
- CRAFTING_TABLE
- CAMPFIRE
- COOKING_POT
- WORKBENCH
- FORGE
- LOOM

RecipeType:
- SHAPELESS
- SHAPED
- COOKING
- SMELTING
- WEAVING
- CARVING
- REPAIRING
- UPGRADING

RecipeCategory:
- TOOLS
- FOOD
- BUILDING
- DECOR
- ADVENTURE
- MATERIALS
```

---

## Cooking-System ausbauen

Aktuell sollte Cooking weiter von „Campfire-Craft“ zu echtem Station Gameplay wachsen.

Offene Features:

- Cooking Pot als eigene Station
- Fuel Slot
- Input Slot
- Output Slot
- Cooking Timer sichtbar machen
- mehrere Rezepte pro Station
- Output nicht sofort erzeugen
- Campfire nur für einfache Rezepte
- Cooking Pot für bessere Foods
- Forge für Metals

Akzeptanz:

- Campfire kann einfache Foods herstellen
- Cooking Pot kann bessere Foods herstellen
- Forge kann Iron verarbeiten
- Server validiert Fuel, Station, Reichweite und Input
- Client zeigt Cooking Progress

---

# 6. Comfort-System – Ausbau

Comfort existiert bereits grundlegend, soll aber spielerisch relevanter werden.

## Offene Verbesserungen

- Comfort stärker mit Basebuilding verbinden
- Comfort-Boni besser erklären
- Comfort-Feedback verbessern
- Comfort Cap pro Phase erhöhen
- Shelter-Erkennung verbessern
- Friendly Animals als Comfort-Quelle stärker nutzen
- Comfort in Sleep/Rest/Regeneration einbauen

## Gewünschte Effekte

Comfort soll beeinflussen:

- Hunger drain
- Stamina regen
- Health regen
- Sleep quality
- Night safety
- vielleicht Crafting-Speed später

## Offene Design-Aufgabe

Comfort darf nicht Pflicht-Grind werden.

Regel:

- ohne Comfort spielbar
- mit Comfort angenehmer
- keine harte Bestrafung
- Cozy statt Hardcore

---

# 7. Tool Progression

## Bereits begonnen, aber noch ausbauen

Offen:

- Iron Tools vollständig einführen
- Crystal Tools vollständig einführen
- Repair-System
- Bonus Drops
- bessere Mining Speeds
- klarere Tooltips
- bessere Block Requirements
- härtere Late-Game Nodes

## Empfohlene Tool-Tiers

| Tier | Level | Funktion |
|---|---:|---|
| Stone | 1 | Early Game |
| Copper | 2 | Mid Game |
| Iron | 3 | Late Game |
| Crystal | 4 | Rare / Magic Utility |

## Offene Features

### Repair-System

Items:

- stone tools reparierbar mit pebble
- copper tools reparierbar mit copper ingot
- iron tools reparierbar mit iron ingot
- crystal tools reparierbar mit glow crystal

### Bonus Drops

- Knife: mehr Fiber, Herbs, Resin
- Axe: mehr Bark, Resin, Logs
- Pickaxe: bessere Ore-Ausbeute
- Crystal Knife: seltene Pflanzen besser harvesten

---

# 8. Biome-Ausbau

## Ziel

Biome sollen sich nicht nur optisch unterscheiden, sondern durch:

- Ressourcen
- Tiere
- Loot
- Structures
- Stimmung
- Gefahren
- Progression

---

## Cozy Meadow

Offen:

- Startbiom stärker polieren
- mehr Blumenvarianten
- bessere Berry-/Fiber-Verteilung
- kleine Campsites
- mehr Hasen/Schafe
- sanfte Einstiegshinweise

Gameplay-Grund:

- sicherer Start
- frühe Nahrung
- erste Crafting-Ressourcen

---

## Pine Forest

Offen:

- Resin Tree stärker nutzen
- Bark Strip Harvesting
- mehr Pilze
- mehr Holztypen
- kleine Cabins
- Boar-Verhalten ausbauen

Gameplay-Grund:

- Resin
- Bark
- bessere Holzressourcen
- frühes Midgame

---

## Mushroom Grove

Offen:

- Glow Mushroom Blocks
- Glow Crystal Nodes
- Spore Particles ausbauen
- Snails stärker nutzen
- seltene Kräuter
- Mushroom Circle Structures

Gameplay-Grund:

- magische Materialien
- Glow Items
- seltene Foods
- cozy fantasy vibe

---

## Lakeside

Offen:

- Clay Deposits
- Reeds
- Water Container
- Cooking-Zutaten
- Lakeside Shack
- Fishing später

Gameplay-Grund:

- Clay
- Cooking
- Pottery
- Wasser-Ressourcen

---

## Old Ruins

Offen:

- Ruin Bricks
- Ancient Tiles
- Ancient Fragments
- Ruin Loot
- Lore Notes
- Ruin Key Progression
- seltene Gefahr optional

Gameplay-Grund:

- Adventure
- Progression
- Lore
- seltene Items

---

## Highlands

Offen:

- stärkere Ore-Verteilung
- Mine Entrances
- Copper/Iron Progression
- alte Watchtowers
- Wind Ambience

Gameplay-Grund:

- Mining
- Tools
- spätes Midgame

---

## Frost Peaks

Offen:

- Cold-System optional
- Crystal Nodes
- seltene Kräuter
- Schnee-/Eisblöcke
- Frozen Shrine

Gameplay-Grund:

- Late Game
- seltene Ressourcen
- Crystal Progression

---

# 9. Structures und Loot

## Offene Structures

- `cozy_campsite`
- `abandoned_cabin`
- `small_ruin`
- `old_watchtower`
- `mushroom_circle`
- `hidden_well`
- `broken_bridge`
- `old_mine_entrance`
- `lakeside_shack`
- `ruined_market_stall`
- `frozen_shrine`
- `ancient_gateway`

## Jede Structure braucht

- Spawn-Biome
- Spawn-Chance
- Größe
- Blockpalette
- LootTable
- Lore-Chance
- Entity-Spawns
- Progression-Wert
- deterministic placement
- chunk-border safe generation

## Loot-System weiter ausbauen

Offene Loot Tables:

- `common_nature`
- `campsite`
- `ruin_common`
- `ruin_rare`
- `old_mine`
- `mushroom_grove`
- `village_market`
- `frost_rare`

Offene Aufgaben:

- LootTable Registry
- Weighted Loot
- Min/Max Counts
- Conditions
- Rare Rolls
- Structure-specific loot markers
- Persistente opened state
- Save/Load für Loot-Crates

---

# 10. Lore und Adventure-System

Noch weitgehend offen.

## Ziel

Exploration soll sich lohnen, ohne Combat-Fokus.

## Features

- Old Notes
- Map Fragments
- Ancient Coins
- Lost Charms
- Ruin Keys
- Journal UI
- discovered biomes
- discovered recipes
- discovered structures

## Journal-System

Speichern:

- gefundene Notes
- entdeckte Biome
- entdeckte Structures
- freigeschaltete Rezepte
- besondere Items

UI:

- einfache Journal-Seite
- Kategorien:
  - Biomes
  - Notes
  - Recipes
  - Ruins
  - Creatures

---

# 11. Entity- und Tier-Ausbau

## Bestehende Tiere weiter vertiefen

Offen:

- Sheep shearing
- Bunny behavior polish
- Snail rare drops
- Boar mushroom finding
- Firefly night behavior verbessern
- Friendly Animal Comfort Bonus sichtbarer machen

## Neue Interaktionen

### Sheep

- mit berries anlocken
- später scheren
- cloth/wool source

### Bunny

- flieht
- kann selten zu hidden herbs führen
- keine Drops

### Moss Snail

- droppt selten moss/slime
- spawnt bei Mushroom Grove
- langsame cozy entity

### Little Boar

- findet mushrooms
- neutral
- kann mit mushroom gelockt werden

### Firefly Swarm

- nachts sichtbar
- Glow Particles
- zeigt besondere Orte an

---

# 12. UI/UX – weitere offene Verbesserungen

Viel UI ist bereits umgesetzt. Jetzt geht es um Polish.

## Offene UI Features

- Journal UI
- Recipe Book mit Unlock-Historie
- Cooking UI
- Forge UI
- Workbench UI
- Storage UI polish
- better item comparison
- better empty inventory states
- better controller/mouse consistency
- optional keybind screen

## HUD-Polish

- Comfort besser erklären
- Temperature nur anzeigen, wenn relevant
- Current biome schöner darstellen
- Day/time icon statt nur Text
- status effects anzeigen

## Tooltip-Polish

- Rarity-Farben
- Repair Material anzeigen
- Effective Tool anzeigen
- Station Requirement anzeigen
- Unlock Source anzeigen
- „Found in biome“-Hinweis optional

---

# 13. Audio und Feedback

Hooks existieren, aber echtes Audio muss weiter ausgebaut werden.

## Offene Aufgaben

- echtes Audio Backend prüfen/implementieren
- Sound Asset Mapping
- Lautstärke-Kategorien:
  - Master
  - Music
  - Ambience
  - SFX
  - UI
- biome-specific ambience
- day/night ambience
- campfire loop sauber machen
- footstep material detection verbessern

## Benötigte Sounds

- grass footsteps
- stone footsteps
- wood footsteps
- water splash
- item pickup
- inventory click
- craft success
- craft fail
- campfire crackle
- night ambience
- meadow birds
- cave drip
- ruin ambience
- mushroom grove ambience

---

# 14. Partikel und Visual Polish

Grundsystem existiert. Jetzt Polishing.

## Offene Partikel

- bessere block-specific debris
- glowing spores verbessern
- fireflies mit besserem Glow
- cooking steam aus Cooking Station
- leaf drift je nach Biom
- water splash stärker polishen
- ore sparkle bei seltenen Nodes
- comfort sparkle sehr subtil

## Regeln

- Partikel budgetieren
- Low-End Presets beachten
- keine visuellen Effekte, die Lesbarkeit stören
- cozy und weich, nicht überladen

---

# 15. Multiplayer und Server-Sicherheit

Viele Intents existieren. Offene Lücken:

## Recipe Unlock Validation

- Server muss prüfen, ob Spieler Rezept wirklich freigeschaltet hat
- Client darf locked recipes nicht einfach ausführen
- Player Progression muss gespeichert werden

## Cooking Pot / Forge Validation

- Station muss korrekt sein
- Station muss erreichbar sein
- Fuel muss passen
- Input Slots müssen passen
- Output darf nicht dupliziert werden

## Loot Persistence

- Chests müssen nach Server Restart stabil bleiben
- geöffnete Loot-Chests dürfen nicht resetten
- Structure Loot muss eindeutig identifizierbar sein

## Entity Drops

- Drops müssen serverseitig erzeugt werden
- cooldowns gegen Spam
- feed/follow/flee darf nicht clientseitig manipuliert werden

---

# 16. Save/Load und Persistence

Sehr wichtig.

## Offene Persistenz-Systeme

### World Save

Speichern:

- seed
- modified blocks
- block entities
- chests
- campfires
- cooking stations
- placed decor
- opened loot
- generated structure state

### Player Save

Speichern:

- position
- inventory
- health
- hunger
- stamina
- breath
- comfort optional nicht direkt, eher recalculated
- spawn point
- discovered recipes
- discovered biomes
- journal entries

### Entity Save

Optional später:

- wichtige Tiere
- named animals
- persistent village entities
- structure-bound entities

## Save Format

Empfohlen:

- versioned save format
- backup before migration
- unknown item fallback
- unknown block fallback
- block entity type ids
- migration hooks

---

# 17. Engine / Rendering / Worldgen – offene Aufgaben

## Rendering

Offen:

- bessere Wasseroptik
- bessere Cutout-Vegetation
- bessere Entity-Materialien
- bessere Glow-/Bloom-Abstimmung
- block light system
- cave darkness
- held item lighting optional
- shader presets

## Shader

Offene Shader-Features:

- biome tint
- day/night color grading
- smoother fog
- emissive material handling
- animated water polish
- glow mushroom emissive
- lantern/campfire light visuals
- particle soft fade

## Chunk / Mesh

Offen:

- Greedy Meshing prüfen
- Mesh memory overlay verbessern
- chunk rebuild priority verfeinern
- water mesh optimization
- cutout vegetation batching
- GL resource leak tracking

## Worldgen

Offen:

- bessere biome transitions
- structure placement über chunk borders robuster machen
- ore distribution final balancen
- cave generation verbessern
- rivers/lakes verbessern
- biome-specific terrain shapes
- deterministic feature placement testen

---

# 18. Tests – offene Testlücken

## Unit Tests

Noch ergänzen:

- Recipe unlock validation
- Cooking Pot recipes
- Forge recipes
- Repair recipes
- Loot conditions
- Journal unlocks
- Save/load item aliases
- Save/load block entities
- Save/load opened loot chests
- Comfort shelter edge cases
- Structure placement chunk borders

## Integration Tests

Noch ergänzen:

- Multiplayer cooking
- Multiplayer forge usage
- server rejects locked recipe
- server rejects wrong station
- server rejects invalid loot reopen
- save/reload crate contents
- save/reload campfire state
- save/reload player progression
- entity interaction with drops
- structure loot persistence after restart

## Manual Tests

Regelmäßig testen:

- Start singleplayer
- collect resources
- craft early tools
- cook food
- build base
- increase comfort
- sleep
- find Pine Forest
- harvest resin
- find Lakeside
- gather clay
- craft cooking pot
- find Mushroom Grove
- gather glow crystal
- find Old Ruin
- loot chest
- restart game
- verify persistence
- join multiplayer
- test same actions online

---

# 19. Neue Roadmap

## Phase 1 – Cleanup und Stabilisierung

Ziel:
Bestehende Systeme sauber benennen und stabilisieren.

Aufgaben:

- Item aliases cleanup
- Recipe metadata finalisieren
- Registry consistency tests
- canonical naming
- tooltip labels bereinigen
- README/TODO aktualisieren

Akzeptanz:

- keine doppelten Ressourcennamen
- alte Saves bleiben kompatibel
- alle Rezepte referenzieren gültige Items
- Build und Tests laufen lokal mit JDK

---

## Phase 2 – Cooking und Stations

Ziel:
Campfire, Cooking Pot, Workbench und Forge als echte Progressionsstationen.

Aufgaben:

- Cooking UI
- Cooking Pot Block
- Forge Block
- Workbench Block
- Fuel handling ausbauen
- Cooking timer anzeigen
- bessere Food-Rezepte
- Smelting-Rezepte

Akzeptanz:

- Campfire macht einfache Foods
- Cooking Pot macht bessere Foods
- Forge verarbeitet Iron
- Server validiert alles
- keine Dupes möglich

---

## Phase 3 – Save/Load und BlockEntities

Ziel:
Gameplay-Zustand bleibt dauerhaft erhalten.

Aufgaben:

- BlockEntityStore persistent machen
- Crates speichern
- Campfires speichern
- Cooking Stations speichern
- Loot Chests speichern
- Player Progress speichern
- Save versioning

Akzeptanz:

- Items in Crates bleiben nach Neustart erhalten
- Loot resetet nicht
- Campfire/Station States bleiben korrekt
- Player Progression bleibt erhalten

---

## Phase 4 – Biome Identity und Exploration

Ziel:
Jedes Biom hat klaren Nutzen.

Aufgaben:

- Biome resource profiles
- Clay in Lakeside
- Glow Crystal in Mushroom/Frost
- Resin in Pine Forest
- Ancient Fragments in Ruins
- bessere Structure tables
- ambience per biome
- biome-specific particles

Akzeptanz:

- Spieler erkennt Biome spielerisch
- jedes Biom hat eigene Ressourcen
- Exploration lohnt sich sichtbar

---

## Phase 5 – Ruins, Loot und Lore

Ziel:
Adventure-Teil ausbauen.

Aufgaben:

- LootTable Registry
- rare loot
- old notes
- map fragments
- journal UI
- ruin key progression
- bigger structures
- hidden chests

Akzeptanz:

- Ruinen enthalten sinnvollen Loot
- Lore-Fragmente werden gespeichert
- bestimmte Rezepte/Items können über Exploration freigeschaltet werden

---

## Phase 6 – Tool Progression und Repair

Ziel:
Werkzeuge fühlen sich wie Fortschritt an.

Aufgaben:

- Iron Tools
- Crystal Tools
- Repair System
- Bonus Drops
- bessere Mining Speeds
- late-game nodes
- tooltip polish

Akzeptanz:

- Stone → Copper → Iron → Crystal ist klar spürbar
- falsches Tool ist verständlich
- Repair lohnt sich
- seltene Ressourcen brauchen passende Tools

---

## Phase 7 – Cozy Endgame

Ziel:
Basebuilding, Tiere und Sammlung als langfristige Motivation.

Aufgaben:

- mehr Furniture
- Comfort Cap erhöhen
- animal comfort
- sheep shearing
- collectibles
- decorations
- restored market/village optional
- rare cosmetic items

Akzeptanz:

- Base fühlt sich lebendig an
- Spieler hat Gründe, Deko zu craften
- Tiere sind mehr als Ambient Models

---

# 20. Empfohlene nächste 12 PRs

## PR 1: Canonical Item Naming & Alias Cleanup

Ziel:
Item-Namen vereinheitlichen und Save-Kompatibilität sichern.

Aufgaben:

- canonical keys definieren
- aliases für alte keys
- recipes aktualisieren
- tooltip labels verbessern
- tests für registry aliases

Akzeptanz:

- keine kaputten Recipes
- alte Items funktionieren
- UI zeigt saubere Namen

---

## PR 2: RecipeDefinition V2

Ziel:
Crafting metadata sauber modellieren.

Aufgaben:

- StationType
- RecipeType
- RecipeCategory
- UnlockCondition
- Adapter für alte CraftingRecipe
- Tests

Akzeptanz:

- alte Recipes funktionieren
- neue Recipes können Station/Unlock/Category nutzen

---

## PR 3: Cooking UI V1

Ziel:
Cooking nicht nur über generisches Crafting lösen.

Aufgaben:

- Campfire Cooking Screen
- Input/Fuel/Output Slots
- Progress Bar
- Server sync
- fail feedback

Akzeptanz:

- Spieler sieht Cooking Progress
- Output wird serverseitig erzeugt
- keine Dupes

---

## PR 4: Cooking Pot Station

Ziel:
Midgame-Food freischalten.

Aufgaben:

- Cooking Pot Block/Item
- Station validation
- neue Food-Rezepte
- Tooltips
- Sprite mapping

Akzeptanz:

- bessere Foods brauchen Cooking Pot
- Campfire bleibt Early Game

---

## PR 5: Forge Station

Ziel:
Iron Progression ermöglichen.

Aufgaben:

- Forge Block/Item
- Iron smelting
- Fuel requirements
- recipe unlocks
- tooltip polish

Akzeptanz:

- Iron Ingots können hergestellt werden
- Iron Tools werden möglich

---

## PR 6: Persistent BlockEntities

Ziel:
Crates, Campfires und Stations speichern.

Aufgaben:

- BlockEntity save format
- crate contents speichern
- campfire state speichern
- station state speichern
- versioning vorbereiten

Akzeptanz:

- Neustart verliert keine Crate Items
- Campfire/Station States bleiben korrekt

---

## PR 7: LootTable Registry

Ziel:
Exploration belohnend machen.

Aufgaben:

- weighted loot
- min/max count
- conditions
- deterministic rolls
- tests

Akzeptanz:

- gleiche Seed/Structure erzeugt gleichen Loot
- Loot ist serverseitig

---

## PR 8: Structure Loot Persistence

Ziel:
Loot-Chests dürfen nicht resetten.

Aufgaben:

- loot marker ids
- generated/opened flags
- save/load
- multiplayer safe transfer

Akzeptanz:

- chest loot entsteht einmal
- nach Neustart bleibt Zustand erhalten

---

## PR 9: Biome Resource Profiles

Ziel:
Biome spielerisch unterscheiden.

Aufgaben:

- resource tables pro biome
- spawn weights
- clay/lakeside
- resin/pine
- glow/mushroom
- ore/highland
- tests

Akzeptanz:

- jedes Biom hat klare Ressourcenidentität

---

## PR 10: Journal & Lore Notes V1

Ziel:
Adventure-Fortschritt speichern.

Aufgaben:

- old_note item
- journal state
- simple journal UI
- note pickup unlocks
- save/load

Akzeptanz:

- gefundene Notes bleiben gespeichert
- Journal zeigt Einträge

---

## PR 11: Tool Repair & Bonus Drops

Ziel:
Tools langfristig nützlicher machen.

Aufgaben:

- repair recipes
- repair material tooltip
- knife/axe/pickaxe bonus drops
- durability balancing
- tests

Akzeptanz:

- Tools können repariert werden
- passende Tools geben spürbare Vorteile

---

## PR 12: Cozy Endgame Furniture Pass

Ziel:
Basebuilding stärker belohnen.

Aufgaben:

- bookshelf
- better lantern
- rug variants
- flower pot variants
- comfort balancing
- structure usage
- sprites

Akzeptanz:

- Base sieht besser aus
- Comfort steigt nachvollziehbar
- Furniture hat Gameplay-Wert