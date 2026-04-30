package dev.voxelgame.client.render;

import java.util.Locale;

public record LightDebugInfo(
        int x,
        int y,
        int z,
        int skyLight,
        int blockLight,
        float emissive
) {
    public LightDebugInfo {
        validateLight("skyLight", skyLight);
        validateLight("blockLight", blockLight);
        if (!Float.isFinite(emissive) || emissive < 0.0f || emissive > 1.0f) {
            throw new IllegalArgumentException("emissive must be finite and within 0..1");
        }
    }

    public int combinedLight() {
        return Math.max(skyLight, blockLight);
    }

    public String format() {
        return "Light @ " + x + " " + y + " " + z
                + ": combined " + combinedLight()
                + " sky " + skyLight
                + " block " + blockLight
                + " emissive " + formatEmissive();
    }

    private String formatEmissive() {
        return emissive > 0.0f ? String.format(Locale.ROOT, "%.2f", emissive) : "0";
    }

    private static void validateLight(String name, int value) {
        if (value < 0 || value > 15) {
            throw new IllegalArgumentException(name + " must be within 0..15");
        }
    }
}
