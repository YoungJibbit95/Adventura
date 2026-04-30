# Adventura – Gameplay TODO List

## Ziel

Adventura soll sich wie ein eigenständiges cozy Survival-Adventure anfühlen: sammeln, craften, kochen, bauen, erkunden, Tiere beobachten, Ruinen entdecken und die eigene Base gemütlicher machen.

## P0 – Core Gameplay Loop schärfen

### Stabilität / Edge Cases
- ~~Beim Wechsel auf Non-Survival-Modi (`CREATIVE`/`SPECTATOR`) nach Tod müssen Survival-Stats sauber zurückgesetzt werden.~~ ✅ Erledigt.

### Early Game verbessern
- Startgebiet klarer lesbar machen.
- Anfänger-Ressourcen besser sichtbar platzieren: twigs, pebbles, fiber, berries, mushrooms, herbs.
- bessere erste Feedback-Meldungen: erstes Item gesammelt, erstes Rezept freigeschaltet, erstes Tool gecraftet, erstes Campfire gebaut.
- erste Nacht emotional stärker machen: dunkler, aber nicht unfair; Campfire als klarer Sicherheits-/Comfort-Ort.
- kleine Startstruktur optional: verlassenes Camp, kaputte Storage Crate, altes Lagerfeuer.

### Mid Game ausbauen
- Pine Forest, Lakeside und Mushroom Grove mit klaren Gründen zum Erkunden versehen.
- Resin, Clay, Copper und Glow Crystal stärker in Recipes einbinden.
- Cooking Pot, Workbench und Forge als echte Progressionsstationen einführen.
- Ruinen sollen Progression, Lore und seltene Items liefern.

### Late Game definieren
- Iron Progression vollständig machen.
- Crystal Progression einführen.
- Ancient Fragment sinnvoll nutzen.
- Ruin Key / Ruin Seal System entwickeln.
- besondere Ruinen und seltene Loot-Chests einführen.

## P1 – Items und Ressourcen

### Naming Cleanup
- `berries` vs `wild_berries`, `clay` vs `clay_lump`, `raw_copper` vs `copper_ore`, `raw_iron` vs `iron_ore`, `planks` vs `wooden_plank` vereinheitlichen.
- alte Keys als Aliase behalten.

### Neue Basic Resources
- `dry_grass`: Firestarter, Sleeping Mat, einfache Deko.
- `bark_strip`: Handles, Repair, rustic decor.
- `charcoal`: Fuel, Lanterns, Forge helper.
- `clay_bowl`: Container für Soup/Stew.
- `clay_pot`: Flower Pot, Planter, Cooking Pot precursor.
- `copper_ingot`: Copper Tools.
- `iron_ingot`: Iron Tools und Forge Progression.
- `glow_crystal`: Glow Lantern, Crystal Tools.
- `ancient_fragment`: Ruin Progression.
- `cloth`: Rugs, Sleeping Mat Upgrade, Furniture.
- `leather_strip`: Tool grips, late recipes.
- `honey`: Sweet food, healing snacks.
- `water_container`: Tea, soup, cooking recipes.

## P2 – Food und Cooking Gameplay

### Neue Foods
- cooked berries, roasted mushroom, mushroom stew, herb soup, berry jam, honey snack, calming tea, hearty stew.

### Food-Balancing
- rohes Essen ist okay, aber schwach.
- gekochtes Essen ist effizienter.
- Stews/Soups sind Midgame-Staple-Foods.
- Tea/Honey haben kleine Utility-Effekte.
- Hearty Stew ist Late-Game-Food.

### Cooking Stations
- Campfire: early foods, roasted foods, einfache Rezepte.
- Cooking Pot: soups, stews, tea, jam.
- Forge: iron, late metals, stärkeres progression gate.

## P3 – Comfort-System vertiefen

