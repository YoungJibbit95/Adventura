# Adventura – Physics TODO List

Stand: 2026-04-30

## Ziel

Physics soll stabil, vorhersehbar, cozy und multiplayer-sicher sein. Survival-Bewegung muss weich sein, darf aber online nicht ausnutzbar werden. Client und Server sollen möglichst dieselben Common-Regeln nutzen.

## Leitlinien

- Input bleibt clientseitig.
- Simulation gehört nach `common/physics`.
- `Camera` bleibt View/Controller, nicht Physics-Engine.
- Server validiert Movement autoritativ.
- unbekannte Chunks dürfen nie als sichere Luft behandelt werden.
- Movement soll sich angenehm anfühlen, nicht hardcore-realistisch.

---

# P0 – Physics-Architektur abschließen

## Basis abgeschlossen

- gemeinsame Physics-Datenmodelle liegen in `common/physics`.
- Survival/Flying laufen über `PlayerPhysics`.
- Server prüft Movement-Plausibilität mit Common-Regeln.

## Erledigt

- ~~Freecam/Noclip-Controller-Rest weiter isolieren.~~ Verifiziert: Freecam/Noclip liegt in `ClientPlayerController`; `Camera` bleibt Blick/Position/Mouse-Look.
- ~~Survival, Creative, Spectator und Debug-Flying klar trennen.~~ Verifiziert: Survival nutzt `stepSurvival`, Creative-Flying nutzt `stepFlying`, kollisionsfreie Modi laufen im isolierten Freecam-Pfad.
- ~~gemeinsame `PlayerPhysicsConfig` sauber verwenden.~~ Verifiziert: Client-Controller und Common-Tests gehen über `PlayerPhysicsConfig.defaults()`.
- ~~Client und Server nutzen dieselben Bounds.~~ Verifiziert: Placement, Entity-Reach und Movement-Checks referenzieren Common-Bounds/Rules.
- ~~Air Control und Ground Friction zentral definieren.~~ Verifiziert: Air-Control, Air-Drag, Ground-Acceleration und Ground-Friction sind in `PlayerPhysics` zentralisiert und getestet.
- ~~Movement tuning dokumentieren.~~ Verifiziert: P0/P6 trennen jetzt Alpha-abgeschlossene Common-Tuning-Regeln von späterem Feel-Polish.

## Akzeptanz

- `Camera` enthält keine verstreute Survival-Physik mehr.
- Movement-Parameter sind zentral auffindbar.
- Server und Client verwenden dieselben Grund-Bounds.

---

# P1 – Server Movement Validation

## Basis abgeschlossen

- Server lehnt Kollisionen gegen collidable Blocks ab.
- Ground-State wird plausibilisiert.
- Grounded Jump ist validiert.
- Water-State wird plausibilisiert.
- Survival Delta Validation nutzt passendere Speed-Grenzen.
- Fall-Damage wird serverseitig berechnet.

## Erledigt

- ~~Beschleunigung genauer prüfen.~~ Verifiziert: Server lehnt horizontale Beschleunigungs-Bursts nach einer akzeptierten Movement-Probe ab.
- ~~Movement Rate limitieren.~~ Verifiziert: Server akzeptiert maximal 30 Movement-Pakete pro Sekunde und resendet bei Rate-Spam autoritativen State.
- ~~Movement mit Sequenznummern vorbereiten.~~ Verifiziert: `PlayerMove` trägt Sequenzen; stale positive Sequenzen werden verworfen.
- ~~Server sendet autoritativen State.~~ Verifiziert: Login, akzeptierte Moves und Rejects schicken `PlayerPositionSnapshot`.
- ~~Client kann später weich korrigieren.~~ Verifiziert: Client puffert autoritative Snapshots und reconciled die Kamera weich bzw. hart bei großen Abweichungen.
- ~~Creative/Spectator-Geschwindigkeiten separat validieren.~~ Verifiziert: Common-Regeln haben Mode-Speed-Hüllen; Online-Normalpfad ist serverseitig auf Survival festgezurrt.
- ~~Teleport/Respawn/Spawn klar von normalem Movement unterscheiden.~~ Verifiziert: Nur der erste Sync hat eine eigene Initial-Distanz; danach wird jeder Teleport als normaler Move rejected und autoritativ zurückgesetzt.
- ~~Ping/lag tolerant validieren, ohne Cheats zu erlauben.~~ Verifiziert: Delta-Zeit wird mit Grace geklemmt; legitime lagged Survival-Schritte werden akzeptiert, Burst/Teleport weiter abgelehnt.

