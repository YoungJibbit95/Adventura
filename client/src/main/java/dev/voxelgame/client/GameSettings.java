package dev.voxelgame.client;

import dev.voxelgame.client.render.RenderDebugView;

public final class GameSettings {
    public enum HudMode {
        NORMAL("Normal"),
        MINIMAL("Minimal"),
        HIDDEN("Hidden");

        private final String label;

        HudMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static HudMode parse(String value) {
            if (value == null || value.isBlank()) {
                return NORMAL;
            }
            return switch (value.trim().toLowerCase()) {
                case "normal", "on", "full" -> NORMAL;
                case "minimal", "min" -> MINIMAL;
                case "hidden", "hide", "off" -> HIDDEN;
                default -> throw new IllegalArgumentException("Unknown HUD mode: " + value);
            };
        }
    }

    private int renderDistanceChunks;
    private int previewRadiusChunks;
    private int meshBuildBudgetChunks = 2;
    private double meshBuildBudgetMilliseconds = 3.0;
    private double gpuUploadBudgetMilliseconds = 2.0;
    private int fieldOfViewDegrees = 72;
    private int mouseSensitivityPercent = 100;
    private int uiScalePercent = 100;
    private boolean fogEnabled = true;
    private boolean ambientOcclusionEnabled = true;
    private boolean softShadowsEnabled = true;
    private boolean bloomEnabled = true;
    private boolean vsyncEnabled = true;
    private HudMode hudMode = HudMode.NORMAL;
    private boolean debugOverlayEnabled = false;
    private boolean debugChunkBordersEnabled = false;
    private boolean debugMeshBoundsEnabled = false;
    private boolean debugSectionBoundsEnabled = false;
    private boolean debugParticleBoundsEnabled = false;
    private boolean chatEnabled = true;
    private boolean transparentWaterEnabled = true;
    private boolean simpleWaterEnabled;
    private boolean greedyMeshingEnabled = true;
    private double particleQuality = 1.0;
    private RenderDebugView renderDebugView = RenderDebugView.NONE;
    private RenderPreset activePreset;

    private GameSettings(ConnectionOptions options) {
        this.renderDistanceChunks = clamp(options.renderDistance(), 2, 18);
        this.previewRadiusChunks = clamp(options.previewRadius(), 1, 8);
    }

    public static GameSettings fromOptions(ConnectionOptions options) {
        return new GameSettings(options);
    }

    public int renderDistanceChunks() {
        return renderDistanceChunks;
    }

    public int previewRadiusChunks() {
        return previewRadiusChunks;
    }