- Comfort soll Basebuilding belohnen, aber nicht erzwingen.
- Quellen: active campfire, lanterns, rugs, chairs, tables, flower pots, bookshelf, sleeping mat, shelter, friendly animals, storage crate, garden fence.
- Effekte: Hunger sinkt langsamer, Stamina regeneriert schneller, Health regeneriert etwas besser, Sleep funktioniert besser, Base fühlt sich sicherer an.
- ~~Comfort nach Respawn zurücksetzen, damit kein alter Base-Buff ins neue Leben geleakt wird.~~ ✅ Erledigt.
- Offene Aufgaben: Boni besser erklären, Comfort-Cap pro Progression erhöhen, Shelter-Erkennung verbessern, Friendly Animal Comfort stärker nutzen, Furniture-Tooltips mit Comfort-Wert anzeigen.

## P4 – Tool Progression

- Tool-Tiers: Stone, Copper, Iron, Crystal.
- Iron Tools vollständig einführen.
- Crystal Tools vollständig einführen.
- Repair-System einbauen.
- Bonus Drops pro Tool Type.
- bessere Mining Speeds.
- härtere Late-Game Nodes.
- Repair: Stone mit Pebble, Copper mit Copper Ingot, Iron mit Iron Ingot, Crystal mit Glow Crystal.
- Bonus Drops: Knife mehr Fiber/Herbs/Resin, Axe mehr Bark/Resin/Logs, Pickaxe bessere Ore-Ausbeute, Crystal Knife seltene Pflanzen.

## P5 – Biome Gameplay

### Cozy Meadow
Startbiom polieren, mehr Blumenvarianten, bessere Berry-/Fiber-Verteilung, kleine Campsites, mehr Hasen/Schafe.

### Pine Forest
Resin Tree stärker nutzen, Bark Strip Harvesting, mehr Pilze, mehr Holztypen, kleine Cabins, Boar-Verhalten ausbauen.

### Mushroom Grove
Glow Mushroom Blocks, Glow Crystal Nodes, Spore Particles, Moss Snails, seltene Kräuter, Mushroom Circle Structures.

### Lakeside
Clay Deposits, Reeds, Water Container, Cooking-Zutaten, Lakeside Shack, Fishing später.

### Old Ruins
Ruin Bricks, Ancient Tiles, Ancient Fragments, Ruin Loot, Lore Notes, Ruin Key Progression.

### Highlands
stärkere Ore-Verteilung, Mine Entrances, Copper/Iron Progression, alte Watchtowers, Wind Ambience.

### Frost Peaks
Crystal Nodes, rare herbs, Schnee-/Eisblöcke, Frozen Shrine, Cold-System optional.

## P6 – Structures, Loot und Lore

- Structures: cozy campsite, abandoned cabin, small ruin, old watchtower, mushroom circle, hidden well, broken bridge, old mine entrance, lakeside shack, ruined market stall, frozen shrine, ancient gateway.
- Loot Tables: common_nature, campsite, ruin_common, ruin_rare, old_mine, mushroom_grove, village_market, frost_rare.
- Lore/Journal: old notes, map fragments, ancient coins, lost charms, ruin keys, discovered biomes, structures and recipes.

## P7 – Tiere und Cozy-Life

- Sheep: mit berries anlocken, später scheren, cloth/wool source.
- Bunny: flieht, kann zu hidden herbs führen, keine Drops.
- Moss Snail: seltene moss/slime drops, Mushroom Grove identity.
- Little Boar: findet mushrooms, neutral, mit mushroom lockbar.
- Firefly Swarm: nachts sichtbar, zeigt besondere Orte an, Glow Particles verbessern.

## Akzeptanzkriterien

- Spieler hat in jedem Biom einen klaren Grund zu bleiben.
- Early Game führt natürlich zu Campfire, Storage und erster Nahrung.
- Mid Game führt zu Clay, Resin, Copper und Cooking Pot.
- Late Game führt zu Iron, Crystal, Ruins und Lore.
- Basebuilding hat spürbaren Nutzen durch Comfort.
