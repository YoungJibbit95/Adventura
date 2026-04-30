package dev.voxelgame.client.animation;

public final class HeldItemAnimation {
    private static final String ITEM_X = "held.item.x";
    private static final String ITEM_Y = "held.item.y";
    private static final String HAND_X = "held.hand.x";
    private static final String HAND_Y = "held.hand.y";
    private static final String SCALE = "held.scale";

    private final AnimationClip idle = AnimationClip.named("held-item-idle")
            .duration(1.2)
            .loopMode(AnimationLoopMode.LOOP)
            .track(FloatAnimationTrack.channel(ITEM_Y)
                    .key(0.0, 0.0f, AnimationCurve.SMOOTH_STEP)
                    .key(0.3, 1.4f, AnimationCurve.SMOOTH_STEP)
                    .key(0.6, 0.0f, AnimationCurve.SMOOTH_STEP)
                    .key(0.9, -1.4f, AnimationCurve.SMOOTH_STEP)
                    .key(1.2, 0.0f)
                    .build())
            .track(FloatAnimationTrack.channel(SCALE)
                    .key(0.0, 1.0f)
                    .key(1.2, 1.0f)
                    .build())
            .build();
    private final AnimationClip breakSwing = actionClip("held-item-break", 0.34, -4.0f, -4.0f, -8.0f, 6.0f, 1.03f);
    private final AnimationClip place = actionClip("held-item-place", 0.24, -7.0f, 6.0f, -12.0f, 9.0f, 0.96f);
    private final AnimationClip use = actionClip("held-item-use", 0.30, -10.0f, -9.0f, -8.0f, -4.0f, 1.0f);
    private final AnimationClip eat = eatClip();
    private final AnimationPlayback action = new AnimationPlayback();

    private Action currentAction = Action.NONE;

    public void breakSwing(double nowSeconds, float toolSpeed) {
        if (action.active(nowSeconds) && currentAction == Action.BREAK) {
            return;
        }
        action.play(breakSwing, nowSeconds, swingSpeed(toolSpeed));
        currentAction = Action.BREAK;
    }

    public void place(double nowSeconds) {
        play(place, Action.PLACE, nowSeconds);
    }

    public void use(double nowSeconds) {
        play(use, Action.USE, nowSeconds);
    }

    public void eat(double nowSeconds) {
        play(eat, Action.EAT, nowSeconds);
    }

    public void clearAction() {
        action.stop();
        currentAction = Action.NONE;
    }

    public boolean actionActive(double nowSeconds) {
        return action.active(nowSeconds);
    }

    public Sample sample(double nowSeconds) {
        AnimationSample idleSample = idle.sample(nowSeconds);
        AnimationSample actionSample = action.active(nowSeconds) ? action.sample(nowSeconds) : AnimationSample.empty();
        return new Sample(
                value(idleSample, actionSample, ITEM_X, 0.0f),
                value(idleSample, actionSample, ITEM_Y, 0.0f),
                value(idleSample, actionSample, HAND_X, 0.0f),
                value(idleSample, actionSample, HAND_Y, 0.0f),
                idleSample.value(SCALE, 1.0f) + actionSample.value(SCALE, 0.0f)
        );
    }

    private void play(AnimationClip clip, Action actionType, double nowSeconds) {
        action.play(clip, nowSeconds);
        currentAction = actionType;
    }

    private static float value(AnimationSample idleSample, AnimationSample actionSample, String channel, float fallback) {
        return idleSample.value(channel, fallback) + actionSample.value(channel, 0.0f);
    }

    private static double swingSpeed(float toolSpeed) {
        if (!Float.isFinite(toolSpeed)) {
            return 1.0;
        }
        return Math.max(0.75, Math.min(1.9, toolSpeed));
    }

    private static AnimationClip actionClip(String name, double duration, float itemX, float itemY, float handX, float handY, float peakScale) {
        return AnimationClip.named(name)
                .duration(duration)
                .track(punchTrack(ITEM_X, duration, itemX))
                .track(punchTrack(ITEM_Y, duration, itemY))
                .track(punchTrack(HAND_X, duration, handX))
                .track(punchTrack(HAND_Y, duration, handY))
                .track(FloatAnimationTrack.channel(SCALE)
                        .key(0.0, 0.0f, AnimationCurve.EASE_OUT_CUBIC)
                        .key(duration * 0.45, peakScale - 1.0f, AnimationCurve.SMOOTH_STEP)
                        .key(duration, 0.0f)
                        .build())
                .build();
    }

    private static AnimationClip eatClip() {
        return AnimationClip.named("held-item-eat")
                .duration(0.46)
                .track(FloatAnimationTrack.channel(ITEM_X)
                        .key(0.0, 0.0f, AnimationCurve.EASE_OUT_CUBIC)
                        .key(0.14, -22.0f, AnimationCurve.SMOOTH_STEP)
                        .key(0.26, -14.0f, AnimationCurve.EASE_OUT_CUBIC)
                        .key(0.36, -24.0f, AnimationCurve.SMOOTH_STEP)
                        .key(0.46, 0.0f)
                        .build())
                .track(FloatAnimationTrack.channel(ITEM_Y)
                        .key(0.0, 0.0f, AnimationCurve.EASE_OUT_CUBIC)
                        .key(0.14, -20.0f, AnimationCurve.SMOOTH_STEP)
                        .key(0.26, -10.0f, AnimationCurve.EASE_OUT_CUBIC)
                        .key(0.36, -22.0f, AnimationCurve.SMOOTH_STEP)
                        .key(0.46, 0.0f)
                        .build())
                .track(punchTrack(HAND_X, 0.46, -7.0f))
                .track(punchTrack(HAND_Y, 0.46, -5.0f))
                .track(FloatAnimationTrack.channel(SCALE)
                        .key(0.0, 0.0f, AnimationCurve.SMOOTH_STEP)
                        .key(0.14, -0.12f, AnimationCurve.SMOOTH_STEP)
                        .key(0.26, -0.04f, AnimationCurve.SMOOTH_STEP)
                        .key(0.46, 0.0f)
                        .build())
                .build();
    }

    private static FloatAnimationTrack punchTrack(String channel, double duration, float peak) {
        return FloatAnimationTrack.channel(channel)
                .key(0.0, 0.0f, AnimationCurve.EASE_OUT_CUBIC)
                .key(duration * 0.45, peak, AnimationCurve.SMOOTH_STEP)
                .key(duration, 0.0f)
                .build();
    }

    public record Sample(float itemX, float itemY, float handX, float handY, float scale) {
        public Sample {
            if (!Float.isFinite(itemX) || !Float.isFinite(itemY)
                    || !Float.isFinite(handX) || !Float.isFinite(handY)
                    || !Float.isFinite(scale) || scale <= 0.0f) {
                throw new IllegalArgumentException("Held item animation sample values must be finite and scale must be > 0");
            }
        }
    }

    private enum Action {
        NONE,
        BREAK,
        PLACE,
        USE,
        EAT
    }
}
