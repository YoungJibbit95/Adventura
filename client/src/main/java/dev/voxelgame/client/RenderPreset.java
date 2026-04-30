package dev.voxelgame.client;

import java.util.Locale;

public enum RenderPreset {
    LOW("Low", 4, 3, 1, 1.5, 1.0, true, false, false, false, true),
    MEDIUM("Medium", 8, 4, 2, 3.0, 2.0, true, true, false, true, true),
    HIGH("High", 12, 6, 4, 5.0, 4.0, true, true, true, true, true);

    private final String label;
    private final int renderDistanceChunks;
    private final int previewRadiusChunks;
    private final int meshBuildBudgetChunks;
    private final double meshBuildBudgetMilliseconds;
    private final double gpuUploadBudgetMilliseconds;
    private final boolean fogEnabled;
    private final boolean ambientOcclusionEnabled;
    private final boolean softShadowsEnabled;
    private final boolean bloomEnabled;
    private final boolean transparentWaterEnabled;

    RenderPreset(
            String label,
            int renderDistanceChunks,
            int previewRadiusChunks,
            int meshBuildBudgetChunks,
            double meshBuildBudgetMilliseconds,
            double gpuUploadBudgetMilliseconds,
            boolean fogEnabled,
            boolean ambientOcclusionEnabled,
            boolean softShadowsEnabled,
            boolean bloomEnabled,
            boolean transparentWaterEnabled
    ) {
        this.label = label;
        this.renderDistanceChunks = renderDistanceChunks;
        this.previewRadiusChunks = previewRadiusChunks;
        this.meshBuildBudgetChunks = meshBuildBudgetChunks;
        this.meshBuildBudgetMilliseconds = meshBuildBudgetMilliseconds;
        this.gpuUploadBudgetMilliseconds = gpuUploadBudgetMilliseconds;
        this.fogEnabled = fogEnabled;
        this.ambientOcclusionEnabled = ambientOcclusionEnabled;
        this.softShadowsEnabled = softShadowsEnabled;
        this.bloomEnabled = bloomEnabled;
        this.transparentWaterEnabled = transparentWaterEnabled;
    }

    public String label() {
        return label;
    }

    public int renderDistanceChunks() {
        return renderDistanceChunks;
    }

    public int previewRadiusChunks() {
        return previewRadiusChunks;
    }

    public int meshBuildBudgetChunks() {
        return meshBuildBudgetChunks;
    }

    public double meshBuildBudgetMilliseconds() {
        return meshBuildBudgetMilliseconds;
    }

    public double gpuUploadBudgetMilliseconds() {
        return gpuUploadBudgetMilliseconds;
    }

    public boolean fogEnabled() {
        return fogEnabled;
    }

    public boolean ambientOcclusionEnabled() {
        return ambientOcclusionEnabled;
    }

    public boolean softShadowsEnabled() {
        return softShadowsEnabled;
    }

    public boolean bloomEnabled() {
        return bloomEnabled;
    }

    public boolean transparentWaterEnabled() {
        return transparentWaterEnabled;
    }

    public static RenderPreset parse(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "low", "l" -> LOW;
            case "medium", "med", "m" -> MEDIUM;
            case "high", "h" -> HIGH;
            default -> throw new IllegalArgumentException("Usage: /preset low|medium|high");
        };
    }
}
