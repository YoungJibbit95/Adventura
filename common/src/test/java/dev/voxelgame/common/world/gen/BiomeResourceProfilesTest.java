package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.world.BiomeType;
import dev.voxelgame.common.world.Biomes;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeResourceProfilesTest {
    @Test
    void everyDefaultBiomeHasGameplayProfile() {
        for (BiomeType biome : Biomes.createDefaultRegistry().values()) {
            BiomeResourceProfile profile = BiomeResourceProfiles.forBiome(biome.key());

            assertEquals(biome.key(), profile.biomeKey());
            assertTrue(profile.surfaceBlocks().contains(biome.surfaceBlock()), biome.key() + " profile should include registry surface block");
            assertFalse(profile.resourceBlocks().isEmpty(), biome.key() + " should have resource identity");
            assertFalse(profile.structureKeys().isEmpty(), biome.key() + " should have structure identity");
            assertFalse(profile.ambientEntityKeys().isEmpty(), biome.key() + " should have ambient entity identity");
        }
    }

    @Test
    void priorityBiomeProfilesExposeAlphaProgressionResources() {
        assertProfileContains("voxel:cozy_meadow",
                Set.of(Blocks.TWIG_PILE, Blocks.SMALL_STONE, Blocks.BERRY_BUSH, Blocks.HERB_PLANTER),
                "voxel:campsite",
                "voxel:cozy_sheep");
        assertProfileContains("voxel:pine_forest",
                Set.of(Blocks.PINE_LOG, Blocks.PINE_LEAVES, Blocks.TREE_STUMP, Blocks.RED_MUSHROOM),
                "voxel:simple_house",
                "voxel:little_boar");
        assertProfileContains("voxel:mushroom_grove",
                Set.of(Blocks.MUSHROOM_CLUSTER, Blocks.GLOW_MUSHROOM, Blocks.GLOW_CRYSTAL_NODE),
                "voxel:mushroom_circle",
                "voxel:firefly_swarm");
        assertProfileContains("voxel:lakeside",
                Set.of(Blocks.CLAY_DEPOSIT, Blocks.REEDS),
                "voxel:campsite",
                "voxel:firefly_swarm");
        assertProfileContains("voxel:old_ruins",
                Set.of(Blocks.MOSSY_STONE, Blocks.GLOW_CRYSTAL_NODE, Blocks.ANCIENT_LANTERN),
                "voxel:watchtower",
                "voxel:little_boar");
        assertProfileContains("voxel:highlands",
                Set.of(Blocks.SMALL_STONE, Blocks.GLOW_CRYSTAL_NODE),
                "voxel:watchtower",
                "voxel:forest_bunny");
        assertProfileContains("voxel:frost_peaks",
                Set.of(Blocks.SNOW, Blocks.ICE, Blocks.GLOW_CRYSTAL_NODE),
                "voxel:small_ruin",
                "voxel:forest_bunny");
    }

    @Test
    void detailResourcesComeFromProfiles() {
        assertEquals(Blocks.GLOW_MUSHROOM, BiomeResourceProfiles.detailResourceFor("voxel:mushroom_grove", 0.010));
        assertEquals(Blocks.REEDS, BiomeResourceProfiles.detailResourceFor("voxel:lakeside", 0.020));
        assertEquals(Blocks.TREE_STUMP, BiomeResourceProfiles.detailResourceFor("voxel:pine_forest", 0.010));
        assertEquals(Blocks.AIR, BiomeResourceProfiles.detailResourceFor("voxel:pine_forest", 0.90));
    }

    private static void assertProfileContains(String biomeKey, Set<Short> resources, String structureKey, String ambientEntityKey) {
        BiomeResourceProfile profile = BiomeResourceProfiles.forBiome(biomeKey);

        assertTrue(profile.resourceBlocks().containsAll(resources), biomeKey + " missing expected resources");
        assertTrue(profile.structureKeys().contains(structureKey), biomeKey + " missing expected structure");
        assertTrue(profile.ambientEntityKeys().contains(ambientEntityKey), biomeKey + " missing expected ambient entity");
    }
}
