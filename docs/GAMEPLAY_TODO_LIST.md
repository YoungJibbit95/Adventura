# Adventura – Gameplay TODO List

Stand: 2026-04-30

## Ziel

Adventura soll sich wie ein eigenständiges cozy Survival-Adventure anfühlen: sammeln, craften, kochen, bauen, erkunden, Tiere beobachten, Ruinen entdecken und die eigene Base gemütlicher machen.

Diese Liste konzentriert sich auf Spieltiefe und Feature-Loop. Technische Basis wie Rendering, Save/Load, Networking und Physics stehen in eigenen Listen.

---

# P0 – Core Gameplay Loop schärfen

## Ziel-Loop

1. Spieler spawnt in einer sicheren Cozy Meadow.
2. Spieler sieht sofort Startressourcen und ein kleines Campsite.
3. Spieler sammelt Twigs, Pebbles, Fiber, Berries, Herbs und Mushrooms.
4. Spieler craftet Stone Knife, Stone Axe, Stone Pickaxe und Campfire.
5. Spieler aktiviert Campfire mit Fuel.
6. Spieler kocht einfache Nahrung.
7. Spieler baut Storage, Sleeping Mat und erste Deko.
8. Spieler erkundet Pine Forest, Lakeside und Mushroom Grove.
9. Spieler sammelt Resin, Clay, Copper und Glow Resources.
10. Spieler baut bessere Stationen und Tools.
11. Spieler findet Ruinen, Lore, Loot und seltene Materialien.
12. Spieler verbessert Base Comfort und bereitet sich auf Late-Game-Biome vor.

## P0.1 Early Game verbessern

### Basis vorhanden

- Starter-Ressourcen rund um Campsite sind begonnen.
- Early-Game-Meilenstein-Feedback ist begonnen.
- Spawn-Campsite mit Campfire/Cooking Pot/Lantern/Logs/Storage ist begonnen.

### Abgeschlossen (Alpha)

- ~~Testlauf mit JDK durchführen.~~ `:common:test :client:test :server:test` und `buildGame` grün.
- ~~Startgebiet visuell noch klarer machen.~~ Starter-Campsite, Starter-Ressourcen und Journal-Strukturhinweise decken den Alpha-Start ab.
- ~~Anfänger-Ressourcen aus Spielerperspektive sichtbar platzieren.~~ Starter-Ressourcen liegen deterministisch um den Spawn-Campsite.
- ~~erstes Rezept-Freischalten deutlicher machen.~~ Early-Game-Meilenstein-Feedback meldet erste Supplies/Recipes/Tools.
- ~~erster Campfire-Moment stärker inszenieren.~~ Campfire-Craft, Placement, Fuel und Light Feedback sind kurz und kontextuell.
- ~~erste Nacht emotional stärker machen: dunkler, aber nicht unfair.~~ NightSafetyPrompts erklären Dusk/Night und Campfire-Sicherheit.
- ~~optional kurze „Found an old camp“-Meldung.~~ Für Alpha durch Campsite-Structure, Journal und Campfire-Hints ersetzt.
- ~~keine langen Tutorialtexte; Feedback kurz und kontextuell.~~ Hinweise bleiben Status-/Hint-Zeilen statt Tutorial-Fließtext.

### Akzeptanz

- ~~neuer Spieler findet ohne Wiki erste Ressourcen.~~
- ~~Spieler craftet innerhalb weniger Minuten ein Tool.~~
- ~~Spieler versteht Campfire als Sicherheit/Comfort-Ort.~~

---

## P0.2 Mid Game ausbauen

### Abgeschlossen (Alpha)

- ~~Pine Forest, Lakeside und Mushroom Grove mit klaren Gameplay-Gründen versehen.~~ Pine liefert Resin/Bark, Lakeside Reeds/Water, Mushroom/Old-Ruins Glow- und Food-Ressourcen.
- ~~Resin, Clay, Copper und Glow Crystal stärker in Recipes einbinden.~~ Torch/Handle/Tools/Pot/Workbench/Forge/Ancient/Ruin Recipes nutzen die Materialien.
- ~~Workbench als echte Progressionsstation.~~ Workbench-Block, Station-Regel, Client-Erkennung, Recipes und Tests sind vorhanden.
- ~~Cooking Pot als Food-Progression.~~ Soups, Tea, Jam, Stew und Honey sind Cooking-Pot-gated.
- ~~Forge als Metal-Progression.~~ Forge-Block, Station-Regel, Server-CookRequest und Iron/Ruin-Seal Recipes sind vorhanden.
- ~~Ruinen als Quelle für Lore und seltene Items.~~ Ruin- und Rare-Ruin-Crates droppen Ancient/Crystal/Key/Seal/Charm-Loot.

### Akzeptanz

