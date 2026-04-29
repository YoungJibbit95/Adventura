package dev.voxelgame.client;

import dev.voxelgame.common.gameplay.ComfortRules;

public final class PlayerStats {
    private int health = 20;
    private int hunger = 20;
    private int armor;
    private int comfort;
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

    public int comfort() {
        return comfort;
    }

    public int stamina() {
        return Math.round(stamina);
    }

    public int breath() {
        return Math.round(breath);
    }

    public boolean dead() {
        return health <= 0;
    }

    public void respawn() {
        health = 20;
        hunger = 20;
        armor = 0;
        breath = 20.0f;
        stamina = 20.0f;
        hungerDrain = 0.0f;
        regenTimer = 0.0f;
        starvationTimer = 0.0f;
    }

    public void resetForMode(GameMode mode) {
        if (mode == GameMode.SPECTATOR) {
            respawn();
        }
    }

    public void tick(float deltaSeconds, GameMode mode, boolean underwater) {
        tick(deltaSeconds, mode, underwater, false, false);
    }

    public void tick(float deltaSeconds, GameMode mode, boolean underwater, boolean sprinting, boolean moving) {
        if (mode == GameMode.SPECTATOR || dead()) {
            return;
        }
        if (underwater) {
            breath = Math.max(0.0f, breath - deltaSeconds * 2.0f);
        } else {
            breath = Math.min(20.0f, breath + deltaSeconds * 4.0f);
        }
        if (mode == GameMode.SURVIVAL) {
            hungerDrain += deltaSeconds * 0.08f * comfortHungerMultiplier();
            if (sprinting && moving && stamina > 0.0f) {
                stamina = Math.max(0.0f, stamina - deltaSeconds * 4.5f);
                hungerDrain += deltaSeconds * 0.70f * comfortHungerMultiplier();
            } else {
                float regenRate = (hunger > 4 ? 3.0f : 1.25f) * comfortStaminaMultiplier();
                stamina = Math.min(20.0f, stamina + deltaSeconds * regenRate);
                if (moving) {
                    hungerDrain += deltaSeconds * 0.16f * comfortHungerMultiplier();
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
        return !dead() && stamina > 1.5f && hunger > 0;
    }

    public boolean canUseFood(int foodValue, int healValue) {
        return !dead() && (foodValue > 0 && hunger < 20 || healValue > 0 && health < 20);
    }

    public void eat(int foodValue, int healValue) {
        if (dead()) {
            return;
        }
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

    public void applySnapshot(int health, int hunger, int stamina, int breath, int armor, int comfort) {
        this.health = clamp(health, 0, 20);
        this.hunger = clamp(hunger, 0, 20);
        this.stamina = clamp(stamina, 0, 20);
        this.breath = clamp(breath, 0, 20);
        this.armor = clamp(armor, 0, 20);
        applyComfort(comfort);
    }

    public void applyComfort(int comfort) {
        this.comfort = clamp(comfort, 0, 40);
    }

    private float comfortHungerMultiplier() {
        return ComfortRules.hungerDrainMultiplier(comfort);
    }

    private float comfortStaminaMultiplier() {
        return ComfortRules.staminaRegenMultiplier(comfort);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
