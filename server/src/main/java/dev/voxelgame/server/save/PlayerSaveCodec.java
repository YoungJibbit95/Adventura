package dev.voxelgame.server.save;

import dev.voxelgame.common.gameplay.status.StatusEffectSaveState;
import dev.voxelgame.common.gameplay.status.StatusEffectState;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.registry.Registry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class PlayerSaveCodec {
    private static final String KIND = "adventura-player";

    private PlayerSaveCodec() {
    }

    public static void write(Path path, PlayerSave save, Registry<ItemType> items) throws IOException {
        Properties properties = encode(save, items);
        SaveFiles.writePropertiesAtomically(path, properties, "Adventura player save");
    }

    public static PlayerSave read(Path path, Registry<ItemType> items) throws IOException {
        return decode(readProperties(path), items);
    }

    static Properties readProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        }
        return properties;
    }

    public static Properties encode(PlayerSave save, Registry<ItemType> items) {
        Properties properties = new Properties();
        properties.setProperty("kind", KIND);
        properties.setProperty("save.version", Integer.toString(save.saveVersion()));
        properties.setProperty("player.id", save.playerId().toString());
        properties.setProperty("player.name", save.playerName());
        properties.setProperty("position.x", Double.toString(save.x()));
        properties.setProperty("position.y", Double.toString(save.y()));
        properties.setProperty("position.z", Double.toString(save.z()));
        properties.setProperty("rotation.yaw", Float.toString(save.yaw()));
        properties.setProperty("rotation.pitch", Float.toString(save.pitch()));
        WorldSaveCodec.writeItemStacks(properties, "inventory", save.inventory(), items);
        properties.setProperty("hotbar.selection", Integer.toString(save.hotbarSelection()));
        properties.setProperty("survival.health", Integer.toString(save.survival().health()));
        properties.setProperty("survival.hunger", Integer.toString(save.survival().hunger()));
        properties.setProperty("survival.stamina", Integer.toString(save.survival().stamina()));
        properties.setProperty("survival.breath", Integer.toString(save.survival().breath()));
        properties.setProperty("spawn.present", Boolean.toString(save.spawnPoint() != null && save.spawnPoint().present()));
        PlayerSave.SpawnPoint spawn = save.spawnPoint() == null ? PlayerSave.SpawnPoint.empty() : save.spawnPoint();
        properties.setProperty("spawn.x", Double.toString(spawn.x()));
        properties.setProperty("spawn.y", Double.toString(spawn.y()));
        properties.setProperty("spawn.z", Double.toString(spawn.z()));
        properties.setProperty("gamemode", save.gameMode());
        properties.setProperty("lastWorldKey", save.lastWorldKey());
        writeStrings(properties, "discovered.recipe", save.discoveredRecipes());
        writeStrings(properties, "discovered.biome", save.discoveredBiomes());
        writeStrings(properties, "journal", save.journalEntries());
        writeStrings(properties, "progress.milestone", save.achievedMilestones());
        writeStrings(properties, "progress.goal", save.completedGoals());
        writeStatusEffects(properties, save.statusEffects());
        writeCreatureFriendships(properties, save.creatureFriendships());
        return properties;
    }

    public static PlayerSave decode(Properties properties, Registry<ItemType> items) {
        if (!KIND.equals(properties.getProperty("kind"))) {
            throw new IllegalArgumentException("Not an Adventura player save");
        }
        UUID playerId = uuidValue(properties.getProperty("player.id", ""), new UUID(0L, 0L));
        int inventoryCount = WorldSaveCodec.intValue(properties, "inventory.count", 36);
        PlayerSave.SpawnPoint spawnPoint = new PlayerSave.SpawnPoint(
                Boolean.parseBoolean(properties.getProperty("spawn.present", "false")),
                WorldSaveCodec.doubleValue(properties, "spawn.x", 0.0),
                WorldSaveCodec.doubleValue(properties, "spawn.y", 0.0),
                WorldSaveCodec.doubleValue(properties, "spawn.z", 0.0)
        );
        return new PlayerSave(
                WorldSaveCodec.intValue(properties, "save.version", SaveMetadata.CURRENT_SAVE_VERSION),
                playerId,
                properties.getProperty("player.name", "Player"),
                WorldSaveCodec.doubleValue(properties, "position.x", 0.0),
                WorldSaveCodec.doubleValue(properties, "position.y", 0.0),
                WorldSaveCodec.doubleValue(properties, "position.z", 0.0),
                (float) WorldSaveCodec.doubleValue(properties, "rotation.yaw", 0.0),
                (float) WorldSaveCodec.doubleValue(properties, "rotation.pitch", 0.0),
                WorldSaveCodec.readItemStacks(properties, "inventory", inventoryCount, items),
                WorldSaveCodec.intValue(properties, "hotbar.selection", 0),
                new PlayerSave.SurvivalStats(
                        WorldSaveCodec.intValue(properties, "survival.health", 20),
                        WorldSaveCodec.intValue(properties, "survival.hunger", 20),
                        WorldSaveCodec.intValue(properties, "survival.stamina", 20),
                        WorldSaveCodec.intValue(properties, "survival.breath", 20)
                ),
                spawnPoint,
                properties.getProperty("gamemode", "survival"),
                readStrings(properties, "discovered.recipe"),
                readStrings(properties, "discovered.biome"),
                readStrings(properties, "journal"),
                readStrings(properties, "progress.milestone"),
                readStrings(properties, "progress.goal"),
                readStatusEffects(properties),
                readCreatureFriendships(properties),
                properties.getProperty("lastWorldKey", "")
        );
    }

    private static void writeStatusEffects(Properties properties, List<StatusEffectSaveState> statusEffects) {
        List<StatusEffectSaveState> safeEffects = statusEffects == null ? List.of() : statusEffects;
        properties.setProperty("status.effect.count", Integer.toString(safeEffects.size()));
        for (int i = 0; i < safeEffects.size(); i++) {
            StatusEffectSaveState effect = safeEffects.get(i);
            String prefix = "status.effect." + i + ".";
            properties.setProperty(prefix + "effectKey", effect.effectKey());
            properties.setProperty(prefix + "remainingSeconds", Double.toString(effect.remainingSeconds()));
            properties.setProperty(prefix + "intensity", Integer.toString(effect.intensity()));
            properties.setProperty(prefix + "tickProgressSeconds", Double.toString(effect.tickProgressSeconds()));
        }
    }

    private static List<StatusEffectSaveState> readStatusEffects(Properties properties) {
        int count = WorldSaveCodec.intValue(
                properties,
                "status.effect.count",
                WorldSaveCodec.intValue(properties, "status.count", 0)
        );
        List<StatusEffectSaveState> effects = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String prefix = "status.effect." + i + ".";
            String legacyPrefix = "status." + i + ".";
            String effectKey = properties.getProperty(
                    prefix + "effectKey",
                    properties.getProperty(prefix + "key", properties.getProperty(legacyPrefix + "effect", ""))
            );
            if (effectKey.isBlank()) {
                continue;
            }
            effects.add(new StatusEffectSaveState(
                    effectKey,
                    doubleValue(properties, prefix + "remainingSeconds", legacyPrefix + "remainingSeconds", 0.0),
                    intValue(properties, prefix + "intensity", legacyPrefix + "intensity", 0),
                    doubleValue(properties, prefix + "tickProgressSeconds", legacyPrefix + "tickProgressSeconds", 0.0)
            ));
        }
        return StatusEffectState.fromSaveStates(effects).saveStates();
    }

    private static int intValue(Properties properties, String key, String legacyKey, int fallback) {
        return properties.containsKey(key)
                ? WorldSaveCodec.intValue(properties, key, fallback)
                : WorldSaveCodec.intValue(properties, legacyKey, fallback);
    }

    private static double doubleValue(Properties properties, String key, String legacyKey, double fallback) {
        return properties.containsKey(key)
                ? WorldSaveCodec.doubleValue(properties, key, fallback)
                : WorldSaveCodec.doubleValue(properties, legacyKey, fallback);
    }

    private static void writeCreatureFriendships(
            Properties properties,
            List<PlayerSave.CreatureFriendshipState> creatureFriendships
    ) {
        List<PlayerSave.CreatureFriendshipState> safeFriendships =
                creatureFriendships == null ? List.of() : creatureFriendships;
        properties.setProperty("creature.friendship.count", Integer.toString(safeFriendships.size()));
        for (int i = 0; i < safeFriendships.size(); i++) {
            PlayerSave.CreatureFriendshipState friendship = safeFriendships.get(i);
            String prefix = "creature.friendship." + i + ".";
            properties.setProperty(prefix + "entity", friendship.entityKey());
            properties.setProperty(prefix + "feedsTotal", Integer.toString(friendship.acceptedFeedsTotal()));
            properties.setProperty(prefix + "feedsToday", Integer.toString(friendship.acceptedFeedsToday()));
            properties.setProperty(prefix + "feedDay", Long.toString(friendship.feedDay()));
            properties.setProperty(prefix + "lastFeedWorldTick", Long.toString(friendship.lastFeedWorldTick()));
        }
    }

    private static List<PlayerSave.CreatureFriendshipState> readCreatureFriendships(Properties properties) {
        int count = WorldSaveCodec.intValue(properties, "creature.friendship.count", 0);
        List<PlayerSave.CreatureFriendshipState> friendships = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String prefix = "creature.friendship." + i + ".";
            String entityKey = properties.getProperty(prefix + "entity", "");
            if (entityKey.isBlank()) {
                continue;
            }
            friendships.add(new PlayerSave.CreatureFriendshipState(
                    entityKey,
                    WorldSaveCodec.intValue(properties, prefix + "feedsTotal", 0),
                    WorldSaveCodec.intValue(properties, prefix + "feedsToday", 0),
                    longValue(properties, prefix + "feedDay", 0L),
                    longValue(properties, prefix + "lastFeedWorldTick", 0L)
            ));
        }
        return List.copyOf(friendships);
    }

    private static void writeStrings(Properties properties, String prefix, List<String> values) {
        properties.setProperty(prefix + ".count", Integer.toString(values.size()));
        for (int i = 0; i < values.size(); i++) {
            properties.setProperty(prefix + "." + i, values.get(i));
        }
    }

    private static List<String> readStrings(Properties properties, String prefix) {
        int count = WorldSaveCodec.intValue(properties, prefix + ".count", 0);
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String value = properties.getProperty(prefix + "." + i, "");
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private static UUID uuidValue(String value, UUID fallback) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static long longValue(Properties properties, String key, long fallback) {
        try {
            return Long.parseLong(properties.getProperty(key, Long.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
