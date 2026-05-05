package dev.voxelgame.common.world.gen;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.content.ContentTagRegistry;
import dev.voxelgame.common.gameplay.AlphaMilestoneKey;
import dev.voxelgame.common.gameplay.AlphaMilestones;
import dev.voxelgame.common.gameplay.JournalProgression;
import dev.voxelgame.common.gameplay.status.StatusEffectDefinitions;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.world.Biomes;
import dev.voxelgame.common.world.structure.StructureCatalog;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeProgressionCatalogTest {
    @Test
    void everyDefaultBiomeHasProgressionDesignCard() {
        Set<String> biomeKeys = Biomes.createDefaultRegistry().values().stream()
                .map(biome -> biome.key())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> progressionKeys = BiomeProgressionCatalog.defaultProfiles().stream()
                .map(BiomeProgressionProfile::biomeKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertEquals(biomeKeys, progressionKeys);
    }

    @Test
    void designCardsReferenceRegisteredContentAndProgressionContracts() {
        var blocks = Blocks.createDefaultRegistry();
        var items = Items.createDefaultRegistry();
        Set<String> entityKeys = ContentTagRegistry.defaultEntityKeys();

        for (BiomeProgressionProfile profile : BiomeProgressionCatalog.defaultProfiles()) {
            assertFalse(profile.silhouette().isBlank(), profile.biomeKey());
            assertFalse(profile.colorLightMood().isBlank(), profile.biomeKey());
            assertFalse(profile.dangerNotes().isEmpty(), profile.biomeKey());
            assertFalse(profile.returnReasons().isEmpty(), profile.biomeKey());
            for (short blockId : profile.resourceBlockIds()) {
                assertTrue(blocks.findById(blockId).isPresent(), profile.biomeKey() + " missing block " + blockId);
            }
            for (String entityKey : profile.ambientEntityKeys()) {
                assertTrue(entityKeys.contains(entityKey), profile.biomeKey() + " missing entity " + entityKey);
            }
            for (String structureKey : profile.structureKeys()) {
                assertTrue(StructureCatalog.find(structureKey).isPresent(), profile.biomeKey() + " missing structure " + structureKey);
            }
            for (var effectType : profile.environmentEffectTypes()) {
                assertEquals(effectType, StatusEffectDefinitions.require(effectType).type(), profile.biomeKey());
            }
            for (AlphaMilestoneKey milestoneKey : profile.milestoneKeys()) {
                assertTrue(AlphaMilestones.find(milestoneKey).isPresent(), profile.biomeKey() + " missing milestone " + milestoneKey);
            }
            for (String itemKey : profile.itemProgressionKeys()) {
                assertTrue(items.findByKey(itemKey).isPresent(), profile.biomeKey() + " missing item " + itemKey);
            }
            for (String journalKey : profile.journalEntryKeys()) {
                assertTrue(JournalProgression.findEntry(journalKey).isPresent(), profile.biomeKey() + " missing journal " + journalKey);
            }
        }
    }

    @Test
    void progressionCardsStayInSyncWithBiomeResourceProfiles() {
        for (BiomeProgressionProfile profile : BiomeProgressionCatalog.defaultProfiles()) {
            BiomeResourceProfile resources = BiomeResourceProfiles.forBiome(profile.biomeKey());
            Set<Short> biomeBlocks = new LinkedHashSet<>();
            biomeBlocks.addAll(resources.surfaceBlocks());
            biomeBlocks.addAll(resources.vegetationBlocks());
            biomeBlocks.addAll(resources.resourceBlocks());
            biomeBlocks.addAll(resources.oreBlocks());
            biomeBlocks.addAll(resources.rareFeatureBlocks());

            assertTrue(biomeBlocks.containsAll(profile.resourceBlockIds()), profile.biomeKey() + " resources drifted");
            assertTrue(resources.ambientEntityKeys().containsAll(profile.ambientEntityKeys()), profile.biomeKey() + " entities drifted");
            assertTrue(resources.structureKeys().containsAll(profile.structureKeys()), profile.biomeKey() + " structures drifted");
            assertTrue(WorldFeatureTables.detailTableForBiome(profile.biomeKey()).isPresent(), profile.biomeKey() + " missing feature table");
        }
    }

    @Test
    void coreBiomeProgressionCoversAlphaMilestoneChainAndStaysSeedRobust() {
        EnumSet<AlphaMilestoneKey> coveredMilestones = EnumSet.noneOf(AlphaMilestoneKey.class);

        for (BiomeProgressionProfile profile : BiomeProgressionCatalog.defaultProfiles()) {
            if (!profile.coreProgression()) {
                continue;
            }
            assertFalse(profile.seedRobustnessContract().isBlank(), profile.biomeKey());
            assertFalse(profile.milestoneKeys().isEmpty(), profile.biomeKey());
            coveredMilestones.addAll(profile.milestoneKeys());
        }

        assertTrue(coveredMilestones.containsAll(EnumSet.allOf(AlphaMilestoneKey.class)));
    }

    @Test
    void everyBiomeHasAReasonToReturn() {
        for (BiomeProgressionProfile profile : BiomeProgressionCatalog.defaultProfiles()) {
            assertTrue(profile.returnReasons().size() >= 2, profile.biomeKey() + " should not be one-and-done");
        }
    }
}
