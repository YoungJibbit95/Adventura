# Adventura Gameplay And Feature Plan

Implementation status:

- Started Phase 1 as a vertical slice.
- Added station/category/unlock metadata for recipes while keeping old ingredient-list crafting compatible.
- Added first Campfire-gated cooking/smelting recipes, more cozy resources, copper tools, mushroom/clay/crystal world resources and server/client station checks.
- Added Campfire Fuel V1: inactive, active and burned-out states, fuel validation, timed burn-out, server tick updates and active-fire-only cooking station checks.
- Added Storage Crate V1: right-click open UI, local singleplayer crate storage, server-validated multiplayer transfer packets, inventory sync and in-memory crate contents.
- Added Storage Transfer V1.5: source slot, optional target slot, count and transaction id packets with server-side stack validation.
- Added Crafting Intent V1.5: recipe count, optional station position, server-side station reach/type validation and atomic count crafting.
- Added BlockInteract server coverage for crate open, harvest cooldown/reach validation and campfire fuel consumption.
- Added EntityInteract V1 packet and server validation for known reachable entities.
- Added EntityInteract FEED V1: server consumes food and updates ambient entity health.
- Added fed-animal follow response: FEED marks ambient entities as FOLLOW and moves them toward the feeding player.
- Added Ambient Flee V1: timid ambient entities flee nearby players on the server tick while fed FOLLOW targets keep priority.
- Added Ambient Entity Tick V1: `EntitySnapshot` state keys and server-tick movement broadcasts.
- Added Comfort Sync V1: common comfort scan/cap rules, server-computed `PlayerStatsSnapshot` comfort and HUD display.
- Added Server PlayerSurvivalState V1: server-owned health/hunger/stamina/breath snapshot state with comfort effects.
- Added server comfort security coverage: throttled comfort scans and forged client stat snapshots are ignored.
- Added StorageOpenRequest packet so crate opening is an explicit block-pos intent instead of only generic BlockInteract.
- Added Sleep Intent V1: sleeping mat block/item/recipe, `SleepRequest` packet and server night/reach validation.
- Added Sleep Safety V1: server sleep checks now require comfort, simple shelter, no nearby danger entity and all online same-world players ready before advancing time.
- Added Cooking Intent V1: `CookRequest` station/input-slot packet, client send path, active-campfire validation and delayed server output.
- Added Tool Mining Gate V1: item `toolLevel`/`toolSpeed`, ore `requiredToolLevel`, client/local hints and server-side low-tier harvest rejection.
- Added Resin Harvest V1: Pine logs expose a server-validated `BlockInteract` resin harvest with cooldown coverage.
- Added Biome Resource Distribution coverage for deterministic pine, lakeside clay and mushroom-grove resource identity.
- Added packet roundtrip coverage for all currently implemented client intent packets.
- Replaced the previous asset sheet wiring with the new `ui_hud_sheet.png`, `blocks_tiles_sheet.png`, `tools_weapons_sheet.png`, `nature_food_sheet.png` and `ores_materials_sheet.png` sheets.
- Added UI asset pass V1: HUD half-icons, asset-backed slot frames, hand/item swing, block break progress overlay, pickup pop feedback, budgeted preview chunk generation, entity debug hitboxes, improved voxel entity/player models and a first shader bloom/glow toggle.
- Added Crafting UI filters: category chips for All/Tools/Food/Building/Decor/Adventure plus a craftable-only toggle.
- Added selected hotbar slot pulse polish.
- Added inventory/storage slot hover hints for stack count, food/heal values and durability.
- Added current biome label to the normal HUD.
- Added Crafting UI search field with keyboard input, backspace, clear button and recipe/ingredient matching.
- Added red missing-ingredient feedback in Crafting UI recipe rows and preview slots.
- Added Crafting UI station requirement lines and locked-recipe presentation for unmet unlock metadata.
- Added backpack sort button in the Crafting inventory panel.
- Added right-click split-stack transfer for the storage screen.
- Added tooltip category/description/placeable details to selected-item and slot hover text.
- Added tooltip tool level, rarity and decor comfort values plus a local HUD day/time readout.
- Added inventory shift-click quick move, polished durability bar state colors and a labeled comfort HUD meter.
- Added Creative-only inventory trash mode plus biome-based HUD temperature readout.
- Added gameplay audio hooks for footsteps, campfire crackle, ambience and craft-fail feedback.
- Fixed Hotbar slot backgrounds by removing broken chest-mapped slot sprites and using stable code-drawn slot panels.
- Added Entity Model Renderer V1: named model parts, rotations, entity culling stats and specific procedural models for bunny, snail, boar, crawler and grazers.
- Added Particle System V1: CPU billboards, particle shader pass, block-break debris, harvest sparkle and F3 particle stats.
- Added Campfire Particle V1: nearby active campfires emit budgeted smoke and sparks from client-side world state.
- Added Firefly Particle V1: firefly swarm and mire wisp snapshots emit lightweight client-side glow particles.
- Added Ambient Particle V1.5: cooking steam, leaf drift, water splash transitions and glow spores use capped client-side sources.
- Added Feedback Log V1 and no-op audio cue coverage for collect, break, craft-success, inventory and water-splash hooks.
- Added Feedback V1.5: far-target hints, required-tool hints, station recipe-unlock hints and local singleplayer comfort feedback.
- Added UI Scale and Recipe Discovery Feedback V1: runtime HUD/chat/feedback scaling plus newly discovered ingredient recipe messages.
- Added Generated Loot Crate V1: structure loot markers fill server crates deterministically once and persist after transfers or break/replace.
- Fixed block face seam rendering by insetting atlas UVs and keeping closed cube faces opaque when source sprites contain transparent edge pixels.
- Added data-driven block render material tables so terrain shader color, alpha, emissive glow, animated fluid and face-gap behavior no longer depend on hardcoded shader ID lists.
- Added Client Entity Interpolation V1: client keeps previous/current snapshots with a short render delay, smooths position/yaw and keeps server authority unchanged.
- Added Client Entity Interaction Targeting V1: right-click can aim at interpolated entities and send server-authoritative `EntityInteract(entityId, selectedSlot, action)` packets.
- Added Chunk Render Layer Split V1: SOLID, CUTOUT and TRANSLUCENT chunk meshes now build, upload, render and report debug stats separately.
- Reduced terrain shader UV uniform tables to the validated block shader range instead of uploading unused 256-entry tables.
- Added Death/HUD UI Cleanup V1: survival death state with respawn plus cleaner asset-backed stat strips for health, hunger and energy.
- Added Inventory Drag Stack V1: compact inventory drag/drop with safe stack merge, empty-slot move and swap behavior.
- Added Survival Loop Test Coverage V1: inventory overflow, food effects, crafting fit/fail, comfort, tool damage and mining gates are covered by unit tests.
- Fixed Local Drop Pickup and Minimal HUD V1: local block drops now use the pre-break block type, mining feedback is softer and survival meters no longer draw extra backplates.
- Added Biome Resource Identity V1: biome-specific surface gatherables now cover meadows, forests, lakes, mire, mushroom groves, highlands, ruins, dunes and frost peaks with registry-backed drop coverage.
- Java tests still need a local JDK/JAVA_HOME before they can be executed.

Stand: Die Codebase hat bereits `common`, `client`, `server`, `launcher`, `tools`, zentrale Registries (`Items`, `Blocks`, `Biomes`), einfache `CraftingRecipe`s, Inventory, Hunger/Stamina/Breath, Tool-Durability, Drops, Worldgen, Biome, Structures, einfache Entity-Snapshots, UI/HUD, Netty-Pakete und erste Server-Validierung fuer `BlockAction` und `BlockInteract`.

