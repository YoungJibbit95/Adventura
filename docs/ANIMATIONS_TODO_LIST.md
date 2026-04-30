# Adventura – Animations TODO List

## Ziel

Animationen sollen Adventura lebendiger, cozy und lesbarer machen. Wichtig ist eine einfache, wartbare Animationsbasis statt verstreuter Sonderlogik im `GameClient`.

## P0 – Animationen aus GameClient herausziehen

- `AnimationClock` einführen.
- `Easing`-Funktionen zentralisieren.
- kleine Komponenten für Held Item Bob, Hand Swing, Block Break Progress, Pickup Pop, UI Pulse und Feedback Fade.
- Ziel: weniger Animation-Code im Main Loop, wiederverwendbare Animationen, einheitliche Timing-Regeln.

## P1 – Entity Animation System

### Pose-System für Box-Modelle
Entity-Modelle bekommen benannte Teile: body, head, arms, legs, ears optional, tail optional, shell optional.

### Standard-Poses
- idle.
- walk.
- flee.
- follow.
- graze.
- jump/fall.
- swim optional.
- interact/feed.

### Creature-spezifisch
- Bunny: hop bob, ear tilt, flee hop schneller.
- Sheep: graze head down, soft walk, follow player.
- Snail: slow crawl, shell bob minimal.
- Boar: sniffing, short trot.
- Firefly: glow pulse, floating swarm motion.

## P2 – Entity Interpolation polish

- Snapshot Buffer weiter nutzen.
- render delay feinjustieren.
- yaw smoothing verbessern.
- teleport snap threshold definieren.
- missing snapshot fallback.
- Ziel: Tiere und Online-Spieler bewegen sich weich, Server-Autorität bleibt unverändert.

## P3 – Block Break Animation

- Crack/Break Overlay direkt auf Zielblock-Face rendern.
- HUD-Crack darf als Zusatz bleiben.
- Zielblock muss eindeutig sichtbar sein.
- Break stages.
- Tool-Speed beeinflusst sichtbaren Fortschritt.
- invalid tool zeigt Feedback statt Progress.

## P4 – Item / Hand Animationen

- idle bob.
- swing on break/use.
- place animation.
- eat animation.
- tool-specific swing speed.
- fishing/cooking später optional.
- Pickup Pop verbessern.

## P5 – UI Animationen

- selected hotbar pulse weiter abstimmen.
- new recipe unlock pop.
- comfort level up pulse.
- inventory slot hover.
- crafting success pulse.
- missing ingredient shake sehr subtil.
- Regeln: cozy, weich, nicht hektisch, UI Scale berücksichtigen.

## P6 – Partikel als Animationserweiterung

- block break debris block-spezifischer machen.
- harvest sparkle subtiler/besser.
- campfire smoke weicher.
- fire sparks lebendiger.
- glow spores stärker biome-gebunden.
- fireflies mit besserem Glow.
- water splash besser timen.
- Particle Quality Setting und Low/Medium/High Preset.
- harte Obergrenze für Partikel.
- Debug Counter im F3 Overlay.

## Tests / Smoke Checks

- Entity idle/walk/flee/follow sichtbar.
- Snapshot interpolation ohne große Sprünge.
- Block break overlay sitzt auf korrektem Block.
- Hand swing triggert bei Break/Place/Eat.
- UI Animationen funktionieren bei UI Scale 1x/2x.
- Partikel brechen Low-End-Budget nicht.

## Akzeptanzkriterien

- Animation-Code ist nicht mehr stark im `GameClient` verstreut.
- Entities wirken lebendig und weich.
- Block Break ist eindeutig lesbar.
- UI-Animationen verbessern Feedback ohne zu nerven.
