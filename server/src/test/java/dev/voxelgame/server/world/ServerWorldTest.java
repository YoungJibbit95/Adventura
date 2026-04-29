package dev.voxelgame.server.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.net.GamePacket;
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

    private record LootPos(int x, int y, int z) {
    }
}
