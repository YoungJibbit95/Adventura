package dev.voxelgame.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameModeTest {
    @Test
    void parsesAndDescribesMovementRules() {
        assertEquals(GameMode.CREATIVE, GameMode.parse("creative").orElseThrow());
        assertTrue(GameMode.SURVIVAL.usesGravity());
        assertTrue(GameMode.CREATIVE.canFly());
        assertFalse(GameMode.SPECTATOR.hasCollision());
    }
}
