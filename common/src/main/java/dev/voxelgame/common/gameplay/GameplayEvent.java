package dev.voxelgame.common.gameplay;

import java.util.Objects;
import java.util.UUID;

public sealed interface GameplayEvent permits
        GameplayEvent.Damage,
        GameplayEvent.Heal,
        GameplayEvent.StatCritical,
        GameplayEvent.Pickup,
        GameplayEvent.Craft,
        GameplayEvent.CookComplete,
        GameplayEvent.ProjectileImpact,
        GameplayEvent.Sleep,
        GameplayEvent.WeatherThunder,
        GameplayEvent.JournalEntryDiscovered,
        GameplayEvent.RecipeUnlocked,
        GameplayEvent.StructureDiscovered {
    GameplayEventType type();

    long sequence();

    String debugKey();

    record Damage(long sequence, long entityId, int amount, String sourceKey) implements GameplayEvent {
        public Damage {
            requireSequence(sequence);
            requireEntityId(entityId);
            requirePositive("amount", amount);
            sourceKey = requireKey(sourceKey, "sourceKey");
        }

        @Override
        public GameplayEventType type() {
            return GameplayEventType.DAMAGE;
        }

        @Override
        public String debugKey() {
            return "damage:" + sourceKey;
        }
    }

    record Heal(long sequence, long entityId, int amount) implements GameplayEvent {
        public Heal {
            requireSequence(sequence);
            requireEntityId(entityId);
            requirePositive("amount", amount);
        }

        @Override
        public GameplayEventType type() {
            return GameplayEventType.HEAL;
        }

        @Override
        public String debugKey() {
            return "heal";
        }
    }

    record StatCritical(long sequence, String statKey, int value) implements GameplayEvent {
        public StatCritical {
            requireSequence(sequence);
            statKey = requireKey(statKey, "statKey");
        }

        @Override
        public GameplayEventType type() {
            return GameplayEventType.STAT_CRITICAL;
        }

        @Override
        public String debugKey() {
            return "stat_critical:" + statKey;
        }
    }

    record Pickup(long sequence, String itemKey, int count) implements GameplayEvent {
        public Pickup {
            requireSequence(sequence);
            itemKey = requireKey(itemKey, "itemKey");
            requirePositive("count", count);
        }

        @Override
        public GameplayEventType type() {
            return GameplayEventType.PICKUP;
        }

        @Override
        public String debugKey() {
            return "pickup:" + itemKey;
        }
    }

    record Craft(long sequence, String recipeKey, boolean success, String reasonKey) implements GameplayEvent {
        public Craft {
            requireSequence(sequence);
            recipeKey = requireKey(recipeKey, "recipeKey");
            reasonKey = reasonKey == null || reasonKey.isBlank() ? "none" : reasonKey;
        }

        @Override
        public GameplayEventType type() {
            return GameplayEventType.CRAFT;
        }

        @Override
        public String debugKey() {
            return success ? "craft.success:" + recipeKey : "craft.fail:" + reasonKey;
        }
    }

    record CookComplete(long sequence, String recipeKey, String itemKey, int count) implements GameplayEvent {
        public CookComplete {
            requireSequence(sequence);
            recipeKey = requireKey(recipeKey, "recipeKey");
            itemKey = requireKey(itemKey, "itemKey");
            requirePositive("count", count);
        }

        @Override
        public GameplayEventType type() {
            return GameplayEventType.COOK_COMPLETE;
        }

        @Override
        public String debugKey() {
            return "cook_complete:" + recipeKey;
        }
    }

    record ProjectileImpact(
            long sequence,
            long projectileId,
            String projectileTypeKey,
            double x,
            double y,
            double z,
            long targetEntityId
    ) implements GameplayEvent {
        public static final long NO_TARGET_ENTITY = -1L;

        public ProjectileImpact {
            requireSequence(sequence);
            requireProjectileId(projectileId);
            projectileTypeKey = requireKey(projectileTypeKey, "projectileTypeKey");
            requireFinite("x", x);
            requireFinite("y", y);
            requireFinite("z", z);
            if (targetEntityId < NO_TARGET_ENTITY) {
                throw new IllegalArgumentException("targetEntityId must be >= -1");
            }
        }

        @Override
        public GameplayEventType type() {
            return GameplayEventType.PROJECTILE_IMPACT;
        }

        @Override
        public String debugKey() {
            return "projectile_impact:" + projectileTypeKey;
        }
    }

    record Sleep(long sequence, UUID playerId, boolean started) implements GameplayEvent {
        public Sleep {
            requireSequence(sequence);
            Objects.requireNonNull(playerId, "playerId");
        }

        @Override
        public GameplayEventType type() {
            return GameplayEventType.SLEEP;
        }

        @Override
        public String debugKey() {
            return started ? "sleep.start" : "sleep.end";
        }
    }

    record WeatherThunder(long sequence, double x, double y, double z) implements GameplayEvent {
        public WeatherThunder {
            requireSequence(sequence);
            requireFinite("x", x);
            requireFinite("y", y);
            requireFinite("z", z);
        }

        @Override
        public GameplayEventType type() {
            return GameplayEventType.WEATHER_THUNDER;
        }

        @Override
        public String debugKey() {
            return "weather.thunder";
        }
    }

    record JournalEntryDiscovered(long sequence, UUID playerId, String entryKey) implements GameplayEvent {
        public JournalEntryDiscovered {
            requireSequence(sequence);
            Objects.requireNonNull(playerId, "playerId");
            entryKey = requireKey(entryKey, "entryKey");
        }

        @Override
        public GameplayEventType type() {
            return GameplayEventType.JOURNAL_ENTRY_DISCOVERED;
        }

        @Override
        public String debugKey() {
            return "journal:" + entryKey;
        }
    }

    record RecipeUnlocked(long sequence, UUID playerId, String recipeKey) implements GameplayEvent {
        public RecipeUnlocked {
            requireSequence(sequence);
            Objects.requireNonNull(playerId, "playerId");
            recipeKey = requireKey(recipeKey, "recipeKey");
        }

        @Override
        public GameplayEventType type() {
            return GameplayEventType.RECIPE_UNLOCKED;
        }

        @Override
        public String debugKey() {
            return "recipe_unlocked:" + recipeKey;
        }
    }

    record StructureDiscovered(long sequence, UUID playerId, String structureKey) implements GameplayEvent {
        public StructureDiscovered {
            requireSequence(sequence);
            Objects.requireNonNull(playerId, "playerId");
            structureKey = requireKey(structureKey, "structureKey");
        }

        @Override
        public GameplayEventType type() {
            return GameplayEventType.STRUCTURE_DISCOVERED;
        }

        @Override
        public String debugKey() {
            return "structure:" + structureKey;
        }
    }

    private static void requireSequence(long sequence) {
        if (sequence < 0L) {
            throw new IllegalArgumentException("Gameplay event sequence must be >= 0");
        }
    }

    private static void requireEntityId(long entityId) {
        if (entityId < 0L) {
            throw new IllegalArgumentException("Entity id must be >= 0");
        }
    }

    private static void requireProjectileId(long projectileId) {
        if (projectileId == 0L) {
            throw new IllegalArgumentException("Projectile id cannot be 0");
        }
    }

    private static void requirePositive(String name, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be > 0");
        }
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static String requireKey(String key, String name) {
        Objects.requireNonNull(key, name);
        if (key.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return key;
    }
}
