# Adventura – Animations TODO List

Stand: 2026-04-30

## Ziel

Animationen sollen Adventura lebendiger, cozy und lesbarer machen. Der Fokus liegt auf einer einfachen, wartbaren Animationsbasis statt verstreuter Sonderlogik im `GameClient`.

Animationen sollen:

- Feedback verbessern
- Interactions lesbarer machen
- Entities lebendiger wirken lassen
- UI angenehm machen
- Performance-Budgets respektieren

---

# P0 – Animation Foundation

## Basis vorhanden

- Held-Item-Bob, Hand Swing, Block Break Progress, Pickup Pop und UI Feedback existieren in erster Form.
- `PopAnimation` und `UiPulse` nutzen gemeinsame Clip/Track-Basis.
- UI-Presets für Pop, Pulse und Shake existieren.

## Offen

- Animation-Code weiter aus `GameClient` herausziehen.
- zentrale `AnimationClock` nutzen.
- zentrale `Easing`-Funktionen definieren.
- kleine Komponenten für:
  - Hand Swing
  - Held Item Bob
  - Block Break Progress
  - Pickup Pop
  - UI Pulse
  - Feedback Fade
- Animationen müssen UI Scale und Pause-State respektieren.

## Akzeptanz

- Main Loop enthält weniger Animations-Sonderlogik.
- Animationen sind wiederverwendbar.
- Timing-Regeln sind einheitlich.

---

# P1 – Entity Animation System

## P1.1 Pose-System für Box-Modelle

### Ziel

Entity Renderer soll nicht pro Entity-Typ komplett eigene Sonderlogik brauchen.

### Modellteile

- body
- head
- legs
- arms optional
- ears/horns/tail optional
- shell/snail body optional
- wing/glow core optional

### Standard-Poses

- idle
- walk
- run/flee
- follow
- graze
- eat
- sleep
- jump/fall
- swim optional
- interact/feed
- hurt
- death optional

### Offen

- Named model parts sauber speichern.
- Pose-Funktionen pro Entity-Familie.
- Animation State aus Entity Snapshot nutzen.
- Walk-Signal aus Velocity oder State ableiten.
- State transitions weich blenden.
- LOD: entfernte Entities weniger animieren.
- 🔴 Braucht Lead Engine Developer: Entity-Animationen sollen `CozyLifeProgression.animationContract` und serverbestaetigte EntitySnapshot-States konsumieren.
  Kontext: Gameplay P9.5 definiert pro Creature Rollen und Animation-Contracts fuer idle/graze/eat/flee/follow/glow; neue Sonderfaelle pro Entity-Typ sollen begrenzt bleiben.
  Erwarteter Contract: Mapping von `CreatureDesign.entityKey()`/`animationContract` auf Pose-Familie, plus Snapshot-State-Namen fuer feed/friendship/flee/graze.
  Akzeptanz: Sheep/Bunny/Snail/Firefly/Boar lesen gemeinsame Creature-Design-Daten, Feed-/Friendship-Animationen koennen serverbestaetigt getriggert werden.

### Akzeptanz

- Bunny hoppt/duckt beim Flee.
- Sheep grazt/folgt sichtbar.
- Boar schnüffelt/wandert.
- Snail kriecht langsam.
- Firefly pulsiert.

---

## P1.2 Creature-spezifische Animationen

### Bunny

- idle ear twitch.
- hop cycle.
- flee stretch.
- short pause/look-around.

### Sheep

- graze head down.
- soft walk.
- follow bounce.
- shear animation später.

### Moss Snail

- slow body stretch.
- shell bob.
- antenna wiggle.

### Little Boar

- sniff animation.
- short trot.
- mushroom search cue.

### Firefly Swarm

- glow pulse.
- loose orbit.
- night-only intensity.

---

# P2 – Entity Interpolation Polish

## Basis vorhanden

- Snapshot Buffer wird genutzt.
- Interpolierte Snapshots können Velocity explizit behalten oder aus Deltas rekonstruieren.

## Offen

- render delay feinjustieren.
- yaw smoothing verbessern.
- teleport snap threshold definieren.
- missing snapshot fallback.
- interpolation per Entity-Typ tuning.
- state transitions beim Snapshot-Wechsel weich machen.
- Debug Toggle: raw vs interpolated positions.

## Akzeptanz

- Tiere und Online-Spieler bewegen sich weich.
- Server-Autorität bleibt unverändert.
- Teleports snappen sauber statt zu tweenen.

---

# P3 – Block Break und Interaction Animation

## Offen

- Crack/Break Overlay direkt auf Zielblock-Face rendern.
- HUD-Crack darf als Zusatz bleiben.
- Zielblock muss eindeutig sichtbar sein.
- Break stages definieren.
- Tool-Speed beeinflusst sichtbaren Fortschritt.
- invalid tool zeigt Feedback statt falschen Progress.
- block-specific debris mit Materialfarben.
- Placement preview Animation optional.

## Akzeptanz

- Spieler sieht exakt, welcher Block bricht.
- Break-Fortschritt fühlt sich passend zum Tool an.
- falsches Tool ist visuell verständlich.

---

# P4 – Item / Hand Animationen

## Offen

- Break Swing.
- Place Swing.
- Eat/Drink Animation.
- Feed Entity Animation.
- Bow Draw Animation später.
- Tool Durability Break Pop.
- Pickup-to-hotbar Pop.
- Cooking/Forge interaction small feedback.