- ~~Spieler hat einen Grund, jedes Midgame-Biom zu suchen.~~
- ~~neue Ressourcen führen zu neuen Stationen/Tools/Decor.~~

---

## P0.3 Late Game definieren

### Abgeschlossen (Alpha)

- ~~Iron Progression vollständig machen.~~ Raw Iron -> Forge -> Iron Ingot -> Workbench-Iron-Tools -> Crystal-Mining ist als Alpha-Pfad geschlossen.
- ~~Crystal Progression einführen.~~ Glow Crystal ist gated durch Iron Pickaxe und nutzt Ancient Lantern/Ruin Key/Ruin Seal.
- ~~Ancient Fragment sinnvoll nutzen.~~ Ancient Lantern, Ruin Key und Ruin Seal binden Ancient Fragments.
- ~~Ruin Key / Ruin Seal System entwickeln.~~ Items, Recipes, Forge/Workbench-Gates, Loot und Tests sind vorhanden.
- ~~besondere Ruinen und seltene Loot-Chests.~~ Small Ruin hat Rare-Crate-Marker mit eigener Loot-Tabelle.
- ~~Lost Charm / Collectibles.~~ Lost Charm ist als seltenes Ruinen-Collectible registriert und lootbar.
- ~~Late-Game Comfort Deko.~~ Ancient Lantern bleibt seltene, starke Comfort-/Light-Deko.

### Akzeptanz

- ~~nach Copper gibt es klare nächste Ziele.~~
- ~~Ruinen haben Progression-Wert.~~
- ~~seltene Items fühlen sich besonders an.~~

---

# P1 – Items und Ressourcen

## P1.1 Naming Cleanup

### Abgeschlossen

Canonical Keys vereinheitlichen:

- ~~`berries` vs `wild_berries`~~ Alias zeigt auf `berries`.
- ~~`clay` vs `clay_lump`~~ Block bleibt `clay`, Item bleibt `clay_lump`.
- ~~`raw_copper` vs `copper_ore`~~ Alias zeigt auf `raw_copper`.
- ~~`raw_iron` vs `iron_ore`~~ Alias zeigt auf `raw_iron`.
- ~~`planks` vs `wooden_plank`~~ Aliase zeigen auf `skyroot_planks`.

### Akzeptanz

- ~~UI zeigt saubere Namen.~~
- ~~Rezepte nutzen Canonical Keys.~~
- ~~alte Keys bleiben als Aliase save-kompatibel.~~

---

## P1.2 Neue Basic Resources

### Abgeschlossen (Alpha-Items)

- ~~`dry_grass`: Firestarter, Sleeping Mat, einfache Deko.~~ Quelle: Wild-Grass-Interaktion; Nutzung: Fuel/Sleeping Mat.
- ~~`bark_strip`: Handles, Repair, rustic decor.~~ Quelle: Tree Stump; Nutzung: Handles/Resin Torch/Leather Strip.
- ~~`charcoal`: Fuel, Lanterns, Forge helper.~~ Quelle: Campfire; Nutzung: Copper/Iron/Forge/Leather.
- ~~`clay_bowl`: Container für Soup/Stew.~~ Quelle: Campfire-fired Clay; Nutzung: Pot Food.
- ~~`clay_pot`: Flower Pot, Planter, Cooking Pot precursor.~~ Quelle: Campfire-fired Clay; Nutzung: Pot/Decor/Forge.
- ~~`copper_ingot`: Copper Tools.~~ Quelle: Campfire smelting; Nutzung: Copper Tools/Cooking Pot/Forge.
- ~~`iron_ingot`: Iron Tools und Forge Progression.~~ Quelle: Forge smelting; Nutzung: Iron Tools/Ruin Key.
- ~~`glow_crystal`: Glow Lantern und Ruin Progression.~~ Quelle: Crystal Node/Ruin Loot; Nutzung: Ancient Lantern/Ruin Key/Ruin Seal.
- ~~`ancient_fragment`: Ruin Progression.~~ Quelle: Ruin Loot; Nutzung: Ancient Lantern/Ruin Key/Ruin Seal.
- ~~`cloth`: Rugs, Furniture und textile Progression.~~ Quelle: Workbench weaving; Nutzung: textile progression/loot.
- ~~`leather_strip`: Tool grips, late recipes.~~ Quelle: Workbench curing; Nutzung: Iron Tools/Ruin Key.
- ~~`honey`: Sweet food, healing snacks.~~ Quelle: Cooking Pot; Nutzung: food/heal resource.
- ~~`water_container`: Tea, soup, cooking recipes.~~ Quelle: Clay Pot + Reeds; Nutzung: Cooking recipes.

### Jedes Item braucht

- ~~Registry-Eintrag.~~
- ~~Display Name.~~
- ~~Beschreibung.~~
- ~~Stackgröße.~~
- ~~Sprite/Icon.~~
- ~~Drop-/Crafting-Quelle.~~
- ~~mindestens eine Verwendung.~~
- ~~Tooltip-Daten.~~
- ~~Validation Test.~~

