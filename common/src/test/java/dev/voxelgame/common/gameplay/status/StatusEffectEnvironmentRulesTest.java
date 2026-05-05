package dev.voxelgame.common.gameplay.status;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusEffectEnvironmentRulesTest {
    @Test
    void mapsAuthoritativeEnvironmentContextToStatusEffects() {
        List<StatusEffectType> effects = StatusEffectEnvironmentRules.effectsFor(
                new StatusEffectEnvironmentRules.EnvironmentContext(
                        true,
                        true,
                        false,
                        StatusEffectEnvironmentRules.FROST_PEAKS_BIOME_KEY,
                        StatusEffectEnvironmentRules.COZY_COMFORT_THRESHOLD
                )
        );

        assertEquals(List.of(
                StatusEffectType.WET,
                StatusEffectType.BURNING,
                StatusEffectType.CHILLED,
                StatusEffectType.COZY
        ), effects);
    }

    @Test
    void ignoresBlankAndLowComfortContext() {
        assertTrue(StatusEffectEnvironmentRules.effectsFor(null).isEmpty());
        assertTrue(StatusEffectEnvironmentRules.effectsFor(
                new StatusEffectEnvironmentRules.EnvironmentContext(false, false, false, " ", 3)
        ).isEmpty());
    }
}
