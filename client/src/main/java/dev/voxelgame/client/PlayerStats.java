package dev.voxelgame.client;

public final class PlayerStats {
    private int health = 20;
    private int hunger = 20;
    private int armor;
    private float breath = 20.0f;

    public int health() {
        return health;
    }

    public int hunger() {
        return hunger;
    }

    public int armor() {
        return armor;
    }

    public int breath() {
        return Math.round(breath);
    }

    public void resetForMode(GameMode mode) {
        if (mode == GameMode.SPECTATOR) {
            health = 20;
            hunger = 20;
            breath = 20.0f;
        }
    }

    public void tick(float deltaSeconds, GameMode mode, boolean underwater) {
        if (mode == GameMode.SPECTATOR) {
            return;
        }
        if (underwater) {
            breath = Math.max(0.0f, breath - deltaSeconds * 2.0f);
        } else {
            breath = Math.min(20.0f, breath + deltaSeconds * 4.0f);
        }
    }
}