Leitlinien:

- Adventura bleibt ein eigenes cozy Survival-Adventure, keine Minecraft-Kopie.
- Keine komplette Neuarchitektur. Bestehende Patterns werden erweitert: Registry, Records, `InteractionRules`, `GamePacket`, `ServerConnectionHandler`, `OverworldGenerator`, `GameClient`.
- Jede Gameplay-Aktion ist ein Intent vom Client und wird serverseitig validiert.
- Neue Features werden in kleinen, testbaren PRs umgesetzt.
- Singleplayer darf lokal laufen, soll aber moeglichst dieselben Common-Regeln wie Multiplayer nutzen.
- Erledigte TODO-Punkte werden per Markdown-Strikethrough markiert, nicht still geloescht.

## 1. Core Gameplay Loop

Finaler Loop:

1. Spieler spawnt in einer Cozy Meadow mit weichem Terrain, Blumen, Beeren, Hasen und sichtbaren Startressourcen.
2. Spieler sammelt `twig`, `pebble`, `fiber`, `wild_berries`, `mushroom`, `wild_herbs`, `resin`, `clay_lump`.
3. Spieler craftet `stone_knife`, `stone_axe`, `stone_pickaxe`, `simple_campfire`.
4. Spieler baut einen kleinen sicheren Lagerplatz mit `storage_crate`, `campfire`, `sleeping_mat`, `garden_fence`.
5. Spieler kocht erstes Essen am Campfire.
6. Spieler erkundet neue Biome fuer besondere Ressourcen.
7. Spieler findet Ruinen, Loot, Lore-Fragmente und seltene Materialien.
8. Spieler schaltet bessere Tools, Rezepte und Comfort-Deko frei.
9. Spieler verbessert Base, Komfort und Exploration-Optionen.
10. Spieler bereitet sich auf weitere Abenteuer, tiefere Ruinen und seltene Biome vor.

### Early Game

Spielerziele:

- Orientierung in Cozy Meadow.
- Erste Ressourcen vom Boden und aus Pflanzen sammeln.
- Stone Knife, Stone Axe, Campfire, Storage Crate bauen.
- Hunger mit Beeren, Pilzen und einfachen Cooked Foods stabilisieren.

Neue Ressourcen:

- `twig`, `pebble`, `fiber`, `wild_berries`, `wild_herbs`, `mushroom`, `dry_grass`, `bark_strip`.

Neue Rezepte:

- `twig + fiber -> simple_rope`
- `twig + pebble + fiber -> stone_knife`
- `twig + pebble + simple_rope -> stone_axe`
- `pebble + pebble + twig + fiber -> stone_pickaxe`
- `log -> wooden_plank`
- `wooden_plank + fiber -> storage_crate`
- `stone + twig -> simple_campfire`
- `berries + campfire -> cooked_berries`

Gefahren oder Herausforderungen:

- Hunger sinkt langsam.
- Nachts ist Sicht schlechter.
- Falsches Tool ist langsam.
- Inventory ist frueh begrenzt.

Belohnungen:

- Erste Tools.
- Sicherer Lagerplatz.
- Mehr Stamina-Regeneration in Comfort-Zone.
- Erste Rezeptfreischaltungen.

Code-Erweiterungen:

- `Items`: neue Basic Resources.
- `Blocks`: Harvestable Blocks fuer `fiber_grass`, `berry_bush`, `mushroom_cluster`.
- `InteractionRules`: Harvest-Interaktionen erweitern.
- `CraftingRecipes`: Early Recipes.
- `GameClient`: bessere Tooltips und Feedback.
- `ServerConnectionHandler`: Server-Validierung fuer Harvest/Crafting beibehalten.

### Mid Game

Spielerziele:

- Pine Forest, Lakeside und Mushroom Grove finden.
- Resin, Clay, Copper Ore, Glow Spores/Glow Crystal sammeln.
- Campfire/Cooking Pot nutzen.
- Base mit Deko und Storage ausbauen.
- Erste Ruinen finden.

Neue Ressourcen:

- `resin`, `clay_lump`, `copper_ore`, `charcoal`, `clay_bowl`, `clay_pot`, `cloth`, `glow_crystal`.

Neue Rezepte:

- `clay_lump + campfire -> clay_bowl`
- `clay_lump + campfire -> clay_pot`
- `mushroom + clay_bowl + campfire -> mushroom_stew`
- `wild_herbs + clay_bowl + campfire -> herb_soup`
- `charcoal + resin + glow_crystal -> cozy_lantern`
- `copper_ore + campfire -> copper_ingot`
- `copper_ingot + stick + simple_rope -> copper_axe`

Gefahren oder Herausforderungen:

- Weiter entfernte Biome, laengere Wege.
- Old Ruins haben dunklere Zonen.
- Campfire braucht Fuel.
- Copper braucht Station/Fuel statt Sofort-Craft.

Belohnungen:

- Bessere Tools.
- Warmere Base.
- Comfort-Boni.
- Recipe Book Unlocks.
- Erste Lore Notes.

Code-Erweiterungen:

- `RecipeType` und `stationType`.
- `BlockState` oder vorerst `CampfireState` als minimaler BlockEntity-Prototyp.
- `LootTable` fuer Ruinen und Campsites.
- `BiomeType` optional um Resource/Spawn-Listen erweitern.

### Late Game

Spielerziele:

- Old Ruins, Highlands, Frost Peaks erkunden.
- Iron und Ancient Fragments finden.
- Ruin Key zusammensetzen.
- Bessere Workstations bauen.
- Seltene Rezepte durch Lore freischalten.

Neue Ressourcen:

- `iron_ore`, `iron_ingot`, `ancient_fragment`, `ancient_coin`, `ruin_key`, `map_fragment`, `lost_charm`.

Neue Rezepte:

- `iron_ore + forge -> iron_ingot`
- `iron_ingot + copper_ingot + handle -> iron_pickaxe`
- `ancient_fragment + glow_crystal -> ruin_key`
- `map_fragment + charcoal -> biome_compass` spaeter.

Gefahren oder Herausforderungen:

- Kälte in Frost Peaks optional.
- Dunkle Ruinen brauchen Licht.
- Seltene Loot-Container sind persistent.
- Hoehere Tool-Level benoetigt.

Belohnungen:

- Iron Tools.
- Ruin Access.
- Mehr Storage/Decor.
- Rare Comfort Items.
- Journal-Eintraege.

Code-Erweiterungen:

- Tool-Level und requiredToolLevel.
- Loot Persistence.
- Lore/Journal State.
- Server-authoritative Player Progression.

### Cozy-Endgame

Spielerziele:

- Base dekorieren und Comfort optimieren.
- Village/Market Stall restaurieren.
- Tiere beobachten/interagieren.
- Alle Biome/Lore-Fragmente sammeln.
- Seltene Deko und Glow Items craften.

Neue Ressourcen:

- `cloth`, `leather_strip`, `glowing_seed`, `lost_charm`, seltene Dyes.

Neue Rezepte:

- `bookshelf`, `sleeping_mat`, `glow_lantern`, `cozy_banner`, `animal_treat`.

Gefahren oder Herausforderungen:

- Wenig Hardcore. Fokus auf Sammlung, Komfort, Exploration und Completion.
- Seltene Spawn-Chancen und Biome finden.

Belohnungen:

- Comfort Cap erreichen.
- Vollstaendiges Journal.
- Friendly Animal Boni.
- Warme, lebendige Base.

Code-Erweiterungen:

- Comfort-System.
- Journal/Discovery-System.
- Entity Interaction.
- UI fuer Collections/Guide.

## 2. Item- und Ressourcenplan

