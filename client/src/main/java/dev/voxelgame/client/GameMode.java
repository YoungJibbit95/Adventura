package dev.voxelgame.client;

import java.util.Locale;
import java.util.Optional;

public enum GameMode {
    SURVIVAL,
    CREATIVE,
    SPECTATOR;

    public boolean hasCollision() {
        return this != SPECTATOR;
    }

    public boolean usesGravity() {
        return this == SURVIVAL;
    }

    public boolean canFly() {
        return this != SURVIVAL;
    }

    public static Optional<GameMode> parse(String value) {
        try {
            return Optional.of(GameMode.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
