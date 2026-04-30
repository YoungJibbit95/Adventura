package dev.voxelgame.client.animation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class FloatAnimationTrack {
    private final String channel;
    private final List<AnimationKeyframe> keyframes;

    private FloatAnimationTrack(String channel, List<AnimationKeyframe> keyframes) {
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("Animation channel must not be blank");
        }
        if (keyframes.isEmpty()) {
            throw new IllegalArgumentException("Animation track needs at least one keyframe");
        }
        this.channel = channel;
        this.keyframes = keyframes.stream()
                .sorted(Comparator.comparingDouble(AnimationKeyframe::timeSeconds))
                .toList();
        requireStrictlyIncreasingTimes(this.keyframes);
    }

    public static Builder channel(String channel) {
        return new Builder(channel);
    }

    public String channel() {
        return channel;
    }

    public double endTimeSeconds() {
        return keyframes.get(keyframes.size() - 1).timeSeconds();
    }

    public float sample(double localTimeSeconds) {
        if (localTimeSeconds <= keyframes.get(0).timeSeconds()) {
            return keyframes.get(0).value();
        }
        for (int i = 0; i < keyframes.size() - 1; i++) {
            AnimationKeyframe start = keyframes.get(i);
            AnimationKeyframe end = keyframes.get(i + 1);
            if (localTimeSeconds <= end.timeSeconds()) {
                double segmentDuration = end.timeSeconds() - start.timeSeconds();
                double progress = segmentDuration <= 0.0 ? 1.0 : (localTimeSeconds - start.timeSeconds()) / segmentDuration;
                float eased = start.curveToNext().apply(progress);
                return start.value() + (end.value() - start.value()) * eased;
            }
        }
        return keyframes.get(keyframes.size() - 1).value();
    }

    private static void requireStrictlyIncreasingTimes(List<AnimationKeyframe> keyframes) {
        double previous = -1.0;
        for (AnimationKeyframe keyframe : keyframes) {
            if (keyframe.timeSeconds() <= previous) {
                throw new IllegalArgumentException("Animation keyframe times must be strictly increasing");
            }
            previous = keyframe.timeSeconds();
        }
    }

    public static final class Builder {
        private final String channel;
        private final List<AnimationKeyframe> keyframes = new ArrayList<>();

        private Builder(String channel) {
            this.channel = channel;
        }

        public Builder key(double timeSeconds, float value) {
            return key(timeSeconds, value, AnimationCurve.LINEAR);
        }

        public Builder key(double timeSeconds, float value, AnimationCurve curveToNext) {
            keyframes.add(new AnimationKeyframe(timeSeconds, value, Objects.requireNonNull(curveToNext, "curveToNext")));
            return this;
        }

        public FloatAnimationTrack build() {
            return new FloatAnimationTrack(channel, keyframes);
        }
    }
}
