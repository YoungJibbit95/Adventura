package dev.voxelgame.client.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeldItemAnimationTest {
    @Test
    void idleBobMovesItemWithoutAction() {
        HeldItemAnimation animation = new HeldItemAnimation();

        HeldItemAnimation.Sample start = animation.sample(0.0);
        HeldItemAnimation.Sample peak = animation.sample(0.3);

        assertTrue(peak.itemY() > start.itemY());
        assertFalse(animation.actionActive(0.3));
    }

    @Test
    void breakSwingUsesToolSpeedForDuration() {
        HeldItemAnimation slow = new HeldItemAnimation();
        HeldItemAnimation fast = new HeldItemAnimation();

        slow.breakSwing(10.0, 0.75f);
        fast.breakSwing(10.0, 1.9f);

        assertTrue(slow.actionActive(10.2));
        assertFalse(fast.actionActive(10.2));
    }

    @Test
    void placeAndEatUseDifferentMotionShapes() {
        HeldItemAnimation animation = new HeldItemAnimation();

        animation.place(2.0);
        HeldItemAnimation.Sample place = animation.sample(2.11);
        animation.eat(4.0);
        HeldItemAnimation.Sample eat = animation.sample(4.14);

        assertTrue(place.handY() > 0.0f);
        assertTrue(eat.itemY() < 0.0f);
        assertTrue(eat.scale() < 1.0f);
    }

    @Test
    void clearingActionKeepsIdleAvailable() {
        HeldItemAnimation animation = new HeldItemAnimation();

        animation.use(1.0);
        animation.clearAction();

        assertFalse(animation.actionActive(1.1));
        assertTrue(animation.sample(1.5).scale() > 0.0f);
    }
}
