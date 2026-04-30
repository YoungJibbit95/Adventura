package dev.voxelgame.server.save;

public record SaveMetadata(
        int saveVersion,
        String gameVersion,
        int worldVersion,
        long worldSeed,
        long createdAtEpochMillis,
        long lastLoadedAtEpochMillis,
        long dayTimeTicks
) {
    public static final int CURRENT_SAVE_VERSION = 1;
    public static final int CURRENT_WORLD_VERSION = 1;
    public static final String CURRENT_GAME_VERSION = "alpha";

    public SaveMetadata {
        gameVersion = gameVersion == null || gameVersion.isBlank() ? CURRENT_GAME_VERSION : gameVersion;
        createdAtEpochMillis = Math.max(0L, createdAtEpochMillis);
        lastLoadedAtEpochMillis = Math.max(0L, lastLoadedAtEpochMillis);
        dayTimeTicks = Math.max(0L, dayTimeTicks);
    }

    public static SaveMetadata current(long worldSeed, long createdAtEpochMillis, long lastLoadedAtEpochMillis, long dayTimeTicks) {
        return new SaveMetadata(
                CURRENT_SAVE_VERSION,
                CURRENT_GAME_VERSION,
                CURRENT_WORLD_VERSION,
                worldSeed,
                createdAtEpochMillis,
                lastLoadedAtEpochMillis,
                dayTimeTicks
        );
    }
}
