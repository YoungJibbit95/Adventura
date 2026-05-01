package dev.voxelgame.common.content;

import dev.voxelgame.common.gameplay.AlphaMilestones;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.loot.LootTables;
import dev.voxelgame.common.world.Biomes;
import dev.voxelgame.common.world.structure.StructureTemplate;
import dev.voxelgame.common.world.structure.Structures;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlphaItemDesignsTest {
    @Test
    void everyMilestoneRequiredItemHasDesignData() {
        Map<String, ItemDesign> designs = AlphaItemDesigns.createDefault();

        for (String itemKey : AlphaMilestones.requiredItemKeys()) {
            assertTrue(designs.containsKey(itemKey), "Missing item design for " + itemKey);
        }
    }

    @Test
    void designRowsMirrorRegistryBalancingFieldsAndTags() {
        var items = Items.createDefaultRegistry();
        ContentTagRegistry tags = ContentTagRegistry.createDefault();

        for (ItemDesign design : AlphaItemDesigns.createDefault(items, tags).values()) {
            var item = items.requireByKey(design.itemKey());
            assertEquals(item.maxStackSize(), design.stackSize(), design.itemKey());
            assertEquals(item.foodValue(), design.foodValue(), design.itemKey());
            assertEquals(item.healValue(), design.healValue(), design.itemKey());
            assertEquals(item.toolLevel(), design.toolTier(), design.itemKey());
            assertEquals(item.durability(), design.durability(), design.itemKey());
            assertEquals(tags.tagsFor(ContentKind.ITEM, design.itemKey()), design.tags(), design.itemKey());
            assertTrue(design.hasWorldSource(), design.itemKey());
            assertFalse(design.uiFeedback().isBlank(), design.itemKey());
            assertFalse(design.saveNetworkContract().isBlank(), design.itemKey());
        }
    }

    @Test
    void designSourcesReferenceKnownBiomesStructuresAndLootTables() {
        var biomes = Biomes.createDefaultRegistry();
        var lootTables = LootTables.createDefaultRegistry();
        Set<String> structures = Stream.of(
                        Structures.campsite(),
                        Structures.simpleHouse(),
                        Structures.smallRuin(),
                        Structures.watchtower(),
                        Structures.mushroomCircle()
                )
                .map(StructureTemplate::key)
                .collect(Collectors.toSet());

        for (ItemDesign design : AlphaItemDesigns.createDefault().values()) {
            for (String biomeKey : design.biomeSources()) {
                assertTrue(biomes.findByKey(biomeKey).isPresent(), design.itemKey() + " missing biome " + biomeKey);
            }
            for (String structureKey : design.structureSources()) {
                assertTrue(structures.contains(structureKey), design.itemKey() + " missing structure " + structureKey);
            }
            for (String lootTableKey : design.lootTableKeys()) {
                assertTrue(lootTables.findByKey(lootTableKey).isPresent(), design.itemKey() + " missing loot table " + lootTableKey);
            }
        }
    }
}