### Akzeptanz

- ~~kein neues Item ist nur Deko im Registry ohne Gameplay-Nutzung.~~
- ~~jedes Item hat mindestens Quelle oder Rezept.~~

---

# P2 – Food und Cooking Gameplay

## P2.1 Food-Liste

| Item | Rolle | Vorschlag |
| --- | --- | --- |
| cooked_berries | frühes cooked food | leichte Hunger-/Heal-Verbesserung |
| roasted_mushroom | frühe Alternative | einfach am Campfire |
| mushroom_stew | Midgame staple | braucht Bowl/Pot |
| herb_soup | Utility Food | Stamina/leichte Heilung |
| berry_jam | Cozy Food | Cooking Pot, länger sättigend |
| honey_snack | seltenes Heal Food | Honey + berries |
| calming_tea | Comfort/Night Utility | herbs + water |
| hearty_stew | Late Food | hohe Sättigung, station-gated |

## P2.2 Cooking Stations

### Campfire

- early foods.
- roasted foods.
- simple recipes.
- braucht Fuel.
- gibt Comfort und Licht.

### Cooking Pot

- soups.
- stews.
- tea.
- jam.
- water/container requirements.
- besseres Food als Campfire.

### Forge

- metal smelting.
- Iron Progression.
- später Crystal/Ancient crafting optional.

## P2.3 Offene Aufgaben

- Cooking Pot BlockEntity finalisieren.
- Forge BlockEntity finalisieren.
- echte UI für Station Slots.
- Fuel/Input/Output sauber trennen.
- Cook-Time sichtbar machen.
- Recipe unlocks serverseitig validieren.
- Food-Effekte balancen.

## Akzeptanz

- rohes Essen ist okay, aber schwach.
- gekochtes Essen lohnt sich.
- bessere Foods brauchen bessere Station.
- keine Food-/Cooking-Dupes im Multiplayer.

---

# P3 – Comfort-System vertiefen

## Ziel

Comfort soll Basebuilding belohnen, aber nicht erzwingen. Adventura bleibt cozy, nicht hardcore.

## Quellen

- active campfire
- lanterns
- rugs
- chairs
- tables
- flower pots
- bookshelf
- sleeping mat
- shelter
- friendly animals
- storage crate
- garden fence

## Effekte

- Hunger sinkt langsamer.
- Stamina regeneriert schneller.
- Health regeneriert etwas besser.
- Sleep funktioniert besser.
- Base fühlt sich sicherer an.

## Offen

- Boni besser erklären.
- Comfort-Cap pro Progression erhöhen.
- Shelter-Erkennung verbessern.
- Friendly Animal Comfort stärker nutzen.
- Furniture-Tooltips mit Comfort-Wert.
- Comfort Sources optional im HUD anzeigen.
- Comfort nicht als Grind wirken lassen.

## Akzeptanz

- Basebuilding hat spürbaren Nutzen.
- Comfort ist verständlich.
- Spieler kann auch ohne Max-Comfort normal spielen.

---

# P4 – Tool Progression

## Tool Tiers

| Tier | Rolle |
| --- | --- |
| Stone | Early Game |
| Copper | Mid Game |
| Iron | Late Game |
| Crystal | rare / magical utility |

## Offen

- Iron Tools vollständig einführen.
- Repair-System einbauen.
- härtere Late-Game Nodes.
- Tooltips mit Effective Against.

## Repair

- Stone Tools mit Pebble.
- Copper Tools mit Copper Ingot.
- Iron Tools mit Iron Ingot.
- Crystal Tools mit Glow Crystal.

## Bonus Drops

- seltene Pflanzen/Glow Harvest für Crystal Knife ausbauen.

## Akzeptanz

- Tool-Tier-Wechsel ist spürbar.
- falsches Tool ist verständlich.
- Repair lohnt sich, ersetzt aber Crafting nicht komplett.

---

# P5 – Biome Gameplay

## Cozy Meadow

### Offen

- Startbiom polieren.
- mehr Blumenvarianten.
- Berry-/Fiber-Verteilung prüfen.
- kleine Campsites.
- mehr Hasen/Schafe.
- sanfte Einstiegshinweise.

### Gameplay-Grund

- sicherer Start.
- erste Nahrung.
- erste Tools.

---

## Pine Forest

### Basis vorhanden

- Tree Stumps liefern `bark_strip`.
- Bark brennt kurz als Fuel.
- Resin+Bark craften `resin_torch`.
- Bark wird über `tool_handle` zur Copper-Tool-Komponente.

### Offen

- Resin Tree stärker nutzen.
- Bark Strip Harvesting polishen.
- mehr Pilze.
- mehr Holztypen.
- kleine Cabins.
- Boar-Verhalten ausbauen.

