package dev.voxelgame.client.world;

import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void spawnPreviewCanGenerateInSmallBatches() {
        ClientWorld world = new ClientWorld(123L);

        world.generatePreview(2, 4);

        assertEquals(4, world.loadedChunkCount());

        while (world.loadedChunkCount() < 25) {
            int before = world.loadedChunkCount();
            world.generatePreview(2, 4);
            assertTrue(world.loadedChunkCount() > before);
        }
        assertEquals(25, world.loadedChunkCount());
    }
}
