package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.world.Biomes;
import dev.voxelgame.common.world.structure.StructureTemplate;
import dev.voxelgame.common.world.structure.Structures;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldFeatureTablesTest {
    @Test
    void requiredP22FeatureTablesExist() {
        Set<String> tableKeys = WorldFeatureTables.defaultTables().keySet();

        assertTrue(tableKeys.contains(WorldFeatureTables.MEADOW_SURFACE_FEATURES));
        assertTrue(tableKeys.contains(WorldFeatureTables.PINE_FOREST_FEATURES));
        assertTrue(tableKeys.contains(WorldFeatureTables.MUSHROOM_GROVE_FEATURES));
        assertTrue(tableKeys.contains(WorldFeatureTables.LAKESIDE_FEATURES));
        assertTrue(tableKeys.contains(WorldFeatureTables.OLD_RUINS_FEATURES));
        assertTrue(tableKeys.contains(WorldFeatureTables.HIGHLANDS_FEATURES));
        assertTrue(tableKeys.contains(WorldFeatureTables.FROST_FEATURES));
    }

    @Test
    void tablesCoverFeatureKindsNeededByWorldContentPipeline() {
        Set<FeatureKind> kinds = WorldFeatureTables.defaultTables().values().stream()
                .flatMap(table -> table.entries().stream())
                .map(FeatureEntry::kind)
                .collect(Collectors.toSet());

        assertTrue(kinds.containsAll(EnumSet.of(
                FeatureKind.RESOURCE_NODE,
                FeatureKind.PLANT_CLUSTER,
                FeatureKind.ROCK,
                FeatureKind.TREE,
                FeatureKind.RUIN,
                FeatureKind.CAMPSITE,
                FeatureKind.VILLAGE_PART
        )));
    }

    @Test
    void featureEntriesReferenceRegisteredBiomesBlocksAndStructures() {
        var biomes = Biomes.createDefaultRegistry();
        var blocks = Blocks.createDefaultRegistry();
        Set<String> structureKeys = Stream.of(
                        Structures.campsite(),
                        Structures.simpleHouse(),
                        Structures.smallRuin(),
                        Structures.watchtower(),
                        Structures.mushroomCircle(),
                        Structures.compactVillage(),
                        Structures.desertWell()
                )
                .map(StructureTemplate::key)
                .collect(Collectors.toSet());

        for (FeatureTable table : WorldFeatureTables.defaultTables().values()) {
            assertFalse(table.entries().isEmpty(), table.key());
            for (FeatureEntry entry : table.entries()) {
                assertTrue(entry.weight() > 0.0, entry.key());
                assertTrue(entry.minCount() <= entry.maxCount(), entry.key());
                assertFalse(entry.allowedBiomeKeys().isEmpty(), entry.key());
                assertFalse(entry.requiredSurfaceBlockIds().isEmpty(), entry.key());
                for (String biomeKey : entry.allowedBiomeKeys()) {
                    assertTrue(biomes.findByKey(biomeKey).isPresent(), entry.key() + " missing biome " + biomeKey);
                }
                for (short surfaceBlockId : entry.requiredSurfaceBlockIds()) {
                    assertTrue(blocks.findById(surfaceBlockId).isPresent(), entry.key() + " missing surface " + surfaceBlockId);
                }
                if (entry.placesBlock()) {
                    assertTrue(blocks.findById(entry.blockId()).isPresent(), entry.key() + " missing block " + entry.blockId());
                }
                if (entry.placesStructure()) {
                    assertTrue(structureKeys.contains(entry.structureKey()), entry.key() + " missing structure " + entry.structureKey());
                }
            }
        }
    }

    @Test
    void detailResourceSelectionPreservesExistingSmokeRolls() {
        assertEquals(Blocks.BERRY_BUSH, WorldFeatureTables.detailResourceFor("voxel:cozy_meadow", 0.010));
        assertEquals(Blocks.HERB_PLANTER, WorldFeatureTables.detailResourceFor("voxel:cozy_meadow", 0.020));
        assertEquals(Blocks.SUN_BLOOM, WorldFeatureTables.detailResourceFor("voxel:flower_fields", 0.010));
        assertEquals(Blocks.CLAY_DEPOSIT, WorldFeatureTables.detailResourceFor("voxel:lakeside", 0.010));
        assertEquals(Blocks.REEDS, WorldFeatureTables.detailResourceFor("voxel:lakeside", 0.020));
        assertEquals(Blocks.SPORE_BLOSSOM, WorldFeatureTables.detailResourceFor("voxel:mushroom_grove", 0.003));
        assertEquals(Blocks.GLOW_MUSHROOM, WorldFeatureTables.detailResourceFor("voxel:mushroom_grove", 0.010));
        assertEquals(Blocks.MUSHROOM_CLUSTER, WorldFeatureTables.detailResourceFor("voxel:mushroom_grove", 0.020));
        assertEquals(Blocks.TREE_STUMP, WorldFeatureTables.detailResourceFor("voxel:skyroot_forest", 0.010));
        assertEquals(Blocks.RED_MUSHROOM, WorldFeatureTables.detailResourceFor("voxel:pine_forest", 0.018));
        assertEquals(Blocks.SMALL_STONE, WorldFeatureTables.detailResourceFor("voxel:highlands", 0.010));
        assertEquals(Blocks.GLOW_CRYSTAL_NODE, WorldFeatureTables.detailResourceFor("voxel:old_ruins", 0.010));
        assertEquals(Blocks.GLOW_CRYSTAL_NODE, WorldFeatureTables.detailResourceFor("voxel:frost_peaks", 0.020));
        assertEquals(Blocks.SMALL_STONE, WorldFeatureTables.detailResourceFor("voxel:sun_dunes", 0.010));
        assertEquals(Blocks.AIR, WorldFeatureTables.detailResourceFor("voxel:old_ruins", 0.50));
    }

    @Test
    void everyDefaultBiomeHasDetailFeatureTable() {
        for (var biome : Biomes.createDefaultRegistry().values()) {
            assertTrue(WorldFeatureTables.detailTableForBiome(biome.key()).isPresent(), biome.key());
        }
    }
}
