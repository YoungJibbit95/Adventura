package dev.voxelgame.common.net;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.world.ChunkPos;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public sealed interface GamePacket permits
        GamePacket.Handshake,
        GamePacket.LoginRequest,
        GamePacket.LoginAccepted,
        GamePacket.LoginRejected,
        GamePacket.ChunkData,
        GamePacket.BlockUpdate,
        GamePacket.BlockAction,
        GamePacket.BlockInteract,
        GamePacket.PlayerMove,
        GamePacket.EntitySnapshots,
        GamePacket.EntityInteract,
        GamePacket.InventorySnapshot,
        GamePacket.PlayerStatsSnapshot,
        GamePacket.StorageOpenRequest,
        GamePacket.SleepRequest,
        GamePacket.CookRequest,
        GamePacket.StorageOpen,
        GamePacket.StorageTransfer,
        GamePacket.CraftRequest,
        GamePacket.Chat {

    int PROTOCOL_VERSION = 15;

    PacketType type();

    record Handshake(int protocolVersion, String clientName) implements GamePacket {
        public Handshake {
            Objects.requireNonNull(clientName, "clientName");
        }

        @Override
        public PacketType type() {
            return PacketType.HANDSHAKE;
        }
    }

    record LoginRequest(String username, String authToken) implements GamePacket {
        public LoginRequest {
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(authToken, "authToken");
        }

        @Override
        public PacketType type() {
            return PacketType.LOGIN_REQUEST;
        }
    }

    record LoginAccepted(UUID playerId, long worldSeed, int minY, int maxYExclusive) implements GamePacket {
        public LoginAccepted {
            Objects.requireNonNull(playerId, "playerId");
        }

        @Override
        public PacketType type() {
            return PacketType.LOGIN_ACCEPTED;
        }
    }

    record LoginRejected(String reason) implements GamePacket {
        public LoginRejected {
            Objects.requireNonNull(reason, "reason");
        }

        @Override
        public PacketType type() {
            return PacketType.LOGIN_REJECTED;
        }
    }

    record ChunkData(ChunkPos pos, int minY, short[] blockIds, byte[] skyLight, byte[] blockLight) implements GamePacket {
        public ChunkData {
            Objects.requireNonNull(pos, "pos");
            blockIds = blockIds.clone();
            skyLight = skyLight.clone();
            blockLight = blockLight.clone();
        }

        public short[] blockIds() {
            return blockIds.clone();
        }

        public byte[] skyLight() {
            return skyLight.clone();
        }

        public byte[] blockLight() {
            return blockLight.clone();
        }

        @Override
        public PacketType type() {
            return PacketType.CHUNK_DATA;
        }
    }

    record BlockUpdate(int x, int y, int z, short blockId) implements GamePacket {
        @Override
        public PacketType type() {
            return PacketType.BLOCK_UPDATE;
        }
    }

    record BlockAction(
            Action action,
            int selectedSlot,
            int targetX,
            int targetY,
            int targetZ,
            int placeX,
            int placeY,
            int placeZ,
            short blockId
    ) implements GamePacket {
        public BlockAction {
            Objects.requireNonNull(action, "action");
            if (selectedSlot < 0) {
                throw new IllegalArgumentException("selectedSlot must be >= 0");
            }
        }

        @Override
        public PacketType type() {
            return PacketType.BLOCK_ACTION;
        }

        public enum Action {
            BREAK,
            PLACE
        }
    }

    record PlayerMove(double x, double y, double z, float yaw, float pitch, boolean onGround) implements GamePacket {
        @Override
        public PacketType type() {
            return PacketType.PLAYER_MOVE;
        }
    }

    record BlockInteract(int selectedSlot, int targetX, int targetY, int targetZ) implements GamePacket {
        public BlockInteract {
            if (selectedSlot < 0) {
                throw new IllegalArgumentException("selectedSlot must be >= 0");
            }
        }

        @Override
        public PacketType type() {
            return PacketType.BLOCK_INTERACT;
        }
    }

    record EntitySnapshots(List<EntitySnapshot> snapshots) implements GamePacket {
        public EntitySnapshots {
            snapshots = List.copyOf(snapshots);
        }

        @Override
        public PacketType type() {
            return PacketType.ENTITY_SNAPSHOT;
        }
    }

    record EntityInteract(long entityId, int selectedSlot, Action action) implements GamePacket {
        public EntityInteract {
            if (selectedSlot < 0) {
                throw new IllegalArgumentException("selectedSlot must be >= 0");
            }
            Objects.requireNonNull(action, "action");
        }

        @Override
        public PacketType type() {
            return PacketType.ENTITY_INTERACT;
        }

        public enum Action {
            OBSERVE,
            FEED
        }
    }

    record InventorySnapshot(List<ItemStack> slots) implements GamePacket {
        public InventorySnapshot {
            slots = List.copyOf(slots);
        }

        @Override
        public PacketType type() {
            return PacketType.INVENTORY_SNAPSHOT;
        }
    }

    record PlayerStatsSnapshot(int health, int hunger, int stamina, int breath, int armor, int comfort) implements GamePacket {
        public PlayerStatsSnapshot {
            if (health < 0 || hunger < 0 || stamina < 0 || breath < 0 || armor < 0 || comfort < 0) {
                throw new IllegalArgumentException("Player stats cannot be negative");
            }
        }

        @Override
        public PacketType type() {
            return PacketType.PLAYER_STATS_SNAPSHOT;
        }
    }

    record StorageOpenRequest(int x, int y, int z) implements GamePacket {
        @Override
        public PacketType type() {
            return PacketType.STORAGE_OPEN_REQUEST;
        }
    }

    record SleepRequest(int x, int y, int z) implements GamePacket {
        @Override
        public PacketType type() {
            return PacketType.SLEEP_REQUEST;
        }
    }

    record CookRequest(int stationX, int stationY, int stationZ, String recipeKey, List<Integer> inputSlots) implements GamePacket {
        public CookRequest {
            Objects.requireNonNull(recipeKey, "recipeKey");
            Objects.requireNonNull(inputSlots, "inputSlots");
            inputSlots = List.copyOf(inputSlots);
            if (inputSlots.isEmpty() || inputSlots.size() > 9) {
                throw new IllegalArgumentException("inputSlots must contain 1..9 slots");
            }
            for (int slot : inputSlots) {
                if (slot < 0) {
                    throw new IllegalArgumentException("inputSlots cannot contain negative slots");
                }
            }
        }

        @Override
        public PacketType type() {
            return PacketType.COOK_REQUEST;
        }
    }

    record StorageOpen(int x, int y, int z, List<ItemStack> slots) implements GamePacket {
        public StorageOpen {
            slots = List.copyOf(slots);
        }

        @Override
        public PacketType type() {
            return PacketType.STORAGE_OPEN;
        }
    }

    record StorageTransfer(int x, int y, int z, boolean fromStorage, int sourceSlot, int targetSlot, int count, int transactionId) implements GamePacket {
        public static final int AUTO_TARGET_SLOT = -1;

        public StorageTransfer(int x, int y, int z, boolean fromStorage, int slot) {
            this(x, y, z, fromStorage, slot, AUTO_TARGET_SLOT, Integer.MAX_VALUE, 1);
        }

        public StorageTransfer {
            if (sourceSlot < 0) {
                throw new IllegalArgumentException("sourceSlot must be >= 0");
            }
            if (targetSlot < AUTO_TARGET_SLOT) {
                throw new IllegalArgumentException("targetSlot must be >= -1");
            }
            if (count < 1) {
                throw new IllegalArgumentException("count must be >= 1");
            }
            if (transactionId < 0) {
                throw new IllegalArgumentException("transactionId must be >= 0");
            }
        }

        public int slot() {
            return sourceSlot;
        }

        @Override
        public PacketType type() {
            return PacketType.STORAGE_TRANSFER;
        }
    }

    record CraftRequest(String recipeKey, int count, boolean hasStation, int stationX, int stationY, int stationZ) implements GamePacket {
        public CraftRequest {
            Objects.requireNonNull(recipeKey, "recipeKey");
            if (count < 1 || count > 64) {
                throw new IllegalArgumentException("count must be in 1..64");
            }
        }

        public CraftRequest(String recipeKey) {
            this(recipeKey, 1, false, 0, 0, 0);
        }

        public static CraftRequest atStation(String recipeKey, int count, int stationX, int stationY, int stationZ) {
            return new CraftRequest(recipeKey, count, true, stationX, stationY, stationZ);
        }

        @Override
        public PacketType type() {
            return PacketType.CRAFT_REQUEST;
        }
    }

    record Chat(String message) implements GamePacket {
        public Chat {
            Objects.requireNonNull(message, "message");
        }

        @Override
        public PacketType type() {
            return PacketType.CHAT;
        }
    }
}
