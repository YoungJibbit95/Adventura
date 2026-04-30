package dev.voxelgame.server.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.physics.PlayerWaterState;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.common.world.structure.StructureMarker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerWorldTest {
    @Test
    void appliesValidatedPlaceAndBreakActions() {
        ServerWorld world = new ServerWorld(123L);
        GamePacket.BlockAction place = new GamePacket.BlockAction(
                GamePacket.BlockAction.Action.PLACE,
                0,
                0,
                249,
                0,
                0,
                250,
                0,
                Blocks.STONE
        );

        GamePacket.BlockUpdate placed = world.applyBlockAction(place).orElseThrow();

        assertEquals(Blocks.STONE, placed.blockId());
        assertEquals(250, placed.y());

        GamePacket.BlockAction breakAction = new GamePacket.BlockAction(
                GamePacket.BlockAction.Action.BREAK,
                0,
                0,
                250,
                0,
                0,
                251,
                0,
                Blocks.AIR
        );

        GamePacket.BlockUpdate broken = world.applyBlockAction(breakAction).orElseThrow();
        assertEquals(Blocks.AIR, broken.blockId());
    }

    @Test
    void rejectsNonAdjacentPlaceAction() {
        ServerWorld world = new ServerWorld(123L);
        GamePacket.BlockAction place = new GamePacket.BlockAction(
                GamePacket.BlockAction.Action.PLACE,
                0,
                0,
                249,
                0,
                0,
                255,
                0,
                Blocks.STONE
        );

        assertTrue(world.applyBlockAction(place).isEmpty());
    }

    @Test
    void playerCollisionChecksSolidBlocksAndIgnoresWater() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(4, 250, 4, Blocks.STONE);
        world.setBlock(5, 250, 4, Blocks.WATER);
        double eyeInsideBlock = 250.0 + PlayerBounds.DEFAULT.eyeHeight();
        double eyeAboveBlock = 251.0 + PlayerBounds.DEFAULT.eyeHeight();

        assertTrue(world.collidesPlayer(4.5, eyeInsideBlock, 4.5, PlayerBounds.DEFAULT));
        assertFalse(world.collidesPlayer(5.5, eyeInsideBlock, 4.5, PlayerBounds.DEFAULT));
        assertFalse(world.collidesPlayer(4.5, eyeAboveBlock, 4.5, PlayerBounds.DEFAULT));
    }

    @Test
    void playerPathClearRejectsTunnelingThroughWalls() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(9, 119, 8, Blocks.STONE);

        assertFalse(world.playerPathClear(8.5, 120.0, 8.5, 10.5, 120.0, 8.5, PlayerBounds.DEFAULT, 0.45));
    }

    @Test
    void playerCollisionWorksAcrossChunkBoundaries() {
        ServerWorld world = new ServerWorld(123L);
        double eyeY = 120.0;
        world.setBlock(16, 119, 8, Blocks.STONE);

        assertFalse(world.collidesPlayer(15.65, eyeY, 8.5, PlayerBounds.DEFAULT));
        assertTrue(world.collidesPlayer(15.75, eyeY, 8.5, PlayerBounds.DEFAULT));
    }

    @Test
    void playerWaterStateSeparatesFeetBodyAndHead() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(8, 64, 8, Blocks.WATER);
        world.setBlock(8, 65, 8, Blocks.AIR);
        world.setBlock(8, 66, 8, Blocks.AIR);
        double eyeY = 66.05;

        PlayerWaterState feetOnly = world.playerWaterState(8.5, eyeY, 8.5, PlayerBounds.DEFAULT);
        assertTrue(feetOnly.feetInWater());
        assertFalse(feetOnly.bodyInWater());
        assertFalse(feetOnly.headUnderwater());
        assertTrue(feetOnly.movementAffected());

        world.setBlock(8, 65, 8, Blocks.WATER);
        PlayerWaterState bodyWater = world.playerWaterState(8.5, eyeY, 8.5, PlayerBounds.DEFAULT);
        assertTrue(bodyWater.bodyInWater());
        assertFalse(bodyWater.headUnderwater());

        world.setBlock(8, 66, 8, Blocks.WATER);
        PlayerWaterState submerged = world.playerWaterState(8.5, eyeY, 8.5, PlayerBounds.DEFAULT);
        assertTrue(submerged.headUnderwater());
    }

    @Test
    void ambientEntityPlacementRequiresSupportAndRejectsSolidOrWaterOverlap() {
        ServerWorld world = new ServerWorld(123L);
        EntitySnapshot sheep = ambient(100L, "voxel:cozy_sheep", 20.5, 250.0, 20.5, EntitySnapshot.STATE_IDLE);
        world.setBlock(20, 249, 20, Blocks.GRASS);
        world.setBlock(20, 250, 20, Blocks.AIR);

        assertTrue(world.entityPlacementClear(sheep));

        world.setBlock(20, 250, 20, Blocks.STONE);
        assertFalse(world.entityPlacementClear(sheep));

        EntitySnapshot waterBody = ambient(101L, "voxel:cozy_sheep", 21.5, 250.0, 20.5, EntitySnapshot.STATE_IDLE);
        world.setBlock(21, 249, 20, Blocks.GRASS);
        world.setBlock(21, 250, 20, Blocks.WATER);
        assertFalse(world.entityPlacementClear(waterBody));

        EntitySnapshot waterSupport = ambient(102L, "voxel:cozy_sheep", 22.5, 250.0, 20.5, EntitySnapshot.STATE_IDLE);
        world.setBlock(22, 249, 20, Blocks.WATER);
        world.setBlock(22, 250, 20, Blocks.AIR);
        assertFalse(world.entityPlacementClear(waterSupport));
    }

    @Test
    void firefliesIgnoreGroundSupportButStillAvoidSolidBlocks() {
        ServerWorld world = new ServerWorld(123L);
        EntitySnapshot firefly = ambient(200L, "voxel:firefly_swarm", 30.5, 250.0, 30.5, EntitySnapshot.STATE_WANDER);

        assertTrue(world.entityPlacementClear(firefly));

        world.setBlock(30, 250, 30, Blocks.STONE);

        assertFalse(world.entityPlacementClear(firefly));
    }

    @Test
    void ambientEntityPathRejectsWallTunneling() {
        ServerWorld world = new ServerWorld(123L);
        for (int x = 40; x <= 42; x++) {
            world.setBlock(x, 249, 40, Blocks.GRASS);
            world.setBlock(x, 250, 40, Blocks.AIR);
        }
        EntitySnapshot current = ambient(300L, "voxel:cozy_sheep", 40.5, 250.0, 40.5, EntitySnapshot.STATE_WANDER);
        EntitySnapshot clearTarget = ambient(300L, "voxel:cozy_sheep", 42.5, 250.0, 40.5, EntitySnapshot.STATE_WANDER);

        assertTrue(world.canMoveAmbientEntity(current, clearTarget));

        world.setBlock(41, 250, 40, Blocks.STONE);

        assertFalse(world.canMoveAmbientEntity(current, clearTarget));
    }

    @Test
    void grazersOnlyGrazeOnGrassSupport() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(50, 249, 50, Blocks.GRASS);
        world.setBlock(51, 249, 50, Blocks.DIRT);
        EntitySnapshot grassGraze = ambient(400L, "voxel:cozy_sheep", 50.5, 250.0, 50.5, EntitySnapshot.STATE_GRAZE);
        EntitySnapshot dirtGraze = ambient(401L, "voxel:cozy_sheep", 51.5, 250.0, 50.5, EntitySnapshot.STATE_GRAZE);
        EntitySnapshot dirtWander = ambient(402L, "voxel:cozy_sheep", 51.5, 250.0, 50.5, EntitySnapshot.STATE_WANDER);

        assertTrue(world.entityPlacementClear(grassGraze));
        assertFalse(world.entityPlacementClear(dirtGraze));
        assertTrue(world.entityPlacementClear(dirtWander));
    }

    @Test
    void campfireFuelTurnsActiveAndExpires() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(0, 80, 0, Blocks.CAMPFIRE);

        GamePacket.BlockUpdate active = world.fuelCampfire(0, 80, 0, 10.0, 5.0).orElseThrow();

        assertEquals(Blocks.CAMPFIRE_ACTIVE, active.blockId());
        assertTrue(world.hasActiveCampfireWithin(0.5, 81.0, 0.5, 4, 12.0));

        GamePacket.BlockUpdate expired = world.tickCampfires(16.0).getFirst();

        assertEquals(Blocks.CAMPFIRE_BURNED_OUT, expired.blockId());
        assertFalse(world.hasActiveCampfireWithin(0.5, 81.0, 0.5, 4, 16.0));
    }

    @Test
    void storageCrateMovesStacksBetweenPlayerAndCrate() {
        ServerWorld world = new ServerWorld(123L);
        Registry<ItemType> items = Items.createDefaultRegistry();
        short dirt = items.requireByKey("voxel:dirt").id();
        Inventory player = new Inventory(36);
        world.setBlock(2, 80, 2, Blocks.STORAGE_CRATE);
        player.add(dirt, 12, items);

        world.transferStorageStack(2, 80, 2, player, items, false, 0).orElseThrow();

        assertTrue(player.slot(0).isEmpty());
        assertEquals(new ItemStack(dirt, 12), world.openStorageCrate(2, 80, 2).orElseThrow().getFirst());

        world.transferStorageStack(2, 80, 2, player, items, true, 0).orElseThrow();

        assertEquals(new ItemStack(dirt, 12), player.slot(0));
        assertTrue(world.openStorageCrate(2, 80, 2).orElseThrow().getFirst().isEmpty());
    }

    @Test
    void storageCrateMovesCountIntoTargetSlots() {
        ServerWorld world = new ServerWorld(123L);
        Registry<ItemType> items = Items.createDefaultRegistry();
        short dirt = items.requireByKey("voxel:dirt").id();
        Inventory player = new Inventory(36);
        world.setBlock(2, 80, 2, Blocks.STORAGE_CRATE);
        player.setSlot(5, new ItemStack(dirt, 12));

        world.transferStorageStack(2, 80, 2, player, items, false, 5, 3, 4).orElseThrow();

        assertEquals(new ItemStack(dirt, 8), player.slot(5));
        assertEquals(new ItemStack(dirt, 4), world.openStorageCrate(2, 80, 2).orElseThrow().get(3));

        world.transferStorageStack(2, 80, 2, player, items, true, 3, 6, 2).orElseThrow();

        assertEquals(new ItemStack(dirt, 2), world.openStorageCrate(2, 80, 2).orElseThrow().get(3));
        assertEquals(new ItemStack(dirt, 2), player.slot(6));
    }

    @Test
    void storageCrateRejectsIncompatibleTargetSlot() {
        ServerWorld world = new ServerWorld(123L);
        Registry<ItemType> items = Items.createDefaultRegistry();
        short dirt = items.requireByKey("voxel:dirt").id();
        short stone = items.requireByKey("voxel:stone").id();
        Inventory player = new Inventory(36);
        world.setBlock(2, 80, 2, Blocks.STORAGE_CRATE);
        player.setSlot(0, new ItemStack(dirt, 4));
        player.setSlot(1, new ItemStack(stone, 5));
        world.transferStorageStack(2, 80, 2, player, items, false, 1, 0, 5).orElseThrow();

        world.transferStorageStack(2, 80, 2, player, items, false, 0, 0, 2).orElseThrow();

        assertEquals(new ItemStack(dirt, 4), player.slot(0));
        assertEquals(new ItemStack(stone, 5), world.openStorageCrate(2, 80, 2).orElseThrow().getFirst());
    }

    @Test
    void generatedStructureLootCratesAreSeededOnceAndPersistAfterTransfer() {
        long seed = 123L;
        ServerWorld world = new ServerWorld(seed);
        Registry<ItemType> items = Items.createDefaultRegistry();
        LootPos loot = firstGeneratedVillageLoot(seed);
        short apple = items.requireByKey("voxel:apple").id();
        Inventory player = new Inventory(36);

        List<ItemStack> opened = world.openStorageCrate(loot.x(), loot.y(), loot.z()).orElseThrow();

        assertFalse(opened.getFirst().isEmpty());
        assertEquals(apple, opened.getFirst().itemId());

        world.transferStorageStack(loot.x(), loot.y(), loot.z(), player, items, true, 0).orElseThrow();

        assertTrue(world.openStorageCrate(loot.x(), loot.y(), loot.z()).orElseThrow().getFirst().isEmpty());
        assertEquals(apple, player.slot(0).itemId());
    }

    @Test
    void generatedStructureLootCratesDoNotRerollAfterBreakAndReplace() {
        long seed = 123L;
        ServerWorld world = new ServerWorld(seed);
        LootPos loot = firstGeneratedVillageLoot(seed);

        world.applyBlockAction(new GamePacket.BlockAction(
                GamePacket.BlockAction.Action.BREAK,
                0,
                loot.x(),
                loot.y(),
                loot.z(),
                loot.x(),
                loot.y() + 1,
                loot.z(),
                Blocks.AIR
        )).orElseThrow();
        world.setBlock(loot.x(), loot.y(), loot.z(), Blocks.STORAGE_CRATE);

        assertTrue(world.openStorageCrate(loot.x(), loot.y(), loot.z()).orElseThrow().stream().allMatch(ItemStack::isEmpty));
    }

    @Test
    void breakingStorageCrateRemovesRuntimeStorageData() {
        ServerWorld world = new ServerWorld(123L);
        Registry<ItemType> items = Items.createDefaultRegistry();
        short dirt = items.requireByKey("voxel:dirt").id();
        Inventory player = new Inventory(36);
        world.setBlock(2, 80, 2, Blocks.STORAGE_CRATE);
        player.add(dirt, 12, items);
        world.transferStorageStack(2, 80, 2, player, items, false, 0).orElseThrow();

        world.applyBlockAction(new GamePacket.BlockAction(
                GamePacket.BlockAction.Action.BREAK,
                0,
                2,
                80,
                2,
                2,
                81,
                2,
                Blocks.AIR
        )).orElseThrow();

        assertTrue(world.openStorageCrate(2, 80, 2).isEmpty());

        world.setBlock(2, 80, 2, Blocks.STORAGE_CRATE);

        assertTrue(world.openStorageCrate(2, 80, 2).orElseThrow().stream().allMatch(ItemStack::isEmpty));
    }

    @Test
    void blockEntitySnapshotSavesAndLoadsStorageAndCampfireState() {
        Registry<ItemType> items = Items.createDefaultRegistry();
        short dirt = items.requireByKey("voxel:dirt").id();
        Inventory player = new Inventory(36);
        ServerWorld source = new ServerWorld(123L);
        source.setBlock(2, 80, 2, Blocks.STORAGE_CRATE);
        source.setBlock(3, 80, 2, Blocks.CAMPFIRE);
        player.add(dirt, 9, items);
        source.transferStorageStack(2, 80, 2, player, items, false, 0).orElseThrow();
        source.fuelCampfire(3, 80, 2, 10.0, 20.0).orElseThrow();

        ServerWorld.BlockEntitySnapshot snapshot = source.saveBlockEntities(14.0);

        ServerWorld target = new ServerWorld(123L);
        target.setBlock(2, 80, 2, Blocks.STORAGE_CRATE);
        target.setBlock(3, 80, 2, Blocks.CAMPFIRE);
        target.loadBlockEntities(snapshot, 100.0);

        assertEquals(new ItemStack(dirt, 9), target.openStorageCrate(2, 80, 2).orElseThrow().getFirst());
        assertEquals(16.0, target.campfireFuelSecondsRemaining(3, 80, 2, 100.0).orElseThrow(), 0.001);
        assertTrue(target.hasActiveCampfireWithin(3.5, 81.0, 2.5, 3, 101.0));
        assertEquals(BlockEntityType.STORAGE_CRATE, target.blockEntityTypeAt(2, 80, 2).orElseThrow());
        assertEquals(BlockEntityType.CAMPFIRE, target.blockEntityTypeAt(3, 80, 2).orElseThrow());
    }

    @Test
    void blockEntityStoreTracksLifecycleForInteractiveBlocks() {
        ServerWorld world = new ServerWorld(123L);

        world.setBlock(2, 80, 2, Blocks.STORAGE_CRATE);
        world.setBlock(3, 80, 2, Blocks.COOKING_POT);
        world.setBlock(4, 80, 2, Blocks.FORGE);

        assertEquals(BlockEntityType.STORAGE_CRATE, world.blockEntityTypeAt(2, 80, 2).orElseThrow());
        assertEquals(BlockEntityType.COOKING_POT, world.blockEntityTypeAt(3, 80, 2).orElseThrow());
        assertEquals(BlockEntityType.FORGE, world.blockEntityTypeAt(4, 80, 2).orElseThrow());

        ServerWorld.BlockEntitySnapshot snapshot = world.saveBlockEntities(0.0);
        assertTrue(snapshot.blockEntities().entries().stream()
                .anyMatch(entry -> entry.typeKey().equals(BlockEntityType.COOKING_POT.typeKey())));

        world.setBlock(3, 80, 2, Blocks.AIR);

        assertTrue(world.blockEntityTypeAt(3, 80, 2).isEmpty());
    }

    @Test
    void blockDiffSnapshotRestoresPlacedAndRemovedBlocksWithUnknownFallback() {
        ServerWorld source = new ServerWorld(123L);
        source.setBlock(5, 80, 5, Blocks.STONE);
        source.setBlock(6, 80, 5, Blocks.DIRT);
        source.setBlock(6, 80, 5, Blocks.AIR);

        ServerWorld target = new ServerWorld(123L);
        target.loadBlockDiffs(source.saveBlockDiffs());
        target.loadBlockDiffs(List.of(
                new ServerWorld.BlockChange(5, 80, 5, "voxel:stone"),
                new ServerWorld.BlockChange(6, 80, 5, "voxel:air"),
                new ServerWorld.BlockChange(7, 80, 5, "voxel:missing_block")
        ));

        assertEquals(Blocks.STONE, target.blockAt(5, 80, 5).orElseThrow().id());
        assertEquals(Blocks.AIR, target.blockAt(6, 80, 5).orElseThrow().id());
        assertEquals(Blocks.AIR, target.blockAt(7, 80, 5).orElseThrow().id());
    }

    @Test
    void computesComfortFromLoadedDecorBlocks() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(0, 80, 0, Blocks.CAMPFIRE_ACTIVE);
        world.setBlock(1, 80, 0, Blocks.WOVEN_RUG);
        world.setBlock(2, 80, 0, Blocks.STORAGE_CRATE);

        assertEquals(9, world.comfortAt(0.5, 80.5, 0.5));
    }

    @Test
    void sleepingMatAdvancesNightToMorning() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(2, 120, 2, Blocks.SLEEPING_MAT);
        world.setBlock(2, 123, 2, Blocks.SKYROOT_PLANKS);
        world.setDayTimeTicks(ServerWorld.NIGHT_START_TICK + 250L);

        assertTrue(world.trySleepAt(2, 120, 2));

        assertEquals(ServerWorld.MORNING_TICK, world.dayTimeTicks() % ServerWorld.DAY_LENGTH_TICKS);
        assertFalse(world.isNight());
    }

    @Test
    void sleepingMatRejectsDaytimeAndWrongBlock() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(2, 120, 2, Blocks.SLEEPING_MAT);
        world.setBlock(2, 123, 2, Blocks.SKYROOT_PLANKS);
        world.setBlock(3, 120, 2, Blocks.STORAGE_CRATE);
        world.setBlock(3, 123, 2, Blocks.SKYROOT_PLANKS);

        assertFalse(world.trySleepAt(2, 120, 2));

        world.setDayTimeTicks(ServerWorld.NIGHT_START_TICK + 250L);

        assertFalse(world.trySleepAt(3, 120, 2));
    }

    @Test
    void sleepingMatRequiresShelter() {
        ServerWorld world = new ServerWorld(123L);
        world.setBlock(2, 120, 2, Blocks.SLEEPING_MAT);
        world.setDayTimeTicks(ServerWorld.NIGHT_START_TICK + 250L);

        assertFalse(world.trySleepAt(2, 120, 2));

        world.setBlock(2, 123, 2, Blocks.SKYROOT_PLANKS);

        assertTrue(world.trySleepAt(2, 120, 2));
    }

    private static LootPos firstGeneratedVillageLoot(long seed) {
        OverworldGenerator.GeneratedStructure structure = new OverworldGenerator(seed)
                .structureAtChunk(new ChunkPos(1, 1))
                .orElseThrow();
        StructureMarker marker = structure.template().lootMarkers().getFirst();
        return new LootPos(
                structure.originX() + marker.x(),
                structure.originY() + marker.y(),
                structure.originZ() + marker.z()
        );
    }

    private static EntitySnapshot ambient(long entityId, String typeKey, double x, double y, double z, String stateKey) {
        return new EntitySnapshot(entityId, typeKey, null, x, y, z, 0.0f, 0.0f, 10, stateKey);
    }

    private record LootPos(int x, int y, int z) {
    }
}