Legende fuer Flags: P = placeable, F = food, T = tool. Code-Aenderungen sind bewusst knapp gehalten, damit die Umsetzung in kleinen PRs moeglich bleibt.

| Key | Anzeigename | Kategorie | Stack | Funktion | Rezept | Quelle | Balance | Flags | Code |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `voxel:twig` | Twig | Basic | 64 | Fruehes Crafting, Fuel klein | none | Boden, Buesche, Tree Stump | sehr haeufig | - | existiert, Worldgen-Drops erweitern |
| `voxel:pebble` | Pebble | Basic | 64 | Stone Tools | none | Small Stone, Gravel, Meadows | sehr haeufig | - | existiert, DropTable spaeter |
| `voxel:fiber` | Fiber | Basic | 64 | Rope, Textil, Decor | none | Fiber Grass, Wild Grass mit Knife | haeufig | - | existiert, Knife-Bonus sichern |
| `voxel:wild_berries` | Wild Berries | Basic | 16 | Roh essen, Kochen | none | Berry Bush | haeufig, seasonal spaeter | F | alias/rename zu `berries` planen |
| `voxel:wild_herbs` | Wild Herbs | Basic | 32 | Soup, Tea, Healing | none | Herb Patch, Herb Planter | mittel | F | existiert |
| `voxel:mushroom` | Mushroom | Basic | 32 | Food, Stew | none | Mushroom Cluster, Grove | mittel | F | alias zu `red_mushroom` klaeren |
| `voxel:resin` | Resin | Basic | 32 | Glue, Lantern, Tables | Knife an Resin Tree | Pine Forest | mittel | - | existiert, Harvest Block fehlt |
| `voxel:clay_lump` | Clay Lump | Basic | 64 | Pottery | shovel clay deposit | Lakeside, Clay Bank | mittel | - | alias zu `clay` planen |
| `voxel:feather` | Feather | Basic | 32 | Charms, Arrows spaeter | none | Nests, rare Boar/Bird loot spaeter | selten | - | alias zu `feathers` |
| `voxel:dry_grass` | Dry Grass | Basic | 64 | Firestarter, Mat | Fiber Grass dry variant | Meadow/Pine edge | haeufig | - | neues Item |
| `voxel:bark_strip` | Bark Strip | Basic | 64 | Handle, Leather alt | Knife on logs/resin tree | Pine/Skyroot | mittel | - | neues interaction result |
| `voxel:simple_rope` | Simple Rope | Processed | 32 | Tools, Fence, Rug | twig + fiber | Crafting | early gate | - | existiert |
| `voxel:wooden_plank` | Wooden Plank | Processed | 64 | Building, Decor | log -> 4 | Logs | core material | P | alias zu `skyroot_planks` |
| `voxel:charcoal` | Charcoal | Processed | 64 | Fuel, Lantern | log + campfire | Campfire | mid fuel | - | neues Item |
| `voxel:clay_bowl` | Clay Bowl | Processed | 16 | Soup container | clay_lump + campfire | Campfire | reusable spaeter | - | neues Item |
| `voxel:clay_pot` | Clay Pot | Processed | 16 | Flower Pot, Planter | clay_lump + campfire | Campfire/Kiln | decor gate | P | item/block mapping |
| `voxel:copper_ore` | Copper Ore | Processed | 64 | Smelting | mine copper ore | Highlands/Caves | mid resource | - | currently `raw_copper`, align naming |
| `voxel:copper_ingot` | Copper Ingot | Processed | 64 | Copper Tools | copper_ore + fire | Campfire/Forge | mid gate | - | neues Item |
| `voxel:iron_ore` | Iron Ore | Processed | 64 | Smelting | mine iron ore | Highlands/Old Mine | late resource | - | currently `raw_iron`, align naming |
| `voxel:iron_ingot` | Iron Ingot | Processed | 64 | Iron Tools | iron_ore + forge | Forge | late gate | - | neues Item |
| `voxel:glow_crystal` | Glow Crystal | Processed | 32 | Glow Lantern, progression | mine crystal node | Mushroom/Frost | selten | - | neues Item/Block |
| `voxel:ancient_fragment` | Ancient Fragment | Processed | 16 | Ruin Key, lore | ruins loot | Old Ruins | selten | - | neues Item/Loot |
| `voxel:cloth` | Cloth | Processed | 32 | Rugs, Sleeping Mat | fiber + loom | Sheep/Fiber | mid cozy | - | neues Item |
| `voxel:leather_strip` | Leather Strip | Processed | 32 | Tool grips | boar drop/trade | Forest/Village | mittel | - | neues Item |
| `voxel:berries` | Berries | Food | 16 | +3 hunger | none | Berry Bush | early food | F | existiert |
| `voxel:cooked_berries` | Cooked Berries | Food | 16 | +4 hunger, +1 heal | berries + campfire | Cooking | better than raw | F | neues Item/RecipeType cooking |
| `voxel:mushroom_stew` | Mushroom Stew | Food | 8 | +7 hunger, +2 heal | mushroom + bowl + campfire | Cooking | mid staple | F | neues Item |
| `voxel:herb_soup` | Herb Soup | Food | 8 | +5 hunger, stamina regen | herbs + bowl + campfire | Cooking | utility food | F | neues Item |
| `voxel:berry_jam` | Berry Jam | Food | 8 | +6 hunger, comfort snack | berries + pot + fuel | Cooking Pot | cozy food | F | neues Item |
| `voxel:roasted_mushroom` | Roasted Mushroom | Food | 16 | +4 hunger | mushroom + campfire | Cooking | early alternate | F | neues Item |
| `voxel:honey_snack` | Honey Snack | Food | 16 | +5 hunger, +2 heal | berries + honey | Cooking | rare sweet | F | neues Item, honey source spaeter |
| `voxel:calming_tea` | Calming Tea | Food | 8 | lowers night stress | herbs + water | Campfire/Pot | comfort utility | F | neues Item/effect |
| `voxel:hearty_stew` | Hearty Stew | Food | 8 | +10 hunger, regen | mushroom + herbs + bowl | Cooking Pot | late food | F | neues Item |
| `voxel:stone_knife` | Stone Knife | Tool | 1 | Plants, resin, bonus fiber | twig + pebble + fiber | Crafting | fast but low durability | T | existiert |
| `voxel:stone_axe` | Stone Axe | Tool | 1 | Wood | twig + pebble + rope | Crafting | early wood gate | T | existiert, recipe tune |
| `voxel:stone_pickaxe` | Stone Pickaxe | Tool | 1 | Stone, coal, copper | pebbles + twig + fiber | Crafting | early mining | T | existiert, recipe tune |
| `voxel:copper_axe` | Copper Axe | Tool | 1 | Faster wood, less durability use | copper_ingot + stick + rope | Workbench | mid tool | T | neues ItemType fields |
| `voxel:copper_pickaxe` | Copper Pickaxe | Tool | 1 | Copper/iron mining | copper_ingot + pebble + stick | Workbench | mid mining | T | neues Item |
| `voxel:iron_axe` | Iron Axe | Tool | 1 | late wood/resin | iron_ingot + handle | Workbench | durable | T | neues Item |
| `voxel:iron_pickaxe` | Iron Pickaxe | Tool | 1 | iron/crystal | iron_ingot + handle | Workbench | late mining | T | neues Item |
| `voxel:crystal_knife` | Crystal Knife | Tool | 1 | rare plants, glow harvest | glow_crystal + handle | Ruin/Workbench | rare utility | T | neues Item |
| `voxel:cozy_planks` | Cozy Planks | Building | 64 | building skin | log/planks + resin | Workbench | decor variant | P | block/item |
| `voxel:mossy_path` | Mossy Path | Building | 64 | path, base decor | mossy stone + fiber | Crafting | early decor | P | existiert |
| `voxel:garden_fence` | Garden Fence | Building | 64 | enclosure | sticks + rope | Crafting | animal/base | P | existiert |
| `voxel:wooden_chair` | Wooden Chair | Building | 64 | comfort | planks + twig | Crafting | comfort +2 | P | existiert |
| `voxel:small_table` | Small Table | Building | 64 | comfort, station req spaeter | planks + resin | Crafting | comfort +2 | P | existiert |
| `voxel:storage_crate` | Storage Crate | Building | 64 | storage block | planks + fiber | Crafting | core base | P | exists, BlockEntity needed |
| `voxel:cozy_lantern` | Cozy Lantern | Building | 64 | light + comfort | charcoal + resin + crystal | Workbench | mid decor | P | alias to lantern or new |
| `voxel:flower_pot` | Flower Pot | Building | 64 | comfort, plant display | clay_pot + flower | Crafting | comfort +1 | P | existiert |
| `voxel:woven_rug` | Woven Rug | Building | 64 | comfort floor | planks + fiber | Crafting/Loom | comfort +3 | P | existiert |
| `voxel:bookshelf` | Bookshelf | Building | 64 | lore/comfort | planks + notes | Workbench | comfort +3 | P | neues Block/Item |
| `voxel:sleeping_mat` | Sleeping Mat | Building | 16 | sleep/respawn | dry_grass + fiber now, cloth later | Crafting | core shelter | P | ~~basic block/item/packet exists~~, ~~shelter~~/respawn later |
| `voxel:old_note` | Old Note | Adventure | 16 | Lore unlock | none | ruins/chests | collectable | - | Item + Journal |
| `voxel:ancient_coin` | Ancient Coin | Adventure | 64 | trade/restoration | none | ruins/market | rare currency | - | Loot |
| `voxel:ruin_key` | Ruin Key | Adventure | 1 | opens rare ruins | fragments + crystal | Workbench | progression | - | key validation |
| `voxel:glowing_seed` | Glowing Seed | Adventure | 16 | rare plant/decor | grove loot | Mushroom Grove | rare | P | plant block |
| `voxel:map_fragment` | Map Fragment | Adventure | 16 | biome clues | towers/ruins | exploration | - | Journal/map UI |
| `voxel:lost_charm` | Lost Charm | Adventure | 1 | collection/buff | rare loot | Ruins | very rare | - | equipment slot later |

