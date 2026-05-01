package dev.voxelgame.common.gameplay.status;

public record StatusEffectModifiers(
        double movementSpeedMultiplier,
        double jumpMultiplier,
        double staminaRegenMultiplier,
        double hungerDrainMultiplier,
        double healthRegenMultiplier
) {
    public static final StatusEffectModifiers NEUTRAL = new StatusEffectModifiers(1.0, 1.0, 1.0, 1.0, 1.0);

    public StatusEffectModifiers {
        movementSpeedMultiplier = requirePositiveFinite("movementSpeedMultiplier", movementSpeedMultiplier);
        jumpMultiplier = requirePositiveFinite("jumpMultiplier", jumpMultiplier);
        staminaRegenMultiplier = requirePositiveFinite("staminaRegenMultiplier", staminaRegenMultiplier);
        hungerDrainMultiplier = requirePositiveFinite("hungerDrainMultiplier", hungerDrainMultiplier);
        healthRegenMultiplier = requirePositiveFinite("healthRegenMultiplier", healthRegenMultiplier);
    }

    public StatusEffectModifiers multiply(StatusEffectModifiers other) {
        StatusEffectModifiers safeOther = other == null ? NEUTRAL : other;
        return new StatusEffectModifiers(
                movementSpeedMultiplier * safeOther.movementSpeedMultiplier,
                jumpMultiplier * safeOther.jumpMultiplier,
                staminaRegenMultiplier * safeOther.staminaRegenMultiplier,
                hungerDrainMultiplier * safeOther.hungerDrainMultiplier,
                healthRegenMultiplier * safeOther.healthRegenMultiplier
        );
    }

    public StatusEffectModifiers scaledByIntensity(int intensity) {
        int safeIntensity = Math.max(1, intensity);
        if (safeIntensity == 1) {
            return this;
        }
        return new StatusEffectModifiers(
                scaleMultiplier(movementSpeedMultiplier, safeIntensity),
                scaleMultiplier(jumpMultiplier, safeIntensity),
                scaleMultiplier(staminaRegenMultiplier, safeIntensity),
                scaleMultiplier(hungerDrainMultiplier, safeIntensity),
                scaleMultiplier(healthRegenMultiplier, safeIntensity)
        );
    }

    private static double scaleMultiplier(double multiplier, int intensity) {
        return 1.0 + (multiplier - 1.0) * intensity;
    }

    private static double requirePositiveFinite(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive and finite");
        }
        return value;
    }
}
