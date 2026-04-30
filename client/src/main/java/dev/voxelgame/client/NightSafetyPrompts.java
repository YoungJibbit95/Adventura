package dev.voxelgame.client;

import java.util.Optional;

public final class NightSafetyPrompts {
    private static final int DUSK_START_MINUTES = 17 * 60;
    private static final int NIGHT_START_MINUTES = 19 * 60;
    private static final int NIGHT_END_MINUTES = 5 * 60;

    private boolean duskWarningShown;
    private boolean nightWarningShown;
    private boolean campSafetyShown;

    public void reset() {
        duskWarningShown = false;
        nightWarningShown = false;
        campSafetyShown = false;
    }

    public Optional<String> update(int day, int totalMinutes, boolean activeCampfireNearby) {
        if (day != 1) {
            return Optional.empty();
        }
        if (activeCampfireNearby && isDuskOrNight(totalMinutes) && !campSafetyShown) {
            campSafetyShown = true;
            return Optional.of("Campfire glow makes this spot safer tonight.");
        }
        if (!activeCampfireNearby && isDusk(totalMinutes) && !duskWarningShown) {
            duskWarningShown = true;
            return Optional.of("Dusk is coming. Build and light a campfire.");
        }
        if (!activeCampfireNearby && isNight(totalMinutes) && !nightWarningShown) {
            nightWarningShown = true;
            return Optional.of("Night is dark. Stay near a lit campfire.");
        }
        return Optional.empty();
    }

    private static boolean isDuskOrNight(int totalMinutes) {
        return isDusk(totalMinutes) || isNight(totalMinutes);
    }

    private static boolean isDusk(int totalMinutes) {
        return totalMinutes >= DUSK_START_MINUTES && totalMinutes < NIGHT_START_MINUTES;
    }

    private static boolean isNight(int totalMinutes) {
        return totalMinutes >= NIGHT_START_MINUTES || totalMinutes < NIGHT_END_MINUTES;
    }
}
