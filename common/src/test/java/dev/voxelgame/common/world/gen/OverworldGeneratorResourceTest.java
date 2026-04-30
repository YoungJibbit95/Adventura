package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverworldGeneratorResourceTest {
    @Test
    void detailResourcesGiveBiomesDistinctGatherables() {
        assertEquals(Blocks.BERRY_BUSH, OverworldGenerator.detailResourceFor("voxel:cozy_meadow", 0.010));
        assertEquals(Blocks.HERB_PLANTER, OverworldGenerator.detailResourceFor("voxel:cozy_meadow", 0.020));
        assertEquals(Blocks.SUN_BLOOM, OverworldGenerator.detailResourceFor("voxel:flower_fields", 0.010));
        assertEquals(Blocks.CLAY_DEPOSIT, OverworldGenerator.detailResourceFor("voxel:lakeside", 0.010));
        assertEquals(Blocks.REEDS, OverworldGenerator.detailResourceFor("voxel:lakeside", 0.020));
        assertEquals(Blocks.MUSHROOM_CLUSTER, OverworldGenerator.detailResourceFor("voxel:mushroom_grove", 0.010));
        assertEquals(Blocks.TREE_STUMP, OverworldGenerator.detailResourceFor("voxel:skyroot_forest", 0.010));
        assertEquals(Blocks.RED_MUSHROOM, OverworldGenerator.detailResourceFor("voxel:pine_forest", 0.018));
        assertEquals(Blocks.SMALL_STONE, OverworldGenerator.detailResourceFor("voxel:highlands", 0.010));
        assertEquals(Blocks.GLOW_CRYSTAL_NODE, OverworldGenerator.detailResourceFor("voxel:old_ruins", 0.010));
        assertEquals(Blocks.GLOW_CRYSTAL_NODE, OverworldGenerator.detailResourceFor("voxel:frost_peaks", 0.020));
        assertEquals(Blocks.SMALL_STONE, OverworldGenerator.detailResourceFor("voxel:sun_dunes", 0.010));
        assertEquals(Blocks.AIR, OverworldGenerator.detailResourceFor("voxel:old_ruins", 0.50));
    }
}
