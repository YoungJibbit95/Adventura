package dev.voxelgame.common.gameplay.status;

import java.util.Objects;

public record StatusEffectPulse(
        StatusEffectType type,
        int intensity,
        StatusEffectTickEffect effect
) {
    public StatusEffectPulse {
        Objects.requireNonNull(type, "type");
        if (intensity <= 0) {
            throw new IllegalArgumentException("intensity must be > 0");
        }
        effect = effect == null ? StatusEffectTickEffect.NONE : effect;
    }
}
