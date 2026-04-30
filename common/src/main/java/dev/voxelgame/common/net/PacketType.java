package dev.voxelgame.common.net;

import java.util.HashMap;
import java.util.Map;

public enum PacketType {
    HANDSHAKE(1),
    LOGIN_REQUEST(2),
    LOGIN_ACCEPTED(3),
    LOGIN_REJECTED(4),
    CHUNK_DATA(5),
    BLOCK_UPDATE(6),
    BLOCK_ACTION(7),
    PLAYER_MOVE(8),
    ENTITY_SNAPSHOT(9),
    CHAT(10),
    INVENTORY_SNAPSHOT(11),
    CRAFT_REQUEST(12),
    BLOCK_INTERACT(13),
    STORAGE_OPEN(14),
    STORAGE_TRANSFER(15),
    ENTITY_INTERACT(16),
    PLAYER_STATS_SNAPSHOT(17),
    STORAGE_OPEN_REQUEST(18),
    SLEEP_REQUEST(19),
    COOK_REQUEST(20),
    CAMPFIRE_STATUS(21),
    PLAYER_POSITION_SNAPSHOT(22);

    private static final Map<Integer, PacketType> BY_ID = new HashMap<>();

    static {
        for (PacketType type : values()) {
            BY_ID.put(type.id, type);
        }
    }

    private final int id;

    PacketType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static PacketType fromId(int id) {
        PacketType type = BY_ID.get(id);
        if (type == null) {
            throw new IllegalArgumentException("Unknown packet id: " + id);
        }
        return type;
    }
}