## 3. Block- und Deko-Plan

| Key | Layer | Collision | Hardness | Tool | Drops | Placeable Item | Comfort | Light | Biomes | Structures | Server Notes | Client Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `voxel:mossy_stone` | SOLID | yes | 1.6 | PICKAXE | mossy_stone | mossy_stone | 0 | 0 | Old Ruins, Mushroom | ruins | tool level later | existing |
| `voxel:soft_grass` | SOLID | yes | 0.45 | SHOVEL | dirt/fiber chance | soft_grass | 0 | 0 | Cozy Meadow | none | drop table | softer color |
| `voxel:pine_needles` | SOLID | yes | 0.35 | SHOVEL | dry_grass | pine_needles | 0 | 0 | Pine Forest | campsites | shovel faster | top texture |
| `voxel:flower_grass` | SOLID | yes | 0.45 | SHOVEL | wild_herbs chance | flower_grass | 0 | 0 | Flower Fields | none | deterministic drops | flower top |
| `voxel:mushroom_soil` | SOLID | yes | 0.55 | SHOVEL | mushroom chance | mushroom_soil | 0 | 0 | Mushroom Grove | circles | supports glow plants | muted texture |
| `voxel:clay_bank` | SOLID | yes | 0.7 | SHOVEL | clay_lump 2 | clay_bank | 0 | 0 | Lakeside | shore | harvest by shovel | wet texture |
| `voxel:shallow_water` | TRANSLUCENT | no | 100 | NONE | none | none | 0 | 0 | Lakeside | shore | not placeable early | low alpha |
| `voxel:glowing_mushroom_block` | CUTOUT/SOLID | partial | 0.4 | KNIFE | glow_spores | item later | 1 | 8 | Mushroom Grove | caves | night light | emissive tint |
| `voxel:berry_bush` | CUTOUT | no | 0 | KNIFE | berries | berry_bush | 1 | 0 | Meadow, Flower | campsites | interact harvest, cooldown | existing cross sprite |
| `voxel:herb_patch` | CUTOUT | no | 0 | KNIFE | wild_herbs | herbs item later | 1 | 0 | Meadow, Frost | none | interact harvest | new sprite |
| `voxel:fiber_grass` | CUTOUT | no | 0 | KNIFE | fiber | none | 0 | 0 | Meadow | none | knife bonus | use plant sheet |
| `voxel:resin_tree` | SOLID | yes | 2.2 | AXE/KNIFE | log/resin | sapling later | 0 | 0 | Pine Forest | none | right-click knife resin | log variant |
| `voxel:clay_deposit` | SOLID | yes | 0.8 | SHOVEL | clay_lump | none | 0 | 0 | Lakeside | shore | shovel required later | clay texture |
| `voxel:mushroom_cluster` | CUTOUT | no | 0 | KNIFE | mushroom | none | 1 | 1 | Mushroom Grove | circles | interact harvest | cross sprite |
| `voxel:glow_crystal_node` | CUTOUT/SOLID | yes | 2.4 | PICKAXE | glow_crystal | none | 1 | 10 | Frost, Grove cave | ruins | required level copper+ | emissive |
| `voxel:cozy_lantern` | CUTOUT | no | 0.2 | NONE | cozy_lantern | cozy_lantern | 3 | 14 | none | cabins, market | place valid by item | existing lantern base |
| `voxel:flower_pot` | CUTOUT | no | 0.2 | NONE | flower_pot | flower_pot | 1 | 0 | none | houses | place valid by item | existing |
| `voxel:woven_rug` | CUTOUT | no | 0.1 | NONE | woven_rug | woven_rug | 3 | 0 | none | cabins | floor only later | flat mesh later |
| `voxel:wooden_chair` | CUTOUT | yes | 0.7 | AXE | wooden_chair | wooden_chair | 2 | 0 | none | cabins | orientation later | cross/prop |
| `voxel:small_table` | CUTOUT | yes | 0.8 | AXE | small_table | small_table | 2 | 0 | none | cabins | workstation later | prop |
| `voxel:storage_crate` | SOLID | yes | 1.0 | AXE | storage_crate | storage_crate | 1 | 0 | none | camps, market | BlockEntity inventory | existing |
| `voxel:bookshelf` | SOLID | yes | 1.0 | AXE | bookshelf | bookshelf | 3 | 0 | none | cabins/ruins | unlock lore nearby | new texture |
| `voxel:sleeping_mat` | CUTOUT | no | 0.1 | NONE | sleeping_mat | sleeping_mat | 4 | 0 | none | campsites | ~~sleep intent~~ | flat sprite |
| `voxel:campfire_active` | CUTOUT | no | 0.5 | AXE | campfire | campfire | 5 | 14 | none | campsites | BlockEntity state | animated fire |
| `voxel:campfire_inactive` | CUTOUT | no | 0.5 | AXE | campfire | campfire | 2 | 0 | none | campsites | fuel interaction | no flame |
| `voxel:garden_fence` | CUTOUT | yes | 0.9 | AXE | garden_fence | garden_fence | 1 | 0 | none | villages | collision thin later | existing |
| `voxel:market_crate` | SOLID | yes | 1.0 | AXE | loot once | none/player later | 1 | 0 | none | market stall | persistent loot | crate variant |
| `voxel:ruin_brick` | SOLID | yes | 2.0 | PICKAXE | ruin_brick | ruin_brick | 0 | 0 | Old Ruins | ruins | tool level stone | new |
| `voxel:cracked_ruin_brick` | SOLID | yes | 1.4 | PICKAXE | ancient_fragment chance | cracked_ruin_brick | 0 | 0 | Old Ruins | ruins | loot drop chance | new |
| `voxel:mossy_ruin_brick` | SOLID | yes | 1.8 | PICKAXE | mossy_ruin_brick | mossy_ruin_brick | 1 | 0 | Old Ruins | ruins | drop table | new |
| `voxel:ancient_tile` | SOLID | yes | 2.2 | PICKAXE | ancient_tile | ancient_tile | 1 | 0 | Old Ruins | floors | rare block | new |
| `voxel:old_wood_beam` | SOLID | yes | 1.3 | AXE | bark_strip/log | old_wood_beam | 1 | 0 | Old Ruins | ruins/cabins | axe faster | new |

