# Adventura – Physics TODO List

## Ziel

Die Physics soll stabil, vorhersehbar und zwischen Client und Server möglichst einheitlich sein. Survival-Bewegung muss weich sein, darf aber im Multiplayer nicht ausnutzbar sein.

## P0 – Physics-Architektur abschließen

- Input bleibt im Client.
- Simulation bleibt in `common/physics`.
- `Camera` wird View/Controller, nicht Physics-Engine.
- Freecam/Noclip-Controller-Rest weiter isolieren.
- Flying/Creative und Survival klar trennen.
- Client und Server nutzen gemeinsame Physics-Regeln.

## P1 – Server Movement Validation

- Kollision gegen Welt prüfen.
- Ground-State plausibilisieren.
- Jump nur plausibel, wenn vorher grounded.
- Water-State plausibilisieren.
- Beschleunigung grob prüfen.
- Speed abhängig von Mode prüfen.
- Fall-Damage/Impact serverseitig absichern.
- Movement mit Sequenznummern vorbereiten.
- Server sendet autoritativen State.
- Client kann später weich korrigieren.

## P2 – Collision Robustness

- Boden, Wand, Ecke, Head-Bump, Chunk-Grenzen testen.
- dünne Blocks/Fences optional.
- unbekannte Chunks dürfen nicht wie Luft behandelt werden.
- Bewegung sanft blockieren oder pausieren.
- Feedback optional: „Loading terrain...“.

## P3 – Wasserphysik

Flags:
- feetInWater.
- bodyInWater.
- headUnderwater.
- swimming.
- breath.
- waterDrag.

Gameplay:
- Wasser bremst horizontal.
- Space erlaubt Auftauchen.
- Breath sinkt nur bei headUnderwater.
- Splash Feedback beim Eintritt/Austritt.
- Strömung später optional.

Tests:
- Breath sinkt nur unter Wasser.
- Auftauchen regeneriert Breath.
- Wasser reduziert Fallgeschwindigkeit.
- Wasser verhindert/vermindert Fall Damage.

## P4 – Placement und Interaction Physics

- Platzieren in eigene Player-AABB verhindern.
- serverseitig gleiche Regel.
- Platzierung gegen andere Spieler/Entities später optional.
- klare Feedback-Meldung bei invalid placement.
- Range prüfen für Block-Abbau, Platzierung, Campfire, Crate, Cooking Pot, Forge, Entity Interactions und Sleep.

## P5 – Entity Physics

- einfache Collision optional.
- Entities sollten nicht in Blöcken spawnen.
- Flee/Follow respektiert grob Terrain.
- Entities können nicht endlos durch Wände laufen.
- Despawn/Park außerhalb relevanter Chunks.
- Future: simple path steering, avoid water for some animals, prefer grass for grazers, fireflies ignore ground collision.

## P6 – Fall Damage und Movement Polish

- definieren, ab welcher Höhe Schaden entsteht.
- Wasser reduziert Schaden.
- Creative/Spectator ausnehmen.
- Server validiert Landung und Impact.
- Movement Feel: sprint acceleration, grounded friction, air control, swim drag, jump buffer optional, coyote time optional.

## Tests

Unit:
- grounded jump, wall collision, sliding, head bump, fall impact, water drag, unknown chunk blocks movement, placement against player AABB.

Integration:
- server rejects impossible movement.
- server rejects far interaction.
- server rejects placement inside player.
- multiplayer movement snapshots stay plausible.

## Akzeptanzkriterien

- Spieler fällt nicht durch ungeladene Chunks.
- Movement fühlt sich weich an.
- Server akzeptiert keine extremen Teleports.
- Wasser ist klar spürbar.
- Block placement kann Player nicht einklemmen.
