package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.item.CraftingRecipes;
import dev.voxelgame.common.item.CraftingStationType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.world.Biomes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationProgressionTest {
    @Test
    void defaultChainDefinesAlphaStationOrderAndFutureAncientStation() {
        var chain = StationProgression.defaultChain();

        assertEquals(StationProgression.INVENTORY, chain.getFirst().key());
        assertEquals(StationProgression.ANCIENT_ALTAR, chain.getLast().key());
        for (int i = 0; i < chain.size(); i++) {
            assertEquals(i + 1, chain.get(i).order(), chain.get(i).key());
        }
        assertTrue(StationProgression.find(StationProgression.FORGE).orElseThrow().implemented());
        assertFalse(StationProgression.find(StationProgression.ANCIENT_ALTAR).orElseThrow().implemented());
        assertTrue(StationProgression.find(StationProgression.ANCIENT_ALTAR).orElseThrow().craftingStationType().isEmpty());
    }

    @Test
    void implementedStationsHaveRegisteredContentAndUsableBlocks() {
        var items = Items.createDefaultRegistry();
        var blocks = Blocks.createDefaultRegistry();
        var biomes = Biomes.createDefaultRegistry();
        Set<String> recipeKeys = CraftingRecipes.createDefaultRecipes(items).stream()
                .map(recipe -> recipe.key())
                .collect(Collectors.toSet());

        for (StationDefinition station : StationProgression.implementedStations()) {
            assertFalse(station.craftingRole().isBlank(), station.key());
            assertFalse(station.blockEntityStateContract().isBlank(), station.key());
            assertFalse(station.uiScreenContract().isBlank(), station.key());
            assertFalse(station.saveStateContract().isBlank(), station.key());
            assertFalse(station.serverTransactionContract().isBlank(), station.key());
            assertFalse(station.feedbackContract().isBlank(), station.key());

            if (!station.placeableItemKey().isBlank()) {
                assertTrue(items.findByKey(station.placeableItemKey()).isPresent(), station.key());
            }
            if (station.hasPlacedBlock()) {
                var block = blocks.requireByKey(station.blockKey());
                CraftingStationType type = station.craftingStationType().orElseThrow();
                if (type != CraftingStationType.INVENTORY) {
                    assertTrue(CraftingStationRules.accepts(type, block.id()), station.key() + " block should satisfy station rules");
                }
            }
            for (String itemKey : station.unlockItemKeys()) {
                assertTrue(items.findByKey(itemKey).isPresent(), station.key() + " missing unlock item " + itemKey);
            }
            for (String biomeKey : station.unlockBiomeKeys()) {
                assertTrue(biomes.findByKey(biomeKey).isPresent(), station.key() + " missing biome " + biomeKey);
            }
            for (String recipeKey : station.primaryRecipeKeys()) {
                assertTrue(recipeKeys.contains(recipeKey), station.key() + " missing recipe " + recipeKey);
            }
        }
    }

    @Test
    void everyDefaultRecipeStationHasProgressionContract() {
        var items = Items.createDefaultRegistry();

        for (var recipe : CraftingRecipes.createDefaultRecipes(items)) {
            assertTrue(
                    StationProgression.forStationType(recipe.stationType()).isPresent(),
                    recipe.key() + " missing station contract for " + recipe.stationType()
            );
        }
    }

    @Test
    void stationKeysAndCraftingStationTypesAreUnique() {
        Set<String> keys = new HashSet<>();
        Set<CraftingStationType> stationTypes = new HashSet<>();

        for (StationDefinition station : StationProgression.defaultChain()) {
            assertTrue(keys.add(station.key()), station.key());
            station.craftingStationType().ifPresent(type -> assertTrue(stationTypes.add(type), type.name()));
        }
    }
}
