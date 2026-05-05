package dev.voxelgame.server.player;

import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.gameplay.status.StatusEffectSaveState;
import dev.voxelgame.common.gameplay.status.StatusEffectType;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void sprintingDrainsServerStaminaAndExtraHunger() {
        ServerPlayerSurvivalState walking = new ServerPlayerSurvivalState();
        ServerPlayerSurvivalState sprinting = new ServerPlayerSurvivalState();

        walking.tick(3.0, true, false, false);
        sprinting.tick(3.0, true, false, true);

        assertTrue(sprinting.stamina() < walking.stamina());
        assertTrue(sprinting.hunger() < walking.hunger());
        assertTrue(sprinting.canSprint());
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
    void statusEffectsApplyServerSidePulsesAndModifiers() {
        ServerPlayerSurvivalState burning = new ServerPlayerSurvivalState();
        ServerPlayerSurvivalState plain = new ServerPlayerSurvivalState();
        ServerPlayerSurvivalState chilled = new ServerPlayerSurvivalState();

        assertEquals(ServerPlayerSurvivalState.StatusEffectChange.APPLIED, burning.applyStatusEffect(StatusEffectType.BURNING, 2.5, 2));
        burning.tick(1.0, false);
        assertEquals(18, burning.health());

        plain.tick(3.0, true, false, true);
        chilled.applyStatusEffect(StatusEffectType.CHILLED, 10.0, 1);
        chilled.tick(3.0, true, false, true);
        plain.tick(1.0, false);
        chilled.tick(1.0, false);
        assertTrue(chilled.stamina() < plain.stamina(), "chilled should slow stamina recovery under status control");
    }

    @Test
    void statusEffectsRoundTripThroughPersistentState() {
        ServerPlayerSurvivalState state = new ServerPlayerSurvivalState();
        state.applyStatusEffect(StatusEffectType.RESTED, 90.0, 1);

        List<StatusEffectSaveState> saveStates = state.statusEffectSaveStates();
        ServerPlayerSurvivalState restored = new ServerPlayerSurvivalState();
        restored.loadPersistentStats(12, 11, 10, 9, saveStates);

        assertEquals(12, restored.health());
        assertEquals(11, restored.hunger());
        assertTrue(restored.hasStatusEffect(StatusEffectType.RESTED));
        assertEquals(saveStates, restored.statusEffectSaveStates());
    }

    @Test
    void fallImpactDamagesOnlyUnsafeFalls() {
        ServerPlayerSurvivalState state = new ServerPlayerSurvivalState();

        assertEquals(0, state.applyFallImpact(3.75, false));
        assertEquals(20, state.health());

        assertEquals(5, state.applyFallImpact(9.5, false));
        assertEquals(15, state.health());
    }

    @Test
    void waterCushionsFallImpact() {
        assertEquals(14, ServerPlayerSurvivalState.fallDamageFor(20.0, false));
        assertEquals(4, ServerPlayerSurvivalState.fallDamageFor(20.0, true));
    }
}