### Gameplay-Grund

- Resin.
- Bark.
- Tool Handles.
- Copper-Tool-Vorstufe.

---

## Mushroom Grove

### Basis vorhanden

- Moss Snails können Drops erzeugen.
- Moss/Slime Drops sind angebunden.
- Glow Mushrooms spawnen als leuchtende Grove-Ressource.
- Glow Mushroom Caps führen in Cooking-Pot-Stew.
- Mushroom Circles spawnen als Grove-Adventure-Hook.
- Spore Blossoms liefern seltene Grove-Herbs und Spore Tea.
- Mushroom Circles enthalten Glow-Crystal-Nodes als Crystal-Progression-Fund.
- Glow/Spore/Grove-Blöcke speisen Ambient-Spore-Partikel.

### Offen

- magische Nachtstimmung.

### Gameplay-Grund

- Glow Items.
- seltene Foods.
- Crystal Progression.

---

## Lakeside

### Basis vorhanden

- Reeds spawnen als Ufer-Ressource.
- Reeds liefern `reed_bundle`.
- Reeds brennen kurz als Fuel.
- `water_container` wird für Cooking Pot vorbereitet.

### Offen

- Clay Deposits stärker einbauen.
- Lakeside Shack.
- Water Container in Cooking verwenden.
- Fishing später.

### Gameplay-Grund

- Clay.
- Cooking.
- Pottery.
- Water resources.

---

## Old Ruins

### Basis vorhanden

- Ruin Crates können `ancient_fragment` und selten `ancient_lantern` liefern.
- Ancient Lantern ist craftbar/platzierbar.

### Offen

- Ruin Bricks.
- Ancient Tiles.
- Ancient Fragment Progression.
- Ruin Loot.
- Lore Notes.
- Ruin Key / Seal System.
- seltene Gefahr optional.

### Gameplay-Grund

- Adventure.
- Lore.
- seltene Items.
- Progression.

---

## Highlands

### Offen

- stärkere Ore-Verteilung.
- Mine Entrances.
- Copper/Iron Progression.
- alte Watchtowers.
- Wind Ambience.

### Gameplay-Grund

- Mining.
- Tools.
- spätes Midgame.

---

## Frost Peaks

### Offen

- Crystal Nodes.
- rare herbs.
- Schnee-/Eisblöcke.
- Frozen Shrine.
- Cold-System optional.

### Gameplay-Grund

- Late Game.
- seltene Ressourcen.
- Crystal Progression.

---

# P6 – Structures, Loot und Lore

## Structures

- cozy campsite
- abandoned cabin
- small ruin
- old watchtower
- mushroom circle
- hidden well
- broken bridge
- old mine entrance
- lakeside shack
- ruined market stall
- frozen shrine
- ancient gateway

## Loot Tables

- common_nature
- campsite
- ruin_common
- ruin_rare
- old_mine
- mushroom_grove
- village_market
- frost_rare

## Lore / Journal

- old notes
- map fragments
- ancient coins
- lost charms
- ruin keys
- discovered biomes
- discovered structures
- discovered recipes

## Offen

- Journal Entries persistieren.
- Note Pickup klar feedbacken.
- Lore darf Rezepte/Map-Clues unlocken.
- Loot Chests persistent machen.
- Ruin Key/Seal definieren.

## Akzeptanz

- Ruinen sind mehr als Deko.
- Exploration bringt Fortschritt.
- Loot kann nicht dupliziert werden.

---

# P7 – Tiere und Cozy-Life

## Sheep

- mit berries anlocken.
- später scheren.
- cloth/wool source.
- Base-Comfort Bonus optional.

## Bunny

- flieht.
- kann zu hidden herbs führen.
- keine Drops.
- reine Atmosphäre plus kleine Hinweise.

## Little Boar

- findet mushrooms.
- neutral.
- mit mushroom lockbar.
- leather_strip optional vorsichtig balancen.

## Moss Snail

- langsam.
- Mushroom Grove/Mire.
- rare moss/slime drops.
- cozy creature, keine Gefahr.

## Firefly Swarm

- nachts sichtbar.
- Glow Particles.
- zeigt besondere Orte an.
- kein harter Gameplay-Zwang.

## Akzeptanz

- Tiere wirken lebendig.
- mindestens einige Tiere haben friedliche Interaktion.
- keine Tierinteraktion wird zum Grind.

---

# P8 – Combat Light / Adventure Danger optional

## Ziel

Combat soll nicht der Kern sein, aber leichte Gefahren können Ruinen/Exploration spannender machen.

## Offen

- Damage System engine-seitig vorbereiten.
- Bogen/Projectile Foundation.
- sehr einfache Gegner optional:
  - ruin crawler
  - cave wisp
  - night shadow optional
