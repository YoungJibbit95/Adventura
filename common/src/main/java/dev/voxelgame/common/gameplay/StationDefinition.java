package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.item.CraftingStationType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record StationDefinition(
        String key,
        int order,
        String title,
        String craftingStationTypeKey,
        boolean implemented,
        String placeableItemKey,
        String blockKey,
        List<String> unlockItemKeys,
        List<String> unlockBiomeKeys,
        List<String> primaryRecipeKeys,
        String craftingRole,
        String blockEntityStateContract,
        String uiScreenContract,
        String saveStateContract,
        String serverTransactionContract,
        String feedbackContract,
        String lootUnlockSource
) {
    public StationDefinition {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(craftingStationTypeKey, "craftingStationTypeKey");
        placeableItemKey = placeableItemKey == null ? "" : placeableItemKey;
        blockKey = blockKey == null ? "" : blockKey;
        unlockItemKeys = List.copyOf(unlockItemKeys);
        unlockBiomeKeys = List.copyOf(unlockBiomeKeys);
        primaryRecipeKeys = List.copyOf(primaryRecipeKeys);
        Objects.requireNonNull(craftingRole, "craftingRole");
        Objects.requireNonNull(blockEntityStateContract, "blockEntityStateContract");
        Objects.requireNonNull(uiScreenContract, "uiScreenContract");
        Objects.requireNonNull(saveStateContract, "saveStateContract");
        Objects.requireNonNull(serverTransactionContract, "serverTransactionContract");
        Objects.requireNonNull(feedbackContract, "feedbackContract");
        Objects.requireNonNull(lootUnlockSource, "lootUnlockSource");
        if (order < 1) {
            throw new IllegalArgumentException("Station order must be positive");
        }
        if (key.isBlank() || title.isBlank() || craftingStationTypeKey.isBlank()) {
            throw new IllegalArgumentException("Station identity fields cannot be blank");
        }
        if (craftingRole.isBlank()
                || blockEntityStateContract.isBlank()
                || uiScreenContract.isBlank()
                || saveStateContract.isBlank()
                || serverTransactionContract.isBlank()
                || feedbackContract.isBlank()
                || lootUnlockSource.isBlank()) {
            throw new IllegalArgumentException("Station contract fields cannot be blank: " + key);
        }
    }

    public Optional<CraftingStationType> craftingStationType() {
        try {
            return Optional.of(CraftingStationType.valueOf(craftingStationTypeKey));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public boolean hasPlacedBlock() {
        return !blockKey.isBlank();
    }
}
