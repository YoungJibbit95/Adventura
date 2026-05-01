package dev.voxelgame.client.render;

import java.util.List;
import java.util.Objects;

public final class RenderPassPlan {
    public static final String SKY_CLEAR = "sky.clear";
    public static final String TERRAIN_OPAQUE = "terrain.opaque";
    public static final String TERRAIN_CUTOUT = "terrain.cutout";
    public static final String ENTITIES_OPAQUE = "entities.opaque";
    public static final String TERRAIN_TRANSLUCENT = "terrain.translucent";
    public static final String PARTICLES = "particles";
    public static final String OUTLINES_OVERLAYS = "outlines.overlays";
    public static final String HUD_UI = "hud.ui";
    public static final String DEBUG = "debug";

    private static final List<Entry> ALPHA_PLAN = List.of(
            new Entry(SKY_CLEAR, Category.CLEAR, false, true, BlendMode.NONE),
            new Entry(TERRAIN_OPAQUE, Category.TERRAIN, true, true, BlendMode.NONE),
            new Entry(TERRAIN_CUTOUT, Category.TERRAIN, true, true, BlendMode.NONE),
            new Entry(ENTITIES_OPAQUE, Category.ENTITY, true, true, BlendMode.NONE),
            new Entry(TERRAIN_TRANSLUCENT, Category.TERRAIN, true, false, BlendMode.ALPHA),
            new Entry(PARTICLES, Category.PARTICLE, true, false, BlendMode.ADDITIVE_OR_ALPHA),
            new Entry(OUTLINES_OVERLAYS, Category.OVERLAY, true, false, BlendMode.ALPHA),
            new Entry(HUD_UI, Category.UI, false, false, BlendMode.ALPHA),
            new Entry(DEBUG, Category.DEBUG, false, false, BlendMode.ALPHA)
    );

    private RenderPassPlan() {
    }

    public static List<Entry> alphaPlan() {
        return ALPHA_PLAN;
    }

    public static int orderOf(String passName) {
        for (int i = 0; i < ALPHA_PLAN.size(); i++) {
            if (ALPHA_PLAN.get(i).passName().equals(passName)) {
                return i;
            }
        }
        return -1;
    }

    public static Entry require(String passName) {
        for (Entry entry : ALPHA_PLAN) {
            if (entry.passName().equals(passName)) {
                return entry;
            }
        }
        throw new IllegalArgumentException("Unknown render pass: " + passName);
    }

    public enum Category {
        CLEAR,
        TERRAIN,
        ENTITY,
        PARTICLE,
        OVERLAY,
        UI,
        DEBUG
    }

    public enum BlendMode {
        NONE,
        ALPHA,
        ADDITIVE_OR_ALPHA
    }

    public record Entry(
            String passName,
            Category category,
            boolean depthTest,
            boolean depthWrite,
            BlendMode blendMode
    ) {
        public Entry {
            if (passName == null || passName.isBlank()) {
                throw new IllegalArgumentException("passName must not be blank");
            }
            category = Objects.requireNonNull(category, "category");
            blendMode = blendMode == null ? BlendMode.NONE : blendMode;
        }

        public boolean blendingEnabled() {
            return blendMode != BlendMode.NONE;
        }
    }
}
