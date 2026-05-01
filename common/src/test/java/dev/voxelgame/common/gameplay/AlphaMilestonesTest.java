package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.world.Biomes;
import dev.voxelgame.common.world.structure.StructureTemplate;
import dev.voxelgame.common.world.structure.Structures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlphaMilestonesTest {
    @Test
    void defaultChainRunsFromSpawnToFirstRuinAndRareFindInOrder() {
        var chain = AlphaMilestones.defaultChain();

        assertEquals(AlphaMilestoneKey.SPAWN_SECURED, chain.getFirst().key());
        assertEquals(AlphaMilestoneKey.FIRST_RARE_FIND, chain.getLast().key());
        for (int i = 0; i < chain.size(); i++) {
            assertEquals(i + 1, chain.get(i).order(), chain.get(i).key().key());
        }
        assertTrue(AlphaMilestones.find(AlphaMilestoneKey.FIRST_RUIN_DISCOVERED).isPresent());
    }

    @Test
    void milestoneKeysAreUniqueAndPersistable() {
        Set<String> saveKeys = new HashSet<>();
        Set<AlphaMilestoneKey> keys = new HashSet<>();

        for (AlphaMilestone milestone : AlphaMilestones.defaultChain()) {
            assertTrue(keys.add(milestone.key()), milestone.key().key());
            assertTrue(saveKeys.add(milestone.saveStateKey()), milestone.saveStateKey());
            assertFalse(milestone.uiFeedback().isBlank(), milestone.key().key());
            assertFalse(milestone.serverValidation().isBlank(), milestone.key().key());
            assertFalse(milestone.smokeCheck().isBlank(), milestone.key().key());
        }
    }

    @Test
    void milestoneContentReferencesExistingItemsBlocksBiomesAndStructures() {
        var items = Items.createDefaultRegistry();
        var blocks = Blocks.createDefaultRegistry();
        var biomes = Biomes.createDefaultRegistry();
        Set<String> structures = Stream.of(
                        Structures.campsite(),
                        Structures.simpleHouse(),
                        Structures.smallRuin(),
                        Structures.watchtower(),
                        Structures.mushroomCircle()
                )
                .map(StructureTemplate::key)
                .collect(Collectors.toSet());

        for (AlphaMilestone milestone : AlphaMilestones.defaultChain()) {
            for (String itemKey : milestone.requiredItemKeys()) {
                assertTrue(items.findByKey(itemKey).isPresent(), milestone.key().key() + " missing item " + itemKey);
            }
            for (String blockKey : milestone.requiredBlockKeys()) {
                assertTrue(blocks.findByKey(blockKey).isPresent(), milestone.key().key() + " missing block " + blockKey);
            }
            for (String biomeKey : milestone.requiredBiomeKeys()) {
                assertTrue(biomes.findByKey(biomeKey).isPresent(), milestone.key().key() + " missing biome " + biomeKey);
            }
            for (String structureKey : milestone.requiredStructureKeys()) {
                assertTrue(structures.contains(structureKey), milestone.key().key() + " missing structure " + structureKey);
            }
        }
    }
}
