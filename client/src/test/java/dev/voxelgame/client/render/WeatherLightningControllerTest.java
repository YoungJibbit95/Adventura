package dev.voxelgame.client.render;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherLightningControllerTest {
    @Test
    void manualTriggerProducesShortFlashAndDelayedThunderOnce() {
        WeatherLightningController controller = new WeatherLightningController();

        controller.trigger(10.0);

        assertTrue(controller.currentFlash(10.01) > 0.8f);
        assertFalse(controller.update(10.20, 123L, 22 * 60, "voxel:mire", null).thunderCue());
        assertTrue(controller.update(10.50, 123L, 22 * 60, "voxel:mire", null).thunderCue());
        assertFalse(controller.update(10.60, 123L, 22 * 60, "voxel:mire", null).thunderCue());
        assertEquals(0.0f, controller.currentFlash(10.90), 0.0001f);
    }

    @Test
    void stormEligibilityRequiresStormyBiomeAndNightOrEdge() {
        assertTrue(WeatherLightningController.stormEligible(22 * 60, "voxel:mire", null));
        assertTrue(WeatherLightningController.stormEligible(18 * 60, "voxel:lakeside", null));
        assertFalse(WeatherLightningController.stormEligible(22 * 60, "voxel:meadow", null));
        assertFalse(WeatherLightningController.stormEligible(12 * 60, "voxel:mire", null));
    }

    @Test
    void flashColorMovesSkyAndFogTowardCoolLightningTint() {
        Vector3f sky = WeatherLightningController.applySkyFlash(new Vector3f(0.20f, 0.30f, 0.40f), 1.0f);
        Vector3f fog = WeatherLightningController.applyFogFlash(new Vector3f(0.18f, 0.22f, 0.28f), 1.0f);

        assertTrue(sky.x > 0.20f);
        assertTrue(sky.y > 0.30f);
        assertTrue(sky.z > 0.40f);
        assertTrue(fog.x > 0.18f);
        assertTrue(fog.y > 0.22f);
        assertTrue(fog.z > 0.28f);
    }
}
