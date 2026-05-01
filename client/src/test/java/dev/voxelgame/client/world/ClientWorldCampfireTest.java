package dev.voxelgame.client.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.net.GamePacket;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientWorldCampfireTest {
    @Test
    void activeCampfiresWithinReturnsNearestActiveCampfiresFirstAndCapsResults() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(8, 64, 8, Blocks.CAMPFIRE_ACTIVE));
        world.applyBlock(new GamePacket.BlockUpdate(3, 64, 3, Blocks.CAMPFIRE_ACTIVE));
        world.applyBlock(new GamePacket.BlockUpdate(5, 64, 5, Blocks.CAMPFIRE));

        List<ClientWorld.BlockPos> campfires = world.activeCampfiresWithin(new Vector3f(2.5f, 64.5f, 2.5f), 12, 1, 0.0);

        assertEquals(List.of(new ClientWorld.BlockPos(3, 64, 3)), campfires);
    }

    @Test
    void nearestBlockWithinFindsCookingPotStation() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(7, 64, 7, Blocks.COOKING_POT));
        world.applyBlock(new GamePacket.BlockUpdate(3, 64, 3, Blocks.COOKING_POT));

        assertEquals(
                new ClientWorld.BlockPos(3, 64, 3),
                world.nearestBlockWithin(new Vector3f(2.5f, 64.5f, 2.5f), Blocks.COOKING_POT, 8).orElseThrow()
        );
    }

    @Test
    void campfireStatusTracksFuelAndCookingProgressFromSnapshot() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(3, 64, 3, Blocks.CAMPFIRE));

        world.applyCampfireStatus(new GamePacket.CampfireStatus(
                3,
                64,
                3,
                true,
                10.0,
                "voxel:cooked_berries",
                4.0,
                4.0
        ), 20.0);

        ClientWorld.CampfireStatusView initial = world.campfireStatusAt(3, 64, 3, 20.0).orElseThrow();
        assertTrue(initial.active());
        assertTrue(initial.cooking());
        assertEquals(10.0, initial.fuelSecondsRemaining(), 0.001);
        assertEquals(0.0f, initial.cookProgress(), 0.001f);

        ClientWorld.CampfireStatusView halfway = world.campfireStatusAt(3, 64, 3, 22.0).orElseThrow();
        assertEquals(8.0, halfway.fuelSecondsRemaining(), 0.001);
        assertEquals(2.0, halfway.cookSecondsRemaining(), 0.001);
        assertEquals(0.5f, halfway.cookProgress(), 0.001f);

        ClientWorld.CampfireStatusView expired = world.campfireStatusAt(3, 64, 3, 31.0).orElseThrow();
        assertFalse(expired.active());
        assertEquals(Blocks.CAMPFIRE_BURNED_OUT, world.blockIdAt(3, 64, 3));
    }

    @Test
    void ambientParticleSourcesReturnNearestLeavesAndSporesWithCaps() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(2, 65, 2, Blocks.SKYROOT_LEAVES));
        world.applyBlock(new GamePacket.BlockUpdate(9, 65, 9, Blocks.PINE_LEAVES));
        world.applyBlock(new GamePacket.BlockUpdate(3, 64, 3, Blocks.GLOW_CRYSTAL_NODE));
        world.applyBlock(new GamePacket.BlockUpdate(4, 64, 4, Blocks.MUSHROOM_CLUSTER));
        world.applyBlock(new GamePacket.BlockUpdate(5, 64, 5, Blocks.SPORE_BLOSSOM));

        ClientWorld.AmbientParticleSources sources = world.ambientParticleSourcesWithin(new Vector3f(2.5f, 64.5f, 2.5f), 10, 1, 3);

        assertEquals(List.of(new ClientWorld.BlockPos(2, 65, 2)), sources.leafSources());
        assertEquals(List.of(
                new ClientWorld.BlockPos(3, 64, 3),
                new ClientWorld.BlockPos(4, 64, 4),
                new ClientWorld.BlockPos(5, 64, 5)
        ), sources.sporeSources());
    }

    @Test
    void comfortAtScansNearbyDecorBlocks() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(3, 64, 3, Blocks.LANTERN));
        world.applyBlock(new GamePacket.BlockUpdate(4, 64, 3, Blocks.WOODEN_CHAIR));

        assertEquals(5, world.comfortAt(new Vector3f(3.5f, 64.5f, 3.5f)));
    }
}
