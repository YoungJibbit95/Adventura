package dev.voxelgame.client.world;

import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientWorldSpawnTest {
    @Test
    void spawnEyeUsesGeneratorSafeSpawnPoint() {
        long seed = 123L;
        ClientWorld world = new ClientWorld(seed);
        Vector3f spawn = world.spawnPosition();

        OverworldGenerator generator = new OverworldGenerator(seed);
        OverworldGenerator.SpawnPoint safeSpawn = generator.safeSpawnPoint();

        assertEquals(safeSpawn.eyeX(), spawn.x, 0.001f);
        assertEquals(safeSpawn.eyeY(), spawn.y, 0.001f);
        assertEquals(safeSpawn.eyeZ(), spawn.z, 0.001f);
        assertEquals(safeSpawn.feetY() + PlayerBounds.DEFAULT.eyeHeight(), spawn.y, 0.001f);
    }
}
