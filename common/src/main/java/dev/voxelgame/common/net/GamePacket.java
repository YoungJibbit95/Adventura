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
        GamePacket.PlayerMove,
        GamePacket.EntitySnapshots,
        GamePacket.InventorySnapshot,
        GamePacket.CraftRequest,
        GamePacket.Chat {

    int PROTOCOL_VERSION = 3;

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

    record EntitySnapshots(List<EntitySnapshot> snapshots) implements GamePacket {
        public EntitySnapshots {
            snapshots = List.copyOf(snapshots);
        }

        @Override
        public PacketType type() {
            return PacketType.ENTITY_SNAPSHOT;
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

    record CraftRequest(String recipeKey) implements GamePacket {
        public CraftRequest {
            Objects.requireNonNull(recipeKey, "recipeKey");
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
