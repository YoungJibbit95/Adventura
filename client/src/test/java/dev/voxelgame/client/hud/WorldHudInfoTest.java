package dev.voxelgame.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldHudInfoTest {
    @Test
    void labelsDayPhasesForHudReadability() {
        assertEquals(WorldHudInfo.Phase.NIGHT, WorldHudInfo.phaseForMinute(2 * 60));
        assertEquals(WorldHudInfo.Phase.MORNING, WorldHudInfo.phaseForMinute(7 * 60));
        assertEquals(WorldHudInfo.Phase.NOON, WorldHudInfo.phaseForMinute(13 * 60));
        assertEquals(WorldHudInfo.Phase.EVENING, WorldHudInfo.phaseForMinute(18 * 60));
        assertEquals(WorldHudInfo.Phase.NIGHT, WorldHudInfo.phaseForMinute(22 * 60));
    }

    @Test
    void buildsCompactWorldHudLines() {
        WorldHudInfo info = WorldHudInfo.of("voxel:forest", "Forest", "Cold", 3, 6 * 60 + 8);

        assertEquals("Day 3  Morning  06:08", info.timeLine());
        assertEquals("Forest", info.biomeLine());
        assertEquals("Morning  COLD", info.detailLine());
        assertEquals("Entered Forest", info.enteredMessage());
        assertTrue(info.showTemperature());
        assertFalse(info.night());
    }
}
