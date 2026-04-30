package dev.voxelgame.client.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationMixerTest {
    @Test
    void mergesActiveLayersAndRemovesFinishedOneshots() {
        AnimationClip base = clip("base", "x", 0.0f, 1.0f, AnimationLoopMode.LOOP);
        AnimationClip overlay = clip("overlay", "alpha", 1.0f, 0.0f, AnimationLoopMode.ONCE);
        AnimationMixer mixer = new AnimationMixer();

        mixer.play("base", base, 10.0);
        mixer.play("overlay", overlay, 10.0);

        AnimationSample active = mixer.sample(10.25);

        assertEquals(0.25f, active.value("x", -1.0f), 0.001f);
        assertEquals(0.75f, active.value("alpha", -1.0f), 0.001f);
        assertTrue(mixer.active("base", 11.5));

        AnimationSample afterOverlay = mixer.sample(11.0);

        assertEquals(-1.0f, afterOverlay.value("alpha", -1.0f), 0.001f);
        assertFalse(mixer.active("overlay", 11.0));
    }

    @Test
    void laterLayersCanOverrideEarlierChannels() {
        AnimationMixer mixer = new AnimationMixer();

        mixer.play("base", clip("base", "scale", 1.0f, 1.0f, AnimationLoopMode.LOOP), 0.0);
        mixer.play("hover", clip("hover", "scale", 1.2f, 1.2f, AnimationLoopMode.LOOP), 0.0);

        assertEquals(1.2f, mixer.sample(0.5).value("scale", -1.0f), 0.001f);
    }

    private static AnimationClip clip(String name, String channel, float start, float end, AnimationLoopMode loopMode) {
        return AnimationClip.named(name)
                .duration(1.0)
                .loopMode(loopMode)
                .track(FloatAnimationTrack.channel(channel)
                        .key(0.0, start)
                        .key(1.0, end)
                        .build())
                .build();
    }
}
