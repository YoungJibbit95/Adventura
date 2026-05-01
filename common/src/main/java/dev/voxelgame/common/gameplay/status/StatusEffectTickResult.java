package dev.voxelgame.common.gameplay.status;

import java.util.List;
import java.util.Objects;

public record StatusEffectTickResult(
        StatusEffectState state,
        List<StatusEffectPulse> pulses
) {
    public StatusEffectTickResult {
        Objects.requireNonNull(state, "state");
        pulses = List.copyOf(pulses == null ? List.of() : pulses);
    }
}
