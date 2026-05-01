package dev.voxelgame.client.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainLightingShaderContractTest {
    @Test
    void terrainShaderKeepsSeparateLightChannelsAndCaveControls() {
        String shader = shaderSource("assets/voxelgame/shaders/chunk.frag");

        assertTrue(shader.contains("uniform float uGlobalBrightness"));
        assertTrue(shader.contains("uniform float uBloomThreshold"));
        assertTrue(shader.contains("uniform float uNightLightBoost"));
        assertTrue(shader.contains("uniform float uCaveDarkness"));
        assertTrue(shader.contains("uniform float uWeatherFlash"));
        assertTrue(shader.contains("vSkyLight"));
        assertTrue(shader.contains("vBlockLight"));
        assertTrue(shader.contains("float terrainLight"));
        assertTrue(shader.contains("float aoForLight"));
        assertTrue(shader.contains("float bloomAmount"));
        assertTrue(shader.contains("smoothstep(threshold, 1.0"));
        assertTrue(shader.contains("clamp(glowContribution, vec3(0.0), vec3(0.42))"));
        assertTrue(shader.contains("animatedFluid()"));
        assertTrue(shader.contains("glowContribution"));
        assertTrue(shader.contains("uWeatherFlash, 0.0, 1.0"));
    }

    @Test
    void entityShaderKeepsWeatherFlashSeparateFromEntityLight() {
        String shader = shaderSource("assets/voxelgame/shaders/entity.frag");

        assertTrue(shader.contains("uniform float uEntityLight"));
        assertTrue(shader.contains("uniform float uWeatherFlash"));
        assertTrue(shader.contains("uWeatherFlash, 0.0, 1.0"));
    }

    @Test
    void particleShaderKeepsFireflyGlowSoftAdditive() {
        String shader = shaderSource("assets/voxelgame/shaders/particle.frag");

        assertTrue(shader.contains("smoothstep(0.72, 1.0"));
        assertTrue(shader.contains("glow * 0.35"));
    }

    private static String shaderSource(String resourcePath) {
        try (var stream = TerrainLightingShaderContractTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Missing shader resource: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
