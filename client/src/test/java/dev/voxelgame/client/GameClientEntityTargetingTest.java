package dev.voxelgame.client;

import dev.voxelgame.common.entity.EntitySnapshot;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameClientEntityTargetingTest {
    @Test
    void picksNearestEntityAlongViewRay() {
        Optional<EntitySnapshot> target = GameClient.nearestEntityTarget(
                List.of(
                        new EntitySnapshot(1L, "voxel:bunny", null, 0.0, 80.0, 5.0, 0.0f, 0.0f, 10),
                        new EntitySnapshot(2L, "voxel:bunny", null, 0.0, 80.0, 3.0, 0.0f, 0.0f, 10)
                ),
                new Vector3f(0.0f, 80.2f, 0.0f),
                new Vector3f(0.0f, 0.0f, 1.0f),
                6.0
        );

        assertTrue(target.isPresent());
        assertEquals(2L, target.get().entityId());
    }

    @Test
    void ignoresEntitiesOutsideAimCone() {
        Optional<EntitySnapshot> target = GameClient.nearestEntityTarget(
                List.of(new EntitySnapshot(1L, "voxel:bunny", null, 3.0, 80.0, 3.0, 0.0f, 0.0f, 10)),
                new Vector3f(0.0f, 80.2f, 0.0f),
                new Vector3f(0.0f, 0.0f, 1.0f),
                6.0
        );

        assertTrue(target.isEmpty());
    }

    @Test
    void respectsCloserBlockHitDistanceAsMaximum() {
        Optional<EntitySnapshot> target = GameClient.nearestEntityTarget(
                List.of(new EntitySnapshot(1L, "voxel:bunny", null, 0.0, 80.0, 4.0, 0.0f, 0.0f, 10)),
                new Vector3f(0.0f, 80.2f, 0.0f),
                new Vector3f(0.0f, 0.0f, 1.0f),
                2.0
        );

        assertTrue(target.isEmpty());
    }
}
