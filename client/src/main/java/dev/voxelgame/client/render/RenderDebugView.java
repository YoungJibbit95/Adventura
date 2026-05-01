package dev.voxelgame.client.render;

import java.util.Locale;

public enum RenderDebugView {
    NONE(0, "off"),
    MATERIAL_INDEX(1, "material"),
    LIGHT(2, "light"),
    AO(3, "ao"),
    BIOME_TINT(4, "biome"),
    RENDER_LAYER(5, "layer"),
    UV_ATLAS(6, "uv"),
    TRANSPARENT(7, "transparent"),
    SKY_LIGHT(8, "sky"),
    BLOCK_LIGHT(9, "block"),
    EMISSIVE(10, "emissive");

    private final int shaderId;
    private final String commandName;

    RenderDebugView(int shaderId, String commandName) {
        this.shaderId = shaderId;
        this.commandName = commandName;
    }

    public int shaderId() {
        return shaderId;
    }

    public String commandName() {
        return commandName;
    }

    public static RenderDebugView parse(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "off", "none", "0" -> NONE;
            case "material", "materials", "mat" -> MATERIAL_INDEX;
            case "light", "lighting", "l" -> LIGHT;
            case "ao", "ambient" -> AO;
            case "biome", "biometint", "biome_tint", "tint" -> BIOME_TINT;
            case "layer", "renderlayer", "render_layer", "layers" -> RENDER_LAYER;
            case "uv", "atlas", "uvatlas", "uv_atlas" -> UV_ATLAS;
            case "transparent", "translucent", "overdraw", "alpha" -> TRANSPARENT;
            case "sky", "skylight", "sky_light" -> SKY_LIGHT;
            case "block", "blocklight", "block_light" -> BLOCK_LIGHT;
            case "emissive", "emit", "glow" -> EMISSIVE;
            default -> throw new IllegalArgumentException("Usage: /debugview off|material|light|sky|block|emissive|ao|biome|layer|uv|transparent");
        };
    }
}
