package dev.voxelgame.client.animation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AnimationClip {
    private final String name;
    private final double durationSeconds;
    private final AnimationLoopMode loopMode;
    private final List<FloatAnimationTrack> tracks;

    private AnimationClip(String name, double durationSeconds, AnimationLoopMode loopMode, List<FloatAnimationTrack> tracks) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Animation clip name must not be blank");
        }
        if (!Double.isFinite(durationSeconds) || durationSeconds <= 0.0) {
            throw new IllegalArgumentException("Animation clip duration must be positive");
        }
        if (tracks.isEmpty()) {
            throw new IllegalArgumentException("Animation clip needs at least one track");
        }
        this.name = name;
        this.durationSeconds = durationSeconds;
        this.loopMode = Objects.requireNonNull(loopMode, "loopMode");
        this.tracks = List.copyOf(tracks);
    }

    public static Builder named(String name) {
        return new Builder(name);
    }

    public String name() {
        return name;
    }

    public double durationSeconds() {
        return durationSeconds;
    }

    public AnimationLoopMode loopMode() {
        return loopMode;
    }

    public boolean finished(double elapsedSeconds) {
        return loopMode == AnimationLoopMode.ONCE && elapsedSeconds >= durationSeconds;
    }

    public AnimationSample sample(double elapsedSeconds) {
        double localTime = loopMode.localTime(Math.max(0.0, elapsedSeconds), durationSeconds);
        Map<String, Float> values = new LinkedHashMap<>();
        for (FloatAnimationTrack track : tracks) {
            values.put(track.channel(), track.sample(localTime));
        }
        return new AnimationSample(values);
    }

    public static final class Builder {
        private final String name;
        private final List<FloatAnimationTrack> tracks = new ArrayList<>();
        private AnimationLoopMode loopMode = AnimationLoopMode.ONCE;
        private double durationSeconds = -1.0;

        private Builder(String name) {
            this.name = name;
        }

        public Builder duration(double durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public Builder loopMode(AnimationLoopMode loopMode) {
            this.loopMode = Objects.requireNonNull(loopMode, "loopMode");
            return this;
        }

        public Builder track(FloatAnimationTrack track) {
            tracks.add(Objects.requireNonNull(track, "track"));
            return this;
        }

        public AnimationClip build() {
            double duration = durationSeconds;
            if (duration <= 0.0 && !tracks.isEmpty()) {
                duration = tracks.stream()
                        .mapToDouble(FloatAnimationTrack::endTimeSeconds)
                        .max()
                        .orElse(0.0);
            }
            return new AnimationClip(name, duration, loopMode, tracks);
        }
    }
}
