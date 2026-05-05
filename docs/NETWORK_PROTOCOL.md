# Adventura Network Protocol

Stand: 2026-05-01

Aktuelle Protocol Version: `25`

Dieser Contract beschreibt die transport-unabhaengigen `GamePacket`-Payloads aus `common`.
Netty rahmt jedes Packet mit einem 4-Byte-Length-Prefix; die Payload beginnt mit der 4-Byte-`PacketType`-ID.
Der Server bleibt autoritativ: Client-Packets sind Intents und duerfen nie als finales Ergebnis behandelt werden.

## Compatibility Policy

- Alpha-V1 unterstuetzt nur exakt gleiche `PROTOCOL_VERSION` zwischen Client und Server.
- N-1-Kompatibilitaet wird bewusst nicht versprochen, bis Region Storage, ActionPipeline und GameplayEventStream stabiler sind.
- Jede Aenderung an Packet-ID, Feldreihenfolge, Feldtyp, Maximalgroesse oder Semantik muss:
  - `GamePacket.PROTOCOL_VERSION` pruefen und bei inkompatiblen Aenderungen erhoehen.
  - diesen Changelog unter der neuen Version ergaenzen.
  - `ProtocolContract` und Golden-Codec-Tests aktualisieren.
- Login-Rejects fuer Protocol-Mismatch muessen die Server-Version und die gesendete Client-Version nennen.

## Version 25

- `SERVER_STATS_SNAPSHOT` meldet zusaetzlich Save-Queue-Telemetrie fuer `save.write`: pending/running/completed/failed/rejected, Bytes, Gesamt-/Durchschnittszeit und Sekundenraten.
- Debug-HUD und Engine-Budgets koennen damit Save-IO-Spikes sichtbar machen, ohne Save-Dateien selbst in den Client zu spiegeln.

## Version 24

- `GAMEPLAY_EVENTS` Packet ergaenzt serverbestaetigte GameplayEvent-Batches fuer UI/Audio/Particle/HUD-Consumer.
- `GameplayEventBatch` Schema-Version 1 wird im Packet mitkodiert; Alpha-V1 bleibt bei exakt gleicher Protocol-Version.

## Version 23

- Erster expliziter Protocol-Contract fuer alle bestehenden Packet-Typen.
- Golden-Codec-Fixtures fixieren die binaere Kodierung aller aktuellen `PacketType`-IDs.
- Compatibility-Entscheidung: kein N-1-Layer fuer Alpha-V1; gleiche Version ist Pflicht.

## Packet Contracts

