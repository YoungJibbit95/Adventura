package dev.voxelgame.server.save;

import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.server.world.BlockEntityStore;
import dev.voxelgame.server.world.BlockEntityType;
import dev.voxelgame.server.world.ServerWorld;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class WorldSaveCodec {
    private static final String KIND = "adventura-world";

    private WorldSaveCodec() {
    }

    public static void write(Path path, WorldSave save, Registry<ItemType> items) throws IOException {
        Properties properties = encode(save, items);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(path)) {
            properties.store(out, "Adventura world save");
        }
    }

    public static WorldSave read(Path path, Registry<ItemType> items) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        }
        return decode(properties, items);
    }

    public static Properties encode(WorldSave save, Registry<ItemType> items) {
        Properties properties = new Properties();
        SaveMetadata metadata = save.metadata();
        properties.setProperty("kind", KIND);
        properties.setProperty("save.version", Integer.toString(metadata.saveVersion()));
        properties.setProperty("game.version", metadata.gameVersion());
        properties.setProperty("world.version", Integer.toString(metadata.worldVersion()));
        properties.setProperty("world.seed", Long.toString(metadata.worldSeed()));
        properties.setProperty("createdAtEpochMillis", Long.toString(metadata.createdAtEpochMillis()));
        properties.setProperty("lastLoadedAtEpochMillis", Long.toString(metadata.lastLoadedAtEpochMillis()));
        properties.setProperty("dayTimeTicks", Long.toString(metadata.dayTimeTicks()));
        writeBlockChanges(properties, save.blockChanges());
        writeBlockEntities(properties, save.blockEntities(), items);
        return properties;
    }

    public static WorldSave decode(Properties properties, Registry<ItemType> items) {
        Properties migrated = migrate(properties);
        if (!KIND.equals(migrated.getProperty("kind"))) {
            throw new IllegalArgumentException("Not an Adventura world save");
        }
        SaveMetadata metadata = new SaveMetadata(
                intValue(migrated, "save.version", SaveMetadata.CURRENT_SAVE_VERSION),
                migrated.getProperty("game.version", SaveMetadata.CURRENT_GAME_VERSION),
                intValue(migrated, "world.version", SaveMetadata.CURRENT_WORLD_VERSION),
                longValue(migrated, "world.seed", 0L),
                longValue(migrated, "createdAtEpochMillis", 0L),
                longValue(migrated, "lastLoadedAtEpochMillis", 0L),
                longValue(migrated, "dayTimeTicks", 0L)
        );
        return new WorldSave(
                metadata,
                readBlockChanges(migrated),
                readBlockEntities(migrated, items)
        );
    }

    private static Properties migrate(Properties properties) {
        Properties migrated = new Properties();
        migrated.putAll(properties);
        int version = intValue(migrated, "save.version", 1);
        if (version > SaveMetadata.CURRENT_SAVE_VERSION) {
            return migrated;
        }
        while (version < SaveMetadata.CURRENT_SAVE_VERSION) {
            version++;
            migrated.setProperty("save.version", Integer.toString(version));
        }
        return migrated;
    }

    private static void writeBlockChanges(Properties properties, List<ServerWorld.BlockChange> blockChanges) {
        properties.setProperty("block.count", Integer.toString(blockChanges.size()));
        for (int i = 0; i < blockChanges.size(); i++) {
            ServerWorld.BlockChange block = blockChanges.get(i);
            String prefix = "block." + i + ".";
            properties.setProperty(prefix + "x", Integer.toString(block.x()));
            properties.setProperty(prefix + "y", Integer.toString(block.y()));
            properties.setProperty(prefix + "z", Integer.toString(block.z()));
            properties.setProperty(prefix + "key", block.blockKey());
        }
    }

    private static List<ServerWorld.BlockChange> readBlockChanges(Properties properties) {
        int count = intValue(properties, "block.count", 0);
        List<ServerWorld.BlockChange> blocks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String prefix = "block." + i + ".";
            blocks.add(new ServerWorld.BlockChange(
                    intValue(properties, prefix + "x", 0),
                    intValue(properties, prefix + "y", 0),
                    intValue(properties, prefix + "z", 0),
                    properties.getProperty(prefix + "key", "voxel:air")
            ));
        }
        return blocks;
    }

    private static void writeBlockEntities(Properties properties, ServerWorld.BlockEntitySnapshot snapshot, Registry<ItemType> items) {
        BlockEntityStore.Snapshot blockEntities = snapshot.blockEntities();
        properties.setProperty("be.count", Integer.toString(blockEntities.entries().size()));
        for (int i = 0; i < blockEntities.entries().size(); i++) {
            BlockEntityStore.EntrySnapshot entry = blockEntities.entries().get(i);
            String prefix = "be." + i + ".";
            properties.setProperty(prefix + "x", Integer.toString(entry.x()));
            properties.setProperty(prefix + "y", Integer.toString(entry.y()));
            properties.setProperty(prefix + "z", Integer.toString(entry.z()));
            properties.setProperty(prefix + "type", entry.typeKey());
        }
        properties.setProperty("be.unknown.count", Integer.toString(blockEntities.unknownEntries().size()));
        for (int i = 0; i < blockEntities.unknownEntries().size(); i++) {
            BlockEntityStore.UnknownEntry entry = blockEntities.unknownEntries().get(i);
            String prefix = "be.unknown." + i + ".";
            properties.setProperty(prefix + "x", Integer.toString(entry.x()));
            properties.setProperty(prefix + "y", Integer.toString(entry.y()));
            properties.setProperty(prefix + "z", Integer.toString(entry.z()));
            properties.setProperty(prefix + "type", entry.typeKey());
            properties.setProperty(prefix + "payload", entry.payload());
        }
        properties.setProperty("storage.count", Integer.toString(snapshot.storageCrates().size()));
        for (int i = 0; i < snapshot.storageCrates().size(); i++) {
            ServerWorld.StorageCrateState storage = snapshot.storageCrates().get(i);
            String prefix = "storage." + i + ".";
            properties.setProperty(prefix + "x", Integer.toString(storage.x()));
            properties.setProperty(prefix + "y", Integer.toString(storage.y()));
            properties.setProperty(prefix + "z", Integer.toString(storage.z()));
            writeItemStacks(properties, prefix + "slot", storage.slots(), items);
        }
        properties.setProperty("campfire.count", Integer.toString(snapshot.campfires().size()));
        for (int i = 0; i < snapshot.campfires().size(); i++) {
            ServerWorld.CampfireState campfire = snapshot.campfires().get(i);
            String prefix = "campfire." + i + ".";
            properties.setProperty(prefix + "x", Integer.toString(campfire.x()));
            properties.setProperty(prefix + "y", Integer.toString(campfire.y()));
            properties.setProperty(prefix + "z", Integer.toString(campfire.z()));
            properties.setProperty(prefix + "fuelSecondsRemaining", Double.toString(campfire.fuelSecondsRemaining()));
        }
        properties.setProperty("loot.count", Integer.toString(snapshot.consumedGeneratedLootCrates().size()));
        for (int i = 0; i < snapshot.consumedGeneratedLootCrates().size(); i++) {
            ServerWorld.BlockEntityPos pos = snapshot.consumedGeneratedLootCrates().get(i);
            String prefix = "loot." + i + ".";
            properties.setProperty(prefix + "x", Integer.toString(pos.x()));
            properties.setProperty(prefix + "y", Integer.toString(pos.y()));
            properties.setProperty(prefix + "z", Integer.toString(pos.z()));
        }
    }

    private static ServerWorld.BlockEntitySnapshot readBlockEntities(Properties properties, Registry<ItemType> items) {
        List<BlockEntityStore.EntrySnapshot> blockEntities = new ArrayList<>();
        List<BlockEntityStore.UnknownEntry> unknownBlockEntities = new ArrayList<>();
        int blockEntityCount = intValue(properties, "be.count", 0);
        for (int i = 0; i < blockEntityCount; i++) {
            String prefix = "be." + i + ".";
            int x = intValue(properties, prefix + "x", 0);
            int y = intValue(properties, prefix + "y", 0);
            int z = intValue(properties, prefix + "z", 0);
            String typeKey = properties.getProperty(prefix + "type", "");
            if (BlockEntityType.fromTypeKey(typeKey).isPresent()) {
                blockEntities.add(new BlockEntityStore.EntrySnapshot(x, y, z, typeKey));
            } else {
                unknownBlockEntities.add(new BlockEntityStore.UnknownEntry(x, y, z, typeKey, ""));
            }
        }
        int unknownCount = intValue(properties, "be.unknown.count", 0);
        for (int i = 0; i < unknownCount; i++) {
            String prefix = "be.unknown." + i + ".";
            unknownBlockEntities.add(new BlockEntityStore.UnknownEntry(
                    intValue(properties, prefix + "x", 0),
                    intValue(properties, prefix + "y", 0),
                    intValue(properties, prefix + "z", 0),
                    properties.getProperty(prefix + "type", ""),
                    properties.getProperty(prefix + "payload", "")
            ));
        }
        List<ServerWorld.StorageCrateState> storageCrates = new ArrayList<>();
        int storageCount = intValue(properties, "storage.count", 0);
        for (int i = 0; i < storageCount; i++) {
            String prefix = "storage." + i + ".";
            storageCrates.add(new ServerWorld.StorageCrateState(
                    intValue(properties, prefix + "x", 0),
                    intValue(properties, prefix + "y", 0),
                    intValue(properties, prefix + "z", 0),
                    readItemStacks(properties, prefix + "slot", ServerWorld.STORAGE_CRATE_SLOTS, items)
            ));
        }
        List<ServerWorld.CampfireState> campfires = new ArrayList<>();
        int campfireCount = intValue(properties, "campfire.count", 0);
        for (int i = 0; i < campfireCount; i++) {
            String prefix = "campfire." + i + ".";
            campfires.add(new ServerWorld.CampfireState(
                    intValue(properties, prefix + "x", 0),
                    intValue(properties, prefix + "y", 0),
                    intValue(properties, prefix + "z", 0),
                    doubleValue(properties, prefix + "fuelSecondsRemaining", 0.0)
            ));
        }
        List<ServerWorld.BlockEntityPos> consumedLoot = new ArrayList<>();
        int lootCount = intValue(properties, "loot.count", 0);
        for (int i = 0; i < lootCount; i++) {
            String prefix = "loot." + i + ".";
            consumedLoot.add(new ServerWorld.BlockEntityPos(
                    intValue(properties, prefix + "x", 0),
                    intValue(properties, prefix + "y", 0),
                    intValue(properties, prefix + "z", 0)
            ));
        }
        return new ServerWorld.BlockEntitySnapshot(
                storageCrates,
                campfires,
                consumedLoot,
                new BlockEntityStore.Snapshot(blockEntities, unknownBlockEntities)
        );
    }

    static void writeItemStacks(Properties properties, String prefix, List<ItemStack> stacks, Registry<ItemType> items) {
        properties.setProperty(prefix + ".count", Integer.toString(stacks.size()));
        for (int i = 0; i < stacks.size(); i++) {
            writeItemStack(properties, prefix + "." + i + ".", stacks.get(i), items);
        }
    }

    static List<ItemStack> readItemStacks(Properties properties, String prefix, int expectedCount, Registry<ItemType> items) {
        int count = intValue(properties, prefix + ".count", expectedCount);
        List<ItemStack> stacks = new ArrayList<>(expectedCount);
        for (int i = 0; i < expectedCount; i++) {
            stacks.add(i < count ? readItemStack(properties, prefix + "." + i + ".", items) : ItemStack.EMPTY);
        }
        return stacks;
    }

    static void writeItemStack(Properties properties, String prefix, ItemStack stack, Registry<ItemType> items) {
        if (stack == null || stack.isEmpty()) {
            properties.setProperty(prefix + "item", "");
            properties.setProperty(prefix + "count", "0");
            properties.setProperty(prefix + "damage", "0");
            return;
        }
        properties.setProperty(prefix + "item", items.findById(stack.itemId()).map(ItemType::key).orElse(""));
        properties.setProperty(prefix + "count", Integer.toString(stack.count()));
        properties.setProperty(prefix + "damage", Integer.toString(stack.damage()));
    }

    static ItemStack readItemStack(Properties properties, String prefix, Registry<ItemType> items) {
        String itemKey = properties.getProperty(prefix + "item", "");
        int count = intValue(properties, prefix + "count", 0);
        int damage = intValue(properties, prefix + "damage", 0);
        if (itemKey.isBlank() || count <= 0) {
            return ItemStack.EMPTY;
        }
        return items.findByKey(itemKey)
                .map(item -> new ItemStack(item.id(), count, Math.max(0, damage)))
                .orElse(ItemStack.EMPTY);
    }

    static int intValue(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static long longValue(Properties properties, String key, long fallback) {
        try {
            return Long.parseLong(properties.getProperty(key, Long.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static double doubleValue(Properties properties, String key, double fallback) {
        try {
            double value = Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
            return Double.isFinite(value) ? value : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
