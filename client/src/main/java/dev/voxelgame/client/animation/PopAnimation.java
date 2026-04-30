package dev.voxelgame.client.animation;

public final class PopAnimation {
    private final AnimationClip clip;
    private final AnimationPlayback playback = new AnimationPlayback();

    private String key = "";

    public PopAnimation(double durationSeconds) {
        this.clip = UiAnimationPresets.pop("ui-pop", durationSeconds);
    }

    public void trigger(String key, double nowSeconds) {
        this.key = key == null ? "" : key;
        if (this.key.isBlank()) {
            playback.stop();
            return;
        }
        playback.play(clip, nowSeconds);
    }

    public boolean active(double nowSeconds) {
        return !key.isBlank() && playback.active(nowSeconds);
    }

    public String key() {
        return key;
    }

    public void clear() {
        key = "";
        playback.stop();
    }

    public Sample sample(double nowSeconds) {
        AnimationSample sample = playback.sample(nowSeconds);
        return new Sample(
                sample.value(AnimationChannels.PROGRESS, 1.0f),
                sample.value(AnimationChannels.ALPHA, 0.0f),
                sample.value(AnimationChannels.SCALE, 1.0f),
                sample.value(AnimationChannels.LIFT, 1.0f)
        );
    }

    public record Sample(float progress, float alpha, float scale, float lift) {
    }
}