| Packet | ID | Richtung | Autoritaet | Trust | Rate-Limit | Max Payload | Save/Replay | Notiz |
| --- | ---: | --- | --- | --- | --- | ---: | --- | --- |
| `HANDSHAKE` | 1 | `BIDIRECTIONAL` | `TRANSPORT_NEGOTIATION` | `NEGOTIATED` | connect once | 128 B | `NONE` | Protocol version and endpoint name. |
| `LOGIN_REQUEST` | 2 | `CLIENT_TO_SERVER` | `CLIENT_INTENT` | `UNTRUSTED_CLIENT` | connect once | 256 B | `NONE` | Username and dev-auth token; stable identity is server owned. |
| `LOGIN_ACCEPTED` | 3 | `SERVER_TO_CLIENT` | `SERVER_AUTHORITATIVE` | `SERVER_CONFIRMED` | connect once | 64 B | `NONE` | Player id, world seed and dimension bounds. |
| `LOGIN_REJECTED` | 4 | `SERVER_TO_CLIENT` | `SESSION_CONTROL` | `SERVER_CONFIRMED` | connect once | 256 B | `NONE` | Human-readable reject reason. |
| `CHUNK_DATA` | 5 | `SERVER_TO_CLIENT` | `SERVER_AUTHORITATIVE` | `SERVER_CONFIRMED` | interest streamed | 2097152 B | `SAVE_AND_REPLAY_RELEVANT` | Authoritative chunk section payload. |
| `BLOCK_UPDATE` | 6 | `SERVER_TO_CLIENT` | `SERVER_AUTHORITATIVE` | `SERVER_CONFIRMED` | interest filtered | 32 B | `SAVE_AND_REPLAY_RELEVANT` | Accepted world mutation. |
| `BLOCK_ACTION` | 7 | `CLIENT_TO_SERVER` | `CLIENT_INTENT` | `UNTRUSTED_CLIENT` | intent.block_action | 96 B | `REPLAY_RELEVANT` | Break/place intent; server validates inventory, range and world state. |
| `PLAYER_MOVE` | 8 | `CLIENT_TO_SERVER` | `CLIENT_INTENT` | `UNTRUSTED_CLIENT` | 20 Hz movement | 96 B | `REPLAY_RELEVANT` | Client movement sample with sequence and water flags. |
| `ENTITY_SNAPSHOT` | 9 | `SERVER_TO_CLIENT` | `SERVER_AUTHORITATIVE` | `SERVER_CONFIRMED` | interest tick | 262144 B | `REPLAY_RELEVANT` | Authoritative entity state snapshot. |
| `CHAT` | 10 | `BIDIRECTIONAL` | `CLIENT_TEXT_RELAY` | `UNTRUSTED_CLIENT` | chat.spam_limit | 256 B | `NONE` | Client chat intent and server broadcast share the same packet. |
| `INVENTORY_SNAPSHOT` | 11 | `SERVER_TO_CLIENT` | `SERVER_AUTHORITATIVE` | `SERVER_CONFIRMED` | after inventory mutation | 4096 B | `SAVE_AND_REPLAY_RELEVANT` | Authoritative full inventory fallback. |
| `CRAFT_REQUEST` | 12 | `CLIENT_TO_SERVER` | `CLIENT_INTENT` | `UNTRUSTED_CLIENT` | intent.craft | 256 B | `REPLAY_RELEVANT` | Craft intent with transaction id. |
| `BLOCK_INTERACT` | 13 | `CLIENT_TO_SERVER` | `CLIENT_INTENT` | `UNTRUSTED_CLIENT` | intent.block_interact | 32 B | `REPLAY_RELEVANT` | Targeted block interaction intent. |
| `STORAGE_OPEN` | 14 | `SERVER_TO_CLIENT` | `SERVER_AUTHORITATIVE` | `SERVER_CONFIRMED` | after open accept | 4096 B | `SAVE_AND_REPLAY_RELEVANT` | Authoritative storage UI snapshot. |
| `STORAGE_TRANSFER` | 15 | `CLIENT_TO_SERVER` | `CLIENT_INTENT` | `UNTRUSTED_CLIENT` | intent.storage_transfer | 64 B | `REPLAY_RELEVANT` | Storage transfer intent with transaction id. |
| `ENTITY_INTERACT` | 16 | `CLIENT_TO_SERVER` | `CLIENT_INTENT` | `UNTRUSTED_CLIENT` | intent.entity_interact | 64 B | `REPLAY_RELEVANT` | Observe/feed/attack intent. |
| `PLAYER_STATS_SNAPSHOT` | 17 | `SERVER_TO_CLIENT` | `SERVER_AUTHORITATIVE` | `SERVER_CONFIRMED` | after stat mutation | 64 B | `SAVE_AND_REPLAY_RELEVANT` | Authoritative survival stats. |
| `STORAGE_OPEN_REQUEST` | 18 | `CLIENT_TO_SERVER` | `CLIENT_INTENT` | `UNTRUSTED_CLIENT` | intent.storage_open | 32 B | `REPLAY_RELEVANT` | Storage open intent with transaction id. |
| `SLEEP_REQUEST` | 19 | `CLIENT_TO_SERVER` | `CLIENT_INTENT` | `UNTRUSTED_CLIENT` | intent.sleep | 32 B | `REPLAY_RELEVANT` | Sleep intent at a bed-like block. |
| `COOK_REQUEST` | 20 | `CLIENT_TO_SERVER` | `CLIENT_INTENT` | `UNTRUSTED_CLIENT` | intent.cook | 512 B | `REPLAY_RELEVANT` | Cooking intent with recipe, slots and transaction id. |
| `CAMPFIRE_STATUS` | 21 | `SERVER_TO_CLIENT` | `SERVER_AUTHORITATIVE` | `SERVER_CONFIRMED` | after campfire mutation | 256 B | `SAVE_AND_REPLAY_RELEVANT` | Public campfire/cooking station state. |
| `PLAYER_POSITION_SNAPSHOT` | 22 | `SERVER_TO_CLIENT` | `SERVER_AUTHORITATIVE` | `SERVER_CONFIRMED` | movement correction | 96 B | `REPLAY_RELEVANT` | Authoritative player position and correction class. |
| `SERVER_STATS_SNAPSHOT` | 23 | `SERVER_TO_CLIENT` | `SERVER_TELEMETRY` | `SERVER_CONFIRMED` | diagnostic tick | 256 B | `NONE` | Debug/diagnostic network/save queue counters. |
| `PROJECTILE_SHOOT` | 24 | `CLIENT_TO_SERVER` | `CLIENT_INTENT` | `UNTRUSTED_CLIENT` | intent.projectile_shoot | 32 B | `REPLAY_RELEVANT` | Projectile launch intent; server validates item, cooldown and durability. |
| `PROJECTILE_IMPACT` | 25 | `SERVER_TO_CLIENT` | `SERVER_AUTHORITATIVE` | `SERVER_CONFIRMED` | interest event | 256 B | `REPLAY_RELEVANT` | Authoritative projectile hit event. |
| `STORAGE_CLOSE` | 26 | `SERVER_TO_CLIENT` | `SERVER_AUTHORITATIVE` | `SERVER_CONFIRMED` | after close/reject | 32 B | `NONE` | Server closes a storage UI target. |
| `GAMEPLAY_EVENTS` | 27 | `SERVER_TO_CLIENT` | `SERVER_AUTHORITATIVE` | `SERVER_CONFIRMED` | interest event batch | 16384 B | `REPLAY_RELEVANT` | Versioned batch of server-confirmed gameplay feedback events. |

## Codec Limits

- Length prefix: 4 bytes.
- Minimum payload: 4 bytes, because every payload starts with a packet ID.
- Maximum payload: 2097152 bytes.
- Strings use Java `DataOutputStream.writeUTF`, so string byte size can be larger than Java character count.
- Array and list lengths are checked before allocation and before reading the remaining payload.
- Gameplay event batches use schema version 1 and are capped at 256 events before allocation.

## Smoke Notes

- Wrong protocol client/test packet: server sends `LoginRejected` with both versions and closes cleanly.
- Invalid frame length or unknown packet ID: decoder rejects without keeping the connection alive.
- Large chunk/entity payload work remains budgeted by P3/P4/P7 follow-up tasks.
