package dev.voxelgame.client.animation;

public final class UiAnimationPresets {
    private UiAnimationPresets() {
    }

    public static AnimationClip pop(String name, double durationSeconds) {
        return AnimationClip.named(name)
                .duration(durationSeconds)
                .track(FloatAnimationTrack.channel(AnimationChannels.PROGRESS)
                        .key(0.0, 0.0f)
                        .key(durationSeconds, 1.0f)
                        .build())
                .track(FloatAnimationTrack.channel(AnimationChannels.ALPHA)
                        .key(0.0, 1.0f)
                        .key(durationSeconds * 0.68, 1.0f, AnimationCurve.SMOOTH_STEP)
                        .key(durationSeconds, 0.0f)
                        .build())
                .track(FloatAnimationTrack.channel(AnimationChannels.SCALE)
                        .key(0.0, 0.88f, AnimationCurve.EASE_OUT_CUBIC)
                        .key(durationSeconds, 1.24f)
                        .build())
                .track(FloatAnimationTrack.channel(AnimationChannels.LIFT)
                        .key(0.0, 0.0f, AnimationCurve.EASE_OUT_CUBIC)
                        .key(durationSeconds, 1.0f)
                        .build())
                .build();
    }

    public static AnimationClip selectedSlotPulse(double frequencyHz, float baseInset, float insetAmplitude, float baseAlpha, float alphaAmplitude) {
        if (!Double.isFinite(frequencyHz) || frequencyHz <= 0.0) {
            throw new IllegalArgumentException("Pulse frequency must be > 0");
        }
        if (baseInset < 0.0f || insetAmplitude < 0.0f) {
            throw new IllegalArgumentException("Pulse inset values must be >= 0");
        }
        if (baseAlpha < 0.0f || alphaAmplitude < 0.0f || baseAlpha + alphaAmplitude > 1.0f) {
            throw new IllegalArgumentException("Pulse alpha must stay in 0..1");
        }
        double duration = 1.0 / frequencyHz;
        return AnimationClip.named("selected-slot-pulse")
                .duration(duration)
                .loopMode(AnimationLoopMode.LOOP)
                .track(pulseTrack(AnimationChannels.AMOUNT, duration, 0.0f, 1.0f))
                .track(pulseTrack(AnimationChannels.INSET, duration, baseInset, insetAmplitude))
                .track(pulseTrack(AnimationChannels.ALPHA, duration, baseAlpha, alphaAmplitude))
                .build();
    }

    public static AnimationClip subtleShake(String name, double durationSeconds, float amplitude) {
        if (amplitude < 0.0f || !Float.isFinite(amplitude)) {
            throw new IllegalArgumentException("Shake amplitude must be finite and >= 0");
        }
        return AnimationClip.named(name)
                .duration(durationSeconds)
                .track(FloatAnimationTrack.channel(AnimationChannels.SHAKE_X)
                        .key(0.0, 0.0f)
                        .key(durationSeconds * 0.18, amplitude, AnimationCurve.SMOOTH_STEP)
                        .key(durationSeconds * 0.38, -amplitude, AnimationCurve.SMOOTH_STEP)
                        .key(durationSeconds * 0.62, amplitude * 0.55f, AnimationCurve.SMOOTH_STEP)
                        .key(durationSeconds, 0.0f)
                        .build())
                .track(FloatAnimationTrack.channel(AnimationChannels.ALPHA)
                        .key(0.0, 1.0f, AnimationCurve.SMOOTH_STEP)
                        .key(durationSeconds, 0.0f)
                        .build())
                .build();
    }

    public static AnimationClip damageFlash(String name, double durationSeconds) {
        return AnimationClip.named(name)
                .duration(durationSeconds)
                .track(FloatAnimationTrack.channel(AnimationChannels.ALPHA)
                        .key(0.0, 1.0f)
                        .key(durationSeconds * 0.18, 0.76f, AnimationCurve.SMOOTH_STEP)
                        .key(durationSeconds, 0.0f)
                        .build())
                .track(FloatAnimationTrack.channel(AnimationChannels.AMOUNT)
                        .key(0.0, 1.0f, AnimationCurve.EASE_OUT_CUBIC)
                        .key(durationSeconds, 0.0f)
                        .build())
                .build();
    }

    private static FloatAnimationTrack pulseTrack(String channel, double duration, float base, float amplitude) {
        return FloatAnimationTrack.channel(channel)
                .key(0.0, base + amplitude * 0.5f, AnimationCurve.SMOOTH_STEP)
                .key(duration * 0.25, base + amplitude, AnimationCurve.SMOOTH_STEP)
                .key(duration * 0.5, base + amplitude * 0.5f, AnimationCurve.SMOOTH_STEP)
                .key(duration * 0.75, base, AnimationCurve.SMOOTH_STEP)
                .key(duration, base + amplitude * 0.5f)
                .build();
    }
}
