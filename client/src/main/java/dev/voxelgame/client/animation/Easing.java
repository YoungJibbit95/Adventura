package dev.voxelgame.client.animation;

public final class Easing {
    private Easing() {
    }

    public static float clamp01(double value) {
        if (value <= 0.0) {
            return 0.0f;
        }
        if (value >= 1.0) {
            return 1.0f;
        }
        return (float) value;
    }

    public static float linear(double progress) {
        return clamp01(progress);
    }

    public static float smoothStep(double progress) {
        float t = clamp01(progress);
        return t * t * (3.0f - 2.0f * t);
    }

    public static float easeOutCubic(double progress) {
        float t = 1.0f - clamp01(progress);
        return 1.0f - t * t * t;
    }

    public static float pulse(double timeSeconds, double frequencyHz) {
        if (frequencyHz <= 0.0) {
            return 0.0f;
        }
        return 0.5f + 0.5f * (float) Math.sin(timeSeconds * frequencyHz * Math.PI * 2.0);
    }
}
