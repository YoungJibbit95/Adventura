package dev.voxelgame.client.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiAnimationPresetsTest {
    @Test
    void popPresetProvidesCommonUiChannels() {
        AnimationClip clip = UiAnimationPresets.pop("pickup", 1.0);

        AnimationSample start = clip.sample(0.0);
        AnimationSample middle = clip.sample(0.5);
        AnimationSample end = clip.sample(1.0);

        assertEquals(0.0f, start.value(AnimationChannels.PROGRESS, -1.0f), 0.001f);
        assertEquals(0.88f, start.value(AnimationChannels.SCALE, -1.0f), 0.001f);
        assertEquals(1.0f, middle.value(AnimationChannels.ALPHA, -1.0f), 0.001f);
        assertTrue(middle.value(AnimationChannels.LIFT, -1.0f) > 0.5f);
        assertEquals(0.0f, end.value(AnimationChannels.ALPHA, -1.0f), 0.001f);
    }

    @Test
    void subtleShakeReturnsToRest() {
        AnimationClip clip = UiAnimationPresets.subtleShake("missing", 0.4, 6.0f);

        assertEquals(0.0f, clip.sample(0.0).value(AnimationChannels.SHAKE_X, -1.0f), 0.001f);
        assertTrue(Math.abs(clip.sample(0.12).value(AnimationChannels.SHAKE_X, 0.0f)) > 0.1f);
        assertEquals(0.0f, clip.sample(0.4).value(AnimationChannels.SHAKE_X, -1.0f), 0.001f);
    }
}
