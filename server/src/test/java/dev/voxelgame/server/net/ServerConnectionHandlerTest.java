package dev.voxelgame.server.net;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.server.auth.AuthResult;
import dev.voxelgame.server.entity.ServerEntityTracker;
import dev.voxelgame.server.world.ServerWorld;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConnectionHandlerTest {
    private static final UUID PLAYER_ID = UUID.fromString("21b6d875-f9b8-46c9-9a8f-8f8333d2821d");
    private static final UUID SECOND_PLAYER_ID = UUID.fromString("f8f95423-5126-4e18-b89e-7c1907e9687b");

    @Test
    void craftRequestCraftsRequestedCount() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L));
        try {
            channel.writeInbound(new GamePacket.CraftRequest("voxel:stone_pickaxe", 2, false, 0, 0, 0));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(3, count(slots, items, "voxel:stone_pickaxe"));
            assertEquals(0, count(slots, items, "voxel:pebble"));
            assertEquals(4, count(slots, items, "voxel:twig"));
            assertEquals(2, count(slots, items, "voxel:fiber"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void timedCampfireRecipeRejectsLegacyCraftRequest() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.CAMPFIRE_ACTIVE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(GamePacket.CraftRequest.atStation("voxel:charcoal", 2, 8, 120, 9));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(0, count(slots, items, "voxel:charcoal"));
            assertEquals(6, count(slots, items, "voxel:skyroot_log"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void campfireRecipeRejectsMissingStation() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.CAMPFIRE_ACTIVE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.CraftRequest("voxel:charcoal", 2, false, 0, 0, 0));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(0, count(slots, items, "voxel:charcoal"));
            assertEquals(6, count(slots, items, "voxel:skyroot_log"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void cookRequestConsumesInputAndProducesAfterCookTime() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.CAMPFIRE_ACTIVE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.CookRequest(8, 120, 9, "voxel:charcoal", List.of(2)));

            List<ItemStack> accepted = readLastInventory(channel);

            assertEquals(5, count(accepted, items, "voxel:skyroot_log"));
            assertEquals(0, count(accepted, items, "voxel:charcoal"));

            channel.pipeline().get(ServerConnectionHandler.class).tickCooking(Double.MAX_VALUE);
            List<ItemStack> completed = readLastInventory(channel);

            assertEquals(2, count(completed, items, "voxel:charcoal"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void cookRequestRejectsInactiveCampfire() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.CAMPFIRE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.CookRequest(8, 120, 9, "voxel:charcoal", List.of(2)));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(6, count(slots, items, "voxel:skyroot_log"));
            assertEquals(0, count(slots, items, "voxel:charcoal"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void blockActionRejectsLowTierToolForHighTierOre() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.IRON_ORE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.BlockAction(
                    GamePacket.BlockAction.Action.BREAK,
                    8,
                    8,
                    120,
                    9,
                    0,
                    0,
                    0,
                    Blocks.AIR
            ));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(0, count(slots, items, "voxel:raw_iron"));
            assertEquals(Blocks.IRON_ORE, world.blockAt(8, 120, 9).orElseThrow().id());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void blockInteractOpensStorageCrate() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.STORAGE_CRATE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.BlockInteract(0, 8, 120, 9));

            GamePacket.StorageOpen storage = readLastStorageOpen(channel);

            assertEquals(8, storage.x());
            assertEquals(120, storage.y());
            assertEquals(9, storage.z());
            assertEquals(ServerWorld.STORAGE_CRATE_SLOTS, storage.slots().size());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void storageOpenRequestOpensReachableCrate() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.STORAGE_CRATE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.StorageOpenRequest(8, 120, 9));

            GamePacket.StorageOpen storage = readLastStorageOpen(channel);

            assertEquals(8, storage.x());
            assertEquals(ServerWorld.STORAGE_CRATE_SLOTS, storage.slots().size());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void storageOpenRequestRejectsFarCrate() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(80, 120, 80, Blocks.STORAGE_CRATE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.StorageOpenRequest(80, 120, 80));

            StorageOpenResponse response = readStorageOpenResponse(channel);

            assertFalse(response.hasStorageOpen());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void sleepRequestAdvancesNightAtReachableMat() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.SLEEPING_MAT);
        world.setBlock(8, 123, 9, Blocks.SKYROOT_PLANKS);
        world.setDayTimeTicks(ServerWorld.NIGHT_START_TICK + 500L);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.SleepRequest(8, 120, 9));

            GamePacket.PlayerStatsSnapshot stats = readLastPlayerStats(channel);

            assertEquals(ServerWorld.MORNING_TICK, world.dayTimeTicks() % ServerWorld.DAY_LENGTH_TICKS);
            assertEquals(4, stats.comfort());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void sleepRequestRejectsDaytime() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.SLEEPING_MAT);
        world.setBlock(8, 123, 9, Blocks.SKYROOT_PLANKS);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.SleepRequest(8, 120, 9));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(0L, world.dayTimeTicks());
            assertFalse(slots.isEmpty());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void sleepRequestRejectsDangerNearby() {
        ServerWorld world = new ServerWorld(123L);
        ServerEntityTracker tracker = new ServerEntityTracker(123L);
        EntitySnapshot boar = tracker.snapshots().stream()
                .filter(snapshot -> "voxel:little_boar".equals(snapshot.typeKey()))
                .findFirst()
                .orElseThrow();
        int matX = (int) Math.floor(boar.x() + 1.0);
        int matY = (int) Math.floor(boar.y());
        int matZ = (int) Math.floor(boar.z());
        world.setBlock(matX, matY, matZ, Blocks.SLEEPING_MAT);
        world.setBlock(matX, matY + 3, matZ, Blocks.SKYROOT_PLANKS);
        world.setDayTimeTicks(ServerWorld.NIGHT_START_TICK + 500L);
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(matX + 0.5, matY, matZ + 0.5, 0.0f, 0.0f, true));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.SleepRequest(matX, matY, matZ));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(ServerWorld.NIGHT_START_TICK + 500L, world.dayTimeTicks());
            assertFalse(slots.isEmpty());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void sleepRequestWaitsForAllOnlinePlayersInSameWorld() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.SLEEPING_MAT);
        world.setBlock(8, 123, 9, Blocks.SKYROOT_PLANKS);
        world.setDayTimeTicks(ServerWorld.NIGHT_START_TICK + 500L);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel first = loggedInChannel(world, tracker, PLAYER_ID);
        EmbeddedChannel second = loggedInChannel(world, tracker, SECOND_PLAYER_ID);
        try {
            first.writeInbound(new GamePacket.SleepRequest(8, 120, 9));
            readLastPlayerStats(first);

            assertEquals(ServerWorld.NIGHT_START_TICK + 500L, world.dayTimeTicks());

            second.writeInbound(new GamePacket.SleepRequest(8, 120, 9));
            GamePacket.PlayerStatsSnapshot firstStats = readLastPlayerStats(first);
            GamePacket.PlayerStatsSnapshot secondStats = readLastPlayerStats(second);

            assertEquals(ServerWorld.MORNING_TICK, world.dayTimeTicks() % ServerWorld.DAY_LENGTH_TICKS);
            assertEquals(4, firstStats.comfort());
            assertEquals(4, secondStats.comfort());
        } finally {
            first.finishAndReleaseAll();
            second.finishAndReleaseAll();
        }
    }

    @Test
    void blockInteractHarvestsAndCooldownRejectsImmediateRepeat() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.BERRY_BUSH);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.BlockInteract(0, 8, 120, 9));
            List<ItemStack> first = readLastInventory(channel);

            channel.writeInbound(new GamePacket.BlockInteract(0, 8, 120, 9));
            List<ItemStack> second = readLastInventory(channel);

            assertEquals(2, count(first, items, "voxel:berries"));
            assertEquals(2, count(second, items, "voxel:berries"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void blockInteractRejectsFarTargets() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(80, 120, 80, Blocks.HERB_PLANTER);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.BlockInteract(0, 80, 120, 80));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(0, count(slots, items, "voxel:wild_herbs"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void blockInteractFuelsCampfireAndConsumesFuel() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.CAMPFIRE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.BlockInteract(4, 8, 120, 9));

            OutboundPackets packets = readOutboundPackets(channel);

            assertEquals(Blocks.CAMPFIRE_ACTIVE, packets.blockUpdate().blockId());
            assertEquals(5, count(packets.inventory().slots(), items, "voxel:twig"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void blockInteractHarvestsResinFromPineLog() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.PINE_LOG);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.BlockInteract(0, 8, 120, 9));
            List<ItemStack> first = readLastInventory(channel);

            channel.writeInbound(new GamePacket.BlockInteract(0, 8, 120, 9));
            List<ItemStack> second = readLastInventory(channel);

            assertEquals(1, count(first, items, "voxel:resin"));
            assertEquals(1, count(second, items, "voxel:resin"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void entityInteractAcceptsKnownReachableEntity() {
        ServerEntityTracker tracker = new ServerEntityTracker(123L);
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        EntitySnapshot target = tracker.snapshots().stream()
                .filter(snapshot -> snapshot.ownerPlayerId() == null)
                .findFirst()
                .orElseThrow();
        try {
            channel.writeInbound(new GamePacket.PlayerMove(target.x(), target.y(), target.z(), 0.0f, 0.0f, true));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.EntityInteract(target.entityId(), 0, GamePacket.EntityInteract.Action.OBSERVE));

            EntityInteractResponse response = readEntityInteractResponse(channel);
            assertTrue(response.hasEntitySnapshots());
            assertTrue(response.entitySnapshots().snapshots().stream().anyMatch(snapshot -> snapshot.entityId() == target.entityId()));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void entityInteractRejectsUnknownEntity() {
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L));
        try {
            channel.writeInbound(new GamePacket.EntityInteract(Long.MAX_VALUE, 0, GamePacket.EntityInteract.Action.OBSERVE));

            EntityInteractResponse response = readEntityInteractResponse(channel);
            assertFalse(response.hasEntitySnapshots());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void entityInteractFeedConsumesFoodAndUpdatesAmbientEntity() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerEntityTracker tracker = new ServerEntityTracker(123L);
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        EntitySnapshot target = tracker.snapshots().stream()
                .filter(snapshot -> snapshot.ownerPlayerId() == null)
                .findFirst()
                .orElseThrow();
        try {
            channel.writeInbound(new GamePacket.PlayerMove(target.x(), target.y(), target.z(), 0.0f, 0.0f, true));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.EntityInteract(target.entityId(), 7, GamePacket.EntityInteract.Action.FEED));

            EntityInteractResponse response = readEntityInteractResponse(channel);
            EntitySnapshot updated = response.entitySnapshots().snapshots().stream()
                    .filter(snapshot -> snapshot.entityId() == target.entityId())
                    .findFirst()
                    .orElseThrow();
            assertEquals(Math.min(20, target.health() + 2), updated.health());
            assertEquals(EntitySnapshot.STATE_FOLLOW, updated.stateKey());
            assertEquals(2, count(response.inventory().slots(), items, "voxel:apple"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void entityInteractFeedRejectsNonFoodItem() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerEntityTracker tracker = new ServerEntityTracker(123L);
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        EntitySnapshot target = tracker.snapshots().stream()
                .filter(snapshot -> snapshot.ownerPlayerId() == null)
                .findFirst()
                .orElseThrow();
        try {
            channel.writeInbound(new GamePacket.PlayerMove(target.x(), target.y(), target.z(), 0.0f, 0.0f, true));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.EntityInteract(target.entityId(), 0, GamePacket.EntityInteract.Action.FEED));

            EntityInteractResponse response = readEntityInteractResponse(channel);
            assertFalse(response.hasEntitySnapshots());
            assertEquals(16, count(response.inventory().slots(), items, "voxel:dirt"));
            assertEquals(target.health(), tracker.snapshot(target.entityId()).orElseThrow().health());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void loginSendsServerComputedComfortSnapshot() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.CAMPFIRE_ACTIVE);
        EmbeddedChannel channel = new EmbeddedChannel(new ServerConnectionHandler(
                world,
                (username, authToken) -> AuthResult.accepted(PLAYER_ID),
                new ServerEntityTracker()
        ));
        try {
            channel.writeInbound(new GamePacket.LoginRequest("Tester", "dev-token"));

            GamePacket.PlayerStatsSnapshot stats = readLastPlayerStats(channel);

            assertEquals(5, stats.comfort());
            assertEquals(20, stats.health());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveRejectsExtremeTeleportAfterAcceptedMove() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.75, 120.0, 8.75, 0.0f, 0.0f, true));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.PlayerMove(1000.0, 120.0, 1000.0, 0.0f, 0.0f, true));
            drainOutbound(channel);

            EntitySnapshot player = playerSnapshot(tracker);
            assertEquals(8.75, player.x(), 0.001);
            assertEquals(120.0, player.y(), 0.001);
            assertEquals(8.75, player.z(), 0.001);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void ignoresClientForgedPlayerStatsSnapshot() {
        ServerWorld world = new ServerWorld(123L);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.PlayerStatsSnapshot(20, 20, 20, 20, 0, 40));
            channel.writeInbound(new GamePacket.PlayerMove(8.6, 120.0, 8.5, 0.0f, 0.0f, true));

            GamePacket.PlayerStatsSnapshot stats = readLastPlayerStats(channel);

            assertEquals(0, stats.comfort());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    private static EmbeddedChannel loggedInChannel(ServerWorld world) {
        return loggedInChannel(world, new ServerEntityTracker());
    }

    private static EmbeddedChannel loggedInChannel(ServerWorld world, ServerEntityTracker tracker) {
        return loggedInChannel(world, tracker, PLAYER_ID);
    }

    private static EmbeddedChannel loggedInChannel(ServerWorld world, ServerEntityTracker tracker, UUID playerId) {
        EmbeddedChannel channel = new EmbeddedChannel(new ServerConnectionHandler(
                world,
                (username, authToken) -> AuthResult.accepted(playerId),
                tracker
        ));
        channel.writeInbound(new GamePacket.LoginRequest("Tester", "dev-token"));
        drainOutbound(channel);
        return channel;
    }

    private static List<ItemStack> readLastInventory(EmbeddedChannel channel) {
        GamePacket.InventorySnapshot snapshot = null;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof GamePacket.InventorySnapshot inventory) {
                snapshot = inventory;
            }
        }
        if (snapshot == null) {
            throw new AssertionError("Expected inventory snapshot");
        }
        return snapshot.slots();
    }

    private static GamePacket.StorageOpen readLastStorageOpen(EmbeddedChannel channel) {
        GamePacket.StorageOpen storage = null;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof GamePacket.StorageOpen open) {
                storage = open;
            }
        }
        if (storage == null) {
            throw new AssertionError("Expected storage open packet");
        }
        return storage;
    }

    private static StorageOpenResponse readStorageOpenResponse(EmbeddedChannel channel) {
        GamePacket.StorageOpen storage = null;
        GamePacket.InventorySnapshot inventory = null;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof GamePacket.StorageOpen open) {
                storage = open;
            } else if (outbound instanceof GamePacket.InventorySnapshot snapshot) {
                inventory = snapshot;
            }
        }
        if (storage == null && inventory == null) {
            throw new AssertionError("Expected storage open or inventory snapshot");
        }
        return new StorageOpenResponse(storage, inventory);
    }

    private static OutboundPackets readOutboundPackets(EmbeddedChannel channel) {
        GamePacket.InventorySnapshot inventory = null;
        GamePacket.BlockUpdate blockUpdate = null;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof GamePacket.InventorySnapshot snapshot) {
                inventory = snapshot;
            } else if (outbound instanceof GamePacket.BlockUpdate update) {
                blockUpdate = update;
            }
        }
        if (inventory == null || blockUpdate == null) {
            throw new AssertionError("Expected block update and inventory snapshot");
        }
        return new OutboundPackets(inventory, blockUpdate);
    }

    private static EntityInteractResponse readEntityInteractResponse(EmbeddedChannel channel) {
        GamePacket.InventorySnapshot inventory = null;
        GamePacket.EntitySnapshots entitySnapshots = null;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof GamePacket.InventorySnapshot snapshot) {
                inventory = snapshot;
            } else if (outbound instanceof GamePacket.EntitySnapshots snapshots) {
                entitySnapshots = snapshots;
            }
        }
        if (inventory == null) {
            throw new AssertionError("Expected inventory snapshot");
        }
        return new EntityInteractResponse(inventory, entitySnapshots);
    }

    private static GamePacket.PlayerStatsSnapshot readLastPlayerStats(EmbeddedChannel channel) {
        GamePacket.PlayerStatsSnapshot stats = null;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof GamePacket.PlayerStatsSnapshot snapshot) {
                stats = snapshot;
            }
        }
        if (stats == null) {
            throw new AssertionError("Expected player stats snapshot");
        }
        return stats;
    }

    private static void drainOutbound(EmbeddedChannel channel) {
        while (channel.readOutbound() != null) {
        }
    }

    private static EntitySnapshot playerSnapshot(ServerEntityTracker tracker) {
        return tracker.snapshots().stream()
                .filter(snapshot -> PLAYER_ID.equals(snapshot.ownerPlayerId()))
                .findFirst()
                .orElseThrow();
    }

    private static int count(List<ItemStack> slots, Registry<ItemType> items, String key) {
        short itemId = items.requireByKey(key).id();
        int total = 0;
        for (ItemStack slot : slots) {
            if (slot.itemId() == itemId) {
                total += slot.count();
            }
        }
        return total;
    }

    private record OutboundPackets(GamePacket.InventorySnapshot inventory, GamePacket.BlockUpdate blockUpdate) {
    }

    private record StorageOpenResponse(GamePacket.StorageOpen storageOpen, GamePacket.InventorySnapshot inventory) {
        boolean hasStorageOpen() {
            return storageOpen != null;
        }
    }

    private record EntityInteractResponse(GamePacket.InventorySnapshot inventory, GamePacket.EntitySnapshots entitySnapshots) {
        boolean hasEntitySnapshots() {
            return entitySnapshots != null;
        }
    }
}
