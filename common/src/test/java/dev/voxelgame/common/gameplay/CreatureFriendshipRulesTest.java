package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.content.ContentKind;
import dev.voxelgame.common.content.ContentTag;
import dev.voxelgame.common.content.ContentTagRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureFriendshipRulesTest {
    @Test
    void feedableCreaturesAcceptOnlyFavoriteFoodAndStayDailyCapped() {
        ContentTagRegistry tags = ContentTagRegistry.createDefault();

        assertTrue(CreatureFriendshipRules.FEEDING_REACH_BLOCKS <= 4);
        assertTrue(CreatureFriendshipRules.FEEDING_COOLDOWN_SECONDS >= 120);
        assertTrue(CreatureFriendshipRules.MAX_ACCEPTED_FEEDS_PER_DAY <= 3);

        for (CreatureDesign design : CozyLifeProgression.defaultCreatures()) {
            if (!design.feedable()) {
                continue;
            }
            if (design.roles().contains(CreatureRole.FRIENDSHIP)) {
                assertTrue(design.friendshipSteps() > 0 && design.friendshipSteps() <= 3, design.entityKey());
            } else {
                assertTrue(design.roles().contains(CreatureRole.RARE_DANGER), design.entityKey());
                assertTrue(design.friendshipSteps() <= 1, design.entityKey());
            }
            assertTrue(CreatureFriendshipRules.canAcceptFeedToday(0));
            assertFalse(CreatureFriendshipRules.canAcceptFeedToday(CreatureFriendshipRules.MAX_ACCEPTED_FEEDS_PER_DAY));
            for (String itemKey : design.favoriteItemKeys()) {
                assertTrue(CreatureFriendshipRules.canFeed(design, itemKey), design.entityKey() + " should accept " + itemKey);
                assertFalse(tags.hasTag(ContentKind.ITEM, itemKey, ContentTag.WEAPON), itemKey);
                assertFalse(tags.hasTag(ContentKind.ITEM, itemKey, ContentTag.RUIN_PROGRESSION), itemKey);
                assertFalse(tags.hasTag(ContentKind.ITEM, itemKey, ContentTag.RARE_LOOT), itemKey);
            }
            assertFalse(CreatureFriendshipRules.canFeed(design, "voxel:stone"), design.entityKey());
        }
    }

    @Test
    void friendshipStepsClampToDesignLimitAndUseStableSaveKeys() {
        for (CreatureDesign design : CozyLifeProgression.defaultCreatures()) {
            assertEquals(0, CreatureFriendshipRules.friendshipStep(design, 0), design.entityKey());
            assertEquals(design.friendshipSteps(), CreatureFriendshipRules.friendshipStep(design, 99), design.entityKey());
            if (design.feedable()) {
                assertTrue(CreatureFriendshipRules.friendshipSaveKey(design).startsWith("player.creatures."));
                assertTrue(CreatureFriendshipRules.friendshipSaveKey(design).endsWith(".friendship"));
            }
        }
    }
}
