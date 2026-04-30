package dev.voxelgame.client.animation;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimationClockTest {
    @Test
    void accumulatesMonotonicElapsedTime() {
        AtomicLong nanos = new AtomicLong(1_000_000_000L);
        AnimationClock clock = new AnimationClock(nanos::get);

        nanos.addAndGet(16_000_000L);
        assertEquals(0.016, clock.tick(), 0.0001);
        assertEquals(0.016, clock.elapsedSeconds(), 0.0001);

        nanos.addAndGet(34_000_000L);
        assertEquals(0.034, clock.tick(), 0.0001);
        assertEquals(0.050, clock.elapsedSeconds(), 0.0001);
    }

    @Test
    void clampsBackwardsClockDeltaToZero() {
        AtomicLong nanos = new AtomicLong(1_000_000_000L);
        AnimationClock clock = new AnimationClock(nanos::get);

        nanos.addAndGet(-50_000_000L);

        assertEquals(0.0, clock.tick(), 0.0001);
        assertEquals(0.0, clock.elapsedSeconds(), 0.0001);

        nanos.set(1_010_000_000L);
        assertEquals(0.010, clock.tick(), 0.0001);
        assertEquals(0.010, clock.elapsedSeconds(), 0.0001);
    }
}
