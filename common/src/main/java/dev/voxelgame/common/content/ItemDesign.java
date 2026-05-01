package dev.voxelgame.common.content;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ItemDesign(
        String itemKey,
        String role,
        List<String> biomeSources,
        List<String> entitySources,
        List<String> structureSources,
        String primaryUse,
        String unlock,
        String uiFeedback,
        String saveNetworkContract,
        String uiIconKey,
        List<String> lootTableKeys,
        int stackSize,
        int foodValue,
        int healValue,
        int toolTier,
        int durability,
        Set<ContentTag> tags
) {
    public ItemDesign {
        Objects.requireNonNull(itemKey, "itemKey");
        Objects.requireNonNull(role, "role");
        biomeSources = List.copyOf(biomeSources);
        entitySources = List.copyOf(entitySources);
        structureSources = List.copyOf(structureSources);
        Objects.requireNonNull(primaryUse, "primaryUse");
        Objects.requireNonNull(unlock, "unlock");
        Objects.requireNonNull(uiFeedback, "uiFeedback");
        Objects.requireNonNull(saveNetworkContract, "saveNetworkContract");
        Objects.requireNonNull(uiIconKey, "uiIconKey");
        lootTableKeys = List.copyOf(lootTableKeys);
        tags = Set.copyOf(tags);
        if (itemKey.isBlank() || role.isBlank() || primaryUse.isBlank() || unlock.isBlank()) {
            throw new IllegalArgumentException("Item design text fields cannot be blank");
        }
        if (stackSize < 1 || foodValue < 0 || healValue < 0 || toolTier < 0 || durability < 0) {
            throw new IllegalArgumentException("Item design numeric fields are invalid: " + itemKey);
        }
        if (tags.isEmpty()) {
            throw new IllegalArgumentException("Item design needs content tags: " + itemKey);
        }
    }

    public boolean hasWorldSource() {
        return !biomeSources.isEmpty() || !entitySources.isEmpty() || !structureSources.isEmpty();
    }
}
