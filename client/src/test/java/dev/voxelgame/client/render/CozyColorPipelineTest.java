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
