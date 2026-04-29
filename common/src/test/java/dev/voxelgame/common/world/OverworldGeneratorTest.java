package dev.voxelgame.common.world;

import dev.voxelgame.common.world.gen.OverworldGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverworldGeneratorTest {
    @Test
    void terrainHeightIsDeterministicForSeed() {
        OverworldGenerator a = new OverworldGenerator(42L);
        OverworldGenerator b = new OverworldGenerator(42L);
        BiomeType biome = a.biomeAt(100, -25);
        assertEquals(a.terrainHeight(100, -25, biome), b.terrainHeight(100, -25, biome));
    }
}
