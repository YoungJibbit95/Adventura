package dev.voxelgame.common.gameplay.status;

public record StatusEffectTickEffect(
        int healthDelta,
        int hungerDelta,
        int staminaDelta
) {
    public static final StatusEffectTickEffect NONE = new StatusEffectTickEffect(0, 0, 0);

    public boolean isEmpty() {
        return healthDelta == 0 && hungerDelta == 0 && staminaDelta == 0;
    }

    public StatusEffectTickEffect scaledByIntensity(int intensity) {
        int safeIntensity = Math.max(1, intensity);
        if (safeIntensity == 1) {
            return this;
        }
        return new StatusEffectTickEffect(
                healthDelta * safeIntensity,
                hungerDelta * safeIntensity,
                staminaDelta * safeIntensity
        );
    }
}
