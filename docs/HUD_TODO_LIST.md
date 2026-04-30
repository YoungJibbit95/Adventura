# Adventura – HUD TODO List

## Ziel

Das HUD soll die wichtigsten Survival-, Comfort-, Welt- und Feedback-Informationen klar, cozy und unaufdringlich anzeigen.

## P0 – HUD-Struktur

- Health, Hunger, Stamina/Energy, Breath und Comfort optisch einheitlich darstellen.
- gleiche Abstände, gleiche Skalierung, gleiche Icon-Logik.
- UI Scale berücksichtigen.
- Modi: Normal HUD, Minimal HUD, Debug HUD, Hidden HUD, Death HUD, Underwater HUD.

## P1 – Survival HUD

### Health
- volle, halbe und leere Herzen.
- Damage Feedback kurz animieren.
- Regeneration subtil anzeigen.

### Hunger
- volle, halbe und leere Hungerkeulen oder Beeren-Icons.
- Food Tooltip zeigt Auswirkung vor Nutzung.

### Stamina / Energy
- klar als Sprint-/Action-Ressource darstellen.
- Regen durch Comfort subtil markieren.
- niedrigen Wert kurz pulsen lassen.

### Breath
- nur anzeigen, wenn relevant: underwater oder kurz nach Auftauchen.
- volle, halbe und leere Luftblasen.

## P2 – Comfort HUD

- Comfort als cozy Meter mit Icon darstellen.
- Level: Low, Cozy, Warm, Restful, Homey.
- kurze Erklärung im Tooltip.
- bei neuem Comfort-Level einmalige Meldung.
- Quellen optional anzeigen: Campfire +5, Rug +3, Lantern +3.

## P3 – Weltinformationen

- Current Biome schöner darstellen.
- bei Biome-Wechsel kurze Meldung.
- Day/time mit Icon statt nur Text.
- Day Number anzeigen.
- Tageszeit: Morning, Noon, Evening, Night.
- Temperature nur anzeigen, wenn Temperature-System relevant ist.

## P4 – Interaction HUD

- Blockname beim Anvisieren anzeigen.
- benötigtes Tool anzeigen.
- Mining Progress am Block oder im HUD.
- Campfire: fuel, active/inactive, recipe, cook progress.
- Cooking Pot/Forge: input needed, fuel needed, output ready.
- Storage: Open-Hint und Range-Hint.

## P5 – Feedback Log

Kategorien:
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

Regeln:
- kurze Meldungen.
- ~~gleiche Meldungen zusammenfassen.~~ ✅ Erledigt (FeedbackLog aggregiert Wiederholungen als `xN`).
- ~~Cooldown gegen Spam.~~ ✅ Erledigt (FeedbackLog merged doppelte aktive Meldungen und refreshed Dauer).
- ~~wichtige Meldungen länger sichtbar.~~ ✅ Erledigt (wichtige Kategorien bekommen längere Default-Dauer im FeedbackLog).
- [ ] Prioritäts-Mapping ausbaubar machen (konfigurierbare wichtige Kategorien statt String-Matching im Code).

## P6 – Debug HUD

Engine Stats: FPS, Frame ms, Render ms, Update ms, Chunkgen ms, Meshing ms, Upload ms, Draw Calls, Triangles, VRAM, Loaded/Visible/Dirty Chunks.

Gameplay Stats: Position, Chunk Coordinate, Current Biome, Light Level, Looking at Block, Selected Item, Gamemode, raw survival values.

Network Stats: Ping, Server tick estimate, packets/sec optional, entity snapshot count, chunk packet count.

## Akzeptanzkriterien

- HUD ist bei 1x, 1.5x und 2x UI Scale lesbar.
- Survival-Werte sind sofort verständlich.
- Comfort ist sichtbar, aber nicht aufdringlich.
- Debug HUD hilft beim Entwickeln.
- Feedback-Meldungen spammen nicht.
