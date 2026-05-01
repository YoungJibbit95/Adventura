package dev.voxelgame.common.net;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.world.ChunkPos;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PacketCodec {
    public static final int MAX_PACKET_SIZE = PacketLimits.MAX_PACKET_SIZE;
    private static final int MAX_CHUNK_ARRAY_LENGTH = 128 * 1024;
    private static final int MAX_ENTITY_SNAPSHOTS = 2_048;
    private static final int MAX_ITEM_STACKS = 128;
    private static final int MAX_COOK_INPUT_SLOTS = 64;
    private static final int ITEM_STACK_BYTES = Short.BYTES + Integer.BYTES + Integer.BYTES;
    private static final int MIN_ENTITY_SNAPSHOT_BYTES = Long.BYTES
            + Short.BYTES
            + 1
            + Double.BYTES * 3
            + Float.BYTES * 2
            + Integer.BYTES
            + Short.BYTES
            + Double.BYTES * 3;

    private PacketCodec() {
    }

    public static byte[] encode(GamePacket packet) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(packet.type().id());
            switch (packet) {
                case GamePacket.Handshake handshake -> {
                    out.writeInt(handshake.protocolVersion());
                    out.writeUTF(handshake.clientName());
                }
                case GamePacket.LoginRequest login -> {
                    out.writeUTF(login.username());
                    out.writeUTF(login.authToken());
                }
                case GamePacket.LoginAccepted accepted -> {
                    writeUuid(out, accepted.playerId());
                    out.writeLong(accepted.worldSeed());
                    out.writeInt(accepted.minY());
                    out.writeInt(accepted.maxYExclusive());
                }
                case GamePacket.LoginRejected rejected -> out.writeUTF(rejected.reason());
                case GamePacket.ChunkData chunk -> {
                    out.writeInt(chunk.pos().x());
                    out.writeInt(chunk.pos().z());
                    out.writeInt(chunk.minY());
                    writeShortArray(out, chunk.blockIds());
                    writeByteArray(out, chunk.skyLight());
                    writeByteArray(out, chunk.blockLight());
                }
                case GamePacket.BlockUpdate update -> {
                    out.writeInt(update.x());
                    out.writeInt(update.y());
                    out.writeInt(update.z());
                    out.writeShort(update.blockId());
                }
                case GamePacket.BlockAction action -> {
                    out.writeUTF(action.action().name());
                    out.writeInt(action.selectedSlot());
                    out.writeInt(action.targetX());
                    out.writeInt(action.targetY());
                    out.writeInt(action.targetZ());
                    out.writeInt(action.placeX());
                    out.writeInt(action.placeY());
                    out.writeInt(action.placeZ());
                    out.writeShort(action.blockId());
                }
                case GamePacket.PlayerMove move -> {
                    out.writeLong(move.sequence());
                    out.writeDouble(move.x());
                    out.writeDouble(move.y());
                    out.writeDouble(move.z());
                    out.writeFloat(move.yaw());
                    out.writeFloat(move.pitch());
                    out.writeBoolean(move.onGround());
                    out.writeBoolean(move.feetInWater());
                    out.writeBoolean(move.bodyInWater());
                    out.writeBoolean(move.headUnderwater());
                }
                case GamePacket.PlayerPositionSnapshot snapshot -> {
                    out.writeLong(snapshot.sequence());
                    out.writeDouble(snapshot.x());
                    out.writeDouble(snapshot.y());
                    out.writeDouble(snapshot.z());
                    out.writeFloat(snapshot.yaw());
                    out.writeFloat(snapshot.pitch());
                    out.writeBoolean(snapshot.onGround());
                    out.writeBoolean(snapshot.feetInWater());
                    out.writeBoolean(snapshot.bodyInWater());
                    out.writeBoolean(snapshot.headUnderwater());
                }
                case GamePacket.BlockInteract interact -> {
                    out.writeInt(interact.selectedSlot());
                    out.writeInt(interact.targetX());
                    out.writeInt(interact.targetY());
                    out.writeInt(interact.targetZ());
                }
                case GamePacket.EntitySnapshots snapshots -> {
                    out.writeInt(snapshots.snapshots().size());
                    for (EntitySnapshot snapshot : snapshots.snapshots()) {
                        out.writeLong(snapshot.entityId());
                        out.writeUTF(snapshot.typeKey());
                        out.writeBoolean(snapshot.ownerPlayerId() != null);
                        if (snapshot.ownerPlayerId() != null) {
                            writeUuid(out, snapshot.ownerPlayerId());
                        }
                        out.writeDouble(snapshot.x());
                        out.writeDouble(snapshot.y());
                        out.writeDouble(snapshot.z());
                        out.writeFloat(snapshot.yaw());
                        out.writeFloat(snapshot.pitch());
                        out.writeInt(snapshot.health());
                        out.writeUTF(snapshot.stateKey());
                        out.writeDouble(snapshot.velocityX());
                        out.writeDouble(snapshot.velocityY());
                        out.writeDouble(snapshot.velocityZ());
                    }
                }
                case GamePacket.EntityInteract interact -> {
                    out.writeLong(interact.entityId());
                    out.writeInt(interact.selectedSlot());
                    out.writeUTF(interact.action().name());
                }
                case GamePacket.InventorySnapshot inventory -> writeItemStacks(out, inventory.slots());
                case GamePacket.PlayerStatsSnapshot stats -> {
                    out.writeInt(stats.health());
                    out.writeInt(stats.hunger());
                    out.writeInt(stats.stamina());
                    out.writeInt(stats.breath());
                    out.writeInt(stats.armor());
                    out.writeInt(stats.comfort());
                }
                case GamePacket.ServerStatsSnapshot stats -> {
                    out.writeInt(stats.chunkSubscriptions());
                    out.writeLong(stats.sentEntitySnapshotPackets());
                    out.writeLong(stats.sentEntitySnapshots());
                    out.writeLong(stats.sentChunkPackets());
                    out.writeLong(stats.sentBlockUpdates());
                    out.writeLong(stats.discardedUpdatesOutsideInterest());
                    out.writeLong(stats.rejectedChunkRequests());
                    out.writeLong(stats.failedChunkRequests());
                    out.writeLong(stats.sentPackets());
                    out.writeLong(stats.estimatedPacketBytes());
                    out.writeLong(stats.averagePacketBytes());
                    out.writeDouble(stats.packetRatePerSecond());
                }
                case GamePacket.StorageOpenRequest storage -> {
                    out.writeInt(storage.x());
                    out.writeInt(storage.y());
                    out.writeInt(storage.z());
                    out.writeInt(storage.transactionId());
                }
                case GamePacket.SleepRequest sleep -> {
                    out.writeInt(sleep.x());
                    out.writeInt(sleep.y());
                    out.writeInt(sleep.z());
                }
                case GamePacket.CookRequest cook -> {
                    out.writeInt(cook.stationX());
                    out.writeInt(cook.stationY());
                    out.writeInt(cook.stationZ());
                    out.writeUTF(cook.recipeKey());
                    out.writeInt(cook.inputSlots().size());
                    for (int slot : cook.inputSlots()) {
                        out.writeInt(slot);
                    }
                    out.writeInt(cook.transactionId());
                }
                case GamePacket.CampfireStatus status -> {
                    out.writeInt(status.x());
                    out.writeInt(status.y());
                    out.writeInt(status.z());
                    out.writeBoolean(status.active());
                    out.writeDouble(status.fuelSecondsRemaining());
                    out.writeUTF(status.cookingRecipeKey());
                    out.writeDouble(status.cookTotalSeconds());
                    out.writeDouble(status.cookSecondsRemaining());
                }
                case GamePacket.StorageOpen storage -> {
                    out.writeInt(storage.x());
                    out.writeInt(storage.y());
                    out.writeInt(storage.z());
                    writeItemStacks(out, storage.slots());
                }
                case GamePacket.StorageTransfer transfer -> {
                    out.writeInt(transfer.x());
                    out.writeInt(transfer.y());
                    out.writeInt(transfer.z());
                    out.writeBoolean(transfer.fromStorage());
                    out.writeInt(transfer.sourceSlot());
                    out.writeInt(transfer.targetSlot());
                    out.writeInt(transfer.count());
                    out.writeInt(transfer.transactionId());
                }
                case GamePacket.CraftRequest craft -> {
                    out.writeUTF(craft.recipeKey());
                    out.writeInt(craft.count());
                    out.writeBoolean(craft.hasStation());
                    out.writeInt(craft.stationX());
                    out.writeInt(craft.stationY());
                    out.writeInt(craft.stationZ());
                    out.writeInt(craft.transactionId());
                }
                case GamePacket.Chat chat -> out.writeUTF(chat.message());
            }
            out.flush();
            byte[] encoded = bytes.toByteArray();
            if (encoded.length > MAX_PACKET_SIZE) {
                throw new IllegalStateException("Encoded packet exceeds max packet size: " + encoded.length);
            }
            return encoded;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode packet", e);
        }
    }

    public static GamePacket decode(byte[] bytes) {
        if (bytes.length < PacketLimits.MIN_PACKET_SIZE || bytes.length > MAX_PACKET_SIZE) {
            throw new IllegalArgumentException("Invalid packet size: " + bytes.length);
        }
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            PacketType type = PacketType.fromId(in.readInt());
            GamePacket packet = switch (type) {
                case HANDSHAKE -> new GamePacket.Handshake(in.readInt(), in.readUTF());
                case LOGIN_REQUEST -> new GamePacket.LoginRequest(in.readUTF(), in.readUTF());
                case LOGIN_ACCEPTED -> new GamePacket.LoginAccepted(readUuid(in), in.readLong(), in.readInt(), in.readInt());
                case LOGIN_REJECTED -> new GamePacket.LoginRejected(in.readUTF());
                case CHUNK_DATA -> new GamePacket.ChunkData(
                        new ChunkPos(in.readInt(), in.readInt()),
                        in.readInt(),
                        readShortArray(in),
                        readByteArray(in),
                        readByteArray(in)
                );
                case BLOCK_UPDATE -> new GamePacket.BlockUpdate(in.readInt(), in.readInt(), in.readInt(), in.readShort());
                case BLOCK_ACTION -> new GamePacket.BlockAction(
                        GamePacket.BlockAction.Action.valueOf(in.readUTF()),
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readShort()
                );
                case PLAYER_MOVE -> new GamePacket.PlayerMove(
                        in.readLong(),
                        in.readDouble(),
                        in.readDouble(),
                        in.readDouble(),
                        in.readFloat(),
                        in.readFloat(),
                        in.readBoolean(),
                        in.readBoolean(),
                        in.readBoolean(),
                        in.readBoolean()
                );
                case PLAYER_POSITION_SNAPSHOT -> new GamePacket.PlayerPositionSnapshot(
                        in.readLong(),
                        in.readDouble(),
                        in.readDouble(),
                        in.readDouble(),
                        in.readFloat(),
                        in.readFloat(),
                        in.readBoolean(),
                        in.readBoolean(),
                        in.readBoolean(),
                        in.readBoolean()
                );
                case BLOCK_INTERACT -> new GamePacket.BlockInteract(in.readInt(), in.readInt(), in.readInt(), in.readInt());
                case ENTITY_SNAPSHOT -> {
                    int count = checkedLength(in.readInt(), MAX_ENTITY_SNAPSHOTS, in.available(), MIN_ENTITY_SNAPSHOT_BYTES, "entity snapshot count");
                    List<EntitySnapshot> snapshots = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        long entityId = in.readLong();
                        String typeKey = in.readUTF();
                        UUID owner = in.readBoolean() ? readUuid(in) : null;
                        snapshots.add(new EntitySnapshot(
                                entityId,
                                typeKey,
                                owner,
                                in.readDouble(),
                                in.readDouble(),
                                in.readDouble(),
                                in.readFloat(),
                                in.readFloat(),
                                in.readInt(),
                                in.readUTF(),
                                in.readDouble(),
                                in.readDouble(),
                                in.readDouble()
                        ));
                    }
                    yield new GamePacket.EntitySnapshots(snapshots);
                }
                case ENTITY_INTERACT -> new GamePacket.EntityInteract(
                        in.readLong(),
                        in.readInt(),
                        GamePacket.EntityInteract.Action.valueOf(in.readUTF())
                );
                case INVENTORY_SNAPSHOT -> new GamePacket.InventorySnapshot(readItemStacks(in));
                case PLAYER_STATS_SNAPSHOT -> new GamePacket.PlayerStatsSnapshot(
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readInt()
                );
                case SERVER_STATS_SNAPSHOT -> new GamePacket.ServerStatsSnapshot(
                        in.readInt(),
                        in.readLong(),
                        in.readLong(),
                        in.readLong(),
                        in.readLong(),
                        in.readLong(),
                        in.readLong(),
                        in.readLong(),
                        in.readLong(),
                        in.readLong(),
                        in.readLong(),
                        in.readDouble()
                );
                case STORAGE_OPEN_REQUEST -> new GamePacket.StorageOpenRequest(in.readInt(), in.readInt(), in.readInt(), in.readInt());
                case SLEEP_REQUEST -> new GamePacket.SleepRequest(in.readInt(), in.readInt(), in.readInt());
                case COOK_REQUEST -> {
                    int stationX = in.readInt();
                    int stationY = in.readInt();
                    int stationZ = in.readInt();
                    String recipeKey = in.readUTF();
                    int count = checkedLength(in.readInt(), MAX_COOK_INPUT_SLOTS, in.available(), Integer.BYTES, "cook input slot count");
                    List<Integer> inputSlots = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        inputSlots.add(in.readInt());
                    }
                    yield new GamePacket.CookRequest(stationX, stationY, stationZ, recipeKey, inputSlots, in.readInt());
                }
                case CAMPFIRE_STATUS -> new GamePacket.CampfireStatus(
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readBoolean(),
                        in.readDouble(),
                        in.readUTF(),
                        in.readDouble(),
                        in.readDouble()
                );
                case STORAGE_OPEN -> new GamePacket.StorageOpen(in.readInt(), in.readInt(), in.readInt(), readItemStacks(in));
                case STORAGE_TRANSFER -> new GamePacket.StorageTransfer(
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readBoolean(),
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readInt()
                );
                case CRAFT_REQUEST -> new GamePacket.CraftRequest(
                        in.readUTF(),
                        in.readInt(),
                        in.readBoolean(),
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readInt()
                );
                case CHAT -> new GamePacket.Chat(in.readUTF());
            };
            if (in.available() != 0) {
                throw new IllegalArgumentException("Trailing bytes after packet decode: " + in.available());
            }
            return packet;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode packet", e);
        }
    }

    private static void writeUuid(DataOutputStream out, UUID uuid) throws IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    private static void writeShortArray(DataOutputStream out, short[] values) throws IOException {
        out.writeInt(values.length);
        for (short value : values) {
            out.writeShort(value);
        }
    }

    private static short[] readShortArray(DataInputStream in) throws IOException {
        int length = checkedLength(in.readInt(), MAX_CHUNK_ARRAY_LENGTH, in.available(), Short.BYTES, "short array length");
        short[] values = new short[length];
        for (int i = 0; i < length; i++) {
            values[i] = in.readShort();
        }
        return values;
    }

    private static void writeByteArray(DataOutputStream out, byte[] values) throws IOException {
        out.writeInt(values.length);
        out.write(values);
    }

    private static byte[] readByteArray(DataInputStream in) throws IOException {
        int length = checkedLength(in.readInt(), MAX_CHUNK_ARRAY_LENGTH, in.available(), 1, "byte array length");
        byte[] values = new byte[length];
        in.readFully(values);
        return values;
    }

    private static void writeItemStacks(DataOutputStream out, List<ItemStack> stacks) throws IOException {
        if (stacks.size() > MAX_ITEM_STACKS) {
            throw new IllegalArgumentException("Invalid item stack count: " + stacks.size());
        }
        out.writeInt(stacks.size());
        for (ItemStack stack : stacks) {
            out.writeShort(stack.itemId());
            out.writeInt(stack.count());
            out.writeInt(stack.damage());
        }
    }

    private static List<ItemStack> readItemStacks(DataInputStream in) throws IOException {
        int length = checkedLength(in.readInt(), MAX_ITEM_STACKS, in.available(), ITEM_STACK_BYTES, "item stack count");
        List<ItemStack> stacks = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            short itemId = in.readShort();
            int count = in.readInt();
            int damage = in.readInt();
            if (itemId < 0) {
                throw new IllegalArgumentException("Invalid item id: " + itemId);
            }
            if (count < 0) {
                throw new IllegalArgumentException("Invalid item count: " + count);
            }
            if (damage < 0) {
                throw new IllegalArgumentException("Invalid item damage: " + damage);
            }
            stacks.add(new ItemStack(itemId, count, damage));
        }
        return stacks;
    }

    private static int checkedLength(int length, int maxLength, int availableBytes, int minBytesPerEntry, String label) {
        if (length < 0 || length > maxLength) {
            throw new IllegalArgumentException("Invalid " + label + ": " + length);
        }
        long minimumPayloadBytes = (long) length * minBytesPerEntry;
        if (minimumPayloadBytes > availableBytes) {
            throw new IllegalArgumentException("Invalid " + label + ": " + length + " exceeds remaining payload");
        }
        return length;
    }
}
