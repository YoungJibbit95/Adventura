package dev.voxelgame.client.animation;

public final class DamageFlash {
    private final AnimationClip clip;
    private final AnimationPlayback playback = new AnimationPlayback();
    private float strength;

    public DamageFlash() {
        this(UiAnimationPresets.damageFlash("damage-flash", 0.46));
    }

    DamageFlash(AnimationClip clip) {
        this.clip = clip;
    }

    public void trigger(int damageAmount, double nowSeconds) {
        if (damageAmount <= 0) {
            return;
        }
        strength = Math.min(1.0f, 0.42f + damageAmount * 0.11f);
        playback.play(clip, nowSeconds);
    }

    public boolean active(double nowSeconds) {
        return playback.active(nowSeconds);
    }

    public void clear() {
        strength = 0.0f;
        playback.stop();
    }

    public Sample sample(double nowSeconds) {
        AnimationSample sample = playback.sample(nowSeconds);
        float alpha = sample.value(AnimationChannels.ALPHA, 0.0f) * strength;
        float amount = sample.value(AnimationChannels.AMOUNT, 0.0f) * strength;
        return new Sample(alpha, amount);
    }

    public record Sample(float alpha, float amount) {
    }
}
