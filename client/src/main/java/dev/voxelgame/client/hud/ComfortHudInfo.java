package dev.voxelgame.client.hud;

import dev.voxelgame.common.gameplay.ComfortRules;

import java.util.List;
import java.util.Locale;

public record ComfortHudInfo(
        int value,
        Level level,
        List<Source> sources
) {
    private static final int MAX_COMFORT = 40;

    public ComfortHudInfo {
        value = Math.max(0, Math.min(MAX_COMFORT, value));
        level = level == null ? levelFor(value) : level;
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    public static ComfortHudInfo none() {
        return new ComfortHudInfo(0, Level.NONE, List.of());
    }

    public static ComfortHudInfo of(int value, List<Source> sources) {
        return new ComfortHudInfo(value, levelFor(value), sources);
    }

    public boolean active() {
        return value > 0;
    }

    public float ratio() {
        return value / (float) MAX_COMFORT;
    }

    public String statLabel() {
        return level == Level.NONE ? "" : level.label().toUpperCase(Locale.ROOT) + " +" + value;
    }

    public String detailLine() {
        if (!active()) {
            return "No comfort sources nearby";
        }
        String sourceText = sources.isEmpty()
                ? "Nearby cozy objects"
                : sources.stream()
                .limit(3)
                .map(Source::summary)
                .reduce((left, right) -> left + "  " + right)
                .orElse("Nearby cozy objects");
        return level.label() + ": " + effectLine() + "  " + sourceText;
    }

    public String feedbackLine() {
        return "Comfort level: " + level.label();
    }

    private String effectLine() {
        int staminaPercent = Math.round((ComfortRules.staminaRegenMultiplier(value) - 1.0f) * 100.0f);
        int hungerPercent = Math.round((1.0f - ComfortRules.hungerDrainMultiplier(value)) * 100.0f);
        if (staminaPercent <= 0 && hungerPercent <= 0) {
            return "resting bonus";
        }
        return "stamina +" + staminaPercent + "% hunger -" + hungerPercent + "%";
    }

    public static Level levelFor(int comfort) {
        if (comfort >= 16) {
            return Level.HOMEY;
        }
        if (comfort >= 12) {
            return Level.RESTFUL;
        }
        if (comfort >= 8) {
            return Level.WARM;
        }
        if (comfort >= 5) {
            return Level.COZY;
        }
        if (comfort > 0) {
            return Level.LOW;
        }
        return Level.NONE;
    }

    public enum Level {
        NONE("None"),
        LOW("Low"),
        COZY("Cozy"),
        WARM("Warm"),
        RESTFUL("Restful"),
        HOMEY("Homey");

        private final String label;

        Level(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record Source(String label, int value) {
        public Source {
            label = label == null || label.isBlank() ? "Comfort" : label;
            value = Math.max(0, value);
        }

        String summary() {
            if (value <= 0) {
                return label;
            }
            return label + " +" + value;
        }
    }
}