    public int fieldOfViewDegrees() {
        return fieldOfViewDegrees;
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

    public int chunkUnloadBudgetChunks() {
        return Math.max(1, meshBuildBudgetChunks * 2);
    }

    public int gpuReleaseBudgetChunks() {
        return Math.max(1, meshBuildBudgetChunks * 2);
    }

    public double effectiveMeshBuildBudgetMilliseconds(double previousFrameMilliseconds) {
        return adaptiveBudget(meshBuildBudgetMilliseconds, previousFrameMilliseconds);
    }

    public double effectiveGpuUploadBudgetMilliseconds(double previousFrameMilliseconds) {
        return adaptiveBudget(gpuUploadBudgetMilliseconds, previousFrameMilliseconds);
    }

    public float mouseSensitivity() {
        return mouseSensitivityPercent / 100.0f;
    }

    public int mouseSensitivityPercent() {
        return mouseSensitivityPercent;
    }

    public float uiScale() {
        return uiScalePercent / 100.0f;
    }

    public int uiScalePercent() {
        return uiScalePercent;
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

    public boolean vsyncEnabled() {
        return vsyncEnabled;
    }

    public boolean hudEnabled() {
        return hudMode != HudMode.HIDDEN;
    }

    public HudMode hudMode() {
        return hudMode;
    }

    public boolean debugOverlayEnabled() {
        return debugOverlayEnabled;
    }

    public boolean debugChunkBordersEnabled() {
        return debugChunkBordersEnabled;
    }

    public boolean debugMeshBoundsEnabled() {
        return debugMeshBoundsEnabled;
    }

    public boolean debugSectionBoundsEnabled() {
        return debugSectionBoundsEnabled;
    }

    public boolean debugParticleBoundsEnabled() {
        return debugParticleBoundsEnabled;
    }

    public boolean chatEnabled() {
        return chatEnabled;
    }

    public boolean transparentWaterEnabled() {
        return transparentWaterEnabled;
    }

    public boolean simpleWaterEnabled() {
        return simpleWaterEnabled;
    }

    public boolean greedyMeshingEnabled() {
        return greedyMeshingEnabled;
    }

    public double particleQuality() {
        return particleQuality;
    }

    public RenderDebugView renderDebugView() {
        return renderDebugView;
    }

    public String activePresetLabel() {
        return activePreset == null ? "Custom" : activePreset.label();
    }

    public void setRenderDistanceChunks(int value) {
        markPresetCustom();
        renderDistanceChunks = clamp(value, 2, 18);
    }

    public void setPreviewRadiusChunks(int value) {
        markPresetCustom();
        previewRadiusChunks = clamp(value, 1, 8);
    }

    public void setFieldOfViewDegrees(int value) {
        fieldOfViewDegrees = clamp(value, 55, 100);
    }

    public void setMeshBuildBudgetChunks(int value) {
        markPresetCustom();
        meshBuildBudgetChunks = clamp(value, 1, 12);
    }

    public void setMeshBuildBudgetMilliseconds(double value) {
        if (!Double.isFinite(value)) {
            return;
        }
        markPresetCustom();
        meshBuildBudgetMilliseconds = clamp(value, 0.5, 16.0);
    }

    public void setGpuUploadBudgetMilliseconds(double value) {
        if (!Double.isFinite(value)) {
            return;
        }
        markPresetCustom();
        gpuUploadBudgetMilliseconds = clamp(value, 0.5, 16.0);
    }

    public void setUiScalePercent(int value) {
        uiScalePercent = clamp(value, 80, 150);
    }

    public void applyPreset(RenderPreset preset) {
        renderDistanceChunks = clamp(preset.renderDistanceChunks(), 2, 18);
        previewRadiusChunks = clamp(preset.previewRadiusChunks(), 1, 8);
        meshBuildBudgetChunks = clamp(preset.meshBuildBudgetChunks(), 1, 12);
        meshBuildBudgetMilliseconds = clamp(preset.meshBuildBudgetMilliseconds(), 0.5, 16.0);
        gpuUploadBudgetMilliseconds = clamp(preset.gpuUploadBudgetMilliseconds(), 0.5, 16.0);
        fogEnabled = preset.fogEnabled();
        ambientOcclusionEnabled = preset.ambientOcclusionEnabled();
        softShadowsEnabled = preset.softShadowsEnabled();
        bloomEnabled = preset.bloomEnabled();
        transparentWaterEnabled = preset.transparentWaterEnabled();
        simpleWaterEnabled = preset.simpleWaterEnabled();
        particleQuality = clamp(preset.particleQuality(), 0.25, 1.0);
        activePreset = preset;
    }

    public void adjustRenderDistance(int delta) {
        markPresetCustom();
        renderDistanceChunks = clamp(renderDistanceChunks + delta, 2, 18);
    }

    public void adjustPreviewRadius(int delta) {
        markPresetCustom();
        previewRadiusChunks = clamp(previewRadiusChunks + delta, 1, 8);
    }

    public void adjustFieldOfView(int delta) {
        fieldOfViewDegrees = clamp(fieldOfViewDegrees + delta, 55, 100);
    }

    public void adjustMeshBuildBudget(int delta) {
        markPresetCustom();
        meshBuildBudgetChunks = clamp(meshBuildBudgetChunks + delta, 1, 12);
    }

    public void adjustMeshBuildBudgetMilliseconds(double delta) {
        setMeshBuildBudgetMilliseconds(meshBuildBudgetMilliseconds + delta);
    }

    public void adjustGpuUploadBudgetMilliseconds(double delta) {
        setGpuUploadBudgetMilliseconds(gpuUploadBudgetMilliseconds + delta);
    }

    public void adjustMouseSensitivity(int delta) {
        mouseSensitivityPercent = clamp(mouseSensitivityPercent + delta, 40, 180);
    }

    public void adjustUiScale(int delta) {
        setUiScalePercent(uiScalePercent + delta);
    }

    public void toggleFog() {
        markPresetCustom();
        fogEnabled = !fogEnabled;
    }

    public void toggleAmbientOcclusion() {
        markPresetCustom();
        ambientOcclusionEnabled = !ambientOcclusionEnabled;
    }

    public void toggleSoftShadows() {
        markPresetCustom();
        softShadowsEnabled = !softShadowsEnabled;
    }

    public void toggleBloom() {
        markPresetCustom();
        bloomEnabled = !bloomEnabled;
    }

    public void toggleVsync() {
        vsyncEnabled = !vsyncEnabled;
    }

    public void toggleHud() {
        hudMode = hudMode == HudMode.HIDDEN ? HudMode.NORMAL : HudMode.HIDDEN;
    }

    public void cycleHudMode() {
        HudMode[] modes = HudMode.values();
        hudMode = modes[(hudMode.ordinal() + 1) % modes.length];
    }

    public void setHudMode(HudMode mode) {
        hudMode = mode == null ? HudMode.NORMAL : mode;
    }

    public void toggleDebugOverlay() {
        debugOverlayEnabled = !debugOverlayEnabled;
    }

    public void toggleDebugChunkBorders() {
        debugChunkBordersEnabled = !debugChunkBordersEnabled;
    }

    public void toggleDebugMeshBounds() {
        debugMeshBoundsEnabled = !debugMeshBoundsEnabled;
    }

    public void toggleDebugSectionBounds() {
        debugSectionBoundsEnabled = !debugSectionBoundsEnabled;
    }

    public void toggleDebugParticleBounds() {
        debugParticleBoundsEnabled = !debugParticleBoundsEnabled;
    }

    public void toggleChat() {
        chatEnabled = !chatEnabled;
    }

    public void toggleTransparentWater() {
        markPresetCustom();
        transparentWaterEnabled = !transparentWaterEnabled;
    }

    public void toggleSimpleWater() {
        markPresetCustom();
        simpleWaterEnabled = !simpleWaterEnabled;
    }

    public void toggleGreedyMeshing() {
        markPresetCustom();
        greedyMeshingEnabled = !greedyMeshingEnabled;
    }

    public void setParticleQuality(double value) {
        if (!Double.isFinite(value)) {
            return;
        }
        markPresetCustom();
        particleQuality = clamp(value, 0.25, 1.0);
    }

    public void adjustParticleQuality(double delta) {
        setParticleQuality(particleQuality + delta);
    }

    public void setRenderDebugView(RenderDebugView view) {
        renderDebugView = view == null ? RenderDebugView.NONE : view;
    }

    public void cycleRenderDebugView() {
        RenderDebugView[] views = RenderDebugView.values();
        renderDebugView = views[(renderDebugView.ordinal() + 1) % views.length];
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double adaptiveBudget(double baseMilliseconds, double previousFrameMilliseconds) {
        if (!Double.isFinite(previousFrameMilliseconds) || previousFrameMilliseconds <= 18.5) {
            return baseMilliseconds;
        }
        double factor;
        if (previousFrameMilliseconds >= 30.0) {
            factor = 0.45;
        } else if (previousFrameMilliseconds >= 24.0) {
            factor = 0.65;
        } else {
            factor = 0.80;
        }
        return clamp(baseMilliseconds * factor, 0.5, baseMilliseconds);
    }

    private void markPresetCustom() {
        activePreset = null;
    }
}