## Tests

- extreme teleport rejected.
- too fast survival move rejected.
- movement packet rate spam rejected.
- lagged survival stride accepted inside envelope.
- creative/flying speed not accepted in survival envelope.
- forged ground rejected.
- forged water rejected.
- sequence mismatch handled.
- authoritative position snapshot sent on accept/reject.

## Akzeptanz

- Server akzeptiert keine extremen Teleports.
- Speed-Hacks werden begrenzt.
- legitime Movement-Spikes durch kleine Lags werden nicht ständig hart bestraft.

---

# P2 – Collision Robustness

## Basis abgeschlossen

- Collision-Substeps oder ähnliche Lösung gegen Tunneling ist begonnen/umgesetzt.
- unbekannte Chunks werden nicht mehr als sichere Luft behandelt.

## Erledigt

- ~~Boden, Wand, Ecke, Head-Bump und Chunk-Grenzen weiter testen.~~ Verifiziert: Common-, Client- und Server-Tests decken Landing, Wall, Corner, Head-Bump und Chunk-Boundary-Kollision ab.
- ~~unbekannte Chunks dürfen nicht wie Luft behandelt werden.~~ Verifiziert: Client blockiert ungeladene Chunks; Server generiert/validiert autoritativ.
- ~~Bewegung bei Terrain Loading sanft blockieren.~~ Verifiziert: Client-Collision behandelt fehlende Chunks als blockierend statt als Luft; Server-Sweeps verhindern Tunneling.
- ~~Feedback: „Loading terrain...“ optional anzeigen.~~ Verifiziert als nicht sicherheitskritisches HUD-Polish; Movement blockiert bereits sicher.

## Weiterer Polish

- dünne Blocks/Fences später optional modellieren.
- Slabs/Paths später nur einführen, wenn Bounds-System bereit ist.
- Step Height optional prüfen.

## Akzeptanz

- Spieler tunnelt nicht durch Wände/Boden.
- Spieler bleibt an Chunk-Grenzen stabil.
- Head-Bump stoppt vertikale Velocity korrekt.
- unbekannter Chunk lässt Spieler nicht fallen.

---

# P3 – Wasserphysik

## Flags

- `feetInWater`
- `bodyInWater`
- `headUnderwater`
- `swimming`
- `breath`
- `waterDrag`

## Basis vorhanden

- Wasserflags werden serverseitig plausibilisiert.
- Wasser reduziert Fall-Damage.

## Erledigt

- ~~horizontaler Drag feinjustieren.~~ Verifiziert: `PlayerPhysics` nutzt Wasser-Control plus `waterHorizontalDrag`, statt horizontal im Wasser hart auf Zielgeschwindigkeit zu snappen.
- ~~vertikaler Swim-Assist feinjustieren.~~ Verifiziert: Jump/Space setzt im Wasser weiterhin klaren `swimRiseSpeed`, Wasser-Gravity/Drag begrenzen Auftrieb und Fall-Speed.
- ~~Auftauchen per Jump/Space klar machen.~~ Verifiziert: Common-Physics und Movement-Flags behandeln Jump im Wasser als Swim-Up; forged upward movement bleibt serverseitig an Wasser/Ground-Assist gebunden.
- ~~Breath sinkt nur bei Head Underwater.~~ Verifiziert: Client und Server ticken Breath nur bei `headUnderwater` nach unten.
- ~~Breath regeneriert über Wasser.~~ Verifiziert: Client und Server regenerieren Breath über Wasser und synchronisieren über `PlayerStatsSnapshot`.
- ~~Splash Feedback bei Eintritt/Austritt.~~ Verifiziert: Client feuert beim Wechsel von/zu water movement Water-Splash-Partikel.
- ~~Underwater visual mit Rendering/HUD abstimmen.~~ Verifiziert: HUD zeigt AIR nur underwater bzw. bei angegriffenem Breath; transparentes Wasser bleibt Render-Toggle.

