package dev.voxelgame.client.ui;

public record UiColor(float r, float g, float b, float a) {
    public static final UiColor WHITE = new UiColor(0.94f, 0.96f, 0.92f, 1.0f);
    public static final UiColor MUTED = new UiColor(0.68f, 0.74f, 0.70f, 1.0f);
    public static final UiColor PANEL = new UiColor(0.05f, 0.07f, 0.08f, 0.78f);
    public static final UiColor BUTTON = new UiColor(0.15f, 0.20f, 0.20f, 0.92f);
    public static final UiColor BUTTON_HOVER = new UiColor(0.22f, 0.30f, 0.28f, 0.96f);
    public static final UiColor BUTTON_DISABLED = new UiColor(0.10f, 0.12f, 0.13f, 0.72f);
    public static final UiColor ACCENT = new UiColor(0.45f, 0.74f, 0.42f, 1.0f);
    public static final UiColor WARNING = new UiColor(0.96f, 0.68f, 0.28f, 1.0f);
    public static final UiColor HEART = new UiColor(0.86f, 0.24f, 0.27f, 1.0f);
    public static final UiColor HUNGER = new UiColor(0.92f, 0.58f, 0.28f, 1.0f);
    public static final UiColor ENERGY = new UiColor(0.98f, 0.82f, 0.32f, 1.0f);
    public static final UiColor WATER = new UiColor(0.35f, 0.65f, 0.92f, 1.0f);
    public static final UiColor SLOT = new UiColor(0.09f, 0.12f, 0.12f, 0.88f);
    public static final UiColor SLOT_ACTIVE = new UiColor(0.34f, 0.48f, 0.40f, 0.96f);
}
