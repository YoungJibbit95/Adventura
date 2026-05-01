package dev.voxelgame.client;

import dev.voxelgame.client.audio.AudioCue;
import dev.voxelgame.common.gameplay.GameplayEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class GameplayEventFeedback {
    private GameplayEventFeedback() {
    }

    public static Optional<Entry> describe(GameplayEvent event, Function<String, String> labeler) {
        Objects.requireNonNull(event, "event");
        Function<String, String> safeLabeler = labeler == null ? GameplayEventFeedback::fallbackLabel : labeler;
        return switch (event) {
            case GameplayEvent.Damage damage -> Optional.of(new Entry(
                    "Took " + damage.amount() + " damage",
                    FeedbackLog.Kind.WARNING,
                    Optional.empty()
            ));
            case GameplayEvent.Heal heal -> Optional.of(new Entry(
                    "Healed " + heal.amount(),
                    FeedbackLog.Kind.SUCCESS,
                    Optional.empty()
            ));
            case GameplayEvent.StatCritical critical -> Optional.of(new Entry(
                    statCriticalMessage(critical),
                    FeedbackLog.Kind.WARNING,
                    Optional.empty()
            ));
            case GameplayEvent.Pickup pickup -> Optional.of(new Entry(
                    "Gathered " + countedLabel(safeLabeler, pickup.itemKey(), pickup.count()),
                    FeedbackLog.Kind.SUCCESS,
                    Optional.of(AudioCue.COLLECT_ITEM)
            ));
            case GameplayEvent.Craft craft -> Optional.of(craftFeedback(craft, safeLabeler));
            case GameplayEvent.CookComplete cook -> Optional.of(new Entry(
                    "Cooked " + countedLabel(safeLabeler, cook.itemKey(), cook.count()),
                    FeedbackLog.Kind.SUCCESS,
                    Optional.of(AudioCue.CAMPFIRE)
            ));
            case GameplayEvent.ProjectileImpact impact -> Optional.of(new Entry(
                    impact.targetEntityId() == GameplayEvent.ProjectileImpact.NO_TARGET_ENTITY
                            ? "Projectile hit"
                            : "Projectile hit target",
                    FeedbackLog.Kind.INFO,
                    Optional.of(impact.targetEntityId() == GameplayEvent.ProjectileImpact.NO_TARGET_ENTITY
                            ? AudioCue.PROJECTILE_HIT
                            : AudioCue.PROJECTILE_HIT_ENTITY)
            ));
            case GameplayEvent.Sleep sleep -> Optional.of(new Entry(
                    sleep.started() ? "Sleep started" : "Rested",
                    sleep.started() ? FeedbackLog.Kind.INFO : FeedbackLog.Kind.SUCCESS,
                    Optional.empty()
            ));
            case GameplayEvent.WeatherThunder ignored -> Optional.of(new Entry(
                    "Thunder nearby",
                    FeedbackLog.Kind.WARNING,
                    Optional.of(AudioCue.THUNDER)
            ));
            case GameplayEvent.JournalEntryDiscovered journal -> Optional.of(new Entry(
                    "Journal updated: " + label(safeLabeler, journal.entryKey()),
                    FeedbackLog.Kind.DISCOVERY,
                    Optional.empty()
            ));
            case GameplayEvent.RecipeUnlocked recipe -> Optional.of(new Entry(
                    "New recipe: " + label(safeLabeler, recipe.recipeKey()),
                    FeedbackLog.Kind.UNLOCK,
                    Optional.of(AudioCue.CRAFT_SUCCESS)
            ));
            case GameplayEvent.StructureDiscovered structure -> Optional.of(new Entry(
                    "Discovered " + label(safeLabeler, structure.structureKey()),
                    FeedbackLog.Kind.DISCOVERY,
                    Optional.empty()
            ));
        };
    }

    private static Entry craftFeedback(GameplayEvent.Craft craft, Function<String, String> labeler) {
        if (craft.success()) {
            return new Entry(
                    "Crafted " + label(labeler, craft.recipeKey()),
                    FeedbackLog.Kind.SUCCESS,
                    Optional.of(AudioCue.CRAFT_SUCCESS)
            );
        }
        String reason = craft.reasonKey().equals("none") ? "" : ": " + label(labeler, craft.reasonKey());
        return new Entry(
                "Craft failed" + reason,
                FeedbackLog.Kind.WARNING,
                Optional.of(AudioCue.CRAFT_FAIL)
        );
    }

    private static String statCriticalMessage(GameplayEvent.StatCritical critical) {
        return switch (critical.statKey()) {
            case "health" -> "Health critical";
            case "hunger" -> critical.value() <= 0 ? "Starving" : "Low hunger";
            case "stamina" -> "Low stamina";
            case "breath" -> "Air running out";
            default -> "Critical " + fallbackLabel(critical.statKey());
        };
    }

    private static String countedLabel(Function<String, String> labeler, String key, int count) {
        String label = label(labeler, key);
        return count == 1 ? label : label + " x" + count;
    }

    private static String label(Function<String, String> labeler, String key) {
        String label = labeler.apply(key);
        return label == null || label.isBlank() ? fallbackLabel(key) : label;
    }

    private static String fallbackLabel(String key) {
        if (key == null || key.isBlank()) {
            return "something";
        }
        int colon = key.indexOf(':');
        String value = colon >= 0 ? key.substring(colon + 1) : key;
        return value.replace('_', ' ');
    }

    public record Entry(String message, FeedbackLog.Kind kind, Optional<AudioCue> audioCue) {
        public Entry {
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("message must not be blank");
            }
            Objects.requireNonNull(kind, "kind");
            audioCue = audioCue == null ? Optional.empty() : audioCue;
        }
    }
}
