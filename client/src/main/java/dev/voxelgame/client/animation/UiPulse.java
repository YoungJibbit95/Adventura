package dev.voxelgame.client.animation;

public final class UiPulse {
    private final AnimationClip clip;

    public UiPulse(double frequencyHz, float baseInset, float insetAmplitude, float baseAlpha, float alphaAmplitude) {
        this.clip = UiAnimationPresets.selectedSlotPulse(frequencyHz, baseInset, insetAmplitude, baseAlpha, alphaAmplitude);
    }

    public static UiPulse selectedHotbarSlot() {
        return new UiPulse(0.92, 4.0f, 2.0f, 0.18f, 0.18f);
    }

    public Sample sample(double timeSeconds) {
        AnimationSample sample = clip.sample(timeSeconds);
        return new Sample(
                sample.value(AnimationChannels.AMOUNT, 0.0f),
                sample.value(AnimationChannels.INSET, 0.0f),
                sample.value(AnimationChannels.ALPHA, 0.0f)
        );
    }

    public record Sample(float amount, float inset, float alpha) {
    }
}
