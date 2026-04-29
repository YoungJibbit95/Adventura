package dev.voxelgame.client;

public final class GameSettings {
    private int renderDistanceChunks;
    private int previewRadiusChunks;
    private int meshBuildBudgetChunks = 2;
    private int fieldOfViewDegrees = 72;
    private int mouseSensitivityPercent = 100;
    private int uiScalePercent = 100;
    private boolean fogEnabled = true;
    private boolean ambientOcclusionEnabled = true;
    private boolean softShadowsEnabled = true;
    private boolean bloomEnabled = true;
    private boolean vsyncEnabled = true;
    private boolean hudEnabled = true;
    private boolean debugOverlayEnabled = false;
    private boolean debugChunkBordersEnabled = false;
    private boolean chatEnabled = true;
    private boolean transparentWaterEnabled = true;

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
        return hudEnabled;
    }

    public boolean debugOverlayEnabled() {
        return debugOverlayEnabled;
    }

    public boolean debugChunkBordersEnabled() {
        return debugChunkBordersEnabled;
    }

    public boolean chatEnabled() {
        return chatEnabled;
    }

    public boolean transparentWaterEnabled() {
        return transparentWaterEnabled;
    }

    public void setRenderDistanceChunks(int value) {
        renderDistanceChunks = clamp(value, 2, 18);
    }

    public void setPreviewRadiusChunks(int value) {
        previewRadiusChunks = clamp(value, 1, 8);
    }

    public void setFieldOfViewDegrees(int value) {
        fieldOfViewDegrees = clamp(value, 55, 100);
    }

    public void setMeshBuildBudgetChunks(int value) {
        meshBuildBudgetChunks = clamp(value, 1, 12);
    }

    public void setUiScalePercent(int value) {
        uiScalePercent = clamp(value, 80, 150);
    }

    public void applyPreset(RenderPreset preset) {
        renderDistanceChunks = clamp(preset.renderDistanceChunks(), 2, 18);
        previewRadiusChunks = clamp(preset.previewRadiusChunks(), 1, 8);
        meshBuildBudgetChunks = clamp(preset.meshBuildBudgetChunks(), 1, 12);
        fogEnabled = preset.fogEnabled();
        ambientOcclusionEnabled = preset.ambientOcclusionEnabled();
        softShadowsEnabled = preset.softShadowsEnabled();
        bloomEnabled = preset.bloomEnabled();
        transparentWaterEnabled = preset.transparentWaterEnabled();
    }

    public void adjustRenderDistance(int delta) {
        renderDistanceChunks = clamp(renderDistanceChunks + delta, 2, 18);
    }

    public void adjustPreviewRadius(int delta) {
        previewRadiusChunks = clamp(previewRadiusChunks + delta, 1, 8);
    }

    public void adjustFieldOfView(int delta) {
        fieldOfViewDegrees = clamp(fieldOfViewDegrees + delta, 55, 100);
    }

    public void adjustMeshBuildBudget(int delta) {
        meshBuildBudgetChunks = clamp(meshBuildBudgetChunks + delta, 1, 12);
    }

    public void adjustMouseSensitivity(int delta) {
        mouseSensitivityPercent = clamp(mouseSensitivityPercent + delta, 40, 180);
    }

    public void adjustUiScale(int delta) {
        setUiScalePercent(uiScalePercent + delta);
    }

    public void toggleFog() {
        fogEnabled = !fogEnabled;
    }

    public void toggleAmbientOcclusion() {
        ambientOcclusionEnabled = !ambientOcclusionEnabled;
    }

    public void toggleSoftShadows() {
        softShadowsEnabled = !softShadowsEnabled;
    }

    public void toggleBloom() {
        bloomEnabled = !bloomEnabled;
    }

    public void toggleVsync() {
        vsyncEnabled = !vsyncEnabled;
    }

    public void toggleHud() {
        hudEnabled = !hudEnabled;
    }

    public void toggleDebugOverlay() {
        debugOverlayEnabled = !debugOverlayEnabled;
    }

    public void toggleDebugChunkBorders() {
        debugChunkBordersEnabled = !debugChunkBordersEnabled;
    }

    public void toggleChat() {
        chatEnabled = !chatEnabled;
    }

    public void toggleTransparentWater() {
        transparentWaterEnabled = !transparentWaterEnabled;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
