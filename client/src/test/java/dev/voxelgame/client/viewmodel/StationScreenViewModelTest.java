package dev.voxelgame.client.viewmodel;

import dev.voxelgame.common.gameplay.StationProgression;
import dev.voxelgame.common.item.CraftingStationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationScreenViewModelTest {
    @Test
    void implementedStationsHaveUiSlotAndFeedbackContracts() {
        List<StationScreenViewModel> viewModels = StationScreenViewModel.implementedDefaults();

        assertEquals(StationProgression.implementedStations().size(), viewModels.size());
        for (StationScreenViewModel viewModel : viewModels) {
            assertTrue(viewModel.implemented(), viewModel.stationKey());
            assertFalse(viewModel.slotGroups().isEmpty(), viewModel.stationKey());
            assertFalse(viewModel.feedbackKeys().isEmpty(), viewModel.stationKey());
            assertFalse(viewModel.screenContract().isBlank(), viewModel.stationKey());
        }
    }

    @Test
    void cookingStationsExposeExpectedProgressBars() {
        StationScreenViewModel campfire = StationScreenViewModel.from(StationProgression.forStationType(CraftingStationType.CAMPFIRE).orElseThrow());
        StationScreenViewModel forge = StationScreenViewModel.from(StationProgression.forStationType(CraftingStationType.FORGE).orElseThrow());

        assertTrue(campfire.progressBars().stream().anyMatch(bar -> bar.key().equals("cook")));
        assertTrue(forge.progressBars().stream().anyMatch(bar -> bar.key().equals("heat")));
    }
}
