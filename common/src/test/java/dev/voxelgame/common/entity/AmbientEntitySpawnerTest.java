package dev.voxelgame.common.entity;

import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmbientEntitySpawnerTest {
    @Test
    void ambientSpawnsAreDeterministicPerSeedAndChunk() {
        OverworldGenerator generator = new OverworldGenerator(42L);

        List<EntitySnapshot> first = AmbientEntitySpawner.spawnForChunk(42L, generator, new ChunkPos(3, -2));
        List<EntitySnapshot> second = AmbientEntitySpawner.spawnForChunk(42L, generator, new ChunkPos(3, -2));

        assertEquals(first, second);
    }

    @Test
    void spawnAreaIncludesGuaranteedCozyCreatures() {
        List<EntitySnapshot> snapshots = AmbientEntitySpawner.spawnAroundSpawn(42L, 1);

        assertTrue(snapshots.stream().anyMatch(snapshot -> "voxel:cozy_sheep".equals(snapshot.typeKey())));
        assertTrue(snapshots.stream().anyMatch(snapshot -> "voxel:forest_bunny".equals(snapshot.typeKey())));
        assertTrue(snapshots.stream().anyMatch(snapshot -> "voxel:firefly_swarm".equals(snapshot.typeKey())));
    }

    @Test
    void baseAmbientSpawnsStayWithinRegionBudget() {
        OverworldGenerator generator = new OverworldGenerator(42L);
        int baseSpawns = 0;
        for (int z = 0; z < AmbientSpawnRules.REGION_SIZE_CHUNKS; z++) {
            for (int x = 0; x < AmbientSpawnRules.REGION_SIZE_CHUNKS; x++) {
                for (EntitySnapshot snapshot : AmbientEntitySpawner.spawnForChunk(42L, generator, new ChunkPos(x, z), 12 * 60)) {
                    if (!"voxel:firefly_swarm".equals(snapshot.typeKey())) {
                        baseSpawns++;
                    }
                }
            }
        }

        assertTrue(baseSpawns <= AmbientSpawnRules.MAX_BASE_SPAWNS_PER_REGION);
    }
}