## Regeln

- kurze Dauer.
- nicht hektisch.
- skalierbar mit FOV.
- nicht motion-sickness-lastig.

## Akzeptanz

- Interactions fühlen sich direkter an.
- Animationen verdecken nicht die Sicht.

---

# P5 – UI Animationen

## Offen

- Button hover/pulse.
- selected hotbar pulse feinjustieren.
- recipe unlock pop.
- craft success pop.
- drag/drop snap.
- error shake dezent.
- journal new entry marker.
- comfort level up soft glow.

## Regeln

- cozy und weich.
- nicht nervig.
- UI Scale berücksichtigen.
- Low Motion Setting optional.

## Akzeptanz

- UI fühlt sich lebendig an.
- Feedback ist klar, aber nicht überladen.

---

# P6 – Partikel als Animationsschicht

## Basis vorhanden

- CPU-Billboard-Partikel.
- block break debris.
- harvest sparkle.
- campfire smoke/sparks.
- fireflies/mire wisps.
- cooking steam.
- leaf drift.
- water splash.
- glow spores.

## Offen

- block break debris block-spezifischer machen.
- harvest sparkle subtiler.
- campfire smoke weicher.
- fire sparks lebendiger.
- glow spores stärker biome-gebunden.
- fireflies mit besserem Glow.
- water splash besser timen.
- ore sparkle bei seltenen Nodes.
- comfort sparkle sehr subtil.
- Particle Quality Setting.
- harte Obergrenze für Partikel.

## Akzeptanz

- Partikel unterstützen Gameplay-Lesbarkeit.
- Low-End-Budget wird nicht gebrochen.
- Partikel wirken cozy, nicht chaotisch.

---

# Tests / Smoke Checks

- Entity idle/walk/flee/follow sichtbar.
- Snapshot interpolation ohne große Sprünge.
- Teleport snappt korrekt.
- Block break overlay sitzt auf korrektem Blockface.
- Hand swing triggert bei Break/Place/Eat.
- UI Animationen funktionieren bei UI Scale 1x/1.5x/2x.
- Partikel respektieren Low/Medium/High Quality.
- Debug-HUD zeigt Particle Budget.

## Akzeptanz gesamt

- Animation-Code ist nicht mehr stark im `GameClient` verstreut.
- Entities wirken lebendig und weich.
- Block Break ist eindeutig lesbar.
- UI-Animationen verbessern Feedback ohne zu nerven.

---

# P7 – Animation State Machines und Event Hooks

Owner: Lead UI/UX Frontend Developer für UI-Animationen, Lead Engine Developer für Entity/Render-Anbindung.

## P7.1 Animation aus GameClient lösen

### Offen

- Animation-Orchestrierung aus `GameClient` extrahieren:
  - UIAnimationController.
  - InteractionAnimationController.
  - HeldItemAnimationController.
  - FeedbackAnimationController.
- `AnimationClock` pro Pause-/Game-State sauber steuern.
- Low Motion Setting vorbereiten.
- Tests für Pause, UI Scale und State-Wechsel.

### Akzeptanz

- Neue Feedback-Animationen brauchen keine neuen `GameClient`-Felder.
- Pause und Menüs beeinflussen Animationen nachvollziehbar.

## P7.2 Entity Animation State Machine

### Offen

- Server-/Common-State für Entity-Verhalten definieren:
  - idle.
  - wander.
  - flee.
  - follow.
  - graze.
  - eat.
  - hurt.
  - sleep.
  - swim.
- Client blendet Pose-Zustände, aber erfindet keine Gameplay-Zustände.
- Snapshot-Deltas mit Animation-State abstimmen.
- LOD-Regeln:
  - full animation near.
  - reduced animation mid.
  - billboard/frozen far.

### Akzeptanz

- Tiere wirken lebendig und bleiben serverkonsistent.
- Animationen skalieren mit Entity-Zahl.

## P7.3 GameplayEvent Hooks

### Status 2026-05-01

- 🟠 Event-Packet und erster Client-Hook sind vorhanden: `GamePacket.GameplayEvents` erreicht `GameClient`, und `GameplayEventFeedback` triggert erste Pickup-, Damage-, Craft- und Recipe-Pop/Flash-Animationen.
  Verifikation: `GameplayEventFeedbackTest`; Packet-/Codec-Verifikation ueber `PacketCodecTest`.

### Offen

- Animationen auf serverbestätigte Events reagieren lassen:
  - ~~pickup.~~
  - ~~damage.~~
  - ~~craft success/fail.~~
  - ~~recipe unlock.~~
  - cook complete.
  - projectile impact.
  - feed entity.
- Lokale Preview-Animationen klar von bestätigten Result-Animationen trennen.

### Akzeptanz

- UI/World-Feedback feuert nicht doppelt.
- Online und Offline fühlen sich konsistent an.

## P7.4 Sprite- und Particle-Animation Pipeline

### Offen

- Particle-Sprites mit Atlas-Frames unterstützen.
- Animated sprite metadata planen:
  - frame duration.
  - loop.
  - random offset.
  - pivot.
  - blend mode.
- Campfire, firefly, sparkle, water splash und projectile trail als erste Kandidaten.

### Akzeptanz

- Partikel-Animationen sind datengetrieben und budgetierbar.
- Pixel-Art bleibt klar.
