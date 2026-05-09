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
    private int chunkGenerationBudgetChunks = 2;
    private double chunkGenerationBudgetMilliseconds = 1.5;
    private int meshBuildBudgetChunks = 8;
    private double meshBuildBudgetMilliseconds = 10.0;
    private double gpuUploadBudgetMilliseconds = 8.0;
    private int fieldOfViewDegrees = 72;
    private int mouseSensitivityPercent = 100;
    private int uiScalePercent = 100;
    private int masterVolumePercent = 100;
    private int musicVolumePercent = 80;
    private int ambienceVolumePercent = 85;
    private int sfxVolumePercent = 100;
    private int uiVolumePercent = 90;
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

    public int chunkGenerationBudgetChunks() {
        return chunkGenerationBudgetChunks;
    }

    public double chunkGenerationBudgetMilliseconds() {
        return chunkGenerationBudgetMilliseconds;
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

    public int effectiveChunkGenerationBudgetChunks(double previousFrameMilliseconds) {
        if (!Double.isFinite(previousFrameMilliseconds)) {
            return chunkGenerationBudgetChunks;
        }
        if (previousFrameMilliseconds >= 30.0) {
            return 1;
        }
        if (previousFrameMilliseconds >= 22.0) {
            return Math.max(1, chunkGenerationBudgetChunks - 1);
        }
        return chunkGenerationBudgetChunks;
    }

    public double effectiveChunkGenerationBudgetMilliseconds(double previousFrameMilliseconds) {
        return adaptiveBudget(chunkGenerationBudgetMilliseconds, previousFrameMilliseconds);
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

    public int masterVolumePercent() {
        return masterVolumePercent;
    }

    public int musicVolumePercent() {
        return musicVolumePercent;
    }

    public int ambienceVolumePercent() {
        return ambienceVolumePercent;
    }

    public int sfxVolumePercent() {
        return sfxVolumePercent;
    }

    public int uiVolumePercent() {
        return uiVolumePercent;
    }

    public double masterVolume() {
        return volumePercentToGain(masterVolumePercent);
    }

    public double musicVolume() {
        return volumePercentToGain(musicVolumePercent);
    }

    public double ambienceVolume() {
        return volumePercentToGain(ambienceVolumePercent);
    }

    public double sfxVolume() {
        return volumePercentToGain(sfxVolumePercent);
    }

    public double uiVolume() {
        return volumePercentToGain(uiVolumePercent);
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

    public double ambientParticleSourceScanIntervalSeconds() {
        if (particleQuality <= 0.35) {
            return 0.95;
        }
        if (particleQuality <= 0.60) {
            return 0.65;
        }
        if (particleQuality <= 0.85) {
            return 0.50;
        }
        return 0.35;
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

    public void setChunkGenerationBudgetChunks(int value) {
        markPresetCustom();
        chunkGenerationBudgetChunks = clamp(value, 1, 6);
    }

    public void setChunkGenerationBudgetMilliseconds(double value) {
        if (!Double.isFinite(value)) {
            return;
        }
        markPresetCustom();
        chunkGenerationBudgetMilliseconds = clamp(value, 0.5, 6.0);
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

    public void setMasterVolumePercent(int value) {
        masterVolumePercent = clamp(value, 0, 100);
    }

    public void setMusicVolumePercent(int value) {
        musicVolumePercent = clamp(value, 0, 100);
    }

    public void setAmbienceVolumePercent(int value) {
        ambienceVolumePercent = clamp(value, 0, 100);
    }

    public void setSfxVolumePercent(int value) {
        sfxVolumePercent = clamp(value, 0, 100);
    }

    public void setUiVolumePercent(int value) {
        uiVolumePercent = clamp(value, 0, 100);
    }

    public void applyPreset(RenderPreset preset) {
        renderDistanceChunks = clamp(preset.renderDistanceChunks(), 2, 18);
        previewRadiusChunks = clamp(preset.previewRadiusChunks(), 1, 8);
        chunkGenerationBudgetChunks = chunkGenerationChunksForPreset(preset);
        chunkGenerationBudgetMilliseconds = chunkGenerationMillisecondsForPreset(preset);
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

    public void adjustChunkGenerationBudget(int delta) {
        markPresetCustom();
        chunkGenerationBudgetChunks = clamp(chunkGenerationBudgetChunks + delta, 1, 6);
    }

    public void adjustChunkGenerationBudgetMilliseconds(double delta) {
        setChunkGenerationBudgetMilliseconds(chunkGenerationBudgetMilliseconds + delta);
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

    public void adjustMasterVolume(int delta) {
        setMasterVolumePercent(masterVolumePercent + delta);
    }

    public void adjustMusicVolume(int delta) {
        setMusicVolumePercent(musicVolumePercent + delta);
    }

    public void adjustAmbienceVolume(int delta) {
        setAmbienceVolumePercent(ambienceVolumePercent + delta);
    }

    public void adjustSfxVolume(int delta) {
        setSfxVolumePercent(sfxVolumePercent + delta);
    }

    public void adjustUiVolume(int delta) {
        setUiVolumePercent(uiVolumePercent + delta);
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

    private static double volumePercentToGain(int percent) {
        return clamp(percent, 0, 100) / 100.0;
    }

    private static int chunkGenerationChunksForPreset(RenderPreset preset) {
        if (preset == RenderPreset.LOW) {
            return 1;
        }
        if (preset == RenderPreset.HIGH) {
            return 3;
        }
        return 2;
    }

    private static double chunkGenerationMillisecondsForPreset(RenderPreset preset) {
        if (preset == RenderPreset.LOW) {
            return 1.0;
        }
        if (preset == RenderPreset.HIGH) {
            return 2.0;
        }
        return 1.5;
    }

    private void markPresetCustom() {
        activePreset = null;
    }
}
