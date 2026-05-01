package dev.voxelgame.common.gameplay.status;

import java.util.Objects;

public record ActiveStatusEffect(
        StatusEffectType type,
        double remainingSeconds,
        int intensity,
        double tickProgressSeconds
) {
    public ActiveStatusEffect {
        Objects.requireNonNull(type, "type");
        if (!Double.isFinite(remainingSeconds) || remainingSeconds <= 0.0) {
            throw new IllegalArgumentException("remainingSeconds must be positive and finite");
        }
        if (intensity <= 0) {
            throw new IllegalArgumentException("intensity must be > 0");
        }
        if (!Double.isFinite(tickProgressSeconds) || tickProgressSeconds < 0.0) {
            throw new IllegalArgumentException("tickProgressSeconds must be finite and >= 0");
        }
    }

    public StatusEffectSaveState toSaveState() {
        return new StatusEffectSaveState(type.key(), remainingSeconds, intensity, tickProgressSeconds);
    }
}