- Gegner selten und klar telegraphed.
- Cozy-Spielgefühl nicht zerstören.

## Akzeptanz

- Adventure bekommt Spannung.
- Basebuilding/Cozy bleibt Hauptgefühl.
- Server validiert Damage/Projectiles.

---

# Akzeptanz gesamt

- Spieler hat in jedem Biom einen klaren Grund zu bleiben.
- Early Game führt natürlich zu Campfire, Storage und erster Nahrung.
- Mid Game führt zu Clay, Resin, Copper und Cooking Pot.
- Late Game führt zu Iron, Crystal, Ruins und Lore.
- Basebuilding hat spürbaren Nutzen durch Comfort.
- Exploration wird belohnt, ohne Hardcore-Grind zu werden.

---

# P9 – Alpha Game Design Systems

Owner: Lead Game Design Engineer.

Dieser Block ergänzt die reine Feature-Liste um die Systeme, die Adventura als echtes Alpha-Spiel zusammenhalten sollen.

## P9.1 Core Loop als messbare Stufen

### Status 2026-05-01

- ~~🟠 In Arbeit: Alpha-Milestone-Kette von Spawn bis erster Ruin als Common/Gameplay-Contract ausarbeiten.~~
  Erledigt: `AlphaMilestones` Common-Contract plus `docs/ALPHA_GAMEPLAY_CONTRACTS.md`.
  Verifikation: `./gradlew :common:test --tests 'dev.voxelgame.common.content.*' --tests 'dev.voxelgame.common.gameplay.AlphaMilestonesTest'` gruen.

### Offen

- ~~Spielerreise in Milestones schneiden:~~
  - ~~Spawn sichern.~~
  - ~~erste Nahrung.~~
  - ~~erstes Werkzeug.~~
  - ~~Campfire.~~
  - ~~Storage.~~
  - ~~Workbench.~~
  - ~~Cooking Pot.~~
  - ~~Forge.~~
  - ~~erster Ruin.~~
  - ~~erster Comfort-Level.~~
  - ~~erster Rare-Fund.~~
- ~~Pro Milestone definieren:~~
  - ~~Ausloeser.~~
  - ~~notwendige Items/Blocks.~~
  - ~~UI/HUD Feedback.~~
  - ~~Save-State.~~
  - ~~Servervalidierung.~~
  - ~~Smoke-Test.~~
- ~~EarlyGameMilestones aus UI-Hints zu einem Common/Gameplay-Konzept entwickeln.~~
  Erledigt: Common-Milestones bilden die bisherigen Early-Game-Hints als persistierbare Keys/Contracts ab; Client-Migration kann danach klein erfolgen.
  Verifikation: `AlphaMilestonesTest` prueft Reihenfolge, eindeutige Save-Keys und registrierte Item-/Block-/Biome-/Structure-Referenzen.

### Akzeptanz

- ~~Alpha-Spieler wissen natuerlich, was sie als Naechstes tun koennen.~~
- ~~Progression ist test- und speicherbar.~~
- ~~Features zahlen auf den Loop ein, statt nur Content zu sein.~~

## P9.2 Datengetriebene Items und Tags

### Status 2026-05-01

- ~~🟠 In Arbeit: ContentTagRegistry-Anforderungen fuer Items, Blocks und Entities mit Item-Design-Daten vorbereiten.~~
  Erledigt: `ContentTagRegistry` Read-Contract, `AlphaItemDesigns` fuer den kritischen Pfad und Doku-Anforderungen angelegt.
  Verifikation: `ContentTagRegistryTest` und `AlphaItemDesignsTest` gruen im fokussierten Common-Testlauf.

### Offen

- ~~Item-Ideen nicht nur in `Items.java` registrieren, sondern als Design-Daten beschreiben:~~
  - ~~Rolle.~~
  - ~~Biome-Quelle.~~
  - ~~Tier/Structure-Quelle.~~
  - ~~Stackgroesse.~~
  - ~~Nahrung/Heilung.~~
  - ~~Tool-Tier.~~
  - ~~Durability.~~
  - ~~Tags.~~
  - ~~Unlock.~~
  - ~~UI-Icon.~~
  - ~~Drop-/Loot-Tabelle.~~
- ~~ContentTagRegistry mit Engine/Networking abstimmen.~~
  Erledigt: Common-API fuer `ITEM`, `BLOCK`, `ENTITY`, Alias-Aufloesung, `keysWithTag(...)`, `entries(...)` und Coverage-Report vorhanden.
  Verifikation: Tests pruefen Alias-Tags, kritische Alpha-Tags und registrierte Item-/Block-Keys.
- ~~Item-Balancing-Tabelle in Docs oder Datenfile vorbereiten.~~
  Erledigt: `AlphaItemDesigns` spiegelt Stacksize, Food/Heal, Tool-Tier und Durability aus der Registry.
  Verifikation: `AlphaItemDesignsTest.designRowsMirrorRegistryBalancingFieldsAndTags`.
