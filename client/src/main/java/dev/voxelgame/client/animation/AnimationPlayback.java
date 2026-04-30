package dev.voxelgame.client.animation;

import java.util.Objects;

public final class AnimationPlayback {
    private AnimationClip clip;
    private double startedAtSeconds;
    private double speed = 1.0;
    private boolean playing;

    public void play(AnimationClip clip, double nowSeconds) {
        play(clip, nowSeconds, 1.0);
    }

    public void play(AnimationClip clip, double nowSeconds, double speed) {
        if (!Double.isFinite(speed) || speed <= 0.0) {
            throw new IllegalArgumentException("Animation playback speed must be positive");
        }
        this.clip = Objects.requireNonNull(clip, "clip");
        this.startedAtSeconds = nowSeconds;
        this.speed = speed;
        this.playing = true;
    }

    public void stop() {
        playing = false;
    }

    public boolean active(double nowSeconds) {
        return playing && clip != null && !clip.finished(elapsedSeconds(nowSeconds));
    }

    public AnimationSample sample(double nowSeconds) {
        if (clip == null || !playing) {
            return AnimationSample.empty();
        }
        return clip.sample(elapsedSeconds(nowSeconds));
    }

    private double elapsedSeconds(double nowSeconds) {
        return Math.max(0.0, (nowSeconds - startedAtSeconds) * speed);
    }
}