## 4. Crafting- und Rezept-System

Bestehende `CraftingRecipe` Ingredient-Listen bleiben V1-kompatibel. V2 sollte ein neuer Record sein, ohne alte Tests zu zerbrechen:

```java
record RecipeDefinition(
    String key,
    String label,
    RecipeType type,
    StationType stationType,
    List<RecipeIngredient> ingredients,
    List<RecipeResult> results,
    UnlockCondition unlockCondition,
    int craftingTimeTicks,
    int outputExperience,
    RecipeCategory category
) {}
```

Enums:

- `StationType`: `INVENTORY`, `CRAFTING_TABLE`, `CAMPFIRE`, `COOKING_POT`, `WORKBENCH`, `FORGE`, `LOOM`.
- `RecipeType`: `SHAPELESS`, `SHAPED`, `COOKING`, `SMELTING`, `WEAVING`, `CARVING`, `REPAIRING`, `UPGRADING`.
- `UnlockCondition`: `ALWAYS`, `DISCOVERED_ITEM`, `NEAR_STATION`, `FOUND_LORE_NOTE`, `BIOME_DISCOVERED`.
- `RecipeCategory`: `TOOLS`, `FOOD`, `BUILDING`, `DECOR`, `ADVENTURE`, `MATERIALS`.

Concrete recipes:

| Recipe | Ingredients | Station | Unlock | Balance | UI | Server Validation |
| --- | --- | --- | --- | --- | --- | --- |
| `simple_rope` | twig 2, fiber 3 -> rope 2 | inventory | always | turns common plants into gate material | materials tab | has ingredients, output space |
| `stone_knife` | twig 1, pebble 2, fiber 1 -> knife | inventory | always | unlocks plant efficiency | tools tab | one tool stack max |
| `stone_axe` | twig 1, pebble 2, rope 1 -> axe | inventory | discovered rope | first wood speed | tools tab | consume exact ingredients |
| `stone_pickaxe` | pebble 2, twig 1, fiber 1 -> pickaxe | inventory | discovered pebble | opens stone/copper | tools tab | no dupes on full inv |
| `wooden_planks` | log 1 -> planks 4 | inventory | discovered log | base building | building tab | stack merge |
| `storage_crate` | planks 6, fiber 2 -> crate | inventory | discovered planks | storage gate | building tab | placeable item |
| `campfire` | stone 3, twig 4 -> campfire | inventory | always | cooking and safety | survival tab | placeable item |
| `cooked_berries` | berries 2 + fuel -> cooked_berries 2 | campfire | near campfire | food upgrade | cooking UI | station active, fuel, time |
| `mushroom_stew` | mushroom 2, clay_bowl 1, fuel -> stew | campfire | discovered bowl | mid food | cooking UI | bowl consumed or returned byproduct later |
| `herb_soup` | herbs 2, clay_bowl 1, fuel -> soup | campfire | discovered herbs | stamina food | cooking UI | station + inventory |
| `honey_snack` | berries 2, honey 1 -> snack | cooking_pot | discovered honey | rare heal | food tab | honey source valid |
| `calming_tea` | herbs 2, water 1 -> tea | campfire | found lore/near pot | night utility | food tab | water container later |
| `woven_rug` | planks 1, fiber 6 -> rug | inventory/loom | discovered fiber | comfort +3 | decor tab | placeable |
| `wooden_chair` | planks 3, twig 2 -> chair | workbench | discovered planks | comfort +2 | decor tab | station nearby |
| `small_table` | planks 4, resin 1 -> table | workbench | discovered resin | comfort +2, station later | decor tab | station nearby |
| `flower_pot` | clay_pot 1, flower/herbs 1 -> pot | inventory | discovered clay_pot | comfort +1 | decor tab | tag ingredient |
| `cozy_lantern` | charcoal 1, resin 1, glow_crystal 1 -> lantern | workbench | discovered crystal | light + comfort | decor tab | consumes rare crystal |
| `copper_ingot` | copper_ore 1, fuel -> ingot | campfire/forge | discovered ore | mid progression | materials tab | fuel/time |
| `copper_axe` | copper_ingot 2, stick 1, rope 1 -> axe | workbench | discovered ingot | mid wood speed | tools tab | tool tier |
| `copper_pickaxe` | copper_ingot 2, pebble 1, stick 1 -> pickaxe | workbench | discovered ingot | iron unlock | tools tab | tool tier |
| `iron_ingot` | iron_ore 1, fuel -> ingot | forge | found forge/lore | late gate | materials tab | forge heat |
| `glow_lantern` | glow_crystal 1, lantern 1 -> glow_lantern | workbench | discovered crystal | late light | decor tab | item replacement |

Implementation order:

1. Keep `CraftingRecipe` as compatibility type.
2. Add `StationType`, `RecipeCategory`, `RecipeDefinition`.
3. Add adapter from old recipes to new recipe definitions.
4. Update UI to show station/category/locked state.
5. ~~Extend `CraftRequest` to include station position, recipe key, count.~~
6. Server validates station block, reach, inventory, fuel/time.

## 5. Survival- und Comfort-System

Comfort is a reward system, not punishment. It should make bases feel warm and useful.

Comfort sources:

| Source | Value | Notes |
| --- | --- | --- |
| campfire_active | 5 | strongest early comfort, radius 8 |
| cozy_lantern | 3 | light + nighttime comfort |
| woven_rug | 3 | floor comfort, stackable with cap |
| wooden_chair | 2 | furniture |
| small_table | 2 | furniture and workstation feel |
| flower_pot | 1 | cheap decor |
| bookshelf | 3 | lore/home comfort |
| sleeping_mat | 4 | sleep/rest |
| enclosed shelter | 4 | if enough nearby solid roof/walls |
| nearby friendly animal | 1 each, max 3 | cozy life |
| storage_crate | 1 | practical base |
| garden_fence | 1 | homestead feel, cap contribution |

Effects:

- Hunger drain multiplier: `1.0 - min(0.25, comfort * 0.0125)`.
- Stamina regen multiplier: `1.0 + min(0.40, comfort * 0.02)`.
- Health regen interval reduced by up to 30 percent.
- Night stress optional later: lower vignette/sound tension.
- ~~Sleep requires safe/comfortable area unless creative.~~

Technical plan:

- ~~Add `comfortValue` to `BlockType` or a separate `DecorComfortRegistry` first to avoid changing constructor too broadly.~~
- ~~Add `PlayerSurvivalState` server-side: health, hunger, stamina, breath, comfort, lastComfortScanTick.~~
- ~~Every 40 ticks, server scans radius 8 around player; limit checked blocks to a cube sample or Manhattan shell to protect performance.~~
- ~~Max comfort cap: 25 early, 40 later.~~
- ~~Client gets comfort through `PlayerStatsSnapshot` packet or temporary HUD sync packet.~~
- ~~UI: small comfort leaf/house meter near stamina.~~

