package dev.voxelgame.common.actions;

public record ActionCost(int durabilityDamage, int hungerCost, int staminaCost) {
    public static final ActionCost NONE = new ActionCost(0, 0, 0);

    public ActionCost {
        if (durabilityDamage < 0 || hungerCost < 0 || staminaCost < 0) {
            throw new IllegalArgumentException("Action costs must be >= 0");
        }
    }
}
