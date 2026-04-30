package dev.voxelgame.client.animation;

import java.util.Objects;

public record AnimationKeyframe(double timeSeconds, float value, AnimationCurve curveToNext) {
    public AnimationKeyframe {
        if (!Double.isFinite(timeSeconds) || timeSeconds < 0.0) {
            throw new IllegalArgumentException("Keyframe time must be finite and >= 0");
        }
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Keyframe value must be finite");
        }
        curveToNext = Objects.requireNonNull(curveToNext, "curveToNext");
    }
}