Tests:

- ~~Comfort values sum and cap.~~
- ~~Comfort scan ignores unloaded chunks.~~
- ~~Hunger drain reduces with comfort.~~
- ~~Multiplayer client cannot fake comfort.~~

## 6. Cooking- und Campfire-System

Campfire states:

- `inactive`: no fuel, no light, comfort 2.
- `active`: has fuel, light 14, comfort 5.
- `cooking`: active plus current recipe.
- `burned_out`: fuel exhausted, optional ash/charcoal output.

Interactions:

- Right-click with `twig`, `stick`, `log`, `charcoal` adds fuel.
- Right-click with food opens Cooking UI or starts first valid campfire recipe.
- Campfire provides light and comfort.
- Campfire creates safe night area.
- Rain/water interaction can reduce fuel later.

Data:

- `fuelTime`
- `cookTime`
- `stationType`
- `activeUntil`
- `currentRecipe`
- `inputSlots`
- `outputSlot`

Server rules:

- Validate reach via `InteractionRules`.
- Validate target block is campfire.
- Validate fuel item tag.
- Validate recipe station and ingredients.
- Consume inputs only when accepted.
- Produce output once and mark recipe complete.
- Sync state to clients.

Client:

- Particle hooks: smoke, sparks, cooking steam.
- Audio hook: `CAMPFIRE`, `CRAFT`, `CRAFT_FAIL`.
- Tooltip: fuel remaining, current recipe.
- UI: 1 input, 1 fuel, 1 output for V1.

## 7. Tool-Progression

Tool types:

- `NONE`
- `KNIFE`
- `AXE`
- `PICKAXE`
- `SHOVEL`

Tool tiers:

| Tier | Level | Use |
| --- | --- | --- |
| Stone | 1 | plants, wood, stone, copper |
| Copper | 2 | faster wood/stone, iron access |
| Iron | 3 | durable, crystal access |
| Crystal | 4 | rare harvesting, glow resources, high durability |

New item properties:

- ~~`toolLevel`~~
- ~~`toolSpeed`~~
- ~~`durability`~~
- `effectiveAgainst`
- `bonusDrops`
- `repairMaterial`

Block properties:

- `requiredToolType`
- ~~`requiredToolLevel`~~
- `hardness`
- `dropTable`

Gameplay:

- ~~Wrong tool is slow for soft blocks, blocked for ores above level.~~
- ~~Correct type speeds mining.~~
- Higher tier reduces durability loss by 25 to 50 percent.
- Knife gives bonus for fiber, herbs, mushrooms, resin.
- Axe gives bonus for bark/resin/logs.
- ~~Pickaxe unlocks ore tiers.~~

Tests:

- ~~Wrong tool blocked where required.~~
- ~~Correct tool succeeds.~~
- ~~Tool breaks at durability.~~
- Drop count and drop table remain correct.
- ~~Server rejects client attempting high-tier block with low-tier tool.~~

## 8. Biome-Feature-Plan

| Biome | Terrain | Palette | Plants | Animals | Loot | Structures | Resources | Gameplay Reason | Code |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Cozy Meadow | rolling, safe, spawn | soft_grass, flower_grass | berry_bush, fiber_grass, flowers | sheep, bunny | common_nature | campsite, small cabin | twig, fiber, berries | tutorial biome | ~~tune spawnPosition~~, resource distribution |
| Pine Forest | dense, darker | pine_needles, pine_log | mushrooms, resin_tree | bunny, boar | campsite | campsite, abandoned cabin | resin, bark, mushrooms | mid materials | ~~resin harvest~~ |
| Mushroom Grove | soft hills, glowing nights | mushroom_soil, glow blocks | mushroom_cluster, glow mushrooms | snail, fireflies | mushroom_grove | mushroom_circle | glow_crystal, rare herbs | magical exploration | particles/light nodes |
| Lakeside | flat shores, water | clay_bank, shallow_water | reeds, herbs | frogs later, fireflies | common_nature | lakeside_shack, hidden_well | clay, water, herbs | pottery/cooking | shallow water, clay deposits |
| Old Ruins | broken stone ridges | ruin bricks, mossy stone | vines, mushrooms | snails, rare danger later | ruin_common, ruin_rare | small_ruin, market, watchtower | ancient fragments, coins | progression/lore | loot persistence |
| Highlands | rocky slopes | stone, gravel, moss | sparse herbs | goats later | old_mine | mine entrance, tower | copper, iron, stone | mining progression | ore weighting |
| Frost Peaks | high cold slopes | snow, ice, crystal | rare frost herbs | snow hare later | frost_rare | frozen shrine | glow_crystal, rare herbs | late exploration | temperature optional |

## 9. Structures und Loot

Structures:

| Structure | Biomes | Chance | Size | Palette | LootTable | Lore | Entity | Progression |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| cozy_campsite | Meadow, Lakeside, Pine | 0.8 percent/chunk | 7x7 | campfire, logs, mat | campsite | 10 percent | bunny/sheep nearby | early campfire hints |
| abandoned_cabin | Pine, Meadow | 0.25 percent | 11x9 | old wood, planks | campsite, common | 20 percent | none | storage/decor ideas |
| small_ruin | Old Ruins, Highlands | 0.8 percent | 9x9 | ruin bricks | ruin_common | 35 percent | snail/firefly | fragments |
| old_watchtower | Highlands, Old Ruins | 0.35 percent | 7x7 tall | stone, old beams | ruin_common/rare | 45 percent | none | map fragments |
| mushroom_circle | Mushroom Grove | 1.0 percent | 9x9 | mushroom blocks | mushroom_grove | 15 percent | fireflies | glow seeds |
| hidden_well | Lakeside, Meadow | 0.25 percent | 5x5 | stone, water | common_nature | 20 percent | fireflies | old coins |
| broken_bridge | Lakeside, Pine | 0.3 percent | 13x5 | old wood | common | 10 percent | none | scenic landmark |
| old_mine_entrance | Highlands | 0.4 percent | 9x7 | beams, stone | old_mine | 30 percent | boar nearby | ore progression |
| lakeside_shack | Lakeside | 0.3 percent | 9x7 | planks, clay | campsite | 25 percent | none | clay/cooking hints |
| ruined_market_stall | Old Ruins, village | 0.25 percent | 9x9 | market crate, mossy stone | village_market | 50 percent | none | coins/trade setup |

Loot tables:

- `common_nature`: twig, fiber, berries, pebble, mushroom.
- `campsite`: twig, charcoal, clay_bowl, old_note, berries, rope.
- `ruin_common`: ancient_coin, ancient_fragment, old_note, mossy_stone.
- `ruin_rare`: ruin_key shard, lost_charm, glow_crystal, map_fragment.
- `old_mine`: copper_ore, iron_ore, charcoal, old_note.
- `mushroom_grove`: mushroom, glow_crystal, glowing_seed, wild_herbs.
- `village_market`: ancient_coin, resin, cloth, decor recipes.

Loot entry shape:

```java
record LootEntry(String itemKey, int min, int max, int weight, LootCondition condition) {}
```

Server requirements:

- Loot deterministic from world seed, chunk pos, structure id and chest local id.
- Opened/generated chests must persist.
- Multiplayer can never regenerate chest loot by reconnecting.
- Chest inventory mutations are server-authoritative.

## 10. Entity- und Tier-Features

