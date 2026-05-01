package dev.voxelgame.common.gameplay.status;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusEffectSystemTest {
    @Test
    void defaultDefinitionsCoverAlphaEffectsAndExposeStableSaveKeys() {
        assertEquals(StatusEffectType.values().length, StatusEffectDefinitions.all().size());

        for (StatusEffectType type : StatusEffectType.values()) {
            StatusEffectDefinition definition = StatusEffectDefinitions.require(type);
            assertEquals(type, definition.type());
            assertTrue(type.key().startsWith("voxel:"));
            assertTrue(type.saveKey().startsWith("player.status."));
            assertTrue(definition.defaultDurationSeconds() <= definition.maxDurationSeconds());
            assertTrue(definition.maxIntensity() >= 1 && definition.maxIntensity() <= 3);
            assertTrue(definition.visibleToClient());
        }
    }

    @Test
    void stackRulesRefreshExtendAndIntensifyWithinDefinitionLimits() {
        StatusEffectState state = StatusEffectState.empty()
                .apply(StatusEffectType.WET, 20.0, 1)
                .apply(StatusEffectType.WET, 15.0, 1)
                .apply(StatusEffectType.RESTED, 300.0, 1)
                .apply(StatusEffectType.RESTED, 400.0, 1)
                .apply(StatusEffectType.BURNING, 4.0, 1)
                .apply(StatusEffectType.BURNING, 5.0, 3);

        assertEquals(20.0, state.find(StatusEffectType.WET).orElseThrow().remainingSeconds(), 0.001);
        assertEquals(600.0, state.find(StatusEffectType.RESTED).orElseThrow().remainingSeconds(), 0.001);
        assertEquals(3, state.find(StatusEffectType.BURNING).orElseThrow().intensity());
        assertEquals(5.0, state.find(StatusEffectType.BURNING).orElseThrow().remainingSeconds(), 0.001);
    }

    @Test
    void tickProducesDamagePulsesAndExpiresEffects() {
        StatusEffectState state = StatusEffectState.empty()
                .apply(StatusEffectType.BURNING, 2.5, 2)
                .apply(StatusEffectType.CHILLED, 5.0, 1);

        StatusEffectTickResult first = state.tick(1.0);
        assertEquals(1, first.pulses().size());
        assertEquals(StatusEffectType.BURNING, first.pulses().getFirst().type());
        assertEquals(-2, first.pulses().getFirst().effect().healthDelta());
        assertTrue(first.state().has(StatusEffectType.BURNING));

        StatusEffectTickResult second = first.state().tick(2.0);
        assertEquals(1, second.pulses().size());
        assertTrue(second.state().find(StatusEffectType.BURNING).isEmpty());
        assertTrue(second.state().has(StatusEffectType.CHILLED));
    }

    @Test
    void combinedModifiersRemainPureCommonContract() {
        StatusEffectState state = StatusEffectState.empty()
                .apply(StatusEffectType.CHILLED, 10.0, 1)
                .apply(StatusEffectType.RESTED, 120.0, 1)
                .apply(StatusEffectType.COZY, 90.0, 2);

        StatusEffectModifiers modifiers = state.combinedModifiers();

        assertTrue(modifiers.movementSpeedMultiplier() < 1.0);
        assertTrue(modifiers.jumpMultiplier() < 1.0);
        assertTrue(modifiers.staminaRegenMultiplier() > 1.0);
        assertTrue(modifiers.hungerDrainMultiplier() < 1.0);
        assertTrue(modifiers.healthRegenMultiplier() > 1.0);
    }

    @Test
    void saveStatesRoundTripAndRejectUnknownKeys() {
        StatusEffectState state = StatusEffectState.empty()
                .apply(StatusEffectType.POISON, 8.0, 2)
                .tick(1.0)
                .state();

        List<StatusEffectSaveState> saveStates = state.saveStates();
        StatusEffectState restored = StatusEffectSystem.restore(saveStates);

        assertEquals(state.effects(), restored.effects());
        assertThrows(IllegalArgumentException.class, () -> StatusEffectType.fromKey("voxel:unknown_effect"));
        assertThrows(IllegalArgumentException.class, () -> StatusEffectSystem.restore(List.of(
                new StatusEffectSaveState("voxel:unknown_effect", 1.0, 1, 0.0)
        )));
    }

    @Test
    void facadeKeepsServerAndPhysicsCallSitesSmall() {
        StatusEffectState state = StatusEffectSystem.apply(null, StatusEffectType.CHILLED, 5.0, 1);
        StatusEffectTickResult result = StatusEffectSystem.tick(state, 1.0);

        assertTrue(result.state().has(StatusEffectType.CHILLED));
        assertTrue(StatusEffectSystem.combinedModifiers(result.state()).movementSpeedMultiplier() < 1.0);
    }
}
