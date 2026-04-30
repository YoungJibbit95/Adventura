package dev.voxelgame.client.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopAnimationTest {
    @Test
    void tracksActiveWindow() {
        PopAnimation animation = new PopAnimation(0.5);

        assertFalse(animation.active(10.0));

        animation.trigger("stick", 10.0);

        assertTrue(animation.active(10.0));
        assertTrue(animation.active(10.49));
        assertFalse(animation.active(10.5));
    }

    @Test
    void sampleGrowsLiftsAndFadesNearEnd() {
        PopAnimation animation = new PopAnimation(1.0);
        animation.trigger("recipe", 4.0);

        PopAnimation.Sample start = animation.sample(4.0);
        PopAnimation.Sample middle = animation.sample(4.5);
        PopAnimation.Sample end = animation.sample(5.0);

        assertEquals(0.0f, start.progress(), 0.001f);
        assertEquals(0.88f, start.scale(), 0.001f);
        assertEquals(1.0f, start.alpha(), 0.001f);
        assertTrue(middle.scale() > start.scale());
        assertTrue(middle.lift() > start.lift());
        assertTrue(end.alpha() < 0.001f);
        assertEquals(1.0f, end.progress(), 0.001f);
    }

    @Test
    void blankTriggerDisablesAnimation() {
        PopAnimation animation = new PopAnimation(0.5);

        animation.trigger("", 1.0);

        assertFalse(animation.active(1.1));
        assertEquals("", animation.key());
    }

    @Test
    void rejectsInvalidDuration() {
        assertThrows(IllegalArgumentException.class, () -> new PopAnimation(0.0));
        assertThrows(IllegalArgumentException.class, () -> new PopAnimation(Double.NaN));
    }
}
