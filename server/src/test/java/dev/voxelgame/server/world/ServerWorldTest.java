package dev.voxelgame.server.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.net.GamePacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
