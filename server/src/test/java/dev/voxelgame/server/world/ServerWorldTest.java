package dev.voxelgame.server.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.item.Inventory;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.registry.Registry;
import org.junit.jupiter.api.Test;

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
}
