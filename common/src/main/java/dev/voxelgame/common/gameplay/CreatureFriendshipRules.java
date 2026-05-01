package dev.voxelgame.common.gameplay;

import java.util.Objects;

public final class CreatureFriendshipRules {
    public static final int FEEDING_REACH_BLOCKS = 4;
    public static final int FEEDING_COOLDOWN_SECONDS = 180;
    public static final int MAX_ACCEPTED_FEEDS_PER_DAY = 3;

    private CreatureFriendshipRules() {
    }

    public static boolean canFeed(CreatureDesign design, String itemKey) {
        Objects.requireNonNull(design, "design");
        Objects.requireNonNull(itemKey, "itemKey");
        return design.feedable() && design.favoriteItemKeys().contains(itemKey);
    }

    public static boolean canAcceptFeedToday(int acceptedFeedsToday) {
        return acceptedFeedsToday >= 0 && acceptedFeedsToday < MAX_ACCEPTED_FEEDS_PER_DAY;
    }

    public static int friendshipStep(CreatureDesign design, int acceptedFeedsTotal) {
        Objects.requireNonNull(design, "design");
        if (acceptedFeedsTotal <= 0 || design.friendshipSteps() == 0) {
            return 0;
        }
        return Math.min(design.friendshipSteps(), acceptedFeedsTotal);
    }

    public static String friendshipSaveKey(CreatureDesign design) {
        Objects.requireNonNull(design, "design");
        return "player.creatures." + design.entityKey().replace("voxel:", "").replace(':', '_') + ".friendship";
    }
}
