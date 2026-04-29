package dev.voxelgame.common.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityBoundsTest {
    @Test
    void playerBaseUsesEyeHeight() {
        EntitySnapshot player = new EntitySnapshot(1L, "voxel:player", UUID.randomUUID(), 0.0, 100.0, 0.0, 0.0f, 0.0f, 20);

        assertEquals(98.38f, EntityBounds.baseY(player), 0.001f);
        assertTrue(EntityBounds.forType("voxel:player").height() > 1.7f);
    }
}
