package dev.voxelgame.server.save;

import dev.voxelgame.common.gameplay.status.StatusEffectSaveState;
import dev.voxelgame.common.gameplay.status.StatusEffectState;
import dev.voxelgame.common.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PlayerSave(
        int saveVersion,
        UUID playerId,
        String playerName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        List<ItemStack> inventory,
        int hotbarSelection,
        SurvivalStats survival,
        SpawnPoint spawnPoint,
        String gameMode,
        List<String> discoveredRecipes,
        List<String> discoveredBiomes,
        List<String> journalEntries,
        List<String> achievedMilestones,
        List<String> completedGoals,
        List<StatusEffectSaveState> statusEffects,
        List<CreatureFriendshipState> creatureFriendships,
        String lastWorldKey
) {
    public PlayerSave(
            int saveVersion,
            UUID playerId,
            String playerName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            List<ItemStack> inventory,
            int hotbarSelection,
            SurvivalStats survival,
            SpawnPoint spawnPoint,
            String gameMode,
            List<String> discoveredRecipes,
            List<String> discoveredBiomes,
            List<String> journalEntries,
            String lastWorldKey
    ) {
        this(
                saveVersion,
                playerId,
                playerName,
                x,
                y,
                z,
                yaw,
                pitch,
                inventory,
                hotbarSelection,
                survival,
                spawnPoint,
                gameMode,
                discoveredRecipes,
                discoveredBiomes,
                journalEntries,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                lastWorldKey
        );
    }

    public PlayerSave(
            int saveVersion,
            UUID playerId,
            String playerName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            List<ItemStack> inventory,
            int hotbarSelection,
            SurvivalStats survival,
            SpawnPoint spawnPoint,
            String gameMode,
            List<String> discoveredRecipes,
            List<String> discoveredBiomes,
            List<String> journalEntries,
            List<String> achievedMilestones,
            List<String> completedGoals,
            String lastWorldKey
    ) {
        this(
                saveVersion,
                playerId,
                playerName,
                x,
                y,
                z,
                yaw,
                pitch,
                inventory,
                hotbarSelection,
                survival,
                spawnPoint,
                gameMode,
                discoveredRecipes,
                discoveredBiomes,
                journalEntries,
                achievedMilestones,
                completedGoals,
                List.of(),
                List.of(),
                lastWorldKey
        );
    }

    public PlayerSave(
            int saveVersion,
            UUID playerId,
            String playerName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            List<ItemStack> inventory,
            int hotbarSelection,
            SurvivalStats survival,
            SpawnPoint spawnPoint,
            String gameMode,
            List<String> discoveredRecipes,
            List<String> discoveredBiomes,
            List<String> journalEntries,
            List<String> achievedMilestones,
            List<String> completedGoals,
            List<StatusEffectSaveState> statusEffects,
            String lastWorldKey
    ) {
        this(
                saveVersion,
                playerId,
                playerName,
                x,
                y,
                z,
                yaw,
                pitch,
                inventory,
                hotbarSelection,
                survival,
                spawnPoint,
                gameMode,
                discoveredRecipes,
                discoveredBiomes,
                journalEntries,
                achievedMilestones,
                completedGoals,
                statusEffects,
                List.of(),
                lastWorldKey
        );
    }

    public PlayerSave {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Player save position and rotation must be finite");
        }
        playerId = Objects.requireNonNull(playerId, "playerId");
        playerName = playerName == null || playerName.isBlank() ? "Player" : playerName;
        inventory = inventory == null ? List.of() : List.copyOf(inventory);
        survival = survival == null ? SurvivalStats.defaults() : survival;
        spawnPoint = spawnPoint == null ? SpawnPoint.empty() : spawnPoint;
        gameMode = gameMode == null || gameMode.isBlank() ? "survival" : gameMode;
        discoveredRecipes = discoveredRecipes == null ? List.of() : List.copyOf(discoveredRecipes);
        discoveredBiomes = discoveredBiomes == null ? List.of() : List.copyOf(discoveredBiomes);
        journalEntries = journalEntries == null ? List.of() : List.copyOf(journalEntries);
        achievedMilestones = copyProgressKeys(achievedMilestones);
        completedGoals = copyProgressKeys(completedGoals);
        statusEffects = copyStatusEffects(statusEffects);
        creatureFriendships = copyCreatureFriendships(creatureFriendships);
        lastWorldKey = lastWorldKey == null ? "" : lastWorldKey;
    }

    private static List<String> copyProgressKeys(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
    }

    private static List<StatusEffectSaveState> copyStatusEffects(List<StatusEffectSaveState> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return StatusEffectState.fromSaveStates(values).saveStates();
    }

    private static List<CreatureFriendshipState> copyCreatureFriendships(List<CreatureFriendshipState> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public record CreatureFriendshipState(
            String entityKey,
            int acceptedFeedsTotal,
            int acceptedFeedsToday,
            long feedDay,
            long lastFeedWorldTick
    ) {
        public CreatureFriendshipState {
            entityKey = entityKey == null || entityKey.isBlank() ? "unknown" : entityKey.strip();
            acceptedFeedsTotal = Math.max(0, acceptedFeedsTotal);
            acceptedFeedsToday = Math.max(0, acceptedFeedsToday);
            feedDay = Math.max(0L, feedDay);
            lastFeedWorldTick = Math.max(0L, lastFeedWorldTick);
        }
    }

    public record SurvivalStats(int health, int hunger, int stamina, int breath) {
        public SurvivalStats {
            health = clampStat(health);
            hunger = clampStat(hunger);
            stamina = clampStat(stamina);
            breath = clampStat(breath);
        }

        public static SurvivalStats defaults() {
            return new SurvivalStats(20, 20, 20, 20);
        }

        private static int clampStat(int value) {
            return Math.max(0, Math.min(20, value));
        }
    }

    public record SpawnPoint(boolean present, double x, double y, double z) {
        public SpawnPoint {
            if (present && (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))) {
                throw new IllegalArgumentException("Spawn point must be finite when present");
            }
        }

        public static SpawnPoint empty() {
            return new SpawnPoint(false, 0.0, 0.0, 0.0);
        }
    }
}
