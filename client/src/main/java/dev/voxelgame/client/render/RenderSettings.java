package dev.voxelgame.client.render;

public record RenderSettings(
        int renderDistanceChunks,
        boolean fogEnabled,
        boolean ambientOcclusionEnabled,
        boolean softShadowsEnabled,
        float fogStart,
        float fogEnd,
        float skyR,
        float skyG,
        float skyB
) {
    public static RenderSettings defaults(int renderDistanceChunks) {
        float end = Math.max(64.0f, renderDistanceChunks * 16.0f);
        return new RenderSettings(renderDistanceChunks, true, true, true, end * 0.55f, end, 0.52f, 0.72f, 0.95f);
    }
}
