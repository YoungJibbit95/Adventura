package dev.voxelgame.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStatsTest {
    @Test
    void survivalHungerDrainsAndStarvationHurts() {
        PlayerStats stats = new PlayerStats();

        for (int i = 0; i < 260; i++) {
            stats.tick(1.0f, GameMode.SURVIVAL, false, true, true);
        }

        assertTrue(stats.hunger() < 20);
        assertTrue(stats.health() < 20);
    }
}
