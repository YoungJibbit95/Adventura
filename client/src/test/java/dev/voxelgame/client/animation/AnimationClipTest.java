package dev.voxelgame.client.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationClipTest {
    @Test
    void samplesMultipleFloatTracks() {
        AnimationClip clip = AnimationClip.named("ui")
                .duration(1.0)
                .track(FloatAnimationTrack.channel(AnimationChannels.ALPHA)
                        .key(0.0, 0.0f)
                        .key(1.0, 1.0f)
                        .build())
                .track(FloatAnimationTrack.channel(AnimationChannels.SCALE)
                        .key(0.0, 1.0f)
                        .key(1.0, 2.0f)
                        .build())
                .build();

        AnimationSample sample = clip.sample(0.5);

        assertEquals(0.5f, sample.value(AnimationChannels.ALPHA, -1.0f), 0.001f);
        assertEquals(1.5f, sample.value(AnimationChannels.SCALE, -1.0f), 0.001f);
    }

    @Test
    void loopsAndPingPongsLocalTime() {
        FloatAnimationTrack track = FloatAnimationTrack.channel("x")
                .key(0.0, 0.0f)
                .key(1.0, 10.0f)
                .build();
        AnimationClip loop = AnimationClip.named("loop")
                .duration(1.0)
                .loopMode(AnimationLoopMode.LOOP)
                .track(track)
                .build();
        AnimationClip pingPong = AnimationClip.named("ping")
                .duration(1.0)
                .loopMode(AnimationLoopMode.PING_PONG)
                .track(track)
                .build();

        assertEquals(2.5f, loop.sample(1.25).value("x", -1.0f), 0.001f);
        assertEquals(7.5f, pingPong.sample(1.25).value("x", -1.0f), 0.001f);
        assertFalse(loop.finished(100.0));
        assertTrue(AnimationClip.named("once").duration(1.0).track(track).build().finished(1.0));
    }

    @Test
    void rejectsEmptyClips() {
        assertThrows(IllegalArgumentException.class, () -> AnimationClip.named("empty").duration(1.0).build());
    }
}
