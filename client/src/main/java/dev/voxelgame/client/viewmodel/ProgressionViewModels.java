package dev.voxelgame.client.viewmodel;

import dev.voxelgame.common.content.ContentTag;
import dev.voxelgame.common.content.ItemDesign;
import dev.voxelgame.common.gameplay.AlphaMilestone;
import dev.voxelgame.common.gameplay.AlphaMilestoneKey;
import dev.voxelgame.common.gameplay.AlphaMilestones;
import dev.voxelgame.common.gameplay.CreatureDesign;
import dev.voxelgame.common.gameplay.CreatureFriendshipRules;
import dev.voxelgame.common.gameplay.CreatureRole;
import dev.voxelgame.common.gameplay.GoalDefinition;
import dev.voxelgame.common.gameplay.JournalEntryDefinition;
import dev.voxelgame.common.gameplay.JournalEntryKind;
import dev.voxelgame.common.gameplay.JournalProgression;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ProgressionViewModels {
    private ProgressionViewModels() {
    }

    public static List<MilestoneProgressViewModel> milestones(Set<String> achievedKeys) {
        Set<String> safeKeys = safeSet(achievedKeys);
        return AlphaMilestones.defaultChain().stream()
                .map(milestone -> MilestoneProgressViewModel.from(milestone, safeKeys))
                .toList();
    }

    public static List<JournalEntryViewModel> journalEntries(Set<String> unlockedKeys) {
        Set<String> safeKeys = safeSet(unlockedKeys);
        return JournalProgression.defaultEntries().stream()
                .map(entry -> JournalEntryViewModel.from(entry, safeKeys))
                .toList();
    }

    public static List<GoalProgressViewModel> goals(
            Set<String> achievedMilestoneKeys,
            Set<String> unlockedJournalEntryKeys,
            Set<String> completedGoalKeys
    ) {
        Set<String> safeMilestones = safeSet(achievedMilestoneKeys);
        Set<String> safeJournal = safeSet(unlockedJournalEntryKeys);
        Set<String> safeGoals = safeSet(completedGoalKeys);
        return JournalProgression.defaultGoals().stream()
                .map(goal -> GoalProgressViewModel.from(goal, safeMilestones, safeJournal, safeGoals))
                .toList();
    }

    private static Set<String> safeSet(Set<String> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    private static boolean containsProgressKey(Set<String> keys, AlphaMilestone milestone) {
        return keys.contains(milestone.key().key())
                || keys.contains(milestone.key().name())
                || keys.contains(milestone.saveStateKey());
    }

    private static boolean containsJournalKey(Set<String> keys, JournalEntryDefinition entry) {
        return keys.contains(entry.key()) || keys.contains(entry.persistenceKey());
    }

    private static boolean containsGoalKey(Set<String> keys, GoalDefinition goal) {
        return keys.contains(goal.key()) || keys.contains(goal.persistenceKey());
    }

    public record MilestoneProgressViewModel(
            String key,
            int order,
            String title,
            boolean achieved,
            List<String> requiredItemKeys,
            List<String> requiredBlockKeys,
            List<String> requiredBiomeKeys,
            List<String> requiredStructureKeys,
            String feedback,
            String persistenceKey
    ) {
        public MilestoneProgressViewModel {
            key = requireText(key, "key");
            title = requireText(title, "title");
            requiredItemKeys = List.copyOf(requiredItemKeys);
            requiredBlockKeys = List.copyOf(requiredBlockKeys);
            requiredBiomeKeys = List.copyOf(requiredBiomeKeys);
            requiredStructureKeys = List.copyOf(requiredStructureKeys);
            feedback = requireText(feedback, "feedback");
            persistenceKey = requireText(persistenceKey, "persistenceKey");
        }

        public static MilestoneProgressViewModel from(AlphaMilestone milestone, Set<String> achievedKeys) {
            Objects.requireNonNull(milestone, "milestone");
            Set<String> safeKeys = safeSet(achievedKeys);
            return new MilestoneProgressViewModel(
                    milestone.key().key(),
                    milestone.order(),
                    milestone.title(),
                    containsProgressKey(safeKeys, milestone),
                    milestone.requiredItemKeys(),
                    milestone.requiredBlockKeys(),
                    milestone.requiredBiomeKeys(),
                    milestone.requiredStructureKeys(),
                    milestone.uiFeedback(),
                    milestone.saveStateKey()
            );
        }
    }

    public record JournalEntryViewModel(
            String key,
            JournalEntryKind kind,
            int order,
            String title,
            String summary,
            boolean unlocked,
            List<String> relatedItemKeys,
            List<String> relatedBlockKeys,
            List<String> relatedBiomeKeys,
            List<String> relatedStructureKeys,
            List<String> relatedEntityKeys,
            List<String> relatedRecipeKeys,
            List<String> milestoneKeys,
            String persistenceKey
    ) {
        public JournalEntryViewModel {
            key = requireText(key, "key");
            Objects.requireNonNull(kind, "kind");
            title = requireText(title, "title");
            summary = requireText(summary, "summary");
            relatedItemKeys = List.copyOf(relatedItemKeys);
            relatedBlockKeys = List.copyOf(relatedBlockKeys);
            relatedBiomeKeys = List.copyOf(relatedBiomeKeys);
            relatedStructureKeys = List.copyOf(relatedStructureKeys);
            relatedEntityKeys = List.copyOf(relatedEntityKeys);
            relatedRecipeKeys = List.copyOf(relatedRecipeKeys);
            milestoneKeys = List.copyOf(milestoneKeys);
            persistenceKey = requireText(persistenceKey, "persistenceKey");
        }

        public static JournalEntryViewModel from(JournalEntryDefinition entry, Set<String> unlockedKeys) {
            Objects.requireNonNull(entry, "entry");
            Set<String> safeKeys = safeSet(unlockedKeys);
            return new JournalEntryViewModel(
                    entry.key(),
                    entry.kind(),
                    entry.order(),
                    entry.title(),
                    entry.summary(),
                    containsJournalKey(safeKeys, entry),
                    entry.relatedItemKeys(),
                    entry.relatedBlockKeys(),
                    entry.relatedBiomeKeys(),
                    entry.relatedStructureKeys(),
                    entry.relatedEntityKeys(),
                    entry.relatedRecipeKeys(),
                    entry.milestoneKeys().stream().map(AlphaMilestoneKey::key).toList(),
                    entry.persistenceKey()
            );
        }
    }

    public record GoalProgressViewModel(
            String key,
            int order,
            String title,
            String summary,
            boolean completed,
            int achievedMilestones,
            int totalMilestones,
            int unlockedJournalEntries,
            int totalJournalEntries,
            String persistenceKey,
            String feedback
    ) {
        public GoalProgressViewModel {
            key = requireText(key, "key");
            title = requireText(title, "title");
            summary = requireText(summary, "summary");
            persistenceKey = requireText(persistenceKey, "persistenceKey");
            feedback = requireText(feedback, "feedback");
            if (achievedMilestones < 0 || totalMilestones < 1 || unlockedJournalEntries < 0 || totalJournalEntries < 0) {
                throw new IllegalArgumentException("Invalid goal progress counts");
            }
        }

        public static GoalProgressViewModel from(
                GoalDefinition goal,
                Set<String> achievedMilestoneKeys,
                Set<String> unlockedJournalEntryKeys,
                Set<String> completedGoalKeys
        ) {
            Objects.requireNonNull(goal, "goal");
            Set<String> safeMilestones = safeSet(achievedMilestoneKeys);
            Set<String> safeJournal = safeSet(unlockedJournalEntryKeys);
            Set<String> safeGoals = safeSet(completedGoalKeys);
            int achieved = 0;
            for (AlphaMilestoneKey milestoneKey : goal.milestoneKeys()) {
                AlphaMilestone milestone = AlphaMilestones.find(milestoneKey).orElseThrow();
                if (containsProgressKey(safeMilestones, milestone)) {
                    achieved++;
                }
            }
            int unlocked = 0;
            for (String journalEntryKey : goal.journalEntryKeys()) {
                JournalEntryDefinition entry = JournalProgression.findEntry(journalEntryKey).orElseThrow();
                if (containsJournalKey(safeJournal, entry)) {
                    unlocked++;
                }
            }
            boolean requirementsComplete = achieved == goal.milestoneKeys().size()
                    && unlocked == goal.journalEntryKeys().size();
            return new GoalProgressViewModel(
                    goal.key(),
                    goal.order(),
                    goal.title(),
                    goal.summary(),
                    containsGoalKey(safeGoals, goal) || requirementsComplete,
                    achieved,
                    goal.milestoneKeys().size(),
                    unlocked,
                    goal.journalEntryKeys().size(),
                    goal.persistenceKey(),
                    goal.uiFeedback()
            );
        }
    }

    public record CreatureInfoViewModel(
            String entityKey,
            String displayName,
            List<CreatureRole> roles,
            List<String> biomeKeys,
            List<String> favoriteItemKeys,
            List<String> resourceItemKeys,
            int friendshipStep,
            int maxFriendshipSteps,
            boolean canAcceptFeedToday,
            String feedback
    ) {
        public CreatureInfoViewModel {
            entityKey = requireText(entityKey, "entityKey");
            displayName = requireText(displayName, "displayName");
            roles = List.copyOf(roles);
            biomeKeys = List.copyOf(biomeKeys);
            favoriteItemKeys = List.copyOf(favoriteItemKeys);
            resourceItemKeys = List.copyOf(resourceItemKeys);
            feedback = requireText(feedback, "feedback");
        }

        public static CreatureInfoViewModel from(CreatureDesign design, int acceptedFeedsTotal, int acceptedFeedsToday) {
            Objects.requireNonNull(design, "design");
            return new CreatureInfoViewModel(
                    design.entityKey(),
                    design.displayName(),
                    design.roles().stream().sorted(Comparator.comparing(Enum::name)).toList(),
                    design.biomeKeys(),
                    design.favoriteItemKeys(),
                    design.resourceItemKeys(),
                    CreatureFriendshipRules.friendshipStep(design, acceptedFeedsTotal),
                    design.friendshipSteps(),
                    CreatureFriendshipRules.canAcceptFeedToday(acceptedFeedsToday),
                    design.uiFeedback()
            );
        }
    }

    public record ItemSourceTooltipViewModel(
            String itemKey,
            String role,
            String primaryUse,
            String unlock,
            List<String> biomeSources,
            List<String> entitySources,
            List<String> structureSources,
            List<String> lootTableKeys,
            List<ContentTag> tags
    ) {
        public ItemSourceTooltipViewModel {
            itemKey = requireText(itemKey, "itemKey");
            role = requireText(role, "role");
            primaryUse = requireText(primaryUse, "primaryUse");
            unlock = requireText(unlock, "unlock");
            biomeSources = List.copyOf(biomeSources);
            entitySources = List.copyOf(entitySources);
            structureSources = List.copyOf(structureSources);
            lootTableKeys = List.copyOf(lootTableKeys);
            tags = List.copyOf(tags);
        }

        public static ItemSourceTooltipViewModel from(ItemDesign design) {
            Objects.requireNonNull(design, "design");
            return new ItemSourceTooltipViewModel(
                    design.itemKey(),
                    design.role(),
                    design.primaryUse(),
                    design.unlock(),
                    design.biomeSources(),
                    design.entitySources(),
                    design.structureSources(),
                    design.lootTableKeys(),
                    design.tags().stream().sorted(Comparator.comparing(Enum::name)).toList()
            );
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.strip();
    }
}
