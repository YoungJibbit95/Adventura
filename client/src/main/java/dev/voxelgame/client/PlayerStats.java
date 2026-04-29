package dev.voxelgame.client;

public final class PlayerStats {
    private int health = 20;
    private int hunger = 20;
    private int armor;
    private float breath = 20.0f;
    private float stamina = 20.0f;
    private float hungerDrain;
    private float regenTimer;
    private float starvationTimer;

    public int health() {
        return health;
    }

    public int hunger() {
        return hunger;
    }

    public int armor() {
        return armor;
    }

    public int stamina() {
        return Math.round(stamina);
    }

    public int breath() {
        return Math.round(breath);
    }

    public void resetForMode(GameMode mode) {
        if (mode == GameMode.SPECTATOR) {
            health = 20;
            hunger = 20;
            breath = 20.0f;
            stamina = 20.0f;
        }
    }

    public void tick(float deltaSeconds, GameMode mode, boolean underwater) {
        tick(deltaSeconds, mode, underwater, false, false);
    }

    public void tick(float deltaSeconds, GameMode mode, boolean underwater, boolean sprinting, boolean moving) {
        if (mode == GameMode.SPECTATOR) {
            return;
        }
        if (underwater) {
            breath = Math.max(0.0f, breath - deltaSeconds * 2.0f);
        } else {
            breath = Math.min(20.0f, breath + deltaSeconds * 4.0f);
        }
        if (mode == GameMode.SURVIVAL) {
            hungerDrain += deltaSeconds * 0.08f;
            if (sprinting && moving && stamina > 0.0f) {
                stamina = Math.max(0.0f, stamina - deltaSeconds * 4.5f);
                hungerDrain += deltaSeconds * 0.70f;
            } else {
                float regenRate = hunger > 4 ? 3.0f : 1.25f;
                stamina = Math.min(20.0f, stamina + deltaSeconds * regenRate);
                if (moving) {
                    hungerDrain += deltaSeconds * 0.16f;
                }
            }
            while (hungerDrain >= 1.0f) {
                hungerDrain -= 1.0f;
                hunger = Math.max(0, hunger - 1);
            }
            if (hunger <= 0) {
                starvationTimer += deltaSeconds;
                if (starvationTimer >= 3.0f) {
                    starvationTimer = 0.0f;
                    health = Math.max(0, health - 1);
                }
            } else {
                starvationTimer = 0.0f;
            }
            if (hunger >= 16 && health < 20) {
                regenTimer += deltaSeconds;
                if (regenTimer >= 4.0f) {
                    regenTimer = 0.0f;
                    health = Math.min(20, health + 1);
                    hungerDrain += 0.35f;
                }
            } else {
                regenTimer = 0.0f;
            }
            if (breath <= 0.0f) {
                health = Math.max(0, health - 1);
                breath = 2.0f;
            }
        }
    }

    public boolean canSprint() {
        return stamina > 1.5f && hunger > 0;
    }

    public boolean canUseFood(int foodValue, int healValue) {
        return foodValue > 0 && hunger < 20 || healValue > 0 && health < 20;
    }

    public void eat(int foodValue, int healValue) {
        hunger = Math.min(20, hunger + foodValue);
        health = Math.min(20, health + healValue);
        stamina = Math.min(20.0f, stamina + foodValue * 0.65f);
        starvationTimer = 0.0f;
    }

    public void hurt(int amount) {
        if (amount <= 0) {
            return;
        }
        health = Math.max(0, health - Math.max(1, amount - armor / 5));
    }
}
