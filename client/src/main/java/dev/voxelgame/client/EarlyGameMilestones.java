package dev.voxelgame.client;

import dev.voxelgame.common.block.ToolType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class EarlyGameMilestones {
    private final Set<String> collectedItemKeys = new HashSet<>();
    private boolean firstSupplyAnnounced;
    private boolean firstRecipeAnnounced;
    private boolean firstToolAnnounced;
    private boolean campfireCraftedAnnounced;
    private boolean campfireBuiltAnnounced;
    private boolean campfireLitAnnounced;
    private boolean storageBuiltAnnounced;

    public void reset() {
        collectedItemKeys.clear();
        firstSupplyAnnounced = false;
        firstRecipeAnnounced = false;
        firstToolAnnounced = false;
        campfireCraftedAnnounced = false;
        campfireBuiltAnnounced = false;
        campfireLitAnnounced = false;
        storageBuiltAnnounced = false;
    }

    public Optional<String> collectItem(String itemKey, String itemName) {
        if (itemKey == null || itemKey.isBlank()) {
            return Optional.empty();
        }
        collectedItemKeys.add(itemKey);
        if (firstSupplyAnnounced) {
            return Optional.empty();
        }
        firstSupplyAnnounced = true;
        return Optional.of("First supply gathered: " + cleanName(itemName));
    }

    public String unlockRecipe(String recipeLabel) {
        String label = cleanName(recipeLabel);
        if (!firstRecipeAnnounced) {
            firstRecipeAnnounced = true;
            return "First recipe unlocked: " + label;
        }
        return "Recipe unlocked: " + label;
    }

    public Optional<String> craftItem(String itemKey, String itemName, ToolType toolType) {
        if ("voxel:campfire".equals(itemKey) && !campfireCraftedAnnounced) {
            campfireCraftedAnnounced = true;
            return Optional.of("Campfire crafted. Place it before nightfall.");
        }
        if (toolType != null && toolType != ToolType.NONE && !firstToolAnnounced) {
            firstToolAnnounced = true;
            return Optional.of("First tool crafted: " + cleanName(itemName));
        }
        return Optional.empty();
    }

    public Optional<String> placeBlock(String blockKey) {
        if ("voxel:campfire".equals(blockKey) && !campfireBuiltAnnounced) {
            campfireBuiltAnnounced = true;
            return Optional.of("Campfire built. Add fuel to make a safe camp.");
        }
        if ("voxel:storage_crate".equals(blockKey) && !storageBuiltAnnounced) {
            storageBuiltAnnounced = true;
            return Optional.of("Storage crate built. Open it to stash supplies.");
        }
        return Optional.empty();
    }

    public Optional<String> lightCampfire() {
        if (campfireLitAnnounced) {
            return Optional.empty();
        }
        campfireLitAnnounced = true;
        return Optional.of("Campfire lit. Stay nearby for warmth and comfort.");
    }

    private static String cleanName(String name) {
        if (name == null || name.isBlank()) {
            return "something";
        }
        return name.strip();
    }
}
