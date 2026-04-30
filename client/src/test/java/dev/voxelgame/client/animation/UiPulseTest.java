package dev.voxelgame.client.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UiPulseTest {
    @Test
    void selectedHotbarPulseKeepsStableRange() {
        UiPulse pulse = UiPulse.selectedHotbarSlot();

        UiPulse.Sample start = pulse.sample(0.0);
        UiPulse.Sample peak = pulse.sample(0.25 / 0.92);

        assertEquals(0.5f, start.amount(), 0.001f);
        assertEquals(5.0f, start.inset(), 0.001f);
        assertEquals(0.27f, start.alpha(), 0.001f);
        assertEquals(1.0f, peak.amount(), 0.001f);
        assertEquals(6.0f, peak.inset(), 0.001f);
        assertEquals(0.36f, peak.alpha(), 0.001f);
    }

    @Test
    void rejectsInvalidRanges() {
        assertThrows(IllegalArgumentException.class, () -> new UiPulse(0.0, 4.0f, 2.0f, 0.1f, 0.1f));
        assertThrows(IllegalArgumentException.class, () -> new UiPulse(1.0, -1.0f, 2.0f, 0.1f, 0.1f));
        assertThrows(IllegalArgumentException.class, () -> new UiPulse(1.0, 4.0f, 2.0f, 0.8f, 0.4f));
    }
}
