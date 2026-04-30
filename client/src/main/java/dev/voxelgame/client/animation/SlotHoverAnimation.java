package dev.voxelgame.client.animation;

public final class SlotHoverAnimation {
    private static final AnimationClip INTRO = AnimationClip.named("slot-hover-intro")
            .duration(0.16)
            .track(FloatAnimationTrack.channel(AnimationChannels.ALPHA)
                    .key(0.0, 0.0f, AnimationCurve.EASE_OUT_CUBIC)
                    .key(0.16, 0.32f)
                    .build())
            .track(FloatAnimationTrack.channel(AnimationChannels.INSET)
                    .key(0.0, 0.0f, AnimationCurve.EASE_OUT_CUBIC)
                    .key(0.16, 2.0f)
                    .build())
            .track(FloatAnimationTrack.channel(AnimationChannels.SCALE)
                    .key(0.0, 1.0f, AnimationCurve.EASE_OUT_CUBIC)
                    .key(0.16, 1.02f)
                    .build())
            .build();
    private static final AnimationClip PULSE = AnimationClip.named("slot-hover-pulse")
            .duration(0.9)
            .loopMode(AnimationLoopMode.LOOP)
            .track(FloatAnimationTrack.channel(AnimationChannels.ALPHA)
                    .key(0.0, 0.32f, AnimationCurve.SMOOTH_STEP)
                    .key(0.45, 0.48f, AnimationCurve.SMOOTH_STEP)
                    .key(0.9, 0.32f)
                    .build())
            .track(FloatAnimationTrack.channel(AnimationChannels.INSET)
                    .key(0.0, 2.0f, AnimationCurve.SMOOTH_STEP)
                    .key(0.45, 4.0f, AnimationCurve.SMOOTH_STEP)
                    .key(0.9, 2.0f)
                    .build())
            .track(FloatAnimationTrack.channel(AnimationChannels.SCALE)
                    .key(0.0, 1.02f, AnimationCurve.SMOOTH_STEP)
                    .key(0.45, 1.05f, AnimationCurve.SMOOTH_STEP)
                    .key(0.9, 1.02f)
                    .build())
            .build();

    private String currentKey = "";
    private double startedAtSeconds;

    public Sample sample(String key, double nowSeconds) {
        if (key == null || key.isBlank()) {
            return Sample.hidden();
        }
        if (!key.equals(currentKey)) {
            currentKey = key;
            startedAtSeconds = nowSeconds;
        }
        double elapsed = Math.max(0.0, nowSeconds - startedAtSeconds);
        AnimationSample sample = elapsed < INTRO.durationSeconds()
                ? INTRO.sample(elapsed)
                : PULSE.sample(elapsed - INTRO.durationSeconds());
        return new Sample(
                sample.value(AnimationChannels.ALPHA, 0.0f),
                sample.value(AnimationChannels.INSET, 0.0f),
                sample.value(AnimationChannels.SCALE, 1.0f)
        );
    }

    public record Sample(float alpha, float inset, float scale) {
        private static final Sample HIDDEN = new Sample(0.0f, 0.0f, 1.0f);

        public Sample {
            if (!Float.isFinite(alpha) || !Float.isFinite(inset) || !Float.isFinite(scale) || scale <= 0.0f) {
                throw new IllegalArgumentException("Slot hover sample values must be finite and scale must be > 0");
            }
        }

        public static Sample hidden() {
            return HIDDEN;
        }
    }
}
