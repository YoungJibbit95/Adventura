package dev.voxelgame.common.gameplay.status;

import java.util.ArrayList;
import java.util.List;

public final class StatusEffectEnvironmentRules {
    public static final int COZY_COMFORT_THRESHOLD = 4;
    public static final String FROST_PEAKS_BIOME_KEY = "voxel:frost_peaks";

    private StatusEffectEnvironmentRules() {
    }

    public static List<StatusEffectType> effectsFor(EnvironmentContext context) {
        if (context == null) {
            return List.of();
        }
        List<StatusEffectType> effects = new ArrayList<>(4);
        if (context.waterMovementAffected()) {
            effects.add(StatusEffectType.WET);
        }
        if (context.hotHazard()) {
            effects.add(StatusEffectType.BURNING);
        }
        if (context.coldHazard() || isColdBiome(context.biomeKey())) {
            effects.add(StatusEffectType.CHILLED);
        }
        if (context.comfort() >= COZY_COMFORT_THRESHOLD) {
            effects.add(StatusEffectType.COZY);
        }
        return List.copyOf(effects);
    }

    public static boolean isColdBiome(String biomeKey) {
        return FROST_PEAKS_BIOME_KEY.equals(biomeKey);
    }

    public record EnvironmentContext(
            boolean waterMovementAffected,
            boolean hotHazard,
            boolean coldHazard,
            String biomeKey,
            int comfort
    ) {
        public EnvironmentContext {
            biomeKey = biomeKey == null ? "" : biomeKey.strip();
            comfort = Math.max(0, comfort);
        }
    }
}
