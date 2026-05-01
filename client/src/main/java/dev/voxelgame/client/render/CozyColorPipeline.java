package dev.voxelgame.client.render;

import dev.voxelgame.common.world.BiomeType;
import org.joml.Vector3f;

import java.util.Locale;

public final class CozyColorPipeline {
    private static final int MINUTES_PER_DAY = 24 * 60;

    private CozyColorPipeline() {
    }

    public static Vector3f skyColorForMinute(int totalMinutes) {
        int minute = Math.floorMod(totalMinutes, MINUTES_PER_DAY);
        if (minute < 5 * 60) {
            return nightSky();
        }
        if (minute < 7 * 60) {
            return mix(nightSky(), morningSky(), (minute - 5 * 60) / 120.0f);
        }
        if (minute < 12 * 60) {
            return mix(morningSky(), noonSky(), (minute - 7 * 60) / 300.0f);
        }
        if (minute < 17 * 60) {
            return noonSky();
        }
        if (minute < 19 * 60) {
            return mix(noonSky(), eveningSky(), (minute - 17 * 60) / 120.0f);
        }
        return mix(eveningSky(), nightSky(), Math.min(1.0f, (minute - 19 * 60) / 180.0f));
    }

    public static Vector3f fogColorForMinute(int totalMinutes) {
        int minute = Math.floorMod(totalMinutes, MINUTES_PER_DAY);
        if (minute < 5 * 60) {
            return nightFog();
        }
        if (minute < 7 * 60) {
            return mix(nightFog(), morningFog(), (minute - 5 * 60) / 120.0f);
        }
        if (minute < 12 * 60) {
            return mix(morningFog(), noonFog(), (minute - 7 * 60) / 300.0f);
        }
        if (minute < 17 * 60) {
            return noonFog();
        }
        if (minute < 19 * 60) {
            return mix(noonFog(), eveningFog(), (minute - 17 * 60) / 120.0f);
        }
        return mix(eveningFog(), nightFog(), Math.min(1.0f, (minute - 19 * 60) / 180.0f));
    }

    public static float globalBrightnessForMinute(int totalMinutes) {
        int minute = Math.floorMod(totalMinutes, MINUTES_PER_DAY);
        if (minute < 5 * 60) {
            return 0.42f;
        }
        if (minute < 7 * 60) {
            return smooth(0.42f, 0.82f, (minute - 5 * 60) / 120.0f);
        }
        if (minute < 12 * 60) {
            return smooth(0.82f, 1.0f, (minute - 7 * 60) / 300.0f);
        }
        if (minute < 17 * 60) {
            return 1.0f;
        }
        if (minute < 19 * 60) {
            return smooth(1.0f, 0.74f, (minute - 17 * 60) / 120.0f);
        }
        return smooth(0.74f, 0.42f, Math.min(1.0f, (minute - 19 * 60) / 180.0f));
    }

    public static float fogDistanceScaleForMinute(int totalMinutes) {
        int minute = Math.floorMod(totalMinutes, MINUTES_PER_DAY);
        if (minute < 5 * 60) {
            return 0.68f;
        }
        if (minute < 7 * 60) {
            return smooth(0.68f, 0.88f, (minute - 5 * 60) / 120.0f);
        }
        if (minute < 17 * 60) {
            return 1.0f;
        }
        if (minute < 19 * 60) {
            return smooth(1.0f, 0.82f, (minute - 17 * 60) / 120.0f);
        }
        return smooth(0.82f, 0.68f, Math.min(1.0f, (minute - 19 * 60) / 180.0f));
    }

    public static float nightLightBoostForMinute(int totalMinutes) {
        return 1.0f - globalBrightnessForMinute(totalMinutes);
    }

    public static Vector3f biomeTint(String biomeKey, BiomeType biome) {
        String key = biomeKey == null ? "" : biomeKey.toLowerCase(Locale.ROOT);
        if (key.contains("pine") || key.contains("skyroot") || key.contains("forest")) {
            return new Vector3f(0.36f, 0.68f, 0.42f);
        }
        if (key.contains("mushroom")) {
            return new Vector3f(0.58f, 0.66f, 0.54f);
        }
        if (key.contains("mire")) {
            return new Vector3f(0.48f, 0.70f, 0.58f);
        }
        if (key.contains("lake")) {
            return new Vector3f(0.46f, 0.76f, 0.72f);
        }
        if (key.contains("frost")) {
            return new Vector3f(0.76f, 0.88f, 0.96f);
        }
        if (key.contains("ruins")) {
            return new Vector3f(0.62f, 0.68f, 0.48f);
        }
        if (key.contains("dunes")) {
            return new Vector3f(0.78f, 0.72f, 0.44f);
        }
        if (key.contains("meadow") || key.contains("flower")) {
            return new Vector3f(0.58f, 0.80f, 0.42f);
        }
        if (biome == null) {
            return new Vector3f(0.56f, 0.78f, 0.42f);
        }
        float temperature = clamp01(biome.temperature());
        float moisture = clamp01(biome.moisture());
        return new Vector3f(
                clamp(0.48f + temperature * 0.16f - moisture * 0.04f, 0.36f, 0.72f),
                clamp(0.62f + moisture * 0.18f - Math.max(0.0f, temperature - 0.82f) * 0.08f, 0.48f, 0.86f),
                clamp(0.34f + moisture * 0.12f - temperature * 0.03f, 0.30f, 0.64f)
        );
    }

    public static Vector3f mix(Vector3f from, Vector3f to, float amount) {
        return new Vector3f(from).lerp(to, clamp01(amount));
    }

    private static Vector3f morningSky() {
        return new Vector3f(0.82f, 0.54f, 0.35f);
    }

    private static Vector3f noonSky() {
        return new Vector3f(0.52f, 0.72f, 0.95f);
    }

    private static Vector3f eveningSky() {
        return new Vector3f(0.78f, 0.42f, 0.28f);
    }

    private static Vector3f nightSky() {
        return new Vector3f(0.10f, 0.14f, 0.25f);
    }

    private static Vector3f morningFog() {
        return new Vector3f(0.78f, 0.58f, 0.42f);
    }

    private static Vector3f noonFog() {
        return new Vector3f(0.58f, 0.74f, 0.88f);
    }

    private static Vector3f eveningFog() {
        return new Vector3f(0.70f, 0.44f, 0.34f);
    }

    private static Vector3f nightFog() {
        return new Vector3f(0.08f, 0.12f, 0.22f);
    }

    private static float clamp01(float value) {
        return clamp(value, 0.0f, 1.0f);
    }

    private static float smooth(float from, float to, float amount) {
        float t = clamp01(amount);
        t = t * t * (3.0f - 2.0f * t);
        return from + (to - from) * t;
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
