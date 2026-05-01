package dev.voxelgame.common.gameplay.status;

public record StatusEffectSaveState(
        String effectKey,
        double remainingSeconds,
        int intensity,
        double tickProgressSeconds
) {
    public StatusEffectSaveState {
        if (effectKey == null || effectKey.isBlank()) {
            throw new IllegalArgumentException("effectKey must not be blank");
        }
        effectKey = effectKey.strip();
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

    public ActiveStatusEffect toActiveEffect() {
        return new ActiveStatusEffect(
                StatusEffectType.fromKey(effectKey),
                remainingSeconds,
                intensity,
                tickProgressSeconds
        );
    }
}
