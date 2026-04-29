package dev.voxelgame.common.entity;

import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmbientEntitySpawnerTest {
    @Test
    void ambientSpawnsAreDeterministicPerSeedAndChunk() {
        OverworldGenerator generator = new OverworldGenerator(42L);

        List<EntitySnapshot> first = AmbientEntitySpawner.spawnForChunk(42L, generator, new ChunkPos(3, -2));
        List<EntitySnapshot> second = AmbientEntitySpawner.spawnForChunk(42L, generator, new ChunkPos(3, -2));

        assertEquals(first, second);
    }
}