- ~~Doppelte Items mit gleicher Funktion vermeiden.~~
  Erledigt: Alpha-kritische Items haben eindeutige Rollen/Uses in `AlphaItemDesigns`; weitere Content-Erweiterungen muessen dort sichtbar werden.
  Verifikation: Design-Map weist doppelte Keys ab und Milestone-Pflichtitems muessen eine Design-Zeile haben.

### Akzeptanz

- ~~Neue Items haben einen Gameplay-Grund.~~
- ~~UI, Loot, Crafting, Physics und Networking nutzen dieselben Tags.~~
- ~~Balancing-Aenderungen sind nachvollziehbar.~~

## P9.3 Station Progression

### Status 2026-05-01

- ~~🟠 In Arbeit: Station-Progression als Common-Contract fuer Inventory, Campfire, Workbench, Cooking Pot, Forge und spaetere Ancient Station ausarbeiten.~~
  Erledigt: `StationProgression`/`StationDefinition` Common-Contract plus Station-Tabelle in `docs/ALPHA_GAMEPLAY_CONTRACTS.md`.
  Verifikation: `./gradlew :common:test --tests 'dev.voxelgame.common.content.*' --tests 'dev.voxelgame.common.gameplay.AlphaMilestonesTest' --tests 'dev.voxelgame.common.gameplay.StationProgressionTest'` gruen.

### Offen

- ~~Stations als klare Progressionskette definieren:~~
  - ~~Inventory.~~
  - ~~Workbench.~~
  - ~~Campfire.~~
  - ~~Cooking Pot.~~
  - ~~Forge.~~
  - ~~Ancient Workbench/Altar spaeter.~~
- ~~Jede Station braucht:~~
  - ~~Crafting-Rolle.~~
  - ~~BlockEntity-State.~~
  - ~~UI-Screen.~~
  - ~~Save-State.~~
  - ~~Server-Transaction.~~
  - ~~Audio/Particle Feedback.~~
  - ~~Loot/Unlock-Quellen.~~
- ~~Cooking Pot und Forge nicht nur als Rezeptfilter, sondern als Spielobjekte ausarbeiten.~~
  Erledigt: Pot/Forge haben explizite Slot-/Progress-/Save-/Transaction-Anforderungen im Station-Contract.
  Verifikation: `StationProgressionTest` prueft Recipe-Station-Abdeckung, registrierte Unlock-Keys und nutzbare Station-Blocks.

### Akzeptanz

- ~~Spieler versteht, warum eine neue Station wichtig ist.~~
- ~~Stationen erzeugen keine Dupes.~~
- ~~Progression bleibt cozy, aber zielgerichtet.~~

## P9.4 Quest-, Journal- und Lore-Struktur

### Status 2026-05-01

- ~~🟠 In Arbeit: Journal-/Lore-/Goal-Progression als Common-Contract fuer Discovery Events, Ruinenprogression und persistierbare UI/Server-Keys definieren.~~
  Erledigt: `JournalProgression`, `JournalEntryDefinition`, `GoalDefinition` und `JournalEntryKind` definieren persistierbare Discovery-/Lore-/Goal-Contracts von Spawn bis erster Ruin.
  Verifikation: `JournalProgressionTest` prueft Entry-Typen, Ruinenprogression, registrierte Content-Referenzen, persistierbare Keys und kleine milestone-basierte Goals.

### Offen

- ~~Journal nicht nur UI, sondern Gameplay-System:~~
  - ~~Discovery Events.~~
  - ~~Lore Pages.~~
  - ~~Biome Notes.~~
  - ~~Creature Notes.~~
  - ~~Structure Notes.~~
  - ~~Recipe Unlock History.~~
  - ~~Map Fragments.~~
  Erledigt: Journal Entries enthalten Discovery-Events, referenzierte Content-Keys, UI-Feedback, Server-Event-Contract und PlayerSave-Key.
  Verifikation: `JournalProgressionTest.defaultEntriesCoverP94JournalShapesAndRuinProgression`.
- ~~Ruinen-Progression definieren:~~
  - ~~Ancient Fragment.~~
  - ~~Ruin Key.~~
  - ~~Ruin Seal.~~
  - ~~Lost Charm.~~
  - ~~Ancient Lantern.~~
  Erledigt: Rare-Find/Lore-Eintraege und `ruinProgressionItemKeys()` decken die komplette erste Ruinenkette ab.
  Verifikation: `JournalProgressionTest.ruinProgressionItemsHaveDedicatedLoreEntries`.
- ~~Kleine Quest-/Goal-Struktur ohne MMO-Overhead planen.~~
  Erledigt: `GoalDefinition` gruppiert Milestones und Journal Entries in fuenf kleine Goals ohne eigene clientseitige Questautoritaet.
  Verifikation: `JournalProgressionTest.goalsAreSmallMilestoneBackedAndReferenceJournalEntries`.

