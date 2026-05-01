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
        RenderDebugView debugView
) {
    public RenderSettings {
        debugView = debugView == null ? RenderDebugView.NONE : debugView;
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
                RenderDebugView.NONE
        );
    }
}
