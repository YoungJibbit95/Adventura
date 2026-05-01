package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.content.ContentTagRegistry;
import dev.voxelgame.common.item.CraftingRecipes;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.world.Biomes;
import dev.voxelgame.common.world.structure.StructureTemplate;
import dev.voxelgame.common.world.structure.Structures;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalProgressionTest {
    @Test
    void defaultEntriesCoverP94JournalShapesAndRuinProgression() {
        Set<JournalEntryKind> kinds = JournalProgression.defaultEntries().stream()
                .map(JournalEntryDefinition::kind)
                .collect(Collectors.toSet());

        assertTrue(kinds.containsAll(EnumSet.of(
                JournalEntryKind.BIOME_NOTE,
                JournalEntryKind.CREATURE_NOTE,
                JournalEntryKind.STRUCTURE_NOTE,
                JournalEntryKind.RECIPE_HISTORY,
                JournalEntryKind.LORE_PAGE,
                JournalEntryKind.MAP_FRAGMENT
        )));
        assertFalse(JournalProgression.entriesOfKind(JournalEntryKind.LORE_PAGE).isEmpty());
        assertFalse(JournalProgression.entriesOfKind(JournalEntryKind.MAP_FRAGMENT).isEmpty());
        assertTrue(JournalProgression.findEntry(JournalProgression.FIRST_RUIN_MAP).isPresent());
    }

    @Test
    void journalEntriesAreUniqueOrderedAndPersistable() {
        Set<String> keys = new HashSet<>();
        Set<String> persistenceKeys = new HashSet<>();

        var entries = JournalProgression.defaultEntries();
        for (int i = 0; i < entries.size(); i++) {
            JournalEntryDefinition entry = entries.get(i);
            assertEquals(i + 1, entry.order(), entry.key());
            assertTrue(keys.add(entry.key()), entry.key());
            assertTrue(persistenceKeys.add(entry.persistenceKey()), entry.persistenceKey());
            assertFalse(entry.discoveryEventKeys().isEmpty(), entry.key());
            assertFalse(entry.uiFeedback().isBlank(), entry.key());
            assertFalse(entry.serverEventContract().isBlank(), entry.key());
        }
    }

    @Test
    void journalEntriesReferenceRegisteredContent() {
        var items = Items.createDefaultRegistry();
        var blocks = Blocks.createDefaultRegistry();
        var biomes = Biomes.createDefaultRegistry();
        Set<String> recipeKeys = CraftingRecipes.createDefaultRecipes(items).stream()
                .map(recipe -> recipe.key())
                .collect(Collectors.toSet());
        Set<String> entityKeys = ContentTagRegistry.defaultEntityKeys();
        Set<String> structures = Stream.of(
                        Structures.campsite(),
                        Structures.simpleHouse(),
                        Structures.smallRuin(),
                        Structures.watchtower(),
                        Structures.mushroomCircle(),
                        Structures.compactVillage()
                )
                .map(StructureTemplate::key)
                .collect(Collectors.toSet());

        for (JournalEntryDefinition entry : JournalProgression.defaultEntries()) {
            for (String itemKey : entry.relatedItemKeys()) {
                assertTrue(items.findByKey(itemKey).isPresent(), entry.key() + " missing item " + itemKey);
            }
            for (String blockKey : entry.relatedBlockKeys()) {
                assertTrue(blocks.findByKey(blockKey).isPresent(), entry.key() + " missing block " + blockKey);
            }
            for (String biomeKey : entry.relatedBiomeKeys()) {
                assertTrue(biomes.findByKey(biomeKey).isPresent(), entry.key() + " missing biome " + biomeKey);
            }
            for (String structureKey : entry.relatedStructureKeys()) {
                assertTrue(structures.contains(structureKey), entry.key() + " missing structure " + structureKey);
            }
            for (String entityKey : entry.relatedEntityKeys()) {
                assertTrue(entityKeys.contains(entityKey), entry.key() + " missing entity " + entityKey);
            }
            for (String recipeKey : entry.relatedRecipeKeys()) {
                assertTrue(recipeKeys.contains(recipeKey), entry.key() + " missing recipe " + recipeKey);
            }
        }
    }

    @Test
    void ruinProgressionItemsHaveDedicatedLoreEntries() {
        Set<String> loreItemKeys = JournalProgression.entriesOfKind(JournalEntryKind.LORE_PAGE).stream()
                .flatMap(entry -> entry.relatedItemKeys().stream())
                .collect(Collectors.toSet());

        assertTrue(loreItemKeys.containsAll(JournalProgression.ruinProgressionItemKeys()));
        assertEquals(Set.of(
                "voxel:ancient_fragment",
                "voxel:ruin_key",
                "voxel:ruin_seal",
                "voxel:lost_charm",
                "voxel:ancient_lantern"
        ), JournalProgression.ruinProgressionItemKeys());
    }

    @Test
    void goalsAreSmallMilestoneBackedAndReferenceJournalEntries() {
        Set<String> goalKeys = new HashSet<>();
        Set<String> persistenceKeys = new HashSet<>();

        for (GoalDefinition goal : JournalProgression.defaultGoals()) {
            assertTrue(goalKeys.add(goal.key()), goal.key());
            assertTrue(persistenceKeys.add(goal.persistenceKey()), goal.persistenceKey());
            assertFalse(goal.milestoneKeys().isEmpty(), goal.key());
            assertTrue(goal.milestoneKeys().size() <= 4, goal.key() + " should stay lightweight");
            assertFalse(goal.uiFeedback().isBlank(), goal.key());
            assertFalse(goal.serverEventContract().isBlank(), goal.key());
            for (AlphaMilestoneKey milestoneKey : goal.milestoneKeys()) {
                assertTrue(AlphaMilestones.find(milestoneKey).isPresent(), goal.key() + " missing milestone " + milestoneKey);
            }
            for (String journalEntryKey : goal.journalEntryKeys()) {
                assertTrue(JournalProgression.findEntry(journalEntryKey).isPresent(), goal.key() + " missing journal entry " + journalEntryKey);
            }
        }
    }
}
