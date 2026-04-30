package dev.voxelgame.client.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotHoverAnimationTest {
    @Test
    void fadesInThenPulsesWhileKeyStaysHovered() {
        SlotHoverAnimation animation = new SlotHoverAnimation();

        SlotHoverAnimation.Sample start = animation.sample("inventory:1", 10.0);
        SlotHoverAnimation.Sample intro = animation.sample("inventory:1", 10.16);
        SlotHoverAnimation.Sample pulse = animation.sample("inventory:1", 10.61);

        assertEquals(0.0f, start.alpha(), 0.001f);
        assertTrue(intro.alpha() > start.alpha());
        assertTrue(pulse.alpha() > intro.alpha());
        assertTrue(pulse.inset() > intro.inset());
    }

    @Test
    void newKeyRestartsIntro() {
        SlotHoverAnimation animation = new SlotHoverAnimation();

        animation.sample("inventory:1", 4.0);
        SlotHoverAnimation.Sample mature = animation.sample("inventory:1", 4.9);
        SlotHoverAnimation.Sample restarted = animation.sample("inventory:2", 5.0);

        assertTrue(mature.alpha() > restarted.alpha());
        assertEquals(0.0f, restarted.inset(), 0.001f);
    }

    @Test
    void blankKeyIsHidden() {
        SlotHoverAnimation.Sample hidden = new SlotHoverAnimation().sample("", 1.0);

        assertEquals(0.0f, hidden.alpha(), 0.001f);
        assertEquals(1.0f, hidden.scale(), 0.001f);
    }
}