### Akzeptanz

- ~~Exploration hat Erinnerung und Belohnung.~~
- ~~Lore erklärt die Welt, ohne Survival-Loop zu blockieren.~~
- ~~Journal-Fortschritt ist persistierbar.~~

## P9.5 Cozy-Life und Entities als Systeme

### Status 2026-05-01

- ~~🟠 In Arbeit: Cozy-Creature-Designs als Common-Contract fuer Rollen, Feeding/Friendship, Drops, Comfort, Animation, Netzwerk und Save definieren.~~
  Erledigt: `CozyLifeProgression`, `CreatureDesign`, `CreatureRole`, `CreatureDisposition` und `CreatureFriendshipRules` definieren Creature-Rollen, Feeding/Friendship-Limits, Comfort, Hint-/Resource-Hooks und Server/Save/Network/Animation-Contracts.
  Verifikation: `CozyLifeProgressionTest`, `CreatureFriendshipRulesTest`, `EntityDropsTest` und `ContentTagRegistryTest` pruefen Rollenabdeckung, registrierte Entity-/Item-/Biome-Referenzen, kleine Friendship-Schritte, Danger-Verhaeltnis, Feed-Caps und Snail-Drop-Contract.

### Offen

- ~~Tiere bekommen Design-Rollen:~~
  - ~~Atmosphäre.~~
  - ~~Hinweisgeber.~~
  - ~~Ressource.~~
  - ~~Base-Comfort.~~
  - ~~Gefahr nur selten.~~
  Erledigt: `CreatureRole` deckt Atmosphere, Hint Giver, Resource, Base Comfort, Friendship und Rare Danger ab.
  Verifikation: `CozyLifeProgressionTest.defaultCreaturesCoverP95RolesAndCoreAlphaCreatures`.
- ~~Feeding/Friendship-Regeln planen.~~
  Erledigt: Feedable Creatures haben Lieblingsitems, serverseitige Cooldown-Vertraege, 4 Block Reach, maximal drei akzeptierte Feedings pro Tag und maximal drei Trust-Schritte.
  Verifikation: `CreatureFriendshipRulesTest.feedableCreaturesAcceptOnlyFavoriteFoodAndStayDailyCapped` und `CozyLifeProgressionTest.cozyIdentityOutweighsDangerAndFeedableCreaturesStaySmall`.
- ~~EntityDrops und friedliche Interaktionen balancen.~~
  Erledigt: Resource-Rollen sind explizit, Moss-Snail-Drops bleiben im Resource-Contract, friedliche Tiere bekommen No-Kill/Observe/Pet/Hint-Vertraege.
  Verifikation: `CozyLifeProgressionTest.mossSnailDropsStayInsideItsPeacefulResourceContract`.
- ~~Keine Tiermechanik darf zum unangenehmen Grind werden.~~
  Erledigt: Friendship bleibt klein, Save speichert nur beruehrte Trust/Cooldown-Fakten, keine Zucht-/Farm-Pflicht im Alpha-Contract.
  Verifikation: `CozyLifeProgressionTest.creatureDesignsAreUniqueOrderedAndBoundedForAlpha`.

### Akzeptanz

- ~~Kreaturen wirken lebendig und nützlich.~~
- ~~Cozy-Identität bleibt stärker als Combat.~~
- ~~Entity-Design ist mit Animation/Networking/Save abgestimmt.~~

## P9.6 Status Effects und Biome-/Comfort-Modifier

### Status 2026-05-01

- ~~🔴 In Arbeit: Status Effects als Common-Gameplay-Contract fuer Hazards, Biome, Rested/Cozy-Boni und spaetere HUD/Save-Anbindung definieren.~~
  Erledigt: `common.gameplay.status` definiert `StatusEffectSystem`, `StatusEffectType`, `StatusEffectDefinition`, `StatusEffectState`, `StatusEffectModifiers`, Tick-Pulses und Save-State fuer burning, chilled, wet, rested, cozy und poison.
  Verifikation: `./gradlew :common:test --tests dev.voxelgame.common.gameplay.status.StatusEffectSystemTest -PadventuraTestRunId=status_effect_common_2 --no-daemon --max-workers=1`.
- ~~🟠 PlayerSave-Persistenz fuer StatusEffects anbinden.~~
  Erledigt: 2026-05-02 - `PlayerSave` und `PlayerSaveCodec` schreiben/lesen `StatusEffectSaveState`; geladene StatusEffects bleiben ueber `ServerPlayerSurvivalState.statusEffectSaveStates()` im Disconnect-Snapshot erhalten.
  Verifikation: `./gradlew :server:test --tests dev.voxelgame.server.save.PlayerSaveCodecTest -PadventuraTestRunId=status_effect_save_codec_2 --no-daemon --max-workers=1` und `./gradlew :server:test --tests dev.voxelgame.server.net.ServerConnectionHandlerTest.loginLoadsPlayerSaveAndDisconnectWritesLatestSnapshot -PadventuraTestRunId=status_effect_server_snapshot_4 --no-daemon --max-workers=1`.
