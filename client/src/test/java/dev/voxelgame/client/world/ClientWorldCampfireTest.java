package dev.voxelgame.client.world;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.net.GamePacket;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void ambientParticleSourcesReturnNearestLeavesAndSporesWithCaps() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(2, 65, 2, Blocks.SKYROOT_LEAVES));
        world.applyBlock(new GamePacket.BlockUpdate(9, 65, 9, Blocks.PINE_LEAVES));
        world.applyBlock(new GamePacket.BlockUpdate(3, 64, 3, Blocks.GLOW_CRYSTAL_NODE));
        world.applyBlock(new GamePacket.BlockUpdate(4, 64, 4, Blocks.MUSHROOM_CLUSTER));

        ClientWorld.AmbientParticleSources sources = world.ambientParticleSourcesWithin(new Vector3f(2.5f, 64.5f, 2.5f), 10, 1, 1);

        assertEquals(List.of(new ClientWorld.BlockPos(2, 65, 2)), sources.leafSources());
        assertEquals(List.of(new ClientWorld.BlockPos(3, 64, 3)), sources.sporeSources());
    }

    @Test
    void comfortAtScansNearbyDecorBlocks() {
        ClientWorld world = new ClientWorld(123L);
        world.applyBlock(new GamePacket.BlockUpdate(3, 64, 3, Blocks.LANTERN));
        world.applyBlock(new GamePacket.BlockUpdate(4, 64, 3, Blocks.WOODEN_CHAIR));

        assertEquals(5, world.comfortAt(new Vector3f(3.5f, 64.5f, 3.5f)));
    }
}