## Weiterer Polish

- Strömung optional später.
- vollflächiger Underwater-Tint/Audio-Filter optional mit Rendering/Audio abstimmen.

## Tests

- Breath sinkt nur unter Wasser: `PlayerStatsTest`, `ServerPlayerSurvivalStateTest`.
- Breath regeneriert beim Auftauchen: `PlayerStatsTest`, `ServerPlayerSurvivalStateTest`.
- Wasser reduziert Fallgeschwindigkeit: `PlayerPhysicsTest`.
- Wasser verringert Fall-Damage: `ServerPlayerSurvivalStateTest`, `ServerConnectionHandlerTest`.
- forged water upward movement rejected: `PlayerMovementRules`/`ServerConnectionHandlerTest`.
- Wasser-Drag bremst horizontales Driften: `PlayerPhysicsTest`.

## Akzeptanz

- Wasser fühlt sich klar anders an.
- Spieler versteht Breath und Auftauchen.
- Server akzeptiert keine gefälschten Wasserzustände.

---

# P4 – Placement und Interaction Physics

## Basis vorhanden

- Platzieren in eigene Player-AABB wird client- und serverseitig verhindert.
- Far-Rejects für viele Interactions sind getestet.

## Erledigt

- ~~Platzierung gegen andere Spieler optional.~~ Verifiziert: Server lehnt Placement in andere Player-Bounds ab.
- ~~Platzierung gegen Entities optional.~~ Verifiziert: Client und Server prüfen Placement gegen sichtbare bzw. getrackte Entity-Bounds.
- ~~klare Feedback-Meldung bei invalid placement.~~ Verifiziert: Client meldet `Too close to place`, `Blocked by entity` oder `Can't place there`.
- ~~Range-Regeln zentral halten.~~ Verifiziert: Block-Abbau, Platzierung, Campfire, Crate, Cooking Pot, Forge, Entity Interaction und Sleep nutzen serverseitige Common-Reach-Regeln.

## Weiterer Polish

- Block-specific placement bounds für Furniture/Fences prüfen.
- block interaction line-of-sight optional prüfen.

## Akzeptanz

- Spieler kann sich nicht einbauen.
- Server nutzt dieselben Regeln wie Client-Hint.
- ungültige Interactions werden verständlich erklärt.

---

# P5 – Entity Physics

## Basis vorhanden

- Dropped Items haben Gravity/Bounce/Drag und Pickup Delay.
- Dropped Items nutzen Entity-Snapshot-System.
- Online-Pickup nutzt atomaren Claim gegen Dupe.

## Erledigt

- ~~Ambient Entities spawnen nicht in Blöcken.~~ Verifiziert: Server-seeded Ambient-Spawns laufen durch `entityPlacementClear`.
- ~~Entities respektieren grob Terrain.~~ Verifiziert: Ambient-Movement braucht solide Bodenunterstützung und blockiert solide AABB-Überlappung.
- ~~Flee/Follow läuft nicht endlos durch Wände.~~ Verifiziert: `canMoveAmbientEntity` swept Zielpfade gegen Entity-Bounds und Terrain.
- ~~einfache local avoidance.~~ Verifiziert: Tracker blockiert Ambient-Zielpositionen, die Player-/Entity-Bounds lokal überschneiden.
- ~~water avoidance pro Entity-Typ.~~ Verifiziert: bodengebundene Ambient-Entities meiden Water-Overlap/Water-Support.
- ~~prefer grass für grazers.~~ Verifiziert: Grazer dürfen `GRAZE` nur auf Grass-Support einnehmen.
- ~~fireflies ignorieren ground collision.~~ Verifiziert: Firefly/Mire-Wisp ignorieren Boden-Support, kollidieren aber weiterhin mit soliden Blöcken.
- ~~Despawn/Park außerhalb relevanter Chunks.~~ Verifiziert: Ambient-Entities ohne Follow-Target parken außerhalb des aktiven Player-Radius.

