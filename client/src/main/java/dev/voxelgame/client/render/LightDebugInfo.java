package dev.voxelgame.client.render;

import java.util.Locale;
import java.util.Objects;

public record LightDebugInfo(
        int x,
        int y,
        int z,
        int skyLight,
        int blockLight,
        float emissive,
        String blockKey,
        int lightSource,
        String occlusionType
) {
    public LightDebugInfo {
        validateLight("skyLight", skyLight);
        validateLight("blockLight", blockLight);
        validateLight("lightSource", lightSource);
        if (!Float.isFinite(emissive) || emissive < 0.0f || emissive > 1.0f) {
            throw new IllegalArgumentException("emissive must be finite and within 0..1");
        }
        blockKey = requireText(blockKey, "blockKey");
        occlusionType = requireText(occlusionType, "occlusionType");
    }

    public LightDebugInfo(int x, int y, int z, int skyLight, int blockLight, float emissive) {
        this(x, y, z, skyLight, blockLight, emissive, "voxel:unknown", 0, "unknown");
    }

    public int combinedLight() {
        return Math.max(skyLight, blockLight);
    }

    public String format() {
        return "Light @ " + x + " " + y + " " + z
                + " " + blockKey
                + ": combined " + combinedLight()
                + " sky " + skyLight
                + " block " + blockLight
                + " source " + lightSource
                + " emissive " + formatEmissive()
                + " occlusion " + occlusionType;
    }

    private String formatEmissive() {
        return emissive > 0.0f ? String.format(Locale.ROOT, "%.2f", emissive) : "0";
    }

    private static void validateLight(String name, int value) {
        if (value < 0 || value > 15) {
            throw new IllegalArgumentException(name + " must be within 0..15");
        }
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name).trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}
