package dev.voxelgame.client;

import dev.voxelgame.client.audio.AudioCue;
import dev.voxelgame.common.gameplay.GameplayEvent;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameplayEventFeedbackTest {
    private static final Function<String, String> LABELER = key -> {
        int colon = key.indexOf(':');
        return (colon >= 0 ? key.substring(colon + 1) : key).replace('_', ' ');
    };

    @Test
    void mapsPickupToSuccessFeedbackAndCollectCue() {
        GameplayEventFeedback.Entry entry = describe(new GameplayEvent.Pickup(1L, "voxel:moss_clump", 3));

        assertEquals("Gathered moss clump x3", entry.message());
        assertEquals(FeedbackLog.Kind.SUCCESS, entry.kind());
        assertEquals(Optional.of(AudioCue.COLLECT_ITEM), entry.audioCue());
    }

    @Test
    void mapsCraftFailureToWarningAndFailureCue() {
        GameplayEventFeedback.Entry entry = describe(new GameplayEvent.Craft(2L, "voxel:campfire", false, "voxel:missing_fuel"));

        assertEquals("Craft failed: missing fuel", entry.message());
        assertEquals(FeedbackLog.Kind.WARNING, entry.kind());
        assertEquals(Optional.of(AudioCue.CRAFT_FAIL), entry.audioCue());
    }

    @Test
    void mapsServerUnlocksAndDiscoveriesToHighPriorityFeedback() {
        UUID playerId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        GameplayEventFeedback.Entry recipe = describe(new GameplayEvent.RecipeUnlocked(3L, playerId, "voxel:stone_pickaxe"));
        GameplayEventFeedback.Entry structure = describe(new GameplayEvent.StructureDiscovered(4L, playerId, "voxel:old_ruin"));

        assertEquals("New recipe: stone pickaxe", recipe.message());
        assertEquals(FeedbackLog.Kind.UNLOCK, recipe.kind());
        assertEquals(Optional.of(AudioCue.CRAFT_SUCCESS), recipe.audioCue());
        assertEquals("Discovered old ruin", structure.message());
        assertEquals(FeedbackLog.Kind.DISCOVERY, structure.kind());
    }

    @Test
    void mapsCriticalStatsToReadableWarnings() {
        assertEquals("Starving", describe(new GameplayEvent.StatCritical(5L, "hunger", 0)).message());
        assertEquals("Air running out", describe(new GameplayEvent.StatCritical(6L, "breath", 2)).message());
    }

    @Test
    void mapsStatusEffectsToFeedbackKinds() {
        UUID playerId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        GameplayEventFeedback.Entry cozy = describe(new GameplayEvent.StatusEffectChanged(7L, playerId, "voxel:cozy", "applied", 1));
        GameplayEventFeedback.Entry burning = describe(new GameplayEvent.StatusEffectChanged(8L, playerId, "voxel:burning", "applied", 2));
        GameplayEventFeedback.Entry expired = describe(new GameplayEvent.StatusEffectChanged(9L, playerId, "voxel:wet", "expired", 1));

        assertEquals("Status: cozy", cozy.message());
        assertEquals(FeedbackLog.Kind.COMFORT, cozy.kind());
        assertEquals("Status: burning", burning.message());
        assertEquals(FeedbackLog.Kind.WARNING, burning.kind());
        assertEquals("Status ended: wet", expired.message());
        assertEquals(FeedbackLog.Kind.INFO, expired.kind());
    }

    private static GameplayEventFeedback.Entry describe(GameplayEvent event) {
        return GameplayEventFeedback.describe(event, LABELER).orElseThrow();
    }
}