## Weiterer Polish

- Entity-Bounds Debug mit Renderer/Physics abgleichen.
- echte Chunk-Ticket-basierte Entity-Aktivierung später mit Networking/Persistence koppeln.

## Tests

- Spawn-Filter für invalid Ambient-Spawns.
- Entity-Placement gegen Solid, Water und fehlenden Support.
- swept Ambient-Pfade gegen Wand-Tunneling.
- Firefly Ground-Bypass bei weiter aktiver Solid-Collision.
- Grazer-Grazing nur auf Grass.
- Movement-Validator hält geblockte Ambient-Entities am alten Ort.
- Local Avoidance gegen Player-Bounds.
- Parken außerhalb des aktiven Player-Radius.

## Akzeptanz

- Tiere wirken nicht wie Geister durch Terrain.
- Item-Drops duplizieren sich online nicht.
- Entity-Physics verursacht keine Tick-Spikes.

---

# P6 – Fall Damage und Movement Feel

## Basis vorhanden

- Server-Schaden beginnt nach 4 Blocks Fallhöhe.
- Wasser reduziert Impact-Schaden.
- Creative/Spectator werden ausgenommen.
- Server nutzt Ground Support statt Client-onGround-Trust.

## Offen

- Damage-Kurve balancen.
- harte Landung visuell/akustisch feedbacken.
- sprint acceleration.
- grounded friction.
- air control.
- swim drag.
- jump buffer optional.
- coyote time optional.
- stamina cost an Movement koppeln.

## Akzeptanz

- Movement fühlt sich weich und kontrollierbar an.
- Fall Damage ist verständlich und nicht unfair.
- Stamina/Sprint sind spürbar, aber cozy.

---

# P7 – Projectile / Combat Physics Foundation

## Ziel

Bogen und spätere simple Combat-Features brauchen ein solides Projectile-/Hit-System.

## Offen

- Projectile Bounds definieren.
- Continuous/Swept collision für schnelle Pfeile.
- Block Hit.
- Entity Hit.
- Water slow optional.
- Gravity drop optional.
- serverseitige Lifetime.
- serverseitiger Despawn.
- Client interpoliert Projectile.

## Akzeptanz

- Pfeile tunneln nicht durch nahe Ziele.
- Server entscheidet Treffer.
- Projektile erzeugen keine Leaks.

---

# Tests

## Unit

- grounded jump.
- wall collision.
- sliding.
- ground friction.
- air control.
- head bump.
- fall impact.
- water drag.
- unknown chunk blocks movement.
- placement against player AABB.
- placement against entity AABB.
- projectile swept hit optional.

## Integration

- server rejects impossible movement.
- server rejects survival speed bursts.
- server rejects movement packet rate spam.
- server accepts lagged movement inside Survival envelope.
- server rejects stale movement sequences.
- server rejects movement into collidable blocks.
- server rejects movement tunneled through a wall.
- server rejects forged grounded movement.
- server rejects forged water state.
- server applies fall damage on landing.
- server rejects far interactions.
- server rejects placement inside player.
- server rejects placement inside other players/entities.
- server rejects far Forge cook/smelt requests.
- multiplayer snapshots stay plausible.

## Akzeptanz gesamt

- Spieler fällt nicht durch ungeladene Chunks.
- Movement fühlt sich weich an.
- Server akzeptiert keine extremen Teleports.
- Wasser ist klar spürbar.
- Block Placement kann Player nicht einklemmen.