| Entity | Spawn | Time | Movement | Interaction | Drops | Animation | Server Snapshot | Client Rendering |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `cozy_sheep` | Cozy Meadow, Flower Fields | day | wander/graze | follows berries, shear later | cloth/wool later | idle, walk, graze | position, state, target | ~~colored box now~~, generic procedural model now, sprite/model assets later |
| `forest_bunny` | Meadow, Pine | day/dusk | idle, hop, flee | observe, maybe feed | none | hop bob | flee state | ~~small model~~, procedural bunny model |
| `moss_snail` | Mushroom Grove, Mire | all | very slow wander | inspect, rare moss drop | moss/slime optional | crawl | position/state | ~~low model~~, procedural snail model |
| `firefly_swarm` | Lakeside, Grove, Meadow | night | float around anchors | ambience only | none | glow pulse | position/intensity | emissive particles |
| `little_boar` | Pine Forest | day | wander, sniff | finds mushrooms, neutral | leather_strip optional | walk/sniff | neutral/flee state | ~~medium model~~, procedural boar model |

AI states V1:

- `IDLE`
- `WANDER`
- `FLEE`
- `FOLLOW`
- `GRAZE`

Implementation:

- ~~Extend `EntitySnapshot` with state key later or add `EntityStateSnapshot`.~~
- ~~Server tick moves ambient entities slowly.~~
- ~~Client interpolates snapshots.~~
- ~~Timid ambient entities flee nearby players on server tick.~~
- ~~Entity interactions use `EntityInteractRequest(entityId, selectedSlot)`.~~

## 11. UI/UX-Feature-Plan

Inventory:

- ~~Shift-click quick move.~~
- ~~Right-click split stack.~~
- ~~Drag stack.~~
- ~~Sort button.~~
- ~~Trash slot optional and disabled in survival by default.~~

Hotbar:

- ~~Durability bar already exists, polish colors.~~
- ~~Food value hint on hover.~~
- ~~Selected slot pulse animation.~~
- Mouse wheel already supported.

Tooltips:

- ~~Name.~~
- ~~Description.~~
- ~~Category.~~
- ~~Hunger/Heal.~~
- ~~Durability.~~
- ~~Tool type and level.~~
- ~~Places block.~~
- ~~Comfort value.~~
- ~~Rarity.~~
- ~~Station requirement for recipes.~~

Crafting UI:

- ~~Categories: All, Tools, Food, Building, Decor, Adventure.~~
- ~~Search field.~~
- ~~Locked recipes greyed out.~~
- ~~Station requirement line.~~
- ~~Missing ingredients red.~~
- ~~Craftable filter.~~

HUD:

- ~~Health.~~
- ~~Hunger.~~
- ~~Stamina.~~
- ~~Comfort.~~
- ~~Breath underwater.~~
- ~~Temperature optional.~~
- ~~Current biome small text or compass UI.~~
- ~~Day time indicator.~~

Settings:

- ~~Render distance.~~
- ~~Preview radius.~~
- ~~FOV.~~
- ~~Sensitivity.~~
- ~~Water.~~
- ~~AO.~~
- ~~Shadows.~~
- ~~VSync.~~
- ~~UI scale.~~

## 12. Audio, Partikel und Feedback

Sound hooks:

- ~~`step_grass`, `step_stone`, `step_wood`~~
- ~~`break_wood`, `break_stone`~~
- ~~`collect_item`~~
- ~~`craft_success`~~, ~~`craft_fail`~~
- ~~`inventory_click`~~
- ~~`campfire_crackle`~~
- ~~`night_ambience`, `meadow_birds`~~
- ~~`cave_drip`~~
- ~~`water_splash`~~

Particles:

- ~~block break particles.~~
- ~~harvest sparkle.~~
- ~~campfire smoke.~~
- ~~fire sparks.~~
- ~~fireflies.~~
- ~~cooking steam.~~
- ~~leaf particles.~~
- ~~water splash.~~
- ~~glow mushroom spores.~~

Feedback messages:

- ~~`New recipe unlocked`~~
- ~~`Too far away`~~
- ~~`Need a pickaxe`~~
- ~~`Inventory full`~~
- ~~`Campfire needs fuel`~~
- ~~`You feel cozy`~~
- ~~`No matching recipe`~~
- ~~`This needs a workbench`~~

Implementation:

- ~~Keep `GameAudio` no-op hook until backend exists.~~
- ~~Add `FeedbackLog` or reuse `statusMessage`/chat for short non-chat messages.~~
- ~~Add `ParticleSystem` client-only with simple CPU billboard sprites.~~

## 13. Multiplayer-Sicherheit

General rules:

- Client sends intent, not result.
- Server validates distance, selected slot, inventory, cooldown, station and world state.
- Server mutates inventory/world.
- Server sends snapshots/updates back.

Crafting intent:

- ~~Packet: recipe key, count, station position optional.~~
- ~~Validate known recipe, ingredients, output space, station reach/type.~~
- Validate recipe unlock/progression once non-ALWAYS unlocks exist.
- ~~Consume and produce server-side only.~~

Cooking intent:

- ~~Packet: station pos, recipe key, input slots.~~
- ~~Validate active campfire, recipe, reach and input slots.~~ Cooking pot BlockEntity and richer fuel UI later.
- ~~Output only after cook time.~~
- ~~Reject legacy instant `CraftRequest` path for timed campfire recipes.~~

Block interaction intent:

- ~~Packet exists: selected slot + target pos.~~
- ~~Extend result handling for campfire, crates, berry bush, herb patch.~~
- ~~Validate reach and cooldown.~~

Chest open/move intent:

- ~~Open packet: block pos.~~
- ~~Move packet: source slot, target slot, count, transaction id.~~
- ~~Validate chest exists, player reach, stack rules.~~
- ~~Persist chest inventory.~~

Entity interact intent:

- ~~Packet: entity id, selected slot, action type.~~
- ~~Validate entity exists, range, selected slot, cooldown.~~
- ~~Server decides feed response.~~
- ~~Follow response for fed animals.~~
- ~~Flee response for timid ambient animals.~~ Drop AI decisions remain.

Sleep intent:

- ~~Packet: bed/sleeping mat pos.~~
- ~~Validate sleeping mat, reach and night/time.~~
- ~~Validate comfort, simple shelter and no danger nearby.~~
- ~~Server advances time only when rules allow sleep.~~
- ~~Multiplayer all-sleep policy for online players in the same world.~~

Comfort sync:

- ~~Server computes comfort.~~
- ~~Server-authoritative hunger/stamina effects move to `PlayerSurvivalState` later.~~
- ~~Sync via PlayerState packet every 1 to 2 seconds or on change.~~

## 14. Tests

Unit tests:

- ~~Inventory add/remove and full inventory.~~
- ~~Tool damage and breakage.~~
- ~~Crafting success/fail.~~
- ~~Crafting full inventory.~~
- ~~Food effects.~~
- ~~Comfort calculation.~~
- ~~Loot table deterministic.~~
- ~~Biome resource distribution.~~
- ~~Tool mining rules.~~
- ~~Packet roundtrips for every implemented intent.~~
- Add roundtrips for future new intent packets when those intents exist.

Integration tests:

- Singleplayer crafting through common rules.
- Multiplayer crafting through server handler or fake channel.
- Server rejects invalid distance.
- ~~Chest opens once and persists.~~
- ~~Loot persists after chest reopen/transfer.~~ Chunk unload/reload save later.
- Cooking consumes fuel and produces output after time.
- Entity interaction sync.
- ~~Block drops match registry.~~

Manual checklist:

