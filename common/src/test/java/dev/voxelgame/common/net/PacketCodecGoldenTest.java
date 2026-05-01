package dev.voxelgame.common.net;

import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.gameplay.GameplayEvent;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.physics.ProjectileHit;
import dev.voxelgame.common.world.ChunkPos;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketCodecGoldenTest {
    private static final UUID PLAYER_ID = UUID.fromString("01234567-89ab-cdef-0fed-cba987654321");

    @TestFactory
    Stream<DynamicTest> goldenCodecFixtures() {
        return fixtures().stream().map(fixture -> DynamicTest.dynamicTest(fixture.type().name(), () -> {
            byte[] expected = bytes(fixture.hex());
            byte[] encoded = PacketCodec.encode(fixture.packet());
            assertArrayEquals(expected, encoded);

            GamePacket decoded = PacketCodec.decode(expected);
            assertEquals(fixture.type(), decoded.type());
            assertArrayEquals(expected, PacketCodec.encode(decoded));
            assertTrue(encoded.length <= ProtocolContract.require(fixture.type()).maxPayloadBytes());
        }));
    }

    private static List<Fixture> fixtures() {
        return List.of(
                new Fixture(
                        PacketType.HANDSHAKE,
                        new GamePacket.Handshake(GamePacket.PROTOCOL_VERSION, "voxel-client"),
                        "0000000100000018000c766f78656c2d636c69656e74"
                ),
                new Fixture(
                        PacketType.LOGIN_REQUEST,
                        new GamePacket.LoginRequest("Player", "dev-token"),
                        "000000020006506c6179657200096465762d746f6b656e"
                ),
                new Fixture(
                        PacketType.LOGIN_ACCEPTED,
                        new GamePacket.LoginAccepted(PLAYER_ID, 99L, -64, 320),
                        "000000030123456789abcdef0fedcba9876543210000000000000063ffffffc000000140"
                ),
                new Fixture(
                        PacketType.LOGIN_REJECTED,
                        new GamePacket.LoginRejected("Protocol mismatch: server uses 24"),
                        "00000004002150726f746f636f6c206d69736d617463683a207365727665722075736573203234"
                ),
                new Fixture(
                        PacketType.CHUNK_DATA,
                        new GamePacket.ChunkData(new ChunkPos(2, -1), -64, new short[]{1, 2, 3}, new byte[]{15, 14, 13}, new byte[]{0, 1, 2}),
                        "0000000500000002ffffffffffffffc000000003000100020003000000030f0e0d00000003000102"
                ),
                new Fixture(
                        PacketType.BLOCK_UPDATE,
                        new GamePacket.BlockUpdate(4, 5, -6, (short) 7),
                        "000000060000000400000005fffffffa0007"
                ),
                new Fixture(
                        PacketType.BLOCK_ACTION,
                        new GamePacket.BlockAction(GamePacket.BlockAction.Action.PLACE, 2, 10, 64, 10, 10, 65, 10, (short) 3),
                        "000000070005504c414345000000020000000a000000400000000a0000000a000000410000000a0003"
                ),
                new Fixture(
                        PacketType.PLAYER_MOVE,
                        new GamePacket.PlayerMove(42L, 1.5, 80.25, -3.75, 45.0f, -12.5f, true, true, false, true),
                        "00000008000000000000002a3ff80000000000004054100000000000c00e00000000000042340000c148000001010001"
                ),
                new Fixture(
                        PacketType.ENTITY_SNAPSHOT,
                        new GamePacket.EntitySnapshots(List.of(
                                new EntitySnapshot(
                                        123L,
                                        "voxel:player",
                                        PLAYER_ID,
                                        1.0,
                                        2.0,
                                        3.0,
                                        90.0f,
                                        -10.0f,
                                        20,
                                        EntitySnapshot.STATE_WANDER
                                ).withVelocity(0.2, -0.1, 0.4),
                                new EntitySnapshot(
                                        456L,
                                        "voxel:bunny",
                                        null,
                                        4.0,
                                        5.0,
                                        6.0,
                                        15.0f,
                                        0.0f,
                                        8,
                                        EntitySnapshot.STATE_FLEE
                                )
                        )),
                        "0000000900000002000000000000007b000c766f78656c3a706c61796572010123456789abcdef0fedcba9876543213ff00000000000004000000000000000400800000000000042b40000c120000000000014000657414e4445523fc999999999999abfb999999999999a3fd999999999999a00000000000001c8000b766f78656c3a62756e6e79004010000000000000401400000000000040180000000000004170000000000000000000080004464c4545000000000000000000000000000000000000000000000000"
                ),
                new Fixture(
                        PacketType.CHAT,
                        new GamePacket.Chat("Hello server"),
                        "0000000a000c48656c6c6f20736572766572"
                ),
                new Fixture(
                        PacketType.INVENTORY_SNAPSHOT,
                        new GamePacket.InventorySnapshot(List.of(new ItemStack((short) 2, 16), ItemStack.EMPTY)),
                        "0000000b000000020002000000100000000000000000000000000000"
                ),
                new Fixture(
                        PacketType.CRAFT_REQUEST,
                        GamePacket.CraftRequest.atStation("voxel:stone_pickaxe", 3, 10, 80, -4, 19),
                        "0000000c0013766f78656c3a73746f6e655f7069636b61786500000003010000000a00000050fffffffc00000013"
                ),
                new Fixture(
                        PacketType.BLOCK_INTERACT,
                        new GamePacket.BlockInteract(4, 12, 70, -3),
                        "0000000d000000040000000c00000046fffffffd"
                ),
                new Fixture(
                        PacketType.STORAGE_OPEN,
                        new GamePacket.StorageOpen(3, 81, -4, List.of(new ItemStack((short) 55, 1), ItemStack.EMPTY)),
                        "0000000e0000000300000051fffffffc000000020037000000010000000000000000000000000000"
                ),
                new Fixture(
                        PacketType.STORAGE_TRANSFER,
                        new GamePacket.StorageTransfer(-2, 72, 8, true, 6, 3, 5, 99),
                        "0000000ffffffffe00000048000000080100000006000000030000000500000063"
                ),
                new Fixture(
                        PacketType.ENTITY_INTERACT,
                        new GamePacket.EntityInteract(123L, 4, GamePacket.EntityInteract.Action.ATTACK),
                        "00000010000000000000007b00000004000641545441434b"
                ),
                new Fixture(
                        PacketType.PLAYER_STATS_SNAPSHOT,
                        new GamePacket.PlayerStatsSnapshot(18, 17, 16, 15, 2, 9),
                        "000000110000001200000011000000100000000f0000000200000009"
                ),
                new Fixture(
                        PacketType.STORAGE_OPEN_REQUEST,
                        new GamePacket.StorageOpenRequest(-3, 72, 8, 17),
                        "00000012fffffffd000000480000000800000011"
                ),
                new Fixture(
                        PacketType.SLEEP_REQUEST,
                        new GamePacket.SleepRequest(4, 80, -9),
                        "000000130000000400000050fffffff7"
                ),
                new Fixture(
                        PacketType.COOK_REQUEST,
                        new GamePacket.CookRequest(4, 80, -9, "voxel:cooked_berries", List.of(3, 5), 18),
                        "000000140000000400000050fffffff70014766f78656c3a636f6f6b65645f6265727269657300000002000000030000000500000012"
                ),
                new Fixture(
                        PacketType.CAMPFIRE_STATUS,
                        new GamePacket.CampfireStatus(4, 80, -9, true, 42.5, "voxel:cooked_berries", 3.0, 1.5),
                        "000000150000000400000050fffffff70140454000000000000014766f78656c3a636f6f6b65645f6265727269657340080000000000003ff8000000000000"
                ),
                new Fixture(
                        PacketType.PLAYER_POSITION_SNAPSHOT,
                        new GamePacket.PlayerPositionSnapshot(99L, 8.5, 120.0, -4.5, 12.0f, -3.0f, true, true, true, false, GamePacket.MovementCorrection.HARD),
                        "0000001600000000000000634021000000000000405e000000000000c01200000000000041400000c040000001010100000448415244"
                ),
                new Fixture(
                        PacketType.SERVER_STATS_SNAPSHOT,
                        new GamePacket.ServerStatsSnapshot(9, 3L, 42L, 18L, 7L, 2L, 1L, 4L, 64L, 8192L, 128L, 12.5),
                        "00000017000000090000000000000003000000000000002a000000000000001200000000000000070000000000000002000000000000000100000000000000040000000000000040000000000000200000000000000000804029000000000000"
                ),
                new Fixture(
                        PacketType.PROJECTILE_SHOOT,
                        new GamePacket.ProjectileShoot(8, 77L),
                        "0000001800000008000000000000004d"
                ),
                new Fixture(
                        PacketType.PROJECTILE_IMPACT,
                        new GamePacket.ProjectileImpact(
                                -12L,
                                "voxel:arrow_projectile",
                                ProjectileHit.Type.BLOCK,
                                1.25,
                                80.5,
                                -3.75,
                                1,
                                80,
                                -4,
                                ProjectileHit.BlockFace.WEST,
                                0L,
                                true,
                                1234L
                        ),
                        "00000019fffffffffffffff40016766f78656c3a6172726f775f70726f6a656374696c650005424c4f434b3ff40000000000004054200000000000c00e0000000000000000000100000050fffffffc00045745535400000000000000000100000000000004d2"
                ),
                new Fixture(
                        PacketType.STORAGE_CLOSE,
                        new GamePacket.StorageClose(3, 81, -4),
                        "0000001a0000000300000051fffffffc"
                ),
                new Fixture(
                        PacketType.GAMEPLAY_EVENTS,
                        new GamePacket.GameplayEvents(List.of(
                                new GameplayEvent.Pickup(4L, "voxel:moss_clump", 5),
                                new GameplayEvent.RecipeUnlocked(11L, PLAYER_ID, "voxel:stone_pickaxe")
                        )),
                        "0000001b000000010000000200065049434b555000000000000000040010766f78656c3a6d6f73735f636c756d7000000005000f5245434950455f554e4c4f434b4544000000000000000b0123456789abcdef0fedcba9876543210013766f78656c3a73746f6e655f7069636b617865"
                )
        );
    }

    private static byte[] bytes(String hex) {
        if ((hex.length() & 1) != 0) {
            throw new IllegalArgumentException("Hex fixture length must be even");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int offset = i * 2;
            bytes[i] = (byte) Integer.parseInt(hex.substring(offset, offset + 2), 16);
        }
        return bytes;
    }

    private record Fixture(PacketType type, GamePacket packet, String hex) {
    }
}
