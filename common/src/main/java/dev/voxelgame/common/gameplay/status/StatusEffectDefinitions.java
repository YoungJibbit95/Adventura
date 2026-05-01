package dev.voxelgame.common.gameplay.status;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public final class StatusEffectDefinitions {
    private static final Map<StatusEffectType, StatusEffectDefinition> DEFAULTS = defaults();

    private StatusEffectDefinitions() {
    }

    public static StatusEffectDefinition require(StatusEffectType type) {
        StatusEffectDefinition definition = DEFAULTS.get(type);
        if (definition == null) {
            throw new IllegalArgumentException("No status effect definition registered for " + type);
        }
        return definition;
    }

    public static Collection<StatusEffectDefinition> all() {
        return DEFAULTS.values();
    }

    private static Map<StatusEffectType, StatusEffectDefinition> defaults() {
        EnumMap<StatusEffectType, StatusEffectDefinition> definitions = new EnumMap<>(StatusEffectType.class);
        register(definitions, new StatusEffectDefinition(
                StatusEffectType.BURNING,
                6.0,
                12.0,
                1.0,
                3,
                StatusEffectStackRule.INCREASE_INTENSITY_REFRESH_DURATION,
                StatusEffectModifiers.NEUTRAL,
                new StatusEffectTickEffect(-1, 0, 0),
                true
        ));
        register(definitions, new StatusEffectDefinition(
                StatusEffectType.CHILLED,
                18.0,
                45.0,
                0.0,
                2,
                StatusEffectStackRule.EXTEND_DURATION,
                new StatusEffectModifiers(0.82, 0.9, 0.85, 1.0, 1.0),
                StatusEffectTickEffect.NONE,
                true
        ));
        register(definitions, new StatusEffectDefinition(
                StatusEffectType.WET,
                30.0,
                60.0,
                0.0,
                1,
                StatusEffectStackRule.REFRESH_DURATION,
                new StatusEffectModifiers(0.96, 0.98, 0.95, 1.0, 1.0),
                StatusEffectTickEffect.NONE,
                true
        ));
        register(definitions, new StatusEffectDefinition(
                StatusEffectType.RESTED,
                300.0,
                600.0,
                0.0,
                1,
                StatusEffectStackRule.EXTEND_DURATION,
                new StatusEffectModifiers(1.0, 1.0, 1.25, 0.92, 1.1),
                StatusEffectTickEffect.NONE,
                true
        ));
        register(definitions, new StatusEffectDefinition(
                StatusEffectType.COZY,
                120.0,
                300.0,
                0.0,
                2,
                StatusEffectStackRule.REFRESH_DURATION,
                new StatusEffectModifiers(1.0, 1.0, 1.15, 0.95, 1.15),
                StatusEffectTickEffect.NONE,
                true
        ));
        register(definitions, new StatusEffectDefinition(
                StatusEffectType.POISON,
                12.0,
                30.0,
                2.0,
                3,
                StatusEffectStackRule.INCREASE_INTENSITY_REFRESH_DURATION,
                new StatusEffectModifiers(0.95, 1.0, 0.9, 1.0, 0.75),
                new StatusEffectTickEffect(-1, 0, 0),
                true
        ));
        return Map.copyOf(definitions);
    }

    private static void register(EnumMap<StatusEffectType, StatusEffectDefinition> definitions, StatusEffectDefinition definition) {
        StatusEffectDefinition previous = definitions.put(definition.type(), definition);
        if (previous != null) {
            throw new IllegalStateException("Duplicate status effect definition for " + definition.type());
        }
    }
}
