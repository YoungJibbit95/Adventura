package dev.voxelgame.common.entity;

import dev.voxelgame.common.world.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmbientSpawnRulesTest {
    @Test
    void regionBudgetCapsEligibleChunksDeterministically() {
        int allowed = 0;
        for (int z = 0; z < AmbientSpawnRules.REGION_SIZE_CHUNKS; z++) {
            for (int x = 0; x < AmbientSpawnRules.REGION_SIZE_CHUNKS; x++) {
                if (AmbientSpawnRules.withinRegionBudget(42L, new ChunkPos(x, z), AmbientSpawnRules.MAX_BASE_SPAWNS_PER_REGION, 99L)) {
                    allowed++;
                }
            }
        }

        assertEquals(AmbientSpawnRules.MAX_BASE_SPAWNS_PER_REGION, allowed);
    }

    @Test
    void typeSelectionRespectsDayAndNightWindows() {
        List<String> candidates = List.of("voxel:cozy_sheep", "voxel:firefly_swarm");

        assertEquals("voxel:cozy_sheep", AmbientSpawnRules.selectType(candidates, 42L, new ChunkPos(0, 0), 12 * 60).orElseThrow());
        assertEquals("voxel:firefly_swarm", AmbientSpawnRules.selectType(candidates, 42L, new ChunkPos(0, 0), 23 * 60).orElseThrow());
    }

    @Test
    void cooldownsArePositiveForRuntimeBudgeting() {
        assertTrue(AmbientSpawnRules.spawnCooldownSeconds("voxel:cozy_sheep") > 0.0);
        assertTrue(AmbientSpawnRules.spawnCooldownSeconds("voxel:firefly_swarm") > AmbientSpawnRules.spawnCooldownSeconds("voxel:cozy_sheep"));
    }
}
