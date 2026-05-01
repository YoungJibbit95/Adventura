package dev.voxelgame.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameServerMainTest {
    @Test
    void autosaveIntervalDefaultsToOneMinute() {
        assertEquals(60L * TickLoop.TPS, GameServerMain.autosaveIntervalTicks(new String[0]));
    }

    @Test
    void autosaveIntervalCanBeConfiguredOrDisabled() {
        assertEquals(5L * TickLoop.TPS, GameServerMain.autosaveIntervalTicks(new String[]{"--autosave-seconds", "5"}));
        assertEquals(0L, GameServerMain.autosaveIntervalTicks(new String[]{"--autosave-seconds", "0"}));
    }
}
