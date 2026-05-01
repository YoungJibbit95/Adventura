package dev.voxelgame.client.hud;

import dev.voxelgame.client.ui.UiColor;

public record HudStat(
        String label,
        String fullKey,
        String halfKey,
        String emptyKey,
        int value,
        int max,
        UiColor accent,
        float iconScale,
        boolean warning,
        boolean boosted,
        String stateLabel
) {
    public HudStat(String label, String fullKey, String halfKey, String emptyKey, int value, int max, UiColor accent, float iconScale) {
        this(label, fullKey, halfKey, emptyKey, value, max, accent, iconScale, false, false, "");
    }

    public HudStat {
        label = label == null ? "" : label;
        fullKey = fullKey == null ? "" : fullKey;
        halfKey = halfKey == null ? "" : halfKey;
        emptyKey = emptyKey == null ? "" : emptyKey;
        max = Math.max(1, max);
        value = Math.max(0, Math.min(max, value));
        iconScale = Float.isFinite(iconScale) && iconScale > 0.0f ? iconScale : 1.0f;
        if (accent == null) {
            throw new IllegalArgumentException("HUD stat accent is required");
        }
        stateLabel = stateLabel == null ? "" : stateLabel;
    }
}
