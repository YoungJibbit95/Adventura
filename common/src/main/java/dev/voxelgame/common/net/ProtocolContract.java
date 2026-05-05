package dev.voxelgame.common.net;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ProtocolContract {
    private static final EnumMap<PacketType, Entry> ENTRIES = new EnumMap<>(PacketType.class);

    static {
        register(PacketType.HANDSHAKE, Direction.BIDIRECTIONAL, Authority.TRANSPORT_NEGOTIATION, TrustLevel.NEGOTIATED, "connect once", 128, SaveReplayRelevance.NONE, "Protocol version and endpoint name.");
        register(PacketType.LOGIN_REQUEST, Direction.CLIENT_TO_SERVER, Authority.CLIENT_INTENT, TrustLevel.UNTRUSTED_CLIENT, "connect once", 256, SaveReplayRelevance.NONE, "Username and dev-auth token; stable identity is server owned.");
        register(PacketType.LOGIN_ACCEPTED, Direction.SERVER_TO_CLIENT, Authority.SERVER_AUTHORITATIVE, TrustLevel.SERVER_CONFIRMED, "connect once", 64, SaveReplayRelevance.NONE, "Player id, world seed and dimension bounds.");
        register(PacketType.LOGIN_REJECTED, Direction.SERVER_TO_CLIENT, Authority.SESSION_CONTROL, TrustLevel.SERVER_CONFIRMED, "connect once", 256, SaveReplayRelevance.NONE, "Human-readable reject reason.");
        register(PacketType.CHUNK_DATA, Direction.SERVER_TO_CLIENT, Authority.SERVER_AUTHORITATIVE, TrustLevel.SERVER_CONFIRMED, "interest streamed", PacketLimits.MAX_PACKET_SIZE, SaveReplayRelevance.SAVE_AND_REPLAY_RELEVANT, "Authoritative chunk section payload.");
        register(PacketType.BLOCK_UPDATE, Direction.SERVER_TO_CLIENT, Authority.SERVER_AUTHORITATIVE, TrustLevel.SERVER_CONFIRMED, "interest filtered", 32, SaveReplayRelevance.SAVE_AND_REPLAY_RELEVANT, "Accepted world mutation.");
        register(PacketType.BLOCK_ACTION, Direction.CLIENT_TO_SERVER, Authority.CLIENT_INTENT, TrustLevel.UNTRUSTED_CLIENT, "intent.block_action", 96, SaveReplayRelevance.REPLAY_RELEVANT, "Break/place intent; server validates inventory, range and world state.");
        register(PacketType.PLAYER_MOVE, Direction.CLIENT_TO_SERVER, Authority.CLIENT_INTENT, TrustLevel.UNTRUSTED_CLIENT, "20 Hz movement", 96, SaveReplayRelevance.REPLAY_RELEVANT, "Client movement sample with sequence and water flags.");
        register(PacketType.ENTITY_SNAPSHOT, Direction.SERVER_TO_CLIENT, Authority.SERVER_AUTHORITATIVE, TrustLevel.SERVER_CONFIRMED, "interest tick", 256 * 1024, SaveReplayRelevance.REPLAY_RELEVANT, "Authoritative entity state snapshot.");
        register(PacketType.CHAT, Direction.BIDIRECTIONAL, Authority.CLIENT_TEXT_RELAY, TrustLevel.UNTRUSTED_CLIENT, "chat.spam_limit", 256, SaveReplayRelevance.NONE, "Client chat intent and server broadcast share the same packet.");
        register(PacketType.INVENTORY_SNAPSHOT, Direction.SERVER_TO_CLIENT, Authority.SERVER_AUTHORITATIVE, TrustLevel.SERVER_CONFIRMED, "after inventory mutation", 4 * 1024, SaveReplayRelevance.SAVE_AND_REPLAY_RELEVANT, "Authoritative full inventory fallback.");
        register(PacketType.CRAFT_REQUEST, Direction.CLIENT_TO_SERVER, Authority.CLIENT_INTENT, TrustLevel.UNTRUSTED_CLIENT, "intent.craft", 256, SaveReplayRelevance.REPLAY_RELEVANT, "Craft intent with transaction id.");
        register(PacketType.BLOCK_INTERACT, Direction.CLIENT_TO_SERVER, Authority.CLIENT_INTENT, TrustLevel.UNTRUSTED_CLIENT, "intent.block_interact", 32, SaveReplayRelevance.REPLAY_RELEVANT, "Targeted block interaction intent.");
        register(PacketType.STORAGE_OPEN, Direction.SERVER_TO_CLIENT, Authority.SERVER_AUTHORITATIVE, TrustLevel.SERVER_CONFIRMED, "after open accept", 4 * 1024, SaveReplayRelevance.SAVE_AND_REPLAY_RELEVANT, "Authoritative storage UI snapshot.");
        register(PacketType.STORAGE_TRANSFER, Direction.CLIENT_TO_SERVER, Authority.CLIENT_INTENT, TrustLevel.UNTRUSTED_CLIENT, "intent.storage_transfer", 64, SaveReplayRelevance.REPLAY_RELEVANT, "Storage transfer intent with transaction id.");
        register(PacketType.ENTITY_INTERACT, Direction.CLIENT_TO_SERVER, Authority.CLIENT_INTENT, TrustLevel.UNTRUSTED_CLIENT, "intent.entity_interact", 64, SaveReplayRelevance.REPLAY_RELEVANT, "Observe/feed/attack intent.");
        register(PacketType.PLAYER_STATS_SNAPSHOT, Direction.SERVER_TO_CLIENT, Authority.SERVER_AUTHORITATIVE, TrustLevel.SERVER_CONFIRMED, "after stat mutation", 64, SaveReplayRelevance.SAVE_AND_REPLAY_RELEVANT, "Authoritative survival stats.");
        register(PacketType.STORAGE_OPEN_REQUEST, Direction.CLIENT_TO_SERVER, Authority.CLIENT_INTENT, TrustLevel.UNTRUSTED_CLIENT, "intent.storage_open", 32, SaveReplayRelevance.REPLAY_RELEVANT, "Storage open intent with transaction id.");
        register(PacketType.SLEEP_REQUEST, Direction.CLIENT_TO_SERVER, Authority.CLIENT_INTENT, TrustLevel.UNTRUSTED_CLIENT, "intent.sleep", 32, SaveReplayRelevance.REPLAY_RELEVANT, "Sleep intent at a bed-like block.");
        register(PacketType.COOK_REQUEST, Direction.CLIENT_TO_SERVER, Authority.CLIENT_INTENT, TrustLevel.UNTRUSTED_CLIENT, "intent.cook", 512, SaveReplayRelevance.REPLAY_RELEVANT, "Cooking intent with recipe, slots and transaction id.");
        register(PacketType.CAMPFIRE_STATUS, Direction.SERVER_TO_CLIENT, Authority.SERVER_AUTHORITATIVE, TrustLevel.SERVER_CONFIRMED, "after campfire mutation", 256, SaveReplayRelevance.SAVE_AND_REPLAY_RELEVANT, "Public campfire/cooking station state.");
        register(PacketType.PLAYER_POSITION_SNAPSHOT, Direction.SERVER_TO_CLIENT, Authority.SERVER_AUTHORITATIVE, TrustLevel.SERVER_CONFIRMED, "movement correction", 96, SaveReplayRelevance.REPLAY_RELEVANT, "Authoritative player position and correction class.");
        register(PacketType.SERVER_STATS_SNAPSHOT, Direction.SERVER_TO_CLIENT, Authority.SERVER_TELEMETRY, TrustLevel.SERVER_CONFIRMED, "diagnostic tick", 256, SaveReplayRelevance.NONE, "Debug/diagnostic network/save queue counters.");
        register(PacketType.PROJECTILE_SHOOT, Direction.CLIENT_TO_SERVER, Authority.CLIENT_INTENT, TrustLevel.UNTRUSTED_CLIENT, "intent.projectile_shoot", 32, SaveReplayRelevance.REPLAY_RELEVANT, "Projectile launch intent; server validates item, cooldown and durability.");
        register(PacketType.PROJECTILE_IMPACT, Direction.SERVER_TO_CLIENT, Authority.SERVER_AUTHORITATIVE, TrustLevel.SERVER_CONFIRMED, "interest event", 256, SaveReplayRelevance.REPLAY_RELEVANT, "Authoritative projectile hit event.");
        register(PacketType.STORAGE_CLOSE, Direction.SERVER_TO_CLIENT, Authority.SERVER_AUTHORITATIVE, TrustLevel.SERVER_CONFIRMED, "after close/reject", 32, SaveReplayRelevance.NONE, "Server closes a storage UI target.");
        register(PacketType.GAMEPLAY_EVENTS, Direction.SERVER_TO_CLIENT, Authority.SERVER_AUTHORITATIVE, TrustLevel.SERVER_CONFIRMED, "interest event batch", 16 * 1024, SaveReplayRelevance.REPLAY_RELEVANT, "Versioned batch of server-confirmed gameplay feedback events.");

        for (PacketType type : PacketType.values()) {
            if (!ENTRIES.containsKey(type)) {
                throw new IllegalStateException("Missing protocol contract for packet type: " + type);
            }
        }
    }

    private ProtocolContract() {
    }

    public static Entry require(PacketType type) {
        Entry entry = ENTRIES.get(Objects.requireNonNull(type, "type"));
        if (entry == null) {
            throw new IllegalArgumentException("Missing protocol contract for packet type: " + type);
        }
        return entry;
    }

    public static List<Entry> entries() {
        return List.copyOf(ENTRIES.values());
    }

    public static Map<PacketType, Entry> asMap() {
        return Map.copyOf(ENTRIES);
    }

    private static void register(
            PacketType type,
            Direction direction,
            Authority authority,
            TrustLevel trustLevel,
            String rateLimit,
            int maxPayloadBytes,
            SaveReplayRelevance saveReplayRelevance,
            String notes
    ) {
        Entry entry = new Entry(type, direction, authority, trustLevel, rateLimit, maxPayloadBytes, saveReplayRelevance, notes);
        Entry previous = ENTRIES.put(type, entry);
        if (previous != null) {
            throw new IllegalStateException("Duplicate protocol contract for packet type: " + type);
        }
    }

    public record Entry(
            PacketType type,
            Direction direction,
            Authority authority,
            TrustLevel trustLevel,
            String rateLimit,
            int maxPayloadBytes,
            SaveReplayRelevance saveReplayRelevance,
            String notes
    ) {
        public Entry {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(direction, "direction");
            Objects.requireNonNull(authority, "authority");
            Objects.requireNonNull(trustLevel, "trustLevel");
            rateLimit = requireText(rateLimit, "rateLimit");
            if (maxPayloadBytes < PacketLimits.MIN_PACKET_SIZE || maxPayloadBytes > PacketLimits.MAX_PACKET_SIZE) {
                throw new IllegalArgumentException("maxPayloadBytes outside packet limits: " + maxPayloadBytes);
            }
            Objects.requireNonNull(saveReplayRelevance, "saveReplayRelevance");
            notes = requireText(notes, "notes");
        }
    }

    public enum Direction {
        CLIENT_TO_SERVER,
        SERVER_TO_CLIENT,
        BIDIRECTIONAL
    }

    public enum Authority {
        CLIENT_INTENT,
        CLIENT_TEXT_RELAY,
        SERVER_AUTHORITATIVE,
        SERVER_TELEMETRY,
        SESSION_CONTROL,
        TRANSPORT_NEGOTIATION
    }

    public enum TrustLevel {
        UNTRUSTED_CLIENT,
        NEGOTIATED,
        SERVER_CONFIRMED
    }

    public enum SaveReplayRelevance {
        NONE,
        SAVE_AND_REPLAY_RELEVANT,
        REPLAY_RELEVANT
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }
}
