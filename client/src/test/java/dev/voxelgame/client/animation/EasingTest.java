package dev.voxelgame.client.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EasingTest {
    @Test
    void clampsLinearProgress() {
        assertEquals(0.0f, Easing.linear(-0.25), 0.001f);
        assertEquals(0.5f, Easing.linear(0.5), 0.001f);
        assertEquals(1.0f, Easing.linear(1.25), 0.001f);
    }

    @Test
    void smoothStepKeepsEndpointsAndEasesMiddle() {
        assertEquals(0.0f, Easing.smoothStep(0.0), 0.001f);
        assertEquals(0.5f, Easing.smoothStep(0.5), 0.001f);
        assertEquals(1.0f, Easing.smoothStep(1.0), 0.001f);
    }

    @Test
    void pulseStaysInUnitRange() {
        assertEquals(0.5f, Easing.pulse(0.0, 1.0), 0.001f);
        assertEquals(1.0f, Easing.pulse(0.25, 1.0), 0.001f);
        assertEquals(0.5f, Easing.pulse(0.5, 1.0), 0.001f);
    }
}
