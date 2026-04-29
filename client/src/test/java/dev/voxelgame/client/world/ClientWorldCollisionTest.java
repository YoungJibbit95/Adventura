package dev.voxelgame.client.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.math.Raycast;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.physics.PlayerWaterState;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientWorldCollisionTest {
    @Test
    void unloadedChunksBlockPlayerCollisionQueries() {
        ClientWorld world = new ClientWorld(123L);

        assertTrue(world.collidesPlayer(8.5, 80.0, 8.5));
    }

    @Test
    void loadedAirDoesNotBlockPlayerCollisionQueries() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 80, 8, Blocks.AIR));

        assertFalse(world.collidesPlayer(8.5, 80.0, 8.5));
    }

    @Test
    void localPlacementCannotIntersectPlayerBounds() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 64, 8, Blocks.AIR));
        Raycast.Hit hit = new Raycast.Hit(8, 63, 8, 0, 1, 0, 1.0);

        assertFalse(world.placeBlock(hit, Blocks.STONE, new Vector3f(8.5f, 65.62f, 8.5f)));
    }

    @Test
    void localPlacementOutsidePlayerBoundsStillWorks() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(10, 64, 8, Blocks.AIR));
        Raycast.Hit hit = new Raycast.Hit(10, 63, 8, 0, 1, 0, 1.0);

        assertTrue(world.placeBlock(hit, Blocks.STONE, new Vector3f(8.5f, 65.62f, 8.5f)));
    }

    @Test
    void waterStateSeparatesFeetBodyAndHead() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 64, 8, Blocks.WATER));
        world.applyBlock(new GamePacket.BlockUpdate(8, 65, 8, Blocks.AIR));
        world.applyBlock(new GamePacket.BlockUpdate(8, 66, 8, Blocks.AIR));

        PlayerWaterState feetOnly = world.playerWaterState(new Vector3f(8.5f, 65.62f, 8.5f));
        assertTrue(feetOnly.feetInWater());
        assertFalse(feetOnly.bodyInWater());
        assertFalse(feetOnly.headUnderwater());
        assertTrue(feetOnly.movementAffected());

        world.applyBlock(new GamePacket.BlockUpdate(8, 65, 8, Blocks.WATER));
        PlayerWaterState bodyWater = world.playerWaterState(new Vector3f(8.5f, 65.62f, 8.5f));
        assertTrue(bodyWater.bodyInWater());
        assertFalse(bodyWater.headUnderwater());

        world.applyBlock(new GamePacket.BlockUpdate(8, 66, 8, Blocks.WATER));
        PlayerWaterState submerged = world.playerWaterState(new Vector3f(8.5f, 66.05f, 8.5f));
        assertTrue(submerged.headUnderwater());
    }
}
