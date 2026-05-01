package dev.voxelgame.client.render;

import dev.voxelgame.common.world.BiomeType;
import org.joml.Vector3f;

import java.util.Locale;

public final class WeatherLightningController {
    private static final double FLASH_DURATION_SECONDS = 0.72;
    private static final double THUNDER_DELAY_SECONDS = 0.48;
    private static final double MIN_INTERVAL_SECONDS = 85.0;
    private static final double MAX_INTERVAL_SECONDS = 210.0;
    private static final Vector3f FLASH_SKY = new Vector3f(0.84f, 0.90f, 1.0f);
    private static final Vector3f FLASH_FOG = new Vector3f(0.74f, 0.82f, 0.95f);

    private double activeStartSeconds = Double.NEGATIVE_INFINITY;
    private double nextStrikeSeconds = Double.NaN;
    private float activeStrength;
    private boolean thunderPending;
    private long scheduleCounter;

    public WeatherLightningSample update(double nowSeconds, long worldSeed, int dayMinute, String biomeKey, BiomeType biome) {
        if (!Double.isFinite(nowSeconds)) {
            return new WeatherLightningSample(0.0f, false);
        }
        if (stormEligible(dayMinute, biomeKey, biome)) {
            if (!Double.isFinite(nextStrikeSeconds)) {
                nextStrikeSeconds = nowSeconds + nextIntervalSeconds(worldSeed, biomeKey, scheduleCounter++);
            }
            if (nowSeconds >= nextStrikeSeconds) {
                trigger(nowSeconds, scheduledStrength(worldSeed, biomeKey, scheduleCounter));
                nextStrikeSeconds = nowSeconds + nextIntervalSeconds(worldSeed, biomeKey, scheduleCounter++);
            }
        } else {
            nextStrikeSeconds = Double.NaN;
        }
        boolean thunderCue = thunderPending && nowSeconds - activeStartSeconds >= THUNDER_DELAY_SECONDS;
        if (thunderCue) {
            thunderPending = false;
        }
        return new WeatherLightningSample(currentFlash(nowSeconds), thunderCue);
    }

    public void trigger(double nowSeconds) {
        trigger(nowSeconds, 1.0f);
    }

    public void reset() {
        activeStartSeconds = Double.NEGATIVE_INFINITY;
        nextStrikeSeconds = Double.NaN;
        activeStrength = 0.0f;
        thunderPending = false;
        scheduleCounter = 0L;
    }

    public float currentFlash(double nowSeconds) {
        if (!Double.isFinite(nowSeconds)) {
            return 0.0f;
        }
        double age = nowSeconds - activeStartSeconds;
        if (age < 0.0 || age > FLASH_DURATION_SECONDS) {
            return 0.0f;
        }
        float pulse;
        if (age < 0.055) {
            pulse = 1.0f;
        } else if (age < 0.15) {
            pulse = 0.26f;
        } else if (age < 0.28) {
            pulse = 0.68f;
        } else {
            float t = (float) ((age - 0.28) / (FLASH_DURATION_SECONDS - 0.28));
            pulse = (1.0f - t) * (1.0f - t);
        }
        return clamp01(activeStrength * pulse);
    }

    public double nextStrikeSeconds() {
        return nextStrikeSeconds;
    }

    public static boolean stormEligible(int dayMinute, String biomeKey, BiomeType biome) {
        int minute = Math.floorMod(dayMinute, 24 * 60);
        boolean nightOrEdge = minute < 7 * 60 || minute >= 17 * 60;
        String key = biomeKey == null ? "" : biomeKey.toLowerCase(Locale.ROOT);
        float moisture = biome == null ? inferredMoisture(key) : biome.moisture();
        boolean stormBiome = moisture >= 0.78f || key.contains("mire") || key.contains("lake") || key.contains("highlands");
        return stormBiome && nightOrEdge;
    }

    public static Vector3f applySkyFlash(Vector3f sky, float flash) {
        return new Vector3f(sky).lerp(FLASH_SKY, clamp01(flash) * 0.70f);
    }

    public static Vector3f applyFogFlash(Vector3f fog, float flash) {
        return new Vector3f(fog).lerp(FLASH_FOG, clamp01(flash) * 0.52f);
    }

    private void trigger(double nowSeconds, float strength) {
        if (!Double.isFinite(nowSeconds)) {
            return;
        }
        activeStartSeconds = nowSeconds;
        activeStrength = clamp01(strength);
        thunderPending = true;
    }

    private static float inferredMoisture(String biomeKey) {
        if (biomeKey.contains("mire") || biomeKey.contains("lake")) {
            return 0.95f;
        }
        if (biomeKey.contains("forest") || biomeKey.contains("frost")) {
            return 0.76f;
        }
        if (biomeKey.contains("highlands")) {
            return 0.72f;
        }
        return 0.35f;
    }

    private static double nextIntervalSeconds(long seed, String biomeKey, long counter) {
        long hash = mix(seed ^ ((long) safeKey(biomeKey).hashCode() << 32) ^ counter * 0x9E3779B97F4A7C15L);
        double unit = (hash >>> 11) * 0x1.0p-53;
        return MIN_INTERVAL_SECONDS + unit * (MAX_INTERVAL_SECONDS - MIN_INTERVAL_SECONDS);
    }

    private static float scheduledStrength(long seed, String biomeKey, long counter) {
        long hash = mix(seed + safeKey(biomeKey).hashCode() * 31L + counter * 17L);
        double unit = (hash >>> 11) * 0x1.0p-53;
        return (float) (0.62 + unit * 0.30);
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private static String safeKey(String biomeKey) {
        return biomeKey == null ? "" : biomeKey;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public record WeatherLightningSample(float flash, boolean thunderCue) {
        public WeatherLightningSample {
            flash = clamp01(flash);
        }
    }
}
