package dev.voxelgame.common.gameplay;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record CreatureDesign(
        String entityKey,
        int order,
        String displayName,
        CreatureDisposition disposition,
        Set<CreatureRole> roles,
        List<String> biomeKeys,
        List<String> favoriteItemKeys,
        List<String> hintTargetKeys,
        List<String> resourceItemKeys,
        int comfortContribution,
        int friendshipSteps,
        String feedingContract,
        String friendshipContract,
        String interactionContract,
        String animationContract,
        String networkStateContract,
        String saveStateContract,
        String uiFeedback
) {
    public CreatureDesign {
        Objects.requireNonNull(entityKey, "entityKey");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(disposition, "disposition");
        Objects.requireNonNull(roles, "roles");
        Objects.requireNonNull(biomeKeys, "biomeKeys");
        Objects.requireNonNull(favoriteItemKeys, "favoriteItemKeys");
        Objects.requireNonNull(hintTargetKeys, "hintTargetKeys");
        Objects.requireNonNull(resourceItemKeys, "resourceItemKeys");
        Objects.requireNonNull(feedingContract, "feedingContract");
        Objects.requireNonNull(friendshipContract, "friendshipContract");
        Objects.requireNonNull(interactionContract, "interactionContract");
        Objects.requireNonNull(animationContract, "animationContract");
        Objects.requireNonNull(networkStateContract, "networkStateContract");
        Objects.requireNonNull(saveStateContract, "saveStateContract");
        Objects.requireNonNull(uiFeedback, "uiFeedback");

        if (entityKey.isBlank() || displayName.isBlank()) {
            throw new IllegalArgumentException("Creature identity fields must not be blank");
        }
        if (order < 1) {
            throw new IllegalArgumentException("Creature order must be >= 1");
        }
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("Creature roles must not be empty");
        }
        if (biomeKeys.isEmpty()) {
            throw new IllegalArgumentException("Creature biome keys must not be empty");
        }
        if (comfortContribution < 0) {
            throw new IllegalArgumentException("Comfort contribution must be >= 0");
        }
        if (friendshipSteps < 0 || friendshipSteps > 3) {
            throw new IllegalArgumentException("Friendship steps must be between 0 and 3");
        }
        if (feedingContract.isBlank()
                || friendshipContract.isBlank()
                || interactionContract.isBlank()
                || animationContract.isBlank()
                || networkStateContract.isBlank()
                || saveStateContract.isBlank()
                || uiFeedback.isBlank()) {
            throw new IllegalArgumentException("Creature contracts must not be blank");
        }

        EnumSet<CreatureRole> roleCopy = EnumSet.copyOf(roles);
        if (roleCopy.contains(CreatureRole.FRIENDSHIP) && favoriteItemKeys.isEmpty()) {
            throw new IllegalArgumentException("Friendship creatures need favorite items");
        }
        if (roleCopy.contains(CreatureRole.BASE_COMFORT) && comfortContribution < 1) {
            throw new IllegalArgumentException("Base comfort creatures need positive comfort contribution");
        }
        if (roleCopy.contains(CreatureRole.RESOURCE) && resourceItemKeys.isEmpty()) {
            throw new IllegalArgumentException("Resource creatures need resource item keys");
        }
        if (roleCopy.contains(CreatureRole.HINT_GIVER) && hintTargetKeys.isEmpty()) {
            throw new IllegalArgumentException("Hint giver creatures need hint targets");
        }

        roles = Set.copyOf(roleCopy);
        biomeKeys = List.copyOf(biomeKeys);
        favoriteItemKeys = List.copyOf(favoriteItemKeys);
        hintTargetKeys = List.copyOf(hintTargetKeys);
        resourceItemKeys = List.copyOf(resourceItemKeys);
    }

    public boolean feedable() {
        return !favoriteItemKeys.isEmpty();
    }
}
