package dev.voxelgame.server.save;

import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.server.world.ServerWorld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public final class WorldSaveStore {
    private static final Registry<ItemType> ITEMS = Items.createDefaultRegistry();

    private WorldSaveStore() {
    }

    public static WorldSave snapshot(ServerWorld world, double nowSeconds, long createdAtEpochMillis, long lastLoadedAtEpochMillis) {
        return new WorldSave(
                SaveMetadata.current(world.seed(), createdAtEpochMillis, lastLoadedAtEpochMillis, world.dayTimeTicks()),
                world.saveBlockDiffs(),
                world.saveBlockEntities(nowSeconds)
        );
    }

    public static void saveWorld(Path path, ServerWorld world, double nowSeconds) throws IOException {
        long now = System.currentTimeMillis();
        saveSnapshot(path, snapshot(world, nowSeconds, createdAtFor(path, now), now));
    }

    public static CompletableFuture<SaveQueue.SaveResult> saveWorldQueued(
            SaveQueue saveQueue,
            Path path,
            ServerWorld world,
            double nowSeconds
    ) {
        Objects.requireNonNull(saveQueue, "saveQueue");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(world, "world");
        long now = System.currentTimeMillis();
        WorldSave save;
        try {
            save = snapshot(world, nowSeconds, createdAtFor(path, now), now);
        } catch (IOException | IllegalArgumentException exception) {
            return CompletableFuture.failedFuture(exception);
        }
        return saveQueue.enqueue(saveKey(path), () -> {
            saveSnapshot(path, save);
            return Files.exists(path) ? Files.size(path) : 0L;
        });
    }

    public static ServerWorld loadWorld(Path path, double nowSeconds) throws IOException {
        return loadWorld(WorldSaveCodec.read(path, ITEMS), nowSeconds);
    }

    public static ServerWorld loadWorld(WorldSave save, double nowSeconds) {
        ServerWorld world = new ServerWorld(save.metadata().worldSeed());
        world.setDayTimeTicks(save.metadata().dayTimeTicks());
        world.loadBlockDiffs(save.blockChanges());
        world.loadBlockEntities(save.blockEntities(), nowSeconds);
        return world;
    }

    private static long createdAtFor(Path path, long fallback) throws IOException {
        if (!Files.exists(path)) {
            return fallback;
        }
        Properties existing = WorldSaveCodec.readProperties(path);
        return WorldSaveCodec.decode(existing, ITEMS).metadata().createdAtEpochMillis();
    }

    private static void saveSnapshot(Path path, WorldSave save) throws IOException {
        if (Files.exists(path)) {
            Properties existing = WorldSaveCodec.readProperties(path);
            SaveBackup.createBeforeMigrationIfNeeded(path, existing);
        }
        WorldSaveCodec.write(path, save, ITEMS);
    }

    private static String saveKey(Path path) {
        return "world:" + path.toAbsolutePath().normalize();
    }
}