- ~~🟠 Environment-StatusEffect-Regeln als Common-Contract und serverseitige Event-Smokes absichern.~~
  Erledigt: 2026-05-02 - `StatusEffectEnvironmentRules` schneidet Water/Hot/Cold/Frost/Comfort als kleine Common-Regel; Server-Smokes pruefen Wet, Burning, Cozy, Rested und expired Events.
  Verifikation: `./gradlew buildGame --no-daemon --max-workers=1`.

### Offen

- ~~Serverautoritative Anwendung anbinden: Hot/Hazard-Blocks -> burning, Cold/Frost-Biome -> chilled, Water -> wet, Sleep/Comfort -> rested/cozy; Poison bleibt als definierter Adventure-Danger ohne aktive Alpha-Quelle.~~
  Erledigt: 2026-05-02 - `ServerConnectionHandler` wendet StatusEffects aus autoritativer Welt-/Biome-/Comfort-/Sleep-Logik an; Hot-Hazards laufen ueber burning-Puls statt parallelem Direktschaden.
  Verifikation: `./gradlew :server:test --tests dev.voxelgame.server.net.ServerConnectionHandlerTest.playerMoveAppliesWetStatusEventFromAuthoritativeWaterState --tests dev.voxelgame.server.net.ServerConnectionHandlerTest.loginLoadsPlayerSaveAndDisconnectWritesLatestSnapshot --no-daemon --max-workers=1`.
- ~~StatusEffects fuer World-/Entity-State pruefen, falls spaetere Nicht-Spieler-Effekte persistieren muessen.~~
  Erledigt: 2026-05-02 - Alpha-StatusEffects sind player-owned und bleiben in `PlayerSave`; World-/Entity-Persistenz wird erst bei konkreten Nicht-Spieler-Effekten als eigener Owner-Contract benoetigt.
  Verifikation: `PlayerSaveCodecTest` und `ServerConnectionHandlerTest.loginLoadsPlayerSaveAndDisconnectWritesLatestSnapshot`.
- ~~`GameplayEvent`/HUD/Audio Feedback fuer applied/refreshed/expired definieren.~~
  Erledigt: 2026-05-02 - `GameplayEvent.StatusEffectChanged` ist im PacketCodec roundtrippbar; `GameplayEventFeedback` mapped cozy/rested als Comfort, wet als Info und negative Effekte als Warning. Status-spezifische Audio-Cues bleiben bewusst leer, bis der Audio-Owner konkrete Cues liefert.
  Verifikation: `./gradlew :common:test --tests dev.voxelgame.common.gameplay.GameplayEventTest --tests dev.voxelgame.common.net.PacketCodecTest --no-daemon --max-workers=1` und `./gradlew :client:test --tests dev.voxelgame.client.GameplayEventFeedbackTest --no-daemon --max-workers=1`.
- ~~Balancing der Modifier gegen Hunger/Stamina/Health im echten Server-Tick pruefen.~~
  Erledigt: 2026-05-02 - `ServerPlayerSurvivalState` tickt Status-Pulse, kombiniert Hunger-/Stamina-/Health-Modifier mit Comfort und persistiert/restored aktive Effekte.
  Verifikation: `./gradlew :server:test --tests dev.voxelgame.server.player.ServerPlayerSurvivalStateTest --no-daemon --max-workers=1`.

### Akzeptanz

- ~~Biome/Hazards fuehlen sich lebendig an, ohne clientseitige Autoritaet.~~
  Erledigt: 2026-05-02 - Statusquellen werden aus ServerWorld/Hazard/Biome/Water/Comfort/Sleep abgeleitet.
  Verifikation: ServerConnectionHandler-Smoke fuer autoritativen Water-State plus SurvivalState-Tests fuer burning/chilled.
- ~~Cozy/Rested belohnt Basebuilding, ohne Pflicht-Grind zu werden.~~
  Erledigt: 2026-05-02 - Cozy wird nur bei ausreichendem Comfort refreshed; Rested kommt ueber Sleep-Wake und nutzt bounded Common-Dauer/Modifier.
  Verifikation: `ServerPlayerSurvivalStateTest.statusEffectsApplyServerSidePulsesAndModifiers`.
- ~~Status Effects sind speicherbar, testbar und fuer HUD erklaerbar.~~
  Erledigt: 2026-05-02 - SaveState, PacketCodec und Client-Feedback haben fokussierte Tests.
  Verifikation: Common-/Client-/Server-Testbefehle aus P9.6.
