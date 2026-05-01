package dev.voxelgame.client.viewmodel;

import dev.voxelgame.common.gameplay.StationDefinition;
import dev.voxelgame.common.gameplay.StationProgression;
import dev.voxelgame.common.item.CraftingStationType;

import java.util.List;
import java.util.Objects;

public record StationScreenViewModel(
        String stationKey,
        String title,
        String stationTypeKey,
        boolean implemented,
        String placeableItemKey,
        String blockKey,
        List<SlotGroup> slotGroups,
        List<ProgressBar> progressBars,
        List<String> primaryRecipeKeys,
        List<String> feedbackKeys,
        String screenContract
) {
    public StationScreenViewModel {
        stationKey = requireText(stationKey, "stationKey");
        title = requireText(title, "title");
        stationTypeKey = requireText(stationTypeKey, "stationTypeKey");
        placeableItemKey = placeableItemKey == null ? "" : placeableItemKey.strip();
        blockKey = blockKey == null ? "" : blockKey.strip();
        slotGroups = List.copyOf(slotGroups);
        progressBars = List.copyOf(progressBars);
        primaryRecipeKeys = List.copyOf(primaryRecipeKeys);
        feedbackKeys = List.copyOf(feedbackKeys);
        screenContract = requireText(screenContract, "screenContract");
    }

    public static StationScreenViewModel from(StationDefinition station) {
        Objects.requireNonNull(station, "station");
        CraftingStationType stationType = station.craftingStationType().orElse(null);
        return new StationScreenViewModel(
                station.key(),
                station.title(),
                station.craftingStationTypeKey(),
                station.implemented(),
                station.placeableItemKey(),
                station.blockKey(),
                slotGroupsFor(stationType, station.key()),
                progressBarsFor(stationType, station.key()),
                station.primaryRecipeKeys(),
                feedbackKeysFor(stationType, station.key()),
                station.uiScreenContract()
        );
    }

    public static List<StationScreenViewModel> implementedDefaults() {
        return StationProgression.implementedStations().stream()
                .map(StationScreenViewModel::from)
                .toList();
    }

    private static List<SlotGroup> slotGroupsFor(CraftingStationType stationType, String stationKey) {
        if (stationType == CraftingStationType.CAMPFIRE) {
            return List.of(
                    new SlotGroup("input", "Input", 1),
                    new SlotGroup("fuel", "Fuel", 1),
                    new SlotGroup("output", "Output", 1)
            );
        }
        if (stationType == CraftingStationType.COOKING_POT) {
            return List.of(
                    new SlotGroup("ingredients", "Ingredients", 4),
                    new SlotGroup("container", "Container", 1),
                    new SlotGroup("fuel", "Fuel", 1),
                    new SlotGroup("output", "Output", 1)
            );
        }
        if (stationType == CraftingStationType.FORGE) {
            return List.of(
                    new SlotGroup("ore", "Ore", 1),
                    new SlotGroup("binder", "Binder", 1),
                    new SlotGroup("fuel", "Fuel", 1),
                    new SlotGroup("output", "Output", 1)
            );
        }
        if (stationType == CraftingStationType.WORKBENCH) {
            return List.of(
                    new SlotGroup("materials", "Materials", 6),
                    new SlotGroup("output", "Output", 1)
            );
        }
        if (stationType == CraftingStationType.INVENTORY) {
            return List.of(
                    new SlotGroup("inventory", "Inventory", 36),
                    new SlotGroup("output", "Output", 1)
            );
        }
        return List.of(new SlotGroup(stationKey, "Offerings", 4));
    }

    private static List<ProgressBar> progressBarsFor(CraftingStationType stationType, String stationKey) {
        if (stationType == CraftingStationType.CAMPFIRE) {
            return List.of(
                    new ProgressBar("fuel", "Fuel"),
                    new ProgressBar("cook", "Cook")
            );
        }
        if (stationType == CraftingStationType.COOKING_POT) {
            return List.of(
                    new ProgressBar("water", "Water"),
                    new ProgressBar("cook", "Cook")
            );
        }
        if (stationType == CraftingStationType.FORGE) {
            return List.of(
                    new ProgressBar("heat", "Heat"),
                    new ProgressBar("smelt", "Smelt")
            );
        }
        return stationKey.equals(StationProgression.ANCIENT_ALTAR)
                ? List.of(new ProgressBar("restoration", "Restoration"))
                : List.of();
    }

    private static List<String> feedbackKeysFor(CraftingStationType stationType, String stationKey) {
        if (stationType == CraftingStationType.CAMPFIRE) {
            return List.of("station.feedback.fuel_missing", "station.feedback.cook_complete", "station.feedback.output_blocked");
        }
        if (stationType == CraftingStationType.COOKING_POT) {
            return List.of("station.feedback.water_missing", "station.feedback.container_missing", "station.feedback.output_blocked");
        }
        if (stationType == CraftingStationType.FORGE) {
            return List.of("station.feedback.heat_missing", "station.feedback.fuel_missing", "station.feedback.output_blocked");
        }
        if (stationType == CraftingStationType.WORKBENCH) {
            return List.of("station.feedback.station_missing", "station.feedback.material_missing");
        }
        if (stationType == CraftingStationType.INVENTORY) {
            return List.of("station.feedback.ingredient_missing", "station.feedback.inventory_full");
        }
        return List.of("station.feedback.locked." + stationKey);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }

    public record SlotGroup(String key, String label, int slots) {
        public SlotGroup {
            key = requireText(key, "key");
            label = requireText(label, "label");
            if (slots < 1) {
                throw new IllegalArgumentException("slots must be positive");
            }
        }
    }

    public record ProgressBar(String key, String label) {
        public ProgressBar {
            key = requireText(key, "key");
            label = requireText(label, "label");
        }
    }
}
