package dev.voxelgame.client.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloatAnimationTrackTest {
    @Test
    void interpolatesLinearlyAndClampsEnds() {
        FloatAnimationTrack track = FloatAnimationTrack.channel("alpha")
                .key(0.25, 0.0f)
                .key(1.25, 1.0f)
                .build();

        assertEquals(0.0f, track.sample(0.0), 0.001f);
        assertEquals(0.5f, track.sample(0.75), 0.001f);
        assertEquals(1.0f, track.sample(2.0), 0.001f);
    }

    @Test
    void appliesSegmentCurveFromStartingKeyframe() {
        FloatAnimationTrack track = FloatAnimationTrack.channel("lift")
                .key(0.0, 0.0f, AnimationCurve.EASE_OUT_CUBIC)
                .key(1.0, 1.0f)
                .build();

        assertTrue(track.sample(0.5) > 0.5f);
    }

    @Test
    void rejectsDuplicateKeyframeTimes() {
        assertThrows(IllegalArgumentException.class, () -> FloatAnimationTrack.channel("x")
                .key(0.0, 0.0f)
                .key(0.0, 1.0f)
                .build());
    }
}
