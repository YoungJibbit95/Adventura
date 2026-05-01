package dev.voxelgame.client.render;

public record RenderSettings(
        int renderDistanceChunks,
        boolean fogEnabled,
        boolean ambientOcclusionEnabled,
        boolean softShadowsEnabled,
        boolean bloomEnabled,
        boolean underwater,
        boolean simpleWater,
        float bloomStrength,
        float bloomThreshold,
        float fogStart,
        float fogEnd,
        float skyR,
        float skyG,
        float skyB,
        float fogR,
        float fogG,
        float fogB,
        float biomeTintR,
        float biomeTintG,
        float biomeTintB,
        float globalBrightness,
        float nightLightBoost,
        float caveDarkness,
        float weatherFlash,
        RenderDebugView debugView
) {
    public RenderSettings {
        debugView = debugView == null ? RenderDebugView.NONE : debugView;
        bloomStrength = clamp(bloomStrength, 0.0f, 0.45f);
        bloomThreshold = clamp(bloomThreshold, 0.05f, 0.95f);
        globalBrightness = clamp(globalBrightness, 0.35f, 1.15f);
        nightLightBoost = clamp(nightLightBoost, 0.0f, 1.0f);
        caveDarkness = clamp(caveDarkness, 0.0f, 1.0f);
        weatherFlash = clamp(weatherFlash, 0.0f, 1.0f);
    }

    public static RenderSettings defaults(int renderDistanceChunks) {
        float end = Math.max(64.0f, renderDistanceChunks * 16.0f);
        return new RenderSettings(
                renderDistanceChunks,
                true,
                true,
                true,
                true,
                false,
                false,
                0.22f,
                0.18f,
                end * 0.55f,
                end,
                0.52f,
                0.72f,
                0.95f,
                0.58f,
                0.74f,
                0.88f,
                0.56f,
                0.78f,
                0.42f,
                1.0f,
                0.0f,
                0.62f,
                0.0f,
                RenderDebugView.NONE
        );
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
