package dev.voxelgame.client.world;

import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientWorldSpawnTest {
    @Test
    void spawnEyeStartsOneBlockAboveTerrainSurface() {
        long seed = 123L;
        ClientWorld world = new ClientWorld(seed);
        Vector3f spawn = world.spawnPosition();

        int x = (int) Math.floor(spawn.x);
        int z = (int) Math.floor(spawn.z);
        OverworldGenerator generator = new OverworldGenerator(seed);
        int surfaceY = generator.terrainHeight(x, z, generator.biomeAt(x, z));

        assertEquals(surfaceY + 1 + PlayerBounds.DEFAULT.eyeHeight(), spawn.y, 0.001f);
    }
}
