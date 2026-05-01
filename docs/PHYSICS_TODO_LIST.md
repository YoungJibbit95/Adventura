# Adventura - Physics TODO List

Stand: 2026-05-01

Diese Liste enthaelt nur noch offene technische Physik-Arbeit. Erledigte Alpha-Punkte aus Movement Validation, Collision Shapes, Placement, Fall Damage, Projectile Foundation, Physics-Diagnostics, Fuzzing, Golden-Replays und der Regression-Testmatrix wurden entfernt.

## Ziel

Physics soll stabil, vorhersehbar, cozy und multiplayer-sicher bleiben. Server-Autoritaet hat Vorrang; Client-Polish kommt danach. Neue Physik-Features sollen zuerst als Common-Regeln modelliert und dann serverseitig validiert werden.

## Leitlinien

- Simulation gehoert nach `common/physics`.
- Server entscheidet, ob Movement, Placement, Interaction und Hits plausibel sind.
- Client darf vorhersagen und weich darstellen, aber nicht die Wahrheit besitzen.
- Unbekannte Chunks, fehlende Daten und NaN/Infinity gelten immer als blockierend oder invalid.
- Jede neue Physik-Regel braucht mindestens einen Missbrauchs- oder Edge-Case-Test.
- Schneller Regression-Gate: `./gradlew physicsRegression`.

---

# P0 - Physics Kernel Hardening

## Offen

- Einheitlichen `PhysicsStepContext` fuer Delta, Tick, Dimension, Mode, Wasserzustand und Debug-Metadaten einfuehren.
- Gemeinsame Numeric Guards fuer Position, Velocity, Delta, Bounds und Projectile-State zentralisieren, statt sie pro Record/Regel zu wiederholen.
- Common-Testfixtures fuer kleine Blockwelten bauen, damit Movement-, Collision-, Placement- und Projectile-Tests weniger handverdrahtet sind.
- Physics-Konfigurationswerte versionieren oder snapshotbar machen, damit Client/Server-Mismatch leichter sichtbar wird.
- Replay-Harness ausbauen: mehrere Steps, Player-Movement, Entity-Movement und Projectile-Schritte aus Golden-Dateien nachspielen.

## Akzeptanz

- Neue Physikmodule nehmen denselben Step-Kontext statt verstreuter Einzelparameter.
- Invalid-Werte koennen nicht durch Movement, Entities oder Projectiles sickern.
- Ein aufgezeichneter Physikablauf laesst sich deterministisch im Test nachspielen.
- Client und Server koennen ihre Physics-Konfigurationsversion vergleichen.

---

# P1 - Entity Physics V2

## Offen

- Dynamische Entity-vs-Entity-Separation fuer nahe Tiere, Drops und Spieler einfuehren, statt nur blockierende Overlaps zu stoppen.
- Ambient-Entities mit einfachen Recovery-Steps aus Block-/Entity-Blockaden holen.
- Per-Entity Physics-Profil einfuehren: ground, flyer, swimmer, tiny, heavy.
- Item-Drop-Merge/Stacking physikalisch sicher machen, ohne Pickup-Dupe zu riskieren.
- Knockback gegen Terrain sweepen und gleiten lassen, statt blockierte Knockbacks nur zu nullen.
- Entity-Wasserverhalten pro Profil definieren: meiden, schwimmen, treiben, ignorieren.

## Akzeptanz

- Entities bleiben nicht dauerhaft ineinander oder in Terrain stecken.
- Knockback und Follow/Flee respektieren dieselben Kollisionsregeln.
- Neue Entity-Typen brauchen ein Profil statt Sonderlogik im Tracker.
- Item-Drops koennen nicht durch Merge/Pickup-Rennen dupliziert werden.

---

# P2 - Projectiles And Hit Resolution

## Offen

- Bow-/Tool-Input und Server-Spawn-Packet an die Projectile-Foundation anbinden.
- Pfeile nach Blocktreffer optional stecken lassen oder als kurzes Impact-Snapshot senden.
- Client-Modell, Trail, Hit-Partikel und Hit-Audio fuer Projektile bauen.
- Projectile-Lag-Kompensation fuer bewegte Ziele evaluieren.
- Damage-/Friendly-Fire-/Owner-Regeln zentral definieren.
- Projectile-vs-Projectile oder Projectile-vs-Door/Furniture-Regeln entscheiden.
- Impact-Face/Normal fuer komplexere Partial-Shapes weiter verfeinern.

## Akzeptanz

- Spieler koennen Projektile nutzen, ohne Client-Hit-Trust.
- Treffer sind reproduzierbar und fuer Client-Effekte genau genug lokalisiert.
- Despawn, Damage und Visuals bleiben synchron.
- Bewegte Ziele bleiben innerhalb eines klaren Lag-Fensters fair treffbar.

---

# P3 - Fluid And Environmental Physics

## Offen

- Stroemungen als optionale Fluid-Velocity-Felder modellieren.
- Buoyancy fuer Items, leichte Entities und Projectiles definieren.
- Lava/Hot-Blocks/Cold-Blocks als Environment-Physics-Hazards pruefen.
- Wasseroberflaechen-Transitions weiter testen: Reinfallen, Auftauchen, Springen an Kante.
- Underwater Audio/Tint an echten `headUnderwater`-State binden.
- Tests fuer fallende Items und Projectiles durch Wasser/Luft-Grenzen ergaenzen, sobald Buoyancy/Fluid-Forces existieren.

## Akzeptanz

- Fluids beeinflussen Bewegung konsistent statt nur ueber Sonderfaelle.
- Umweltgefahren laufen serverseitig autoritativ.
- Wasseruebergaenge bleiben lesbar und nicht unfair.
