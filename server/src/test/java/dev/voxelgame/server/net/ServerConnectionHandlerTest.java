package dev.voxelgame.server.net;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.entity.ItemDropType;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.physics.PlayerWaterState;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.server.auth.AuthResult;
import dev.voxelgame.server.entity.ServerEntityTracker;
import dev.voxelgame.server.save.PlayerSave;
import dev.voxelgame.server.save.PlayerSaveStore;
import dev.voxelgame.server.save.SaveMetadata;
import dev.voxelgame.server.world.ServerWorld;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConnectionHandlerTest {
    private static final int TEST_STREAM_RADIUS_CHUNKS = 0;
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
    void craftRequestAcceptsLegacyRecipeAlias() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L));
        try {
            channel.writeInbound(new GamePacket.CraftRequest("voxel:planks"));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(4, count(slots, items, "voxel:skyroot_planks"));
            assertEquals(5, count(slots, items, "voxel:skyroot_log"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void duplicateCraftTransactionIsIgnored() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L));
        try {
            GamePacket.CraftRequest request = new GamePacket.CraftRequest("voxel:planks", 1, false, 0, 0, 0, 1);

            channel.writeInbound(request);
            readLastInventory(channel);
            channel.writeInbound(request);
            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(4, count(slots, items, "voxel:skyroot_planks"));
            assertEquals(5, count(slots, items, "voxel:skyroot_log"));
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

            CampfireResponse acceptedResponse = readCampfireResponse(channel);
            List<ItemStack> accepted = acceptedResponse.inventory().slots();

            assertEquals(5, count(accepted, items, "voxel:skyroot_log"));
            assertEquals(0, count(accepted, items, "voxel:charcoal"));
            assertTrue(acceptedResponse.campfireStatus().cooking());
            assertEquals("voxel:charcoal", acceptedResponse.campfireStatus().cookingRecipeKey());
            assertEquals(5.0, acceptedResponse.campfireStatus().cookTotalSeconds(), 0.001);

            channel.pipeline().get(ServerConnectionHandler.class).tickCooking(Double.MAX_VALUE);
            CampfireResponse completedResponse = readCampfireResponse(channel);
            List<ItemStack> completed = completedResponse.inventory().slots();

            assertEquals(2, count(completed, items, "voxel:charcoal"));
            assertFalse(completedResponse.campfireStatus().cooking());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void cookRequestUsesCookingPotForSoupProgression() throws Exception {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.COOKING_POT);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            Inventory inventory = inventory(channel);
            inventory.clear();
            inventory.add(items.requireByKey("voxel:wild_herbs").id(), 2, items);
            inventory.add(items.requireByKey("voxel:water_container").id(), 1, items);
            inventory.add(items.requireByKey("voxel:clay_bowl").id(), 1, items);

            channel.writeInbound(new GamePacket.CookRequest(8, 120, 9, "voxel:herb_soup", List.of(0, 1, 2)));

            List<ItemStack> accepted = readLastInventory(channel);
            assertEquals(0, count(accepted, items, "voxel:wild_herbs"));
            assertEquals(0, count(accepted, items, "voxel:water_container"));
            assertEquals(0, count(accepted, items, "voxel:clay_bowl"));
            assertEquals(0, count(accepted, items, "voxel:herb_soup"));

            channel.pipeline().get(ServerConnectionHandler.class).tickCooking(Double.MAX_VALUE);
            List<ItemStack> completed = readLastInventory(channel);

            assertEquals(1, count(completed, items, "voxel:herb_soup"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void cookRequestUsesForgeForIronProgression() throws Exception {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.FORGE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            Inventory inventory = inventory(channel);
            inventory.clear();
            inventory.add(items.requireByKey("voxel:raw_iron").id(), 2, items);
            inventory.add(items.requireByKey("voxel:charcoal").id(), 2, items);

            channel.writeInbound(new GamePacket.CookRequest(8, 120, 9, "voxel:iron_ingot", List.of(0, 1)));

            List<ItemStack> accepted = readLastInventory(channel);
            assertEquals(0, count(accepted, items, "voxel:raw_iron"));
            assertEquals(0, count(accepted, items, "voxel:charcoal"));
            assertEquals(0, count(accepted, items, "voxel:iron_ingot"));

            channel.pipeline().get(ServerConnectionHandler.class).tickCooking(Double.MAX_VALUE);
            List<ItemStack> completed = readLastInventory(channel);

            assertEquals(1, count(completed, items, "voxel:iron_ingot"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void cookRequestRejectsFarCookingPot() throws Exception {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(80, 120, 80, Blocks.COOKING_POT);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            Inventory inventory = inventory(channel);
            inventory.clear();
            inventory.add(items.requireByKey("voxel:wild_herbs").id(), 2, items);
            inventory.add(items.requireByKey("voxel:water_container").id(), 1, items);
            inventory.add(items.requireByKey("voxel:clay_bowl").id(), 1, items);

            channel.writeInbound(new GamePacket.CookRequest(80, 120, 80, "voxel:herb_soup", List.of(0, 1, 2)));

            List<ItemStack> slots = readLastInventory(channel);
            assertEquals(2, count(slots, items, "voxel:wild_herbs"));
            assertEquals(1, count(slots, items, "voxel:water_container"));
            assertEquals(1, count(slots, items, "voxel:clay_bowl"));
            assertEquals(0, count(slots, items, "voxel:herb_soup"));
            assertNull(pendingCook(channel));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void cookRequestRejectsFarForge() throws Exception {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(80, 120, 80, Blocks.FORGE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            Inventory inventory = inventory(channel);
            inventory.clear();
            inventory.add(items.requireByKey("voxel:raw_iron").id(), 2, items);
            inventory.add(items.requireByKey("voxel:charcoal").id(), 2, items);

            channel.writeInbound(new GamePacket.CookRequest(80, 120, 80, "voxel:iron_ingot", List.of(0, 1)));

            List<ItemStack> slots = readLastInventory(channel);
            assertEquals(2, count(slots, items, "voxel:raw_iron"));
            assertEquals(2, count(slots, items, "voxel:charcoal"));
            assertEquals(0, count(slots, items, "voxel:iron_ingot"));
            assertNull(pendingCook(channel));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void cookingJobClearsWhenCookingPotIsRemovedBeforeCompletion() throws Exception {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.COOKING_POT);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            Inventory inventory = inventory(channel);
            inventory.clear();
            inventory.add(items.requireByKey("voxel:wild_herbs").id(), 2, items);
            inventory.add(items.requireByKey("voxel:water_container").id(), 1, items);
            inventory.add(items.requireByKey("voxel:clay_bowl").id(), 1, items);

            channel.writeInbound(new GamePacket.CookRequest(8, 120, 9, "voxel:herb_soup", List.of(0, 1, 2)));
            readLastInventory(channel);

            world.setBlock(8, 120, 9, Blocks.AIR);
            channel.pipeline().get(ServerConnectionHandler.class).tickCooking(Double.MAX_VALUE);
            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(0, count(slots, items, "voxel:herb_soup"));
            assertNull(pendingCook(channel));
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
    void cookingJobClearsWhenCampfireTurnsInactiveBeforeCompletion() throws Exception {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.CAMPFIRE_ACTIVE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.CookRequest(8, 120, 9, "voxel:charcoal", List.of(2)));
            readLastInventory(channel);

            world.setBlock(8, 120, 9, Blocks.CAMPFIRE_BURNED_OUT);
            channel.pipeline().get(ServerConnectionHandler.class).tickCooking(Double.MAX_VALUE);
            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(0, count(slots, items, "voxel:charcoal"));
            assertNull(pendingCook(channel));
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
    void blockPlaceRejectsPlacementInsidePlayerBounds() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 119, 8, Blocks.STONE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.BlockAction(
                    GamePacket.BlockAction.Action.PLACE,
                    0,
                    8,
                    119,
                    8,
                    8,
                    120,
                    8,
                    Blocks.DIRT
            ));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(Blocks.AIR, world.blockAt(8, 120, 8).orElseThrow().id());
            assertEquals(16, count(slots, items, "voxel:dirt"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void blockPlaceRejectsPlacementInsideOtherPlayerBounds() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 119, 9, Blocks.STONE);
        world.setBlock(8, 120, 9, Blocks.AIR);
        ServerEntityTracker tracker = new ServerEntityTracker();
        tracker.registerPlayer(SECOND_PLAYER_ID);
        tracker.updatePlayer(SECOND_PLAYER_ID, 8.5, 121.62, 9.5, 0.0f, 0.0f);
        EmbeddedChannel channel = loggedInChannel(world, tracker, PLAYER_ID);
        try {
            channel.writeInbound(new GamePacket.BlockAction(
                    GamePacket.BlockAction.Action.PLACE,
                    0,
                    8,
                    119,
                    9,
                    8,
                    120,
                    9,
                    Blocks.DIRT
            ));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(Blocks.AIR, world.blockAt(8, 120, 9).orElseThrow().id());
            assertEquals(16, count(slots, items, "voxel:dirt"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void blockPlaceRejectsPlacementInsideAmbientEntityBounds() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 119, 9, Blocks.STONE);
        world.setBlock(8, 120, 9, Blocks.AIR);
        ServerEntityTracker tracker = new ServerEntityTracker();
        tracker.addAmbient(new EntitySnapshot(
                9002L,
                "voxel:cozy_sheep",
                null,
                8.5,
                120.0,
                9.5,
                0.0f,
                0.0f,
                10
        ));
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        try {
            channel.writeInbound(new GamePacket.BlockAction(
                    GamePacket.BlockAction.Action.PLACE,
                    0,
                    8,
                    119,
                    9,
                    8,
                    120,
                    9,
                    Blocks.DIRT
            ));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(Blocks.AIR, world.blockAt(8, 120, 9).orElseThrow().id());
            assertEquals(16, count(slots, items, "voxel:dirt"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void blockActionRejectsFarBreakTarget() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(80, 120, 80, Blocks.DIRT);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.BlockAction(
                    GamePacket.BlockAction.Action.BREAK,
                    0,
                    80,
                    120,
                    80,
                    0,
                    0,
                    0,
                    Blocks.AIR
            ));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(Blocks.DIRT, world.blockAt(80, 120, 80).orElseThrow().id());
            assertEquals(16, count(slots, items, "voxel:dirt"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void blockActionRejectsFarPlacementTarget() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(80, 119, 80, Blocks.STONE);
        world.setBlock(80, 120, 80, Blocks.AIR);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.BlockAction(
                    GamePacket.BlockAction.Action.PLACE,
                    0,
                    80,
                    119,
                    80,
                    80,
                    120,
                    80,
                    Blocks.DIRT
            ));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(Blocks.AIR, world.blockAt(80, 120, 80).orElseThrow().id());
            assertEquals(16, count(slots, items, "voxel:dirt"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void blockUpdatesOnlyReachConnectionsThatHaveStreamedTheChunk() {
        ServerWorld world = new ServerWorld(123L);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel viewer = loggedInChannel(world, tracker, PLAYER_ID);
        EmbeddedChannel distantViewer = loggedInChannel(world, tracker, SECOND_PLAYER_ID);
        try {
            viewer.writeInbound(new GamePacket.PlayerMove(96.5, 180.0, 8.5, 0.0f, 0.0f, false));
            drainOutbound(viewer);
            drainOutbound(distantViewer);

            world.setBlock(96, 180, 9, Blocks.DIRT);
            viewer.writeInbound(new GamePacket.BlockAction(
                    GamePacket.BlockAction.Action.BREAK,
                    0,
                    96,
                    180,
                    9,
                    0,
                    0,
                    0,
                    Blocks.AIR
            ));

            OutboundPackets viewerPackets = readOutboundPackets(viewer);

            assertEquals(Blocks.AIR, viewerPackets.blockUpdate().blockId());
            assertFalse(readBlockUpdateResponse(distantViewer).hasBlockUpdate());
        } finally {
            viewer.finishAndReleaseAll();
            distantViewer.finishAndReleaseAll();
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
    void storageOpenAndTransferUseSharedTransactions() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.STORAGE_CRATE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.StorageOpenRequest(8, 120, 9, 1));
            readLastStorageOpen(channel);

            GamePacket.StorageTransfer transfer = new GamePacket.StorageTransfer(
                    8,
                    120,
                    9,
                    false,
                    0,
                    GamePacket.StorageTransfer.AUTO_TARGET_SLOT,
                    1,
                    2
            );
            channel.writeInbound(transfer);
            readStorageOpenResponse(channel);
            channel.writeInbound(transfer);
            StorageOpenResponse replay = readStorageOpenResponse(channel);

            assertEquals(15, count(replay.inventory().slots(), items, "voxel:dirt"));
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
    void storageTransferRejectsFarCrate() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(80, 120, 80, Blocks.STORAGE_CRATE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.StorageTransfer(
                    80,
                    120,
                    80,
                    false,
                    0,
                    GamePacket.StorageTransfer.AUTO_TARGET_SLOT,
                    1,
                    1
            ));

            StorageOpenResponse response = readStorageOpenResponse(channel);

            assertFalse(response.hasStorageOpen());
            assertEquals(16, count(response.inventory().slots(), items, "voxel:dirt"));
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
    void sleepRequestRejectsFarMat() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(80, 120, 80, Blocks.SLEEPING_MAT);
        world.setBlock(80, 123, 80, Blocks.SKYROOT_PLANKS);
        world.setDayTimeTicks(ServerWorld.NIGHT_START_TICK + 500L);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.SleepRequest(80, 120, 80));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(ServerWorld.NIGHT_START_TICK + 500L, world.dayTimeTicks());
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
            channel.writeInbound(new GamePacket.PlayerMove(matX + 0.5, matY, matZ + 0.5, 0.0f, 0.0f, false));
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
    void blockInteractHarvestsReedsForWaterContainers() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.REEDS);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.BlockInteract(0, 8, 120, 9));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(1, count(slots, items, "voxel:reed_bundle"));
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

            CampfireResponse packets = readCampfireResponse(channel);

            assertEquals(Blocks.CAMPFIRE_ACTIVE, packets.blockUpdate().blockId());
            assertEquals(5, count(packets.inventory().slots(), items, "voxel:twig"));
            assertTrue(packets.campfireStatus().active());
            assertTrue(packets.campfireStatus().fuelSecondsRemaining() > 30.0);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void cookRequestRejectsFarCampfire() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(80, 120, 80, Blocks.CAMPFIRE_ACTIVE);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.CookRequest(80, 120, 80, "voxel:charcoal", List.of(2)));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(6, count(slots, items, "voxel:skyroot_log"));
            assertEquals(0, count(slots, items, "voxel:charcoal"));
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
    void blockInteractHarvestsBarkFromTreeStump() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 120, 9, Blocks.TREE_STUMP);
        EmbeddedChannel channel = loggedInChannel(world);
        try {
            channel.writeInbound(new GamePacket.BlockInteract(0, 8, 120, 9));

            List<ItemStack> slots = readLastInventory(channel);

            assertEquals(2, count(slots, items, "voxel:bark_strip"));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void entityInteractAcceptsKnownReachableEntity() {
        ServerWorld world = new ServerWorld(123L);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EntitySnapshot target = reachableAmbientTarget();
        tracker.addAmbient(target);
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        try {
            channel.writeInbound(new GamePacket.EntityInteract(target.entityId(), 0, GamePacket.EntityInteract.Action.OBSERVE));

            EntityInteractResponse response = readEntityInteractResponse(channel);
            assertFalse(response.hasEntitySnapshots());

            GamePacket.EntitySnapshots snapshots = flushEntitySnapshots(world, tracker, channel);
            assertTrue(snapshots.snapshots().stream().anyMatch(snapshot -> snapshot.entityId() == target.entityId()));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void entitySnapshotsAreFilteredPerViewerRadius() {
        ServerWorld world = new ServerWorld(123L);
        ServerEntityTracker tracker = new ServerEntityTracker(123L);
        EmbeddedChannel near = loggedInChannel(world, tracker, PLAYER_ID);
        EmbeddedChannel far = loggedInChannel(world, tracker, SECOND_PLAYER_ID);
        try {
            drainOutbound(near);
            drainOutbound(far);

            near.writeInbound(new GamePacket.PlayerMove(8.5, 120.0, 8.5, 0.0f, 0.0f, false));
            drainOutbound(near);
            drainOutbound(far);

            far.writeInbound(new GamePacket.PlayerMove(120.0, 180.0, 8.5, 0.0f, 0.0f, false));

            ServerConnectionHandler.broadcastEntitySnapshots(world, tracker);
            GamePacket.EntitySnapshots nearSnapshots = readLastEntitySnapshots(near);
            GamePacket.EntitySnapshots farSnapshots = readLastEntitySnapshots(far);
            long nearPlayerEntityId = playerSnapshot(tracker, PLAYER_ID).entityId();
            long farPlayerEntityId = playerSnapshot(tracker, SECOND_PLAYER_ID).entityId();

            assertTrue(containsEntity(nearSnapshots, nearPlayerEntityId));
            assertFalse(containsEntity(nearSnapshots, farPlayerEntityId));
            assertTrue(containsEntity(farSnapshots, farPlayerEntityId));
            assertFalse(containsEntity(farSnapshots, nearPlayerEntityId));
        } finally {
            near.finishAndReleaseAll();
            far.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveMarksEntitySnapshotsDirtyWithoutImmediateBroadcast() {
        ServerWorld world = new ServerWorld(123L);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        try {
            ServerConnectionHandler.consumeEntitySnapshotDirty(world);

            channel.writeInbound(new GamePacket.PlayerMove(8.75, 120.0, 8.75, 0.0f, 0.0f, false));

            assertFalse(drainHasEntitySnapshots(channel));
            assertTrue(ServerConnectionHandler.consumeEntitySnapshotDirty(world));
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
    void entityInteractRejectsFarEntity() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EntitySnapshot target = reachableAmbientTarget();
        tracker.addAmbient(target);
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(120.0, 180.0, 8.5, 0.0f, 0.0f, false));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.EntityInteract(target.entityId(), 0, GamePacket.EntityInteract.Action.OBSERVE));

            EntityInteractResponse response = readEntityInteractResponse(channel);
            assertFalse(response.hasEntitySnapshots());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void entityInteractFeedConsumesFoodAndUpdatesAmbientEntity() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EntitySnapshot target = reachableAmbientTarget();
        tracker.addAmbient(target);
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        try {
            channel.writeInbound(new GamePacket.EntityInteract(target.entityId(), 7, GamePacket.EntityInteract.Action.FEED));

            EntityInteractResponse response = readEntityInteractResponse(channel);
            assertFalse(response.hasEntitySnapshots());
            GamePacket.EntitySnapshots snapshots = flushEntitySnapshots(world, tracker, channel);
            EntitySnapshot updated = snapshots.snapshots().stream()
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
        ServerEntityTracker tracker = new ServerEntityTracker();
        EntitySnapshot target = reachableAmbientTarget();
        tracker.addAmbient(target);
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
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
    void entityInteractAttackDamagesReachableAmbientEntity() {
        ServerWorld world = new ServerWorld(123L);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EntitySnapshot target = reachableAmbientTarget();
        tracker.addAmbient(target);
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        try {
            channel.writeInbound(new GamePacket.EntityInteract(target.entityId(), 8, GamePacket.EntityInteract.Action.ATTACK));

            EntityInteractResponse response = readEntityInteractResponse(channel);
            assertFalse(response.hasEntitySnapshots());
            GamePacket.EntitySnapshots snapshots = flushEntitySnapshots(world, tracker, channel);
            EntitySnapshot updated = snapshots.snapshots().stream()
                    .filter(snapshot -> snapshot.entityId() == target.entityId())
                    .findFirst()
                    .orElseThrow();
            assertEquals(target.health() - 3, updated.health());
            assertEquals(EntitySnapshot.STATE_FLEE, updated.stateKey());
            assertTrue(updated.velocityX() != 0.0 || updated.velocityZ() != 0.0);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void entityInteractAttackSpawnsMossSnailDropsOnKill() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerWorld world = new ServerWorld(123L);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EntitySnapshot target = new EntitySnapshot(
                1_000L,
                "voxel:moss_snail",
                null,
                8.5,
                120.0,
                8.5,
                0.0f,
                0.0f,
                3
        );
        tracker.addAmbient(target);
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(target.x() - 1.0, playerEyeY(target), target.z(), 0.0f, 0.0f, false));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.EntityInteract(target.entityId(), 8, GamePacket.EntityInteract.Action.ATTACK));

            EntityInteractResponse response = readEntityInteractResponse(channel);
            assertEquals(0, count(response.inventory().slots(), items, "voxel:moss_clump"));
            assertEquals(0, count(response.inventory().slots(), items, "voxel:slime_drop"));
            assertTrue(tracker.snapshot(target.entityId()).isEmpty());
            assertEquals(1, tracker.itemDropCount());
            assertFalse(response.hasEntitySnapshots());
            GamePacket.EntitySnapshots snapshots = flushEntitySnapshots(world, tracker, channel);
            assertFalse(snapshots.snapshots().stream().anyMatch(snapshot -> snapshot.entityId() == target.entityId()));
            assertTrue(snapshots.snapshots().stream().anyMatch(snapshot -> ItemDropType.isTypeKey(snapshot.typeKey())));
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveCollectsNearbyReadyItemDrops() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        ServerEntityTracker tracker = new ServerEntityTracker();
        tracker.spawnItemDrop(
                "voxel:moss_clump",
                new ItemStack(items.requireByKey("voxel:moss_clump").id(), 1),
                8.5,
                120.0,
                8.5,
                -20L
        );
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.5, 120.0, 8.5, 0.0f, 0.0f, false));

            List<ItemStack> slots = readLastInventory(channel);
            assertEquals(1, count(slots, items, "voxel:moss_clump"));
            assertEquals(0, tracker.itemDropCount());
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
                new ServerEntityTracker(),
                TEST_STREAM_RADIUS_CHUNKS
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
    void loginRejectsSecondLoginAttemptOnSameConnection() {
        ServerWorld world = new ServerWorld(123L);
        EmbeddedChannel channel = new EmbeddedChannel(new ServerConnectionHandler(
                world,
                (username, authToken) -> AuthResult.accepted(PLAYER_ID),
                new ServerEntityTracker(),
                TEST_STREAM_RADIUS_CHUNKS
        ));
        try {
            channel.writeInbound(new GamePacket.LoginRequest("Tester", "dev-token"));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.LoginRequest("TesterAgain", "dev-token"));

            Object outbound;
            GamePacket.LoginRejected rejected = null;
            while ((outbound = channel.readOutbound()) != null) {
                if (outbound instanceof GamePacket.LoginRejected loginRejected) {
                    rejected = loginRejected;
                }
            }

            assertTrue(rejected != null && "Already logged in".equals(rejected.reason()));
            assertFalse(channel.isActive());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void loginLoadsPlayerSaveAndDisconnectWritesLatestSnapshot(@TempDir Path playerSaveDirectory) throws Exception {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short berries = items.requireByKey("voxel:berries").id();
        PlayerSaveStore.save(playerSaveDirectory, new PlayerSave(
                SaveMetadata.CURRENT_SAVE_VERSION,
                PLAYER_ID,
                "SavedTester",
                9.5,
                120.0,
                10.5,
                30.0f,
                4.0f,
                List.of(new ItemStack(berries, 6)),
                0,
                new PlayerSave.SurvivalStats(13, 11, 9, 18),
                PlayerSave.SpawnPoint.empty(),
                "survival",
                List.of("voxel:herb_soup"),
                List.of("voxel:meadow"),
                List.of("found-camp"),
                "overworld"
        ));
        EmbeddedChannel channel = new EmbeddedChannel(new ServerConnectionHandler(
                new ServerWorld(123L),
                (username, authToken) -> AuthResult.accepted(PLAYER_ID),
                new ServerEntityTracker(),
                TEST_STREAM_RADIUS_CHUNKS,
                playerSaveDirectory
        ));
        try {
            channel.writeInbound(new GamePacket.LoginRequest("Tester", "dev-token"));

            GamePacket.PlayerPositionSnapshot position = readLastPlayerPosition(channel);
            assertEquals(9.5, position.x(), 0.001);
            assertEquals(10.5, position.z(), 0.001);

            channel.writeInbound(new GamePacket.PlayerMove(
                    1L,
                    10.5,
                    120.0,
                    10.5,
                    45.0f,
                    0.0f,
                    false,
                    new PlayerWaterState(false, false, false)
            ));
            readLastPlayerPosition(channel);
        } finally {
            channel.close();
            channel.finishAndReleaseAll();
        }

        PlayerSave saved = PlayerSaveStore.load(playerSaveDirectory, PLAYER_ID).orElseThrow();
        assertEquals(10.5, saved.x(), 0.001);
        assertEquals(new ItemStack(berries, 6), saved.inventory().getFirst());
        assertEquals(13, saved.survival().health());
        assertEquals(List.of("voxel:herb_soup"), saved.discoveredRecipes());
        assertEquals(List.of("found-camp"), saved.journalEntries());
    }

    @Test
    void playerMoveRejectsExtremeTeleportAfterAcceptedMove() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.75, 120.0, 8.75, 0.0f, 0.0f, false));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.PlayerMove(1000.0, 120.0, 1000.0, 0.0f, 0.0f, false));
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
    void playerMoveRejectsSurvivalSpeedBurstAfterAcceptedMove() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.75, 180.0, 8.75, 0.0f, 0.0f, false));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.PlayerMove(11.95, 180.0, 8.75, 0.0f, 0.0f, false));
            drainOutbound(channel);

            EntitySnapshot player = playerSnapshot(tracker);
            assertEquals(8.75, player.x(), 0.001);
            assertEquals(180.0, player.y(), 0.001);
            assertEquals(8.75, player.z(), 0.001);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveRejectsSuddenAccelerationBurstAfterMovementSample() throws Exception {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.75, 180.0, 8.75, 0.0f, 0.0f, false));
            drainOutbound(channel);

            ageLastAcceptedMove(channel, 0.1);
            channel.writeInbound(new GamePacket.PlayerMove(8.85, 180.0, 8.75, 0.0f, 0.0f, false));
            drainOutbound(channel);

            ageLastAcceptedMove(channel, 0.1);
            channel.writeInbound(new GamePacket.PlayerMove(10.85, 180.0, 8.75, 0.0f, 0.0f, false));
            drainOutbound(channel);

            EntitySnapshot player = playerSnapshot(tracker);
            assertEquals(8.85, player.x(), 0.001);
            assertEquals(180.0, player.y(), 0.001);
            assertEquals(8.75, player.z(), 0.001);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveRejectsExcessiveMovementPacketRate() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
            for (int i = 1; i <= 35; i++) {
                channel.writeInbound(new GamePacket.PlayerMove(8.75 + i * 0.01, 180.0, 8.75, 0.0f, 0.0f, false));
                drainOutbound(channel);
            }

            EntitySnapshot player = playerSnapshot(tracker);
            assertTrue(player.x() < 9.10);
            assertTrue(player.x() >= 9.0);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveAcceptsLaggedSurvivalStrideInsideEnvelope() throws Exception {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.75, 180.0, 8.75, 0.0f, 0.0f, false));
            drainOutbound(channel);

            ageLastAcceptedMove(channel, 0.5);
            channel.writeInbound(new GamePacket.PlayerMove(12.25, 180.0, 8.75, 0.0f, 0.0f, false));
            drainOutbound(channel);

            EntitySnapshot player = playerSnapshot(tracker);
            assertEquals(12.25, player.x(), 0.001);
            assertEquals(180.0, player.y(), 0.001);
            assertEquals(8.75, player.z(), 0.001);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveRejectsPathThroughWallEvenWhenDestinationIsClear() throws Exception {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(9, 179, 8, Blocks.STONE);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.5, 180.0, 8.5, 0.0f, 0.0f, false));
            drainOutbound(channel);

            ageLastAcceptedMove(channel, 0.5);
            channel.writeInbound(new GamePacket.PlayerMove(10.5, 180.0, 8.5, 0.0f, 0.0f, false));
            drainOutbound(channel);

            EntitySnapshot player = playerSnapshot(tracker);
            assertEquals(8.5, player.x(), 0.001);
            assertEquals(180.0, player.y(), 0.001);
            assertEquals(8.5, player.z(), 0.001);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveEchoesAuthoritativeStateWithSequence() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(7L, 8.75, 180.0, 8.75, 12.0f, -4.0f, false));

            GamePacket.PlayerPositionSnapshot snapshot = readLastPlayerPosition(channel);
            assertEquals(7L, snapshot.sequence());
            assertEquals(8.75, snapshot.x(), 0.001);
            assertEquals(180.0, snapshot.y(), 0.001);
            assertEquals(8.75, snapshot.z(), 0.001);
            assertEquals(12.0f, snapshot.yaw(), 0.001f);
            assertEquals(-4.0f, snapshot.pitch(), 0.001f);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveRejectsStalePositiveSequenceAndResendsAuthoritativeState() throws Exception {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(3L, 8.75, 180.0, 8.75, 0.0f, 0.0f, false));
            drainOutbound(channel);

            ageLastAcceptedMove(channel, 0.5);
            channel.writeInbound(new GamePacket.PlayerMove(3L, 9.75, 180.0, 8.75, 0.0f, 0.0f, false));

            GamePacket.PlayerPositionSnapshot snapshot = readLastPlayerPosition(channel);
            assertEquals(3L, snapshot.sequence());
            assertEquals(8.75, snapshot.x(), 0.001);
            assertEquals(180.0, snapshot.y(), 0.001);
            assertEquals(8.75, snapshot.z(), 0.001);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveRejectsPositionInsideSolidBlock() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(10, 119, 8, Blocks.STONE);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.75, 120.0, 8.75, 0.0f, 0.0f, false));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.PlayerMove(9.75, 120.0, 8.75, 0.0f, 0.0f, false));
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
    void playerMoveAcceptsGroundedClaimWithSupport() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 119, 8, Blocks.STONE);
        world.setBlock(8, 120, 8, Blocks.AIR);
        world.setBlock(8, 121, 8, Blocks.AIR);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        double eyeY = 120.0 + PlayerBounds.DEFAULT.eyeHeight();
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.5, eyeY, 8.5, 0.0f, 0.0f, true));
            drainOutbound(channel);

            EntitySnapshot player = playerSnapshot(tracker);
            assertEquals(8.5, player.x(), 0.001);
            assertEquals(eyeY, player.y(), 0.001);
            assertEquals(8.5, player.z(), 0.001);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveRejectsGroundedClaimWithoutSupport() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.75, 180.0, 8.75, 0.0f, 0.0f, false));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.PlayerMove(9.0, 180.0, 8.75, 0.0f, 0.0f, true));
            drainOutbound(channel);

            EntitySnapshot player = playerSnapshot(tracker);
            assertEquals(8.75, player.x(), 0.001);
            assertEquals(180.0, player.y(), 0.001);
            assertEquals(8.75, player.z(), 0.001);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveRejectsAirborneUpwardStartWithoutPriorGround() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.75, 180.0, 8.75, 0.0f, 0.0f, false));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.PlayerMove(8.75, 180.4, 8.75, 0.0f, 0.0f, false));
            drainOutbound(channel);

            EntitySnapshot player = playerSnapshot(tracker);
            assertEquals(8.75, player.x(), 0.001);
            assertEquals(180.0, player.y(), 0.001);
            assertEquals(8.75, player.z(), 0.001);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveRejectsWaterAssistWhenWaterIsOnlyNearby() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(9, 180, 8, Blocks.WATER);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.5, 180.0, 8.5, 0.0f, 0.0f, false));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.PlayerMove(8.5, 180.4, 8.5, 0.0f, 0.0f, false));
            drainOutbound(channel);

            EntitySnapshot player = playerSnapshot(tracker);
            assertEquals(8.5, player.x(), 0.001);
            assertEquals(180.0, player.y(), 0.001);
            assertEquals(8.5, player.z(), 0.001);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveRejectsForgedWaterStateClaim() {
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(new ServerWorld(123L), tracker);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.5, 180.0, 8.5, 0.0f, 0.0f, false));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.PlayerMove(
                    8.6,
                    180.0,
                    8.5,
                    0.0f,
                    0.0f,
                    false,
                    new PlayerWaterState(false, true, false)
            ));
            drainOutbound(channel);

            EntitySnapshot player = playerSnapshot(tracker);
            assertEquals(8.5, player.x(), 0.001);
            assertEquals(180.0, player.y(), 0.001);
            assertEquals(8.5, player.z(), 0.001);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveAllowsWaterAssistedUpwardStartWhenBodyTouchesWater() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 179, 8, Blocks.WATER);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        PlayerWaterState bodyWater = new PlayerWaterState(false, true, false);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.5, 180.0, 8.5, 0.0f, 0.0f, false, bodyWater));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.PlayerMove(8.5, 180.4, 8.5, 0.0f, 0.0f, false, bodyWater));
            drainOutbound(channel);

            EntitySnapshot player = playerSnapshot(tracker);
            assertEquals(8.5, player.x(), 0.001);
            assertEquals(180.4, player.y(), 0.001);
            assertEquals(8.5, player.z(), 0.001);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveAcceptsJumpStartAfterSupportedGroundMove() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 119, 8, Blocks.STONE);
        world.setBlock(8, 120, 8, Blocks.AIR);
        world.setBlock(8, 121, 8, Blocks.AIR);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        double groundedEyeY = 120.0 + PlayerBounds.DEFAULT.eyeHeight();
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.5, groundedEyeY, 8.5, 0.0f, 0.0f, true));
            drainOutbound(channel);

            channel.writeInbound(new GamePacket.PlayerMove(8.5, groundedEyeY + 0.35, 8.5, 0.0f, 0.0f, false));
            drainOutbound(channel);

            EntitySnapshot player = playerSnapshot(tracker);
            assertEquals(8.5, player.x(), 0.001);
            assertEquals(groundedEyeY + 0.35, player.y(), 0.001);
            assertEquals(8.5, player.z(), 0.001);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveAppliesFallDamageWhenLandingAfterLongAcceptedFall() throws Exception {
        ServerWorld world = new ServerWorld(123L);
        prepareFallLandingColumn(world, false);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        double groundedEyeY = 120.0 + PlayerBounds.DEFAULT.eyeHeight();
        double highEyeY = groundedEyeY + 20.0;
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.5, highEyeY, 8.5, 0.0f, 0.0f, false));
            drainOutbound(channel);

            ageLastAcceptedMove(channel, 0.5);
            channel.writeInbound(new GamePacket.PlayerMove(8.5, highEyeY - 12.0, 8.5, 0.0f, 0.0f, false));
            drainOutbound(channel);

            ageLastAcceptedMove(channel, 0.5);
            channel.writeInbound(new GamePacket.PlayerMove(8.5, groundedEyeY, 8.5, 0.0f, 0.0f, true));

            GamePacket.PlayerStatsSnapshot stats = readLastPlayerStats(channel);
            assertEquals(4, stats.health());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void playerMoveCushionsFallDamageWhenLandingInWater() throws Exception {
        ServerWorld world = new ServerWorld(123L);
        prepareFallLandingColumn(world, true);
        ServerEntityTracker tracker = new ServerEntityTracker();
        EmbeddedChannel channel = loggedInChannel(world, tracker);
        double groundedEyeY = 120.0 + PlayerBounds.DEFAULT.eyeHeight();
        double highEyeY = groundedEyeY + 20.0;
        PlayerWaterState landingWater = new PlayerWaterState(true, true, true);
        try {
            channel.writeInbound(new GamePacket.PlayerMove(8.5, highEyeY, 8.5, 0.0f, 0.0f, false));
            drainOutbound(channel);

            ageLastAcceptedMove(channel, 0.5);
            channel.writeInbound(new GamePacket.PlayerMove(8.5, highEyeY - 12.0, 8.5, 0.0f, 0.0f, false));
            drainOutbound(channel);

            ageLastAcceptedMove(channel, 0.5);
            channel.writeInbound(new GamePacket.PlayerMove(8.5, groundedEyeY + 5.0, 8.5, 0.0f, 0.0f, false));
            drainOutbound(channel);

            ageLastAcceptedMove(channel, 0.5);
            channel.writeInbound(new GamePacket.PlayerMove(8.5, groundedEyeY, 8.5, 0.0f, 0.0f, true, landingWater));

            GamePacket.PlayerStatsSnapshot stats = readLastPlayerStats(channel);
            assertEquals(16, stats.health());
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
            channel.writeInbound(new GamePacket.PlayerMove(8.6, 120.0, 8.5, 0.0f, 0.0f, false));

            GamePacket.PlayerStatsSnapshot stats = readLastPlayerStats(channel);

            assertEquals(0, stats.comfort());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void clientTransactionIdsMustAdvanceByExactlyOne() throws Exception {
        ServerConnectionHandler handler = new ServerConnectionHandler(
                new ServerWorld(123L),
                (username, authToken) -> AuthResult.accepted(PLAYER_ID),
                new ServerEntityTracker()
        );
        Field lastTransactionField = ServerConnectionHandler.class.getDeclaredField("lastClientTransactionId");
        lastTransactionField.setAccessible(true);
        Method validator = ServerConnectionHandler.class.getDeclaredMethod("isNextClientTransaction", int.class);
        validator.setAccessible(true);

        lastTransactionField.setInt(handler, 5);
        assertFalse((boolean) validator.invoke(handler, 5));
        assertFalse((boolean) validator.invoke(handler, 7));
        assertTrue((boolean) validator.invoke(handler, 6));
    }

    @Test
    void clientTransactionIdWrapRequiresOneAfterIntegerMax() throws Exception {
        ServerConnectionHandler handler = new ServerConnectionHandler(
                new ServerWorld(123L),
                (username, authToken) -> AuthResult.accepted(PLAYER_ID),
                new ServerEntityTracker()
        );
        Field lastTransactionField = ServerConnectionHandler.class.getDeclaredField("lastClientTransactionId");
        lastTransactionField.setAccessible(true);
        Method validator = ServerConnectionHandler.class.getDeclaredMethod("isNextClientTransaction", int.class);
        validator.setAccessible(true);

        lastTransactionField.setInt(handler, Integer.MAX_VALUE);
        assertTrue((boolean) validator.invoke(handler, 1));
        assertFalse((boolean) validator.invoke(handler, 2));
        assertFalse((boolean) validator.invoke(handler, 0));
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
                tracker,
                TEST_STREAM_RADIUS_CHUNKS
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

    private static CampfireResponse readCampfireResponse(EmbeddedChannel channel) {
        GamePacket.InventorySnapshot inventory = null;
        GamePacket.BlockUpdate blockUpdate = null;
        GamePacket.CampfireStatus campfireStatus = null;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof GamePacket.InventorySnapshot snapshot) {
                inventory = snapshot;
            } else if (outbound instanceof GamePacket.BlockUpdate update) {
                blockUpdate = update;
            } else if (outbound instanceof GamePacket.CampfireStatus status) {
                campfireStatus = status;
            }
        }
        if (inventory == null || campfireStatus == null) {
            throw new AssertionError("Expected inventory and campfire status");
        }
        return new CampfireResponse(inventory, blockUpdate, campfireStatus);
    }

    private static BlockUpdateResponse readBlockUpdateResponse(EmbeddedChannel channel) {
        GamePacket.BlockUpdate blockUpdate = null;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof GamePacket.BlockUpdate update) {
                blockUpdate = update;
            }
        }
        return new BlockUpdateResponse(blockUpdate);
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

    private static GamePacket.PlayerPositionSnapshot readLastPlayerPosition(EmbeddedChannel channel) {
        GamePacket.PlayerPositionSnapshot position = null;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof GamePacket.PlayerPositionSnapshot snapshot) {
                position = snapshot;
            }
        }
        if (position == null) {
            throw new AssertionError("Expected player position snapshot");
        }
        return position;
    }

    private static GamePacket.EntitySnapshots readLastEntitySnapshots(EmbeddedChannel channel) {
        GamePacket.EntitySnapshots entitySnapshots = null;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof GamePacket.EntitySnapshots snapshots) {
                entitySnapshots = snapshots;
            }
        }
        if (entitySnapshots == null) {
            throw new AssertionError("Expected entity snapshots");
        }
        return entitySnapshots;
    }

    private static GamePacket.EntitySnapshots flushEntitySnapshots(
            ServerWorld world,
            ServerEntityTracker tracker,
            EmbeddedChannel channel
    ) {
        ServerConnectionHandler.broadcastEntitySnapshots(world, tracker);
        return readLastEntitySnapshots(channel);
    }

    private static boolean drainHasEntitySnapshots(EmbeddedChannel channel) {
        boolean hasEntitySnapshots = false;
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof GamePacket.EntitySnapshots) {
                hasEntitySnapshots = true;
            }
        }
        return hasEntitySnapshots;
    }

    private static void drainOutbound(EmbeddedChannel channel) {
        while (channel.readOutbound() != null) {
        }
    }

    private static EntitySnapshot playerSnapshot(ServerEntityTracker tracker) {
        return playerSnapshot(tracker, PLAYER_ID);
    }

    private static EntitySnapshot playerSnapshot(ServerEntityTracker tracker, UUID playerId) {
        return tracker.snapshots().stream()
                .filter(snapshot -> playerId.equals(snapshot.ownerPlayerId()))
                .findFirst()
                .orElseThrow();
    }

    private static EntitySnapshot reachableAmbientTarget() {
        return new EntitySnapshot(
                9001L,
                "voxel:cozy_sheep",
                null,
                9.5,
                120.0,
                8.5,
                0.0f,
                0.0f,
                10
        );
    }

    private static double playerEyeY(EntitySnapshot target) {
        return target.y() + PlayerBounds.DEFAULT.eyeHeight();
    }

    private static void prepareFallLandingColumn(ServerWorld world, boolean waterLanding) {
        world.setBlock(8, 119, 8, Blocks.STONE);
        for (int y = 120; y <= 150; y++) {
            if (waterLanding && (y == 120 || y == 121)) {
                world.setBlock(8, y, 8, Blocks.WATER);
            } else {
                world.setBlock(8, y, 8, Blocks.AIR);
            }
        }
    }

    private static void ageLastAcceptedMove(EmbeddedChannel channel, double seconds) throws Exception {
        Field lastAcceptedMoveTimeField = ServerConnectionHandler.class.getDeclaredField("lastAcceptedMoveTime");
        lastAcceptedMoveTimeField.setAccessible(true);
        lastAcceptedMoveTimeField.setDouble(
                channel.pipeline().get(ServerConnectionHandler.class),
                System.nanoTime() / 1_000_000_000.0 - seconds
        );
    }

    private static boolean containsEntity(GamePacket.EntitySnapshots snapshots, long entityId) {
        return snapshots.snapshots().stream().anyMatch(snapshot -> snapshot.entityId() == entityId);
    }

    private static Object pendingCook(EmbeddedChannel channel) throws Exception {
        Field pendingCookField = ServerConnectionHandler.class.getDeclaredField("pendingCook");
        pendingCookField.setAccessible(true);
        return pendingCookField.get(channel.pipeline().get(ServerConnectionHandler.class));
    }

    private static Inventory inventory(EmbeddedChannel channel) throws Exception {
        Field inventoryField = ServerConnectionHandler.class.getDeclaredField("inventory");
        inventoryField.setAccessible(true);
        return (Inventory) inventoryField.get(channel.pipeline().get(ServerConnectionHandler.class));
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

    private record CampfireResponse(
            GamePacket.InventorySnapshot inventory,
            GamePacket.BlockUpdate blockUpdate,
            GamePacket.CampfireStatus campfireStatus
    ) {
    }

    private record BlockUpdateResponse(GamePacket.BlockUpdate blockUpdate) {
        boolean hasBlockUpdate() {
            return blockUpdate != null;
        }
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