- Start launcher.
- Start singleplayer.
- Collect twigs/berries/fiber.
- Craft knife.
- Harvest fiber with knife and see bonus.
- Craft campfire.
- Add fuel and cook food.
- Place chair/rug/lantern.
- See comfort increase.
- Mine copper.
- Craft copper tool.
- Find ruin.
- Open loot chest once.
- Join local multiplayer.
- Verify invalid far block edit is rejected.

## 15. Priorisierte Umsetzung

### Phase 1: Content + Basic Cooking

Files/classes:

- `Items`, `Blocks`, `CraftingRecipes`, `InteractionRules`
- `GamePacket`, `PacketCodec`, `ServerConnectionHandler`
- `GameClient`, `Hotbar`, `GameSprites`

Risk: low to medium.
Effort: 3 to 5 PRs.
Visible Nutzen: neue Items, mehr Rezepte, erste Cooking-Loop.
Tests: recipe tests, packet tests, interaction tests.
Akzeptanz: Spieler kann sammeln, craften, campfire nutzen und food output erhalten.

### Phase 2: Comfort + Campfire Fuel

Files/classes:

- new `ComfortRules`, server PlayerState, `PlayerStats`
- `Blocks` or comfort registry
- `GameClient` HUD

Risk: medium because server/client state sync.
Effort: 3 PRs.
Visible Nutzen: Base fuehlt sich nuetzlich an.
Tests: comfort scan/cap/effects.
Akzeptanz: Comfort wird serverseitig berechnet und im HUD angezeigt.

### Phase 3: Biome Resources + Structures + Loot

Files/classes:

- `OverworldGenerator`, `Structures`, new `LootTable`
- `ServerWorld` persistence hooks
- `Blocks`, `Items`

Risk: medium/high due persistence.
Effort: 4 to 6 PRs.
Visible Nutzen: Exploration wird belohnend.
Tests: deterministic loot, structure spawn, no duplicate chest loot.
Akzeptanz: Ruin chest gives deterministic loot once.

### Phase 4: Tool Tiers + Ores + Progression

Files/classes:

- `ItemType`, `BlockType`, `InteractionRules`
- `Items`, `Blocks`, `CraftingRecipes`
- `ServerConnectionHandler`

Risk: medium because constructor changes.
Effort: 3 PRs.
Visible Nutzen: klare Progression.
Tests: required tool level, durability, mining restrictions.
Akzeptanz: Copper pickaxe unlocks iron; stone cannot mine late nodes.

### Phase 5: Entities + Audio/Particles + UI Polish

Files/classes:

- `AmbientEntitySpawner`, `ServerEntityTracker`, `EntitySnapshot`
- ~~`EntityRenderer` model pass~~, ~~new ParticleSystem~~, `GameAudio`
- `GameClient` UI

Risk: medium.
Effort: 5+ PRs.
Visible Nutzen: Welt wirkt lebendig.
Tests: entity snapshot, interactions, spawn rules.
Akzeptanz: Animals wander/flee/follow; fireflies glow at night.

## First 10 Pull Requests

### PR 1: Data names and aliases cleanup

Ziel: Item naming stabilisieren (`berries` vs `wild_berries`, `clay` vs `clay_lump`, `planks` vs `wooden_plank`) ohne Saves zu brechen.
Dateien: `Items`, `CraftingRecipes`, `GameSprites`, docs.
Schritte: aliases definieren, display labels in UI verbessern, recipes auf canonical keys mappen.
Tests: registry no duplicate keys, recipes valid.
Akzeptanz: Alle alten Items funktionieren, neue Namen sind im UI sichtbar.

### PR 2: Recipe metadata V1.5

Ziel: Bestehende `CraftingRecipe` um category/station/unlock light erweitern oder Adapter `RecipeDefinition` einfuehren.
Dateien: `CraftingRecipe`, `CraftingRecipes`, `Hotbar`, `GameClient`.
Schritte: Enums adden, alte recipes adaptieren, UI zeigt category/station.
Tests: old crafting still passes, metadata roundtrip if packeted.
Akzeptanz: Crafting UI kann nach Kategorie filtern.

### PR 3: Basic cooking intent

Ziel: Campfire kann `cooked_berries`, `roasted_mushroom`, `herb_soup` herstellen.
Dateien: `GamePacket`, `PacketCodec`, `ServerConnectionHandler`, `InteractionRules`, `CraftingRecipes`.
Schritte: `CookRequest`, station validation, delayed output V1.
Tests: server rejects no station, consumes ingredients, inventory full.
Akzeptanz: Rechtsklick Campfire + valid ingredients gibt cooked food.

### PR 4: Campfire fuel state

Ziel: Campfire hat Fuel und Active/Inactive State.
Dateien: `ServerWorld`, new minimal `BlockEntityStore`, `Blocks`, `GameClient`.
Schritte: fuel items, activeUntil, light update, sync packet.
Tests: fuel add, expires, no dupe.
Akzeptanz: Campfire braucht Fuel und gibt nur aktiv Licht/Comfort.

### PR 5: Comfort rules and HUD

Ziel: Comfort serverseitig berechnen und im HUD anzeigen.
Dateien: new `ComfortRules`, `PlayerStats`, `ServerConnectionHandler`, `GamePacket`, `GameClient`.
Schritte: radius scan, cap, survival modifiers, UI meter.
Tests: cap, scan, hunger multiplier.
Akzeptanz: Deko um Base erhoeht Comfort und beeinflusst Regen/Hunger.

### PR 6: Storage crate BlockEntity

Ziel: Storage Crate als echte Kiste.
Dateien: `GamePacket`, `PacketCodec`, `ServerWorld`, new `BlockEntityStore`, `GameClient` inventory UI.
Schritte: open, move item, sync inventory, distance validation.
Tests: cannot dupe, far open rejected, contents persist in memory.
Akzeptanz: Crate speichert Items im Multiplayer sicher.

### PR 7: Loot tables and one-shot chests

Ziel: Structures bekommen Loot-Container.
Dateien: new `LootTable`, `Structures`, `ServerWorld`, `BlockEntityStore`.
Schritte: deterministic generation, opened flag, loot table registry.
Tests: same seed same loot, chest not regenerated.
Akzeptanz: Ruin chest gibt einmal Loot und bleibt danach persistent.

### PR 8: Tool tiers and mining gates

Ziel: Stone/Copper/Iron/Crystal Progression.
Dateien: `ItemType`, `BlockType`, `Items`, `Blocks`, `InteractionRules`.
Schritte: ~~toolLevel/toolSpeed~~, ~~requiredToolLevel~~, ~~server validation~~.
Tests: ~~wrong tool blocked~~, ~~correct tool accepted~~, ~~durability~~.
Akzeptanz: ~~Copper unlocks iron~~; iron unlocks crystal once iron tool items exist.

### PR 9: Biome resource identity pass

Ziel: Jedes Zielbiom hat eigene Ressourcen.
Dateien: `Biomes`, `OverworldGenerator`, `Blocks`, `Items`, `AmbientEntitySpawner`.
Schritte: ~~resin trees~~, clay banks, mushroom clusters, crystal nodes.
Tests: ~~deterministic distribution, biome resource checks.~~
Akzeptanz: Spieler erkennt Biome an Ressourcen und Gameplay-Grund.

### PR 10: Entity AI V1

Ziel: Friedliche Entities wandern/fliehen/folgen rudimentaer.
Dateien: `ServerEntityTracker`, `EntitySnapshot`, `AmbientEntitySpawner`, `EntityRenderer`, `GamePacket`.
Schritte: ~~state, velocity/target, periodic server tick~~, client interpolation.
Tests: ~~snapshot contains state, movement deterministic enough~~, range sync.
Akzeptanz: ~~Bunnies flee~~, sheep wander/follow berries, fireflies pulse at night.
