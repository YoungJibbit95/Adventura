package dev.voxelgame.common.net;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.world.ChunkPos;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketCodecTest {
    @Test
    void roundTripsHandshake() {
        GamePacket.Handshake decoded = (GamePacket.Handshake) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.Handshake(GamePacket.PROTOCOL_VERSION, "voxel-client")
        ));

        assertEquals(GamePacket.PROTOCOL_VERSION, decoded.protocolVersion());
        assertEquals("voxel-client", decoded.clientName());
    }

    @Test
    void roundTripsLoginRequest() {
        GamePacket.LoginRequest decoded = (GamePacket.LoginRequest) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.LoginRequest("Player", "dev-token")
        ));

        assertEquals("Player", decoded.username());
        assertEquals("dev-token", decoded.authToken());
    }

    @Test
    void roundTripsLoginAccepted() {
        UUID playerId = UUID.randomUUID();
        GamePacket decoded = PacketCodec.decode(PacketCodec.encode(new GamePacket.LoginAccepted(playerId, 99L, -64, 320)));

        GamePacket.LoginAccepted accepted = (GamePacket.LoginAccepted) decoded;
        assertEquals(playerId, accepted.playerId());
        assertEquals(99L, accepted.worldSeed());
        assertEquals(-64, accepted.minY());
        assertEquals(320, accepted.maxYExclusive());
    }

    @Test
    void roundTripsLoginRejected() {
        GamePacket.LoginRejected decoded = (GamePacket.LoginRejected) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.LoginRejected("Nope")
        ));

        assertEquals("Nope", decoded.reason());
    }

    @Test
    void roundTripsChunkDataDefensively() {
        short[] blocks = {1, 2, 3};
        byte[] sky = {15, 14, 13};
        byte[] block = {0, 1, 2};
        GamePacket.ChunkData packet = new GamePacket.ChunkData(new ChunkPos(2, -1), -64, blocks, sky, block);

        blocks[0] = 99;
        GamePacket.ChunkData decoded = (GamePacket.ChunkData) PacketCodec.decode(PacketCodec.encode(packet));

        assertEquals(new ChunkPos(2, -1), decoded.pos());
        assertArrayEquals(new short[]{1, 2, 3}, decoded.blockIds());
        assertArrayEquals(new byte[]{15, 14, 13}, decoded.skyLight());
        assertArrayEquals(new byte[]{0, 1, 2}, decoded.blockLight());
    }

    @Test
    void roundTripsBlockAction() {
        GamePacket decoded = PacketCodec.decode(PacketCodec.encode(new GamePacket.BlockAction(
                GamePacket.BlockAction.Action.PLACE,
                2,
                10,
                64,
                10,
                10,
                65,
                10,
                (short) 3
        )));

        GamePacket.BlockAction action = (GamePacket.BlockAction) decoded;
        assertEquals(GamePacket.BlockAction.Action.PLACE, action.action());
        assertEquals(2, action.selectedSlot());
        assertEquals(10, action.targetX());
        assertEquals(65, action.placeY());
        assertEquals(3, action.blockId());
    }

    @Test
    void roundTripsBlockUpdate() {
        GamePacket.BlockUpdate decoded = (GamePacket.BlockUpdate) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.BlockUpdate(4, 5, -6, (short) 7)
        ));

        assertEquals(4, decoded.x());
        assertEquals(5, decoded.y());
        assertEquals(-6, decoded.z());
        assertEquals(7, decoded.blockId());
    }

    @Test
    void roundTripsPlayerMove() {
        GamePacket.PlayerMove decoded = (GamePacket.PlayerMove) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.PlayerMove(1.5, 80.25, -3.75, 45.0f, -12.5f, true)
        ));

        assertEquals(1.5, decoded.x());
        assertEquals(80.25, decoded.y());
        assertEquals(-3.75, decoded.z());
        assertEquals(45.0f, decoded.yaw());
        assertEquals(-12.5f, decoded.pitch());
        assertTrue(decoded.onGround());
    }

    @Test
    void roundTripsBlockInteract() {
        GamePacket.BlockInteract decoded = (GamePacket.BlockInteract) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.BlockInteract(4, 12, 70, -3)
        ));

        assertEquals(4, decoded.selectedSlot());
        assertEquals(12, decoded.targetX());
        assertEquals(70, decoded.targetY());
        assertEquals(-3, decoded.targetZ());
    }

    @Test
    void roundTripsEntitySnapshots() {
        UUID playerId = UUID.randomUUID();
        EntitySnapshot snapshot = new EntitySnapshot(123L, "voxel:player", playerId, 1.0, 2.0, 3.0, 90.0f, -10.0f, 20, EntitySnapshot.STATE_WANDER);

        GamePacket.EntitySnapshots decoded = (GamePacket.EntitySnapshots) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.EntitySnapshots(List.of(snapshot))
        ));

        assertEquals(1, decoded.snapshots().size());
        EntitySnapshot actual = decoded.snapshots().getFirst();
        assertEquals(123L, actual.entityId());
        assertEquals("voxel:player", actual.typeKey());
        assertEquals(playerId, actual.ownerPlayerId());
        assertEquals(3.0, actual.z());
        assertEquals(90.0f, actual.yaw());
        assertEquals(EntitySnapshot.STATE_WANDER, actual.stateKey());
    }

    @Test
    void roundTripsEntityInteract() {
        GamePacket.EntityInteract decoded = (GamePacket.EntityInteract) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.EntityInteract(123L, 4, GamePacket.EntityInteract.Action.FEED)
        ));

        assertEquals(123L, decoded.entityId());
        assertEquals(4, decoded.selectedSlot());
        assertEquals(GamePacket.EntityInteract.Action.FEED, decoded.action());
    }

    @Test
    void roundTripsInventorySnapshot() {
        GamePacket.InventorySnapshot decoded = (GamePacket.InventorySnapshot) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.InventorySnapshot(List.of(new ItemStack((short) 2, 16), ItemStack.EMPTY))
        ));

        assertEquals(2, decoded.slots().size());
        assertEquals(new ItemStack((short) 2, 16), decoded.slots().getFirst());
        assertEquals(ItemStack.EMPTY, decoded.slots().get(1));
    }

    @Test
    void roundTripsPlayerStatsSnapshot() {
        GamePacket.PlayerStatsSnapshot decoded = (GamePacket.PlayerStatsSnapshot) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.PlayerStatsSnapshot(18, 17, 16, 15, 2, 9)
        ));

        assertEquals(18, decoded.health());
        assertEquals(17, decoded.hunger());
        assertEquals(16, decoded.stamina());
        assertEquals(15, decoded.breath());
        assertEquals(2, decoded.armor());
        assertEquals(9, decoded.comfort());
    }

    @Test
    void roundTripsStorageOpenRequest() {
        GamePacket.StorageOpenRequest decoded = (GamePacket.StorageOpenRequest) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.StorageOpenRequest(-3, 72, 8)
        ));

        assertEquals(-3, decoded.x());
        assertEquals(72, decoded.y());
        assertEquals(8, decoded.z());
    }

    @Test
    void roundTripsSleepRequest() {
        GamePacket.SleepRequest decoded = (GamePacket.SleepRequest) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.SleepRequest(4, 80, -9)
        ));

        assertEquals(4, decoded.x());
        assertEquals(80, decoded.y());
        assertEquals(-9, decoded.z());
    }

    @Test
    void roundTripsCookRequest() {
        GamePacket.CookRequest decoded = (GamePacket.CookRequest) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.CookRequest(4, 80, -9, "voxel:cooked_berries", List.of(3, 5))
        ));

        assertEquals(4, decoded.stationX());
        assertEquals(80, decoded.stationY());
        assertEquals(-9, decoded.stationZ());
        assertEquals("voxel:cooked_berries", decoded.recipeKey());
        assertEquals(List.of(3, 5), decoded.inputSlots());
    }

    @Test
    void roundTripsStorageOpen() {
        GamePacket.StorageOpen decoded = (GamePacket.StorageOpen) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.StorageOpen(3, 81, -4, List.of(new ItemStack((short) 55, 1), ItemStack.EMPTY))
        ));

        assertEquals(3, decoded.x());
        assertEquals(81, decoded.y());
        assertEquals(-4, decoded.z());
        assertEquals(new ItemStack((short) 55, 1), decoded.slots().getFirst());
        assertEquals(ItemStack.EMPTY, decoded.slots().get(1));
    }

    @Test
    void roundTripsStorageTransfer() {
        GamePacket.StorageTransfer decoded = (GamePacket.StorageTransfer) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.StorageTransfer(-2, 72, 8, true, 6, 3, 5, 99)
        ));

        assertEquals(-2, decoded.x());
        assertEquals(72, decoded.y());
        assertEquals(8, decoded.z());
        assertTrue(decoded.fromStorage());
        assertEquals(6, decoded.sourceSlot());
        assertEquals(6, decoded.slot());
        assertEquals(3, decoded.targetSlot());
        assertEquals(5, decoded.count());
        assertEquals(99, decoded.transactionId());
    }

    @Test
    void roundTripsCraftRequest() {
        GamePacket.CraftRequest decoded = (GamePacket.CraftRequest) PacketCodec.decode(PacketCodec.encode(
                GamePacket.CraftRequest.atStation("voxel:stone_pickaxe", 3, 10, 80, -4)
        ));

        assertEquals("voxel:stone_pickaxe", decoded.recipeKey());
        assertEquals(3, decoded.count());
        assertTrue(decoded.hasStation());
        assertEquals(10, decoded.stationX());
        assertEquals(80, decoded.stationY());
        assertEquals(-4, decoded.stationZ());
    }

    @Test
    void roundTripsChat() {
        GamePacket.Chat decoded = (GamePacket.Chat) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.Chat("Hello server")
        ));

        assertEquals("Hello server", decoded.message());
    }

    @Test
    void rejectsInventorySnapshotWithNegativeItemCount() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(PacketType.INVENTORY_SNAPSHOT.id());
        out.writeInt(1);
        out.writeShort(2);
        out.writeInt(-5);
        out.writeInt(0);
        out.flush();

        assertThrows(IllegalArgumentException.class, () -> PacketCodec.decode(bytes.toByteArray()));
    }
}
