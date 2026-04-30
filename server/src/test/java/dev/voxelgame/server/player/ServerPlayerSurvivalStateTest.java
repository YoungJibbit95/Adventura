package dev.voxelgame.server.player;

import dev.voxelgame.common.net.GamePacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerPlayerSurvivalStateTest {
    @Test
    void tracksComfortScanTickAndSnapshot() {
        ServerPlayerSurvivalState state = new ServerPlayerSurvivalState();

        state.updateComfort(12, 40L);
        GamePacket.PlayerStatsSnapshot snapshot = state.snapshot();

        assertEquals(12, snapshot.comfort());
        assertEquals(40L, state.lastComfortScanTick());
        assertEquals(20, snapshot.health());
        assertEquals(20, snapshot.hunger());
    }

    @Test
    void movementDrainsHungerServerSide() {
        ServerPlayerSurvivalState state = new ServerPlayerSurvivalState();

        state.tick(60.0, 0, 1L, true);

        assertTrue(state.hunger() < 20);
        assertEquals(0, state.comfort());
    }

    @Test
    void breathDrainsOnlyWhenHeadUnderwaterAndRegeneratesAboveWater() {
        ServerPlayerSurvivalState state = new ServerPlayerSurvivalState();

        state.tick(2.0, false, true);
        assertEquals(16, state.breath());

        state.tick(2.0, false, false);
        assertEquals(20, state.breath());
    }

    @Test
    void drowningDamageIsServerAuthoritative() {
        ServerPlayerSurvivalState state = new ServerPlayerSurvivalState();

        state.tick(5.0, false, true);
        state.tick(5.0, false, true);

        assertEquals(19, state.health());
        assertEquals(2, state.breath());
    }

    @Test
    void comfortScanIsThrottledToFortyTicks() {
        ServerPlayerSurvivalState state = new ServerPlayerSurvivalState();

        assertTrue(state.shouldScanComfort(0L));

        state.updateComfort(8, 0L);

        assertFalse(state.shouldScanComfort(39L));
        assertTrue(state.shouldScanComfort(40L));
    }

    @Test
    void comfortReducesHungerDrain() {
        ServerPlayerSurvivalState plain = new ServerPlayerSurvivalState();
        ServerPlayerSurvivalState cozy = new ServerPlayerSurvivalState();

        plain.tick(120.0, 0, 1L, true);
        cozy.tick(120.0, 25, 1L, true);

        assertTrue(cozy.hunger() > plain.hunger());
        assertEquals(25, cozy.comfort());
    }

    @Test
    void fallImpactDamagesOnlyUnsafeFalls() {
        ServerPlayerSurvivalState state = new ServerPlayerSurvivalState();

        assertEquals(0, state.applyFallImpact(3.75, false));
        assertEquals(20, state.health());

        assertEquals(6, state.applyFallImpact(9.5, false));
        assertEquals(14, state.health());
    }

    @Test
    void waterCushionsFallImpact() {
        assertEquals(16, ServerPlayerSurvivalState.fallDamageFor(20.0, false));
        assertEquals(4, ServerPlayerSurvivalState.fallDamageFor(20.0, true));
    }
}
