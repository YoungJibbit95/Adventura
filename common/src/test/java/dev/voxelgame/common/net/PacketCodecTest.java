package dev.voxelgame.common.net;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.world.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketCodecTest {
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
        assertEquals(10, action.targetX());
        assertEquals(65, action.placeY());
        assertEquals(3, action.blockId());
    }

    @Test
    void roundTripsEntitySnapshots() {
        UUID playerId = UUID.randomUUID();
        EntitySnapshot snapshot = new EntitySnapshot(123L, "voxel:player", playerId, 1.0, 2.0, 3.0, 90.0f, -10.0f, 20);

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
    void roundTripsCraftRequest() {
        GamePacket.CraftRequest decoded = (GamePacket.CraftRequest) PacketCodec.decode(PacketCodec.encode(
                new GamePacket.CraftRequest("voxel:stone_pickaxe")
        ));

        assertEquals("voxel:stone_pickaxe", decoded.recipeKey());
    }
}
