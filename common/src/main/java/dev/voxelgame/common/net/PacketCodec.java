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
    private static final int MAX_ARRAY_LENGTH = 1_000_000;

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
                    out.writeDouble(move.x());
                    out.writeDouble(move.y());
                    out.writeDouble(move.z());
                    out.writeFloat(move.yaw());
                    out.writeFloat(move.pitch());
                    out.writeBoolean(move.onGround());
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
                    }
                }
                case GamePacket.InventorySnapshot inventory -> writeItemStacks(out, inventory.slots());
                case GamePacket.CraftRequest craft -> out.writeUTF(craft.recipeKey());
                case GamePacket.Chat chat -> out.writeUTF(chat.message());
            }
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode packet", e);
        }
    }

    public static GamePacket decode(byte[] bytes) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            PacketType type = PacketType.fromId(in.readInt());
            return switch (type) {
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
                case PLAYER_MOVE -> new GamePacket.PlayerMove(in.readDouble(), in.readDouble(), in.readDouble(), in.readFloat(), in.readFloat(), in.readBoolean());
                case BLOCK_INTERACT -> new GamePacket.BlockInteract(in.readInt(), in.readInt(), in.readInt(), in.readInt());
                case ENTITY_SNAPSHOT -> {
                    int count = checkedLength(in.readInt());
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
                                in.readInt()
                        ));
                    }
                    yield new GamePacket.EntitySnapshots(snapshots);
                }
                case INVENTORY_SNAPSHOT -> new GamePacket.InventorySnapshot(readItemStacks(in));
                case CRAFT_REQUEST -> new GamePacket.CraftRequest(in.readUTF());
                case CHAT -> new GamePacket.Chat(in.readUTF());
            };
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
        int length = checkedLength(in.readInt());
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
        int length = checkedLength(in.readInt());
        byte[] values = new byte[length];
        in.readFully(values);
        return values;
    }

    private static void writeItemStacks(DataOutputStream out, List<ItemStack> stacks) throws IOException {
        out.writeInt(stacks.size());
        for (ItemStack stack : stacks) {
            out.writeShort(stack.itemId());
            out.writeInt(stack.count());
            out.writeInt(stack.damage());
        }
    }

    private static List<ItemStack> readItemStacks(DataInputStream in) throws IOException {
        int length = checkedLength(in.readInt());
        List<ItemStack> stacks = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            stacks.add(new ItemStack(in.readShort(), in.readInt(), in.readInt()));
        }
        return stacks;
    }

    private static int checkedLength(int length) {
        if (length < 0 || length > MAX_ARRAY_LENGTH) {
            throw new IllegalArgumentException("Invalid array length: " + length);
        }
        return length;
    }
}
