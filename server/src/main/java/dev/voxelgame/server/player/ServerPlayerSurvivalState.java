package dev.voxelgame.server.player;

import dev.voxelgame.common.gameplay.ComfortRules;
import dev.voxelgame.common.net.GamePacket;

public final class ServerPlayerSurvivalState {
    private static final int MAX_STAT = 20;
    private static final long COMFORT_SCAN_INTERVAL_TICKS = 40L;
    private static final double SAFE_FALL_DISTANCE_BLOCKS = 4.0;
    private static final double WATER_FALL_DAMAGE_MULTIPLIER = 0.25;

    private int health = MAX_STAT;
    private int hunger = MAX_STAT;
    private float stamina = MAX_STAT;
    private float breath = MAX_STAT;
    private int armor;
    private int comfort;
    private float hungerDrain;
    private float starvationTimer;
    private float regenTimer;
    private long lastComfortScanTick = -1L;

    public int health() {
        return health;
    }

    public int hunger() {
        return hunger;
    }

    public int stamina() {
        return Math.round(stamina);
    }

    public int breath() {
        return Math.round(breath);
    }

    public int armor() {
        return armor;
    }

    public int comfort() {
        return comfort;
    }

    public long lastComfortScanTick() {
        return lastComfortScanTick;
    }

    public void updateComfort(int comfort, long tick) {
        this.comfort = clamp(comfort, 0, 40);
        lastComfortScanTick = tick;
    }

    public boolean shouldScanComfort(long tick) {
        return lastComfortScanTick < 0L || tick - lastComfortScanTick >= COMFORT_SCAN_INTERVAL_TICKS;
    }

    public void tick(double deltaSeconds, int comfort, long tick, boolean moving) {
        tick(deltaSeconds, comfort, tick, moving, false);
    }

    public void tick(double deltaSeconds, int comfort, long tick, boolean moving, boolean headUnderwater) {
        updateComfort(comfort, tick);
        tick(deltaSeconds, moving, headUnderwater);
    }

    public void tick(double deltaSeconds, boolean moving) {
        tick(deltaSeconds, moving, false);
    }

    public void tick(double deltaSeconds, boolean moving, boolean headUnderwater) {
        float delta = (float) Math.max(0.0, Math.min(5.0, deltaSeconds));
        if (delta == 0.0f) {
            return;
        }

        float hungerMultiplier = ComfortRules.hungerDrainMultiplier(this.comfort);
        hungerDrain += delta * 0.08f * hungerMultiplier;
        if (moving) {
            hungerDrain += delta * 0.16f * hungerMultiplier;
        }
        while (hungerDrain >= 1.0f) {
            hungerDrain -= 1.0f;
            hunger = Math.max(0, hunger - 1);
        }

        float staminaRegen = (hunger > 4 ? 3.0f : 1.25f) * ComfortRules.staminaRegenMultiplier(this.comfort);
        stamina = Math.min(MAX_STAT, stamina + delta * staminaRegen);
        if (headUnderwater) {
            breath = Math.max(0.0f, breath - delta * 2.0f);
        } else {
            breath = Math.min(MAX_STAT, breath + delta * 4.0f);
        }

        if (hunger <= 0) {
            starvationTimer += delta;
            if (starvationTimer >= 3.0f) {
                starvationTimer = 0.0f;
                health = Math.max(0, health - 1);
            }
        } else {
            starvationTimer = 0.0f;
        }

        if (hunger >= 16 && health < MAX_STAT) {
            regenTimer += delta;
            if (regenTimer >= 4.0f) {
                regenTimer = 0.0f;
                health = Math.min(MAX_STAT, health + 1);
                hungerDrain += 0.35f;
            }
        } else {
            regenTimer = 0.0f;
        }

        if (breath <= 0.0f) {
            health = Math.max(0, health - 1);
            breath = 2.0f;
            regenTimer = 0.0f;
        }
    }

    public int applyFallImpact(double fallDistanceBlocks, boolean waterCushioned) {
        int damage = fallDamageFor(fallDistanceBlocks, waterCushioned);
        if (damage <= 0) {
            return 0;
        }
        health = Math.max(0, health - damage);
        regenTimer = 0.0f;
        return damage;
    }

    public static int fallDamageFor(double fallDistanceBlocks, boolean waterCushioned) {
        if (!Double.isFinite(fallDistanceBlocks) || fallDistanceBlocks <= SAFE_FALL_DISTANCE_BLOCKS) {
            return 0;
        }
        double damage = fallDistanceBlocks - SAFE_FALL_DISTANCE_BLOCKS;
        if (waterCushioned) {
            damage *= WATER_FALL_DAMAGE_MULTIPLIER;
        }
        return Math.max(0, (int) Math.ceil(damage));
    }

    public GamePacket.PlayerStatsSnapshot snapshot() {
        return new GamePacket.PlayerStatsSnapshot(
                health,
                hunger,
                Math.round(stamina),
                Math.round(breath),
                armor,
                comfort
        );
    }

    public void loadPersistentStats(int health, int hunger, int stamina, int breath) {
        this.health = clamp(health, 0, MAX_STAT);
        this.hunger = clamp(hunger, 0, MAX_STAT);
        this.stamina = clamp(stamina, 0, MAX_STAT);
        this.breath = clamp(breath, 0, MAX_STAT);
        armor = 0;
        comfort = 0;
        hungerDrain = 0.0f;
        starvationTimer = 0.0f;
        regenTimer = 0.0f;
        lastComfortScanTick = -1L;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
