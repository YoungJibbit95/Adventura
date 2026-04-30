package dev.voxelgame.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void appliesServerSnapshotAndClampsValues() {
        PlayerStats stats = new PlayerStats();

        stats.applySnapshot(30, 19, 18, 17, 21, 50);

        assertEquals(20, stats.health());
        assertEquals(19, stats.hunger());
        assertEquals(18, stats.stamina());
        assertEquals(17, stats.breath());
        assertEquals(20, stats.armor());
        assertEquals(40, stats.comfort());
    }

    @Test
    void foodRestoresHungerHealthAndStamina() {
        PlayerStats stats = new PlayerStats();
        stats.applySnapshot(12, 10, 5, 20, 0, 0);

        assertTrue(stats.canUseFood(5, 4));

        stats.eat(5, 4);

        assertEquals(16, stats.health());
        assertEquals(15, stats.hunger());
        assertTrue(stats.stamina() > 5);
    }

    @Test
    void appliesLocalComfortAndClampsValue() {
        PlayerStats stats = new PlayerStats();

        stats.applyComfort(50);

        assertEquals(40, stats.comfort());

        stats.applyComfort(-5);

        assertEquals(0, stats.comfort());
    }

    @Test
    void deathBlocksActionsUntilRespawn() {
        PlayerStats stats = new PlayerStats();
        stats.applyComfort(30);

        stats.hurt(100);

        assertTrue(stats.dead());
        assertEquals(0, stats.health());

        stats.tick(10.0f, GameMode.SURVIVAL, false, true, true);
        assertEquals(0, stats.health());

        stats.respawn();

        assertEquals(20, stats.health());
        assertEquals(20, stats.hunger());
        assertEquals(20, stats.stamina());
        assertEquals(20, stats.breath());
        assertEquals(0, stats.comfort());
    }
}
