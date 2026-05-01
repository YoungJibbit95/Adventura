package dev.voxelgame.client.render;

import dev.voxelgame.common.world.BiomeType;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CozyColorPipelineTest {
    @Test
    void blendsDayPaletteAcrossKeyTimes() {
        Vector3f night = CozyColorPipeline.skyColorForMinute(2 * 60);
        Vector3f morning = CozyColorPipeline.skyColorForMinute(6 * 60);
        Vector3f noon = CozyColorPipeline.skyColorForMinute(12 * 60);
        Vector3f evening = CozyColorPipeline.skyColorForMinute(18 * 60);

        assertTrue(morning.x > night.x);
        assertTrue(noon.z > evening.z);
        assertTrue(evening.x > night.x);
    }

    @Test
    void keepsFogPaletteSeparateFromSkyPalette() {
        Vector3f sky = CozyColorPipeline.skyColorForMinute(12 * 60);
        Vector3f fog = CozyColorPipeline.fogColorForMinute(12 * 60);

        assertTrue(fog.x > sky.x);
        assertTrue(fog.z < sky.z);
    }

    @Test
    void dayNightBrightnessAndFogDistanceStayPlayableAndSmooth() {
        float night = CozyColorPipeline.globalBrightnessForMinute(2 * 60);
        float dawn = CozyColorPipeline.globalBrightnessForMinute(6 * 60);
        float noon = CozyColorPipeline.globalBrightnessForMinute(12 * 60);

        assertTrue(night >= 0.40f);
        assertTrue(dawn > night);
        assertTrue(noon > dawn);
        assertTrue(CozyColorPipeline.fogDistanceScaleForMinute(2 * 60) < CozyColorPipeline.fogDistanceScaleForMinute(12 * 60));
        assertTrue(CozyColorPipeline.nightLightBoostForMinute(2 * 60) > CozyColorPipeline.nightLightBoostForMinute(12 * 60));
    }

    @Test
    void prefersNamedBiomeTintsAndFallsBackToBiomeClimate() {
        Vector3f pine = CozyColorPipeline.biomeTint("voxel:pine_forest", null);
        Vector3f lake = CozyColorPipeline.biomeTint("voxel:lakeside", null);
        Vector3f climate = CozyColorPipeline.biomeTint(
                "voxel:custom",
                new BiomeType("voxel:custom", (short) 1, (short) 2, (short) 3, 0.9f, 0.2f, 0.0f, 0.0f, 0.0f)
        );

        assertTrue(pine.y > pine.x);
        assertTrue(lake.z > pine.z);
        assertTrue(climate.x > 0.55f);
    }
}
