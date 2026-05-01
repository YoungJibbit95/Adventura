package dev.voxelgame.common.gameplay.status;

import java.util.Objects;

public record StatusEffectDefinition(
        StatusEffectType type,
        double defaultDurationSeconds,
        double maxDurationSeconds,
        double tickIntervalSeconds,
        int maxIntensity,
        StatusEffectStackRule stackRule,
        StatusEffectModifiers modifiers,
        StatusEffectTickEffect tickEffect,
        boolean visibleToClient
) {
    public StatusEffectDefinition {
        Objects.requireNonNull(type, "type");
        defaultDurationSeconds = requirePositiveFinite("defaultDurationSeconds", defaultDurationSeconds);
        maxDurationSeconds = requirePositiveFinite("maxDurationSeconds", maxDurationSeconds);
        if (maxDurationSeconds < defaultDurationSeconds) {
            throw new IllegalArgumentException("maxDurationSeconds must be >= defaultDurationSeconds");
        }
        if (!Double.isFinite(tickIntervalSeconds) || tickIntervalSeconds < 0.0) {
            throw new IllegalArgumentException("tickIntervalSeconds must be finite and >= 0");
        }
        if (maxIntensity <= 0) {
            throw new IllegalArgumentException("maxIntensity must be > 0");
        }
        Objects.requireNonNull(stackRule, "stackRule");
        modifiers = modifiers == null ? StatusEffectModifiers.NEUTRAL : modifiers;
        tickEffect = tickEffect == null ? StatusEffectTickEffect.NONE : tickEffect;
        if (tickEffect.isEmpty() && tickIntervalSeconds > 0.0) {
            throw new IllegalArgumentException("tickIntervalSeconds requires a non-empty tickEffect");
        }
        if (!tickEffect.isEmpty() && tickIntervalSeconds <= 0.0) {
            throw new IllegalArgumentException("tickEffect requires a positive tickIntervalSeconds");
        }
    }

    public ActiveStatusEffect createDefault() {
        return create(defaultDurationSeconds, 1);
    }

    public ActiveStatusEffect create(double durationSeconds, int intensity) {
        double safeDuration = Math.min(maxDurationSeconds, requirePositiveFinite("durationSeconds", durationSeconds));
        int safeIntensity = Math.max(1, Math.min(maxIntensity, intensity));
        return new ActiveStatusEffect(type, safeDuration, safeIntensity, 0.0);
    }

    private static double requirePositiveFinite(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive and finite");
        }
        return value;
    }
}
