package dev.voxelgame.server.player;

import dev.voxelgame.common.gameplay.ComfortRules;
import dev.voxelgame.common.gameplay.status.ActiveStatusEffect;
import dev.voxelgame.common.gameplay.status.StatusEffectModifiers;
import dev.voxelgame.common.gameplay.status.StatusEffectPulse;
import dev.voxelgame.common.gameplay.status.StatusEffectSaveState;
import dev.voxelgame.common.gameplay.status.StatusEffectState;
import dev.voxelgame.common.gameplay.status.StatusEffectSystem;
import dev.voxelgame.common.gameplay.status.StatusEffectTickEffect;
import dev.voxelgame.common.gameplay.status.StatusEffectTickResult;
import dev.voxelgame.common.gameplay.status.StatusEffectType;
import dev.voxelgame.common.net.GamePacket;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ServerPlayerSurvivalState {
    private static final int MAX_STAT = 20;
    private static final long COMFORT_SCAN_INTERVAL_TICKS = 40L;
    private static final double SAFE_FALL_DISTANCE_BLOCKS = 4.0;
    private static final double FALL_DAMAGE_SCALE = 0.85;
    private static final double WATER_FALL_DAMAGE_MULTIPLIER = 0.25;
    private static final float SPRINT_STAMINA_DRAIN_PER_SECOND = 4.5f;
    private static final float SPRINT_HUNGER_DRAIN_PER_SECOND = 0.70f;

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
    private StatusEffectState statusEffects = StatusEffectSystem.empty();

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

    public StatusEffectState statusEffects() {
        return statusEffects;
    }

    public boolean hasStatusEffect(StatusEffectType type) {
        return statusEffects.has(type);
    }

    public Set<StatusEffectType> activeStatusTypes() {
        EnumSet<StatusEffectType> active = EnumSet.noneOf(StatusEffectType.class);
        for (ActiveStatusEffect effect : statusEffects.effects()) {
            active.add(effect.type());
        }
        return Set.copyOf(active);
    }

    public List<StatusEffectSaveState> statusEffectSaveStates() {
        return statusEffects.saveStates();
    }

    public StatusEffectChange applyStatusEffect(StatusEffectType type) {
        return applyStatusEffect(type, -1.0, 1);
    }

    public StatusEffectChange applyStatusEffect(StatusEffectType type, double durationSeconds, int intensity) {
        boolean hadEffect = statusEffects.has(type);
        StatusEffectState previous = statusEffects;
        statusEffects = durationSeconds > 0.0
                ? StatusEffectSystem.apply(statusEffects, type, durationSeconds, intensity)
                : StatusEffectSystem.apply(statusEffects, type);
        if (previous.equals(statusEffects)) {
            return StatusEffectChange.UNCHANGED;
        }
        return hadEffect ? StatusEffectChange.REFRESHED : StatusEffectChange.APPLIED;
    }

    public void updateComfort(int comfort, long tick) {
        this.comfort = clamp(comfort, 0, 40);
        lastComfortScanTick = tick;
    }

    public boolean shouldScanComfort(long tick) {
        return lastComfortScanTick < 0L || tick - lastComfortScanTick >= COMFORT_SCAN_INTERVAL_TICKS;
    }

    public void tick(double deltaSeconds, int comfort, long tick, boolean moving) {
        tick(deltaSeconds, comfort, tick, moving, false, false);
    }

    public void tick(double deltaSeconds, int comfort, long tick, boolean moving, boolean headUnderwater) {
        tick(deltaSeconds, comfort, tick, moving, headUnderwater, false);
    }

    public void tick(double deltaSeconds, int comfort, long tick, boolean moving, boolean headUnderwater, boolean sprinting) {
        updateComfort(comfort, tick);
        tick(deltaSeconds, moving, headUnderwater, sprinting);
    }

    public void tick(double deltaSeconds, boolean moving) {
        tick(deltaSeconds, moving, false, false);
    }

    public void tick(double deltaSeconds, boolean moving, boolean headUnderwater) {
        tick(deltaSeconds, moving, headUnderwater, false);
    }

    public void tick(double deltaSeconds, boolean moving, boolean headUnderwater, boolean sprinting) {
        float delta = (float) Math.max(0.0, Math.min(5.0, deltaSeconds));
        if (delta == 0.0f) {
            return;
        }

        StatusEffectTickResult statusTick = StatusEffectSystem.tick(statusEffects, delta);
        statusEffects = statusTick.state();
        applyStatusPulses(statusTick.pulses());

        StatusEffectModifiers statusModifiers = StatusEffectSystem.combinedModifiers(statusEffects);
        float hungerMultiplier = (float) (ComfortRules.hungerDrainMultiplier(this.comfort) * statusModifiers.hungerDrainMultiplier());
        hungerDrain += delta * 0.08f * hungerMultiplier;
        if (moving) {
            hungerDrain += delta * 0.16f * hungerMultiplier;
        }
        if (sprinting && moving && stamina > 0.0f) {
            stamina = Math.max(0.0f, stamina - delta * SPRINT_STAMINA_DRAIN_PER_SECOND);
            hungerDrain += delta * SPRINT_HUNGER_DRAIN_PER_SECOND * hungerMultiplier;
        } else {
            float staminaRegen = (float) ((hunger > 4 ? 3.0f : 1.25f)
                    * ComfortRules.staminaRegenMultiplier(this.comfort)
                    * statusModifiers.staminaRegenMultiplier());
            stamina = Math.min(MAX_STAT, stamina + delta * staminaRegen);
        }
        while (hungerDrain >= 1.0f) {
            hungerDrain -= 1.0f;
            hunger = Math.max(0, hunger - 1);
        }
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
            float regenInterval = (float) (4.0 / statusModifiers.healthRegenMultiplier());
            if (regenTimer >= regenInterval) {
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

    private void applyStatusPulses(List<StatusEffectPulse> pulses) {
        for (StatusEffectPulse pulse : pulses) {
            StatusEffectTickEffect effect = pulse.effect();
            if (effect.healthDelta() != 0) {
                health = clamp(health + effect.healthDelta(), 0, MAX_STAT);
                if (effect.healthDelta() < 0) {
                    regenTimer = 0.0f;
                }
            }
            if (effect.hungerDelta() != 0) {
                hunger = clamp(hunger + effect.hungerDelta(), 0, MAX_STAT);
            }
            if (effect.staminaDelta() != 0) {
                stamina = clamp(Math.round(stamina + effect.staminaDelta()), 0, MAX_STAT);
            }
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

    public int applyEnvironmentalDamage(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int before = health;
        health = Math.max(0, health - amount);
        regenTimer = 0.0f;
        return before - health;
    }

    public static int fallDamageFor(double fallDistanceBlocks, boolean waterCushioned) {
        if (!Double.isFinite(fallDistanceBlocks) || fallDistanceBlocks <= SAFE_FALL_DISTANCE_BLOCKS) {
            return 0;
        }
        double damage = (fallDistanceBlocks - SAFE_FALL_DISTANCE_BLOCKS) * FALL_DAMAGE_SCALE;
        if (waterCushioned) {
            damage *= WATER_FALL_DAMAGE_MULTIPLIER;
        }
        return Math.max(0, (int) Math.ceil(damage));
    }

    public boolean canSprint() {
        return health > 0 && hunger > 0 && stamina > 1.5f;
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
        loadPersistentStats(health, hunger, stamina, breath, List.of());
    }

    public void loadPersistentStats(int health, int hunger, int stamina, int breath, List<StatusEffectSaveState> statusEffectSaveStates) {
        this.health = clamp(health, 0, MAX_STAT);
        this.hunger = clamp(hunger, 0, MAX_STAT);
        this.stamina = clamp(stamina, 0, MAX_STAT);
        this.breath = clamp(breath, 0, MAX_STAT);
        statusEffects = StatusEffectSystem.restore(statusEffectSaveStates);
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

    public enum StatusEffectChange {
        UNCHANGED,
        APPLIED,
        REFRESHED
    }
}
