package dev.voxelgame.client.viewmodel;

import dev.voxelgame.common.content.AlphaItemDesigns;
import dev.voxelgame.common.content.ContentTag;
import dev.voxelgame.common.gameplay.AlphaMilestoneKey;
import dev.voxelgame.common.gameplay.CozyLifeProgression;
import dev.voxelgame.common.gameplay.CreatureRole;
import dev.voxelgame.common.gameplay.JournalProgression;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionViewModelsTest {
    @Test
    void milestoneAndGoalViewModelsConsumePersistedKeys() {
        var milestones = ProgressionViewModels.milestones(Set.of(AlphaMilestoneKey.FIRST_SUPPLY.key()));
        var firstSupply = milestones.stream()
                .filter(model -> model.key().equals(AlphaMilestoneKey.FIRST_SUPPLY.key()))
                .findFirst()
                .orElseThrow();

        assertTrue(firstSupply.achieved());
        assertFalse(firstSupply.requiredItemKeys().isEmpty());

        var firstGoal = JournalProgression.defaultGoals().getFirst();
        var goals = ProgressionViewModels.goals(
                firstGoal.milestoneKeys().stream().map(AlphaMilestoneKey::key).collect(java.util.stream.Collectors.toSet()),
                Set.copyOf(firstGoal.journalEntryKeys()),
                Set.of()
        );

        assertTrue(goals.getFirst().completed());
        assertEquals(firstGoal.milestoneKeys().size(), goals.getFirst().achievedMilestones());
    }

    @Test
    void journalCreatureAndItemTooltipViewModelsExposeCommonContracts() {
        var journalEntry = JournalProgression.defaultEntries().getFirst();
        var journal = ProgressionViewModels.journalEntries(Set.of(journalEntry.persistenceKey())).getFirst();
        var creature = CozyLifeProgression.creaturesWithRole(CreatureRole.FRIENDSHIP).getFirst();
        var creatureView = ProgressionViewModels.CreatureInfoViewModel.from(creature, 2, 1);
        var item = AlphaItemDesigns.createDefault().get("voxel:twig");
        var itemTooltip = ProgressionViewModels.ItemSourceTooltipViewModel.from(item);

        assertTrue(journal.unlocked());
        assertEquals(creature.displayName(), creatureView.displayName());
        assertTrue(creatureView.friendshipStep() <= creatureView.maxFriendshipSteps());
        assertTrue(itemTooltip.tags().contains(ContentTag.FUEL));
        assertFalse(itemTooltip.biomeSources().isEmpty());
    }
}
