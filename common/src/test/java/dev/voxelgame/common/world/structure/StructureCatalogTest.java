package dev.voxelgame.common.world.structure;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.content.ContentTagRegistry;
import dev.voxelgame.common.gameplay.JournalProgression;
import dev.voxelgame.common.world.gen.BiomeResourceProfiles;
import dev.voxelgame.common.world.gen.FeatureEntry;
import dev.voxelgame.common.world.gen.WorldFeatureTables;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureCatalogTest {
    @Test
    void defaultCatalogCoversBuiltinStructures() {
        assertEquals(Set.of(
                "voxel:campsite",
                "voxel:simple_house",
                "voxel:small_ruin",
                "voxel:watchtower",
                "voxel:mushroom_circle",
                "voxel:compact_village",
                "voxel:desert_well"
        ), StructureCatalog.templateKeys());
    }

    @Test
    void entriesExposeBoundsPalettesRotationsMarkersLootAndJournalKeys() {
        Set<String> keys = new HashSet<>();

        for (StructureCatalogEntry entry : StructureCatalog.defaultEntries()) {
            assertTrue(keys.add(entry.key()), entry.key());
            assertEquals(entry.key(), entry.template().key());
            assertEquals(StructureBounds.fromTemplate(entry.template()), entry.bounds());
            assertFalse(entry.paletteKey().isBlank(), entry.key());
            assertFalse(entry.allowedRotations().isEmpty(), entry.key());
            assertTrue(entry.bounds().width() > 0, entry.key());
            assertTrue(entry.bounds().height() > 0, entry.key());
            assertTrue(entry.bounds().depth() > 0, entry.key());
            assertEquals(entry.template().markers().size(), entry.markerIds().size(), entry.key());
            assertTrue(JournalProgression.findEntry(entry.journalKey()).isPresent(), entry.key() + " missing journal entry");

            for (StructureMarker marker : entry.template().lootMarkers()) {
                assertTrue(entry.lootTableKeys().contains(marker.key()), entry.key() + " missing loot table " + marker.key());
            }
        }
    }

    @Test
    void templateBlocksAndEncounterKeysReferenceRegisteredContentOrTemplateMarkers() {
        var blocks = Blocks.createDefaultRegistry();
        Set<String> entityKeys = ContentTagRegistry.defaultEntityKeys();

        for (StructureCatalogEntry entry : StructureCatalog.defaultEntries()) {
            for (BlockPlacement block : entry.template().blocks()) {
                assertTrue(blocks.findById(block.blockId()).isPresent(), entry.key() + " missing block id " + block.blockId());
            }
            if (!entry.encounterTableKey().isBlank()) {
                assertTrue(
                        entityKeys.contains(entry.encounterTableKey()) || hasEntityMarker(entry, entry.encounterTableKey()),
                        entry.key() + " missing encounter key " + entry.encounterTableKey()
                );
            }
        }
    }

    @Test
    void biomeProfilesOnlyReferenceCatalogStructures() {
        for (var profile : BiomeResourceProfiles.all()) {
            for (String structureKey : profile.structureKeys()) {
                assertTrue(StructureCatalog.find(structureKey).isPresent(), profile.biomeKey() + " missing " + structureKey);
            }
        }
    }

    @Test
    void featureTableStructureReferencesResolveAgainstCatalogAndJournal() {
        Set<String> entityKeys = ContentTagRegistry.defaultEntityKeys();

        for (var table : WorldFeatureTables.defaultTables().values()) {
            for (FeatureEntry entry : table.entries()) {
                if (!entry.placesStructure()) {
                    continue;
                }
                StructureCatalogEntry catalogEntry = StructureCatalog.find(entry.structureKey())
                        .orElseThrow(() -> new AssertionError(entry.key() + " missing catalog entry " + entry.structureKey()));
                if (!entry.lootTableKey().isBlank()) {
                    assertTrue(catalogEntry.lootTableKeys().contains(entry.lootTableKey()), entry.key() + " missing loot table");
                }
                if (!entry.encounterTableKey().isBlank()) {
                    assertTrue(
                            entityKeys.contains(entry.encounterTableKey()) || hasEntityMarker(catalogEntry, entry.encounterTableKey()),
                            entry.key() + " missing encounter key"
                    );
                }
                if (!entry.journalKey().isBlank()) {
                    assertTrue(JournalProgression.findEntry(entry.journalKey()).isPresent(), entry.key() + " missing journal key");
                }
            }
        }
    }

    private static boolean hasEntityMarker(StructureCatalogEntry entry, String markerKey) {
        return entry.template().markers("entity").stream()
                .anyMatch(marker -> marker.key().equals(markerKey));
    }
}
