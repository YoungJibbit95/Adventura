package dev.voxelgame.client.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationPlaybackTest {
    @Test
    void tracksActiveStateAndSpeed() {
        AnimationPlayback playback = new AnimationPlayback();
        AnimationClip clip = AnimationClip.named("fade")
                .duration(1.0)
                .track(FloatAnimationTrack.channel(AnimationChannels.ALPHA)
                        .key(0.0, 0.0f)
                        .key(1.0, 1.0f)
                        .build())
                .build();

        playback.play(clip, 4.0, 2.0);

        assertTrue(playback.active(4.25));
        assertEquals(0.5f, playback.sample(4.25).value(AnimationChannels.ALPHA, -1.0f), 0.001f);
        assertFalse(playback.active(4.5));
        assertEquals(1.0f, playback.sample(4.5).value(AnimationChannels.ALPHA, -1.0f), 0.001f);
    }

    @Test
    void stopReturnsEmptySamples() {
        AnimationPlayback playback = new AnimationPlayback();
        AnimationClip clip = AnimationClip.named("fade")
                .duration(1.0)
                .track(FloatAnimationTrack.channel("x")
                        .key(0.0, 0.0f)
                        .key(1.0, 1.0f)
                        .build())
                .build();

        playback.play(clip, 1.0);
        playback.stop();

        assertFalse(playback.active(1.2));
        assertEquals(-1.0f, playback.sample(1.2).value("x", -1.0f), 0.001f);
    }

    @Test
    void rejectsInvalidSpeed() {
        AnimationPlayback playback = new AnimationPlayback();
        AnimationClip clip = AnimationClip.named("fade")
                .duration(1.0)
                .track(FloatAnimationTrack.channel("x")
                        .key(0.0, 0.0f)
                        .key(1.0, 1.0f)
                        .build())
                .build();

        assertThrows(IllegalArgumentException.class, () -> playback.play(clip, 0.0, 0.0));
    }
}
