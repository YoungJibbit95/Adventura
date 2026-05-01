package dev.voxelgame.common.gameplay.status;

import java.util.List;

public final class StatusEffectSystem {
    private StatusEffectSystem() {
    }

    public static StatusEffectState empty() {
        return StatusEffectState.empty();
    }

    public static StatusEffectState apply(StatusEffectState state, StatusEffectType type) {
        return safeState(state).apply(type);
    }

    public static StatusEffectState apply(StatusEffectState state, StatusEffectType type, double durationSeconds, int intensity) {
        return safeState(state).apply(type, durationSeconds, intensity);
    }

    public static StatusEffectTickResult tick(StatusEffectState state, double deltaSeconds) {
        return safeState(state).tick(deltaSeconds);
    }

    public static StatusEffectModifiers combinedModifiers(StatusEffectState state) {
        return safeState(state).combinedModifiers();
    }

    public static StatusEffectState restore(List<StatusEffectSaveState> saveStates) {
        return StatusEffectState.fromSaveStates(saveStates);
    }

    private static StatusEffectState safeState(StatusEffectState state) {
        return state == null ? StatusEffectState.empty() : state;
    }
}
