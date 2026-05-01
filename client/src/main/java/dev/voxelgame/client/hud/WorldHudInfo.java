package dev.voxelgame.client.hud;

import java.util.Locale;

public record WorldHudInfo(
        String biomeKey,
        String biomeLabel,
        String temperatureLabel,
        int day,
        int minuteOfDay,
        Phase phase
) {
    public WorldHudInfo {
        biomeKey = biomeKey == null || biomeKey.isBlank() ? "unknown" : biomeKey;
        biomeLabel = biomeLabel == null || biomeLabel.isBlank() ? "Unknown" : biomeLabel;
        temperatureLabel = temperatureLabel == null || temperatureLabel.isBlank() ? "MILD" : temperatureLabel.toUpperCase(Locale.ROOT);
        day = Math.max(1, day);
        minuteOfDay = Math.floorMod(minuteOfDay, 24 * 60);
        phase = phase == null ? phaseForMinute(minuteOfDay) : phase;
    }

    public static WorldHudInfo of(String biomeKey, String biomeLabel, String temperatureLabel, int day, int minuteOfDay) {
        return new WorldHudInfo(biomeKey, biomeLabel, temperatureLabel, day, minuteOfDay, phaseForMinute(minuteOfDay));
    }

    public String timeLine() {
        return "Day " + day + "  " + phase.label() + "  " + clock();
    }

    public String biomeLine() {
        return biomeLabel;
    }

    public String detailLine() {
        return showTemperature() ? phase.label() + "  " + temperatureLabel : phase.label();
    }

    public String journalHeader() {
        return biomeLabel + "  " + timeLine();
    }

    public String enteredMessage() {
        return "Entered " + biomeLabel;
    }

    public boolean showTemperature() {
        return !temperatureLabel.equals("MILD");
    }

    public boolean night() {
        return phase == Phase.NIGHT;
    }

    private String clock() {
        int hour = minuteOfDay / 60;
        int minute = minuteOfDay % 60;
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    public static Phase phaseForMinute(int minuteOfDay) {
        int minute = Math.floorMod(minuteOfDay, 24 * 60);
        if (minute < 5 * 60) {
            return Phase.NIGHT;
        }
        if (minute < 7 * 60) {
            return Phase.MORNING;
        }
        if (minute < 12 * 60) {
            return Phase.MORNING;
        }
        if (minute < 17 * 60) {
            return Phase.NOON;
        }
        if (minute < 19 * 60) {
            return Phase.EVENING;
        }
        return Phase.NIGHT;
    }

    public enum Phase {
        MORNING("Morning"),
        NOON("Noon"),
        EVENING("Evening"),
        NIGHT("Night");

        private final String label;

        Phase(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
