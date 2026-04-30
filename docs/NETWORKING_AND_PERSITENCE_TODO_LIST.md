# Adventura – Networking & Persistence TODO List

Stand: 2026-04-30

## Ziel

Networking und Persistenz sollen dafür sorgen, dass Adventura online sicher und offline dauerhaft spielbar ist. Der Server bleibt autoritativ. Der Client sendet Intents, keine finalen Ergebnisse.

Diese Liste ergänzt die Engine-Liste und sammelt Multiplayer-, Packet-, Save-/Load- und BlockEntity-Sync-Themen.

---

# P0 – Packet-Sicherheit

## Basis vorhanden

- Client- und Server-Decoder begrenzen Packet-Größe.
- zu kleine und zu große Frames werden verworfen.
- `PacketLimits` zentralisiert Grenzwerte.
- `PacketCodec` prüft Listen-/Array-Längen gegen Maximalwerte und Payload-Budget.
- Textfelder für Login, Recipe Keys und Chat haben Limits.
- trailing bytes nach Packetdecode werden abgelehnt.

## Offen

- alle neuen Packets sofort in Roundtrip-Tests aufnehmen.
- alle neuen Stringfelder mit Domain-Limits versehen.
- alle neuen Listen/Arrays budgetieren.
- Packet Reject Metrics zählen.
- ungültige Packets ohne Servercrash loggen.
- Rate Limits für spammbare Intents prüfen:
  - interact
  - craft
  - cook
  - transfer
  - shoot
  - chat

## Akzeptanz

- manipulierte Längenfelder erzeugen keine OOMs.
- kaputte Frames crashen nicht den Server.
- neue Packets haben Tests und Limits.

---

# P1 – Client Intents und Server-Autorität

## Regeln

- Client sendet nur Absicht.
- Server validiert.
- Server mutiert Welt/Inventar/Entities.
- Server sendet Snapshot/Result.
- Client zeigt Feedback.

## Wichtige Intents

- Block Break
- Block Place
- Block Interact
- Craft
- Cook
- Storage Open
- Storage Transfer
- Entity Interact
- Sleep
- Shoot Projectile
- Journal/Lore pickup optional

## Offen

- Recipe Unlock Validation serverseitig.
- Cooking Pot / Forge validation.
- Loot crate validation.
- Entity damage validation.
- projectile shoot validation.
- transfer transaction reconciliation.

## Akzeptanz

- Client kann keine Items duplizieren.
- Client kann locked recipes nicht ausführen.
- Client kann Loot nicht resetten.
- Client kann Entity Health nicht setzen.

---

# P2 – Interest Management

## Ziel

Server sendet nur relevante Welt- und Entity-Daten an Clients.

## Offen

- Chunk subscriptions pro Client.
- Entity snapshots nach Radius/Chunk filtern.
- Block updates nur an abonnierte Chunks.
- BlockEntity snapshots nur an Spieler mit UI/Interest.
- Structure/Loot updates nur an relevante Spieler.
- packet stats pro Client.
- Debug Log für Interest Reject/Filter.

## Interest-Regeln

- Player position bestimmt aktive Chunk-Ringe.
- Render Distance beeinflusst Client-Wunsch.
- Server darf maximale Distance begrenzen.
- Entity Interest kann kleiner sein als Chunk Interest.
- BlockEntity UI Interest ist reichweitenbasiert.

## Akzeptanz

- Spieler bekommt keine Updates aus weit entfernten Gebieten.
- Multiplayer skaliert besser.
- Client kann Chunk-Unload sicher nutzen.

---

# P3 – Async Server Chunk Generation

## Problem

Chunk-Erzeugung darf Netty-Handler nicht blockieren.

## Offen

- Server Chunk Request Queue.
- Worker Threads.
- Request Deduplikation.
- Priorität nach Spielerposition.
- Packet-Auslieferung nach Fertigstellung.
- Cancel/Ignore, wenn Spieler Chunk nicht mehr braucht.
- Backpressure bei Queue Overflow.
- Error handling.

## Akzeptanz

- Netty bleibt responsiv.
- Chunk Requests blockieren nicht den Netzwerkthread.
- Chunks werden nur an relevante Clients gesendet.
- doppelte Requests werden zusammengeführt.

---

# P4 – World Persistence

## Ziel

Spieleränderungen und generierte Spezialzustände bleiben nach Neustart erhalten.

## World Save Daten

- seed
- save version
- modified blocks
- removed generated blocks
- placed blocks
- block entities
- campfire state
- storage contents
- cooking/forge state
- loot generated/opened flags
- structure states
- optional persistent entities

## Offen

- versioniertes Save-Format.
- world diff storage.
- chunk diff storage.
- save-on-exit.
- periodic autosave optional.
- safe write mit temp file + rename.
- backup vor Migration.
- unknown block/item fallback.

## Akzeptanz

- abgebaute Blöcke bleiben entfernt.
- platzierte Blöcke bleiben gesetzt.
- Kisteninhalte bleiben erhalten.
- Loot resetet nicht.
- Save-Crash beschädigt Welt nicht leicht.

---

# P5 – Player Persistence

## Zu speichern

- player id/name
- position
- rotation optional
- inventory
- selected hotbar slot
- health
- hunger
- stamina
- breath
- spawn point
- gamemode
- discovered recipes
- discovered biomes
- journal entries
- known structures optional

## Offen

- Player Save V1.
- Player Load V1.
- safe fallback bei invalid position.
- inventory item alias mapping.
- unknown item fallback.
- last safe position optional.
- respawn state.

## Akzeptanz

- Spieler behält Fortschritt nach Neustart.
- ungültige Items crashen nicht.
- Spieler lädt nicht in Wand/Wasser/Cave.

---

# P6 – BlockEntity Sync

## BlockEntity Typen

- StorageCrate
- LootCrate
- Campfire
- CookingPot
- Forge
- Workbench optional
- SleepingMat optional

## Offen

- serverseitiger BlockEntityStore.
- UI open request.
- snapshot response.
- transfer/cook/smelt requests mit transaction id.
- range validation.
- block still exists validation.
- item stack validation.
- output blocked validation.
- close UI optional.

## Akzeptanz

- UI zeigt serverseitigen Zustand.
- invalid transfer/cook/smelt wird abgelehnt.
- keine Dupes.
- zerstörter Block schließt/invalidiert UI.

---

# P7 – Authoritative Movement / Reconciliation

## Offen

- Movement sequencing.
- server authoritative state snapshots.
- client correction smoothing.
- teleport/respawn as explicit event.
- move rate limit.
- physics validation using common rules.
- server fall damage based on accepted moves.

## Akzeptanz

- Movement bleibt flüssig.
- Cheats werden begrenzt.
- Korrekturen sind nicht extrem hart.

---

# P8 – Save/Network Tests

## Unit Tests

- Packet roundtrip für alle Packets.
- Packet too small rejected.
- Packet too large rejected.
- trailing bytes rejected.
- string limit tests.
- array/list limit tests.
- save version parse.
- item alias load.
- unknown item fallback.
- block entity serialization.

## Integration Tests

- multiplayer craft validation.
- multiplayer cook validation.
- storage transfer no dupe.
- loot chest generated once.
- save/reload crate contents.
- save/reload player inventory.
- client receives only interested chunk updates.
- entity snapshots filtered by distance.

## Akzeptanz

- Multiplayer bleibt serverautoritativ.
- Save/Load ist robust.
- Packet-Limits schützen Client und Server.