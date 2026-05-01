package dev.voxelgame.common.actions;

public record ActionCooldown(double seconds) {
    public static final ActionCooldown NONE = new ActionCooldown(0.0);

    public ActionCooldown {
        if (!Double.isFinite(seconds) || seconds < 0.0) {
            throw new IllegalArgumentException("Action cooldown seconds must be finite and >= 0");
        }
    }
}
